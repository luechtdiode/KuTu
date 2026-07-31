package ch.seidel.kutu.http

import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.model.{HttpHeader, MediaType, MediaTypes}
import org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.*
import org.apache.pekko.http.scaladsl.model.headers.{CacheDirectives, `Cache-Control`}
import org.apache.pekko.http.scaladsl.server.{Directive0, Directives, Route}

trait ResourceService extends Directives {
  private val noCacheIndex = `Cache-Control`(`no-cache`, `no-store`, `must-revalidate`)

  private val cacheAssets = `Cache-Control`(`public`, `max-age`(31536000))

  private def cacheControlFor(mediaType: MediaType): HttpHeader = {
    mediaType match {
      case m if m.mainType == "image" =>
        `Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(31536000)) // 1 year for images
      case MediaTypes.`text/css` | MediaTypes.`application/javascript` =>
        `Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(86400)) // 1 day for assets
      case MediaTypes.`application/json` =>
        `Cache-Control`(CacheDirectives.`no-cache`, CacheDirectives.`no-store`) // No cache for API data
      case _ =>
        `Cache-Control`(CacheDirectives.`no-cache`, `must-revalidate`) // Default fallback
    }
  }

  private def withMimeTypeCaching: Directive0 = mapResponse { response =>
    val mediaType = response.entity.contentType.mediaType
    response.mapHeaders(headers => headers :+ cacheControlFor(mediaType))
  }

  val fallbackRoute: Route = respondWithHeader(noCacheIndex) {
    getFromResource("app/index.html")
  }

  private def appRoute = {
    pathPrefixLabeled("", "index") {
      pathEndOrSingleSlash {
        fallbackRoute
      }
    } ~
    withMimeTypeCaching {
      getFromResourceDirectory("app") ~
      getFromResourceDirectory("static")
    }
  }

  val resourceRoutes: Route = appRoute
}
