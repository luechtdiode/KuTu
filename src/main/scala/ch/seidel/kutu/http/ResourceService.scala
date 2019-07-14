package ch.seidel.kutu.http

import akka.http.scaladsl.server.Directives

trait ResourceService extends Directives {
  val fallbackRoute = getFromResource("app/index.html")

  def appRoute = {
    pathPrefix("") {
      pathEndOrSingleSlash {
        fallbackRoute
      }
    } ~
    getFromResourceDirectory("app")
  }

  val resourceRoutes = appRoute
}
