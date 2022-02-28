package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, RouteConcatenation}
import ch.seidel.kutu.domain.toDurationFormat
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.pathPrefixLabeled



trait ApiService extends RouteConcatenation with CIDSupport with RouterLogging with IpToDeviceID
  with LoginRoutes
  with WertungenRoutes
  with WettkampfRoutes
  with ScoreRoutes
  with ReportRoutes
  with RegistrationRoutes
  //    with WebSockets
  with ResourceService
  with MetricsController {

  //  private implicit lazy val _ = ch.seidel.kutu.http.Core.system.dispatcher
  import AbuseHandler._

  def allroutes(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = {
    def myExceptionHandler: ExceptionHandler = ExceptionHandler {
      case e: Exception =>
        (handleCID & extractUri) { (clientId: String, uri: Uri) =>
          log.error(handleExceptionAbuse(e, clientId, uri))
          complete(HttpResponse(InternalServerError, entity = "Bad Request"))
        }
    }
    val standardRoutes = resourceRoutes ~
      pathPrefixLabeled("api", "api") {
        login(userLookup, userIdLookup) ~
          wertungenRoutes ~
          wettkampfRoutes ~
          scoresRoutes ~
          reportRoutes ~
          registrationRoutes ~
          //      websocket
          complete (StatusCodes.NotFound)
      } ~ metricsroute ~ fallbackRoute

    handleExceptions(myExceptionHandler) {
      extractClientIP { ip =>
        extractUri { uri =>
          handleCID { clientId: String =>
            findAbusedClient(clientId, uri) match {
              case Some(AbuseCounter(counter, lastSeen)) =>
                log.warning(s"abused request from $clientId to $uri. $counter times failed, last exception since ${toDurationFormat(lastSeen, System.currentTimeMillis())}")
                //toAbuseMap(clientId, uri, counter)
                complete(StatusCodes.NotFound)
              case None => standardRoutes
            }
          }
        }
      }
    }
  }
}
