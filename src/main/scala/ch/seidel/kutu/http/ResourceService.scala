package ch.seidel.kutu.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

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
