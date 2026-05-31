package ch.seidel.kutu.data

import ch.seidel.kutu.domain.{Athlet, AthletView, Wertung}
import org.scalatest.funsuite.AnyFunSuite

class ResourceExchangerTest extends AnyFunSuite {

  test("testGetValues") {
    assert(ResourceExchanger.getValues(Athlet(33)) ===
      "\"0\",\"0\",\"M\",\"<Name>\",\"<Vorname>\",\"\",\"\",\"\",\"\",\"33\",\"true\"")
  }

  test("testGetHeader") {
    assert(ResourceExchanger.getHeader[AthletView] ===
      "\"id\",\"js_id\",\"geschlecht\",\"name\",\"vorname\",\"gebdat\",\"strasse\",\"plz\",\"ort\",\"verein\",\"activ\"")
  }

  test("testWertungHeaderContainsReserve") {
    assert(ResourceExchanger.getHeader[Wertung].contains("\"reserve\""))
  }

  test("testWertungValuesContainReserve") {
    val value = ResourceExchanger.getValues(Wertung(1, 2, 3, 4, "uuid", None, None, None, None, None, Some(1), None, None, reserve = Some(2)))
    assert(value.endsWith("\"2\""))
  }

}
