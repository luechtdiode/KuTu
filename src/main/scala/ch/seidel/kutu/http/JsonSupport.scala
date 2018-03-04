package ch.seidel.kutu.http

import scala.reflect.ClassTag
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.akka._

trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  
  implicit val wkFormat = jsonFormat(Wettkampf, "id", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote", "uuid")
  implicit val pgmFormat = jsonFormat7(ProgrammRaw)
  implicit val disziplinFormat = jsonFormat2(Disziplin)
  implicit val wertungFormat = jsonFormat(Wertung, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2")  
  implicit val vereinFormat = jsonFormat(Verein, "id", "name", "verband")
  implicit val atheltViewFormat = jsonFormat(AthletView, "id", "js_id", "geschlecht", "name", "vorname", "gebdat", "strasse", "plz", "ort", "verein", "activ")
//  implicit val programmViewFormat: JsonFormat[ProgrammView] = lazyFormat(jsonFormat(ProgrammView, "id", "name", "aggregate", "parent", "ord", "alterVon", "alterBis"))
//  implicit val wettkampfViewFormat: JsonFormat[WettkampfView] = jsonFormat(WettkampfView, "id", "datum", "titel", "programm", "auszeichnung", "auszeichnungendnote", "uuid")

  //  implicit val attNotenModusFormat: JsonFormat[Athletiktest] = jsonFormat(Athletiktest, "punktemapping", "punktgewicht")
//  implicit val kutuNotenModusFormat: JsonFormat[KuTuWettkampf] = jsonFormat0(KuTuWettkampf)
//  implicit val getuNotenModusFormat: JsonFormat[GeTuWettkampf] = jsonFormat0(GeTuWettkampf)
//  implicit object NotenModusJsonSupport extends CaseObjectJsonSupport[NotenModus]
//  implicit val notenModusFormat: RootJsonFormat[NotenModus] = NotenModusJsonSupport
//  implicit val wettkampfDisciplinViewFormat = jsonFormat(WettkampfdisziplinView, "id", "programm", "disziplin", "kurzbeschreibung", "detailbeschreibung", "notenSpez", "ord", "masculin", "feminim")
//  implicit val wertungViewFormat = jsonFormat(WertungView, "id", "athlet", "wettkampfdisziplin", "wettkampf", "noteD", "noteE", "endnote", "riege", "riege2")
  
  implicit val wertungContainerFormat = jsonFormat7(WertungContainer)
  implicit val athletWertungFormat = jsonFormat5(UpdateAthletWertung)
  implicit val wertungUpdatedFormat = jsonFormat5(AthletWertungUpdated)
  
  // support for websocket incoming json-messages
  val caseClassesJsonReader: Map[String, JsonReader[_ <: KutuAppEvent]] = Map(      
      classOf[AthletWertungUpdated].getSimpleName -> wertungUpdatedFormat
  )

  implicit val messagesFormat: JsonReader[KutuAppEvent] = { json =>
    json.asOpt[JsObject].flatMap(_.fields.get("type").flatMap(_.asOpt[String])).map(caseClassesJsonReader) match {
      case Some(jsonReader) =>
        val plain = json.withoutFields("type")
        jsonReader.read(plain)
      case _ => throw new Exception(s"Unable to parse $json to KutuAppProtokoll")
    }
  }
}
