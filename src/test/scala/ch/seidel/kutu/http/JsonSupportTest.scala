package ch.seidel.kutu.http

import ch.seidel.kutu.actors.{AthletWertungUpdated, KutuAppEvent}
import ch.seidel.kutu.domain.{AdminTokenRequest, AdminTokenResponse, ApproveEMailRequest, ApproveEMailResponse, CreatorMetaData, SyncActionKey, SyncApplyRequest, SyncApplyResponse}
import org.scalatest.funsuite.AnyFunSuite
import spray.json.{JsNumber, _}

class JsonSupportTest extends AnyFunSuite with JsonSupport with EnrichedJson {

  test("testOptionalDeSerialize without team") {
    val eventText =
      """{
        |"athlet":{"activ":true,"gebdat":"2009-05-13T00:00:00.000+0000","geschlecht":"M","id":853,"js_id":0,"name":"Muster","ort":"","plz":"","strasse":"",
        |"verein":{"id":16,"name":"BTV Basel","verband":"BLTV"},"vorname":"Maximilian"},"durchgang":"","geraet":6,"programm":"K2","type":"AthletWertungUpdated",
        |"wertung":{"athletId":853,"endnote":8.6,"id":46636,"noteD":0,"noteE":8.6,"riege":"M,K2,BTV Basel","riege2":"Barren K2","wettkampfId":24,"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251","wettkampfdisziplinId":82},
        |"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251"
        |}""".stripMargin.replace("\n", "").replace("\r", "").replace(" ", "")
    val event = eventText.asType[KutuAppEvent].asInstanceOf[AthletWertungUpdated]
    assert(
      event.wertung.team === None)
    assert(event.wertung.reserve === None)

    val roundtripped = event.toJson.toJsonStringWithType(event).asType[KutuAppEvent].asInstanceOf[AthletWertungUpdated]
    assert(roundtripped.wertung.team === None)
    assert(roundtripped.wertung.reserve === None)
  }

  test("testOptionalDeSerialize with team") {
    val event = """{
                  |"athlet":{"activ":true,"gebdat":"2009-05-13T00:00:00.000+0000","geschlecht":"M","id":853,"js_id":0,"name":"Muster","ort":"","plz":"","strasse":"",
                  |"verein":{"id":16,"name":"BTV Basel","verband":"BLTV"},"vorname":"Maximilian"},"durchgang":"","geraet":6,"programm":"K2","type":"AthletWertungUpdated",
                  |"wertung":{"athletId":853,"endnote":8.6,"id":46636,"noteD":0,"noteE":8.6,"riege":"M,K2,BTV Basel","riege2":"Barren K2","wettkampfId":24,"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251","wettkampfdisziplinId":82, "team":2, "reserve":3},
                  |"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251"
                  |}""".stripMargin.asType[KutuAppEvent].asInstanceOf[AthletWertungUpdated]
    assert(
      event.wertung.team === Some(2))
    assert(event.wertung.reserve === Some(3))
  }

