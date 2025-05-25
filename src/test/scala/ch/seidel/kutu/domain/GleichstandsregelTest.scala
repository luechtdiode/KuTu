package ch.seidel.kutu.domain

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelTest extends AnyWordSpec with Matchers {

  val diszipline = List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck")

  def testWertungen(birthdate: LocalDate = LocalDate.of(2004, 3, 2),
                    wertungen: List[String] = diszipline.zipWithIndex.map(_._2).map(i => s"${7.5-i},${3.2+i/3*2}")) = {
    val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None, None, None)
    val a = Athlet(1L).copy(name = s"Testathlet", gebdat = Some(birthdate)).toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    for (
      geraet <- diszipline.zip(wertungen).zipWithIndex
    )
    yield {
      val wd = WettkampfdisziplinView(100 + geraet._2, ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1), Disziplin(geraet._2, geraet._1._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
      geraet._1._2.split(",").map(BigDecimal(_)).toList match {
        case List(enote, dnote) =>
          val endnote = enote + dnote
          println(s"athlet $a, disziplin ${wd.disziplin.id} note ${enote} ${dnote} ${endnote}")
          WertungView(wd.id, a, wd, wk, Some(dnote), Some(enote), Some(endnote), None, None, 0)
      }
    }
  }
  val testResultate = testWertungen()

  "check long-range" in {
    val maxfactor = STANDARD_SCORE_FACTOR / 1000L
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("Disziplin(Boden,Pauschen,Ring,Sprung,Barren)").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Summe").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Summe/JugendVorAlter").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Best").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/D-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung)").factorize(testResultate) < maxfactor)
    assertThrows[RuntimeException](Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring,Barren)/E-Note-Summe/E-Note-Best/D-Note-Summe/D-Note-Best/JugendVorAlter"))
  }

  "Ohne - Default" in {
    assert(Gleichstandsregel("Ohne").factorize(testResultate) == 1000000000000000000L)
    assert(Gleichstandsregel("").factorize(testResultate) == 1000000000000000000L)
  }

  "wettkampf-constructor" in {
    assert(Gleichstandsregel(20L).toFormel == "Disziplin(Schaukelringe,Sprung,Reck)")
    assert(Gleichstandsregel(testResultate.head.wettkampf).toFormel == "Ohne")
  }

  "Jugend vor Alter" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("JugendVorAlter").factorize(testResultate) == 810000000000000000L)
  }

  "factorize E-Note-Best" in {
    println(testResultate.map(_.noteE).max)
    assert(Gleichstandsregel("E-Note-Best").factorize(testResultate) == 750000000000000000L)
  }

  "factorize E-Note-Summe" in {
    println(testResultate.map(w => w.resultat).reduce(_+_).noteE)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate) == 300000000000000000L)
  }

  "factorize D-Note-Best" in {
    println(testResultate.map(w => w.resultat).map(_.noteE).max)
    assert(Gleichstandsregel("D-Note-Best").factorize(testResultate) == 520000000000000000L)
  }

  "factorize D-Note-Summe" in {
    println(testResultate.map(w => w.resultat).reduce(_+_).noteD)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testResultate) == 252000000000000000L)
  }

  "factorize Disziplin" in {
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testResultate) == 253700000000000000L)
  }

  "factorize StreichDisziplin" in {
    // 5=Reck, 3=Sprung, 1=Pauschen, (2=Ring, 4=Barren, 0=Boden)
    assert(Gleichstandsregel("StreichDisziplin(Reck,Sprung,Pauschen)").factorize(testResultate) ==         14188500000000000L)
  }

  "test" in {
    // 5=Reck, 3=Sprung, 1=Pauschen, (2=Ring, 4=Barren, 0=Boden)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate) ==                                                                      300000000000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testResultate) ==                                                          300007500000000000L)
    assert(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) ==                           68426000000000000L)
    assert(Gleichstandsregel("StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) ==                           69926000000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) == 300007500068426000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) == 300007500069926000L)
  }

  "factorize StreichWertungen" in {
    assert(Gleichstandsregel("StreichWertungen").factorize(testResultate) ==         5345000000000000000L)
  }
  "factorize StreichWertungen(Endnote)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote)").factorize(testResultate) ==         5345000000000000000L)
  }
  "factorize StreichWertungen(E-Note)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note)").factorize(testResultate) ==         3275000000000000000L)
  }
  "factorize StreichWertungen(D-Note)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note)").factorize(testResultate) ==         2580000000000000000L)
  }
  "factorize StreichWertungen(Endnote,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote,Max)").factorize(testResultate) ==         4775000000000000000L)
  }
  "factorize StreichWertungen(E-Note,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note,Max)").factorize(testResultate) ==         2225000000000000000L)
  }
  "factorize StreichWertungen(D-Note,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note,Max)").factorize(testResultate) ==         2040000000000000000L)
  }
  "factorize StreichWertungen(Endnote,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote,Min)").factorize(testResultate) ==         5345000000000000000L)
  }
  "factorize StreichWertungen(E-Note,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note,Min)").factorize(testResultate) ==         3275000000000000000L)
  }
  "factorize StreichWertungen(D-Note,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note,Min)").factorize(testResultate) ==         2580000000000000000L)
  }
  "factorize StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)").factorize(testResultate) ==         5345032750258000000L)
  }

  "construct combined rules" in {
    assert(Gleichstandsregel("JugendVorAlter").factorize(testResultate)                          == 810000000000000000L)
    assert(Gleichstandsregel("E-Note-Best").factorize(testResultate)                             == 750000000000000000L)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate)                            == 300000000000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testResultate)                == 300007500000000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testResultate) == 300007500810000000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) == 300007500068426000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) == 300007500069926000L)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichWertungen").factorize(testResultate) == 300007505345000000L)
    assert(Gleichstandsregel("E-Note-Best/E-Note-Summe/JugendVorAlter").factorize(testResultate) == 750030000810000000L)
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").factorize(testResultate) == 817500300000000000L)
  }

  "toFormel" in {
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").toFormel == "JugendVorAlter/E-Note-Best/E-Note-Summe")
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").toFormel == "Disziplin(Reck,Sprung,Pauschen)")
    assert(Gleichstandsregel("StreichDisziplin(Reck,Sprung,Pauschen)").toFormel == "StreichDisziplin(Reck,Sprung,Pauschen)")
    assert(Gleichstandsregel("StreichWertungen").toFormel == "StreichWertungen(Endnote,Min)")
    assert(Gleichstandsregel("StreichWertungen(Endnote)").toFormel == "StreichWertungen(Endnote,Min)")
    assert(Gleichstandsregel("StreichWertungen(Endnote,Min)").toFormel == "StreichWertungen(Endnote,Min)")
    assert(Gleichstandsregel("StreichWertungen(Endnote,Max)").toFormel == "StreichWertungen(Endnote,Max)")
    assert(Gleichstandsregel("StreichWertungen(E-Note,Max)").toFormel == "StreichWertungen(E-Note,Max)")
    assert(Gleichstandsregel("StreichWertungen(D-Note,Max)").toFormel == "StreichWertungen(D-Note,Max)")
    assert(Gleichstandsregel("D-Note-Best/D-Note-Summe").toFormel == "D-Note-Best/D-Note-Summe")
  }
}
