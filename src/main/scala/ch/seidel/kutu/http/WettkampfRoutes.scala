package ch.seidel.kutu.http

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import scala.util.Try
import java.util.concurrent.TimeUnit

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.EnhancedFromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshal

import akka.stream.scaladsl.StreamConverters
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.Wettkampf
import ch.seidel.kutu.domain.WettkampfService

trait WettkampfRoutes extends JwtSupport with RouterLogging with WettkampfService {
  
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
  
 
  def httpDownloadRequest(request: HttpRequest) = {
    import Core._
    val source = Source.single((request, ()))
    val requestResponseFlow = Http().superPool[Unit]()
    def responseOrFail[T](in: (Try[HttpResponse], T)): (HttpResponse, T) = in match {
      case (responseTry, context) => (responseTry.get, context)
    }
    def importData(httpResponse : HttpResponse) {
      val is = httpResponse.entity.dataBytes.runWith(StreamConverters.asInputStream())
      ResourceExchanger.importWettkampf(is)
    }
    source.via(requestResponseFlow)
          .map(responseOrFail)
          .map(_._1)
          .runWith(Sink.foreach(importData))
  }
      
  lazy val wettkampfRoutes: Route = {
    pathPrefix("competition" / "upload") {
      pathEnd {
        authenticated { userId =>
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
      }
    } ~
    path("competition" / "download" / JavaUUID) { wettkampfid =>
      authenticated { userId =>
        log.info("serving wettkampf: " + wettkampfid)
        val wettkampf = readWettkampf(wettkampfid.toString())
        val bos = new ByteArrayOutputStream()
        ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
        val bytes = bos.toByteArray()
        complete(HttpEntity(
                MediaTypes.`application/zip`,
                bos.toByteArray))
      }
    }
    
  }
}
