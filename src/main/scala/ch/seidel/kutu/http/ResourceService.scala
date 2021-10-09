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
    path("robots.txt") {
      complete(
        """
          |User-agent: *
          |Disallow: /
          |""".stripMargin)
    } ~
    getFromResourceDirectory("app")
  }

  val resourceRoutes = appRoute
}
