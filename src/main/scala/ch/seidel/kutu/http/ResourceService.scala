package ch.seidel.kutu.http

import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.server.Directives

trait ResourceService extends Directives {
  val fallbackRoute = getFromResource("app/index.html")

  def appRoute = {
    pathPrefixLabeled("", "index") {
      pathEndOrSingleSlash {
        fallbackRoute
      }
    } ~
    getFromResourceDirectory("app") ~ getFromResourceDirectory("static")
  }

  val resourceRoutes = appRoute
}
