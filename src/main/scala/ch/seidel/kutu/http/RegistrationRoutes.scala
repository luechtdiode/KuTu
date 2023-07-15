package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.{ByteString, Timeout}
import ch.seidel.kutu.{Config, domain}
import ch.seidel.kutu.Config.remoteAdminBaseUrl
import ch.seidel.kutu.akka._
import ch.seidel.kutu.data.RegistrationAdmin.adjustWertungRiegen
import ch.seidel.kutu.domain.{AthletRegistration, AthletView, JudgeRegistration, KutuService, NewRegistration, ProgrammRaw, Registration, RegistrationResetPW, Verein, Wettkampf, dateToExportedStr, encodeFileName}
import ch.seidel.kutu.http.AuthSupport.OPTION_LOGINRESET
import ch.seidel.kutu.renderer.MailTemplates.createPasswordResetMail
import ch.seidel.kutu.renderer.{CompetitionsClubsToHtmlRenderer, CompetitionsJudgeToHtmlRenderer, PrintUtil}
import ch.seidel.kutu.view.WettkampfInfo
import spray.json._

import java.util.UUID
import scala.concurrent.duration.{DAYS, Duration, DurationInt}
import scala.concurrent.{Await, Future}
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

trait RegistrationRoutes extends SprayJsonSupport with JwtSupport with JsonSupport with AuthSupport with RouterLogging
  with KutuService with CompetitionsClubsToHtmlRenderer with CompetitionsJudgeToHtmlRenderer with IpToDeviceID with CIDSupport {

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

  def updateVereinRemote(p: Wettkampf, vereinToUpdate: Option[Verein]): Unit = {
    vereinToUpdate.foreach(verein => Await.result(httpPutClientRequest(
      s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/verein",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(verein.toJson.compactPrint)
      )), Duration.Inf))
  }

  def updateRemoteAthletes(p: Wettkampf, athleteRemoteUpdates: List[AthletView]): Unit = {
    Await.result(httpPutClientRequest(
      s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/athletes",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(athleteRemoteUpdates.toJson.compactPrint)
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

  def getAllJudgesRemote(p: Wettkampf): List[(Registration,List[JudgeRegistration])] = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/judges").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[List[(Registration,List[JudgeRegistration])]]
      case _ => Future(List.empty)
    }
    , Duration.Inf)


  def getAllJudgesHTMLRemote(p: Wettkampf): String = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/judges?html").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[String]
      case _ => Future("")
    }
    , Duration.Inf)

  def getAllRegistrationsHtmlRemote(p: Wettkampf): String = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}?html").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[String]

      case _ => Future("")
    }
    , Duration.Inf)

  lazy val registrationRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      pathPrefixLabeled("registrations" / "clubnames", "registration/clubnames") {
        pathEndOrSingleSlash {
          get {
            val since1Year = System.currentTimeMillis() - Duration(500, DAYS).toMillis
            complete(selectRegistrations()
              .filter(_.registrationTime > since1Year)
              .map(r => r.toVerein).distinct
              .sortBy(v => v.easyprint))
          }
        }
      } ~ pathPrefixLabeled("registrations" / JavaUUID, "registrations/:competition-id") { competitionId =>
        import AbuseHandler._
        if (!wettkampfExists(competitionId.toString)) {
          log.error(handleAbuse(clientId, uri))
          complete(StatusCodes.NotFound)
        } else {
          val wettkampf = readWettkampf(competitionId.toString)
          val logodir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
          val logofile = PrintUtil.locateLogoFile(logodir)
          pathEndOrSingleSlash {
            authenticated(true) { id =>
              get {
                parameters(Symbol("html").?) {
                  case None =>
                    complete(selectRegistrationsOfWettkampf(competitionId))
                  case _ =>
                    complete(ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`, toHTMLasClubRegistrationsList(wettkampf, selectRegistrationsOfWettkampf(competitionId), logofile))))
                }
              }
            } ~ get { // list Vereinsregistration
              parameters(Symbol("html").?) {
                case None =>
                  complete(selectRegistrationsOfWettkampf(competitionId).map(_.toPublicView))
                case _ =>
                  complete(ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`, toHTMLasClubRegistrationsList(wettkampf, selectRegistrationsOfWettkampf(competitionId).map(_.toPublicView), logofile))))
              }
            } ~ post { // create Vereinsregistration
              entity(as[NewRegistration]) { newRegistration =>
                val registration = createRegistration(newRegistration)
                respondWithJwtHeader(s"${registration.id}") {
                  complete(registration)
                }
              }
            }
          } ~ pathLabeled("approvemail", "approvemail") {
            get {
              parameters(Symbol("mail").?) {
                case Some(mail) =>
                  complete{
                    CompetitionRegistrationClientActor.publish(ApproveEMail(wettkampf.uuid.get, mail), clientId).map{ _ =>
                      s"${wettkampf.easyprint} EMail ${mail} verifiziert"
                    }
                  }
                case _ =>
                  complete {Future {
                    ""
                  }}
              }
            }
          } ~ pathLabeled("programmlist", "programmlist") {
            get {
              complete {
                val wi = WettkampfInfo(wettkampf.toView(readProgramm(wettkampf.programmId)), this)
                wi.leafprograms.map(p => ProgrammRaw(p.id, p.name, p.aggregate, p.parent.map(_.id).getOrElse(0), p.ord, p.alterVon, p.alterBis, p.uuid, p.riegenmode))
              }
            }
          } ~ pathLabeled("refreshsyncs", "refreshsyncs") {
            get {
              complete(
                AthletIndexActor.publish(ResyncIndex).map { _ =>
                  CompetitionRegistrationClientActor.publish(RegistrationResync(wettkampf.uuid.get), clientId)
                  StatusCodes.OK
                })
            }
          } ~ pathLabeled("syncactions", "syncactions") {
            get {
              withRequestTimeout(60.seconds) {
                complete(CompetitionRegistrationClientActor.publish(AskRegistrationSyncActions(wettkampf.uuid.get), clientId).map {
                  case RegistrationSyncActions(actions) => actions.toVector
                  case _ => Vector()
                })
              }
            }
          } ~ pathPrefixLabeled("programmdisziplinlist", "programmdisziplinlist") {
            pathEndOrSingleSlash {
              get {
                complete(listJudgeRegistrationProgramItems(readWettkampfLeafs(wettkampf.programmId).map(p => p.id)))
              }
            }
          } ~ pathPrefixLabeled("judges", "judges") {
            authenticated() { userId =>
              pathEndOrSingleSlash {
                get { // list Judges per club
                  parameters(Symbol("html").?) {
                    case None =>
                      complete(Future {
                        loadAllJudgesOfCompetition(wettkampf.uuid.map(UUID.fromString).get).toList
                      })
                    case _ =>
                      complete(Future {
                        toHTMLasJudgeRegistrationsList(wettkampf, loadAllJudgesOfCompetition(wettkampf.uuid.map(UUID.fromString).get), logofile)
                      })
                  }
                }
              }
            }
          } ~ pathPrefixLabeled("verein", "verein") {
            pathEndOrSingleSlash {
              authenticated() { userId =>
                if (userId.equals(competitionId.toString)) {
                  put {
                    entity(as[Verein]) { verein =>
                      updateVerein(verein)
                      log.info(s"SyncAdmin $clientId - verein updated: $verein")
                      CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                      complete(StatusCodes.OK)
                    }
                  }
                } else
                  complete(StatusCodes.Conflict)
              }
            }
          } ~ pathPrefixLabeled("athletes", "athletes") {
            pathEndOrSingleSlash {
              authenticated() { userId =>
                if (userId.equals(competitionId.toString)) {
                  put {
                    entity(as[List[AthletView]]) { athletlist =>
                      val sexchanges = athletlist
                        .map(a => (a, loadAthlet(a.id).get))
                        .filter(aa => aa._1.geschlecht != aa._2.geschlecht)
                      val reg = insertAthletes(athletlist.map(aw => (aw.id.toString, aw.toAthlet)))
                      sexchanges.foreach(aa => adjustWertungRiegen(wettkampf, this, aa._1.toAthlet))
                      athletlist.foreach(aa => adjustOnlineRegistrations(aa))
                      AthletIndexActor.publish(ResyncIndex)
                      CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wettkampf.uuid.get), clientId)
                      CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                      log.info(s"SyncAdmin $clientId - athletes updated and riegenwertungen sex adjusted: $athletlist")
                      complete(StatusCodes.OK)
                    }
                  }
                } else
                  complete(StatusCodes.Conflict)
              }
            }
          } ~ pathPrefixLabeled(LongNumber / "loginreset", ":registration-id/loginreset") { registrationId =>
            (authenticated() & extractHost & optionalHeaderValueByName("Referer")) { (loginresetToken, host, refererOption) =>
              if (loginresetToken.endsWith(OPTION_LOGINRESET)) {
                val userId = loginresetToken.substring(0, loginresetToken.length - OPTION_LOGINRESET.length)
                if (extractRegistrationId(userId).contains(registrationId)) {
                  val decodedorigin = refererOption.map(r => r.substring(0, r.indexOf("/registration"))).getOrElse(s"https://$host")
                  val wkid: String = wettkampf.uuid.get
                  val registration = selectRegistration(registrationId)
                  val resetLoginQuery = createOneTimeResetRegistrationLoginToken(wkid, registrationId)
                  val link = s"$decodedorigin/registration/${wkid}/${registrationId}?$resetLoginQuery"
                  complete(
                    KuTuMailerActor.send(createPasswordResetMail(wettkampf, registration, link))
                  )
                } else {
                  complete(StatusCodes.Forbidden)
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          } ~ pathPrefixLabeled(LongNumber, ":registration-id") { registrationId =>
            authenticated() { userId =>
              if (userId.equals(competitionId.toString)) {
                // approve registration - means assign verein-id if missing, and apply registrations
                // This is made from the FX-Client
                // 1. get all
                pathPrefixLabeled("athletes", "athletes") {
                  pathEndOrSingleSlash {
                    get { // list Athletes
                      complete(
                        selectAthletRegistrations(registrationId)
                      )
                    }
                  }
                } ~ put {
                  entity(as[Verein]) { verein =>
                    val registration = selectRegistration(registrationId)
                    if (registration.vereinId.isEmpty) {
                      log.info(s"SyncAdmin $clientId - neuer Verein zu Vereinsregistration wird angelegt: $verein")
                      selectVereine.find(v => v.name.equals(verein.name) && (v.verband.isEmpty || v.verband.equals(verein.verband))) match {
                        case Some(v) => complete(updateRegistration(registration.copy(vereinId = Some(v.id))))
                        case None => complete(Future {
                          val insertedVerein = insertVerein(verein)
                          val reg = updateRegistration(registration.copy(vereinId = Some(insertedVerein.id)))
                          CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                          reg
                        })
                      }
                    } else {
                      complete(StatusCodes.Conflict)
                    }
                  }
                }
              } else if (extractRegistrationId(userId).contains(registrationId)) {
                respondWithJwtHeader(s"$registrationId") {
                  pathEndOrSingleSlash {
                    get {
                      complete(selectRegistration(registrationId))
                    } ~ put { // update Vereinsregistration
                      entity(as[Registration]) { registration =>
                        if (selectRegistration(registrationId).vereinId.equals(registration.vereinId)) {
                          complete(Future {
                            val reg = updateRegistration(registration)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                            log.info(s"$clientId: Vereinsregistration aktualisiert: ${registration}")
                            reg
                          })
                        } else {
                          complete(StatusCodes.Conflict)
                        }
                      }
                    } ~ delete { // delete  Vereinsregistration
                      complete(Future {
                        deleteRegistration(registrationId)
                        CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                        log.info(s"$clientId: Vereinsregistration gelöscht: ${registrationId}")
                        StatusCodes.OK
                      })
                    }
                  } ~ pathPrefixLabeled("pwchange", "pwchange") {
                    pathEndOrSingleSlash {
                      put { // update/reset Password
                        entity(as[RegistrationResetPW]) { regPwReset =>
                          if (selectRegistration(registrationId).id.equals(regPwReset.id)) {
                            val registration = resetRegistrationPW(regPwReset)
                            log.info(s"$clientId: Passwort geändert")
                            respondWithJwtHeader(s"${registration.id}") {
                              complete(registration)
                            }
                          } else {
                            complete(StatusCodes.Conflict)
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("copyfrom", "copyfrom") {
                    pathEndOrSingleSlash {
                      put {
                        entity(as[Wettkampf]) { wettkampfCopyFrom =>
                          complete {
                            copyClubRegsFromCompetition(wettkampfCopyFrom.uuid.get, registrationId)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                            log.info(s"$clientId: Anmeldungen kopiert: von ${wettkampfCopyFrom.easyprint} nach ${registrationId}")
                            StatusCodes.OK
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("athletlist", "athletlist") {
                    pathEndOrSingleSlash {
                      get { // list Athletes
                        complete {
                          val reg = selectRegistration(registrationId)
                          if (reg.vereinId.isEmpty) {
                            List[AthletRegistration]()
                          } else {
                            val existingAthletRegs = selectAthletRegistrations(registrationId)
                            val pgm = readWettkampfLeafs(wettkampf.programmId).head
                            selectAthletesView(Verein(reg.vereinId.getOrElse(0), reg.vereinname, Some(reg.verband)))
                              .filter(_.activ)
                              .filter(r => (pgm.aggregate == 1 && pgm.riegenmode == 1) || !existingAthletRegs.exists { er =>
                                er.athletId.contains(r.id) || (er.name == r.name && er.vorname == r.vorname)
                              })
                              .map {
                                case a@AthletView(id, _, geschlecht, name, vorname, gebdat, _, _, _, _, _) =>
                                  AthletRegistration(0L, reg.id, Some(id), geschlecht, name, vorname, gebdat.map(dateToExportedStr).getOrElse(""), 0L, 0, Some(a), None)
                              }
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("athletes", "athletes") {
                    pathEndOrSingleSlash {
                      get { // list Athletes
                        complete(
                          selectAthletRegistrations(registrationId)
                        )
                      } ~ post { // create Athletes
                        log.info("post athletregistration")
                        entity(as[AthletRegistration]) { athletRegistration =>
                          complete {
                            val x: Option[AthletView] = athletRegistration.athletId.map(loadAthleteView)
                            if (athletRegistration.athletId.isDefined && x.isEmpty) {
                              StatusCodes.BadRequest
                            } else {
                              val reg = selectRegistration(registrationId)
                              if (x.isEmpty || x.map(_.verein).flatMap(_.map(_.id)).equals(reg.vereinId)) {
                                try {
                                  val reg = createAthletRegistration(athletRegistration)
                                  CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                  log.info(s"$clientId: Athletanmeldung angelegt: ${athletRegistration.toPublicView.easyprint}")
                                  reg
                                } catch {
                                  case e: IllegalArgumentException =>
                                    log.error(e.getMessage)
                                    StatusCodes.Conflict
                                }
                              } else {
                                StatusCodes.BadRequest
                              }
                            }
                          }
                        }
                      }
                    } ~ pathPrefixLabeled(LongNumber, ":athlet-id") { id =>
                      pathEndOrSingleSlash {
                        get {
                          complete(
                            selectAthletRegistration(id)
                          )
                        } ~ put { // update Athletes
                          entity(as[AthletRegistration]) { athletRegistration =>
                            complete(Future {
                              val reg = updateAthletRegistration(athletRegistration)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                              log.info(s"$clientId: Athletanmeldung aktualisiert: ${athletRegistration.toPublicView.easyprint}")
                              reg
                            })
                          }
                        } ~ delete { // delete  Athletes
                          complete(Future {
                            deleteAthletRegistration(id)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                            log.info(s"$clientId: Athletanmeldung gelöscht: ${id}")
                            StatusCodes.OK
                          })
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("judges", "judges") {
                    pathEndOrSingleSlash {
                      get { // list judges
                        complete(
                          selectJudgeRegistrations(registrationId)
                        )
                      } ~ post { // create judges
                        log.info("post judgesregistration")
                        entity(as[JudgeRegistration]) { judgeRegistration =>
                          complete {
                            try {
                              val reg = createJudgeRegistration(judgeRegistration)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                              log.info(s"$clientId: WR-Anmeldung angelegt: ${judgeRegistration.easyprint}")
                              reg
                            } catch {
                              case e: IllegalArgumentException =>
                                log.error(e.getMessage)
                                StatusCodes.Conflict
                            }
                          }
                        }
                      }
                    } ~ pathPrefixLabeled(LongNumber, ":judgereg-id") { id =>
                      pathEndOrSingleSlash {
                        get {
                          complete(
                            selectJudgeRegistration(id)
                          )
                        } ~ put { // update judges
                          entity(as[JudgeRegistration]) { judgesRegistration =>
                            complete(Future {
                              val reg = updateJudgeRegistration(judgesRegistration)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                              log.info(s"$clientId: WR-Anmeldung aktualisiert: ${judgesRegistration.easyprint}")
                              reg
                            })
                          }
                        } ~ delete { // delete  judges
                          complete(Future {
                            deleteJudgeRegistration(id)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                            log.info(s"$clientId: WR-Anmeldung gelöscht: ${id}")
                            StatusCodes.OK
                          })
                        }
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
}
