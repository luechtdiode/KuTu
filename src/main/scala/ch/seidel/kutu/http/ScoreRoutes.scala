package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import ch.seidel.kutu.KuTuServer.handleCID
import ch.seidel.kutu.actors.{CompetitionCoordinatorClientActor, MessageAck, ResponseMessage, StartedDurchgaenge}
import ch.seidel.kutu.data.*
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.renderer.PrintUtil.*
import ch.seidel.kutu.renderer.{PrintUtil, ScoreToHtmlRenderer, ScoreToJsonRenderer}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import java.io.File
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Base64
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait
ScoreRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {
  import spray.json.*
  import spray.json.DefaultJsonProtocol.*

  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

  val allGroupers = List(
      ByWettkampfProgramm(), ByProgramm(), ByWettkampf(),
      ByJahrgang(), ByJahrgangsAltersklasse("Turn10® Altersklassen", Altersklasse.altersklassenTurn10), ByAltersklasse("DTB Altersklassen", Altersklasse.altersklassenDTB),
      ByGeschlecht(), ByVerband(), ByVerein(), ByAthlet(),
      ByRiege(), ByRiege2(), ByDisziplin(), ByJahr()
  )
                  
  private def queryScoreResults(wettkampf: String, groupby: Option[String], filter: Iterable[String], html: Boolean,
                        groupers: List[FilterBy], data: Seq[WertungView], alphanumeric: Boolean, isAvgOnMultipleCompetitions: Boolean,
                        kind: ScoreListKind, counting: ScoreListBestN,
                        logofile: File): HttpEntity.Strict = {
    val query = GroupBy(groupby, filter, data, alphanumeric, isAvgOnMultipleCompetitions, kind, counting, groupers)

    if html then {
      HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer(){override val title: String = wettkampf}
      .toHTML(query.select(data).toList, athletsPerPage = 0, sortAlphabetically = alphanumeric, isAvgOnMultipleCompetitions = isAvgOnMultipleCompetitions, logofile))
    } else {
      HttpEntity(ContentTypes.`application/json`,  ScoreToJsonRenderer
      .toJson(wettkampf, query.select(data).toList, sortAlphabetically = alphanumeric, isAvgOnMultipleCompetitions = isAvgOnMultipleCompetitions, logofile))
    }
  }
  
  private def queryFilters(groupby: Option[String], groupers: List[FilterBy], data: Seq[WertungView]): Seq[String] = {
    val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map{groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }.filter{case Some(_) => true case None => false}.map(_.get)
    cblist.foreach(_.reset)
    val query = if cblist.nonEmpty then {
      cblist
    } else {
      groupers
    }
    query.map(g => s"${encodeURIParam(g.groupname)}:${g.analyze(data).map(x => encodeURIParam(x.easyprint)).mkString("!")}")
  }
  
  lazy val scoresRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      pathPrefixLabeled("scores", "scores") {
        pathEnd {
          get {
            parameters(Symbol("html").?) { html =>
              complete(
                listWettkaempfeAsync.map{competitions => html match {
                  case None => 
                    val allMap: (String, Map[String, String]) = "all" -> Map(
                        "scores-href" -> "/api/scores/all",
                        "grouper-href" -> "/api/scores/all/grouper",
                        "filter-href" -> "/api/scores/all/filter",
                        "name" -> "Übergreifend"
                    )
                      ToResponseMarshallable((competitions
                        .filter(comp => comp.uuid.nonEmpty)
                        .map(comp =>
                          comp.uuid.get ->
                            Map(
                              "scores-href" -> s"/api/scores/${comp.uuid.get}",
                              "name" -> comp.easyprint
                            )
                        ).toMap + allMap).toJson)
                  case Some(_) =>
                    ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                        competitions
                        .filter(comp => comp.uuid.nonEmpty)
                        .map(comp => s"""
                          <li> <a href='/api/scores/${comp.uuid.get}?html'>${comp.easyprint}</a></li>""")
                        .mkString("<html><body><h1>Ranglisten</h1>\n", "\n", "</body></html>")
                    ))
                  }
                }
              )
            }
          }
        } ~
        pathPrefixLabeled("all", "all") {
          val data = selectWertungen()
          val logodir = new java.io.File(Config.homedir)
          val logofile = PrintUtil.locateLogoFile(logodir)

//          val programmText = data.head.wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
          pathEnd {
            get {
              parameters(Symbol("groupby").?, Symbol("filter").*, Symbol("html").?
                , Symbol("alphanumeric").?
                , Symbol("avg").?
                , Symbol("kind").?
                , Symbol("counting").?
              ) { (groupby, filter, html, alphanumeric, avg, kind, counting) =>
                complete(Future{
                  queryScoreResults("Alle Wettkämpfe", groupby, filter, html.nonEmpty, allGroupers, data, alphanumeric.nonEmpty, !avg.exists(s => s.equals("false")),
                    ScoreListKind(kind), ScoreListBestN(counting), logofile)
                })
              }
            }
          } ~
          pathLabeled("grouper", "grouper") {
            get {
              complete{ Future { 
                allGroupers.map(g => encodeURIParam(g.groupname)).toJson
              }}
            }
          } ~
          pathLabeled("filter", "filter") {
            get {
              parameters(Symbol("groupby").?) { groupby =>
                complete{ Future {
                  queryFilters(groupby, allGroupers, data).toJson
                }}
              }
            }
          }
        } ~
        pathPrefixLabeled(JavaUUID, ":competition-id") { competitionId =>
          import AbuseHandler.*
          if !wettkampfExists(competitionId.toString) then {
            log.error(handleAbuse(clientId, uri))
            complete(StatusCodes.NotFound)
          } else {
            val wettkampf = readWettkampf(competitionId.toString)
            val dgEvents = selectSimpleDurchgaenge(wettkampf.id)
              .map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate)))
            //val startDate = (LocalDateTime.of(wettkampf.datum.toLocalDate, LocalTime.MIN) +: dgEvents.map(_._2)).distinct.min.toLocalDate
            val endDate = (LocalDateTime.of(wettkampf.datum.toLocalDate, LocalTime.MAX) +: dgEvents.map(_._2)).distinct.max.toLocalDate
            //val wkEventString = if (startDate.equals(endDate))  f"$startDate%td.$startDate%tm.$startDate%ty" else  f"$startDate%td.$startDate%tm.$startDate%ty - $endDate%td.$endDate%tm.$endDate%ty"
            //val wkdate: LocalDate = ch.seidel.kutu.domain.sqlDate2ld(wettkampf.datum)
            //val wkEndDate = ld2SQLDate(endDate)
            val scheduledDisziplines = listScheduledDisziplinIdsZuWettkampf(wettkampf.id)
            val data = selectWertungen(wettkampfId = Some(wettkampf.id))
              .filter(w => scheduledDisziplines.contains(w.wettkampfdisziplin.disziplin.id))

            val logodir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
            val logofile = PrintUtil.locateLogoFile(logodir)
            val programmText = wettkampf.programmId match {case 20 => "Kategorie" case _ => "Programm"}
            val altersklassen = Altersklasse.parseGrenzen(wettkampf.altersklassen.get)
            val jgAltersklassen = Altersklasse.parseGrenzen(wettkampf.jahrgangsklassen.get)
            def riegenZuDurchgang: Map[String, Durchgang] = {
              val riegen = listRiegenZuWettkampf(wettkampf.id)
              riegen.map(riege => riege._1 -> riege._3.map(durchgangName => Durchgang(0, durchgangName)).getOrElse(Durchgang())).toMap
            }
            val byDurchgangMat = ByDurchgang(riegenZuDurchgang)
            val groupers: List[FilterBy] = {
              val standardGroupers = List(
                ByWettkampfProgramm(programmText), ByProgramm(programmText),
                ByWettkampfProgramm("Programm"), ByProgramm("Programm"),
                ByWettkampfProgramm("Kategorie"), ByProgramm("Kategorie"),
                ByJahrgang(), ByJahrgangsAltersklasse("Turn10® Altersklassen", Altersklasse.altersklassenTurn10), ByAltersklasse("DTB Altersklassen", Altersklasse.altersklassenDTB),
                ByGeschlecht(), ByVerband(), ByVerein(), byDurchgangMat,
                ByRiege(), ByRiege2(), ByDisziplin(), ByJahr())
              val akenhanced = (altersklassen.nonEmpty, jgAltersklassen.nonEmpty) match {
                case (true,true) => standardGroupers ++ List(ByAltersklasse("Wettkampf Altersklassen", altersklassen), ByJahrgangsAltersklasse("Wettkampf JG-Altersklassen", jgAltersklassen))
                case (false,true) => standardGroupers :+ ByJahrgangsAltersklasse("Wettkampf JG-Altersklassen", jgAltersklassen)
                case (true,false) => standardGroupers :+ ByAltersklasse("Wettkampf Altersklassen", altersklassen)
                case _ => standardGroupers
              }
              if wettkampf.hasTeams then {
                TeamRegel(wettkampf).getTeamRegeln.map(r => ByTeamRule("Wettkampf Teamregel " + r.toRuleName, r)).toList ++ akenhanced
              } else {
                akenhanced
              }
            }
            val logoHtml = if logofile.exists() then s"""<img class=logo src="${logofile.imageSrcForWebEngine}" title="Logo"/>""" else ""
            pathEnd {
              get {
                parameters(Symbol("html").?) { html =>
                  complete(
                    listPublishedScores(competitionId).map{(scores:List[PublishedScoreView]) => html match {
                      case None =>
                        ToResponseMarshallable((
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
                          ))).toJson)
                      case Some(_) =>
                        ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                          (scores
                            .map(score => if score.published then s"""
                                  |<li><a href='/api/scores/${competitionId.toString}/${score.id}/?html'>${score.title}</a></li>""".stripMargin else s"""
                                  |<li><a href='/api/scores/${competitionId.toString}/${score.id}/?html'>${score.title} (unveröffentlicht)</a></li>""".stripMargin
                            )
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
            pathPrefixLabeled(JavaUUID, ":score-id") { scoreUUID =>
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
                        val query = GroupBy(score.map(_.query).getOrElse(""), publishedData, groupers)
                        if html.nonEmpty then {
                          HttpEntity(ContentTypes.`text/html(UTF-8)`, new ScoreToHtmlRenderer() {
                            override val title: String = wettkampf.easyprint // + " - " + score.map(_.title).getOrElse(wettkampf.easyprint)
                          }
                            .toHTML(query.select(publishedData).toList, athletsPerPage = 0, sortAlphabetically = score.exists(_.isAlphanumericOrdered), isAvgOnMultipleCompetitions = score.exists(_.isAvgOnMultipleCompetitions), logofile))
                        } else {
                          HttpEntity(ContentTypes.`application/json`, ScoreToJsonRenderer
                            .toJson(wettkampf.easyprint, query.select(publishedData).toList, sortAlphabetically = score.exists(_.isAlphanumericOrdered), isAvgOnMultipleCompetitions = score.exists(_.isAvgOnMultipleCompetitions), logofile))
                        }
                      }
                      }
                  )
                }
              }
            } ~
            pathLabeled("query", "query") {
              get {
                parameters(Symbol("groupby").?, Symbol("filter").*, Symbol("html").?
                  , Symbol("alphanumeric").?
                  , Symbol("avg").?
                  , Symbol("kind").?
                  , Symbol("counting").?
                ) { (groupby, filter, html, alphanumeric, avg, kind, counting) =>
                  complete(
                    if !endDate.atStartOfDay().isBefore(LocalDate.now.atStartOfDay) || (groupby.isEmpty && filter.isEmpty && ScoreListKind(kind) != Teamrangliste) then {
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
                           |  <p>Nach dem Wettkampf-Tag (ab dem '${endDate.plusDays(1)}') können die Resultate dynamisch abgefragt werden.</p>
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
                           |  <ul>${queryFilters(groupby, groupers, data).map(filter => s"<li><pre>$filter</pre></li>").mkString("\n")}</ul>
                           |  <h2>Alphanumerische Sortierung (optional)</h2>
                           |  <p>Mit dem Parameter '<b>alphanumeric</b>' kann die Auflistung alphanumerisch (alphabetisch) auf dem Namen sortiert werden.
                           |  Ohne Angabe ist die Sortierung numerisch gem&auml;ss der Rangierung.</p></div>
                           |  <h2>Summen anstatt Durchschnittswertung beim Zusammenzug aus mehreren Wettkämpfen (optional)</h2>
                           |  <p>Mit dem Parameter '<b>avg=false</b>' werden die Resultate aus mehreren Wettkämpfen summiert anstatt den Durchschnittswert gerechnet.</p></div>
                           |  <h2>HTML-Ausgabe (optional)</h2>
                           |  <p>Mit dem Parameter '<b>html</b>' wird die Rangliste in lesbarer Form als HTML generiert.
                           |  Ohne Angabe werden die Rohdaten der Rangliste als JSON generiert.</p></div>
                           |  <h2>Rangliste-Typ (optional)</h2>
                           |  <p>Mit dem Parameter:
                           |  <ul>
                           |  <li><pre>kind=Einzelrangliste</pre> (default) kann explizit die Einzelrangliste abgefragt werden (immer vorhanden).</li>
                           |  <li><pre>kind=Teamrangliste</pre> kann explizit die Teamrangliste abgefragt werden (falls vorhanden).</li>
                           |  <li><pre>kind=Kombirangliste</pre> Wird die Einzelrangliste und falls vorhanden auch die Teamrangliste abgefragt.</li>
                           |  </p></div>
                           |</body>
                           |</html>
                         """.stripMargin))
                    } else {
                      Future{
                        queryScoreResults(wettkampf.easyprint, groupby, filter, html.nonEmpty, groupers, data,
                          alphanumeric.nonEmpty, !avg.exists(s => s.equals("false")), ScoreListKind(kind), ScoreListBestN(counting), logofile)
                      }
                    }
                  )
                }
              }
            } ~
            pathLabeled("intermediate", "intermediate") {
              get {
                (parameters(Symbol("q").?, Symbol("filter").*, Symbol("html").?) & optionalHeaderValueByName("clientid")) { (q, filter, html, clientid) =>

                  def filterMatchingWertungenToQuery = {
                    val queryTokens = q.toList.flatMap(x => x.split(" ")).map(_.toLowerCase)
                    (w: WertungView) => {
                      queryTokens.isEmpty ||
                        queryTokens.forall {
                          case s: String if s == s"${w.athlet.id}" => true
                          case s: String if s == w.athlet.name.toLowerCase => true
                          case s: String if s == w.athlet.vorname.toLowerCase => true
                          case s: String if s == w.athlet.verein.mkString.toLowerCase => true
                          case s: String if s == w.wettkampfdisziplin.programm.name.toLowerCase => true
                          case s: String if s == w.athlet.geschlecht.toLowerCase => true
                          case s: String if s.nonEmpty => w.athlet.verein.mkString.toLowerCase.contains(s) || w.riege.exists(_.toLowerCase.contains(s))
                          case _ => false
                        }
                    }
                  }
                  complete(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(competitionId.toString), clientid.getOrElse("")).flatMap {
                    case ResponseMessage(startedDurchgaenge) =>
                      val sd = startedDurchgaenge.asInstanceOf[Set[String]]
                      val kind = if wettkampf.teamrule.nonEmpty then Kombirangliste else Einzelrangliste
                      if sd.nonEmpty then {
                            Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None,
                                filter ++ Iterable(byDurchgangMat.groupname + ":" + sd.mkString("!")),
                                html.nonEmpty, groupers, data.filter(filterMatchingWertungenToQuery), alphanumeric = false, isAvgOnMultipleCompetitions = true, kind, AlleWertungen, logofile)
                            }
                      } else {
                            Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None,
                                filter,
                                html.nonEmpty, groupers, Seq(), alphanumeric = false, isAvgOnMultipleCompetitions = true, kind, AlleWertungen, logofile)
                            }
                      }
                    case MessageAck(msg) =>
                      val kind = if wettkampf.teamrule.nonEmpty then Kombirangliste else Einzelrangliste
                      Future {queryScoreResults(s"${wettkampf.easyprint} - Zwischenresultate", None,
                        filter,
                        html.nonEmpty, groupers, Seq(), alphanumeric = false, isAvgOnMultipleCompetitions = true, kind, AlleWertungen, logofile)
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
            pathLabeled("grouper", "grouper") {
              get {
                complete{ Future {
                  groupers.map(g => encodeURIParam(g.groupname)).toJson
                }}
              }
            } ~
            pathLabeled("filter", "filter") {
              get {
                parameters(Symbol("groupby").?) { groupby =>
                  complete{ Future {
                    queryFilters(groupby, groupers, data).toJson
                  }}
                }
              }
            }
          }
        }
      }
    }
  }
}
