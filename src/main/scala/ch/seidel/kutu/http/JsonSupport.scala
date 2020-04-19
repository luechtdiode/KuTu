package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain._
import spray.json.{JsValue, _}


trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  
  implicit val wkFormat = jsonFormat(Wettkampf, "id", "uuid", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote")
  implicit val pgmFormat = jsonFormat7(ProgrammRaw)
  implicit val disziplinFormat = jsonFormat2(Disziplin)
  implicit val wertungFormat = jsonFormat(Wertung, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2")  
  implicit val vereinFormat = jsonFormat(Verein, "id", "name", "verband")
  implicit val atheltViewFormat = jsonFormat(AthletView, "id", "js_id", "geschlecht", "name", "vorname", "gebdat", "strasse", "plz", "ort", "verein", "activ")
  implicit val wertungContainerFormat = jsonFormat9(WertungContainer)

  implicit val resultatFormat = jsonFormat(Resultat, "noteD", "noteE", "endnote")
  implicit val dataObjectFormat = new RootJsonWriter[DataObject]{
    def write(p: DataObject) = {
      p.easyprint.toJson
    }
  }

  // actions (via rest-request)
  implicit val getResultsToReplicate = jsonFormat2(GetResultsToReplicate)
  implicit val startDurchgangFormat = jsonFormat2(StartDurchgang)
  implicit val updateAthletWertungFormat = jsonFormat7(UpdateAthletWertung)
  implicit val finishDurchgangStationFormat = jsonFormat4(FinishDurchgangStation)
  implicit val finishDurchgangFormat = jsonFormat2(FinishDurchgang)
  implicit val finishDurchgangStepFormat = jsonFormat1(FinishDurchgangStep)
  implicit val publishScores = jsonFormat4(PublishScores)

  // events (via ws and rest-response)
  implicit val durchgangStartedFormat = jsonFormat3(DurchgangStarted)
  implicit val wertungUpdatedFormat = jsonFormat6(AthletWertungUpdated)
  implicit val wertungUpdatedFormatSeq = jsonFormat7(AthletWertungUpdatedSequenced)
  implicit val stationsWertungenCompletedFormat = jsonFormat1(StationWertungenCompleted)
  implicit val newLastResultsFormat = jsonFormat2(NewLastResults)
  implicit val durchgangFinishedFormat = jsonFormat3(DurchgangFinished)
  implicit val scoresPublished = jsonFormat5(ScoresPublished)
  implicit val lastResults = jsonFormat1(LastResults)
  implicit val bulkEvents = jsonFormat2(BulkEvent)
  implicit val athletRemovedFromWettkampf = jsonFormat2(AthletRemovedFromWettkampf)
  implicit val athletMovedInWettkampf = jsonFormat3(AthletMovedInWettkampf)
  implicit val messageAckFormat = jsonFormat1(MessageAck)

  // support for websocket incoming json-messages
  val caseClassesJsonFormatter: Map[String, JsonFormat[_ <: KutuAppEvent]] = Map(
      classOf[DurchgangStarted].getSimpleName -> durchgangStartedFormat,
      classOf[AthletWertungUpdated].getSimpleName -> wertungUpdatedFormat,
      classOf[AthletWertungUpdatedSequenced].getSimpleName -> wertungUpdatedFormatSeq,
      classOf[StationWertungenCompleted].getSimpleName -> stationsWertungenCompletedFormat,
      classOf[NewLastResults].getSimpleName -> newLastResultsFormat,
      classOf[DurchgangFinished].getSimpleName -> durchgangFinishedFormat,
      classOf[ScoresPublished].getSimpleName -> scoresPublished,
      classOf[LastResults].getSimpleName -> lastResults,
      classOf[BulkEvent].getSimpleName -> bulkEvents,
      classOf[AthletRemovedFromWettkampf].getSimpleName -> athletRemovedFromWettkampf,
      classOf[AthletMovedInWettkampf].getSimpleName -> athletMovedInWettkampf,
      classOf[MessageAck].getSimpleName -> messageAckFormat
  )

  implicit val messagesFormatter: RootJsonFormat[KutuAppEvent] = new RootJsonFormat[KutuAppEvent] { 
    override def read(json: JsValue) = json.asJsObject.fields("type").asOpt[String] match {
      case Some(s) if s.contains("BulkEvent") =>
        val list = json.asJsObject.fields("events").asInstanceOf[JsArray].elements
            .toList
            .map{this.read(_)}
        BulkEvent( json.asJsObject.fields("wettkampfUUID").convertTo[String], list)
      case _ => json.asOpt[JsObject].flatMap(_.fields.get("type").flatMap(_.asOpt[String])).map(caseClassesJsonFormatter) match {
        case Some(jsonReader) =>
          val plain = json.withoutFields("type")
          jsonReader.read(plain)
        case _ => throw new Exception(s"Unable to parse $json to KutuAppProtokoll")
      }
    }

    override def write(obj: KutuAppEvent): JsValue = {
      obj match {
        case be: BulkEvent =>
          val jv = JsObject()
          jv.addFields(Map(
            "type" -> JsString(be.getClass.getSimpleName),
            "wettkampfUUID" -> JsString(be.wettkampfUUID),
            "events" -> JsArray(be.events.map(ev => write(ev)).toVector)
          ))
        case _ =>
          caseClassesJsonFormatter.get(obj.getClass.getSimpleName) match {
            case Some(jsonWriter) =>
              jsonWriter.asInstanceOf[JsonFormat[KutuAppEvent]].write(obj).addFields(Map(("type" -> JsString(obj.getClass.getSimpleName))))
            case _ => throw new Exception(s"Unable to find jsonFormatter for $obj")
          }
      }
    }
  }
}
