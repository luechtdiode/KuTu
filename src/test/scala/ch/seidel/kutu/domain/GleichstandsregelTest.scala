package ch.seidel.kutu.domain

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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

  "check long-range" in {
    val maxfactor = STANDARD_SCORE_FACTOR / 100
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring)").factorize(testWertungen.head, testResultate) < maxfactor)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden)/E-Note-Summe/E-Note-Best/D-Note-Summe/D-Note-Best/JugendVorAlter").factorize(testWertungen.head, testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testWertungen.head, testResultate) < maxfactor)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring,Barren)/E-Note-Summe/E-Note-Best/D-Note-Summe/D-Note-Best/JugendVorAlter").factorize(testWertungen.head, testResultate) < maxfactor)
    //assertThrows[RuntimeException](Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring,Barren)"))
    //assertThrows[RuntimeException](Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring,Barren)/E-Note-Summe/E-Note-Best/D-Note-Summe/D-Note-Best/JugendVorAlter"))
  }

  "Ohne - Default" in {
    assert(Gleichstandsregel("Ohne").factorize(testWertungen.head, testResultate) == 100000000000L)
    assertThrows[RuntimeException](Gleichstandsregel("").factorize(testWertungen.head, testResultate))
  }

  "wettkampf-constructor" in {
    assert(Gleichstandsregel(20L).toFormel == "Disziplin(Schaukelringe,Sprung,Reck)")
    assert(Gleichstandsregel(testWertungen.head.wettkampf).toFormel == "Ohne")
  }

  "Jugend vor Alter" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testResultate) == 81000000000L)
  }

  "factorize E-Note-Best" in {
    println(testResultate.map(_.noteE).max)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testResultate) == 75000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(1).head, testResultate) == 75000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(2).head, testResultate) == 75000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(3).head, testResultate) == 75000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(4).head, testResultate) == 75000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.drop(5).head, testResultate) == 75000000000L)
  }

  "factorize E-Note-Summe" in {
    println(testResultate.reduce(_+_).noteE)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testResultate) == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(1).head, testResultate) == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(2).head, testResultate) == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(3).head, testResultate) == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(4).head, testResultate) == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.drop(5).head, testResultate) == 30000000000L)
  }

  "factorize D-Note-Best" in {
    println(testResultate.map(_.noteE).max)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.head, testResultate) == 82000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(1).head, testResultate) == 82000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(2).head, testResultate) == 82000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(3).head, testResultate) == 82000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(4).head, testResultate) == 82000000000L)
    assert(Gleichstandsregel("D-Note-Best").factorize(testWertungen.drop(5).head, testResultate) == 82000000000L)
  }

  "factorize D-Note-Summe" in {
    println(testResultate.reduce(_+_).noteD)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.head, testResultate) == 34200000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(1).head, testResultate) == 34200000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(2).head, testResultate) == 34200000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(3).head, testResultate) == 34200000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(4).head, testResultate) == 34200000000L)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testWertungen.drop(5).head, testResultate) == 34200000000L)
  }

  "factorize Disziplin" in {
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.head, testResultate) ==               0L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(1).head, testResultate) ==     300L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(2).head, testResultate) ==       0L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(3).head, testResultate) ==  900000L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(4).head, testResultate) ==       0L)
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testWertungen.drop(5).head, testResultate) == 6775365L)
  }

  "construct combined rules" in {
    assert(Gleichstandsregel("JugendVorAlter").factorize(testWertungen.head, testResultate)                          == 81000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testWertungen.head, testResultate)                             == 75000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testWertungen.head, testResultate)                            == 30000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testWertungen.head, testResultate)                == 30000750000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testWertungen.head, testResultate) == 30000750081L)
    assert(Gleichstandsregel("E-Note-Best/E-Note-Summe/JugendVorAlter").factorize(testWertungen.head, testResultate) == 75003000081L)
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").factorize(testWertungen.head, testResultate) == 81750030000L)
  }

  "toFormel" in {
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe/Disziplin(Reck,Sprung,Pauschen)").toFormel == "JugendVorAlter/E-Note-Best/E-Note-Summe/Disziplin(Reck,Sprung,Pauschen)")
  }
}
