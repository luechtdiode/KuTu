package ch.seidel.kutu.view

import akka.http.javadsl.model.HttpResponse
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain.{AddRegistration, AddVereinAction, Athlet, AthletRegistration, AthletView, EmptyAthletRegistration, MatchCode, MoveRegistration, Registration, RemoveRegistration, SyncAction, Verein, WertungView}
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
  type RegTuple = (Registration, AthletRegistration, Athlet, AthletView)

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

  // TODO Sync with existing assignments
  def doSyncUnassignedClubRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes)(registrations: List[RegTuple]): Vector[SyncAction] = {
    val relevantClubs = registrations.map(_._1).toSet
    val existingPgmAthletes: Map[Long, List[Long]] = registrations.filter(!_._2.isEmptyRegistration).groupBy(_._2.programId).map(group => (group._1 -> group._2.map(_._4.id)))
    val existingAthletes: Set[Long] = registrations.map(_._4.id).filter(_ > 0).toSet

    val addClubActions = registrations.filter(r => r._1.vereinId == None).map(r => AddVereinAction(r._1)).toSet.toVector

    val nonmatching: Map[String, Seq[(Registration, WertungView)]] = service.
      selectWertungen(wkuuid = wkInfo.wettkampf.uuid)
      .map { wertung: WertungView =>
        (relevantClubs.find(club => wertung.athlet.verein.map(_.id) == club.vereinId), wertung)
      }
      .filter {_._1 != None}
      .map(t => (t._1.get, t._2))
      .groupBy { t =>
        val (verein, wertung: WertungView) = t
        if (existingAthletes.contains(wertung.athlet.id)) {
          (existingPgmAthletes.get(wertung.wettkampfdisziplin.programm.id) match {
            case Some(athletList: List[Long]) if athletList.contains(wertung.athlet.id) => "Unverändert"
            case _ => "Umteilen"
          })
        } else {
          "Entfernen"
        }
      }

    val removeActions: Seq[SyncAction] = nonmatching.get("Entfernen") match {
      case Some(list) => list
        .map(t => RemoveRegistration(t._1, t._2.wettkampfdisziplin.programm.id, t._2.athlet.toAthlet, t._2.athlet))
        .toSet.toVector
      case _ => Vector()
    }

    val mutationActions: Seq[SyncAction] = registrations.filter(r => nonmatching.get("Unverändert") match {
      case None => true
      case Some(list) => !list.exists(p => p._2.athlet.id == r._4.id)
    }).flatMap { r =>
      (nonmatching.get("Umteilen") match {
        case Some(list) => list.find(p => p._2.athlet.id == r._4.id) match {
          case Some(t) => Some(MoveRegistration(t._1, t._2.wettkampfdisziplin.programm.id, r._2.programId, r._3, r._4))
          case _ => None
        }
        case _ => None

      }) match {
        case Some(moveRegistration) => Some(moveRegistration)
        case None if (!r._2.isEmptyRegistration) => Some(AddRegistration(r._1, r._2.programId, r._3, r._4))
        case None => None
      }
    }

    addClubActions ++ removeActions ++ mutationActions
  }

  def computeSyncActions(wkInfo: WettkampfInfo, service: RegistrationRoutes): Future[Vector[SyncAction]] = {
    import scala.concurrent.ExecutionContext.Implicits._
    Future {
      val vereineList = service.selectVereine
      val cache = new java.util.ArrayList[MatchCode]()

      doSyncUnassignedClubRegistrations(wkInfo, service)((for {
        (verein, athleten) <- service.getAllRegistrations(wkInfo.wettkampf.toWettkampf)
        resolvedVerein <- vereineList.find(v => v.name.equals(verein.vereinname) && (v.verband.isEmpty || v.verband.get.equals(verein.verband))) match {
          case Some(v) => List(verein.copy(vereinId = Some(v.id)))
          case None => List(verein)
        }
        athlet <- (athleten :+ EmptyAthletRegistration(verein.id))
      } yield {
        val parsed = athlet.toAthlet.copy(verein = resolvedVerein.vereinId)
        val candidate = if (athlet.isEmptyRegistration) parsed else service.findAthleteLike(cache)(parsed)
        (resolvedVerein, athlet, parsed, AthletView(
          candidate.id, candidate.js_id,
          candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
          candidate.strasse, candidate.plz, candidate.ort,
          vereineList.find(v => resolvedVerein.vereinId.contains(v.id)), true))
      }).toList)
    }
  }

  def importRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes, reloader: Boolean => Unit)(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}
    val athletModel = ObservableBuffer[SyncAction]()
    val vereineList = service.selectVereine
    val programms = wkInfo.leafprograms
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf: Future[Vector[SyncAction]] = KuTuApp.invokeAsyncWithBusyIndicator {
      service.loginWithWettkampf(wkInfo.wettkampf.toWettkampf).map {
        case r: HttpResponse if r.status.isSuccess() =>
          (for {
            (verein, athleten) <- service.getAllRegistrations(wkInfo.wettkampf.toWettkampf)
            resolvedVerein <- vereineList.find(v => v.name.equals(verein.vereinname) && (v.verband.isEmpty || v.verband.get.equals(verein.verband))) match {
              case Some(v) => List(verein.copy(vereinId = Some(v.id)))
              case None => List(verein.copy(vereinId = importVereinRegistration(service, verein, reloader).map(_.id).headOption))
            }
            athlet <- (athleten :+ EmptyAthletRegistration(verein.id))
          } yield {
            val parsed = athlet.toAthlet.copy(verein = resolvedVerein.vereinId)
            val candidate = if (athlet.isEmptyRegistration) parsed else service.findAthleteLike(cache)(parsed)
            (resolvedVerein, athlet, parsed, AthletView(
              candidate.id, candidate.js_id,
              candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
              candidate.strasse, candidate.plz, candidate.ort,
              vereineList.find(v => resolvedVerein.vereinId.contains(v.id)), true))
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
                      case AddVereinAction(verein) => s"${verein.vereinname}"
                      case AddRegistration(reg, programId, athlet, suggestion) =>
                        s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.map(_.verband).getOrElse(reg.verband)})"
                      case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) =>
                        s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.map(_.verband).getOrElse(reg.verband)})"
                      case RemoveRegistration(reg, programId, athlet, suggestion) =>
                        s"${suggestion.verein.map(_.name).getOrElse(reg.vereinname)} (${suggestion.verein.map(_.verband).getOrElse(reg.verband)})"
                    }
                  })
                }
                minWidth = 200
              }, new TableColumn[SyncAction, String] {
                text = "Athlet"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    val athlet = x.value match {
                      case AddVereinAction(verein) => Athlet()
                      case AddRegistration(reg, programId, athlet, suggestion) => athlet
                      case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) => athlet
                      case RemoveRegistration(reg, programId, athlet, suggestion) => athlet
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
                      case AddVereinAction(verein) => 0L
                      case AddRegistration(reg, programId, athlet, suggestion) => programId
                      case MoveRegistration(reg, fromProgramId, toProgramid, athlet, suggestion) => toProgramid
                      case RemoveRegistration(reg, programId, athlet, suggestion) => programId
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
            text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
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
                reloader(false)
              }
            }
          }, new Button("OK Alle") {
            onAction = (event: ActionEvent) => {
              processSync(wkInfo, service, filteredModel)
              reloader(false)
            }
          })
        } else {
          PageDisplayer.showErrorDialog("Keine neuen Daten zum verarbeiten.")
        }
      }
    }
  }

  def processSync(wkInfo: WettkampfInfo, service: RegistrationRoutes, selectedAthleten: ObservableBuffer[SyncAction]) = {
    val newClubs = for (addVereinAction: AddVereinAction <- selectedAthleten.flatMap {
      case av: AddVereinAction => Some(av)
      case _ => None
    }) yield {
      service.insertVerein(addVereinAction.verein.toVerein)
    }

    for ((progId, athletes) <- selectedAthleten.flatMap {
      case AddRegistration(reg, programId, importathlet, candidateView) =>
        if (candidateView.verein == None) {
          newClubs.find(c => c.name.equals(reg.vereinname) && c.verband.getOrElse("").equals(reg.verband)) match {
            case Some(verein) =>
              Some(mapAddRegistration(service, programId, importathlet, candidateView.copy(verein = Some(verein))))
            case None =>
              None
          }
        } else {
          Some(mapAddRegistration(service, programId, importathlet, candidateView))
        }

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
