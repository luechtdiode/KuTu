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
      val wd = WettkampfdisziplinView(100 + geraet._2, ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1, 0), Disziplin(geraet._2, geraet._1._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
      geraet._1._2.split(",").map(BigDecimal(_)).toList match {
        case List(enote, dnote) =>
          val endnote = enote + dnote
          //println(s"athlet $a, disziplin ${wd.disziplin.id} note ${enote} ${dnote} ${endnote}")
          WertungView(wd.id, a, wd, wk, Some(dnote), Some(enote), Some(endnote), None, None, 0)
      }
    }
  }
  val testResultate = testWertungen()
  val maxfactor = STANDARD_SCORE_FACTOR / 1000L


  "check long-range Disziplin" in {
    //println(GleichstandsregelDisziplin(List("Boden", "Pauschen", "Ring", "Sprung", "Barren")).factorize(testResultate))
    //println(Gleichstandsregel("Disziplin(Boden,Pauschen,Ring,Sprung,Barren)").factorize(testResultate))
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testResultate) < maxfactor)
    assert(GleichstandsregelDisziplin(List("Boden", "Pauschen", "Ring", "Sprung", "Barren")).factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("Disziplin(Boden,Pauschen,Ring,Sprung,Barren)").factorize(testResultate) < maxfactor)
  }

  "check long-range StreichDisziplin" in {
    println(GleichstandsregelStreichDisziplin(List("Boden", "Pauschen", "Ring", "Sprung", "Barren")).factorize(testResultate))
    println(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren)").factorize(testResultate))
    assert(GleichstandsregelStreichDisziplin(List("Boden", "Pauschen", "Ring")).factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring)").factorize(testResultate) < maxfactor)
    assert(GleichstandsregelStreichDisziplin(List("Boden", "Pauschen", "Ring", "Sprung", "Barren")).factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren)").factorize(testResultate) < maxfactor)
  }

  "check long-range StreichWertungen" in {
    assert(GleichstandsregelStreichWertungen("Endnote", "Min").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichWertungen(Endnote,Min)").factorize(testResultate) < maxfactor)
    assert(GleichstandsregelStreichWertungen("E-Note", "Max").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichWertungen(E-Note,Max)").factorize(testResultate) < maxfactor)
    assert(GleichstandsregelStreichWertungen("D-Note", "Max").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("StreichWertungen(D-Note,Max)").factorize(testResultate) < maxfactor)
  }

  "check long-range E-Note-Summe/E-Note-Best/D-Note-Summe" in {
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Best").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Best/D-Note-Summe").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Summe").factorize(testResultate) < maxfactor)
    assert(Gleichstandsregel("E-Note-Summe/D-Note-Summe").factorize(testResultate) < maxfactor)
  }

  "check long-range E-Note-Summe/E-Note-Best/D-Note-Summe/JugendVorAlter" in {
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Summe/JugendVorAlter").factorize(testResultate) < maxfactor)
  }

  "check long-range E-Note-Summe/E-Note-Best/D-Note-Best" in {
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/D-Note-Best").factorize(testResultate) < maxfactor)
  }


  "check long-range E-Note-Summe/D-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung)" in {
    assert(Gleichstandsregel("E-Note-Summe/D-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung)").factorize(testResultate) < maxfactor)
  }

  /*"check long-range exceptions" in {
    assertThrows[RuntimeException](Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen,Boden,Ring,Barren)/E-Note-Summe/E-Note-Best/D-Note-Summe/D-Note-Best/JugendVorAlter"))
  }*/

  "Ohne - Default" in {
    assert(Gleichstandsregel("Ohne").factorize(testResultate) == BigDecimal("100000000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("").factorize(testResultate) == BigDecimal("100000000000000000000000000000000000000000000000"))
  }

  "wettkampf-constructor" in {
    assert(Gleichstandsregel(20L).toFormel == "Disziplin(Schaukelringe,Sprung,Reck)")
    assert(Gleichstandsregel(testResultate.head.wettkampf).toFormel == "Ohne")
  }

  "Jugend vor Alter" in {
    // 100 - (2023 - 2004) = 81
    assert(Gleichstandsregel("JugendVorAlter").factorize(testResultate) == BigDecimal("81000000000000000000000000000000000000000000000"))
  }

  "factorize E-Note-Best" in {
    println(testResultate.map(_.noteE).max)
    assert(Gleichstandsregel("E-Note-Best").factorize(testResultate) == BigDecimal("75000000000000000000000000000000000000000000000"))
  }

  "factorize E-Note-Summe" in {
    println(testResultate.map(w => w.resultat).reduce(_+_).noteE)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate) ==BigDecimal("30000000000000000000000000000000000000000000000"))
  }

  "factorize D-Note-Best" in {
    println(testResultate.map(w => w.resultat).map(_.noteE).max)
    assert(Gleichstandsregel("D-Note-Best").factorize(testResultate) == BigDecimal("17333333333333333333333333333333330000000000000"))
  }

  "factorize D-Note-Summe" in {
    println(testResultate.map(w => w.resultat).reduce(_+_).noteD)
    assert(Gleichstandsregel("D-Note-Summe").factorize(testResultate) == BigDecimal("8400000000000000000000000000000000000000000000"))
  }

  "factorize Disziplin" in {
    assert(Gleichstandsregel("Disziplin(Reck,Sprung,Pauschen)").factorize(testResultate) == BigDecimal("25667744480370370370370370370370370000000000"))
  }

  "factorize StreichDisziplin" in {
    // 5=Reck, 3=Sprung, 1=Pauschen, (2=Ring, 4=Barren, 0=Boden)
    assert(Gleichstandsregel("StreichDisziplin(Reck,Sprung,Pauschen)").factorize(testResultate) == BigDecimal("1466829994474927602499618960524310000000000"))
  }

  "test" in {
    // 5=Reck, 3=Sprung, 1=Pauschen, (2=Ring, 4=Barren, 0=Boden)
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate) ==                                                                      BigDecimal("30000000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testResultate) ==                                                          BigDecimal("30750000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) ==                          BigDecimal("1374237411649254475064285384245643000000000"))
    assert(Gleichstandsregel("StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) ==                          BigDecimal("1466830004241847062756678958195213000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) == BigDecimal("30750001374237411649254475064285380000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) == BigDecimal("30750001466830004241847062756678960000000000000"))
  }

  "factorize StreichWertungen" in {
    assert(Gleichstandsregel("StreichWertungen").factorize(testResultate) ==         BigDecimal("26509160042245399771564482228507020000000000000"))
  }
  "factorize StreichWertungen(Endnote)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote)").factorize(testResultate) == BigDecimal("26509160042245399771564482228507020000000000000"))
  }
  "factorize StreichWertungen(E-Note)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note)").factorize(testResultate) == BigDecimal("15352187551602035785722215636354740000000000000"))
  }
  "factorize StreichWertungen(D-Note)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note)").factorize(testResultate) == BigDecimal("12280515396749709751411727736474980000000000000"))
  }
  "factorize StreichWertungen(Endnote,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote,Max)").factorize(testResultate) == BigDecimal("24830061580057217075837204882573980000000000000"))
  }
  "factorize StreichWertungen(E-Note,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note,Max)").factorize(testResultate) == BigDecimal("12549563330086853479501957884318300000000000000"))
  }
  "factorize StreichWertungen(D-Note,Max)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note,Max)").factorize(testResultate) == BigDecimal("11156955343869898069588157481263210000000000000"))
  }
  "factorize StreichWertungen(Endnote,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(Endnote,Min)").factorize(testResultate) == BigDecimal("26509160042245399771564482228507020000000000000"))
  }
  "factorize StreichWertungen(E-Note,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(E-Note,Min)").factorize(testResultate) == BigDecimal("15352187551602035785722215636354740000000000000"))
  }
  "factorize StreichWertungen(D-Note,Min)" in {
    assert(Gleichstandsregel("StreichWertungen(D-Note,Min)").factorize(testResultate) == BigDecimal("12280515396749709751411727736474980000000000000"))
  }
  "factorize StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)" in {
    println(Gleichstandsregel("StreichWertungen(Endnote)").factorize(testResultate))
    println(Gleichstandsregel("StreichWertungen(E-Note)").factorize(testResultate))
    println(Gleichstandsregel("StreichWertungen(D-Note)").factorize(testResultate))
    println(Gleichstandsregel("StreichWertungen(Endnote)/StreichWertungen(E-Note)").factorize(testResultate))
    println(Gleichstandsregel("StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)").factorize(testResultate))
    assert(Gleichstandsregel("StreichWertungen(Endnote)").factorize(testResultate) == BigDecimal("26509160042245399771564482228507020000000000000"))
    assert(Gleichstandsregel("StreichWertungen(Endnote)/StreichWertungen(E-Note)").factorize(testResultate) == BigDecimal("26509160042245851144210728532064990000000000000"))
    assert(Gleichstandsregel("StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)").factorize(testResultate) == BigDecimal("26509160042245851144210728542680640000000000000"))
  }

  "construct combined rules" in {
    assert(Gleichstandsregel("JugendVorAlter").factorize(testResultate)                          == BigDecimal("81000000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Best").factorize(testResultate)                             == BigDecimal("75000000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Summe").factorize(testResultate)                            == BigDecimal("30000000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best").factorize(testResultate)                == BigDecimal("30750000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/JugendVorAlter").factorize(testResultate) == BigDecimal("30831000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Boden,Pauschen,Ring,Sprung,Barren,Reck)").factorize(testResultate) == BigDecimal("30750001374237411649254475064285380000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichDisziplin(Reck,Pauschen,Ring,Sprung,Barren,Boden)").factorize(testResultate) == BigDecimal("30750001466830004241847062756678960000000000000"))
    assert(Gleichstandsregel("E-Note-Summe/E-Note-Best/StreichWertungen").factorize(testResultate) == BigDecimal("30776509160042245399771564482228510000000000000"))
    assert(Gleichstandsregel("E-Note-Best/E-Note-Summe/JugendVorAlter").factorize(testResultate) == BigDecimal("78081000000000000000000000000000000000000000000"))
    assert(Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").factorize(testResultate) == BigDecimal("81780000000000000000000000000000000000000000000"))
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
