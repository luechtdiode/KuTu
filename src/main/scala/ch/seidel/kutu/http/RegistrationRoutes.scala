package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.{AthletRegistration, KutuService, NewRegistration, ProgrammRaw, Registration, Verein}
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.view.WettkampfInfo

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
        } ~ path("programmlist") {
          get {
            complete {
              val wi = WettkampfInfo(wettkampf.toView(readProgramm(wettkampf.programmId)), this)
              wi.leafprograms.map(p => ProgrammRaw(p.id, p.name, 0, 0, p.ord, p.alterVon, p.alterBis))
            }
          }
        } ~ pathPrefix(LongNumber) { registrationId =>
          authenticated() { userId =>
            if (userId.equals(competitionId.toString)) {
              // approve registration - means assign verein-id if missing, and apply registrations
              // This is made from the FX-Client
              // 1. get all
              pathPrefix("athletes") {
                pathEndOrSingleSlash {
                  get { // list Athletes
                    complete(
                      selectAthletRegistrations(registrationId) // also judges
                    )
                  }
                }
              }
            } else if (extractRegistrationId(userId).contains(registrationId)) {
              respondWithJwtHeader(registrationId + "") {
                pathEndOrSingleSlash {
                  get {
                    complete(selectRegistration(registrationId))
                  } ~ put { // update Vereinsregistration
                    entity(as[Registration]) { registration =>
                      complete(updateRegistration(registration))
                    }
                  } ~ delete { // delete  Vereinsregistration
                    deleteRegistration(registrationId)
                    complete(StatusCodes.OK)
                  }
                } ~ pathPrefix("athletlist") {
                  pathEndOrSingleSlash {
                    get { // list Athletes
                      complete {
                        val reg = selectRegistration(registrationId)
                        selectAthletesView(Verein(reg.vereinId.getOrElse(0), reg.vereinname, Some(reg.verband)))
                      }
                    }
                  }
                } ~ pathPrefix("athletes") {
                  pathEndOrSingleSlash {
                    get { // list Athletes
                      complete(
                        selectAthletRegistrations(registrationId)
                      )
                    } ~ post { // create Athletes
                      log.info("post athletreistration")
                      entity(as[AthletRegistration]) { athletRegistration =>
                        complete(
                          createAthletRegistration(athletRegistration)
                        )
                      }
                    }
                  } ~ pathPrefix(LongNumber) { id =>
                    pathEndOrSingleSlash {
                      get {
                        complete(
                          selectAthletRegistration(id)
                        )
                      } ~ put { // update Athletes
                        entity(as[AthletRegistration]) { athletRegistration =>
                          complete(
                            updateAthletRegistration(athletRegistration)
                          )
                        }
                      } ~ delete { // delete  Athletes
                        deleteAthletRegistration(id)
                        complete(StatusCodes.OK)
                      }
                    }
                  }
                }
              } ~ pathPrefix("judges") {
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
                } ~ pathPrefix(LongNumber) { id =>
                  pathEndOrSingleSlash {
                    get {
                      complete(
                        HttpEntity(ContentTypes.`application/json`, "")
                      )
                    } ~ put { // update judges
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
