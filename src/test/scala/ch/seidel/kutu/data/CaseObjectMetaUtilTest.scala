package ch.seidel.kutu.data

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.data.{labels, caseClassToMap, mapToCaseClass, mergeMissingProperties}
import org.scalatest.funsuite.AnyFunSuite

class CaseObjectMetaUtilTest extends AnyFunSuite {
  private val rm = reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  test("testCopyWithValues") {
    val athlet: Athlet = Athlet.apply(33)

    val newathlet = copyToCaseClass[Athlet](athlet, Map("strasse" -> "Teststrasse"))
    assert(newathlet.strasse === "Teststrasse")
    assert(newathlet.verein === Some(33))
  }

  test("testToMap") {
    val athlet = Athlet(33)

    val map = caseClassToMap(athlet)
    assert(map("verein") === Some(33))
  }

  test("testMergeMissingProperties") {
    val keepingAthlet: Athlet = Athlet(33).copy(
      name = "TestathletName1",
      vorname = "TestathletVorname1",
      geschlecht = "W"
    )
    val gebdate = Some(new java.sql.Date(System.currentTimeMillis()))
    val toDeletAthlet: Athlet = keepingAthlet.copy(
      id = 1L,
      js_id = 11,
      geschlecht = "M",
      name = "TestathletName2",
      vorname = "TestathletVorname2",
      gebdat = gebdate,
      strasse = "Teststrasse2",
      plz = "1234",
      ort = "Testort2",
      verein = None,
      activ = false
    )

    val athlet = mergeMissingProperties(keepingAthlet, toDeletAthlet)

    assert(athlet.id === 0L) // ID should not be overridden
    assert(athlet.js_id === 11)
    assert(athlet.geschlecht === "W") // Nonempty Strings should not be overridden
    assert(athlet.name === "TestathletName1") // Nonempty Strings should not be overridden
    assert(athlet.vorname === "TestathletVorname1") // Nonempty Strings should not be overridden
    assert(athlet.gebdat === gebdate)
    assert(athlet.strasse === "Teststrasse2")
    assert(athlet.plz === "1234")
    assert(athlet.ort === "Testort2")
    assert(athlet.verein === Some(33)) // None is less than Some(33)
    assert(athlet.activ === true)// false is les than true
  }

}
