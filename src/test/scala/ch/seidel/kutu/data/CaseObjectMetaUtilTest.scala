package ch.seidel.kutu.data

import ch.seidel.kutu.domain._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CaseObjectMetaUtilTest extends FunSuite {
  private val rm = reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  test("testCopyWithValues") {
    val athlet = Athlet(33)

    val newathlet = CaseObjectMetaUtil.copyWithValues(athlet, Map("strasse" -> "Teststrasse"))
    assert(newathlet.strasse === "Teststrasse")
    assert(newathlet.verein === Some(33))
  }

  test("testToMap") {
    val athlet = Athlet(33)

    val map = CaseObjectMetaUtil.toMap(athlet)
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

    val athlet = CaseObjectMetaUtil.mergeMissingProperties(keepingAthlet, toDeletAthlet)

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

  import scala.reflect.runtime.universe._

  private def getValues[T: TypeTag : reflect.ClassTag](instance: T) = {
    val im = rm.reflect(instance)
    val values = typeOf[T].members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        im.reflectMethod(m).apply() match {
          case Some(verein: Verein) => verein.id + ""
          case Some(programm: Programm) => programm.id + ""
          case Some(athlet: Athlet) => athlet.id + ""
          case Some(athlet: AthletView) => athlet.id + ""
          case Some(disziplin: Disziplin) => disziplin.id + ""
          case Some(value) => value.toString
          case None => ""
          case e => e.toString
        }
    }
    values.map("\"" + _ + "\"").mkString(",")
  }
}
