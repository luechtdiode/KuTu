package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, RouteConcatenation}
import ch.seidel.kutu.domain.DBService

trait ApiService extends RouteConcatenation with CIDSupport with RouterLogging
  with LoginRoutes
  with WertungenRoutes
  with WettkampfRoutes
  with ScoreRoutes
  with ReportRoutes
  with RegistrationRoutes
  //    with WebSockets
  with ResourceService {

  //  private implicit lazy val _ = ch.seidel.kutu.http.Core.system.dispatcher

  def allroutes(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = {
    def myExceptionHandler: ExceptionHandler = ExceptionHandler {
      case e: Exception =>
        (handleCID & extractUri) { (clientId: String, uri: Uri) =>
          log.error(e, s"Request from $clientId to $uri could not be handled normally")
          complete(HttpResponse(InternalServerError, entity = "Bad Request"))
        }
    }

    handleExceptions(myExceptionHandler) {
      resourceRoutes ~
      pathPrefix("api") {
        login(userLookup, userIdLookup) ~
          wertungenRoutes ~
          wettkampfRoutes ~
          scoresRoutes ~
          reportRoutes ~
          registrationRoutes ~
        //      websocket
        complete(StatusCodes.NotFound)
      } ~ fallbackRoute
    }
  }
}
