package ch.seidel.kutu.http

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source, StreamConverters}
import akka.util.ByteString
import authentikat.jwt.JsonWebToken
import ch.seidel.kutu.Config._
import ch.seidel.kutu.akka.{StartDurchgang, _}
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.{Wettkampf, WettkampfService, WettkampfView}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, Promise}
import scala.util.Try

trait WettkampfRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with WettkampfService with CIDSupport {

  import DefaultJsonProtocol._

  def responseOrFail[T](in: (Try[HttpResponse], T)): (HttpResponse, T) = in match {
    case (responseTry, context) => (responseTry.get, context)
  }

  def toHttpEntity(wettkampf: Wettkampf) = {
    val bos = new ByteArrayOutputStream()
    ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
    val bytes = bos.toByteArray()
    val responseEntity = HttpEntity(bytes)
    Multipart.FormData(
      Multipart.FormData.BodyPart.Strict(
        "zip",
        responseEntity,
        Map("filename" -> s"${wettkampf.easyprint}.zip")
      )
    ) toEntity
  }

  def startDurchgang(p: WettkampfView, durchgang: String) = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/start",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(StartDurchgang(p.uuid.get, durchgang).toJson.compactPrint)
      )
    )
  }

  def finishDurchgangStep(p: WettkampfView) = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/finishedStep",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(FinishDurchgangStep(p.uuid.get).toJson.compactPrint)
      )
    )
  }

  def finishDurchgang(p: WettkampfView, durchgang: String) = {
    httpPostClientRequest(s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}/stop",
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(FinishDurchgang(p.uuid.get, durchgang).toJson.compactPrint)
      )
    )
  }

  def httpUploadWettkampfRequest(wettkampf: Wettkampf) = {
    import Core.materializer
    val uuid = wettkampf.uuid match {
      case None => saveWettkampf(wettkampf.id, wettkampf.datum, wettkampf.titel, Set(wettkampf.programmId), wettkampf.auszeichnung, wettkampf.auszeichnungendnote, None).uuid.get
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
      log.info("post to " + s"${remoteAdminBaseUrl}/api/competition/${uuid}")
      httpClientRequest(
        HttpRequest(method = HttpMethods.POST, uri = s"${remoteAdminBaseUrl}/api/competition/${uuid}", entity = wettkampfEntity)).map {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          val secret = headers.filter(h => h.is(jwtAuthorizationKey)).headOption.flatMap {
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

        case HttpResponse(_, headers, entity, _) => entity match {
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
        log.info("put to " + s"${remoteAdminBaseUrl}/api/competition/${uuid}")
        httpPutClientRequest(s"$remoteAdminBaseUrl/api/competition/${uuid}", wettkampfEntity)
      } else {
        Future {
          response
        }
      }
    }

    process
  }

  def httpDownloadRequest(request: HttpRequest) = {
    import Core._
    val source = Source.single(request, ())
    val requestResponseFlow = Http().superPool[Unit](settings = poolsettings)

    def importData(httpResponse: HttpResponse) = {
      val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
      ResourceExchanger.importWettkampf(is)
    }

    val wettkampf = source.via(requestResponseFlow)
      .map(responseOrFail)
      .map(_._1)
      .map(importData)
      .runWith(Sink.head)
    wettkampf
  }

  def httpRemoveWettkampfRequest(wettkampf: Wettkampf) = {
    httpDeleteClientRequest(s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}")
    wettkampf.removeSecret(homedir, remoteHostOrigin)
  }

  def extractWettkampfUUID: HttpHeader => Option[String] = {
    case HttpHeader("wkuuid", value) => Some(value)
    case _ => None
  }

  lazy val wettkampfRoutes: Route = {
    handleCID { clientId: String =>
      path("isTokenExpired") {
        pathEnd {
          authenticated() { wettkampfUUID =>
            val claims = setClaims(wettkampfUUID, jwtTokenExpiryPeriodInDays)
            respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
              complete(StatusCodes.OK)
            }
          }
        }
      } ~
      path("competition" / "ws") {
        pathEnd {
          (authenticated() & parameters('lastSequenceId.?)) { (wettkampfUUID, lastSequenceId: Option[String]) =>
            handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(clientId, wettkampfUUID, None, lastSequenceId.map(_.toLong)))
          }
        }
      } ~
      pathPrefix("competition") {
        pathEnd {
          get {
            complete {
              listWettkaempfeAsync
            }
          }
        }
      } ~
      pathPrefix("competition" / JavaUUID) { wkuuid =>
        path("start") {
          post {
            authenticated() { userId =>
              entity(as[StartDurchgang]) { sd =>
                if (userId.equals(wkuuid.toString())) {
                  complete(CompetitionCoordinatorClientActor.publish(sd, clientId))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        } ~
          path("stop") {
            post {
              authenticated() { userId =>
                entity(as[FinishDurchgang]) { fd =>
                  if (userId.equals(wkuuid.toString())) {
                    complete(CompetitionCoordinatorClientActor.publish(fd, clientId))
                  } else {
                    complete(StatusCodes.Conflict)
                  }
                }
              }
            }
          } ~
          path("finishedStep") {
            post {
              authenticated() { userId =>
                entity(as[FinishDurchgangStep]) { fd =>
                  if (userId.equals(wkuuid.toString())) {
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
                onSuccess(wettkampfExistsAsync(wkuuid.toString())) {
                  case exists if (!exists) =>
                    fileUpload("zip") {
                      //                uploadedFile("zip") {
                      case (metadata, file: Source[ByteString, Any]) =>
                        // do something with the file and file metadata ...
                        log.info(s"receiving wettkampf: $metadata, $wkuuid")
                        import Core.materializer;
                        val is = file.runWith(StreamConverters.asInputStream(FiniteDuration(60, TimeUnit.SECONDS)))
                        ResourceExchanger.importWettkampf(is)
                        is.close()
                        //                    file.delete()
                        val claims = setClaims(wkuuid.toString(), Int.MaxValue)
                        respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                          complete(StatusCodes.OK)
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
                  if (userId.equals(wkuuid.toString())) {
                    fileUpload("zip") {
                      case (metadata, file) =>
                        // do something with the file and file metadata ...
                        log.info("receiving wettkampf: " + metadata)
                        onSuccess(Future {
                          import Core.materializer;
                          val is = file.runWith(StreamConverters.asInputStream(FiniteDuration(120, TimeUnit.SECONDS)))
                          ResourceExchanger.importWettkampf(is)
                          is.close()
                        }) {
                          complete(StatusCodes.OK)
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
                  if (userId.equals(wkuuid.toString())) {
                    onSuccess(readWettkampfAsync(wkuuid.toString())) { wettkampf =>
                      deleteWettkampf(wettkampf.id)
                      CompetitionCoordinatorClientActor.publish(Delete(wkuuid.toString), clientId)
                      complete(StatusCodes.OK)
                    }
                  } else {
                    complete(StatusCodes.Unauthorized)
                  }
                }
              } ~
              get {
                //      authenticated { userId =>
                log.info("serving wettkampf: " + wkuuid)
                val wettkampf = readWettkampf(wkuuid.toString())
                val bos = new ByteArrayOutputStream()
                ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
                val bytes = bos.toByteArray()
                complete(
                  HttpEntity(
                    MediaTypes.`application/zip`,
                    bytes
                  )
                )
                //      }
              }
          }
      }
    }
  }
}
