package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.remoteAdminBaseUrl
import ch.seidel.kutu.actors.*
import ch.seidel.kutu.data.RegistrationAdmin.adjustWertungRiegen
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.AuthSupport.OPTION_LOGINRESET
import ch.seidel.kutu.renderer.MailTemplates.createPasswordResetMail
import ch.seidel.kutu.renderer.{CompetitionsClubsToHtmlRenderer, CompetitionsJudgeToHtmlRenderer, PrintUtil}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.POST
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.directives.FileInfo
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.scaladsl.{Sink, Source, StreamConverters}
import org.apache.pekko.util.{ByteString, Timeout}
import spray.json.*

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DAYS, Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

trait RegistrationRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging
  with KutuService with CompetitionsClubsToHtmlRenderer with CompetitionsJudgeToHtmlRenderer with IpToDeviceID with CIDSupport with FailureSupport {

  import Core.*
  import spray.json.DefaultJsonProtocol.*

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

  def regchanged(p: Wettkampf): Unit = httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/regchanged")

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
          (registrations: List[Registration]) =>
            (for r <- registrations yield {
              r -> getAthletRegistrations(p, r)
            }).toMap
        }
      case _ => Future(Map[Registration, List[AthletRegistration]]())
    }
    , Duration.Inf)

  def getAllJudgesRemote(p: Wettkampf): List[(Registration, List[JudgeRegistration])] = Await.result(
    httpGetClientRequest(s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/judges").flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[List[(Registration, List[JudgeRegistration])]]
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

  def doMediaDownloadRemote(p: Wettkampf, mediaList: List[MediaAdmin]): Unit = {
    val request = withAuthHeader(
      HttpRequest(
        POST,
        uri = s"$remoteAdminBaseUrl/api/registrations/${p.uuid.get}/mediafiles",
        entity = HttpEntity(
          ContentTypes.`application/json`,
          ByteString(mediaList.toJson.compactPrint
          )
        )
      )
    )
    Await.result(httpMediaDownloadRequest(p, mediaList, request), Duration.Inf)
  }

  private def httpMediaDownloadRequest(wettkampf: Wettkampf, mediaList: List[MediaAdmin], request: HttpRequest): Future[Unit] = {
    import Core.*
    val source = Source.single(request, ())
    val requestResponseFlow = Http().superPool[Unit](settings = poolsettings)

    def importData(httpResponse: HttpResponse): Unit = {
      if httpResponse.status.isSuccess() then {
        val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
        ResourceExchanger.importWettkampfMediaFiles(wettkampf, mediaList, is)
      } else {
        httpResponse.entity match {
          case HttpEntity.Strict(_, text) =>
            log.error(text.utf8String)
            throw HTTPFailure(httpResponse.status, text.utf8String)
          case x =>
            log.error(x.toString)
            throw HTTPFailure(httpResponse.status, x.toString)
        }
      }
    }
    source.via(requestResponseFlow)
      .map(responseOrFail)
      .map(_._1)
      .map(importData)
      .runWith(Sink.head)
  }

  lazy val registrationRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, uri: Uri) =>
      pathPrefixLabeled("registrations" / "clubnames", "registration/clubnames") {
        pathEndOrSingleSlash {
          get {
            val since1Year = System.currentTimeMillis() - Duration(500, DAYS).toMillis
            complete(selectRegistrations()
              .filter(_.registrationTime > since1Year)
              .map(r => r.toVerein).distinct
              .sortBy(v => v.easyprint)
              .toJson)
          }
        }
      } ~ pathPrefixLabeled("registrations" / JavaUUID, "registrations/:competition-id") { competitionId =>
        import AbuseHandler.*
        if !wettkampfExists(competitionId.toString) then {
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
                    complete(selectRegistrationsOfWettkampf(competitionId).toJson)
                  case _ =>
                    complete(ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`, toHTMLasClubRegistrationsList(wettkampf, selectRegistrationsOfWettkampf(competitionId), logofile))))
                }
              }
            } ~ get { // list Vereinsregistration
              parameters(Symbol("html").?) {
                case None =>
                  complete(selectRegistrationsOfWettkampf(competitionId).map(_.toPublicView).toJson)
                case _ =>
                  complete(ToResponseMarshallable(HttpEntity(ContentTypes.`text/html(UTF-8)`, toHTMLasClubRegistrationsList(wettkampf, selectRegistrationsOfWettkampf(competitionId).map(_.toPublicView), logofile))))
              }
            } ~ post { // create Vereinsregistration
              entity(as[NewRegistration]) { newRegistration =>
                val registration = createRegistration(newRegistration)
                respondWithJwtHeader(s"${registration.id}") {
                  complete(registration.toJson)
                }
              }
            }
          } ~ pathLabeled("approvemail", "approvemail") {
            get {
              parameters(Symbol("mail").?) {
                case Some(mail) =>
                  complete {
                    CompetitionRegistrationClientActor.publish(ApproveEMail(wettkampf.uuid.get, mail), clientId).map {
                      case EMailApproved(message, success) =>
                        s"${wettkampf.easyprint}: $message"
                      case _ =>
                        "unable to approve - unexpected behavior"
                    }
                  }
                case _ =>
                  complete {
                    Future {
                      "unable to approve without mail"
                    }
                  }
              }
            }
          } ~ pathLabeled("programmlist", "programmlist") {
            get {
              complete {
                val leafprograms = listWettkampfDisziplineViews(wettkampf).map(wd => wd.programm).distinct.sortBy(_.ord)
                leafprograms.map(p => ProgrammRaw(p.id, p.name, p.aggregate, p.parent.map(_.id).getOrElse(0), p.ord, p.alterVon, p.alterBis, p.uuid, p.riegenmode, p.bestOfCount)).toJson
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
                  case RegistrationSyncActions(actions) => actions.toJson(using baseSyncActionListFormat)
                  case _ => List.empty[ch.seidel.kutu.domain.SyncAction].toJson(using baseSyncActionListFormat)
                })
              }
            }
          } ~ pathPrefixLabeled("programmdisziplinlist", "programmdisziplinlist") {
            pathEndOrSingleSlash {
              get {
                complete(listJudgeRegistrationProgramItems(readWettkampfLeafs(wettkampf.programmId).map(p => p.id)).map(_.toList).map(items => items.toJson(using listFormat(using judgeRegistrationProgramItemFormat))))
              }
            }
          } ~ pathPrefixLabeled("judges", "judges") {
            authenticated() { userId =>
              pathEndOrSingleSlash {
                get { // list Judges per club
                  parameters(Symbol("html").?) {
                    case None =>
                      complete(Future {
                        loadAllJudgesOfCompetition(wettkampf.uuid.map(UUID.fromString).get).toJson
                      })
                    case _ =>
                      complete(Future {
                        toHTMLasJudgeRegistrationsList(wettkampf, loadAllJudgesOfCompetition(wettkampf.uuid.map(UUID.fromString).get), logofile)
                      })
                  }
                }
              }
            }
          } ~ pathLabeled("regchanged", "regchanged") {
            pathEndOrSingleSlash {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  get {
                    log.info(s"SyncAdmin $clientId - registration changed - resync ...")
                    CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                    complete(StatusCodes.OK)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~ pathPrefixLabeled("verein", "verein") {
            pathEndOrSingleSlash {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
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
                if userId.equals(competitionId.toString) then {
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
          } ~ pathPrefixLabeled("mediafiles", "mediafiles") {
            pathEndOrSingleSlash {
              authenticated() { userId =>
                if userId.equals(competitionId.toString) then {
                  post {
                    withoutRequestTimeout {
                      entity(as[List[Media]]) { medialist =>
                        log.info(s"SyncAdmin $clientId - serving medialist: $medialist")
                        onComplete(Future {
                          val bos = new ByteArrayOutputStream()
                          ResourceExchanger.exportWettkampfMediaFilesToStream(wettkampf, medialist, bos)
                          HttpEntity(
                            MediaTypes.`application/zip`,
                            bos.toByteArray
                          )
                        }) {
                          case Success(entity) =>
                            complete(entity)
                          case Failure(e) =>
                            log.error(e, s"mediapackage for wettkampf $competitionId cannot be exported: " + e.toString)
                            complete(StatusCodes.BadRequest,
                              s"""Das Media-Package des Wettkampfs ${wettkampf.easyprint}
                                 |konnte wg. einem technischen Fehler nicht vollständig zum Download exportiert werden.
                                 |=>${e.getMessage}""".stripMargin)
                        }
                      }
                    }
                  }
                } else
                  complete(StatusCodes.Conflict)
              }
            }
          } ~ pathPrefixLabeled(LongNumber / "loginreset", ":registration-id/loginreset") { registrationId =>
            (authenticated() & extractHost & optionalHeaderValueByName("Referer")) { (loginresetToken, host, refererOption) =>
              if loginresetToken.endsWith(OPTION_LOGINRESET) then {
                val userId = loginresetToken.substring(0, loginresetToken.length - OPTION_LOGINRESET.length)
                if extractRegistrationId(userId).contains(registrationId) then {
                  val decodedorigin = refererOption.map(r => r.substring(0, r.indexOf("/registration"))).getOrElse(s"https://$host")
                  val wkid: String = wettkampf.uuid.get
                  val registration = selectRegistration(registrationId)
                  val resetLoginQuery = createOneTimeResetRegistrationLoginToken(wkid, registrationId)
                  val link = s"$decodedorigin/registration/$wkid/$registrationId?$resetLoginQuery"
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
              if userId.equals(competitionId.toString) then {
                // approve registration - means assign verein-id if missing, and apply registrations
                // This is made from the FX-Client
                // 1. get all
                pathPrefixLabeled("athletes", "athletes") {
                  pathEndOrSingleSlash {
                    get { // list Athletes
                      complete(
                        selectAthletRegistrations(registrationId).toJson(using listFormat(using athletregistrationFormat))
                      )
                    }
                  }
                } ~ put {
                  entity(as[Verein]) { verein =>
                    val registration = selectRegistration(registrationId)
                    if registration.vereinId.isEmpty then {
                      log.info(s"SyncAdmin $clientId - neuer Verein zu Vereinsregistration wird angelegt: $verein")
                      selectVereine.find(v => v.name.equals(verein.name) && (v.verband.isEmpty || v.verband.equals(verein.verband))) match {
                        case Some(v) => complete(updateRegistration(registration.copy(vereinId = Some(v.id))).toJson)
                        case None => complete(Future {
                          val insertedVerein = insertVerein(verein)
                          val reg = updateRegistration(registration.copy(vereinId = Some(insertedVerein.id)))
                          CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                          reg.toJson
                        })
                      }
                    } else {
                      complete(StatusCodes.Conflict)
                    }
                  }
                }
              } else if extractRegistrationId(userId).contains(registrationId) then {
                respondWithJwtHeader(s"$registrationId") {
                  pathEndOrSingleSlash {
                    get {
                      complete(selectRegistration(registrationId).toJson(using registrationFormat))
                    } ~ put { // update Vereinsregistration
                      entity(as[Registration]) { registration =>
                        if selectRegistration(registrationId).vereinId.equals(registration.vereinId) then {
                          complete(Future {
                            val reg = updateRegistration(registration)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                            log.info(s"$clientId: Vereinsregistration aktualisiert: $registration")
                            reg.toJson(using registrationFormat)
                          })
                        } else {
                          complete(StatusCodes.Conflict)
                        }
                      }
                    } ~ delete { // delete  Vereinsregistration
                      complete(Future {
                        deleteRegistration(registrationId)
                        CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                        log.info(s"$clientId: Vereinsregistration gelöscht: $registrationId")
                        StatusCodes.OK
                      })
                    }
                  } ~ pathPrefixLabeled("pwchange", "pwchange") {
                    pathEndOrSingleSlash {
                      put { // update/reset Password
                        entity(as[RegistrationResetPW]) { regPwReset =>
                          if selectRegistration(registrationId).id.equals(regPwReset.id) then {
                            val registration = resetRegistrationPW(regPwReset)
                            log.info(s"$clientId: Passwort geändert")
                            respondWithJwtHeader(s"${registration.id}") {
                              complete(registration.toJson(using registrationFormat))
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
                            log.info(s"$clientId: Anmeldungen kopiert: von ${wettkampfCopyFrom.easyprint} nach $registrationId")
                            StatusCodes.OK
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("teams", "teams") {
                    pathEndOrSingleSlash {
                      get {
                        complete {
                          if wettkampf.hasTeams then {
                            val registration = selectRegistration(registrationId)
                            val (teamname, teamNumbers) = if wettkampf.teamrule.exists(r => TeamRegel.vereinRegeln.exists(p => r.contains(p))) then
                              (s"${registration.toVerein.extendedprint}", selectAthletRegistrations(registrationId)
                                .flatMap(_.team)
                                .filter(_ > 0).distinct.sorted)
                            else if wettkampf.teamrule.exists(r => TeamRegel.verbandRegeln.exists(p => r.contains(p))) then
                              (s"${registration.verband}", selectRegistrations()
                                .filter(_.verband.equalsIgnoreCase(registration.verband))
                                .flatMap(vereinsReg => selectAthletRegistrations(vereinsReg.id))
                                .flatMap(_.team)
                                .filter(_ > 0).distinct.sorted)
                            else (s"${registration.toVerein.extendedprint}", List.empty)

                            val nextTeamNumber = if teamNumbers.isEmpty then 1 else teamNumbers.max + 1

                            ((1 to nextTeamNumber).toList.map(idx => TeamItem(idx, teamname)) ::: 
                              wettkampf.extraTeams
                                .filter(_.nonEmpty)
                                .zipWithIndex.map(item => TeamItem(item._2 * -1 - 1, item._1))).toJson(using listFormat(using teamFormat))
                          } else {
                            List[TeamItem]().toJson(using listFormat(using teamFormat))
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("athletlist", "athletlist") {
                    pathEndOrSingleSlash {
                      get { // list Athletes
                        complete {
                          val reg = selectRegistration(registrationId)
                            if reg.vereinId.isEmpty then {
                            List[AthletRegistration]().toJson(using listFormat(using athletregistrationFormat))
                          } else {
                            val existingAthletRegs = selectAthletRegistrations(registrationId)
                            val pgm = readWettkampfLeafs(wettkampf.programmId).head

                            val officialCandidates = selectAthletesView(Verein(reg.vereinId.getOrElse(0), reg.vereinname, Some(reg.verband)))
                              .filter(_.activ)
                              .filter(r => (pgm.aggregate == 1 && pgm.riegenmode == 1) || !existingAthletRegs.exists { er =>
                                er.athletId.contains(r.id) || (er.name == r.name && er.vorname == r.vorname)
                              })
                              .map {
                                case a@AthletView(id, _, geschlecht, name, vorname, gebdat, _, _, _, _, _) =>
                                  AthletRegistration(0L, reg.id, Some(id), geschlecht, name, vorname, gebdat.map(dateToExportedStr).getOrElse(""), 0L, 0, Some(a), None, None)
                              }
                            val incompletedCandidates = selectUnaprovedAthletRegistrations(registrationId)
                              .filter(ar => !existingAthletRegs.exists { aro => aro.geschlecht.equals(ar.geschlecht) && aro.name.equals(ar.name) && aro.vorname.equals(ar.vorname) })
                              .map(ar => ar.copy(gebdat = dateToExportedStr(str2SQLDate(ar.gebdat))))
                            val preret = officialCandidates ++ incompletedCandidates.filter { ar =>
                              !officialCandidates.exists { aro => aro.geschlecht.equals(ar.geschlecht) && aro.name.equals(ar.name) && aro.vorname.equals(ar.vorname) }
                            }
                            preret.sortBy(_.toAthlet.easyprint).toJson(using listFormat(using athletregistrationFormat))
                          }
                        }
                      }
                    }
                  } ~ pathPrefixLabeled("athletes", "athletes") {
                      pathEndOrSingleSlash {
                        get { // list Athletes
                          complete(
                            selectAthletRegistrations(registrationId).toJson(using listFormat(using athletregistrationFormat))
                          )
                        } ~ post { // create Athletes
                          log.info("post athletregistration")
                          entity(as[AthletRegistration]) { athletRegistration =>
                            val x: Option[AthletView] = athletRegistration.athletId.filter(_ > 0L).map(loadAthleteView)
                            if athletRegistration.athletId.isDefined && athletRegistration.athletId.get > 0L && x.isEmpty then {
                              complete(StatusCodes.BadRequest)
                            } else {
                              val reg = selectRegistration(registrationId)
                              if x.isEmpty || x.map(_.verein).flatMap(_.map(_.id)).equals(reg.vereinId) then {
                                try {
                                  val isOverride = athletRegistration.athletId match {
                                    case Some(id) if id > 0 =>
                                      Await.result(AthletIndexActor.publish(FindAthletLike(athletRegistration.toAthlet)), Duration.Inf) match {
                                        case AthletLikeFound(_, found) => found.id != id
                                        case _ => false
                                      }
                                    case _ => false
                                  }
                                  if isOverride then {
                                    throw new IllegalArgumentException("Person-Überschreibung in einer Anmeldung zu einer anderen Person ist nicht erlaubt!")
                                  }
                                  val reg = createAthletRegistration(athletRegistration)
                                  CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                  log.info(s"$clientId: Athletanmeldung angelegt: ${reg.toPublicView.easyprint}")
                                  complete(reg.toJson(using athletregistrationFormat))
                                } catch {
                                  case e: IllegalArgumentException =>
                                    log.error(e.getMessage)
                                    complete(StatusCodes.Conflict, e.getMessage)
                                }
                              } else {
                                complete(StatusCodes.BadRequest)
                              }
                            }
                          }
                        }
                      } ~ pathPrefixLabeled(LongNumber, ":athlet-id") { id =>
                        pathPrefixLabeled("mediafile", "mediafile") {
                          pathEndOrSingleSlash {
                            withSizeLimit(10 * 1024 * 1024) {
                              withoutRequestTimeout {
                                get {
                                  val registration = selectAthletRegistration(id)
                                  registration.mediafile.flatMap(mf => loadMedia(mf.id)) match {
                                    case Some(mf) => getFromFile(mf.computeFilePath(wettkampf))
                                    case _ => complete(StatusCodes.Conflict, s"Es ist keine Media-Datei in der Anmeldung verknüpft")
                                  }
                                } ~ delete {
                                  val registration = selectAthletRegistration(id)
                                  log.info(s"$clientId: Mediafile ${registration.mediafile} löschen ...")
                                  registration.mediafile.flatMap(mf => loadMedia(mf.id)) match {
                                    case Some(mf) =>
                                      val cleanedAthletReg = registration.copy(mediafile = None)
                                      ResourceExchanger.updateAthletRegistration(cleanedAthletReg)
                                      mf.computeFilePath(wettkampf).delete()
                                      log.info(s"Mediafile ${registration.mediafile.map(_.name).getOrElse("")} gelöscht")
                                      CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                      complete(StatusCodes.OK, cleanedAthletReg.toJson(using athletregistrationFormat))
                                    case _ => complete(StatusCodes.Conflict, s"Es ist keine Media-Datei in der Anmeldung verknüpft")
                                  }
                                } ~ fileUpload("mediafile") {
                                  case (metadata: FileInfo, file: Source[ByteString, Any]) =>
                                    import Core.materializer
                                    val fileext = "mp3"
                                    if metadata.contentType.mediaType.fileExtensions.contains(fileext) then {
                                      val is: InputStream = file.runWith(StreamConverters.asInputStream(FiniteDuration(180, TimeUnit.SECONDS)))
                                      val processor = Future {
                                        log.info(s"$clientId: Mediafile ${metadata.fileName} aktualisieren ...")
                                        if id > 0 then {
                                          ResourceExchanger.importWettkampfMediaFile(wettkampf, selectAthletRegistration(id).copy(mediafile = Some(MediaAdmin("", metadata.fileName, fileext, 0, "", "", 0L))), is)
                                        } else {
                                          AthletRegistration(id, 0, None, "", "", "", "", 0L, 0L, None, None, Some(ResourceExchanger.saveMediaFile(is, wettkampf, Media("", metadata.fileName, fileext))))
                                        }
                                      }
                                      onComplete(processor) {
                                        case Success(r) =>
                                          CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                          log.info(s"$clientId: Mediafile ${metadata.fileName} aktualisiert: ${r.easyprint}")
                                          complete(r.toJson(using athletregistrationFormat))

                                        case Failure(e) =>
                                          log.warning(s"mediafile ${metadata.fileName} cannot be uploaded: " + e.toString)
                                          complete(StatusCodes.Conflict, s"Die Datei ${metadata.fileName} konnte nicht hochgeladen werden. (${e.getMessage})")
                                      }
                                    } else {
                                      log.error(s"invalid media type (${metadata.contentType.mediaType})")
                                      complete(StatusCodes.Conflict, s"Ungültige Audiodatei (${metadata.contentType.mediaType})")
                                    }
                                }
                              }
                            }
                          }
                        } ~ pathEndOrSingleSlash {
                          get {
                            complete(selectAthletRegistration(id).toJson(using athletregistrationFormat))
                          } ~ put { // update Athletes
                            entity(as[AthletRegistration]) { athletRegistration =>
                              try {
                                val isOverride = athletRegistration.athletId match {
                                  case Some(id) if id > 0 =>
                                    Await.result(AthletIndexActor.publish(FindAthletLike(athletRegistration.toAthlet)), Duration.Inf) match {
                                      case AthletLikeFound(_, found) => found.id != id
                                      case _ => false
                                    }
                                  case _ => false
                                }
                                if isOverride then {
                                  throw new IllegalArgumentException("Person-Überschreibung in einer Anmeldung zu einer anderen Person ist nicht erlaubt!")
                                }
                                val reg: Option[AthletRegistration] = updateAthletRegistration(athletRegistration)
                                CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                log.info(s"$clientId: Athletanmeldung aktualisiert: ${athletRegistration.toPublicView.easyprint}, $reg")
                                complete(reg.getOrElse(athletRegistration).toJson(using athletregistrationFormat))
                              } catch {
                                case e: IllegalArgumentException =>
                                  log.error(e.getMessage)
                                  complete(StatusCodes.Conflict, e.getMessage)
                              }
                            }
                          } ~ delete { // delete  Athletes
                            complete(Future {
                              deleteAthletRegistration(id)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                              log.info(s"$clientId: Athletanmeldung gelöscht: $id")
                              StatusCodes.OK
                            })
                          }
                        }
                      }
                    } ~ pathPrefixLabeled("judges", "judges") {
                      pathEndOrSingleSlash {
                        get { // list judges
                          complete(
                            selectJudgeRegistrations(registrationId).toJson(using listFormat(using judgeregistrationFormat))
                          )
                        } ~ post { // create judges
                          log.info("post judgesregistration")
                          entity(as[JudgeRegistration]) { judgeRegistration =>
                            complete {
                              try {
                                val reg = createJudgeRegistration(judgeRegistration)
                                CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                log.info(s"$clientId: WR-Anmeldung angelegt: ${judgeRegistration.easyprint}")
                                reg.toJson(using judgeregistrationFormat)
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
                              selectJudgeRegistration(id).toJson(using judgeregistrationFormat)
                            )
                          } ~ put { // update judges
                            entity(as[JudgeRegistration]) { judgesRegistration =>
                              complete(Future {
                                val reg = updateJudgeRegistration(judgesRegistration)
                                CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                                log.info(s"$clientId: WR-Anmeldung aktualisiert: ${judgesRegistration.easyprint}")
                                reg.toJson(using judgeregistrationFormat)
                              })
                            }
                          } ~ delete { // delete  judges
                            complete(Future {
                              deleteJudgeRegistration(id)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wettkampf.uuid.get), clientId)
                              log.info(s"$clientId: WR-Anmeldung gelöscht: $id")
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
