package ch.seidel.kutu.http

import scala.reflect.ClassTag
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import ch.seidel.kutu.domain._

trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  
  // support for websocket incoming json-messages
//  val caseClassesJsonReader: Map[String, JsonReader[_ <: MobileTicketQueueProtokoll]] = Map(
//    classOf[TicketTicketActionPerformed].getSimpleName -> userAuthenticatedFailedFormat, classOf[UserAuthenticated].getSimpleName -> userAuthenticatedFormat, classOf[MessageAck].getSimpleName -> messageAckFormat, classOf[TicketClosed].getSimpleName -> ticketDeletedFormat, classOf[TicketAccepted].getSimpleName -> ticketAcceptedFormat, classOf[TicketIssued].getSimpleName -> ticketIssued, classOf[TicketExpired].getSimpleName -> ticketExpiredFormat, classOf[TicketConfirmed].getSimpleName -> ticketConfirmedFormat, classOf[HelloImOnline].getSimpleName -> helloFormat, classOf[Subscribe].getSimpleName -> subscribeFormat, classOf[UnSubscribe].getSimpleName -> unsubscribeFormat, classOf[TicketCalled].getSimpleName -> ticketCalledFormat, classOf[TicketReactivated].getSimpleName -> ticketReactivatedFormat, classOf[TicketSkipped].getSimpleName -> ticketSkippedFormat
//  )
//
//  implicit val messagesFormat: JsonReader[MobileTicketQueueProtokoll] = { json =>
//    json.asOpt[JsObject].flatMap(_.fields.get("type").flatMap(_.asOpt[String])).map(caseClassesJsonReader) match {
//      case Some(jsonReader) =>
//        val plain = json.withoutFields("type")
//        jsonReader.read(plain)
//      case _ => throw new Exception(s"Unable to parse $json to PubSub")
//    }
//  }
}
