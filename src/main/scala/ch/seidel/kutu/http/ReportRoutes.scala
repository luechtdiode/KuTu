package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.KuTuServer.handleCID
import ch.seidel.kutu.akka.{CompetitionCoordinatorClientActor, GeraeteRiegeList, GetGeraeteRiegeList, KutuAppEvent, WertungContainer}
import ch.seidel.kutu.domain.{Kandidat, KutuService, encodeFileName, encodeURIComponent}
import ch.seidel.kutu.renderer._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

trait ReportRoutes extends SprayJsonSupport
  with JsonSupport with AuthSupport with RouterLogging
  with KutuService with IpToDeviceID
  with AbuseListHTMLRenderer {
  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

  val renderer: KategorieTeilnehmerToHtmlRenderer = new KategorieTeilnehmerToHtmlRenderer() {
    override val logger: Logger = LoggerFactory.getLogger(classOf[ReportRoutes])
  }
  val jsonrenderer: KategorieTeilnehmerToJSONRenderer = new KategorieTeilnehmerToJSONRenderer() {
    override val logger: Logger = LoggerFactory.getLogger(classOf[ReportRoutes])
  }

  lazy val reportRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      pathPrefixLabeled("report", "report") {
        pathLabeled("abused", "abused") {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
            abusedClientsToHTMListe()))
        }~
        pathPrefix(JavaUUID) { competitionId =>
          import AbuseHandler._
          if (!wettkampfExists(competitionId.toString)) {
            log.error(handleAbuse(clientId, uri))
            complete(StatusCodes.NotFound)
          } else {

            val wettkampf = readWettkampf(competitionId.toString)
            val logodir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
            val logofile = PrintUtil.locateLogoFile(logodir)

            pathLabeled("startlist", "startlist") {
              get {
                parameters(Symbol("html").?, Symbol("q").?, Symbol("gr").?) { (html, q, gr) =>
                  val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                  html match {
                    case Some(_) => complete {
                      val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                        case GeraeteRiegeList(riegen, _) =>
                          val filteredRiegen = riegen.filter { k => k.kandidaten.exists(filterMatchingCandidatesToQuery(q))}
                          gr match {
                            case Some(grv) if (grv.equalsIgnoreCase("verein")) =>
                              HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToVereinListeAsHTML(filteredRiegen, logofile))
                            case _ =>
                              HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToKategorienListeAsHTML(filteredRiegen, logofile))
                          }
                        case _ =>
                          StatusCodes.Conflict
                      }
                      toResponseMarshallable
                    }
                    case None => complete {
                      val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                        case GeraeteRiegeList(riegen, _) =>
                          HttpEntity(ContentTypes.`application/json`, jsonrenderer.riegenToKategorienListeAsJSON(riegen.filter { k => k.kandidaten.exists(filterMatchingCandidatesToQuery(q))}, logofile))
                        case _ =>
                          StatusCodes.Conflict
                      }
                      toResponseMarshallable
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

  private def filterMatchingCandidatesToQuery(q: Option[String]) = {
    val queryTokens = q.toList.flatMap(x => x.split(" ")).map(_.toLowerCase)
    k: Kandidat => {
      queryTokens.isEmpty ||
        queryTokens.forall {
          case s: String if s == s"${k.id}" => true
          case s: String if s == k.name.toLowerCase => true
          case s: String if s == k.vorname.toLowerCase => true
          case s: String if s == k.verein.toLowerCase => true
          case s: String if s == k.programm.toLowerCase => true
          case s: String if s == k.geschlecht.toLowerCase => true
          case s: String if s.nonEmpty => {
            k.verein.toLowerCase.contains(s) ||
              k.einteilung.exists(_.easyprint.toLowerCase.contains(s))
          }
          case _ => false
        }
    }
  }
}
