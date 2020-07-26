package ch.seidel.kutu.view

import akka.http.javadsl.model.HttpResponse
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain.{AddRegistration, AddVereinAction, Athlet, AthletView, EmptyAthletRegistration, MatchCode, MoveRegistration, Registration, RemoveRegistration, SyncAction, Verein}
import ch.seidel.kutu.http.RegistrationRoutes
import ch.seidel.kutu.view.RegistrationAdmin.{doSyncUnassignedClubRegistrations, processSync}
import javafx.beans.value.ObservableValue
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, Priority}

import scala.concurrent.Future

object RegistrationAdminDialog {

  def importVereinRegistration(service: RegistrationRoutes, verein: Registration, reloader: Boolean => Unit) = {
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
          reloader(true);
          // TODO update registration with verein_id or better do this operation remote and download verein
          v
        }
      ).toList
    }
  }

  def importRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: Boolean => Unit)(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}
    val athletModel = ObservableBuffer[SyncAction]()
    val vereineList = service.selectVereine
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf: Future[(Set[Verein],Vector[SyncAction])] = KuTuApp.invokeAsyncWithBusyIndicator {
      service.loginWithWettkampf(wkInfo.wettkampf.toWettkampf).map {
        case r: HttpResponse if r.status.isSuccess() =>
          (for {
            (registration, athleten) <- service.getAllRegistrationsRemote(wkInfo.wettkampf.toWettkampf)
            athlet <- athleten :+ EmptyAthletRegistration(registration.id)
          } yield {
            val resolvedVerein = vereineList.find(v => v.name.equals(registration.vereinname) && (v.verband.isEmpty || v.verband.get.equals(registration.verband)))
            val parsed = athlet.toAthlet.copy(verein = resolvedVerein.map(_.id))
            val candidate = if (athlet.isEmptyRegistration) parsed else service.findAthleteLike(cache)(parsed)
            (registration, athlet, parsed, AthletView(
              candidate.id, candidate.js_id,
              candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
              candidate.strasse, candidate.plz, candidate.ort,
              resolvedVerein, true))
          }).toList
        case r: HttpResponse if !r.status.isSuccess() =>
          throw new IllegalStateException(s"Es konnten keine Anmeldungen abgefragt werden: ${r.status.reason()}, ${r.status.defaultMessage()}")
      }.map(doSyncUnassignedClubRegistrations(wkInfo, service))
    }
    cliprawf.onComplete {
      case Failure(t) => PageDisplayer.showErrorDialog("Online-Anmeldungen abfragen", t.getMessage)
      case Success(clipraw) => Platform.runLater {
        if (clipraw._2.nonEmpty) {
          showImportDialog(wkInfo, service, reloader, athletModel, clipraw)(event)
        } else {
          PageDisplayer.showMessageDialog("Anmeldungen verarbeiten", "Keine neuen Daten zum verarbeiten.")
        }
      }
    }
  }

  private def showImportDialog(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: Boolean => Unit, athletModel: ObservableBuffer[SyncAction], clipraw: (Set[Verein],Vector[SyncAction]))(implicit event: ActionEvent) = {
    athletModel.appendAll(clipraw._2)
    val programms = wkInfo.leafprograms
    val filteredModel = ObservableBuffer[SyncAction](athletModel)
    val athletTable = new TableView[SyncAction](filteredModel) {
      columns ++= List(
        new TableColumn[SyncAction, String] {
          text = "Verein"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "verein", {
              x.value match {
                case AddVereinAction(verein) => s"${verein.vereinname} (${verein.verband})"
                case AddRegistration(reg, programId, athlet, suggestion) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
                case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.flatMap(_.verband).getOrElse(reg.verband)})"
              }
            })
          }
          minWidth = 200
        }, new TableColumn[SyncAction, String] {
          text = "Athlet"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "athlet", {
              def renderAthlet(athlet: Athlet) = s"${athlet.name} ${athlet.vorname}, ${
                athlet.gebdat.map(d => f"$d%tY") match {
                  case None => ""
                  case Some(t) => t
                }
              }"

              x.value match {
                case AddVereinAction(verein) => s"${verein.respVorname} ${verein.respName}, ${verein.mail}, ${verein.mobilephone}"
                case AddRegistration(reg, programId, athlet, suggestion) => renderAthlet(athlet)
                case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) => renderAthlet(athlet)
                case RemoveRegistration(reg, programId, athlet, suggestion) => renderAthlet(athlet)
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
                case AddRegistration(reg, programId, athlet, suggestion) => programId
                case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) => toProgramid
                case RemoveRegistration(reg, programId, athlet, suggestion) => programId
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
                case AddRegistration(reg, programId, athlet, suggestion) =>
                  if (suggestion.id > 0) "als " + suggestion.easyprint else "wird neu importiert"
                case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) =>
                  if (suggestion.id > 0) "als " + suggestion.easyprint else "wird neu importiert"
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  if (suggestion.id > 0) "als " + suggestion.easyprint else "wird neu importiert"
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
                case AddRegistration(reg, programId, athlet, suggestion) =>
                  "hinzufügen"
                case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) =>
                  val pgmText = programms.find { p => p.id == fromProgramId || p.aggregatorHead.id == fromProgramId } match {
                    case Some(programm) => programm.name
                    case _ => "unbekannt"
                  }
                  s"umteilen von $pgmText"
                case RemoveRegistration(reg, programId, athlet, suggestion) =>
                  s"entfernen"
              }
            })
          }
        }
      )
    }
    athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
    val filter = new TextField() {
      promptText = "Such-Text"
      text.addListener { (o: ObservableValue[_ <: String], oldVal: String, newVal: String) =>
        val sortOrder = athletTable.sortOrder.toList
        filteredModel.clear()
        val searchQuery = newVal.toUpperCase().split(" ")
        for {syncAction <- athletModel
             } {
          val (athlet, verein) = syncAction match {
            case AddVereinAction(verein) => (Athlet(), Some(verein.toVerein))
            case AddRegistration(reg, programId, athlet, suggestion) => (athlet, suggestion.verein)
            case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) => (athlet, suggestion.verein)
            case RemoveRegistration(reg, programId, athlet, suggestion) => (athlet, suggestion.verein)
          }
          val matches = searchQuery.forall { search =>
            if (search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
              true
            }
            else if (athlet.vorname.toUpperCase().contains(search)) {
              true
            }
            else if (verein match {
              case Some(v) => v.name.toUpperCase().contains(search)
              case None => false
            }) {
              true
            }
            else {
              false
            }
          }

          if (matches) {
            filteredModel.add(syncAction)
          }
        }
        athletTable.sortOrder.clear()
        athletTable.sortOrder ++= sortOrder
      }
    }
    PageDisplayer.showInDialog("Anmeldungen importieren ...", new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          minWidth = 900
          center = new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            top = filter
            center = athletTable
            minWidth = 900
          }

        }
      }
    }, new Button("OK") {
      onAction = (event: ActionEvent) => {
        if (!athletTable.selectionModel().isEmpty) {
          val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
            x => athletTable.selectionModel.value.isSelected(x._2)
          }.map(_._1)
          processSync(wkInfo, service, selectedAthleten.toList, clipraw._1)
          reloader(selectedAthleten.exists {
            case AddVereinAction(_) => true
            case _ => false
          })
          PageDisplayer.showMessageDialog(
            "Anmeldungen verarbeiten",
            "Die ausgewählten Mutationen sind durchgeführt. \n" +
              "Die Anpassungen sind nur lokal und müssen bei Bedarf über 'upload' noch im Neztwerk bereitgestellt werden.\n" +
              "Bitte auch die Einteilung kontrollieren. Wenn neue Vereine oder Programme/Kategorien eingeteilt werden,\n" +
              "müssen dort die Einteilungen in Riegen, Durchgang und Startgerät manuell nachgeführt werden.")
        } else {
          PageDisplayer.showWarnDialog("Anmeldungen verarbeiten", "Keine Verarbeitung!\nEs wurde nichts in der Liste ausgewählt.")
        }
      }
    }, new Button("OK Alle") {
      onAction = (event: ActionEvent) => {
        processSync(wkInfo, service, filteredModel.toList, clipraw._1)
        reloader(filteredModel.exists {
          case AddVereinAction(_) => true
          case _ => false
        })
        PageDisplayer.showMessageDialog(
          "Anmeldungen verarbeiten",
          "Die Mutationen sind durchgeführt. \n" +
            "Die Anpassungen sind nur lokal und müssen bei Bedarf über 'upload' noch im Neztwerk bereitgestellt werden.\n" +
            "Bitte auch die Einteilung kontrollieren. Wenn neue Vereine oder Programme/Kategorien eingeteilt werden,\n" +
            "müssen dort die Einteilungen in Riegen, Durchgang und Startgerät manuell nachgeführt werden.")
      }
    })
  }
}
