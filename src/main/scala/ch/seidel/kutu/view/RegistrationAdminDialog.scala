package ch.seidel.kutu.view

import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.data.RegistrationAdmin.{doSyncUnassignedClubRegistrations, findAthletLike, processSync}
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.RegistrationRoutes
import javafx.beans.value.{ChangeListener, ObservableValue}
import org.apache.pekko.http.javadsl.model.HttpResponse
import org.slf4j.LoggerFactory
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.layout.{BorderPane, Priority}

import scala.concurrent.Future

object RegistrationAdminDialog {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def importVereinRegistration(service: RegistrationRoutes, verein: Registration, reloader: Boolean => Unit): List[Verein] = {
    verein.vereinId match {
      case Some(id) => List(service.insertVerein(Verein(id, verein.vereinname, Some(verein.verband))))
      case None => PageDisplayer.confirm(
        "Verein importieren ...",
        Seq(
          s"Soll der Verein ${verein.vereinname}, (${verein.verband})",
          s"mit Verantwortlichen ${verein.respVorname} ${verein.respName}",
          s"mit Kontaktadresse ${verein.mail} ${verein.mobilephone}",
          "neu angelegt werden?"
        ),
        () => {
          val v = service.insertVerein(Verein(verein.vereinId.getOrElse(0), verein.vereinname, Some(verein.verband)))
          reloader(true)
          // TODO update registration with verein_id or better do this operation remote and download verein
          v
        }
      ).toList
    }
  }

