package ch.seidel.kutu.http

import akka.http.scaladsl.server.RouteConcatenation
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.headers.RawHeader

trait ApiService extends RouteConcatenation
    with WertungenRoutes
//    with WebSockets
    with ResourceService {

//  private implicit lazy val _ = ch.seidel.kutu.http.Core.system.dispatcher

  lazy val allroutes =
      wertungenRoutes ~
      resourceRoutes ~
//      websocket ~
      complete(StatusCodes.NotFound)
}
