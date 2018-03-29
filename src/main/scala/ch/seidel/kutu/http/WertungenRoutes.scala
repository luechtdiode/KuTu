package ch.seidel.kutu.http

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.{ ActorRef, ActorSystem, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._

import spray.json._

import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.RiegenBuilder
import ch.seidel.kutu.view.WertungEditor
import ch.seidel.kutu.akka.UpdateAthletWertung
import ch.seidel.kutu.akka.CompetitionCoordinatorClientActor
import ch.seidel.kutu.akka.WertungContainer
import scala.concurrent.Await
import java.util.UUID
import scala.util.Success
import ch.seidel.kutu.akka.AthletWertungUpdated
import scala.util.Failure
import ch.seidel.kutu.akka.FinishDurchgangStation

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import slick.jdbc.SQLiteProfile
  import slick.jdbc.SQLiteProfile.api._
  
  import DefaultJsonProtocol._
  import Core._
  
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val wertungenRoutes: Route = {
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
              handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(UUID.randomUUID().toString, competitionId.toString, Some(durchgang)))
            } else {
              complete(StatusCodes.Unauthorized)
            }
          }          
        } ~
        authenticated(true) { id =>
         if (id == competitionId.toString) {
            handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(UUID.randomUUID().toString, competitionId.toString, Some(durchgang)))
          } else {
            complete(StatusCodes.Unauthorized)
          }
        } ~
        pathEnd {
          handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSource(UUID.randomUUID().toString, competitionId.toString, Some(durchgang)))          
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
      path(Segments) {segments => 
        get {
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
                      gid)))
              }
            }
          }
        } ~
        put {
          authenticated() { userId =>
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
                            halt-1))).headOption
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
                        case a @ AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet) =>
                          val verein: String = athlet.verein.map(_.name).getOrElse("")
                          WertungContainer(athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, verein,
                              verifiedWertung, geraet)
                        case _ => StatusCodes.Conflict
                      }
                      case Failure(error) => StatusCodes.Conflict
                    })
                    
                  case _ => 
                    complete(StatusCodes.Conflict)
                }
              }
            }
          }
        }
      }
    }
  }
}
