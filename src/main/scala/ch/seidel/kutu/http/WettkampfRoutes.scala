package ch.seidel.kutu.http

import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.scaladsl.{Sink, Source, StreamConverters}
import org.apache.pekko.util.ByteString
import ch.seidel.jwt.{JsonWebToken, JwtClaimsSetMap}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.actors._
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.{RegistrationService, ProgrammRaw, Wettkampf, WettkampfService, WettkampfView, encodeURIParam}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

trait WettkampfRoutes extends SprayJsonSupport
  with JsonSupport with JwtSupport with AuthSupport with RouterLogging with WettkampfService with RegistrationService
  with CIDSupport {

  import DefaultJsonProtocol._
  
  def responseOrFail[T](in: (Try[HttpResponse], T)): (HttpResponse, T) = in match {
    case (responseTry, context) => (responseTry.get, context)
  }

  def toHttpEntity(wettkampf: Wettkampf): HttpEntity.Strict = {
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
    ) toEntity
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
    if (remoteHost.startsWith("localhost") && !wettkampf.hasSecred(homedir, remoteHostOrigin)) {
      wettkampf.saveSecret(homedir, remoteHostOrigin, JsonWebToken(jwtHeader, setClaims(uuid, Int.MaxValue), jwtSecretKey))
    }
    val hadSecret = wettkampf.hasSecred(homedir, remoteHostOrigin)

    def postWettkampf(prom: Promise[String]): Future[HttpResponse] = {
      // try to initial upload new wettkampf
      log.info("post to " + s"$remoteAdminBaseUrl/api/competition/$uuid")
      val reqresp = httpClientRequest(
        HttpRequest(method = HttpMethods.POST, uri = s"$remoteAdminBaseUrl/api/competition/$uuid", entity = wettkampfEntity)).map {
        case response@HttpResponse(StatusCodes.OK, headers, entity, _) =>
          val secretOption = catchSecurityHeader(wettkampf, headers, entity).map(_.value)
          if (secretOption.isEmpty) {
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

    if (!hadSecret) {
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
          log.warning(s"login with existing secret impossible ($f), remote competition not existing! try post ...")
          postWettkampf(Promise[String]())

        case e: Throwable =>
          log.warning(s"login with existing secret impossible ($f), remote competition not existing! abort!")
          throw e
      }

      case Success(response) =>
        log.info("got login resonse ...")
        if (interaction == Upload && hadSecret && response.status.isSuccess()) {
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
    if (secret.nonEmpty) {
      wettkampf.saveSecret(homedir, remoteHostOrigin, secret.get.value)
    }
    secret
  }

  def httpDownloadRequest(request: HttpRequest): Future[Wettkampf] = {
    import Core._
    val source = Source.single(request, ())
    val requestResponseFlow = Http().superPool[Unit](settings = poolsettings)

    def importData(httpResponse: HttpResponse) = {
      if (httpResponse.status.isSuccess()) {
        val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
        val wettkampf = ResourceExchanger.importWettkampf(is)
        if (!wettkampf.hasRemote(homedir, remoteHostOrigin)) {
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

  lazy val wettkampfRoutes: Route = {
    handleCID { clientId: String =>
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
                listWettkaempfeByVereinIdAsync(vereinId).map(list => list.map(_.toPublic))
              }
            }
          }
        } ~ pathLabeled("programmlist", "programmlist") {
          get {
            complete(listRootProgrammeAsync.map(list => list.map(pv => {
              ProgrammRaw(pv.id, pv.name, pv.aggregate, pv.parent.map(_.id).getOrElse(0L), pv.ord, pv.alterVon, pv.alterBis, pv.uuid, pv.riegenmode, pv.bestOfCount)
            })))
          }
        } ~ pathEnd {
          get {
            complete {
              listWettkaempfeAsync.map(list => list.map(_.toPublic))
            }
          }
        }
      } ~
      pathPrefixLabeled("competition" / JavaUUID, "competition/:competition-id") { wkuuid =>
        pathLabeled("start", "start") {
          post {
            authenticated() { userId =>
              entity(as[StartDurchgang]) { sd =>
                if (userId.equals(wkuuid.toString)) {
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
            authenticated() { userId =>
              entity(as[ResetStartDurchgang]) { rsd =>
                if (userId.equals(wkuuid.toString)) {
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
              authenticated() { userId =>
                entity(as[FinishDurchgang]) { fd =>
                  if (userId.equals(wkuuid.toString)) {
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
              authenticated() { userId =>
                entity(as[FinishDurchgangStep]) { fd =>
                  if (userId.equals(wkuuid.toString)) {
                    complete(CompetitionCoordinatorClientActor.publish(fd, clientId))
                  } else {
                    complete(StatusCodes.Conflict)
                  }
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
                              val decodedorigin = s"${if (uri.authority.host.toString().contains("localhost")) "http" else "https"}://${uri.authority}"
                              val link = s"$decodedorigin/api/registrations/${wettkampf.uuid.get}/approvemail?mail=${encodeURIParam(wettkampf.notificationEMail)}"
                              AthletIndexActor.publish(ResyncIndex)
                              CompetitionRegistrationClientActor.publish(CompetitionCreated(wkuuid.toString, link), clientId)
                              val claims = setClaims(wkuuid.toString, Int.MaxValue)
                              (wettkampf, claims)
                            } finally {
                              is.close()
                            }
                          }
                          onComplete(processor) {
                            case Success((wettkampf, claims)) =>
                              respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                                if (wettkampf.notificationEMail == null || wettkampf.notificationEMail.trim.isEmpty) {
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
                    if (userId.equals(wkuuid.toString)) {
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
                                if (!before.notificationEMail.equalsIgnoreCase(wettkampf.notificationEMail)) {
                                  val decodedorigin = s"${if (uri.authority.host.toString().contains("localhost")) "http" else "https"}://${uri.authority}"
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
                                if (wettkampf.notificationEMail == null || wettkampf.notificationEMail.trim.isEmpty) {
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
                  if (userId.equals(wkuuid.toString)) {
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
                log.info("serving wettkampf: " + wkuuid)
                val wettkampf = readWettkampf(wkuuid.toString)
                onComplete(Future {
                  val bos = new ByteArrayOutputStream()
                  ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
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
