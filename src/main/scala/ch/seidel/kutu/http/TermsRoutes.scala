package ch.seidel.kutu.http

import ch.seidel.kutu.Config.remoteAdminBaseUrl
import ch.seidel.kutu.KuTuApp.httpGetClientRequest
import ch.seidel.kutu.KuTuServer.executionContext
import ch.seidel.kutu.actors.FinishDurchgang
import ch.seidel.kutu.domain.{Terms, TermsBlock, TermsInfo}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.model.headers.CacheDirectives
import org.apache.pekko.http.scaladsl.model.headers.`Cache-Control`
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.server.{Directives, Route}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.util.ByteString
import spray.json.*

import scala.concurrent.{Await, Future}

object TermsClient extends Directives with SprayJsonSupport with JsonSupport {
  def fetchTerms(): TermsInfo = {
    import Core.*
    val info = httpGetClientRequest(s"$remoteAdminBaseUrl/api/terms").flatMap {
      case org.apache.pekko.http.scaladsl.model.HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[TermsInfo]
      case _ => Future {
        TermsInfo(
          version = Terms.version,
          stand = Terms.stand,
          title = Terms.title,
          blocks = Terms.blocks.map(block => TermsBlock(block.kind.toString, block.text))
        )
      }
    }
    Await.result(info, scala.concurrent.duration.Duration.Inf)
  }
}

/**
 * Liefert die Nutzungsbedingungen an den Web-Client, damit Desktop und Web-Oberfläche Text und
 * Version aus derselben Quelle beziehen (`terms/nutzungsbedingungen.md`, siehe [[Terms]]).
 *
 * Bewusst ohne Authentifizierung: die Nutzungsbedingungen sind öffentlich, und die Seite zur
 * Bestätigung eines hochgeladenen Wettkampfs kommt ohne Token aus. Es werden keine Wettkampf- oder
 * Personendaten ausgeliefert, deshalb ist die Antwort cachebar.
 */
trait TermsRoutes extends Directives with SprayJsonSupport with JsonSupport with RouterLogging {

  private val termsCacheControl = `Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(300))

  lazy val termsRoutes: Route = {
    pathLabeled("terms", "terms") {
      pathEnd {
        get {
          respondWithHeader(termsCacheControl) {
            complete(
              TermsInfo(
                version = Terms.version,
                stand = Terms.stand,
                title = Terms.title,
                blocks = Terms.blocks.map(block => TermsBlock(block.kind.toString, block.text))
              ).toJson
            )
          }
        }
      }
    }
  }
}
