package ch.seidel.kutu.http

import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.server.{Directives, Route}

trait ResourceService extends Directives {
  val fallbackRoute: Route = getFromResource("app/index.html")

  private def appRoute = {
    pathPrefixLabeled("", "index") {
      pathEndOrSingleSlash {
        fallbackRoute
      }
    } ~
    getFromResourceDirectory("app") ~ getFromResourceDirectory("static")
  }

  val resourceRoutes: Route = appRoute
}
