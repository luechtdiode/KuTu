package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.RouteConcatenation
import ch.seidel.kutu.domain.DBService

trait ApiService extends RouteConcatenation
  with LoginRoutes
  with WertungenRoutes
  with WettkampfRoutes
  with ScoreRoutes
  with ReportRoutes
  //    with WebSockets
  with ResourceService
  with DBService {

  //  private implicit lazy val _ = ch.seidel.kutu.http.Core.system.dispatcher

  def allroutes(userLookup: (String) => String) =
    resourceRoutes ~
      pathPrefix("api") {
        login(userLookup) ~
        wertungenRoutes ~
        wettkampfRoutes ~
        scoresRoutes ~
        reportRoutes
        //      websocket
      } ~
      complete(StatusCodes.NotFound)
}
