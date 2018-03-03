package ch.seidel.kutu.http

import scala.reflect.ClassTag
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.akka.KutuAppProtokoll

trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  
  implicit val wkFormat = jsonFormat(Wettkampf, "id", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote", "uuid")
  implicit val pgmFormat = jsonFormat7(ProgrammRaw)
  implicit val disziplinFormat = jsonFormat2(Disziplin)
  implicit val wertungFormat = jsonFormat(Wertung, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2")
  
  case class AthletWertung(id: Long, name: String, vorname: String, verein: String, geschlecht: String, wertung: Wertung, geraet: Long)
  implicit val athletWertungFormat = jsonFormat7(AthletWertung)
  
  // support for websocket incoming json-messages
  val caseClassesJsonReader: Map[String, JsonReader[_ <: KutuAppProtokoll]] = Map(
//    classOf[TicketTicketActionPerformed].getSimpleName -> userAuthenticatedFailedFormat
  )

  implicit val messagesFormat: JsonReader[KutuAppProtokoll] = { json =>
    json.asOpt[JsObject].flatMap(_.fields.get("type").flatMap(_.asOpt[String])).map(caseClassesJsonReader) match {
      case Some(jsonReader) =>
        val plain = json.withoutFields("type")
        jsonReader.read(plain)
      case _ => throw new Exception(s"Unable to parse $json to KutuAppProtokoll")
    }
  }
}
