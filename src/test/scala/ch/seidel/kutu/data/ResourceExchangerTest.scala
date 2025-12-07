package ch.seidel.kutu.data

import ch.seidel.kutu.domain.{Athlet, AthletView}
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

}
