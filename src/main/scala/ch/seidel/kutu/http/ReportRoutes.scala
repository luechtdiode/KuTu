package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import ch.seidel.kutu.KuTuServer.handleCID
import ch.seidel.kutu.actors.{CompetitionCoordinatorClientActor, GeraeteRiegeList, GetGeraeteRiegeList, KutuAppEvent}
import ch.seidel.kutu.domain.{Kandidat, KutuService, encodeFileName}
import ch.seidel.kutu.renderer.{AbuseListHTMLRenderer, KategorieTeilnehmerToHtmlRenderer, KategorieTeilnehmerToJSONRenderer}
import ch.seidel.kutu.renderer.ServerPrintUtil.*
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.slf4j.{Logger, LoggerFactory}

import java.net.URLDecoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

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
  private val jsonrenderer: KategorieTeilnehmerToJSONRenderer = new KategorieTeilnehmerToJSONRenderer() {
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
          import AbuseHandler.*
          if !wettkampfExists(competitionId.toString) then {
            log.error(handleAbuse(clientId, uri))
            complete(StatusCodes.NotFound)
          } else {

            val wettkampf = readWettkampf(competitionId.toString)
            val dgEvents = selectSimpleDurchgaenge(wettkampf.id)
              .map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate)))
            val logodir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
            val logofile = locateLogoFile(logodir)

            pathLabeled("startlist", "startlist") {
              get {
                parameters(Symbol("html").?, Symbol("q").?, Symbol("gr").?) { (html, q, gr) =>
                  val eventualKutuAppEvent: Future[KutuAppEvent] = CompetitionCoordinatorClientActor.publish(GetGeraeteRiegeList(competitionId.toString), clientId)
                  html match {
                    case Some(_) => complete {
                      val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                        case GeraeteRiegeList(riegen, _) if riegen.nonEmpty =>
                          val filteredRiegen = riegen.filter { k => k.kandidaten.exists(filterMatchingCandidatesToQuery(q))}
                          gr match {
                            case Some(grv) if grv.equalsIgnoreCase("verein") =>
                              HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToVereinListeAsHTML(filteredRiegen, logofile, dgEvents))
                            case Some(grv) if grv.equalsIgnoreCase("durchgang") =>
                              HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToDurchgangListeAsHTML(filteredRiegen, logofile, dgEvents))
                            case _ =>
                              HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToKategorienListeAsHTML(filteredRiegen, logofile, dgEvents))
                          }
                        case _ =>
                          StatusCodes.NotFound
                      }
                      toResponseMarshallable
                    }
                    case None => complete {
                      val toResponseMarshallable: Future[ToResponseMarshallable] = eventualKutuAppEvent.map {
                        case GeraeteRiegeList(riegen, _) if riegen.nonEmpty =>
                          HttpEntity(ContentTypes.`application/json`, jsonrenderer.riegenToKategorienListeAsJSON(riegen.filter { k => k.kandidaten.exists(filterMatchingCandidatesToQuery(q))}, logofile, dgEvents))
                        case _ =>
                          StatusCodes.NotFound
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
    val queryTokens = q.toList.flatMap(x => URLDecoder.decode(x, "UTF-8").split(" ")).map(_.toLowerCase)
    (k: Kandidat) => {
      queryTokens.isEmpty ||
        queryTokens.forall {
          case s: String if s.equals(s"${k.id}") => true
          case s: String if s.equals(k.name.toLowerCase) => true
          case s: String if s.equals(k.vorname.toLowerCase) => true
          case s: String if s.equals(k.verein.toLowerCase) => true
          case s: String if s.equals(k.programm.toLowerCase) => true
          case s: String if s.equals(k.geschlecht.toLowerCase) => true
          case s: String if s.nonEmpty => k.verein.toLowerCase.contains(s) || k.einteilung.exists(_.easyprint.toLowerCase.contains(s))
          case _ => false
        }
    }
  }
}
