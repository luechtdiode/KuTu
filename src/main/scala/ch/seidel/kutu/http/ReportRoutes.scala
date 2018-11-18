package ch.seidel.kutu.http

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.KutuService
import ch.seidel.kutu.renderer.{KategorieTeilnehmerToHtmlRenderer, PrintUtil, RiegenBuilder}

import scala.concurrent.duration.DurationInt

trait ReportRoutes extends SprayJsonSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {

  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val renderer = new KategorieTeilnehmerToHtmlRenderer() {}

  lazy val reportRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("report") {
        pathPrefix(JavaUUID) { competitionId =>
          val wettkampf = readWettkampf(competitionId.toString)
          val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          val logofile = PrintUtil.locateLogoFile(logodir)

          path("startlist") {
            get {
              complete({
                val kandidaten = getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get))
                val riegen = RiegenBuilder.mapToGeraeteRiegen(kandidaten)

                HttpEntity(ContentTypes.`text/html(UTF-8)`, renderer.riegenToKategorienListeAsHTML(riegen, logofile))
              })
            }
          }
        }
      }
    }
  }

}
