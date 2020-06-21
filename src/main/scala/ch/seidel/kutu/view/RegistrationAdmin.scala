package ch.seidel.kutu.view

import akka.http.javadsl.model.HttpResponse
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain.{Athlet, AthletRegistration, AthletView, MatchCode, Registration, Verein, WertungView}
import ch.seidel.kutu.http.RegistrationRoutes
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

object RegistrationAdmin {

  sealed trait SyncAction

  case class AddRegistration(programId: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction

  case class MoveRegistration(fromProgramId: Long, toProgramid: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction

  case class RemoveRegistration(programId: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction

  def importVereinRegistration(service: RegistrationRoutes, verein: Registration) = {
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
          // TODO update registration with verein_id or better do this operation remote and download verein
          v
        }
      ).toList
    }
  }

  // TODO Sync with existing assignments
  def doSyncUnassignedClubRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes)(registrations: List[(Registration, AthletRegistration, Athlet, AthletView)]): List[SyncAction] = {
    val relevantClubs = registrations.flatMap(_._4.verein).toSet
    val existingPgmAthletes: Map[Long, List[Long]] = registrations.groupBy(_._2.programId).map(group => (group._1 -> group._2.map(_._4.id)))
    val existingAthletes: Set[Long] = registrations.map(_._4.id).toSet
    val nonmatching: Map[String, Seq[WertungView]] = service.
      selectWertungen(wkuuid = wkInfo.wettkampf.uuid).
      filter { wertung: WertungView =>
        relevantClubs.exists(club => wertung.athlet.verein.contains(club))
      }.groupBy { wertung: WertungView =>
      if (existingAthletes.contains(wertung.athlet.id)) {
        (existingPgmAthletes.get(wertung.wettkampfdisziplin.programm.id) match {
          case Some(athletList: List[Long]) if athletList.contains(wertung.athlet.id) => "Unverändert"
          case _ => "Umteilen"
        })
      } else {
        "Entfernen"
      }
    }
    println(nonmatching)
    val removeActions: Seq[SyncAction] = nonmatching.get("Entfernen") match {
      case Some(list) => list
        .map(wertung => RemoveRegistration(wertung.wettkampfdisziplin.programm.id, wertung.athlet.toAthlet, wertung.athlet))
        .toSet.toSeq
      case _ => Seq()
    }

    val mutationActions: Seq[SyncAction] = registrations.filter(r => nonmatching.get("Unverändert") match {
      case None => true
      case Some(list) => !list.exists(p => p.athlet.id == r._4.id)
    }).map { r =>
      (nonmatching.get("Umteilen") match {
        case Some(list) => list.find(p => p.athlet.id == r._4.id) match {
          case Some(wertungView) => Some(MoveRegistration(wertungView.wettkampfdisziplin.programm.id, r._2.programId, r._3, r._4))
          case _ => None
        }
        case _ => None

      }) match {
        case Some(moveRegistration) => moveRegistration
        case None => AddRegistration(r._2.programId, r._3, r._4)
      }
    }
    val actions = removeActions ++ mutationActions

    actions.toList
  }

  def importRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: () => Unit)(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}
    val athletModel = ObservableBuffer[SyncAction]()
    val vereineList = service.selectVereine
    val programms = wkInfo.leafprograms
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf: Future[List[SyncAction]] = KuTuApp.invokeAsyncWithBusyIndicator {
      service.loginWithWettkampf(wkInfo.wettkampf.toWettkampf).map {
        case r: HttpResponse if r.status.isSuccess() =>
          (for {
            (verein, athleten) <- service.getAllRegistrations(wkInfo.wettkampf.toWettkampf)
            resolvedVerein <- vereineList.find(v => v.name.equals(verein.vereinname) && (v.verband.isEmpty || v.verband.get.equals(verein.verband))) match {
              case Some(verein) => List(verein)
              case None => importVereinRegistration(service, verein)
            }
            athlet <- athleten
          } yield {
            val parsed = athlet.toAthlet.copy(verein = Some(resolvedVerein.id))
            val candidate = service.findAthleteLike(cache)(parsed)
            (verein, athlet, parsed, AthletView(
              candidate.id, candidate.js_id,
              candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
              candidate.strasse, candidate.plz, candidate.ort,
              Some(resolvedVerein), true))
          }).toList
        case r: HttpResponse if !r.status.isSuccess() =>
          throw new IllegalStateException(s"Es konnten keine Anmeldungen abgefragt werden: ${r.status.reason()}, ${r.status.defaultMessage()}")
      }.map(doSyncUnassignedClubRegistrations(wkInfo, service))
    }
    cliprawf.onComplete {
      case Failure(t) => PageDisplayer.showErrorDialog("Online-Anmeldungen abfragen", t.getMessage)
      case Success(clipraw) => Platform.runLater {
        if (clipraw.nonEmpty) {
          athletModel.appendAll(clipraw)
          val filteredModel = ObservableBuffer[SyncAction](athletModel)
          val athletTable = new TableView[SyncAction](filteredModel) {
            columns ++= List(
              new TableColumn[SyncAction, String] {
                text = "Verein"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "verein", {
                    x.value match {
                      case AddRegistration(programId, athlet, suggestion) =>
                        s"${suggestion.verein.get.name} (${suggestion.verein.get.verband.get})"
                      case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) =>
                        s"${suggestion.verein.get.name} (${suggestion.verein.get.verband.get})"
                      case RemoveRegistration(programId, athlet, suggestion) =>
                        s"${suggestion.verein.get.name} (${suggestion.verein.get.verband.get})"
                    }
                  })
                }
                minWidth = 200
              }, new TableColumn[SyncAction, String] {
                text = "Athlet"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    val athlet = x.value match {
                      case AddRegistration(programId, athlet, suggestion) => athlet
                      case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) => athlet
                      case RemoveRegistration(programId, athlet, suggestion) => athlet
                    }
                    s"${athlet.name} ${athlet.vorname}, ${
                      athlet.gebdat.map(d => f"$d%tY") match {
                        case None => ""
                        case Some(t) => t
                      }
                    }"
                  })
                }
                minWidth = 250
              },
              new TableColumn[SyncAction, String] {
                text = "Kategorie/Programm"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "programm", {
                    val programId = x.value match {
                      case AddRegistration(programId, athlet, suggestion) => programId
                      case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) => toProgramid
                      case RemoveRegistration(programId, athlet, suggestion) => programId
                    }
                    programms.find { p => p.id == programId || p.aggregatorHead.id == programId } match {
                      case Some(programm) => programm.name
                      case _ => "unbekannt"
                    }
                  })
                }
              },
              new TableColumn[SyncAction, String] {
                text = "Import-Vorschlag"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "dbmatch", {
                    val suggestion = x.value match {
                      case AddRegistration(programId, athlet, suggestion) => suggestion
                      case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) => suggestion
                      case RemoveRegistration(programId, athlet, suggestion) => suggestion
                    }
                    if (suggestion.id > 0) "als " + suggestion.easyprint else "wird neu importiert"
                  })
                }
              },
              new TableColumn[SyncAction, String] {
                text = "Einteilungs-Aktion"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "assignaction", {
                    x.value match {
                      case AddRegistration(programId, athlet, suggestion) =>
                        "hinzufügen"
                      case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) =>
                        val pgmText = programms.find { p => p.id == fromProgramId || p.aggregatorHead.id == fromProgramId } match {
                          case Some(programm) => programm.name
                          case _ => "unbekannt"
                        }
                        s"umteilen von $pgmText"
                      case RemoveRegistration(programId, athlet, suggestion) =>
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
            text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for {syncAction <- athletModel
                   } {
                val (athlet, vorschlag) = syncAction match {
                  case AddRegistration(programId, athlet, suggestion) => (athlet, suggestion)
                  case MoveRegistration(fromProgramId, toProgramid, athlet, suggestion) => (athlet, suggestion)
                  case RemoveRegistration(programId, athlet, suggestion) => (athlet, suggestion)
                }
                val matches = searchQuery.forall { search =>
                  if (search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if (athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if (vorschlag.verein match {
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
              val restored = athletTable.sortOrder ++= sortOrder
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
                processSync(wkInfo, service, selectedAthleten)
                reloader()
              }
            }
          }, new Button("OK Alle") {
            onAction = (event: ActionEvent) => {
              processSync(wkInfo, service, filteredModel)
              reloader()
            }
          })
        } else {
          PageDisplayer.showErrorDialog("Keine neuen Daten zum verarbeiten.")
        }
      }
    }
  }

  def processSync(wkInfo: WettkampfInfo, service: RegistrationRoutes, selectedAthleten: ObservableBuffer[SyncAction]) = {
    for ((progId, athletes) <- selectedAthleten.flatMap {
      case AddRegistration(programId, importathlet, candidateView) =>
        Some(mapAddRegistration(service, programId, importathlet, candidateView))
      case _ => None
    }.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))) {
      service.assignAthletsToWettkampf(wkInfo.wettkampf.id, Set(progId), athletes.toSet)
    }

    for (moveRegistration: MoveRegistration <- selectedAthleten.flatMap {
      case mr: MoveRegistration => Some(mr)
      case _ => None
    }) {
      service.moveToProgram(wkInfo.wettkampf.id, moveRegistration.toProgramid, moveRegistration.suggestion)
    }

    for (wertungenIds: Set[Long] <- selectedAthleten.flatMap {
      case rr: RemoveRegistration => Some(service.listAthletWertungenZuWettkampf(rr.suggestion.id, wkInfo.wettkampf.id).map(_.id).toSet)
      case _ => None
    }) {
      service.unassignAthletFromWettkampf(wertungenIds)
    }
  }

  private def mapAddRegistration(service: RegistrationRoutes, programId: Long, importathlet: Athlet, candidateView: AthletView) = {
    val id = if (candidateView.id > 0 &&
      (importathlet.gebdat match {
        case Some(d) =>
          candidateView.gebdat match {
            case Some(cd) => f"${cd}%tF".endsWith("-01-01")
            case _ => true
          }
        case _ => false
      })) {
      val athlet = service.insertAthlete(Athlet(
        id = candidateView.id,
        js_id = candidateView.js_id,
        geschlecht = candidateView.geschlecht,
        name = candidateView.name,
        vorname = candidateView.vorname,
        gebdat = importathlet.gebdat,
        strasse = candidateView.strasse,
        plz = candidateView.plz,
        ort = candidateView.ort,
        verein = candidateView.verein.map(_.id),
        activ = true
      ))
      athlet.id
    }
    else if (candidateView.id > 0) {
      candidateView.id
    }
    else {
      val athlet = service.insertAthlete(Athlet(
        id = 0,
        js_id = candidateView.js_id,
        geschlecht = candidateView.geschlecht,
        name = candidateView.name,
        vorname = candidateView.vorname,
        gebdat = candidateView.gebdat,
        strasse = candidateView.strasse,
        plz = candidateView.plz,
        ort = candidateView.ort,
        verein = candidateView.verein.map(_.id),
        activ = true
      ))
      // TODO update athletregistration with athlet_id or better do this operation remote and download athlet
      athlet.id
    }
    (programId, id)
  }
}
