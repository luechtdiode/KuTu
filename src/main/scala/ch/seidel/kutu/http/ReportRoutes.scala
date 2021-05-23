package ch.seidel.kutu.http

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.{Kandidat, KutuService}
import ch.seidel.kutu.renderer.{KategorieTeilnehmerToHtmlRenderer, KategorieTeilnehmerToJSONRenderer, PrintUtil, RiegenBuilder}

import scala.concurrent.duration.DurationInt

trait ReportRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {

  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val renderer = new KategorieTeilnehmerToHtmlRenderer() {}
  val jsonrenderer = new KategorieTeilnehmerToJSONRenderer() {}

  lazy val reportRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("report") {
        pathPrefix(JavaUUID) { competitionId =>
          val wettkampf = readWettkampf(competitionId.toString)
          val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          val logofile = PrintUtil.locateLogoFile(logodir)

          path("startlist") {
            get {
              parameters(Symbol("html").?, Symbol("q").?, Symbol("gr").?) { (html, q, gr) => html match {
                case Some(_) =>
                  complete({
                    val kandidaten = getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get))
                      .filter(filterMatchingCandidatesToQuery(q))
                    val riegen = RiegenBuilder.mapToGeraeteRiegen(kandidaten)
                    gr match {
                      case Some(grv) if (grv.equalsIgnoreCase("verein")) =>
                        HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToVereinListeAsHTML(riegen, logofile))
                      case _ =>
                        HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToKategorienListeAsHTML(riegen, logofile))
                    }
                  })
                case None =>
                  complete({
                    val kandidaten = getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get))
                      .filter(filterMatchingCandidatesToQuery(q))
                    val riegen = RiegenBuilder.mapToGeraeteRiegen(kandidaten)
                    HttpEntity(ContentTypes.`application/json`, jsonrenderer.riegenToKategorienListeAsJSON(riegen, logofile))
                  })
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
