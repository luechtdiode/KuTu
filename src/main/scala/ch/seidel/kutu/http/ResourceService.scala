package ch.seidel.kutu.http

import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.*
import org.apache.pekko.http.scaladsl.model.headers.`Cache-Control`
import org.apache.pekko.http.scaladsl.server.{Directives, Route}

trait ResourceService extends Directives {
  private val noCacheIndex = `Cache-Control`(`no-cache`, `no-store`, `must-revalidate`)

  private val cacheAssets = `Cache-Control`(`public`, `max-age`(31536000))

  val fallbackRoute: Route = respondWithHeader(noCacheIndex) {
    getFromResource("app/index.html")
  }

  private def appRoute = {
    pathPrefixLabeled("", "index") {
      pathEndOrSingleSlash {
        fallbackRoute
      }
    } ~
    respondWithHeader(cacheAssets) {
      getFromResourceDirectory("app")
    } ~ respondWithHeader(cacheAssets) {
      getFromResourceDirectory("static")
    }
  }

  val resourceRoutes: Route = appRoute
}
