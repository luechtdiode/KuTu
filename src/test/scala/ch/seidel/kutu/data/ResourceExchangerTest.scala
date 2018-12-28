package ch.seidel.kutu.data

import ch.seidel.kutu.domain.{Athlet, AthletView}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ResourceExchangerTest extends FunSuite {

  test("testGetValues") {
    assert(ResourceExchanger.getValues(Athlet(33)) ===
      "\"true\",\"33\",\"\",\"\",\"\",\"\",\"<Vorname>\",\"<Name>\",\"M\",\"0\",\"0\"")
  }

  test("testGetHeader") {
    assert(ResourceExchanger.getHeader[AthletView] ===
      "\"activ\",\"verein\",\"ort\",\"plz\",\"strasse\",\"gebdat\",\"vorname\",\"name\",\"geschlecht\",\"js_id\",\"id\"")
  }

}
