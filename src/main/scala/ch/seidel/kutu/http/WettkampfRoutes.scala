package ch.seidel.kutu.http

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.Files
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Promise
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import java.util.concurrent.TimeUnit

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.EnhancedFromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import akka.stream.scaladsl.StreamConverters
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import spray.json._
import authentikat.jwt.JsonWebToken

import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.Wettkampf
import ch.seidel.kutu.domain.WettkampfService
import ch.seidel.kutu.akka._
import ch.seidel.kutu.Config._
import java.util.UUID
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.scaladsl.Keep
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.settings.ConnectionPoolSettings

trait WettkampfRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with WettkampfService {
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
  
  
  def httpUploadWettkampfRequest(wettkampf: Wettkampf) = {
    import Core.system
    import Core.materializer
    val uuid = wettkampf.uuid match {
      case None => saveWettkampf(wettkampf.id, wettkampf.datum, wettkampf.titel, Set(wettkampf.programmId), wettkampf.auszeichnung, wettkampf.auszeichnungendnote, None).uuid.get
      case Some(uuid) => uuid
    }
    val wettkampfEntity = toHttpEntity(wettkampf)
    val uploadProm = Promise[String]()
    val uploadFut = uploadProm.future
    if (remoteHost.startsWith("localhost") && !wettkampf.hasSecred(homedir, remoteHostOrigin)) {
      wettkampf.saveSecret(homedir, remoteHostOrigin,  JsonWebToken(jwtHeader, setClaims(uuid, Int.MaxValue), jwtSecretKey))
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
                case token @ Some(_) => token
                case _ => Await.result(Unmarshal(entity).to[JsObject].map{json =>
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
    
    val process = uploadFut.flatMap{secret =>
      httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
    }.flatMap{request =>  
      if (hadSecret) {
      log.info("put to " + s"${remoteAdminBaseUrl}/api/competition/${uuid}")
        httpPutClientRequest(s"$remoteAdminBaseUrl/api/competition/${uuid}", wettkampfEntity)
      } else {
        Future{request}
      }
    }

    process
  }
 
  def httpDownloadRequest(request: HttpRequest) = {
    import Core._
    val source = Source.single((request, ()))
    val requestResponseFlow = Http().superPool[Unit]()

    def importData(httpResponse : HttpResponse) {
      val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
      ResourceExchanger.importWettkampf(is)
    }
    source.via(requestResponseFlow)
          .map(responseOrFail)
          .map(_._1)
          .runWith(Sink.foreach(importData))
  }
      
  def httpRemoveWettkampfRequest(wettkampf: Wettkampf) = {
    httpDeleteClientRequest(s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}")
    wettkampf.removeSecret(homedir, remoteHostOrigin)
  }
  
  def extractWettkampfUUID: HttpHeader => Option[String] = {
    case HttpHeader("wkuuid", value) => Some(value)
    case _                           => None
  }

  lazy val wettkampfRoutes: Route = {
    path("competition" / "ws") {
      pathEnd {
        authenticated { wettkampfUUID =>
          handleWebSocketMessages(CompetitionCoordinatorClientActor.createActorSinkSource(UUID.randomUUID().toString, wettkampfUUID, None))
        }
      }
    } ~
    pathPrefix("competition") {
      pathEnd {
        get {
          complete{ listWettkaempfeAsync }          
        }
      }
    } ~
    pathPrefix("competition" / JavaUUID) { wkuuid =>
      pathEnd {
        post {
          onSuccess(wettkampfExistsAsync(wkuuid.toString())) {
            case exists if (!exists) =>
              uploadedFile("zip") {
                case (metadata, file) =>
                  // do something with the file and file metadata ...
                  log.info(s"receiving wettkampf: $metadata, $wkuuid")
                  val is = new FileInputStream(file)
                  ResourceExchanger.importWettkampf(is)
                  is.close()
                  file.delete()

                  val claims = setClaims(wkuuid.toString(), Int.MaxValue)
                  respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                    complete(StatusCodes.OK)
                  }
              }
            case _ => 
              log.warning(s"wettkampf $wkuuid cannot be uploaded twice")
              complete(StatusCodes.Conflict, s"wettkampf $wkuuid kann nicht mehrfach hochgeladen werden.")
          }
        }~
        put {
          authenticated { userId =>
            if (userId.equals(wkuuid.toString())) {
              uploadedFile("zip") {
                case (metadata, file) =>
                  // do something with the file and file metadata ...
                  log.info("receiving wettkampf: " + metadata)
                  val is = new FileInputStream(file)
                  ResourceExchanger.importWettkampf(is)
                  is.close()
                  file.delete()
                  complete(StatusCodes.OK)
              }
            }
            else {
              complete(StatusCodes.Unauthorized)
            }
          }
        }~
        delete {
          authenticated { userId =>
            onSuccess(readWettkampfAsync(wkuuid.toString())) { wettkampf =>
              deleteWettkampf(wettkampf.id)
              complete(StatusCodes.OK)
            }
          }
        }~
        get {
  //      authenticated { userId =>
          log.info("serving wettkampf: " + wkuuid)
          val wettkampf = readWettkampf(wkuuid.toString())
          val bos = new ByteArrayOutputStream()
          ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
          val bytes = bos.toByteArray()
          complete(HttpEntity(
                  MediaTypes.`application/zip`,
                  bos.toByteArray))
  //      }
        }
      }
    }
  }
}
