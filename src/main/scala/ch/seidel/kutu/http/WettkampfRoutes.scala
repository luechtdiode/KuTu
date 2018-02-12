package ch.seidel.kutu.http

import java.io.FileInputStream
import java.io.ByteArrayOutputStream

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.HttpEntity

import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.WettkampfService
import akka.http.javadsl.model.Multiparts
import akka.http.javadsl.model.HttpEntities
import ch.seidel.kutu.domain.Wettkampf
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.Multipart
import akka.stream.scaladsl._

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
  
  def fromEntity(e: HttpEntity) {
    import Core._
    ResourceExchanger.importWettkampf(e.dataBytes.runWith(StreamConverters.asInputStream()))
  }
    
  lazy val wettkampfRoutes: Route = {
    pathPrefix("competition" / "upload") {
      pathEnd {
        authenticated { userId =>
          uploadedFile("zip") {
            case (metadata, file) =>
              // do something with the file and file metadata ...
              val is = new FileInputStream(file)
              ResourceExchanger.importWettkampf(is)
              is.close()
              file.delete()
              complete(StatusCodes.OK)
          }
        }
      }
    } ~
    path("competition" / "download" / LongNumber) { wettkampfid =>
      authenticated { userId =>
        val bos = new ByteArrayOutputStream()
        val wettkampf = readWettkampf(wettkampfid)
        ResourceExchanger.exportWettkampfToStream(wettkampf, bos)
        val bytes = bos.toByteArray()
        val responseEntity = HttpEntity(
//          MediaTypes.`application/zip`
          bytes)
        complete(responseEntity)
      }
    }
    
  }
}
