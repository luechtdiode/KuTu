package ch.seidel.kutu.http

import java.io.File
import java.util.Base64

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.akka.{CompetitionCoordinatorClientActor, MessageAck, ResponseMessage, StartedDurchgaenge}
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Durchgang, KutuService, NullObject, PublishedScoreView, WertungView, encodeURIParam}
import ch.seidel.kutu.renderer.{PrintUtil, ScoreToHtmlRenderer, ScoreToJsonRenderer}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait ScoreRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

  val allGroupers = List(
      ByWettkampfProgramm(), ByProgramm(), ByWettkampf(),
      ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), ByAthlet(),
      ByRiege(), ByDisziplin(), ByJahr()
  )
                  
  def queryScoreResults(wettkampf: String, groupby: Option[String], filter: Iterable[String], html: Boolean,
                        groupers: List[FilterBy], data: Seq[WertungView], alphanumeric: Boolean,
                        logofile: File): HttpEntity.Strict = {
    val diszMap = data.groupBy { x => x.wettkampf.programmId }.map{ x =>
      x._1 -> Map(
            "W" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("W"))
          , "M" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("M")))
    }
    val query = GroupBy(groupby, filter, data, alphanumeric, groupers);

    if (html) {
      HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer(){override val title: String = wettkampf}
      .toHTML(query.select(data).toList, athletsPerPage = 0, sortAlphabetically = alphanumeric, diszMap, logofile))
    } else {
      HttpEntity(ContentTypes.`application/json`,  ScoreToJsonRenderer
      .toJson(wettkampf, query.select(data).toList, sortAlphabetically = alphanumeric, diszMap, logofile))
    }
  }
  
  def queryFilters(groupby: Option[String], groupers: List[FilterBy], data: Seq[WertungView]): Seq[String] = {
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
            parameters('html.?) { html =>
              complete(
                listWettkaempfeAsync.map{competitions => html match {
                  case None => 
                    val allMap = "all" -> Map(
                        "scores-href" -> "/api/scores/all",
                        "grouper-href" -> "/api/scores/all/grouper",
                        "filter-href" -> "/api/scores/all/filter",
                        "name" -> "Übergreifend"
                    )
                    ToResponseMarshallable(
                      competitions.map(comp => 
                        comp.uuid.get.toString ->
                          Map(
                            "scores-href" -> s"/api/scores/${comp.uuid.get.toString}",
                            "name" -> comp.easyprint
                          )
                      ).toMap + allMap)
                  case Some(_) =>
                    ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                        competitions
                        .map(comp => s"""
                          <li> <a href='/api/scores/${comp.uuid.get.toString}?html'>${comp.easyprint}</a></li>""")
                        .mkString("<html><body><h1>Ranglisten</h1>\n", "\n", "</body></html>")
                    ))
                  }
                }
              )
            }
          }
        } ~
        pathPrefix("all") {
          val data = selectWertungen()
          val logodir = new java.io.File(Config.homedir)
          val logofile = PrintUtil.locateLogoFile(logodir)

//          val programmText = data.head.wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          pathEnd {
            get {
              parameters('groupby.?, 'filter.*, 'html.?, 'alphanumeric.?) { (groupby, filter, html, alphanumeric) =>
                complete(Future{
                  queryScoreResults("Alle Wettkämpfe", groupby, filter, html.nonEmpty, allGroupers, data, alphanumeric.nonEmpty, logofile)
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
              parameters('groupby.?) { groupby =>
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
          val logofile = PrintUtil.locateLogoFile(logodir)
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
              parameters('html.?) { html =>
                complete(
                  listPublishedScores(competitionId).map{scores:List[PublishedScoreView] => html match {
                    case None =>
                      ToResponseMarshallable(
                        scores.map(score =>
                          competitionId.toString + score.title ->
                            Map(
                              "scores-href" -> s"/api/scores/${competitionId.toString}/query?${score.query}",
                              "name" -> score.title
                            )
                        ).toMap + ("generic" -> Map(
                          "intermediate-scores-href" -> s"/api/scores/${competitionId.toString}/intermediate",
                          "grouper-href" -> s"/api/scores/${competitionId.toString}/grouper",
                          "filter-href" -> s"/api/scores/${competitionId.toString}/filter",
                          "lastresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString(s"last&c=${competitionId.toString}".getBytes))}",
                          "topresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString(s"top&c=${competitionId.toString}".getBytes))}",
                          "name" -> "Zwischenresultate"
                        )))
                    case Some(_) =>
                      ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                        (scores
                          .map(score => s"""
                                |<li><a href='/api/scores/${competitionId.toString}/query?html&${score.query}'>${score.title}</a></li>""".stripMargin)
                          :+ s"""
                                |<li><a href='/api/scores/${competitionId.toString}/intermediate?html'>Zwischenresultate</a></li>
                                |<li><a href='/?${new String(Base64.getUrlEncoder.encodeToString(s"last&c=${competitionId.toString}".getBytes))}'>Letzte Resultate</a></li>
                                |<li><a href='/?${new String(Base64.getUrlEncoder.encodeToString(s"top&c=${competitionId.toString}".getBytes))}'>Top Resultate</a></li>
                                |<li><a href='/api/scores/${competitionId.toString}/query?html'>Generische Abfragen</a></li>
                                |""".stripMargin
                        ).mkString(s"<html><body><h1>Ranglisten zu ${wettkampf.easyprint}</h1>\n", "\n", "</body></html>")
                      ))
                  }
                  }
                )
              }
            }
          } ~
          path("query") {
            get {
              parameters('groupby.?, 'filter.*, 'html.?, 'alphanumeric.?) { (groupby, filter, html, alphanumeric) =>
                complete(
                  if (groupby == None && filter.isEmpty) {
                    ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                      s"""
                         |<html>
                         |<body>
                         |  <h1>Ranglisten dynamisch abfragen zu ${wettkampf.easyprint}</h1>
                         |  <h2>Syntax</h2>
                         |  <p>
                         |  <pre>
                         |  /api/scores/${competitionId.toString}/query?html
                         |    &<em>groupby=<b>grouper #1 </b>[:<b>grouper #n] </b></em>
                         |    [ & <em>filter=<b>filtername #1</b>:<b>filtervalue #1 </b>[!<b>filtervalue #n] </b></em> ]
                         |    [ & <em>filter=<b>filtername #n</b>:<b>filtervalue #1 </b>[!<b>filtervalue #n] </b></em> ]
                         |    [ & <em><b>alphanumeric</b></em> ]
                         |  </pre>
                         |  </p>
                         |  <h2>Grouper Syntax (erforderlich)</h2>
                         |  <p>Mindestens ein Grouper muss angegeben werden.<br>
                         |  Parametername: <b>'groupby='</b>. Dieser Abfrage-Parameter darf genau einmal in der URL verwendet werden.<br>
                         |  Parameterwert: Auflistung von URL-codierten Grouper-Namen, getrennt mit ':'<br>
                         |  <pre>
                         |  groupby=<b>grouper #1 </b>[:<b>grouper #n] </b>
                         |  </pre>
                         |  </p>
                         |  <h3>Verf&uuml;gbare Grouper</h3>
                         |  <ul>${groupers.map(g => s"<li><pre>${encodeURIParam(g.groupname)}</pre></li>").mkString("\n")}</ul>
                         |  <h2>Filter Syntax (optional)</h2>
                         |  <p>Es k&ouml;nnen beliebig viele Filter hinzugef&uuml;gt werden.<br>
                         |  Parametername: <b>'filter='</b>. Dieser Parameter darf beliebig oft eingesetzt werden<br>
                         |  Parameterwert: URL-codierte Filter-name, gefolgt von Filterdefinitionen, getrennt mit ':'<br>
                         |  Filterdefinition: Auflistung von URL-codierten Filterwerten, getrennt mit '!'<br>
                         |  <pre>
                         |  filter=<b>filtername</b>:<b>filtervalue #1 [</b>!<b>filtervalue #n </b>]
                         |  </pre>
                         |  </p>
                         |  <h3>Verf&uuml;gbare Filter</h3>
                         |  <ul>${queryFilters(groupby, groupers, data).map(filter => s"<li><pre>${filter}</pre></li>").mkString("\n")}</ul>
                         |  <h2>Alphanumerische Sortierung (optional)</h2>
                         |  <p>Mit dem Parameter '<b>alphanumeric</b>' kann die Auflistung alphanumerisch (alphabetisch) auf dem Namen sortiert werden.
                         |  Ohne Angabe ist die Sortierung numerisch gem&auml;ss der Rangierung.</p>
                         |</body>
                         |</html>
                       """.stripMargin))
                  } else {
                    Future{
                      queryScoreResults(wettkampf.easyprint, groupby, filter, html.nonEmpty, groupers, data, alphanumeric.nonEmpty, logofile)
                    }
                  }
                )
              }
            }
          } ~
          path("intermediate") {
            get {
              (parameters('filter.*, 'html.?) & optionalHeaderValueByName("clientid")) { (filter, html, clientid) =>
                complete(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(competitionId.toString()), clientid.getOrElse("")).flatMap {
                  case ResponseMessage(startedDurchgaenge) =>
                    val sd = startedDurchgaenge.asInstanceOf[Set[String]]
                    if (sd.nonEmpty) {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter ++ Iterable(byDurchgangMat.groupname + ":" + sd.mkString("!")),
                              html.nonEmpty, groupers, data, false, logofile)
                          }
                    } else {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter, 
                              html.nonEmpty, groupers, Seq(), false, logofile)
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