  test("testSyncActionKeySerializationRoundtrip") {
    val key = SyncActionKey(registrationId = 42L, athletId = Some(7L), oldVereinId = None)
    val json = key.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncActionKey]
    assert(deserialized === key)
  }

  test("testSyncActionKeySerializationRoundtripWithOldVerein") {
    val key = SyncActionKey(registrationId = 1L, athletId = None, oldVereinId = Some(99L))
    val json = key.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncActionKey]
    assert(deserialized === key)
  }

  test("testSyncActionKeySerializationDefaultValues") {
    val key = SyncActionKey(registrationId = 5L)
    val json = key.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncActionKey]
    assert(deserialized === key)
    val fields = json.parseJson.asJsObject.fields
    assert(fields("registrationId") === JsNumber(5))
    assert(fields.get("athletId") === None)
    assert(fields.get("oldVereinId") === None)
  }

  test("testSyncApplyRequestSerializationRoundtrip") {
    val request = SyncApplyRequest(List(
      SyncActionKey(1L, Some(2L)),
      SyncActionKey(3L, Some(4L), Some(5L))
    ))
    val json = request.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncApplyRequest]
    assert(deserialized === request)
    val actions = json.parseJson.asJsObject.fields("actions").asInstanceOf[spray.json.JsArray]
    assert(actions.elements.length === 2)
  }

  test("testSyncApplyRequestSerializationEmptyList") {
    val request = SyncApplyRequest(List.empty)
    val json = request.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncApplyRequest]
    assert(deserialized === request)
    assert(deserialized.actions.isEmpty)
  }

  test("testSyncApplyResponseSerializationRoundtrip") {
    val response = SyncApplyResponse(processed = 3, messages = List("ok", "created new team"))
    val json = response.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncApplyResponse]
    assert(deserialized === response)
    val fields = json.parseJson.asJsObject.fields
    assert(fields("processed") === JsNumber(3))
    assert(fields("messages").asInstanceOf[spray.json.JsArray].elements.length === 2)
  }

  test("testSyncApplyResponseSerializationEmptyMessages") {
    val response = SyncApplyResponse(processed = 0, messages = List.empty)
    val json = response.toJson.compactPrint
    val deserialized = json.parseJson.convertTo[SyncApplyResponse]
    assert(deserialized === response)
  }

  test("testCreatorMetaDataSerializationRoundtrip") {
    val creator = CreatorMetaData("Hans Muster", "Musterstrasse 1, 1234 Musterstadt", "+49 123 456789", "1.0")
    val json = creator.toJson.compactPrint
    assert(json.parseJson.convertTo[CreatorMetaData] === creator)
    assert(json.parseJson.asJsObject.fields.keys == Set("creatorName", "creatorAddress", "creatorPhone", "termsVersion"))
    assert(creator.isComplete)
    assert(!creator.copy(creatorPhone = "  ").isComplete)
    assert(!creator.copy(termsVersion = null).isComplete)
  }

  test("testAdminTokenRequestSerializationRoundtrip") {
    val creator = CreatorMetaData("Hans", "Musterstrasse 1", "+49 123", "1.0")
    val request = AdminTokenRequest(creator)
    val json = request.toJson.compactPrint
    assert(json.parseJson.convertTo[AdminTokenRequest] === request)
    assert(json.parseJson.asJsObject.fields("creator").asJsObject.fields.keys == Set("creatorName", "creatorAddress", "creatorPhone", "termsVersion"))
  }

  test("testAdminTokenResponseSerializationRoundtrip") {
    val response = AdminTokenResponse("a.b.c")
    val json = response.toJson.compactPrint
    assert(json.parseJson.convertTo[AdminTokenResponse] === response)
    assert(json.parseJson.asJsObject.fields("token") === JsString("a.b.c"))
  }

  test("testApproveEMailRequestSerializationRoundtrip") {
    val request = ApproveEMailRequest("org@test.ch", CreatorMetaData("Hans", "Musterstrasse 1", "+49 123", "1.0"))
    val json = request.toJson.compactPrint
    assert(json.parseJson.convertTo[ApproveEMailRequest] === request)
  }

  test("testApproveEMailResponseSerializationRoundtrip") {
    val response = ApproveEMailResponse("EMail erfolgreich verifiziert", success = true)
    val json = response.toJson.compactPrint
    assert(json.parseJson.convertTo[ApproveEMailResponse] === response)
    assert(json.parseJson.asJsObject.fields("success") === JsBoolean(true))
  }

  test("testDataObjectWriterIsOnlyUsedAsFallback") {
    // Der generische DataObject-Writer darf spezifische Formate nicht verdrängen.
    val response = AdminTokenResponse("a.b.c")
    assert(response.toJson.compactPrint === """{"token":"a.b.c"}""")
  }
}
