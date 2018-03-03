package ch.seidel.kutu.http

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute

import scala.concurrent.duration.FiniteDuration
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
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Promise

trait WettkampfRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with BasicAuthSupport with RouterLogging with WettkampfService with Config {
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
    import Config._
    val wettkampfEntity = toHttpEntity(wettkampf)
    val hadSecret = wettkampf.hasSecred(homedir)
    val uploadProm = Promise[String]()
    val uploadFut = uploadProm.future
    
    if (!hadSecret) {
      // try to initial upload new wettkampf
      Http().singleRequest(
          HttpRequest(method = HttpMethods.POST, uri = s"${remoteAdminBaseUrl}/api/competition/${wettkampf.uuid.get}", entity = wettkampfEntity)).map {
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
              wettkampf.saveSecret(homedir, secret.get.value)
              uploadProm.success(secret.get.value)
              
            case x => println("something wrong", x.toString)
              uploadProm.failure(new RuntimeException(x.toString))              
          }
      
    } else {
      wettkampf.readSecret(homedir) match {
        case Some(secret) => uploadProm.success(secret)
        case _ => uploadProm.failure(new RuntimeException("No Secret for Competition avaliable"))
      }
    }
    
    val process = uploadFut.flatMap{secret =>
      httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", wettkampf.uuid.get, secret)
    }.flatMap{request =>  
      if (hadSecret) {
        httpPutClientRequest(s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}", wettkampfEntity)
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
      
  def extractWettkampfUUID: HttpHeader => Option[String] = {
    case HttpHeader("wkuuid", value) => Some(value)
    case _                           => None
  }
  
  lazy val wettkampfRoutes: Route = {
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
            // FIXME activate this exists guard
            case exists /*if (!exists)*/ =>
              uploadedFile("zip") {
                case (metadata, file) =>
                  // do something with the file and file metadata ...
                  log.info(s"receiving wettkampf: $metadata, $wkuuid")
                  val is = new FileInputStream(file)
                  ResourceExchanger.importWettkampf(is)
                  is.close()
                  file.delete()

                  import Config._
                  val claims = setClaims(wkuuid.toString(), Int.MaxValue)
                  respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
                    complete(StatusCodes.OK)
                  }
              }
            case _ => 
              log.warning(s"wettkampf $wkuuid cannot be uploaded twice")
              complete(StatusCodes.Conflict, s"wettkampf $wkuuid cannot be uploaded twice")
          }
        }
      }
    } ~
    pathPrefix("competition" / JavaUUID) { wkuuid =>
      pathEnd {
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
        }
      }
    } ~
    pathPrefix("competition" / JavaUUID) { wkuuid =>
      pathEnd {
        delete {
          authenticated { userId =>
            onSuccess(readWettkampfAsync(wkuuid.toString())) { wettkampf =>
              deleteWettkampf(wettkampf.id)
              complete(StatusCodes.OK)
            }
          }
        }
      }
    } ~    
    path("competition" / JavaUUID) { wkuuid =>
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
