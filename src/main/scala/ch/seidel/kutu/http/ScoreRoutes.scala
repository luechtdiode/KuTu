package ch.seidel.kutu.http

import java.io.File

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.data.ByDisziplin
import ch.seidel.kutu.data.ByGeschlecht
import ch.seidel.kutu.data.ByJahr
import ch.seidel.kutu.data.ByJahrgang
import ch.seidel.kutu.data.ByProgramm
import ch.seidel.kutu.data.ByRiege
import ch.seidel.kutu.data.ByVerband
import ch.seidel.kutu.data.ByVerein
import ch.seidel.kutu.data.ByWettkampfProgramm
import ch.seidel.kutu.data.FilterBy
import ch.seidel.kutu.data.GroupBy
import ch.seidel.kutu.domain.KutuService
import ch.seidel.kutu.renderer.ScoreToJsonRenderer
import spray.json.enrichString

trait ScoreRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.DefaultJsonProtocol._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val scoresRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("scores") {
        path("grouper") {
          get {
            complete{ Future { 
              List(ByWettkampfProgramm(), ByProgramm(), 
                  ByJahrgang, ByGeschlecht, ByVerband, ByVerein, 
                  ByRiege, ByDisziplin, ByJahr).map(_.groupname)
            }}
          }
        } ~
        pathPrefix(JavaUUID) { competitionId =>
          val data = selectWertungen(wkuuid = Some(competitionId.toString))
          val diszMap = data.groupBy { x => x.wettkampf.programmId }.map{ x =>
            x._1 -> Map(
                  "W" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("W"))
                , "M" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("M")))
            }              
          val logofile = new File(competitionId.toString);
          val programmText = data.head.wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          val groupers: List[FilterBy] = {
            List(ByWettkampfProgramm(programmText), ByProgramm(programmText), 
                ByJahrgang, ByGeschlecht, ByVerband, ByVerein, 
                ByRiege, ByDisziplin, ByJahr)
          }
          pathEnd {
            get {
              parameters('groupby.?, 'filter.?) { (groupby, filter) =>
                complete(Future{
                  val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
                    groupers.find(grouper => grouper.groupname.equals(groupername))
                  }.filter{case Some(_) => true case None => false}.map(_.get)
                  
                  val query = if (cblist.nonEmpty) {
                    cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
                  } else {
                    ByWettkampfProgramm().groupBy(ByGeschlecht)
                  }
                  ScoreToJsonRenderer.toJson(data.head.wettkampf.easyprint, query.select(data).toList, false, diszMap, logofile).parseJson
                })
              }
            }
          } ~
          path("grouper") {
            get {
              complete{ Future { 
                groupers.map(_.groupname)
              }}
            }
          }
        }
      }
    }
  }
}
