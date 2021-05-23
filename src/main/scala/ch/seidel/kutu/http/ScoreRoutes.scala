package ch.seidel.kutu.http

import java.io.File
import java.time.LocalDate
import java.util.Base64

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.akka.{CompetitionCoordinatorClientActor, MessageAck, ResponseMessage, StartedDurchgaenge}
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Durchgang, Kandidat, KutuService, NullObject, PublishedScoreView, WertungView, encodeURIParam}
import ch.seidel.kutu.renderer.{PrintUtil, ScoreToHtmlRenderer, ScoreToJsonRenderer}
import ch.seidel.kutu.renderer.PrintUtil._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait
ScoreRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
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
            parameters(Symbol("html").?) { html =>
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
              parameters(Symbol("groupby").?, Symbol("filter").*, Symbol("html").?, Symbol("alphanumeric").?) { (groupby, filter, html, alphanumeric) =>
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
              parameters(Symbol("groupby").?) { groupby =>
                complete{ Future {
                  queryFilters(groupby, allGroupers, data)
                }}
              }
            }
          }
        } ~
        pathPrefix(JavaUUID) { competitionId =>
          val wettkampf = readWettkampf(competitionId.toString)
          val wkdate: LocalDate = ch.seidel.kutu.domain.sqlDate2ld(wettkampf.datum)
          val data = selectWertungen(wkuuid = Some(competitionId.toString))
          val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          val logofile = PrintUtil.locateLogoFile(logodir)
          val programmText = wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          def riegenZuDurchgang: Map[String, Durchgang] = {
            val riegen = listRiegenZuWettkampf(wettkampf.id)
            riegen.map(riege => riege._1 -> riege._3.map(durchgangName => Durchgang(0, durchgangName)).getOrElse(Durchgang())).toMap
          }
          val byDurchgangMat = ByDurchgang(riegenZuDurchgang)
          val groupers: List[FilterBy] = {
            List(ByWettkampfProgramm(programmText), ByProgramm(programmText), 
                ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), byDurchgangMat, 
                ByRiege(), ByDisziplin(), ByJahr())
          }
          val logoHtml = if (logofile.exists()) s"""<img class=logo src="${logofile.imageSrcForWebEngine}" title="Logo"/>""" else ""
          pathEnd {
            get {
              parameters(Symbol("html").?) { html =>
                complete(
                  listPublishedScores(competitionId).map{scores:List[PublishedScoreView] => html match {
                    case None =>
                      ToResponseMarshallable(
                        scores.map(score =>
                          score.title ->
                            Map(
                              "scores-href" -> s"/api/scores/${competitionId.toString}/${score.id}?html",
                              "scores-query" -> s"/api/scores/${competitionId.toString}/query?${score.query}",
                              "name" -> score.title,
                              "published" -> s"${score.published}",
                              "published-date" -> s"${score.publishedDate}"
                            )
                        ).toMap + ("generic" -> Map(
                          "intermediate-scores-href" -> s"/api/scores/${competitionId.toString}/intermediate",
                          "grouper-href" -> s"/api/scores/${competitionId.toString}/grouper",
                          "filter-href" -> s"/api/scores/${competitionId.toString}/filter",
                          "lastresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString(s"last&c=${competitionId.toString}".getBytes))}",
                          "topresults-href" -> s"/?${new String(Base64.getUrlEncoder.encodeToString(s"top&c=${competitionId.toString}".getBytes))}",
                          "name" -> "Zwischenresultate",
                          "logo" -> logoHtml
                        )))
                    case Some(_) =>
                      ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                        (scores
                          .map(score => (if (score.published) s"""
                                |<li><a href='/api/scores/${competitionId.toString}/${score.id}/?html'>${score.title}</a></li>""".stripMargin else s"""
                                |<li><a href='/api/scores/${competitionId.toString}/${score.id}/?html'>${score.title} (unveröffentlicht)</a></li>""".stripMargin
                          ))
                          :+ s"""
                                |<li><a href='/api/scores/${competitionId.toString}/intermediate?html'>Zwischenresultate</a></li>
                                |<li><a href='/?${new String(Base64.getUrlEncoder.encodeToString(s"last&c=${competitionId.toString}".getBytes))}'>Letzte Resultate</a></li>
                                |<li><a href='/?${new String(Base64.getUrlEncoder.encodeToString(s"top&c=${competitionId.toString}".getBytes))}'>Top Resultate</a></li>
                                |<li><a href='/api/scores/${competitionId.toString}/query?html'>Generische Abfragen</a></li>
                                |""".stripMargin
                        ).mkString(s"""<html><head>
                          <style type="text/css">
                            .headline {
                              display: block;
                              border: 0px;
                              overflow: auto;
                              margin-top: 10px;
                              padding: 10px;
                            }
                            .logo {
                              float: right;
                              height: 100px;
                              border-radius: 5px;
                            }
                            .title {
                              float: left;
                            }
                            .content {
                              display: block;
                              margin-top: 10px;
                              padding: 10px;
                            }                            
                            body {
                              font-family: "Arial", "Verdana", sans-serif;
                            }
                          </style></head><body><div class=headline>
                          $logoHtml
                          <div class=title><h1>Ranglisten zu ${wettkampf.easyprint}</h1></div></div><div class="content">\n""", "\n", "</div></body></html>")
                      ))
                  }
                  }
                )
              }
            }
          } ~
          pathPrefix(JavaUUID) { scoreUUID =>
            get {
              parameters(Symbol("html").?) { html =>
                val scoreId = scoreUUID.toString
                complete(
                  listPublishedScores(competitionId)
                    .map(sc => sc.filter(c => {
                      c.id == scoreId && c.published
                    }))
                    .map {list => ToResponseMarshallable {
                      val (score, publishedData) = list match {
                        case Nil => (None,Seq[WertungView]())
                        case c::_ => (Some(c), data)
                      }
                      val diszMap = publishedData.groupBy { x => x.wettkampf.programmId }.map { x =>
                        x._1 -> Map(
                          "W" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("W"))
                          , "M" -> listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("M")))
                      }
                      val query = GroupBy(score.map(_.query).getOrElse(""), publishedData)
                      if (html.nonEmpty) {
                        HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer() {
                          override val title: String = wettkampf.easyprint // + " - " + score.map(_.title).getOrElse(wettkampf.easyprint)
                        }
                          .toHTML(query.select(publishedData).toList, athletsPerPage = 0, sortAlphabetically = score.map(_.isAlphanumericOrdered).getOrElse(false), diszMap, logofile))
                      } else {
                        HttpEntity(ContentTypes.`application/json`, ScoreToJsonRenderer
                          .toJson(wettkampf.easyprint, query.select(publishedData).toList, sortAlphabetically = score.map(_.isAlphanumericOrdered).getOrElse(false), diszMap, logofile))
                      }
                    }
                    }
                )
              }
            }
          } ~
          path("query") {
            get {
              parameters(Symbol("groupby").?, Symbol("filter").*, Symbol("html").?, Symbol("alphanumeric").?) { (groupby, filter, html, alphanumeric) =>
                complete(
                  if (!wkdate.atStartOfDay().isBefore(LocalDate.now.atStartOfDay) || (groupby == None && filter.isEmpty)) {
                    ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                      f"""
                         |<html>
                         |<head>
                         |                          <style type="text/css">
                         |                            .headline {
                         |                              display: block;
                         |                              border: 0px;
                         |                              overflow: auto;
                         |                              margin-top: 10px;
                         |                              padding: 10px;
                         |                            }
                         |                            .logo {
                         |                              float: right;
                         |                              height: 100px;
                         |                              border-radius: 5px;
                         |                            }
                         |                            .title {
                         |                              float: left;
                         |                            }
                         |                            .content {
                         |                              display: block;
                         |                              margin-top: 10px;
                         |                              padding: 10px;
                         |                            }
                         |                            body {
                         |                              font-family: "Arial", "Verdana", sans-serif;
                         |                            }
                         |                          </style></head><body><div class=headline>
                         |                          $logoHtml
                         |                          <div class=title><h1>Ranglisten dynamisch abfragen</h1><h2>${wettkampf.easyprint}</h2></div></div><div class="content">\n
                         |  <p>Nach dem Wettkampf-Tag (ab dem '${wkdate.plusDays(1)}') können die Resultate dynamisch abgefragt werden.</p>
                         |  <h2>Syntax</h2>
                         |  <p>
                         |  <pre>
                         |  /api/scores/${competitionId.toString}/query?
                         |    <em>groupby=<b>grouper #1 </b>[:<b>grouper #n] </b></em>
                         |    [ &<em>filter=<b>filtername #1</b>:<b>filtervalue #1 </b>[!<b>filtervalue #n] </b></em> ]
                         |    [ &<em>filter=<b>filtername #n</b>:<b>filtervalue #1 </b>[!<b>filtervalue #n] </b></em> ]
                         |    [ &<em><b>alphanumeric</b></em> ]
                         |    [ &<em><b>html</b></em> ]
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
                         |  Ohne Angabe ist die Sortierung numerisch gem&auml;ss der Rangierung.</p></div>
                         |  <h2>HTML-Ausgabe (optional)</h2>
                         |  <p>Mit dem Parameter '<b>html</b>' wird die Rangliste in lesbarer Form als HTML generiert.
                         |  Ohne Angabe werden die Rohdaten der Rangliste als JSON generiert.</p></div>
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
              (parameters(Symbol("q").?, Symbol("filter").*, Symbol("html").?) & optionalHeaderValueByName("clientid")) { (q, filter, html, clientid) =>

                def filterMatchingWertungenToQuery = {
                  val queryTokens = q.toList.flatMap(x => x.split(" ")).map(_.toLowerCase)
                  w: WertungView => {
                    queryTokens.isEmpty ||
                      queryTokens.forall {
                        case s: String if s == w.athlet.id + "" => true
                        case s: String if s == w.athlet.name.toLowerCase => true
                        case s: String if s == w.athlet.vorname.toLowerCase => true
                        case s: String if s == w.athlet.verein.mkString.toLowerCase => true
                        case s: String if s == w.wettkampfdisziplin.programm.name.toLowerCase => true
                        case s: String if s == w.athlet.geschlecht.toLowerCase => true
                        case s: String if s.nonEmpty => {
                          w.athlet.verein.mkString.toLowerCase.contains(s) ||
                            w.riege.exists(_.toLowerCase.contains(s))
                        }
                        case _ => false
                      }
                  }
                }
                complete(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(competitionId.toString()), clientid.getOrElse("")).flatMap {
                  case ResponseMessage(startedDurchgaenge) =>
                    val sd = startedDurchgaenge.asInstanceOf[Set[String]]
                    if (sd.nonEmpty) {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter ++ Iterable(byDurchgangMat.groupname + ":" + sd.mkString("!")),
                              html.nonEmpty, groupers, data.filter(filterMatchingWertungenToQuery), false, logofile)
                          }
                    } else {
                          Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None, 
                              filter, 
                              html.nonEmpty, groupers, Seq(), false, logofile)
                          }
                    }
                  case MessageAck(msg) =>
                    Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None,
                      filter,
                      html.nonEmpty, groupers, Seq(), false, logofile)
                    }
//                    Future {
//                    if (html.nonEmpty) {
//                      HttpEntity(ContentTypes.`text/html(UTF-8)`, s"""<html><body><h1>Meldung</h1><p>$msg</p></body><html>""")
//                    } else {
//                      HttpEntity(ContentTypes.`application/json`, s"""{"message":"$msg"}""")
//                    }
//                  }
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
              parameters(Symbol("groupby").?) { (groupby) =>
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
