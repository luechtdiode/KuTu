package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Assertions._

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelTest extends AnyWordSpec with Matchers {

  val testWertungen = {
    // class Wettkampf(id: Long, uuid: Option[String], datum: Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: BigDecimal, notificationEMail: String, altersklassen: Option[String], jahrgangsklassen: Option[String], punktgleichstandregel: Option[String])
    val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None)
    val a = Athlet(1L).copy(name = s"Testathlet", gebdat = Some(LocalDate.of(2004, 3, 2))).toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    val d = for (
      geraet <- List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck").zipWithIndex
    )
    yield {
      WettkampfdisziplinView(100 + geraet._2, ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1), Disziplin(geraet._2, geraet._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
    }
    for (wd <- d) yield {
      // id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: Option[scala.math.BigDecimal], noteE: Option[scala.math.BigDecimal], endnote: Option[scala.math.BigDecimal], riege: Option[String], riege2: Option[String]) extends DataObject {
      val enote = 7.5 - wd.disziplin.id
      val dnote = 3.2 + wd.disziplin.id
      val endnote = enote + dnote
      WertungView(wd.id, a, wd, wk, Some(BigDecimal(dnote)), Some(BigDecimal(enote)), Some(BigDecimal(endnote)), None, None)
    }
  }

  "Ohne - Default" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("Ohne").factorize(testWertungen.head, testWertungen) == 10000000000L)
    assert(Gleichstandsregel("").factorize(testWertungen.head, testWertungen) == 10000000000L)
  }

  "Jugend vor Alter" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testWertungen) == 810000000000L)
  }

  "factorize E-Note-Best" in {
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testWertungen) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(1).head, testWertungen) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(2).head, testWertungen) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(3).head, testWertungen) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(4).head, testWertungen) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(5).head, testWertungen) == 75000000000000L)
  }

  "factorize E-Note-Summe" in {
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testWertungen) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(1).head, testWertungen) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(2).head, testWertungen) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(3).head, testWertungen) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(4).head, testWertungen) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(5).head, testWertungen) == 300000000000000L)
  }

  "factorize D-Note-Best" in {
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.head, testWertungen) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(1).head, testWertungen) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(2).head, testWertungen) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(3).head, testWertungen) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(4).head, testWertungen) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(5).head, testWertungen) == 82000000000000L)
  }

  "factorize D-Note-Summe" in {
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.head, testWertungen) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(1).head, testWertungen) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(2).head, testWertungen) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(3).head, testWertungen) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(4).head, testWertungen) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(5).head, testWertungen) == 342000000000000L)
  }

  "factorize Disciplin" in {
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.head, testWertungen) ==         10000000000L)
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(1).head, testWertungen) == 1000000000000L)
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(2).head, testWertungen) == 10000000000L)
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(3).head, testWertungen) == 100000000000000L)
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(4).head, testWertungen) == 10000000000L)
    assert(Gleichstandsregel("Disciplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(5).head, testWertungen) == 10000000000000000L)
  }

  "construct combined rules" in {
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testWertungen)                          ==    810000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testWertungen)                             ==  75000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testWertungen)                            == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testWertungen.head, testWertungen)                == 307500000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testWertungen.head, testWertungen) == 307508100000000L)
    assert(Gleichstandsregel("E-Note-Best/E-Note-Summe/JugendVorAlter").factorize(testWertungen.head, testWertungen) == 105008100000000L)
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").factorize(testWertungen.head, testWertungen) ==  11310000000000L)
  }
}
