package ch.seidel.kutu.http

import ch.seidel.kutu.domain.toDurationFormat
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.pathPrefixLabeled
import org.apache.pekko.http.scaladsl.model.StatusCodes.*
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.{ExceptionHandler, RouteConcatenation}


case class HTTPFailure(status: StatusCode,
                       private val message: String = "",
                       private val cause: Throwable = None.orNull) extends RuntimeException(cause) {
  val text =
    if message.isEmpty || status.reason().equals(message) then s"Status: $status"
    else s"Status: $status, $message"
  override def getMessage: String = text
}

case object EmptyResponse {
  def apply() = HttpResponse(StatusCodes.NoContent, Seq.empty)
}

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
  import AbuseHandler.*

  def allroutes(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = {
    def myExceptionHandler: ExceptionHandler = ExceptionHandler {
      case e: Exception =>
        (handleCID & extractUri & authenticatedId) { (clientId: String, uri: Uri, authId: Option[String]) =>
          authId match {
            case None =>
              // just unauthenticated requests should lead to abused clients
              log.error(handleExceptionAbuse(e, clientId, uri))
            case Some(user) =>
              log.error(s"Error handling request from authenticated $user, $clientId, $uri: {}", e.toString)
          }
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
      (extractClientIP & extractUri & handleCID & authenticatedId) { (ip, uri, clientId: String, user: Option[String]) =>
        user match {
          case Some(_) => standardRoutes
          case None =>
            findAbusedClient(clientId, uri) match {
              case Some(AbuseCounter(_, counter, lastSeen)) =>
                log.warning(s"abused request from $clientId to $uri. $counter times failed, last exception since ${toDurationFormat(lastSeen, System.currentTimeMillis())}")
                complete(StatusCodes.NotFound)
              case None => standardRoutes
            }
        }
      }
    }
  }
}
