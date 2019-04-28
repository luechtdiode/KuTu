package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain.{Disziplin, GeraeteRiege, KutuService, ProgrammRaw, Wertung, encodeURIComponent, str2Int, str2Long}
import ch.seidel.kutu.renderer.RiegenBuilder

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with CIDSupport {

  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val wertungenRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      log.debug(s"$clientId is calling $uri")
      pathPrefix("programm") {
        pathEnd {
          get {
            complete {
              Future {
                listRootProgramme().map(x => ProgrammRaw(x.id, x.name, x.aggregate, x.parent.map(_.id).getOrElse(0), x.ord, x.alterVon, x.alterBis))
              }
            }
          }
        }
      } ~
      pathPrefix("durchgang" / JavaUUID) { competitionId =>
        pathEnd {
          get {
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            } else
            complete {
              Future {
                RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                  .filter(gr => gr.durchgang.nonEmpty)
                  .map(gr => gr.durchgang.get)
                  .toSet.toList.sorted
              }
            }
          }
        } ~
        (path(Segment / "ws") & parameters('lastSequenceId.?)) { (durchgang: String, lastSequenceId: Option[String]) =>
          val lastSequenceIdOption: Option[Long] = lastSequenceId.map(str2Long)
          parameters('jwt.as[String]) { jwt =>
            authenticateWith(Some(jwt), true) { id =>
              if (!wettkampfExists(competitionId.toString)) {
                complete(StatusCodes.NotFound)
              }
              else if (id == competitionId.toString) {
                if (durchgang.equalsIgnoreCase("all")) {
                  handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, competitionId.toString, None, lastSequenceIdOption))
                } else {
                  handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, competitionId.toString, Some(durchgang), lastSequenceIdOption))
                }
              } else {
                complete(StatusCodes.Unauthorized)
              }
            }
          } ~
          authenticated(true) { id =>
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            } else if (id == competitionId.toString) {
              handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, competitionId.toString, Some(durchgang), lastSequenceIdOption))
            } else {
              complete(StatusCodes.Unauthorized)
            }
          } ~
          pathEnd {
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            } else if (durchgang.equalsIgnoreCase("all")) {
              handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(clientId, competitionId.toString, None, lastSequenceIdOption))
            } else {
              handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(clientId, competitionId.toString, Some(durchgang)))
            }
          }
        } ~
        path("finish") {
          post {
            authenticated() { userId =>
              entity(as[String]) { fd =>
                complete(CompetitionCoordinatorClientActor.publish(fd.asType[FinishDurchgangStation], clientId))
              }
            }
          }
        } ~
        path("geraete") {
          get {
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            } else
            complete {
              Future {
                RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                  .filter(gr => gr.disziplin.nonEmpty)
                  .map(gr => gr.disziplin.get)
                  .foldLeft(List[Disziplin]())((acc, geraet) => if (acc.contains(geraet)) acc else acc :+ geraet)
              }
            }
          }
        } ~
        path("diszipline") {
          get {
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            } else
            complete {
              Future {
                RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                  .filter(gr => gr.disziplin.nonEmpty)
                  .map(gr => gr.disziplin.get)
                  .foldLeft(List[Disziplin]())((acc, geraet) => if (acc.contains(geraet)) acc else acc :+ geraet)
              }
            }
          }
        } ~
        path(Segments) { segments =>
          get {
            if (!wettkampfExists(competitionId.toString)) {
              complete(StatusCodes.NotFound)
            }
            else {
              val wkPgmId = readWettkampf(competitionId.toString()).programmId
              val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
              // Durchgang/Geraet/Step
              segments match {
                case List(durchgang) => complete {
                  Future {
                    RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                      .filter(gr => gr.durchgang.exists(encodeURIComponent(_) == durchgang) && gr.disziplin.nonEmpty)
                      .map(gr => gr.disziplin.get)
                      .foldLeft(List[Disziplin]())((acc, geraet) => if (acc.contains(geraet)) acc else acc :+ geraet)
                  }
                }
                case List(durchgang, geraet) => complete {
                  Future {
                    val gid: Long = geraet
                    RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                      .filter(gr => gr.durchgang.exists(encodeURIComponent(_) == durchgang) && gr.disziplin.exists(_.id == gid))
                      .map(gr => gr.halt + 1)
                      .toSet.toList.sorted
                  }
                }
                case List(durchgang, geraet, step) => complete {
                  Future {
                    val halt: Int = step
                    val gid: Long = geraet
                    RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                      .filter(gr =>
                        gr.durchgang.exists(encodeURIComponent(_) == durchgang) &&
                          gr.disziplin.exists(_.id == gid) &&
                          gr.halt == halt - 1)
                      .flatMap(gr => gr.kandidaten.map(k =>
                        WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein,
                          k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).map(_.toWertung).head,
                          gid, k.programm, isDNoteUsed)))
                  }
                }
              }
            }
          } ~
          put {
            authenticated() { userId =>
              if (!wettkampfExists(competitionId.toString)) {
                complete(StatusCodes.NotFound)
              } else if (userId.equals(competitionId.toString())) {
                entity(as[Wertung]) { wertung =>
                  segments match {
                    case List(durchgang, geraet, step) => {
                        val halt: Int = step
                        val gid: Long = geraet
                        getCurrentWertung(wertung) match {
                          case None => None
                          case Some(currentWertung) =>
                            val durchgangEff = selectRiegenRaw(currentWertung.wettkampf.id).filter(ds => encodeURIComponent(ds.durchgang.getOrElse("")) == durchgang)
                              .map(_.durchgang.get)
                              .headOption.getOrElse(durchgang)
                            /*selectDurchgangstationen(currentWertung.wettkampf.id)
                              .filter(ds => encodeURIComponent(ds.durchgang) == durchgang)
                              .map(_.durchgang)
                              .headOption.getOrElse(durchgang)*/
                            val normalizedwertung = UpdateAthletWertung(
                              loadAthleteView(wertung.athletId),
                              currentWertung.toWertung.updatedWertung(wertung),
                              competitionId.toString,
                              durchgangEff,
                              gid,
                              halt - 1, currentWertung.wettkampfdisziplin.programm.name)
                            Some(currentWertung.wettkampfdisziplin.programm.id, normalizedwertung)
                        }
                      } match {
                      case Some((wkPgmId, normalizedwertung)) =>
                        complete(CompetitionCoordinatorClientActor.publish(normalizedwertung, clientId).andThen {
                          case Success(w) => w match {
                            case AthletWertungUpdatedSequenced(athlet, verifiedWertung, _, _, ger, programm, _) =>
                              val verein: String = athlet.verein.map(_.name).getOrElse("")
                              val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
                              WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                verifiedWertung, ger, programm, isDNoteUsed)

                            case AthletWertungUpdated(athlet, verifiedWertung, _, _, ger, programm) =>
                              val verein: String = athlet.verein.map(_.name).getOrElse("")
                              val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
                              WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                verifiedWertung, ger, programm, isDNoteUsed)

                            case _ =>
                              log.error(s"Conflict: unexpected Wertung value: $w")
                              StatusCodes.Conflict
                          }
                          case Failure(error) =>
                            log.error(s"Conflict: Error publishing Wertung: $wertung", error)
                            StatusCodes.Conflict
                        })

                      case x =>
                        log.error(s"Conflict: unexpected Result: $x, Non-matching wertung: $wertung")
                        complete(StatusCodes.Conflict)
                    }
                  }
                }
              } else {
                log.error(s"Unauthorized: Reject unauthenticated update of wertung")
                complete(StatusCodes.Unauthorized)
              }
            }
          }
        }
      }
    }
  }
}
