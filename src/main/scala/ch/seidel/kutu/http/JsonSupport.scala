package ch.seidel.kutu.http

import ch.seidel.kutu.actors.*
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate, ScoreCalcTemplateView, ScoreCalcVariable}
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*


trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol.*

  given kutuAppEventFormat: RootJsonFormat[KutuAppEvent] = messagesFormatter.asInstanceOf[RootJsonFormat[KutuAppEvent]]


  given wkFormat: RootJsonFormat[Wettkampf] = jsonFormat(Wettkampf.apply, "id", "uuid", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote", "notificationEMail", "altersklassen", "jahrgangsklassen", "punktegleichstandsregel", "rotation", "teamrule")
  given pgmFormat: RootJsonFormat[ProgrammRaw] = jsonFormat10(ProgrammRaw.apply)
  lazy given programmViewFormat: RootJsonFormat[ch.seidel.kutu.domain.ProgrammView] = new RootJsonFormat[ch.seidel.kutu.domain.ProgrammView] {
    override def write(p: ch.seidel.kutu.domain.ProgrammView): JsValue = JsObject(
      "id" -> JsNumber(p.id),
      "name" -> JsString(p.name),
      "aggregate" -> JsNumber(p.aggregate),
      "parent" -> p.parent.map(_.toJson).getOrElse(JsNull),
      "ord" -> JsNumber(p.ord),
      "alterVon" -> JsNumber(p.alterVon),
      "alterBis" -> JsNumber(p.alterBis),
      "uuid" -> JsString(p.uuid),
      "riegenmode" -> JsNumber(p.riegenmode),
      "bestOfCount" -> JsNumber(p.bestOfCount)
    )

    override def read(json: JsValue): ch.seidel.kutu.domain.ProgrammView = {
      val fields = json.asJsObject.fields
      val parent = fields.get("parent") match {
        case Some(JsNull) | None => None
        case Some(jv) => Some(jv.convertTo[ch.seidel.kutu.domain.ProgrammView])
      }
      ch.seidel.kutu.domain.ProgrammView(
        fields("id").convertTo[Long],
        fields("name").convertTo[String],
        fields("aggregate").convertTo[Int],
        parent,
        fields("ord").convertTo[Int],
        fields("alterVon").convertTo[Int],
        fields("alterBis").convertTo[Int],
        fields("uuid").convertTo[String],
        fields("riegenmode").convertTo[Int],
        fields("bestOfCount").convertTo[Int]
      )
    }
  }
  given wettkampfViewFormat: RootJsonFormat[ch.seidel.kutu.domain.WettkampfView] = jsonFormat(ch.seidel.kutu.domain.WettkampfView.apply,
    "id", "uuid", "datum", "titel", "programm", "auszeichnung", "auszeichnungendnote", "notificationEMail", "altersklassen", "jahrgangsklassen", "punktegleichstandsregel", "rotation", "teamrule")
  given publishedScoreViewFormat: RootJsonFormat[ch.seidel.kutu.domain.PublishedScoreView] = jsonFormat6(ch.seidel.kutu.domain.PublishedScoreView.apply)
  given disziplinFormat: RootJsonFormat[Disziplin] = jsonFormat2(Disziplin.apply)
  given scoreCalcVariableFormat: RootJsonFormat[ScoreCalcVariable] = jsonFormat6(ScoreCalcVariable.apply)
  given scoreAggrFnFormat: RootJsonFormat[ScoreAggregateFn] = CaseObjectJsonSupport(ScoreAggregateFn.values)
  given scoreCalcTemplateFormat: RootJsonFormat[ScoreCalcTemplate] = jsonFormat(ScoreCalcTemplate.apply,
    "id", "wettkampfId", "disziplinId", "wettkampfdisziplinId", "dFormula", "eFormula", "pFormula", "aggregateFn")
  given scoreCalcTemplateViewFormat: RootJsonFormat[ScoreCalcTemplateView] = jsonFormat10(ScoreCalcTemplateView.apply)
  given mediaFormat: RootJsonFormat[Media] = jsonFormat3(Media.apply)
  given mediaAdminFormat: RootJsonFormat[MediaAdmin] = jsonFormat7(MediaAdmin.apply)
  given wertungFormat: RootJsonFormat[Wertung] = jsonFormat(Wertung.apply, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2", "team", "mediafile", "variables")
  given vereinFormat: RootJsonFormat[Verein] = jsonFormat(Verein.apply, "id", "name", "verband")
  given atheltViewFormat: RootJsonFormat[AthletView] = jsonFormat(AthletView.apply, "id", "js_id", "geschlecht", "name", "vorname", "gebdat", "strasse", "plz", "ort", "verein", "activ")
  given wertungContainerFormat: RootJsonFormat[WertungContainer] = jsonFormat11(WertungContainer.apply)
  given registrationFormat: RootJsonFormat[Registration] = jsonFormat11(Registration.apply)
  given newregistrationFormat: RootJsonFormat[NewRegistration] = jsonFormat8(NewRegistration.apply)
  given resetRegistrationPWFormat: RootJsonFormat[RegistrationResetPW] = jsonFormat3(RegistrationResetPW.apply)
  given athletregistrationFormat: RootJsonFormat[AthletRegistration] = jsonFormat12(AthletRegistration.apply)
  given teamFormat: RootJsonFormat[TeamItem] = jsonFormat2(TeamItem.apply)
  given judgeregistrationFormat: RootJsonFormat[JudgeRegistration] = jsonFormat9(JudgeRegistration.apply)
  given judgeregistrationPgmFormat: RootJsonFormat[JudgeRegistrationProgram] = jsonFormat5(JudgeRegistrationProgram.apply)
  given judgeRegistrationProgramItemFormat: RootJsonFormat[JudgeRegistrationProgramItem] = jsonFormat3(JudgeRegistrationProgramItem.apply)

  given resultatFormat: RootJsonFormat[Resultat] = jsonFormat(Resultat.apply, "noteD", "noteE", "endnote", "isStreichwertung", "teilresultateD", "teilresultateE", "teilresultateP")

  given dataObjectFormat: RootJsonWriter[DataObject] = (p: DataObject) => {
    p.easyprint.toJson
  }

  // actions (via rest-request)
  given refresWettkampfMap: RootJsonFormat[RefreshWettkampfMap] = jsonFormat1(RefreshWettkampfMap.apply)
  given getResultsToReplicate: RootJsonFormat[GetResultsToReplicate] = jsonFormat2(GetResultsToReplicate.apply)
  given startDurchgangFormat: RootJsonFormat[StartDurchgang] = jsonFormat2(StartDurchgang.apply)
  given resetStartDurchgangFormat: RootJsonFormat[ResetStartDurchgang] = jsonFormat2(ResetStartDurchgang.apply)
  given updateAthletWertungFormat: RootJsonFormat[UpdateAthletWertung] = jsonFormat7(UpdateAthletWertung.apply)
  given finishDurchgangStationFormat: RootJsonFormat[FinishDurchgangStation] = jsonFormat4(FinishDurchgangStation.apply)
  given finishDurchgangFormat: RootJsonFormat[FinishDurchgang] = jsonFormat2(FinishDurchgang.apply)
  given finishDurchgangStepFormat: RootJsonFormat[FinishDurchgangStep] = jsonFormat1(FinishDurchgangStep.apply)
  given publishScores: RootJsonFormat[PublishScores] = jsonFormat4(PublishScores.apply)
  given approveDonation: RootJsonFormat[DonationApproved] = jsonFormat2(DonationApproved.apply)

  // events (via ws and rest-response)
  given durchgangStartedFormat: RootJsonFormat[DurchgangStarted] = jsonFormat3(DurchgangStarted.apply)
  given durchgangResettedFormat: RootJsonFormat[DurchgangResetted] = jsonFormat2(DurchgangResetted.apply)
  given wertungUpdatedFormat: RootJsonFormat[AthletWertungUpdated] = jsonFormat6(AthletWertungUpdated.apply)
  given wertungUpdatedFormatSeq: RootJsonFormat[AthletWertungUpdatedSequenced] = jsonFormat7(AthletWertungUpdatedSequenced.apply)
  given stationsWertungenCompletedFormat: RootJsonFormat[StationWertungenCompleted] = jsonFormat1(StationWertungenCompleted.apply)
  given newLastResultsFormat: RootJsonFormat[NewLastResults] = jsonFormat3(NewLastResults.apply)
  given durchgangFinishedFormat: RootJsonFormat[DurchgangFinished] = jsonFormat3(DurchgangFinished.apply)
  given scoresPublished: RootJsonFormat[ScoresPublished] = jsonFormat5(ScoresPublished.apply)
  given lastResults: RootJsonFormat[LastResults] = jsonFormat1(LastResults.apply)
  given bulkEvents: RootJsonFormat[BulkEvent] = new RootJsonFormat[BulkEvent] {
    override def write(obj: BulkEvent): JsValue = JsObject(
      "wettkampfUUID" -> obj.wettkampfUUID.toJson,
      "events" -> obj.events.map(_.toJson(using kutuAppEventFormat)).toJson
    )
    override def read(json: JsValue): BulkEvent = {
      val fields = json.asJsObject.fields
      BulkEvent(
        fields("wettkampfUUID").convertTo[String],
        fields("events").convertTo[List[JsValue]].map(_.convertTo[KutuAppEvent](using kutuAppEventFormat))
      )
    }
  }
  given athletRemovedFromWettkampf: RootJsonFormat[AthletRemovedFromWettkampf] = jsonFormat2(AthletRemovedFromWettkampf.apply)
  given athletMovedInWettkampf: RootJsonFormat[AthletMovedInWettkampf] = jsonFormat4(AthletMovedInWettkampf.apply)
  given durchgangChangedFormat: RootJsonFormat[DurchgangChanged] = jsonFormat3(DurchgangChanged.apply)
  given athletAddedToettkampf: RootJsonFormat[AthletsAddedToWettkampf] = jsonFormat4(AthletsAddedToWettkampf.apply)
  given messageAckFormat: RootJsonFormat[MessageAck] = jsonFormat1(MessageAck.apply)

  given athletMediaAquireFormat: RootJsonFormat[AthletMediaAquire] = jsonFormat3(AthletMediaAquire.apply)
  given athletMediaReleaseFormat: RootJsonFormat[AthletMediaRelease] = jsonFormat3(AthletMediaRelease.apply)
  given athletMediaStartFormat: RootJsonFormat[AthletMediaStart] = jsonFormat3(AthletMediaStart.apply)
  given athletMediaPauseFormat: RootJsonFormat[AthletMediaPause] = jsonFormat3(AthletMediaPause.apply)
  given athletMediaToStartFormat: RootJsonFormat[AthletMediaToStart] = jsonFormat3(AthletMediaToStart.apply)

  given useMyMediaPlayerFormat: RootJsonFormat[UseMyMediaPlayer] = jsonFormat2(UseMyMediaPlayer.apply)
  given forgetMyMediaPlayerFormat: RootJsonFormat[ForgetMyMediaPlayer] = jsonFormat2(ForgetMyMediaPlayer.apply)
  given mediaPlayerIsReadyFormat: RootJsonFormat[MediaPlayerIsReady] = jsonFormat1(MediaPlayerIsReady.apply)
  given mediaPlayerDisconnectedFormat: RootJsonFormat[MediaPlayerDisconnected] = jsonFormat1(MediaPlayerDisconnected.apply)
  given athletMediaIsFreeFormat: RootJsonFormat[AthletMediaIsFree] = jsonFormat2(AthletMediaIsFree.apply)
  given athletMediaIsAtStartFormat: RootJsonFormat[AthletMediaIsAtStart] = jsonFormat2(AthletMediaIsAtStart.apply)
  given athletMediaIsRunningFormat: RootJsonFormat[AthletMediaIsRunning] = jsonFormat2(AthletMediaIsRunning.apply)
  given athletMediaIsPausedFormat: RootJsonFormat[AthletMediaIsPaused] = jsonFormat2(AthletMediaIsPaused.apply)

  // support for websocket incoming json-messages
  val caseClassesJsonFormatter: Map[String, JsonFormat[? <: KutuAppEvent]] = Map(
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
    classOf[DurchgangChanged].getSimpleName -> durchgangChangedFormat,
    classOf[MessageAck].getSimpleName -> messageAckFormat,
    classOf[UseMyMediaPlayer].getSimpleName -> useMyMediaPlayerFormat,
    classOf[ForgetMyMediaPlayer].getSimpleName -> forgetMyMediaPlayerFormat,
    classOf[MediaPlayerIsReady].getSimpleName -> mediaPlayerIsReadyFormat,
    classOf[MediaPlayerDisconnected].getSimpleName -> mediaPlayerDisconnectedFormat,
    classOf[AthletMediaAquire].getSimpleName -> athletMediaAquireFormat,
    classOf[AthletMediaRelease].getSimpleName -> athletMediaReleaseFormat,
    classOf[AthletMediaStart].getSimpleName -> athletMediaStartFormat,
    classOf[AthletMediaPause].getSimpleName -> athletMediaPauseFormat,
    classOf[AthletMediaToStart].getSimpleName -> athletMediaToStartFormat,
    classOf[AthletMediaIsFree].getSimpleName -> athletMediaIsFreeFormat,
    classOf[AthletMediaIsAtStart].getSimpleName -> athletMediaIsAtStartFormat,
    classOf[AthletMediaIsRunning].getSimpleName -> athletMediaIsRunningFormat,
    classOf[AthletMediaIsPaused].getSimpleName -> athletMediaIsPausedFormat,
  )

  given messagesFormatter: RootJsonFormat[? <: KutuAppEvent] = new RootJsonFormat[? <: KutuAppEvent] {
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
            "events" -> JsArray(be.events.map(ev => write(ev)).toList)
          ))
        case _ =>
          caseClassesJsonFormatter.get(obj.getClass.getSimpleName) match {
            case Some(jsonWriter) =>
              try {
                jsonWriter.asInstanceOf[JsonFormat[KutuAppEvent]].write(obj).addFields(Map("type" -> JsString(obj.getClass.getSimpleName)))
              } catch {
                case e: Exception =>
                  println(obj)
                  throw e
              }
            case _ => throw new Exception(s"Unable to find jsonFormatter for $obj")
          }
      }
    }
  }

  //  given syncAddVereinActionFormat = jsonFormat1(AddVereinAction)
  //  given syncAddRegistrationActionFormat = jsonFormat3(AddRegistration)
  //  given syncMoveRegistrationActionFormat = jsonFormat4(MoveRegistration)
  //  given syncRemoveRegistrationActionFormat = jsonFormat3(RemoveRegistration)

  given syncActionFormatter: RootJsonFormat[SyncAction] = new RootJsonFormat[SyncAction] {
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
  given baseSyncActionListFormat: AnyRef & RootJsonFormat[List[SyncAction]] = listFormat(using syncActionFormatter)
}
