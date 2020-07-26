package ch.seidel.kutu.http

import spray.json._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.{ByteString, Timeout}
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.remoteAdminBaseUrl
import ch.seidel.kutu.domain.{AthletRegistration, KutuService, NewRegistration, ProgrammRaw, Registration, Verein, Wettkampf}
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.view.{RegistrationAdmin, WettkampfInfo}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

trait RegistrationRoutes extends SprayJsonSupport with JwtSupport with JsonSupport with AuthSupport with RouterLogging with KutuService with IpToDeviceID {

  import Core._
  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  // Required by the `ask` (?) method below
  // usually we'd obtain the timeout from the system's configuration
  private implicit lazy val timeout: Timeout = Timeout(5.seconds)


  def joinVereinWithRegistration(p: Wettkampf, reg: Registration, verein: Verein): Unit = {
    Await.result(httpPutClientRequest(
      s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/${reg.id}",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(verein.toJson.compactPrint)
      )), Duration.Inf)
  }

  def getRegistrations(p: Wettkampf): List[Registration] = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) => Unmarshal(entity).to[List[Registration]]
      case _ => Future(List[Registration]())
    }
    , Duration.Inf)

  def getAthletRegistrations(p: Wettkampf, r: Registration): List[AthletRegistration] = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/${r.id}/athletes").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) => Unmarshal(entity).to[List[AthletRegistration]]
      case _ => Future(List[AthletRegistration]())
    }
    , Duration.Inf)

  def getAllRegistrationsRemote(p: Wettkampf): Map[Registration, List[AthletRegistration]] = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[List[Registration]].map {
          registrations: List[Registration] =>
            (for (r <- registrations) yield {
              r -> getAthletRegistrations(p, r)
            }).toMap
        }
      case _ => Future(Map[Registration, List[AthletRegistration]]())
    }
    , Duration.Inf)

  lazy val registrationRoutes: Route = {
    extractClientIP { ip =>
      pathPrefix("registrations" / JavaUUID) { competitionId =>
        val wettkampf = readWettkampf(competitionId.toString)
        val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
        val logofile = PrintUtil.locateLogoFile(logodir)
        pathEndOrSingleSlash {
          get { // list Vereinsregistration
            complete(selectRegistrationsOfWettkampf(competitionId))
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
        } ~ path("syncactions") {
          get {
            val wi = WettkampfInfo(wettkampf.toView(readProgramm(wettkampf.programmId)), this)
            complete {
              RegistrationAdmin.computeSyncActions(wi, this)
                //.map(synclist => synclist.map(sa => sa_.caption))
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
              } ~ put {
                entity(as[Verein]) { verein =>
                  val registration = selectRegistration(registrationId)
                  if (registration.vereinId.isEmpty) {
                    selectVereine.find(v => v.name.equals(verein.name) && (v.verband.isEmpty || v.verband.equals(verein.verband))) match {
                      case Some(v) => complete(updateRegistration(registration.copy(vereinId = Some(v.id))))
                      case None =>
                        val insertedVerein = insertVerein(verein)
                        complete(updateRegistration(registration.copy(vereinId = Some(insertedVerein.id))))
                    }
                  } else {
                    complete(StatusCodes.Conflict)
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
                      if (selectRegistration(registrationId).vereinId.equals(registration.vereinId)) {
                        complete(updateRegistration(registration))
                      } else {
                        complete(StatusCodes.Conflict)
                      }
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
