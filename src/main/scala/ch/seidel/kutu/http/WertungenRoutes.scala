package ch.seidel.kutu.http

import ch.seidel.kutu.actors.*
import ch.seidel.kutu.domain.*
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable
import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with CIDSupport {

  import ch.seidel.kutu.domain.{given_Conversion_String_Int, given_Conversion_String_Long}
  import spray.json.*
  import spray.json.DefaultJsonProtocol.*

  import scala.concurrent.ExecutionContext.Implicits.global

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  import AbuseHandler.*

  lazy val wertungenRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      log.debug(s"$clientId is calling $uri")
      pathPrefixLabeled("programm", "programm") {
        pathEnd {
          get {
              complete {
                Future {
                listRootProgramme().map(x => ProgrammRaw(x.id, x.name, x.aggregate, x.parent.map(_.id).getOrElse(0), x.ord, x.alterVon, x.alterBis, x.uuid, x.riegenmode, x.bestOfCount)).toJson
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
                  val wettkampf = readWettkampf(competitionId.toString)
                  val wertungen = Kandidat.mapToBestOfCounting(selectWertungen(wettkampfId = Some(wettkampf.id), athletId = Some(athletId)))
                  wertungen.filter { wertung =>
                    if wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M") then {
                      false
                    }
                    else if wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M") then {
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
                      w.wettkampfdisziplin.disziplin.id, w.wettkampfdisziplin.programm.name, "", w.wettkampfdisziplin.isDNoteUsed, w.isStroked)
                  }.toJson
                }
              }
            }
          }
        } ~
        pathPrefixLabeled("music" / JavaUUID, "music/:competition-id") { competitionId =>
          pathLabeled("aquire", "aquire") {
            put {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  entity(as[Wertung]) { wertung =>
                    getCurrentWertung(wertung) match {
                      case None => None
                      case Some(currentWertung) =>
                        CompetitionCoordinatorClientActor.publish(AthletMediaAquire(userId, currentWertung.athlet, currentWertung.toWertung), clientId)
                    }
                    complete(StatusCodes.OK)
                  }
                } else {
                  log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated update of wertung")
                  complete(StatusCodes.Unauthorized)
                }
              }
            }
          } ~
          pathLabeled("release", "release") {
            put {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  entity(as[Wertung]) { wertung =>
                    getCurrentWertung(wertung) match {
                      case None => None
                      case Some(currentWertung) =>
                        CompetitionCoordinatorClientActor.publish(AthletMediaRelease(userId, currentWertung.athlet, currentWertung.toWertung), clientId)
                    }
                    complete(StatusCodes.OK)
                  }
                } else {
                  log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated media release command")
                  complete(StatusCodes.Unauthorized)
                }
              }
            }
          } ~
          pathLabeled("start", "start") {
            put {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  entity(as[Wertung]) { wertung =>
                    getCurrentWertung(wertung) match {
                      case None => None
                      case Some(currentWertung) =>
                        CompetitionCoordinatorClientActor.publish(AthletMediaStart(userId, currentWertung.athlet, currentWertung.toWertung), clientId)
                    }
                    complete(StatusCodes.OK)
                  }
                } else {
                  log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated update of wertung")
                  complete(StatusCodes.Unauthorized)
                }
              }
            }
          } ~
          pathLabeled("stop", "stop") {
            put {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  entity(as[Wertung]) { wertung =>
                    getCurrentWertung(wertung) match {
                      case None => None
                      case Some(currentWertung) =>
                        CompetitionCoordinatorClientActor.publish(AthletMediaToStart(userId, currentWertung.athlet, currentWertung.toWertung), clientId)
                    }
                    complete(StatusCodes.OK)
                  }
                } else {
                  log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated update of wertung")
                  complete(StatusCodes.Unauthorized)
                }
              }
            }
          }
        } ~
        pathPrefixLabeled("durchgang" / JavaUUID, "durchgang/:competition-id") { competitionId =>
          if !wettkampfExists(competitionId.toString) then {
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
                      .distinct.toJson
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
                  if id == competitionId.toString then {
                    if durchgang.equalsIgnoreCase("all") then {
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
                  if id == competitionId.toString then {
                    handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, competitionId.toString, Some(durchgang), lastSequenceIdOption))
                  } else {
                    complete(StatusCodes.Unauthorized)
                  }
                } ~
                pathEnd {
                  if durchgang.equalsIgnoreCase("all") then {
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
                if !wettkampfExists(competitionId.toString) then {
                  complete(StatusCodes.NotFound)
                } else
                  complete {
                    listDisziplinZuWettkampf(readWettkampf(competitionId.toString)).map(_.toList).map(items => items.toJson(using listFormat(using disziplinFormat)))
                  }
              }
            } ~
            pathLabeled("validate", "validate") {
              put {
                authenticated() { userId =>
                  if userId.equals(competitionId.toString) then {
                    entity(as[Wertung]) { wertung =>
                      try {
                        val w = getCurrentWertung(wertung) match {
                          case None => validateWertung(wertung)
                          case Some(currentWertung) => validateWertung(currentWertung.toWertung.updatedWertung(wertung))
                        }
                        complete(w.toJson)
                      } catch {
                        case e: IllegalArgumentException =>
                          complete(MessageAck(e.getMessage).toJson)
                      }
                    }
                  } else {
                    log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated update of wertung")
                    complete(StatusCodes.Unauthorized)
                  }
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
                          //.filter(d => !d.isPause)
                          //.map(_.harmless)
                          .distinct
                          .toJson
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
                          .toJson
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
                            gr.disziplin.exists(_.id == gid ) &&
                            gr.halt == halt - 1)
                          .flatMap(gr => gr.kandidaten.map(k => {
                            k.markedWertungen.find(w => w.wettkampfdisziplin.disziplin.id == gid) match {
                              case Some(wertungView: WertungView) =>
                                WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein,
                                  wertungView.toWertung,
                                  gid, k.programm, gr.durchgang.getOrElse(""), wertungView.wettkampfdisziplin.isDNoteUsed, wertungView.isStroked)
                              case None =>
                                val wertungView: WertungView = k.markedWertungen.head
                                WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein,
                                  wertungView.toWertung.copy(wettkampfdisziplinId = 0L),
                                  gid, k.programm, gr.durchgang.getOrElse(""), wertungView.wettkampfdisziplin.isDNoteUsed, wertungView.isStroked)
                            }
                          })).toJson
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
                    if userId.equals(competitionId.toString) then {
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
                                val isDNoteUsed = currentWertung.wettkampfdisziplin.isDNoteUsed
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
                                case AthletWertungUpdatedSequenced(athlet, verifiedWertung, _, durchgang, ger, programm, _) =>
                                  val verein: String = athlet.verein.map(_.name).getOrElse("")
                                  WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                    verifiedWertung, ger, programm, durchgang, isDNoteUsed, isStroked = false)

                                case AthletWertungUpdated(athlet, verifiedWertung, _, durchgang, ger, programm) =>
                                  val verein: String = athlet.verein.map(_.name).getOrElse("")
                                  WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                    verifiedWertung, ger, programm, durchgang, isDNoteUsed, isStroked = false)

                                case unexpectedEvent: KutuAppEvent => unexpectedEvent
                              }
                              complete(toResponseMarshallable.andThen {
                                case Success(w) => w
                                case Failure(error) =>
                                  log.error(s"[$clientId/$userId] Conflict: Error publishing Wertung: $wertung", error)
                                  StatusCodes.Conflict
                              })

                            case x =>
                              log.error(s"[$clientId/$userId] Conflict: unexpected Result: $x, Non-matching wertung: $wertung")
                              complete(StatusCodes.Conflict)
                          }
                          case _ => complete(StatusCodes.Conflict)
                        }
                      }
                    } else {
                      log.error(s"[$clientId/$userId] Unauthorized: Reject unauthenticated update of wertung")
                      complete(StatusCodes.Unauthorized)
                    }
                  }
                }
            }
        }
    }
  }
}
