package ch.seidel.kutu.http

import akka.http.scaladsl.server.RouteConcatenation
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.headers.RawHeader

trait ApiService extends RouteConcatenation
    with LoginRoutes
    with WertungenRoutes
    with WettkampfRoutes
//    with WebSockets
    with ResourceService {

//  private implicit lazy val _ = ch.seidel.kutu.http.Core.system.dispatcher

  def allroutes(userLookup: (String) => String) =
      resourceRoutes ~
      pathPrefix("api") {
        login(userLookup) ~
        wertungenRoutes ~
        wettkampfRoutes
//      websocket
      } ~
      complete(StatusCodes.NotFound)
}
