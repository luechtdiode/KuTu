package ch.seidel.kutu.http

import ch.seidel.kutu.actors.*
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate, ScoreCalcTemplateView, ScoreCalcVariable}
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*


trait JsonSupport extends SprayJsonSupport with EnrichedJson {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol.*


  given wkFormat: RootJsonFormat[Wettkampf] = jsonFormat(Wettkampf.apply, "id", "uuid", "datum", "titel", "programmId", "auszeichnung", "auszeichnungendnote", "notificationEMail", "altersklassen", "jahrgangsklassen", "punktegleichstandsregel", "rotation", "teamrule")
  given pgmFormat: RootJsonFormat[ProgrammRaw] = jsonFormat10(ProgrammRaw.apply)
  given programmViewFormat: RootJsonFormat[ch.seidel.kutu.domain.ProgrammView] = new RootJsonFormat[ch.seidel.kutu.domain.ProgrammView] {
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

  given wertungFormat: RootJsonFormat[Wertung] = jsonFormat(Wertung.apply, "id", "athletId", "wettkampfdisziplinId", "wettkampfId", "wettkampfUUID", "noteD", "noteE", "endnote", "riege", "riege2", "team", "mediafile", "variables", "reserve")
  /*given wertungFormat: RootJsonFormat[Wertung] = new RootJsonFormat[Wertung] {
    override def write(obj: Wertung): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "athletId" -> obj.athletId.toJson,
      "wettkampfdisziplinId" -> obj.wettkampfdisziplinId.toJson,
      "wettkampfId" -> obj.wettkampfId.toJson,
      "wettkampfUUID" -> obj.wettkampfUUID.toJson,
      "noteD" -> obj.noteD.toJson,
      "noteE" -> obj.noteE.toJson,
      "endnote" -> obj.endnote.toJson,
      "riege" -> obj.riege.toJson,
      "riege2" -> obj.riege2.toJson,
      "team" -> obj.team.toJson,
      "mediafile" -> obj.mediafile.toJson,
      "variables" -> obj.variables.toJson,
      "reserve" -> obj.reserve.toJson
    )

    override def read(json: JsValue): Wertung = {
      val fields = json.asJsObject.fields
      Wertung(
        id = fields("id").convertTo[Long],
        athletId = fields("athletId").convertTo[Long],
        wettkampfdisziplinId = fields("wettkampfdisziplinId").convertTo[Long],
        wettkampfId = fields("wettkampfId").convertTo[Long],
        wettkampfUUID = fields("wettkampfUUID").convertTo[String],
        noteD = fields.get("noteD").flatMap(_.convertTo[Option[BigDecimal]]),
        noteE = fields.get("noteE").flatMap(_.convertTo[Option[BigDecimal]]),
        endnote = fields.get("endnote").flatMap(_.convertTo[Option[BigDecimal]]),
        riege = fields.get("riege").flatMap(_.convertTo[Option[String]]),
        riege2 = fields.get("riege2").flatMap(_.convertTo[Option[String]]),
        team = normalizeZeroToNone(fields.get("team").flatMap(_.convertTo[Option[Int]]).getOrElse(0)),
        mediafile = fields.get("mediafile").flatMap(_.convertTo[Option[Media]]),
        variables = fields.get("variables").flatMap(_.convertTo[Option[ScoreCalcTemplateView]]),
        reserve = normalizeZeroToNone(fields.get("reserve").map(_.convertTo[Int]).getOrElse(0))
      )
    }
  }
*/
  given vereinFormat: RootJsonFormat[Verein] = jsonFormat(Verein.apply, "id", "name", "verband")
  given atheltViewFormat: RootJsonFormat[AthletView] = jsonFormat(AthletView.apply, "id", "js_id", "geschlecht", "name", "vorname", "gebdat", "strasse", "plz", "ort", "verein", "activ")
  given wertungContainerFormat: RootJsonFormat[WertungContainer] = jsonFormat11(WertungContainer.apply)
  given registrationFormat: RootJsonFormat[Registration] = jsonFormat11(Registration.apply)
  given newregistrationFormat: RootJsonFormat[NewRegistration] = jsonFormat8(NewRegistration.apply)
  given resetRegistrationPWFormat: RootJsonFormat[RegistrationResetPW] = jsonFormat3(RegistrationResetPW.apply)
  given athletregistrationFormat: RootJsonFormat[AthletRegistration] = jsonFormat13(AthletRegistration.apply)
  /*
  given athletregistrationFormatx: RootJsonFormat[AthletRegistration] = new RootJsonFormat[AthletRegistration] {
    override def write(obj: AthletRegistration): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "vereinregistrationId" -> obj.vereinregistrationId.toJson,
      "athletId" -> obj.athletId.toJson,
      "geschlecht" -> obj.geschlecht.toJson,
      "name" -> obj.name.toJson,
      "vorname" -> obj.vorname.toJson,
      "gebdat" -> obj.gebdat.toJson,
      "programId" -> obj.programId.toJson,
      "registrationTime" -> obj.registrationTime.toJson,
      "athlet" -> obj.athlet.toJson,
      "team" -> obj.team.toJson,
      "mediafile" -> obj.mediafile.toJson,
      "reserve" -> obj.reserve.toJson
    )
    override def read(json: JsValue): AthletRegistration = {
      val fields = json.asJsObject.fields
      AthletRegistration(
        id = fields("id").convertTo[Long],
        vereinregistrationId = fields("vereinregistrationId").convertTo[Long],
        athletId = fields.get("athletId").flatMap(_.convertTo[Option[Long]]),
        geschlecht = fields("geschlecht").convertTo[String],
        name = fields("name").convertTo[String],
        vorname = fields("vorname").convertTo[String],
        gebdat = fields("gebdat").convertTo[String],
        programId = fields("programId").convertTo[Long],
        registrationTime = fields("registrationTime").convertTo[Long],
        athlet = fields.get("athlet").flatMap(_.convertTo[Option[AthletView]]),
        team = fields.get("team").flatMap(_.convertTo[Option[Int]]),
        mediafile = fields.get("mediafile").flatMap(_.convertTo[Option[MediaAdmin]]),
        reserve = fields.get("reserve").map(_.convertTo[Int]).getOrElse(0)
      )
    }
  }
*/
  given teamFormat: RootJsonFormat[TeamItem] = jsonFormat2(TeamItem.apply)
  given judgeregistrationFormat: RootJsonFormat[JudgeRegistration] = jsonFormat9(JudgeRegistration.apply)
  given judgeregistrationPgmFormat: RootJsonFormat[JudgeRegistrationProgram] = jsonFormat5(JudgeRegistrationProgram.apply)
  given judgeRegistrationProgramItemFormat: RootJsonFormat[JudgeRegistrationProgramItem] = jsonFormat3(JudgeRegistrationProgramItem.apply)

  given resultatFormat: RootJsonFormat[Resultat] = jsonFormat(Resultat.apply, "noteD", "noteE", "endnote", "isStreichwertung", "teilresultateD", "teilresultateE", "teilresultateP")

  given adminCreateCompetitionRequestFormat: RootJsonFormat[AdminCreateCompetitionRequest] = jsonFormat(AdminCreateCompetitionRequest.apply, "datum", "titel", "programmId", "notificationEMail", "auszeichnung", "auszeichnungendnote", "altersklassen", "jahrgangsklassen", "punktegleichstandsregel", "rotation", "teamrule", "creatorName", "creatorAddress", "creatorPhone", "termsAccepted", "termsVersion")
  given adminCreateCompetitionResponseFormat: RootJsonFormat[AdminCreateCompetitionResponse] = jsonFormat(AdminCreateCompetitionResponse.apply, "uuid", "titel", "datum", "secret")

  given riegeSuggestionRequestFormat: RootJsonFormat[RiegeSuggestionRequest] = jsonFormat(RiegeSuggestionRequest.apply, "maxRiegenSize", "splitPgm", "splitSexOption", "onDisziplinIds", "separateRiegen2Durchgaenge")
  given updateRiegeRequestFormat: RootJsonFormat[UpdateRiegeRequest] = jsonFormat4(UpdateRiegeRequest.apply)
  given riegeItemFormat: RootJsonFormat[RiegeItem] = jsonFormat6(RiegeItem.apply)
  given durchgangDurationItemFormat: RootJsonFormat[DurchgangDurationItem] = jsonFormat7(DurchgangDurationItem.apply)
  given riegePreviewResponseFormat: RootJsonFormat[RiegePreviewResponse] = jsonFormat2(RiegePreviewResponse.apply)

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
        "events" -> obj.events.map(_.toJson(using messagesFormatter)).toJson
    )
    override def read(json: JsValue): BulkEvent = {
      val fields = json.asJsObject.fields
      BulkEvent(
        fields("wettkampfUUID").convertTo[String],
        fields("events").convertTo[List[JsValue]].map(_.convertTo[KutuAppEvent](using messagesFormatter))
      )
    }
  }
  given athletRemovedFromWettkampf: RootJsonFormat[AthletRemovedFromWettkampf] = jsonFormat2(AthletRemovedFromWettkampf.apply)
  given athletMovedInWettkampf: RootJsonFormat[AthletMovedInWettkampf] = jsonFormat5(AthletMovedInWettkampf.apply)
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
  private val caseClassesJsonFormatter: Map[String, JsonFormat[? <: KutuAppEvent]] = Map(
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

  given messagesFormatter: RootJsonFormat[KutuAppEvent] = new RootJsonFormat[KutuAppEvent] {
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
        case null => throw new Exception(s"Unable to find jsonFormatter for $obj")
      }
    }
  }
  given baseSyncActionListFormat: RootJsonFormat[List[SyncAction]] = listFormat(using syncActionFormatter)
}
