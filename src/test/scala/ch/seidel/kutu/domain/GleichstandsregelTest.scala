package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Assertions._

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelTest extends AnyWordSpec with Matchers {

  val testWertungen = {
    val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None)
    val a = Athlet(1L).copy(name = s"Testathlet", gebdat = Some(LocalDate.of(2004, 3, 2))).toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    val d = for (
      geraet <- List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck").zipWithIndex
    )
    yield {
      WettkampfdisziplinView(100 + geraet._2, ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1), Disziplin(geraet._2, geraet._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
    }
    for (wd <- d) yield {
      val enote = 7.5 - wd.disziplin.id
      val dnote = 3.2 + wd.disziplin.id
      val endnote = enote + dnote
      WertungView(wd.id, a, wd, wk, Some(BigDecimal(dnote)), Some(BigDecimal(enote)), Some(BigDecimal(endnote)), None, None)
    }
  }
  val testResultate = testWertungen.map(_.resultat)

  "Ohne - Default" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("Ohne").factorize(testWertungen.head, testResultate) == 10000000000L)
    assert(Gleichstandsregel("").factorize(testWertungen.head, testResultate) == 10000000000L)
  }

  "wettkampf-constructor" in {
    assert(Gleichstandsregel(20L).toFormel == "Disziplin(Schaukelringe,Sprung,Reck)")
    assert(Gleichstandsregel(testWertungen.head.wettkampf).toFormel == "Ohne")
  }

  "Jugend vor Alter" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testResultate) == 810000000000L)
  }

  "factorize E-Note-Best" in {
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testResultate) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(1).head, testResultate) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(2).head, testResultate) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(3).head, testResultate) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(4).head, testResultate) == 75000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(5).head, testResultate) == 75000000000000L)
  }

  "factorize E-Note-Summe" in {
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testResultate) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(1).head, testResultate) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(2).head, testResultate) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(3).head, testResultate) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(4).head, testResultate) == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(5).head, testResultate) == 300000000000000L)
  }

  "factorize D-Note-Best" in {
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.head, testResultate) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(1).head, testResultate) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(2).head, testResultate) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(3).head, testResultate) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(4).head, testResultate) == 82000000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(5).head, testResultate) == 82000000000000L)
  }

  "factorize D-Note-Summe" in {
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.head, testResultate) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(1).head, testResultate) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(2).head, testResultate) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(3).head, testResultate) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(4).head, testResultate) == 342000000000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(5).head, testResultate) == 342000000000000L)
  }

  "factorize Disziplin" in {
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.head, testResultate) ==         10000000000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(1).head, testResultate) == 1000000000000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(2).head, testResultate) == 10000000000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(3).head, testResultate) == 100000000000000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(4).head, testResultate) == 10000000000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(5).head, testResultate) == 10000000000000000L)
  }

  "construct combined rules" in {
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testResultate)                          ==    810000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testResultate)                             ==  75000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testResultate)                            == 300000000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testWertungen.head, testResultate)                == 307500000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testWertungen.head, testResultate) == 307508100000000L)
    assert(Gleichstandsregel("E-Note-Best/E-Note-Summe/JugendVorAlter").factorize(testWertungen.head, testResultate) == 105008100000000L)
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").factorize(testWertungen.head, testResultate) ==  11310000000000L)
  }

  "toFormel" in {
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe/Disziplin(Reck,Sprung,Pauschen)").toFormel == "JugendVorAlter/E-Note-Best/E-Note-Summe/Disziplin(Reck,Sprung,Pauschen)")
  }
}