  def importRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: Boolean => Unit)(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.*
    import scala.util.{Failure, Success}
    val athletModel = ObservableBuffer[SyncAction]()
    val vereineList = service.selectVereine
    logger.info("start import Registration Analyse ...")
    val cliprawf: Future[(Set[Verein],List[SyncAction])] = KuTuApp.invokeAsyncWithBusyIndicator("Online-Registrierungen abgleichen ...") {
      service.loginWithWettkampf(wkInfo.wettkampf.toWettkampf).map {
        case r: HttpResponse if r.status.isSuccess() =>
          (for
            (registration, athleten) <- service.getAllRegistrationsRemote(wkInfo.wettkampf.toWettkampf)
            athlet <- athleten :+ EmptyAthletRegistration(registration.id)
          yield {
            logger.info(s"start processing Registration ${registration.vereinname}")
            val startime = System.currentTimeMillis()
            val resolvedVerein = vereineList.find(v => registration.matchesVerein(v))
            logger.info(s"resolved Verein for Registration ${registration.vereinname}")
            val parsed = athlet.toAthlet.copy(id = 0L, verein = resolvedVerein.map(_.id))
            val candidate = if athlet.isEmptyRegistration then parsed else findAthletLike(parsed)
            logger.info(s"resolved candidate for $parsed in ${System.currentTimeMillis() - startime}ms")
            (registration, athlet, parsed, candidate.toAthletView(resolvedVerein))
          }).toList
        case r: HttpResponse if !r.status.isSuccess() =>
          throw new IllegalStateException(s"Es konnten keine Anmeldungen abgefragt werden: ${r.status.reason()}, ${r.status.defaultMessage()}")
      }.map(doSyncUnassignedClubRegistrations(wkInfo, service))
    }
    cliprawf.onComplete {
      case Failure(t) => PageDisplayer.showErrorDialog("Online-Anmeldungen abfragen", t.getMessage)
      case Success(clipraw) => Platform.runLater {
        if clipraw._2.nonEmpty then {
          showImportDialog(wkInfo, service, reloader, athletModel, clipraw)(using event)
        } else {
          PageDisplayer.showMessageDialog("Anmeldungen verarbeiten", "Keine neuen Daten zum verarbeiten.")
        }
      }
    }
  }

  private def showImportDialog(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: Boolean => Unit, athletModel: ObservableBuffer[SyncAction], clipraw: (Set[Verein],List[SyncAction]))(implicit event: ActionEvent): Unit = {
    athletModel.appendAll(clipraw._2)
    val programms = wkInfo.leafprograms
    val filteredModel = ObservableBuffer.from(athletModel)

    def suggestImportAthletText(parsed: Athlet, suggestion: AthletView) = {
      if suggestion.id > 0 then {
        "als " + suggestion.toAthlet.extendedprint
      }
      else {
        "wird neu importiert"
      }
    }

    val athletTable = new TableView[SyncAction](filteredModel) {
      columns ++= List(
        new TableColumn[SyncAction, String] {
          text = "Verein"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "verein", {
              x.value match {
                case AddVereinAction(verein) => s"${verein.vereinname} (${verein.verband})"
                case ApproveVereinAction(verein) => s"${verein.vereinname} (${verein.verband})"
                case RenameVereinAction(verein, oldVerein) => s"${oldVerein.easyprint})"
                case RenameAthletAction(verein, _, _, _) => s"${verein.vereinname} (${verein.verband})"
                case AddRegistration(reg, programId, athlet, suggestion, team, media) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
                case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
                case ua:UpdateAthletMediaAction =>
                  s"${ua.verein.vereinname} (${ua.verein.verband})"
                case am: AddMedia => ""
              }
            })
          }
          minWidth = 200
        }, new TableColumn[SyncAction, String] {
          text = "Person (Name, Vorname, Geb.)"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "athlet", {
              x.value match {
                case AddVereinAction(verein) => s"${verein.respVorname} ${verein.respName}, ${verein.mail}, ${verein.mobilephone}"
                case ApproveVereinAction(verein) => s"${verein.respVorname} ${verein.respName}, ${verein.mail}, ${verein.mobilephone}"
                case RenameVereinAction(verein, oldVerein) => s"${verein.respVorname} ${verein.respName}, ${verein.mail}, ${verein.mobilephone}"
                case RenameAthletAction(_, _, existing, _) => s"${existing.extendedprint}"
                case AddRegistration(reg, programId, athlet, suggestion, team, media) => athlet.extendedprint
                case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) => athlet.extendedprint
                case RemoveRegistration(reg, programId, athlet, suggestion) => athlet.extendedprint
                case ua:UpdateAthletMediaAction => ua.athletReg.toAthlet.extendedprint
                case am: AddMedia => ""
              }
            })
          }
          minWidth = 250
        },
        new TableColumn[SyncAction, String] {
          text = "Kategorie/Programm"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "programm", {
              val programId = x.value match {
                case AddVereinAction(verein) => 0L
                case ApproveVereinAction(verein) => 0L
                case AddRegistration(reg, programId, athlet, suggestion, team, media) => programId
                case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) => toProgramid
                case RemoveRegistration(reg, programId, athlet, suggestion) => programId
                case _ => ""
              }
              programms.find { p => p.id == programId || p.aggregatorHead.id == programId } match {
                case Some(programm) => programm.name
                case _ => ""
              }
            })
          }
        },
        new TableColumn[SyncAction, String] {
          text = "Import-Vorschlag"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "dbmatch", {
              x.value match {
                case AddVereinAction(verein) => s"Verein ${verein.vereinname} wird neu importiert"
                case ApproveVereinAction(verein) => s"Verein ${verein.vereinname} wird bestätigt"
                case RenameVereinAction(verein, oldVerein) => s"Verein wird auf ${verein.toVerein.easyprint} korrigiert"
                case RenameAthletAction(verein, _, _, expected) => s"Athlet wird auf ${expected.extendedprint} korrigiert"
                case AddRegistration(reg, programId, athlet, suggestion, team, media) =>
                  suggestImportAthletText(athlet, suggestion)
                case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) =>
                  suggestImportAthletText(athlet, suggestion)
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  suggestImportAthletText(athlet, suggestion)
                case ua:UpdateAthletMediaAction => ua.caption
                case am: AddMedia => am.caption
              }
            })
          }
        },
        new TableColumn[SyncAction, String] {
          text = "Einteilungs-Aktion"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "assignaction", {
              x.value match {
                case AddVereinAction(verein) => "hinzufügen"
                case ApproveVereinAction(verein) => "bestätigen"
                case RenameVereinAction(verein, _) => ""
                case RenameAthletAction(verein, _, _, _) => ""
                case AddRegistration(reg, programId, athlet, suggestion, team, media) => "hinzufügen"
                case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) =>
                  val teamText = if fromTeam != toTeam then s", von Team $fromTeam auf $toTeam" else ""
                  val pgmText = programms.find { p => p.id == fromProgramId || p.aggregatorHead.id == fromProgramId } match {
                    case Some(programm) => programm.name
                    case _ => "unbekannt"
                  }
                  s"umteilen von $pgmText$teamText"
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  s"entfernen"
                case ua:UpdateAthletMediaAction =>
                  if ua.athletReg.mediafile.nonEmpty then "Playlist nachführen" else "Musik entfernen"
                case am: AddMedia => "Musik herunterladen"
              }
            })
          }
        }
      )
    }
    athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
    val filter = new TextField() {
      promptText = "Such-Text"
      text.addListener(new ChangeListener[String] {
        def changed(o: ObservableValue[? <: String], oldVal: String, newVal: String): Unit = {
          val sortOrder = athletTable.sortOrder.toList
          filteredModel.clear()
          val searchQuery = newVal.toUpperCase().split(" ")
          for syncAction <- athletModel  do {
            val (athlet, verein) = syncAction match {
              case AddVereinAction(verein) => (Athlet(), Some(verein.toVerein))
              case ApproveVereinAction(verein) => (Athlet(), Some(verein.toVerein))
              case AddRegistration(reg, programId, athlet, suggestion, team, media) => (athlet, suggestion.verein)
              case MoveRegistration(reg, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) => (athlet, suggestion.verein)
              case RemoveRegistration(reg, programId, athlet, suggestion) => (athlet, suggestion.verein)
              case RenameVereinAction(verein, oldVerein) => (Athlet(), Some(verein.toVerein))
              case RenameAthletAction(verein, athlet, existing, expected) => (expected, Some(verein.toVerein))
              case _ => (Athlet(), None)
            }
            val matches = searchQuery.forall { search =>
              if search.isEmpty || athlet.name.toUpperCase().contains(search) then {
                true
              }
              else if athlet.vorname.toUpperCase().contains(search) then {
                true
              }
              else if verein match {
                case Some(v) => v.name.toUpperCase().contains(search)
                case None => false
              } then {
                true
              }
              else {
                false
              }
            }

            if matches then {
              filteredModel.add(syncAction)
            }
          }
          athletTable.sortOrder.clear()
          athletTable.sortOrder ++= sortOrder
        }
      })
    }
    PageDisplayer.showInDialog("Anmeldungen importieren ...", new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          minWidth = 1000
          center = new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            top = filter
            center = athletTable
            minWidth = 1000
          }

        }
      }
    }, new Button("OK") {
      disable <== when(athletTable.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false
      onAction = (event: ActionEvent) => {
        if !athletTable.selectionModel().isEmpty then {
          val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
            x => athletTable.selectionModel.value.isSelected(x._2)
          }.map(_._1)
          val localAttentionNeeded = processSync(wkInfo, service, selectedAthleten.toList, clipraw._1)
          reloader(selectedAthleten.exists {
            case AddVereinAction(_) => true
            case _: RenameVereinAction => true
            case _ => false
          })
          if localAttentionNeeded.nonEmpty then {
            println(localAttentionNeeded)
            PageDisplayer.showMessageDialog(
              "Anmeldungen verarbeiten",
              s"""Die ausgewählten Mutationen sind durchgeführt.
                |Es gab ${localAttentionNeeded.size} neue Riegen, welche noch frisch eingeteilt werden müssen.
                |
                |Bitte die Einteilungen in Riegen, Durchgang und Startgerät manuell nachführen und dann den
                |Wettkampf wieder hochladen.""".stripMargin)
          } else {
            PageDisplayer.showMessageDialog(
              "Anmeldungen verarbeiten",
              """Die ausgewählten Mutationen sind vollständig automatisiert durchgeführt.
                |
                |Es sind keine weiteren Aktionen notwendig.
                """.stripMargin)
          }
        } else {
          PageDisplayer.showWarnDialog("Anmeldungen verarbeiten", "Keine Verarbeitung!\nEs wurde nichts in der Liste ausgewählt.")
        }
      }
    }, new Button("OK Alle") {
      onAction = (event: ActionEvent) => {
        val localAttentionNeeded = processSync(wkInfo, service, filteredModel.toList, clipraw._1)
        reloader(filteredModel.exists {
          case AddVereinAction(_) => true
          case _ => false
        })
        if localAttentionNeeded.nonEmpty then {
          println(localAttentionNeeded)
          PageDisplayer.showMessageDialog(
            "Anmeldungen verarbeiten",
            s"""Die ausgewählten Mutationen sind durchgeführt.
               |Es gab ${localAttentionNeeded.size} neue Riegen, welche noch frisch eingeteilt werden müssen.
               |${localAttentionNeeded.mkString("* ", "\n  ", "")}
               |Bitte die Einteilungen in Riegen, Durchgang und Startgerät manuell nachführen und dann den
               |Wettkampf wieder hochladen.""".stripMargin)
        } else {
          PageDisplayer.showMessageDialog(
            "Anmeldungen verarbeiten",
            """Die ausgewählten Mutationen sind vollständig automatisiert durchgeführt.
              |
              |Es sind keine weiteren Aktionen notwendig.
                """.stripMargin)
        }
      }
    })
  }
}
