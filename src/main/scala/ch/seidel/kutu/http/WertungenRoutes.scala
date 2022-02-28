package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain.{KutuService, ProgrammRaw, Wertung, encodeURIComponent, str2Int, str2Long}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with CIDSupport {

  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  import AbuseHandler._

  lazy val wertungenRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      log.debug(s"$clientId is calling $uri")
      pathPrefixLabeled("programm", "programm") {
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
        pathPrefixLabeled("athlet" / JavaUUID / LongNumber, "athlet/:competition-id/:athlet-id") { (competitionId, athletId) =>
          pathEnd {
            get {
              complete {
                Future {
                  val wettkampf = readWettkampf(competitionId.toString())
                  val wertungen = selectWertungen(wettkampfId = Some(wettkampf.id), athletId = Some(athletId))
                  wertungen.filter { wertung =>
                    if (wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
                      false
                    }
                    else if (wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
                      false
                    }
                    else {
                      true
                    }
                  }.map { w =>
                    WertungContainer(
                      w.athlet.id, w.athlet.vorname, w.athlet.name, w.athlet.geschlecht,
                      w.athlet.verein.map(_.easyprint).getOrElse(""),
                      w.toWertung,
                      w.wettkampfdisziplin.disziplin.id, w.wettkampfdisziplin.programm.name, w.wettkampfdisziplin.notenSpez.isDNoteUsed)
                  }
                }
              }
            }
          }
        } ~
        pathPrefixLabeled("durchgang" / JavaUUID, "durchgang/:competition-id") { competitionId =>
          if (!wettkampfExists(competitionId.toString)) {
            log.error(handleAbuse(clientId, uri))
            complete(StatusCodes.NotFound)
          } else
          pathEnd {
            get {
              complete {
                val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                  case GeraeteRiegeList(list, _) =>
                    list
                      .flatMap(gr => gr.durchgang)
                      .distinct
                  case _ =>
                    StatusCodes.Conflict
                }
                toResponseMarshallable
              }
            }
          } ~
            (pathLabeled(Segment / "ws", ":durchgang/ws") & parameters(Symbol("lastSequenceId").?)) { (durchgang: String, lastSequenceId: Option[String]) =>
              val lastSequenceIdOption: Option[Long] = lastSequenceId.map(str2Long)
              parameters(Symbol("jwt").as[String]) { jwt =>
                authenticateWith(Some(jwt), true) { id =>
                  if (id == competitionId.toString) {
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
                  if (id == competitionId.toString) {
                    handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, competitionId.toString, Some(durchgang), lastSequenceIdOption))
                  } else {
                    complete(StatusCodes.Unauthorized)
                  }
                } ~
                pathEnd {
                  if (durchgang.equalsIgnoreCase("all")) {
                    handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(clientId, competitionId.toString, None, lastSequenceIdOption))
                  } else {
                    handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(clientId, competitionId.toString, Some(durchgang)))
                  }
                }
            } ~
            pathLabeled("finish", "finish") {
              post {
                authenticated() { userId =>
                  entity(as[String]) { fd =>
                    complete(CompetitionCoordinatorClientActor.publish(fd.asType[FinishDurchgangStation], clientId))
                  }
                }
              }
            } ~
            pathLabeled("geraete", "geraete") {
              get {
                if (!wettkampfExists(competitionId.toString)) {
                  complete(StatusCodes.NotFound)
                } else
                  complete {
                    listDisziplinZuWettkampf(readWettkampf(competitionId.toString()))
                  }
              }
            } ~
            pathLabeled(Segments, ":durchgang/:geraet/:step") { segments =>
              get {
                // Durchgang/Geraet/Step
                segments match {
                  case List(durchgang) => complete {
                    val dg = encodeURIComponent(durchgang)
                    val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                    val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                      case GeraeteRiegeList(list, _) =>
                        list
                          .filter(_.durchgang.exists(encodeURIComponent(_) == dg))
                          .flatMap(gr => gr.disziplin)
                          .distinct
                      case _ =>
                        StatusCodes.Conflict
                    }
                    toResponseMarshallable
                  }
                  case List(durchgang, geraet) => complete {
                    val dg = encodeURIComponent(durchgang)
                    val gid: Long = geraet
                    val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                    val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                      case GeraeteRiegeList(list, _) =>
                        list.filter(gr => {
                          gr.durchgang.exists(encodeURIComponent(_) == dg) && gr.disziplin.exists(_.id == gid)
                        })
                          .map(gr => gr.halt + 1)
                          .distinct.sorted
                      case _ =>
                        StatusCodes.Conflict
                    }
                    toResponseMarshallable
                  }
                  case List(durchgang, geraet, step) => complete {
                    val dg = encodeURIComponent(durchgang)
                    val halt: Int = step
                    val gid: Long = geraet
                    val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                    val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                      case GeraeteRiegeList(list, _) =>
                        list.filter(gr =>
                          gr.durchgang.exists(encodeURIComponent(_) == dg) &&
                            gr.disziplin.exists(_.id == gid) &&
                            gr.halt == halt - 1)
                          .flatMap(gr => gr.kandidaten.map(k => {
                            val wertungView = k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).head
                            WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein,
                              wertungView.toWertung,
                              gid, k.programm, wertungView.wettkampfdisziplin.notenSpez.isDNoteUsed)
                          }))
                      case _ =>
                        StatusCodes.Conflict
                    }
                    toResponseMarshallable
                  }
                  case _ => complete(StatusCodes.Conflict)
                }
              } ~
                put {
                  authenticated() { userId =>
                    if (userId.equals(competitionId.toString)) {
                      entity(as[Wertung]) { wertung =>
                        segments match {
                          case List(dg, geraet, step) => {
                            val halt: Int = step
                            val gid: Long = geraet
                            val durchgang = encodeURIComponent(dg)
                            getCurrentWertung(wertung) match {
                              case None => None
                              case Some(currentWertung) =>
                                val wkPgmId = currentWertung.wettkampfdisziplin.programm.head.id
                                val isDNoteUsed = currentWertung.wettkampfdisziplin.notenSpez.isDNoteUsed
                                val durchgangEff = selectRiegenRaw(currentWertung.wettkampf.id).filter(ds => encodeURIComponent(ds.durchgang.getOrElse("")) == durchgang)
                                  .map(_.durchgang.get)
                                  .headOption.getOrElse(durchgang)
                                val normalizedwertung = UpdateAthletWertung(
                                  loadAthleteView(wertung.athletId),
                                  currentWertung.toWertung.updatedWertung(wertung),
                                  competitionId.toString,
                                  durchgangEff,
                                  gid,
                                  halt - 1, currentWertung.wettkampfdisziplin.programm.name)
                                Some(isDNoteUsed, normalizedwertung)
                            }
                          } match {
                            case Some((isDNoteUsed, normalizedwertung)) =>
                              val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(normalizedwertung, clientId)
                              val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                                case AthletWertungUpdatedSequenced(athlet, verifiedWertung, _, _, ger, programm, _) =>
                                  val verein: String = athlet.verein.map(_.name).getOrElse("")
                                  WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                    verifiedWertung, ger, programm, isDNoteUsed)

                                case AthletWertungUpdated(athlet, verifiedWertung, _, _, ger, programm) =>
                                  val verein: String = athlet.verein.map(_.name).getOrElse("")
                                  WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                    verifiedWertung, ger, programm, isDNoteUsed)

                                case unexpectedEvent: KutuAppEvent => unexpectedEvent
                              }
                              complete(toResponseMarshallable.andThen {
                                case Success(w) => w
                                case Failure(error) =>
                                  log.error(s"Conflict: Error publishing Wertung: $wertung", error)
                                  StatusCodes.Conflict
                              })

                            case x =>
                              log.error(s"Conflict: unexpected Result: $x, Non-matching wertung: $wertung")
                              complete(StatusCodes.Conflict)
                          }
                          case _ => complete(StatusCodes.Conflict)
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
