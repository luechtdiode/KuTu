package ch.seidel.kutu.http

import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source, StreamConverters}
import akka.util.ByteString
import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config._
import ch.seidel.kutu.akka.{StartDurchgang, _}
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.{RegistrationService, Wettkampf, WettkampfService, WettkampfView}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

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

  def httpUploadWettkampfRequest(wettkampf: Wettkampf): Future[HttpResponse] = {
    import Core.materializer
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
    if (!hadSecret) {
      // try to initial upload new wettkampf
      log.info("post to " + s"$remoteAdminBaseUrl/api/competition/$uuid")
      httpClientRequest(
        HttpRequest(method = HttpMethods.POST, uri = s"$remoteAdminBaseUrl/api/competition/$uuid", entity = wettkampfEntity)).map {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          val secret = headers.find(h => h.is(jwtAuthorizationKey)).flatMap {
            case HttpHeader(_, token) =>
              entity.discardBytes()
              Some(RawHeader(jwtAuthorizationKey, token))
          } match {
            case token@Some(_) => token
            case _ => Await.result(Unmarshal(entity).to[JsObject].map { json =>
              json.getFields("token").map(field => RawHeader(jwtAuthorizationKey, field.toString)).headOption
            }, Duration.Inf)
          }
          println(s"New Secret: " + secret)
          wettkampf.saveSecret(homedir, remoteHostOrigin, secret.get.value)
          uploadProm.success(secret.get.value)

        case HttpResponse(_, _, entity, _) => entity match {
          case HttpEntity.Strict(_, text) =>
            log.error(text.utf8String)
            uploadProm.failure(new RuntimeException(text.utf8String))
          case x =>
            log.error(x.toString)
            uploadProm.failure(new RuntimeException(x.toString))
        }
      }

    } else {
      wettkampf.readSecret(homedir, remoteHostOrigin) match {
        case Some(secret) => uploadProm.success(secret)
        case _ => uploadProm.failure(new RuntimeException("No Secret for Competition avaliable"))
      }
    }

    val process = uploadFut.flatMap { secret =>
      httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
    }.flatMap { response =>
      if (hadSecret) {
        log.info("put to " + s"$remoteAdminBaseUrl/api/competition/$uuid")
        httpPutClientRequest(s"$remoteAdminBaseUrl/api/competition/$uuid", wettkampfEntity).flatMap {
          case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
            Future {
              resp
            }
          case HttpResponse(_, _, entity, _) => entity match {
            case HttpEntity.Strict(_, text) =>
              log.error(text.utf8String)
              Future.failed(new RuntimeException(text.utf8String))
            case x =>
              log.error(x.toString)
              Future.failed(new RuntimeException(x.toString))
          }
        }
      } else {
        Future {
          response
        }
      }
    }

    process
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
            throw new RuntimeException(text.utf8String)
          case x =>
            log.error(x.toString)
            throw  new RuntimeException(x.toString)
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
                  complete(CompetitionCoordinatorClientActor.publish(sd, clientId))
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
              withoutRequestTimeout {
                onSuccess(wettkampfExistsAsync(wkuuid.toString)) {
                  case exists if !exists =>
                    fileUpload("zip") {
                      case (metadata, file: Source[ByteString, Any]) =>
                        // do something with the file and file metadata ...
                        log.info(s"receiving new wettkampf: $metadata, $wkuuid")
                        import Core.materializer
                        val is = file.runWith(StreamConverters.asInputStream(FiniteDuration(180, TimeUnit.SECONDS)))
                        try {
                          val wettkampf = ResourceExchanger.importWettkampf(is)
                          val claims = setClaims(wkuuid.toString, Int.MaxValue)
                          respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                            if (wettkampf.notificationEMail == null || wettkampf.notificationEMail.trim.isEmpty) {
                              complete(StatusCodes.Conflict, s"Die EMail-Adresse für die Notifikation von Online-Registrierungen ist noch nicht erfasst.")
                            } else {
                              complete(StatusCodes.OK)
                            }
                          }
                        } catch {
                          case e: Exception =>
                            log.warning(s"wettkampf $wkuuid cannot be uploaded: " + e.toString)
                            complete(StatusCodes.Conflict, s"wettkampf $wkuuid konnte wg. einem technischen Fehler nicht hochgeladen werden. (${e.toString})")
                        } finally {
                          is.close()
                        }
                    }
                  case _ =>
                    log.warning(s"wettkampf $wkuuid cannot be uploaded twice")
                    complete(StatusCodes.Conflict, s"wettkampf $wkuuid kann nicht mehrfach hochgeladen werden.")
                }
              }
            } ~
              put {
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
                              val wettkampf = ResourceExchanger.importWettkampf(is)
                              AthletIndexActor.publish(ResyncIndex)
                              CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                              CompetitionRegistrationClientActor.publish(RegistrationChanged(wkuuid.toString), clientId)

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
                                   |konnte wg. einem technischen Fehler nicht vollständig hochgeladen werden.
                                   |=>${e.getMessage}""".stripMargin)
                          }
                      }
                    }
                  }
                  else {
                    complete(StatusCodes.Unauthorized)
                  }
                }
              } ~
              delete {
                authenticated() { userId =>
                  if (userId.equals(wkuuid.toString)) {
                    onSuccess(readWettkampfAsync(wkuuid.toString)) { wettkampf =>
                      complete(
                        CompetitionCoordinatorClientActor.publish(Delete(wkuuid.toString), clientId)
                        .andThen {
                          case _ =>
                            deleteRegistrations(UUID.fromString(wettkampf.uuid.get))
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
