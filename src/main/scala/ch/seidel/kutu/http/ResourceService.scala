package ch.seidel.kutu.http

import akka.http.scaladsl.server.Directives

trait ResourceService extends Directives {

  def appRoute = {
    pathPrefix("") {
      pathEndOrSingleSlash {
        getFromResource("app/index.html")
      }
    } ~
      getFromResourceDirectory("app")
  }

  val resourceRoutes = appRoute

}
