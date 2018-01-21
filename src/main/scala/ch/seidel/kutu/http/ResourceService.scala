package ch.seidel.kutu.http

import akka.http.scaladsl.server.Directives

trait ResourceService extends Directives {

  def appRoute = {
    pathPrefix("") {
      pathEndOrSingleSlash {
        getFromResource("app/resultcatcher/dist/index.html")
      }
    } ~
      getFromResourceDirectory("app/resultcatcher/dist")
  }

  val resourceRoutes = appRoute

}
