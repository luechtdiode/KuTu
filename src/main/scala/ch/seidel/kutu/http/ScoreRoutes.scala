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
import spray.json._
import akka.http.scaladsl.model.ContentTypes
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer
import akka.http.scaladsl.model.HttpEntity
import ch.seidel.kutu.domain.encodeURIParam
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import ch.seidel.kutu.Config
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.domain.WertungView
import java.util.Base64
import ch.seidel.kutu.domain.Wettkampf
import ch.seidel.kutu.domain.Durchgang
import ch.seidel.kutu.data.ByDurchgang
import ch.seidel.kutu.akka.CompetitionCoordinatorClientActor
import scala.util.Success
import ch.seidel.kutu.akka.StartedDurchgaenge
import ch.seidel.kutu.akka.MessageAck
import ch.seidel.kutu.akka.ResponseMessage

trait ScoreRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.DefaultJsonProtocol._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  val allGroupers = List(
      ByWettkampfProgramm(), ByProgramm(), 
      ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), 
      ByRiege(), ByDisziplin(), ByJahr()
  )
                  
  def queryScoreResults(wettkampf: String, groupby: Option[String], filter: Iterable[String], html: Boolean, groupers: List[FilterBy], data: Seq[WertungView], logofile: File) = {
    val diszMap = data.groupBy { x => x.wettkampf.programmId }.map{ x =>
      x._1 -> Map(
            "W" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("W"))
          , "M" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("M")))
    }
    val filterList = filter.map{flt =>
      val keyvalues = flt.split(":")
      val key = keyvalues(0)
      val values = keyvalues(1).split("!")//.map(encodeURIParam(_)) // FIXME should be decode
      (key -> values.toSet)
    }.toMap
    
    val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }.filter{case Some(_) => true case None => false}.map(_.get)
    val cbllist = if(cblist.nonEmpty) cblist else Seq(ByWettkampfProgramm(), ByGeschlecht())
    
    val cbflist = filterList.keys.map{groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }
    .filter{case Some(_) => true case None => false}.map(_.get)
    .filter(grouper => !cbllist.contains(grouper)) ++ cbllist
    
    cbflist.foreach{gr =>
      gr.reset
      filterList.get(gr.groupname) match {
        case Some(filterValues) =>
          gr.setFilter(gr.analyze(data).filter{f => 
            val encFilterValueName = encodeURIParam(f.easyprint)
            filterValues.contains(encFilterValueName)
          }.toSet)
        case _ =>
      }
    }
    val query = if (cbflist.nonEmpty) {
      cbflist.foldLeft(cbflist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
    } else {      
      ByWettkampfProgramm().groupBy(ByGeschlecht())
    }
    if (html) {
      HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer(){override val title = wettkampf}
      .toHTML(query.select(data).toList, 0, false, diszMap, logofile))
    } else {
      HttpEntity(ContentTypes.`application/json`,  ScoreToJsonRenderer
      .toJson(wettkampf, query.select(data).toList, false, diszMap, logofile))
    }
  }
  
  def queryFilters(groupby: Option[String], groupers: List[FilterBy], data: Seq[WertungView]) = {
    val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }.filter{case Some(_) => true case None => false}.map(_.get)
    cblist.foreach(_.reset)
    val query = if (cblist.nonEmpty) {
      cblist
    } else {
      groupers
    }
    query.map(g => s"${encodeURIParam(g.groupname)}:${g.analyze(data).map(x => encodeURIParam(x.easyprint)).mkString("!")}")
  }
  
  lazy val scoresRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("scores") {
        pathEnd {
          get {
            parameters('html.?) { (html) =>
              complete(
                listWettkaempfeAsync.map{competitions => html match {
                  case None => 
                    val allMap = ("all" -> Map(
                        ("scores-href" -> "/api/scores/all"), 
                        ("grouper-href" -> "/api/scores/all/grouper"), 
                        ("filter-href" -> "/api/scores/all/filter"),
                        ("lastresults-href" -> s"/?all"), 
                        ("topresults-href" -> s"/?top"), 
                        ("name" -> "Übergreifend")
                    ))
                    ToResponseMarshallable(
                      competitions.map(comp => 
                        (comp.uuid.get.toString -> 
                          Map(
                            ("scores-href" -> s"/api/scores/${comp.uuid.get.toString}"), 
                            ("intermediate-scores-href" -> s"/api/scores/${comp.uuid.get.toString}/intermediate"), 
                            ("grouper-href" -> s"/api/scores/${comp.uuid.get.toString}/grouper"), 
                            ("filter-href" -> s"/api/scores/${comp.uuid.get.toString}/filter"),
                            ("lastresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString((s"last&c=${comp.uuid.get.toString}").getBytes))}"), 
                            ("topresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString((s"top&c=${comp.uuid.get.toString}").getBytes))}"), 
                            ("name" -> comp.easyprint)
                          )
                        )
                      ).toMap + allMap)
                  case Some(_) =>
                    ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                        competitions
                        .map(comp => (s"""
                          <li>
                          <a href='/api/scores/${comp.uuid.get.toString}?html'>${comp.easyprint}</a>, 
                          <a href='/api/scores/${comp.uuid.get.toString}/intermediate?html'>Zwischenresultate</a>, 
                          <a href='/?${new String(Base64.getUrlEncoder.encodeToString((s"last&c=${comp.uuid.get.toString}").getBytes))}'>Letzte Resultate</a>, 
                          <a href='/?${new String(Base64.getUrlEncoder.encodeToString((s"top&c=${comp.uuid.get.toString}").getBytes))}'>Top Resultate</a>
                          </li>"""))
                        .mkString("<html><body>\n", "\n", "</body></html>")
                    ))
                  }
                }
              )
            }
          }
        } ~
        pathPrefix("all") {
          val data = selectWertungen()
          val logodir = new java.io.File(Config.homedir + "/" + data.head.wettkampf.easyprint.replace(" ", "_"))
          val logofile = PrintUtil.locateLogoFile(logodir);

          val programmText = data.head.wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          pathEnd {
            get {
              parameters('groupby.?, 'filter.*, 'html.?) { (groupby, filter, html) =>
                complete(Future{
                  queryScoreResults("Alle Wettkämpfe", groupby, filter, html.nonEmpty, allGroupers, data, logofile)
                })
              }
            }
          } ~
          path("grouper") {
            get {
              complete{ Future { 
                allGroupers.map(g => encodeURIParam(g.groupname))
              }}
            }
          } ~
          path("filter") {
            get {
              parameters('groupby.?) { (groupby) =>
                complete{ Future {
                  queryFilters(groupby, allGroupers, data)
                }}
              }
            }
          }
        } ~
        pathPrefix(JavaUUID) { competitionId =>
          val wettkampf = readWettkampf(competitionId.toString)
          val data = selectWertungen(wkuuid = Some(competitionId.toString))
          val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          val logofile = PrintUtil.locateLogoFile(logodir);
          val programmText = wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          def riegenZuDurchgang: Map[String, Durchgang] = {
            val riegen = listRiegenZuWettkampf(wettkampf.id)
            riegen.map(riege => riege._1 -> riege._3.map(riege => Durchgang(0, riege)).getOrElse(Durchgang())).toMap
          }
          val byDurchgangMat = ByDurchgang(riegenZuDurchgang)
          val groupers: List[FilterBy] = {
            List(ByWettkampfProgramm(programmText), ByProgramm(programmText), 
                ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), byDurchgangMat, 
                ByRiege(), ByDisziplin(), ByJahr())
          }
          pathEnd {
            get {
              parameters('groupby.?, 'filter.*, 'html.?) { (groupby, filter, html) =>
                complete(
                  Future{
                    queryScoreResults(wettkampf.easyprint, groupby, filter, html.nonEmpty, groupers, data, logofile)                
                  }
                )
              }
            }
          } ~
          path("intermediate") {
            get {
              parameters('filter.*, 'html.?) { (filter, html) =>
                complete(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(competitionId.toString())).flatMap {
                  case ResponseMessage(startedDurchgaenge) =>
                    val sd = startedDurchgaenge.asInstanceOf[Set[String]]
                    if (sd.nonEmpty) {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter ++ Iterable(byDurchgangMat.groupname + ":" + sd.map(encodeURIParam(_)).mkString("!")), 
                              html.nonEmpty, groupers, data, logofile)
                          }
                    } else {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter, 
                              html.nonEmpty, groupers, Seq(), logofile)
                          }
                    }
                  case MessageAck(msg) => Future {
                    if (html.nonEmpty) {
                      HttpEntity(ContentTypes.`text/html(UTF-8)`, s"""<html><body><h1>Meldung</h1><p>$msg</p></body><html>""")
                    } else {
                      HttpEntity(ContentTypes.`application/json`, s"""{"message":"$msg"}""")
                    }
                  }
                  case _ => Future { throw new IllegalArgumentException("unknown competition/arguments") }
                })
              }
            }
          } ~
          path("grouper") {
            get {
              complete{ Future { 
                groupers.map(g => encodeURIParam(g.groupname))
              }}
            }
          } ~
          path("filter") {
            get {
              parameters('groupby.?) { (groupby) =>
                complete{ Future {
                  queryFilters(groupby, groupers, data)
                }}
              }
            }
          }
        }
      }
    }
  }
}
