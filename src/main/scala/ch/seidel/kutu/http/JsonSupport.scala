package ch.seidel.kutu.http

import ch.seidel.kutu.actors._
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate, ScoreCalcTemplateView, ScoreCalcVariable}
import ch.seidel.kutu.domain._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._

  implicit val wkFormat: RootJsonFormat[Wettkampf] = jsonFormat(Wettkampf, "id", "uuid", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote", "notificationEMail", "altersklassen", "jahrgangsklassen", "punktegleichstandsregel", "rotation", "teamrule")
  implicit val pgmFormat: RootJsonFormat[ProgrammRaw] = jsonFormat10(ProgrammRaw)
  implicit val disziplinFormat: RootJsonFormat[Disziplin] = jsonFormat2(Disziplin)
  implicit val scoreCalcVariableFormat: RootJsonFormat[ScoreCalcVariable] = jsonFormat6(ScoreCalcVariable)
  implicit val scoreAggrFnFormat: RootJsonFormat[ScoreAggregateFn] = new CaseObjectJsonSupport[ScoreAggregateFn]
  implicit val scoreCalcTemplateFormat: RootJsonFormat[ScoreCalcTemplate] = jsonFormat(ScoreCalcTemplate,
    "id", "wettkampfId", "disziplinId", "wettkampfdisziplinId", "dFormula", "eFormula", "pFormula", "aggregateFn")
  implicit val scoreCalcTemplateViewFormat: RootJsonFormat[ScoreCalcTemplateView] = jsonFormat10(ScoreCalcTemplateView)
  implicit val wertungFormat: RootJsonFormat[Wertung] = jsonFormat(Wertung, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2", "team", "mediafile", "variables")
  implicit val vereinFormat: RootJsonFormat[Verein] = jsonFormat(Verein, "id", "name", "verband")
  implicit val atheltViewFormat: RootJsonFormat[AthletView] = jsonFormat(AthletView, "id", "js_id", "geschlecht", "name", "vorname", "gebdat", "strasse", "plz", "ort", "verein", "activ")
  implicit val wertungContainerFormat: RootJsonFormat[WertungContainer] = jsonFormat11(WertungContainer)
  implicit val registrationFormat: RootJsonFormat[Registration] = jsonFormat11(Registration)
  implicit val newregistrationFormat: RootJsonFormat[NewRegistration] = jsonFormat8(NewRegistration)
  implicit val resetRegistrationPWFormat: RootJsonFormat[RegistrationResetPW] = jsonFormat3(RegistrationResetPW)
  implicit val athletregistrationFormat: RootJsonFormat[AthletRegistration] = jsonFormat12(AthletRegistration)
  implicit val teamFormat: RootJsonFormat[TeamItem] = jsonFormat2(TeamItem)
  implicit val judgeregistrationFormat: RootJsonFormat[JudgeRegistration] = jsonFormat9(JudgeRegistration)
  implicit val judgeregistrationPgmFormat: RootJsonFormat[JudgeRegistrationProgram] = jsonFormat5(JudgeRegistrationProgram)
  implicit val judgeRegistrationProgramItemFormat: RootJsonFormat[JudgeRegistrationProgramItem] = jsonFormat3(JudgeRegistrationProgramItem)

  implicit val resultatFormat: RootJsonFormat[Resultat] = jsonFormat(Resultat, "noteD", "noteE", "endnote", "isStreichwertung", "teilresultateD", "teilresultateE", "teilresultateP")

  implicit val dataObjectFormat: RootJsonWriter[DataObject] = (p: DataObject) => {
    p.easyprint.toJson
  }

  // actions (via rest-request)
  implicit val refresWettkampfMap: RootJsonFormat[RefreshWettkampfMap] = jsonFormat1(RefreshWettkampfMap)
  implicit val getResultsToReplicate: RootJsonFormat[GetResultsToReplicate] = jsonFormat2(GetResultsToReplicate)
  implicit val startDurchgangFormat: RootJsonFormat[StartDurchgang] = jsonFormat2(StartDurchgang)
  implicit val resetStartDurchgangFormat: RootJsonFormat[ResetStartDurchgang] = jsonFormat2(ResetStartDurchgang)
  implicit val updateAthletWertungFormat: RootJsonFormat[UpdateAthletWertung] = jsonFormat7(UpdateAthletWertung)
  implicit val finishDurchgangStationFormat: RootJsonFormat[FinishDurchgangStation] = jsonFormat4(FinishDurchgangStation)
  implicit val finishDurchgangFormat: RootJsonFormat[FinishDurchgang] = jsonFormat2(FinishDurchgang)
  implicit val finishDurchgangStepFormat: RootJsonFormat[FinishDurchgangStep] = jsonFormat1(FinishDurchgangStep)
  implicit val publishScores: RootJsonFormat[PublishScores] = jsonFormat4(PublishScores)
  implicit val approveDonation: RootJsonFormat[DonationApproved] = jsonFormat2(DonationApproved)

  // events (via ws and rest-response)
  implicit val durchgangStartedFormat: RootJsonFormat[DurchgangStarted] = jsonFormat3(DurchgangStarted)
  implicit val durchgangResettedFormat: RootJsonFormat[DurchgangResetted] = jsonFormat2(DurchgangResetted)
  implicit val wertungUpdatedFormat: RootJsonFormat[AthletWertungUpdated] = jsonFormat6(AthletWertungUpdated)
  implicit val wertungUpdatedFormatSeq: RootJsonFormat[AthletWertungUpdatedSequenced] = jsonFormat7(AthletWertungUpdatedSequenced)
  implicit val stationsWertungenCompletedFormat: RootJsonFormat[StationWertungenCompleted] = jsonFormat1(StationWertungenCompleted)
  implicit val newLastResultsFormat: RootJsonFormat[NewLastResults] = jsonFormat3(NewLastResults)
  implicit val durchgangFinishedFormat: RootJsonFormat[DurchgangFinished] = jsonFormat3(DurchgangFinished)
  implicit val scoresPublished: RootJsonFormat[ScoresPublished] = jsonFormat5(ScoresPublished)
  implicit val lastResults: RootJsonFormat[LastResults] = jsonFormat1(LastResults)
  implicit val bulkEvents: RootJsonFormat[BulkEvent] = jsonFormat2(BulkEvent)
  implicit val athletRemovedFromWettkampf: RootJsonFormat[AthletRemovedFromWettkampf] = jsonFormat2(AthletRemovedFromWettkampf)
  implicit val athletMovedInWettkampf: RootJsonFormat[AthletMovedInWettkampf] = jsonFormat4(AthletMovedInWettkampf)
  implicit val athletAddedToettkampf: RootJsonFormat[AthletsAddedToWettkampf] = jsonFormat4(AthletsAddedToWettkampf)
  implicit val messageAckFormat: RootJsonFormat[MessageAck] = jsonFormat1(MessageAck)

  // support for websocket incoming json-messages
  val caseClassesJsonFormatter: Map[String, JsonFormat[_ <: KutuAppEvent]] = Map(
    classOf[DurchgangStarted].getSimpleName -> durchgangStartedFormat,
    classOf[AthletWertungUpdated].getSimpleName -> wertungUpdatedFormat,
    classOf[AthletWertungUpdatedSequenced].getSimpleName -> wertungUpdatedFormatSeq,
    classOf[StationWertungenCompleted].getSimpleName -> stationsWertungenCompletedFormat,
    classOf[NewLastResults].getSimpleName -> newLastResultsFormat,
    classOf[DurchgangFinished].getSimpleName -> durchgangFinishedFormat,
    classOf[DurchgangResetted].getSimpleName -> durchgangResettedFormat,
    classOf[ScoresPublished].getSimpleName -> scoresPublished,
    classOf[LastResults].getSimpleName -> lastResults,
    classOf[BulkEvent].getSimpleName -> bulkEvents,
    classOf[AthletRemovedFromWettkampf].getSimpleName -> athletRemovedFromWettkampf,
    classOf[AthletMovedInWettkampf].getSimpleName -> athletMovedInWettkampf,
    classOf[AthletsAddedToWettkampf].getSimpleName -> athletAddedToettkampf,
    classOf[MessageAck].getSimpleName -> messageAckFormat
  )

  implicit val messagesFormatter: RootJsonFormat[KutuAppEvent] = new RootJsonFormat[KutuAppEvent] {
    override def read(json: JsValue): KutuAppEvent = json.asJsObject.fields("type").asOpt[String] match {
      case Some(s) if s.contains("BulkEvent") =>
        val list = json.asJsObject.fields("events").asInstanceOf[JsArray].elements
          .toList
          .map(read)
        BulkEvent(json.asJsObject.fields("wettkampfUUID").convertTo[String], list)
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
              jsonWriter.asInstanceOf[JsonFormat[KutuAppEvent]].write(obj).addFields(Map("type" -> JsString(obj.getClass.getSimpleName)))
            case _ => throw new Exception(s"Unable to find jsonFormatter for $obj")
          }
      }
    }
  }

  //  implicit val syncAddVereinActionFormat = jsonFormat1(AddVereinAction)
  //  implicit val syncAddRegistrationActionFormat = jsonFormat3(AddRegistration)
  //  implicit val syncMoveRegistrationActionFormat = jsonFormat4(MoveRegistration)
  //  implicit val syncRemoveRegistrationActionFormat = jsonFormat3(RemoveRegistration)

  implicit val syncActionFormatter: RootJsonFormat[SyncAction] = new RootJsonFormat[SyncAction] {
    override def read(json: JsValue): SyncAction = json.asJsObject.fields("type").asOpt[String] match {
      case _ => throw new Exception(s"Unable to parse $json to KutuAppProtokoll")
    }

    override def write(obj: SyncAction): JsValue = {
      obj match {
        case be: SyncAction =>
          val jv = JsObject()
          jv.addFields(Map(
            "caption" -> JsString(be.caption),
            "verein" -> registrationFormat.write(be.verein)
          ))
        case _ => throw new Exception(s"Unable to find jsonFormatter for $obj")
      }
    }
  }
  implicit val baseSyncActionListFormat: AnyRef with RootJsonFormat[List[SyncAction]] = listFormat(syncActionFormatter)
}
