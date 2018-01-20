package ch.seidel.kutu.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorLogging

trait WertungenRoutes extends JsonSupport with JwtSupport with RouterLogging {
  // Required by the `ask` (?) method below
  private implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val eventRoutes: Route = {
    pathPrefix("api" / "events") {
      pathEnd {
        get { ???
//          val events: Future[Events] =
//            (eventRegistryActor ? GetEvents).mapTo[Events]
//          complete(events)
        } ~ post { ???
//          authenticated { userid =>
//            entity(as[Event]) { event =>
//              val eventCreated: Future[ActionPerformed] =
//                (eventRegistryActor ? CreateEvent(event)).mapTo[ActionPerformed]
//              onSuccess(eventCreated) { performed =>
//                log.info("Created event [{}]: {}", performed.event, performed.description)
//                complete((StatusCodes.Created, performed))
//              }
//            }
//          }
        }

      } ~
        //#events-get-post
        //#events-get-delete
        path(LongNumber) { id =>
          pathEnd {
            get { ???
              //#retrieve-event-info
//              val maybeEvent: Future[Option[Event]] =
//                (eventRegistryActor ? GetEvent(id)).mapTo[Option[Event]]
//              rejectEmptyResponse {
//                complete(maybeEvent)
//              }
              //#retrieve-event-info
            } ~ delete { ???
//              authenticated { userid =>
//                //#events-delete-logic
//                val eventDeleted: Future[ActionPerformed] =
//                  (eventRegistryActor ? DeleteEvent(id)).mapTo[ActionPerformed]
//                onSuccess(eventDeleted) { performed =>
//                  log.info("Deleted event [{}]: {}", id, performed.description)
//                  complete((StatusCodes.OK, performed))
//                }
//                //#events-delete-logic
//              }
            } ~ put { ???
//              authenticated { userid =>
//                entity(as[Event]) { event =>
//                  val eventCreated: Future[ActionPerformed] =
//                    (eventRegistryActor ? UpdateEvent(event)).mapTo[ActionPerformed]
//                  onSuccess(eventCreated) { performed =>
//                    log.info("Created event [{}]: {}", performed.event, performed.description)
//                    complete((StatusCodes.Created, performed))
//                  }
//                  //#events-put-logic
//                }
//              }
            }
          }
        }
    }
  }
}
