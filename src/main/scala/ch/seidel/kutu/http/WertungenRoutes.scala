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

trait WertungenRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with BasicAuthSupport with RouterLogging with KutuService {
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
          authenticated { userId =>
            entity(as[Wertung]) { wertung =>
              segments match { 
                case List(durchgang, geraet, step) => complete { Future {
                  val halt: Int = step
                  val gid: Long = geraet
                  val gerateRiegen = RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                  val wkid = gerateRiegen.head.kandidaten.head.wertungen.head.wettkampf.id
                  def filter(gr: GeraeteRiege): Boolean = {
                    gr.durchgang.exists(encodeURIComponent(_) == durchgang) && 
                    gr.disziplin.exists(_.id == gid) && 
                    gr.halt == halt -1                    
                  }
                  val filteredRiegen = gerateRiegen.filter(filter)
                  val wertungOriginal = filteredRiegen
                    .flatMap(gr => gr.kandidaten
                        .filter(k => k.id == wertung.athletId)
                        .map(k => UpdateAthletWertung(loadAthleteView(k.id), 
                          k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).map(_.toWertung).head, 
                          competitionId.toString, durchgang, gid))).headOption
                  val compOK = wertung.wettkampfId == wkid
                  val wertungOk = wertungOriginal.exists(wo => wo.wertung.id == wertung.id)
                  if (wertungOk && compOK) {
                    Await.result(CompetitionCoordinatorClientActor.publish(
                      UpdateAthletWertung(
                          wertungOriginal.get.ahtlet,
                          wertungOriginal.get.wertung.updatedWertung(wertung), 
                          competitionId.toString, durchgang, geraet) 
                    ), Duration.Inf)
                  }
                  RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(competitionId).toList)
                    .filter(gr => 
                        gr.durchgang.exists(encodeURIComponent(_) == durchgang) && 
                        gr.disziplin.exists(_.id == gid) && 
                        gr.halt == halt -1)
                    .flatMap(gr => gr.kandidaten.map(k => WertungContainer(k.id, k.vorname, k.name, k.geschlecht, k.verein, 
                      k.wertungen.filter(w => w.wettkampfdisziplin.disziplin.id == gid).map(_.toWertung).head, 
                      gid)))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
