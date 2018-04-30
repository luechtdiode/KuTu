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
import akka.http.scaladsl.model.ContentTypes
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer
import akka.http.scaladsl.model.HttpEntity
import ch.seidel.kutu.domain.encodeURIParam

trait ScoreRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.DefaultJsonProtocol._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val scoresRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("scores") {
        pathEnd {
          get {
            complete(
              listWettkaempfeAsync.map{competitions =>
                HttpEntity(ContentTypes.`text/html(UTF-8)`,
                    competitions
                    .map(comp => (s"<li><a href='/api/scores/${comp.uuid.get.toString}?html'>${comp.easyprint}</a></li>"))
                    .mkString("<html><body>\n", "\n", "</body></html>")
                )
              }
            )
          }
        } ~
        path("grouper") {
          get {
            complete{ Future { 
              List(ByWettkampfProgramm(), ByProgramm(), 
                  ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), 
                  ByRiege(), ByDisziplin(), ByJahr()).map(_.groupname)
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
                ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), 
                ByRiege(), ByDisziplin(), ByJahr())
          }
          pathEnd {
            get {
              parameters('groupby.?, 'filter.*, 'html.?) { (groupby, filter, html) =>
                complete(Future{
                  val filterList = filter.map{flt =>
                    val keyvalues = flt.split(":")
                    val key = keyvalues(0)
                    val values = keyvalues(1).split("!").map(encodeURIParam(_))
                    (key -> values.toSet)
                  }.toMap
                  
                  val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
                    val grouper = groupers.find(grouper => grouper.groupname.equals(groupername))
                    grouper
                  }.filter{case Some(_) => true case None => false}.map(_.get)
                  
                  cblist.foreach{gr =>
                    gr.reset
                    filterList.get(gr.groupname) match {
                      case Some(filterValues) =>
                        gr.setFilter(gr.analyze(data).filter(f => filterValues.contains(encodeURIParam(f.easyprint))).toSet)
                      case _ =>
                    }
                  }
                  
                  val query = if (cblist.nonEmpty) {
                    cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
                  } else {
                    ByWettkampfProgramm().groupBy(ByGeschlecht())
                  }
                  if (html.nonEmpty) {
                    HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer(){override val title = data.head.wettkampf.easyprint}
                    .toHTML(query.select(data).toList, 0, false, diszMap, logofile))
                  } else {
                    HttpEntity(ContentTypes.`application/json`,  ScoreToJsonRenderer
                    .toJson(data.head.wettkampf.easyprint, query.select(data).toList, false, diszMap, logofile))
                  }
//                  ScoreToJsonRenderer
//                    .toJson(data.head.wettkampf.easyprint, query.select(data).toList, false, diszMap, logofile).parseJson
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
          } ~
          path("filter") {
            get {
              parameters('groupby.?) { (groupby) =>
                complete{ Future {
                  val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
                    groupers.find(grouper => grouper.groupname.equals(groupername))
                  }.filter{case Some(_) => true case None => false}.map(_.get)
                  cblist.foreach(_.reset)
                  val query = if (cblist.nonEmpty) {
                    cblist
                  } else {
                    List(ByWettkampfProgramm(),ByGeschlecht())
                  }
                  query.map(g => s"${g.groupname}:${g.analyze(data).map(x => encodeURIParam(x.easyprint)).mkString("!")}")
                }}
              }
            }
          }
        }
      }
    }
  }
}
