package ch.seidel.kutu.http

import ch.seidel.jwt.{JsonWebToken, JwtClaimsSetMap}
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.*
import ch.seidel.kutu.actors.*
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.renderer.ServerPrintUtil
import ch.seidel.kutu.squad.{DurchgangBuilder, DurchgangGrouper}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.scaladsl.{Sink, Source, StreamConverters}
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory
import spray.json.*

import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

trait WettkampfClient extends AuthSupport with KutuService with FailureSupport {
  private val log = LoggerFactory.getLogger(WettkampfClient.this.getClass)
  import DefaultJsonProtocol.*

  private def toHttpEntity(wettkampf: Wettkampf): HttpEntity.Strict = {
    val bos = new ByteArrayOutputStream()
    ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
    val bytes = bos.toByteArray
    val responseEntity = HttpEntity(bytes)
    Multipart.FormData(
      Multipart.FormData.BodyPart.Strict(
        "zip",
        responseEntity,
        Map("filename" -> s"${wettkampf.easyprint}.zip")
      )
    ).toEntity
  }

  def startDurchgang(p: WettkampfView, durchgang: String): Future[HttpResponse] = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/start",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(StartDurchgang(p.uuid.get, durchgang).toJson.compactPrint)
      )
    )
  }

  def resetDurchgang(p: WettkampfView, durchgang: String): Future[HttpResponse] = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/reset",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(ResetStartDurchgang(p.uuid.get, durchgang).toJson.compactPrint)
      )
    )
  }

  def finishDurchgangStep(p: WettkampfView): Future[HttpResponse] = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/finishedStep",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(FinishDurchgangStep(p.uuid.get).toJson.compactPrint)
      )
    )
  }

  def finishDurchgang(p: WettkampfView, durchgang: String): Future[HttpResponse] = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/stop",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(FinishDurchgang(p.uuid.get, durchgang).toJson.compactPrint)
      )
    )
  }

  sealed trait ServerInteraction
  case object Connect extends ServerInteraction
  case object Upload extends ServerInteraction
  def httpUploadWettkampfRequest(wettkampf: Wettkampf, interaction: ServerInteraction = Upload): Future[HttpResponse] = {
    val uuid = wettkampf.uuid match {
      case None => createUUIDForWettkampf(wettkampf.id).uuid.get
      case Some(uuid) => uuid
    }
    val wettkampfEntity = toHttpEntity(wettkampf)
    val uploadProm = Promise[String]()
    val uploadFut = uploadProm.future
    if Config.isLocalHostServer && remoteHost.startsWith("localhost") && !wettkampf.hasSecred(homedir, remoteHostOrigin) then {
      wettkampf.saveSecret(homedir, remoteHostOrigin, JsonWebToken(jwtHeader, setClaims(uuid, Int.MaxValue, isAdmin = true), jwtSecretKey))
    }
    val hadSecret = wettkampf.hasSecred(homedir, remoteHostOrigin)

    def postWettkampf(prom: Promise[String]): Future[HttpResponse] = {
      // try to initial upload new wettkampf
      log.info("post to " + s"$remoteAdminBaseUrl/api/competition/$uuid")
      val reqresp = httpClientRequest(
        HttpRequest(method = HttpMethods.POST, uri = s"$remoteAdminBaseUrl/api/competition/$uuid", entity = wettkampfEntity)).map {
        case response@HttpResponse(StatusCodes.OK, headers, entity, _) =>
          val secretOption = catchSecurityHeader(wettkampf, headers, entity).map(_.value)
          if secretOption.isEmpty then {
            log.info("failed post to " + s"$remoteAdminBaseUrl/api/competition/$uuid, no secret available")
            throw new RuntimeException("No Secret for Competition available")
          } else {
            log.info("success post to " + s"$remoteAdminBaseUrl/api/competition/$uuid, got new secret.")
            prom.success(secretOption.get)
            response
          }

        case response@HttpResponse(status, headers, entity, _) =>
          log.info("failed post to " + s"$remoteAdminBaseUrl/api/competition/$uuid, returned ${response.status}")
          throw HTTPFailure(status, entity match {
            case HttpEntity.Strict(_, text) =>
              log.error(s"post failed with ${text.utf8String}")
              catchSecurityHeader(wettkampf, headers, entity)
              text.utf8String
            case x =>
              log.error(s"post failed with $x")
              x.toString
          })
      }
      reqresp.onComplete {
        case Success(_) =>
        case Failure(s) =>
          prom.failure(s)
      }
      reqresp
    }

    if !hadSecret then {
      log.info("no secret seen, post instead of put competition ...")
      postWettkampf(uploadProm)
    } else {
      wettkampf.readSecret(homedir, remoteHostOrigin) match {
        case Some(secret) =>
          log.info("secret seen, map for put operation uploadProm success ...")
          uploadProm.success(secret)
        case _ =>
          log.info("no secret seen, uploadProm failed")
          uploadProm.failure(new RuntimeException("No Secret for Competition available"))
      }
    }

    val process = uploadFut.flatMap { secret =>
      httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
    }.transformWith {
      case Failure(f) => f match {
        case HTTPFailure(StatusCodes.NotFound, _, _ ) =>
          log.warn(s"login with existing secret impossible ($f), remote competition not existing! try post ...")
          postWettkampf(Promise[String]())

        case e: Throwable =>
          log.warn(s"login with existing secret impossible ($f), remote competition not existing! abort!")
          throw e
      }

      case Success(response) =>
        log.info("got login resonse ...")
        if interaction == Upload && hadSecret && response.status.isSuccess() then {
          log.info("login successful - put to " + s"$remoteAdminBaseUrl/api/competition/$uuid")
          httpPutClientRequest(s"$remoteAdminBaseUrl/api/competition/$uuid", wettkampfEntity).map {
            case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
              log.info("put successful")
              resp
            case HttpResponse(status, _, entity, _) => entity match {
              case HttpEntity.Strict(_, text) =>
                log.error(s"put failed: ${text.utf8String}")
                throw HTTPFailure(status, text.utf8String)
              case x =>
                log.error(s"put failed: $x")
                throw HTTPFailure(status, x.toString)
            }
          }
        }
        else {
          Future{response}
        }
    }
    log.info("returning share competition process-future ...")
    process
  }

  private def catchSecurityHeader(wettkampf: Wettkampf, headers: Seq[HttpHeader], entity: ResponseEntity) = {
    import Core.materializer
    val secret = headers.find(h => h.is(jwtAuthorizationKey)).flatMap {
      case HttpHeader(_, token) =>
        entity.discardBytes()
        Some(RawHeader(jwtAuthorizationKey, token))
    } match {
      case token@Some(_) => token
      case _ => try {
        Await.result(Unmarshal(entity).to[JsObject].map { json =>
          json.getFields("token").map(field => RawHeader(jwtAuthorizationKey, field.toString)).headOption
        }, Duration.Inf)
      } catch {
        case e: Exception => Option.empty
      }
    }
    println(s"New Secret: " + secret)
    if secret.nonEmpty then {
      wettkampf.saveSecret(homedir, remoteHostOrigin, secret.get.value)
    }
    secret
  }

  def httpDownloadRequest(request: HttpRequest): Future[Wettkampf] = {
    import Core.*
    val source = Source.single(request, ())
    val requestResponseFlow = Http().superPool[Unit](settings = poolsettings)

    def importData(httpResponse: HttpResponse) = {
      if httpResponse.status.isSuccess() then {
        val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
        val wettkampf = ResourceExchanger.importWettkampf(is)
        if !wettkampf.hasRemote(homedir, remoteHostOrigin) then {
          wettkampf.saveRemoteOrigin(homedir, remoteHostOrigin)
        }
        wettkampf
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

    val wettkampf = source.via(requestResponseFlow)
      .map(responseOrFail)
      .map(_._1)
      .map(importData)
      .runWith(Sink.head)
    wettkampf
  }

  def httpRemoveWettkampfRequest(wettkampf: Wettkampf): Future[HttpResponse] = {
    val eventualResponse: Future[HttpResponse] = httpDeleteClientRequest(s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}")
    eventualResponse.onComplete {
      case scala.util.Success(_) =>
        wettkampf.removeSecret(homedir, remoteHostOrigin)
        wettkampf.removeRemote(homedir, remoteHostOrigin)
      case _ =>
    }
    eventualResponse
  }

  def extractWettkampfUUID: HttpHeader => Option[String] = {
    case HttpHeader("wkuuid", value) => Some(value)
    case _ => None
  }

}

trait WettkampfRoutes extends WettkampfClient with SprayJsonSupport
  with JsonSupport with JwtSupport with AuthSupport with RouterLogging with WettkampfService with RegistrationService
  with CIDSupport with FailureSupport {

  import DefaultJsonProtocol.*

  lazy val wettkampfRoutes: Route = {
    handleCID { (clientId: String) =>
      pathLabeled("isTokenExpired", "isTokenExpired") {
        pathEnd {
          authenticated() { wettkampfUUID =>
            val claims = setClaims(wettkampfUUID, jwtTokenExpiryPeriodInDays)
            respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
              complete(wettkampfUUID)
            }
          }
        }
      } ~
      pathPrefixLabeled("admin" / "competition", "admin/competition") {
        pathEnd {
          post {
            extractUri { uri =>
              entity(as[AdminCreateCompetitionRequest]) { request =>
                if (!request.termsAccepted) {
                  complete(StatusCodes.BadRequest, "Terms of use must be accepted")
                } else {
                  val uuid = java.util.UUID.randomUUID().toString
                  onComplete(Future {
                    val wettkampf = createWettkampf(
                      datum = request.datum,
                      titel = request.titel,
                      programmId = Set(request.programmId),
                      notificationEMail = request.notificationEMail,
                      auszeichnung = request.auszeichnung,
                      auszeichnungendnote = request.auszeichnungendnote,
                      uuidOption = Some(uuid),
                      altersklassen = request.altersklassen,
                      jahrgangsklassen = request.jahrgangsklassen,
                      punktegleichstandsregel = request.punktegleichstandsregel,
                      rotation = request.rotation,
                      teamrule = request.teamrule,
                      creatorName = Some(request.creatorName),
                      creatorAddress = Some(request.creatorAddress),
                      creatorPhone = Some(request.creatorPhone),
                      termsAccepted = true,
                      termsAcceptedAt = Some(new java.sql.Timestamp(System.currentTimeMillis())),
                      termsVersion = Some(request.termsVersion)
                    )
                    val claims = setClaims(uuid, Int.MaxValue, isAdmin = true)
                    val secret = JsonWebToken(jwtHeader, claims, jwtSecretKey)
                    wettkampf.saveSecret(Config.homedir, Config.remoteHostOrigin, secret)
                    val decodedorigin = s"${if uri.authority.host.toString().contains("localhost") then "http" else "https"}://${uri.authority}"
                    val link = s"$decodedorigin/api/registrations/$uuid/approvemail?mail=${encodeURIParam(request.notificationEMail)}"
                    CompetitionRegistrationClientActor.publish(CompetitionCreated(uuid, link), clientId)
                    AdminCreateCompetitionResponse(uuid, request.titel, request.datum, secret)
                  }) {
                    case Success(response) => complete(response.toJson)
                    case Failure(e) =>
                      log.error(e, "Failed to create competition")
                      complete(StatusCodes.InternalServerError, s"Failed to create competition: ${e.getMessage}")
                  }
                }
              }
            }
          }
        } ~
        pathPrefixLabeled(JavaUUID, ":uuid") { wkuuid =>
          get {
            authenticatedAdmin() { userId =>
              if userId.equals(wkuuid.toString) then {
                onSuccess(readWettkampfAsync(wkuuid.toString)) { wettkampf =>
                  complete(AdminGetCompetitionResponse(
                    id = wettkampf.id,
                    uuid = wettkampf.uuid.get,
                    datum = wettkampf.datum,
                    titel = wettkampf.titel,
                    programmId = wettkampf.programmId,
                    auszeichnung = wettkampf.auszeichnung,
                    auszeichnungendnote = wettkampf.auszeichnungendnote,
                    notificationEMail = wettkampf.notificationEMail,
                    altersklassen = wettkampf.altersklassen.getOrElse(""),
                    jahrgangsklassen = wettkampf.jahrgangsklassen.getOrElse(""),
                    punktegleichstandsregel = wettkampf.punktegleichstandsregel.getOrElse(""),
                    rotation = wettkampf.rotation.getOrElse(""),
                    teamrule = wettkampf.teamrule.getOrElse("")
                  ).toJson)
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          } ~
          put {
            authenticatedAdmin() { userId =>
              if userId.equals(wkuuid.toString) then {
                entity(as[AdminUpdateCompetitionRequest]) { request =>
                  onComplete(Future {
                    saveWettkampf(
                      id = request.id,
                      datum = request.datum,
                      titel = request.titel,
                      programmId = Set(request.programmId),
                      notificationEMail = request.notificationEMail,
                      auszeichnung = request.auszeichnung,
                      auszeichnungendnote = request.auszeichnungendnote,
                      uuidOption = Some(wkuuid.toString),
                      altersklassen = request.altersklassen,
                      jahrgangsklassen = request.jahrgangsklassen,
                      punktegleichstandsregel = request.punktegleichstandsregel,
                      rotation = request.rotation,
                      teamrule = request.teamrule
                    )
                  }) {
                    case Success(wk) => complete(JsObject("status" -> JsString("ok"), "easyprint" -> JsString(wk.easyprint)))
                    case Failure(e) =>
                      log.error(e, "Failed to update competition")
                      complete(StatusCodes.InternalServerError, s"Failed to update competition: ${e.getMessage}")
                  }
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          }
        }
      } ~
      pathLabeled("competition" / "ws", "competition/ws") {
        pathEnd {
          (authenticated() & parameters(Symbol("lastSequenceId").?)) { (wettkampfUUID, lastSequenceId: Option[String]) =>
            handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, wettkampfUUID, None, lastSequenceId.map(_.toLong)))
          }
        }
      } ~
      pathPrefixLabeled("competition", "competition") {
        pathPrefixLabeled("byVerein" / LongNumber, "byVerein/:verein-id") { vereinId =>
          pathEnd {
            get {
              complete {
                listWettkaempfeByVereinIdAsync(vereinId).map(list => list.map(_.toPublic).toJson)
              }
            }
          }
        } ~ pathLabeled("programmlist", "programmlist") {
          get {
            complete(listRootProgrammeAsync.map(list => list.map(pv => {
              ProgrammRaw(pv.id, pv.name, pv.aggregate, pv.parent.map(_.id).getOrElse(0L), pv.ord, pv.alterVon, pv.alterBis, pv.uuid, pv.riegenmode, pv.bestOfCount)
            }).toJson))
          }
        } ~ pathPrefixLabeled("programmdisziplinen" / LongNumber, "programmdisziplinen/:programId") { programId =>
          get {
            complete(listProgramDisziplinenAsync(programId).map(result => JsArray(result.map(JsString(_)).toVector)))
          }
        } ~ pathPrefixLabeled("programmkategorien" / LongNumber, "programmkategorien/:programId") { programId =>
          get {
            complete(listProgramKategorienAsync(programId).map(result => JsArray(result.map(JsString(_)).toVector)))
          }
        } ~ pathEnd {
          get {
            complete {
              listWettkaempfeAsync.map(list => list.map(_.toPublic).toJson)
            }
          }
        }
      } ~
      pathPrefixLabeled("competition" / JavaUUID, "competition/:competition-id") { wkuuid =>
        val manager = new DurchgangStartriegenManager(this, () => CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId))
        pathLabeled("start", "start") {
          post {
            authenticatedAdmin() { userId =>
              entity(as[StartDurchgang]) { sd =>
                if userId.equals(wkuuid.toString) then {
                  AbuseHandler.clearAbusedClients()
                  complete(CompetitionCoordinatorClientActor.publish(sd, clientId))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        } ~
        pathLabeled("reset", "reset") {
        post {
          authenticatedAdmin() { userId =>
            entity(as[ResetStartDurchgang]) { rsd =>
              if userId.equals(wkuuid.toString) then {
                AbuseHandler.clearAbusedClients()
                complete(CompetitionCoordinatorClientActor.publish(rsd, clientId))
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          }
        }
      } ~
        pathLabeled("stop", "stop") {
          post {
            authenticatedAdmin() { userId =>
              entity(as[FinishDurchgang]) { fd =>
                if userId.equals(wkuuid.toString) then {
                  complete(CompetitionCoordinatorClientActor.publish(fd, clientId))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        } ~
        pathLabeled("finishedStep", "finishedStep") {
          post {
            authenticatedAdmin() { userId =>
              entity(as[FinishDurchgangStep]) { fd =>
                if userId.equals(wkuuid.toString) then {
                  complete(CompetitionCoordinatorClientActor.publish(fd, clientId))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        } ~
        pathPrefixLabeled("riege", "riege") {
          pathEnd {
            authenticatedAdmin() { userId =>
              if userId.equals(wkuuid.toString) then {
                get {
                  onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                    complete(manager.getAllStartRiegen(wk.id).toJson)
                  }
                } ~
                put {
                  entity(as[UpdateRiegeRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      val item = manager.updateStartRiege(RiegeRaw(wk.id, request.name, request.durchgang, request.startId, request.kind))
                      complete(item.toJson)
                    }
                  }
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          } ~
          pathLabeled("generate", "generate") {
            post {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[RiegeSuggestionRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      onComplete(Future {
                        val disziplinlist = listDisziplinesZuWettkampf(wk.id)
                        val onDisziplin = request.onDisziplinIds.map(ids => disziplinlist.filter(d => ids.contains(d.id)).toSet)
                        val splitSex = request.splitSexOption.flatMap {
                          case "GemischteRiegen" => Some(GemischteRiegen)
                          case "GemischterDurchgang" => Some(GemischterDurchgang)
                          case "GetrennteDurchgaenge" => Some(GetrennteDurchgaenge)
                          case _ => None
                        }
                        val durchgangBuilder = DurchgangBuilder(this)

                        durchgangBuilder.generateRiegen(wk,
                          durchgangfilter = request.filterDurchgang.getOrElse(Set.empty),
                          onDisziplinList = onDisziplin,
                          maxRiegenSize = request.maxRiegenSize,
                          maxParallelDg = request.maxParallelDg,
                          splitSexOption = splitSex,
                          splitPgm = request.splitPgm,
                          separateRiegen2Durchgaenge = request.separateRiegen2Durchgaenge
                        )
                        CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                        CompetitionRegistrationClientActor.publish(RegistrationChanged(wkuuid.toString), clientId)

                        // prepare response
                        val riegen = selectRiegen(wk.id)
                        val counts = listRiegenZuWettkampf(wk.id).groupMap(_._1)(_._2).view.mapValues(_.sum)
                        val riegenItems = riegen.map(r => RiegeItem(
                          name = r.r,
                          durchgang = r.durchgang,
                          startId = r.start.map(_.id),
                          startName = r.start.map(_.name),
                          kind = r.kind,
                          athletCount = counts.getOrElse(r.r, 0)
                        ))
                        val durchgaenge = selectDurchgaenge(wkuuid)
                        val athleteCountsByDurchgang = listRiegenZuWettkampf(wk.id)
                          .groupBy(_._3).view.mapValues(_.map(_._2).sum)
                        val durchgangItems = durchgaenge.map(d => DurchgangDurationItem(
                          name = d.name, title = d.title,
                          offsetMillis = d.planStartOffset,
                          einturnenMillis = d.planEinturnen,
                          geraetMillis = d.planGeraet,
                          totalMillis = d.planTotal,
                          athletCount = athleteCountsByDurchgang.getOrElse(Some(d.name), 0)
                        ))
                        RiegePreviewResponse(riegenItems, durchgangItems)
                      }) {
                        case Success(response) => complete(response.toJson)
                        case Failure(e) =>
                          log.error(e.getMessage, e)
                          complete(StatusCodes.InternalServerError, s"Failed to generate riegen: ${e.getMessage}")
                      }
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("reset", "reset") {
            post {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                    cleanAllRiegenDurchgaenge(wk.id)
                    CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                    CompetitionRegistrationClientActor.publish(RegistrationChanged(wkuuid.toString), clientId)
                    complete(StatusCodes.OK, JsObject.empty)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("duration", "duration") {
            get {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                    val durchgaenge = selectDurchgaenge(wkuuid)
                    val athleteCountsByDurchgang = listRiegenZuWettkampf(wk.id)
                      .groupBy(_._3).view.mapValues(_.map(_._2).sum)
                    complete(durchgaenge.map(d => DurchgangDurationItem(
                      name = d.name, title = d.title,
                      offsetMillis = d.planStartOffset,
                      einturnenMillis = d.planEinturnen,
                      geraetMillis = d.planGeraet,
                      totalMillis = d.planTotal,
                      athletCount = athleteCountsByDurchgang.getOrElse(Some(d.name), 0)
                    )).toJson)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("renamedg", "renamedg") {
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[UpdateDurchgangRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      manager.renameDurchgang(wk.id, request.oldTitle, request.newTitle)
                      complete(JsObject("status" -> JsString("ok")))
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("movegroup", "movegroup") {
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[GroupDurchgangRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      manager.moveDurchgangToGroup(wk.id, request.durchgangNames, request.groupTitle)
                      complete(JsObject("status" -> JsString("ok")))
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("mergedg", "mergedg") {
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[MergeDurchgangRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      manager.mergeDurchgaenge(wk.id, request.durchgangNames, request.targetName)
                      complete(JsObject("status" -> JsString("ok")))
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("ungroup", "ungroup") {
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[UngroupDurchgangRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      manager.ungroupDurchgaenge(wk.id, request.durchgangNames)
                      complete(JsObject("status" -> JsString("ok")))
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("aggregate", "aggregate") {
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[GroupDurchgangRequest]) { request =>
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wk =>
                      manager.aggregateDurchgaenge(wk.id, request.durchgangNames, request.groupTitle)
                      complete(JsObject("status" -> JsString("ok")))
                    }
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        } ~
        pathLabeled("logo", "logo") {
          get {
            authenticatedAdmin() { userId =>
              if userId.equals(wkuuid.toString) then {
                onSuccess(readWettkampfAsync(wkuuid.toString)) { wettkampf =>
                  val competitionDir = wettkampf.prepareFilePath(Config.homedir, readOnly = true)
                  val logofile = ServerPrintUtil.locateLogoFile(competitionDir)
                  if logofile.exists() then {
                    val contentType = ContentType(logofile.getName match {
                      case n if n.endsWith(".svg") => MediaTypes.`image/svg+xml`
                      case n if n.endsWith(".png") => MediaTypes.`image/png`
                      case n if n.endsWith(".jpg") || n.endsWith(".jpeg") => MediaTypes.`image/jpeg`
                      case _ => MediaTypes.`application/octet-stream`
                    })
                    getFromFile(logofile, contentType)
                  } else {
                    complete(StatusCodes.NotFound)
                  }
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          } ~
          post {
            authenticatedAdmin() { userId =>
              if userId.equals(wkuuid.toString) then {
                withSizeLimit(Config.logoFileMaxSize) {
                  withoutRequestTimeout {
                    fileUpload("logo") {
                      case (metadata, file: Source[ByteString, Any]) =>
                        val allowedExtensions = List("svg", "png", "jpg", "jpeg")
                        val fileext = metadata.fileName.split("\\.").last.toLowerCase
                        if allowedExtensions.contains(fileext) && metadata.contentType.mediaType.toString.startsWith("image/") then {
                          import Core.materializer
                          val is = file.runWith(StreamConverters.asInputStream(FiniteDuration(180, TimeUnit.SECONDS)))
                          val processor = readWettkampfAsync(wkuuid.toString).flatMap { wettkampf =>
                            Future {
                              try {
                                LogoHandler.handleLogoUpload(wettkampf, fileext, is)
                              } finally { is.close() }
                            }
                          }
                          onComplete(processor) {
                            case Success(_) => complete(StatusCodes.OK)
                            case Failure(e) =>
                              log.error(e.getMessage, e)
                              complete(StatusCodes.Conflict, s"Logo konnte nicht gespeichert werden: ${e.getMessage}")
                          }
                        } else {
                          complete(StatusCodes.Conflict, s"Ungültiges Format. Erlaubt sind: ${allowedExtensions.mkString(", ")}")
                        }
                    }
                  }
                }
              } else {
                complete(StatusCodes.Conflict)
              }
            }
          }
        } ~
        pathEnd {
          post {
            extractUri { uri =>
              withoutRequestTimeout {
                onSuccess(wettkampfExistsAsync(wkuuid.toString)) {
                  case exists if !exists =>
                    fileUpload("zip") {
                      case (metadata, file: Source[ByteString, Any]) =>
                        // do something with the file and file metadata ...
                        log.info(s"receiving new wettkampf: $metadata, $wkuuid")
                        import Core.materializer
                        val is = file.async.runWith(StreamConverters.asInputStream(FiniteDuration(180, TimeUnit.SECONDS)))
                        val processor = Future[(Wettkampf,JwtClaimsSetMap)] {
                          try {
                            val wettkampf = ResourceExchanger.importWettkampf(is)
                            val decodedorigin = s"${if uri.authority.host.toString().contains("localhost") then "http" else "https"}://${uri.authority}"
                            val link = s"$decodedorigin/api/registrations/${wettkampf.uuid.get}/approvemail?mail=${encodeURIParam(wettkampf.notificationEMail)}"
                            AthletIndexActor.publish(ResyncIndex)
                            CompetitionRegistrationClientActor.publish(CompetitionCreated(wkuuid.toString, link), clientId)
                            val claims = setClaims(wkuuid.toString, Int.MaxValue, isAdmin = true)
                            (wettkampf, claims)
                          } finally {
                            is.close()
                          }
                        }
                        onComplete(processor) {
                          case Success((wettkampf, claims)) =>
                            respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                              if wettkampf.notificationEMail == null || wettkampf.notificationEMail.trim.isEmpty then {
                                complete(StatusCodes.Conflict, s"Die EMail-Adresse für die Notifikation von Online-Registrierungen ist noch nicht erfasst.")
                              } else {
                                complete(StatusCodes.OK)
                              }
                            }

                          case Failure(e) =>
                            log.warning(s"wettkampf $wkuuid cannot be uploaded: " + e.toString)
                            complete(StatusCodes.Conflict, s"Wettkampf $wkuuid konnte wg. einem technischen Fehler nicht hochgeladen werden. (${e.toString})")
                        }
                    }
                  case _ =>
                    log.warning(s"wettkampf $wkuuid cannot be uploaded twice")
                    complete(StatusCodes.Conflict, s"Wettkampf $wkuuid kann nicht mehrfach hochgeladen werden.")
                }
              }
            }
          } ~
          put {
            extractUri { uri =>
              authenticated() { userId =>
                if userId.equals(wkuuid.toString) then {
                  withoutRequestTimeout {
                    fileUpload("zip") {
                      case (metadata, file: Source[ByteString, Any]) =>
                        // do something with the file and file metadata ...
                        log.info(s"receiving and updating wettkampf: $metadata, $wkuuid")
                        import Core.materializer
                        val is = file.runWith(StreamConverters.asInputStream(FiniteDuration(180, TimeUnit.SECONDS)))
                        val processor = Future {
                          try {
                            val before = readWettkampf(wkuuid.toString)
                            val wettkampf = ResourceExchanger.importWettkampf(is)
                            AthletIndexActor.publish(ResyncIndex)
                            CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                            CompetitionRegistrationClientActor.publish(RegistrationChanged(wkuuid.toString), clientId)
                            AbuseHandler.clearAbusedClients()
                            if !before.notificationEMail.equalsIgnoreCase(wettkampf.notificationEMail) then {
                              val decodedorigin = s"${if uri.authority.host.toString().contains("localhost") then "http" else "https"}://${uri.authority}"
                              val link = s"$decodedorigin/api/registrations/${wettkampf.uuid.get}/approvemail?mail=${encodeURIParam(wettkampf.notificationEMail)}"
                              CompetitionRegistrationClientActor.publish(CompetitionCreated(wkuuid.toString, link), clientId)
                            }
                            wettkampf
                          } finally {
                            is.close()
                          }
                        }

                        onComplete(processor) {
                          case Success(wettkampf) =>
                            log.info(s"wettkampf ${wettkampf.easyprint} updated")
                            if wettkampf.notificationEMail == null || wettkampf.notificationEMail.trim.isEmpty then {
                              complete(StatusCodes.Conflict, s"Die EMail-Adresse für die Notifikation von Online-Registrierungen ist noch nicht erfasst.")
                            } else {
                              complete(StatusCodes.OK)
                            }
                          case Failure(e) =>
                            log.error(e, s"wettkampf $wkuuid cannot be uploaded: " + e.toString)
                            complete(StatusCodes.BadRequest,
                              s"""Der Wettkampf ${metadata.fileName}
                                 |konnte wg. einem technischen Fehler nicht hochgeladen werden!
                                 |=>${e.getMessage}""".
                                stripMargin)
                      }
                  }
                }
              }
                else {
                  complete(
                    StatusCodes.Unauthorized)
                }
              }
            }
          } ~
          delete {
            authenticated() { userId =>
              if userId.equals(wkuuid.toString) then {
                onSuccess(readWettkampfAsync(wkuuid.toString)) { wettkampf =>
                  log.info("deleting wettkampf: " + wettkampf)
                  complete(
                    CompetitionCoordinatorClientActor.publish(Delete(wkuuid.toString), clientId)
                    .andThen {
                      case _ =>
                        CompetitionRegistrationClientActor.stop(wkuuid.toString)
                        deleteRegistrations(UUID.fromString(wkuuid.toString))
                        deleteWettkampf(wettkampf.id)
                        StatusCodes.OK
                    }
                  )
                }
              } else {
                complete(StatusCodes.Unauthorized)
              }
            }
          } ~
          get {
            extractUri { uri =>
              authenticatedId { authUserId =>
                log.info("serving wettkampf: " + wkuuid)
                val wettkampf = readWettkampf(wkuuid.toString)
                val isAdmin = authUserId.contains(wkuuid.toString)
                val adminJwt = if isAdmin then Some(JsonWebToken(jwtHeader, setClaims(wkuuid.toString, Int.MaxValue, isAdmin = true), jwtSecretKey)) else None
                val adminOrigin = if isAdmin then Some(uri.authority.host.toString) else None
                onComplete(Future {
                  val bos = new ByteArrayOutputStream()
                  ResourceExchanger.exportWettkampfToStream(wettkampf, bos, withSecret = isAdmin, adminJwt = adminJwt, adminOrigin = adminOrigin)
                  HttpEntity(
                    MediaTypes.`application/zip`,
                    bos.toByteArray
                  )
                }){
                  case Success(entity) =>
                    complete(entity)
                  case Failure(e) =>
                    log.error(e, s"wettkampf $wkuuid cannot be exported: " + e.toString)
                    complete(StatusCodes.BadRequest,
                      s"""Der Wettkampf ${wettkampf.easyprint}
                          |konnte wg. einem technischen Fehler nicht vollständig zum Download exportiert werden.
                          |=>${e.getMessage}""".stripMargin)
                }
              }
            }
          }
        }
      }
    }
  }
}
