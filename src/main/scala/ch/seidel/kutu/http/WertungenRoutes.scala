package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain.{Disziplin, GeraeteRiege, KutuService, ProgrammRaw, Wertung, encodeURIComponent, str2Int, str2Long}
import ch.seidel.kutu.renderer.RiegenBuilder

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val wertungenRoutes: Route = {
    extractClientIP { ip =>
    pathPrefix("programm") {
      pathEnd {
        get {
          complete{ Future { 
            listRootProgramme().map(x => ProgrammRaw(x.id, x.name, x.aggregate, x.parent.map(_.id).getOrElse(0), x.ord, x.alterVon, x.alterBis)) 
          } }          
        }
      }
    } ~
    pathPrefix("durchgang" / JavaUUID) { competitionId =>
      pathEnd {
        get {
          complete { Future {
            RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
              .filter(gr => gr.durchgang.nonEmpty) 
              .map(gr => gr.durchgang.get)
              .toSet.toList.sorted
            }
          }
        }
      } ~
      path(Segment / "ws") { durchgang =>
        parameters('jwt.as[String]) { (jwt) =>
          authenticateWith(Some(jwt), true) { id =>
            if (id == competitionId.toString) {
              if (durchgang.equalsIgnoreCase("all")) {
                handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(makeDeviceId(ip), competitionId.toString, None))
              } else {
                handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(makeDeviceId(ip), competitionId.toString, Some(durchgang)))
              }
            } else {
              complete(StatusCodes.Unauthorized)
            }
          }          
        } ~
        authenticated(true) { id =>
         if (id == competitionId.toString) {
            handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(makeDeviceId(ip), competitionId.toString, Some(durchgang)))
          } else {
            complete(StatusCodes.Unauthorized)
          }
        } ~
        pathEnd {
          handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(makeDeviceId(ip), competitionId.toString, Some(durchgang)))          
        }
      } ~
      path("finish") { 
        post {
          authenticated() { userId =>
            entity(as[String]) { fd =>
              complete(CompetitionCoordinatorClientActor.publish(fd.asType[FinishDurchgangStation]))
            }
          }
        }
      } ~
      path("geraete") { 
        get {
           complete { Future {
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
           complete { Future {
             RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
              .filter(gr => gr.disziplin.nonEmpty) 
              .map(gr => gr.disziplin.get)
              .foldLeft(List[Disziplin]())((acc, geraet) => if (acc.contains(geraet)) acc else acc :+ geraet)
            }
          }
        }
      } ~
      path(Segments) {segments => 
        get {
          val wkPgmId = readWettkampf(competitionId.toString()).programmId
          val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
          // Durchgang/Geraet/Step
          segments match { 
            case List(durchgang) => complete { Future {
              RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                .filter(gr => gr.durchgang.exists(encodeURIComponent(_) == durchgang) && gr.disziplin.nonEmpty) 
                .map(gr => gr.disziplin.get)
                .foldLeft(List[Disziplin]())((acc, geraet) => if (acc.contains(geraet)) acc else acc :+ geraet)
              }
            }
            case List(durchgang, geraet) => complete { Future {
              val gid: Long = geraet
              RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                .filter(gr => gr.durchgang.exists(encodeURIComponent(_) == durchgang)  && gr.disziplin.exists(_.id == gid))
                .map(gr => gr.halt + 1)
                .toSet.toList.sorted
              }
            }
            case List(durchgang, geraet, step) => complete { Future {
              val halt: Int = step
              val gid: Long = geraet
              RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                .filter(gr => 
                    gr.durchgang.exists(encodeURIComponent(_) == durchgang) && 
                    gr.disziplin.exists(_.id == gid) && 
                    gr.halt == halt -1)
                .flatMap(gr => gr.kandidaten.map(k => 
                  WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein, 
                      k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).map(_.toWertung).head, 
                      gid, k.programm, isDNoteUsed)))
              }
            }
          }
        } ~
        put {
          authenticated() { userId =>
            if (userId.equals(competitionId.toString())) {              
              entity(as[Wertung]) { wertung =>
                segments match { 
                  case List(durchgang, geraet, step) => onComplete{ Future {
                    val halt: Int = step
                    val gid: Long = geraet
                    val gerateRiegen = RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                    val wkid = gerateRiegen.head.kandidaten.head.wertungen.head.wettkampf.id
                    def filter(gr: GeraeteRiege): Boolean = {
                      gr.durchgang.exists(encodeURIComponent(_) == durchgang) && 
                      gr.disziplin.exists(_.id == gid) && 
                      gr.halt == halt -1                    
                    }
                    
                    if (wertung.wettkampfId == wkid) {
                      gerateRiegen.filter(filter).flatMap(gr => gr.kandidaten
                          .filter(k => k.id == wertung.athletId)
                          .map(k => UpdateAthletWertung(
                              loadAthleteView(k.id), 
                              k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid && w.id == wertung.id).map(_.toWertung.updatedWertung(wertung)).head, 
                              competitionId.toString, 
                              gr.durchgang.get, 
                              gid, 
                              halt-1, k.programm))).headOption
                    } else {
                      None
                    }
                  }} {
                    case Success(Some(wertung)) => 
  //                  RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
  //                    .filter(gr => 
  //                        gr.durchgang.exists(encodeURIComponent(_) == durchgang) && 
  //                        gr.disziplin.exists(_.id == gid) && 
  //                        gr.halt == halt -1)
  //                    .flatMap(gr => gr.kandidaten.map(k => WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein, 
  //                      k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).map(_.toWertung).head, 
  //                      gid)))                  
                      complete(CompetitionCoordinatorClientActor.publish(wertung).andThen{
                        case Success(w) => w match {
                          case a @ AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet, programm) =>
                            val verein: String = athlet.verein.map(_.name).getOrElse("")
                            val wkPgmId = readWettkampf(competitionId.toString()).programmId
                            val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
                            WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                                verifiedWertung, geraet, programm, isDNoteUsed)
                          case _ => StatusCodes.Conflict
                        }
                        case Failure(error) => StatusCodes.Conflict
                      })
                      
                    case _ => 
                      complete(StatusCodes.Conflict)
                  }
                }
              }
            } else {
              complete(StatusCodes.Unauthorized)
            }
          }
        }
      }
    }
  }
  }
}
