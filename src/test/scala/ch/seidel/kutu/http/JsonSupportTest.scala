package ch.seidel.kutu.http

import ch.seidel.kutu.akka.{AthletWertungUpdated, KutuAppEvent}
import org.scalatest.funsuite.AnyFunSuite
import spray.json.enrichAny

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

    assert(event.toJson.toJsonStringWithType(event) === eventText)
  }

  test("testOptionalDeSerialize with team") {
    val event = """{
                  |"athlet":{"activ":true,"gebdat":"2009-05-13T00:00:00.000+0000","geschlecht":"M","id":853,"js_id":0,"name":"Muster","ort":"","plz":"","strasse":"",
                  |"verein":{"id":16,"name":"BTV Basel","verband":"BLTV"},"vorname":"Maximilian"},"durchgang":"","geraet":6,"programm":"K2","type":"AthletWertungUpdated",
                  |"wertung":{"athletId":853,"endnote":8.6,"id":46636,"noteD":0,"noteE":8.6,"riege":"M,K2,BTV Basel","riege2":"Barren K2","wettkampfId":24,"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251","wettkampfdisziplinId":82, "team":2},
                  |"wettkampfUUID":"3328007b-0472-46a4-94ae-3093067bc251"
                  |}""".stripMargin.asType[KutuAppEvent].asInstanceOf[AthletWertungUpdated]
    assert(
      event.wertung.team === Some(2))
  }
}
