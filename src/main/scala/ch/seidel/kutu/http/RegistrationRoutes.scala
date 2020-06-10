package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.{KutuService, NewRegistration, Registration}
import ch.seidel.kutu.renderer.PrintUtil

import scala.concurrent.duration.DurationInt

trait RegistrationRoutes extends SprayJsonSupport with JwtSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {

  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

  lazy val registrationRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("registrations" / JavaUUID) { competitionId =>
        val wettkampf = readWettkampf(competitionId.toString)
        val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
        val logofile = PrintUtil.locateLogoFile(logodir)
        pathEndOrSingleSlash {
          get { // list Vereinsregistration
            complete(selectRegistrations)
          } ~ post { // create Vereinsregistration
            entity(as[NewRegistration]) { newRegistration =>
              val registration = createRegistration(newRegistration)
              respondWithJwtHeader(registration.id + "") {
                complete(registration)
              }
            }
          }
        } ~ path(LongNumber) { registrationId =>
          authenticated() { userId =>
            if (extractRegistrationId(userId).exists(id => id == registrationId)) {
              respondWithJwtHeader(registrationId + "") {
                pathEndOrSingleSlash {
                  get {
                    complete(selectRegistration(registrationId))
                  } ~
                  put { // update Vereinsregistration
                    entity(as[Registration]) { registration =>
                      complete(updateRegistration(registration))
                    }
                  } ~ delete { // delete  Vereinsregistration
                    deleteRegistration(registrationId)
                    complete(StatusCodes.OK)
                  }
                } ~ path("athletes") {
                  pathEndOrSingleSlash {
                    get { // list Athletes
                      complete(
                        HttpEntity(ContentTypes.`application/json`, "")
                      )
                    } ~ post { // create Athletes
                      complete(
                        HttpEntity(ContentTypes.`application/json`, "")
                      )
                    }
                  } ~ path(LongNumber) { id =>
                    pathEndOrSingleSlash {
                      put { // update Athletes
                        complete(
                          HttpEntity(ContentTypes.`application/json`, "")
                        )
                      } ~ delete { // delete  Athletes
                        complete(
                          HttpEntity(ContentTypes.`application/json`, "")
                        )
                      }
                    }
                  }
                }
              } ~ path("judges") {
                pathEndOrSingleSlash {
                  get { // list judges
                    complete(
                      HttpEntity(ContentTypes.`application/json`, "")
                    )
                  } ~ post { // create judges
                    complete(
                      HttpEntity(ContentTypes.`application/json`, "")
                    )
                  }
                } ~ path(LongNumber) { id =>
                  pathEndOrSingleSlash {
                    put { // update judges
                      complete(
                        HttpEntity(ContentTypes.`application/json`, "")
                      )
                    } ~ delete { // delete  judges
                      complete(
                        HttpEntity(ContentTypes.`application/json`, "")
                      )
                    }
                  }
                }
              }
            } else {
              complete(StatusCodes.Forbidden)
            }
          }
        }
      }
    }
  }
}
