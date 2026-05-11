package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelTest extends AnyWordSpec with Matchers {
  private val diszipline = List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck")

  private def testWertungen(
      name: String,
      birthdate: LocalDate = LocalDate.of(2004, 3, 2),
      wertungen: List[String] = diszipline.zipWithIndex.map(_._2).map(i => s"${7.5 - i},${3.2 + i / 3 * 2}")
  ): List[WertungView] = {
    val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None, None, None)
    val a = Athlet(1L).copy(name = name, gebdat = Some(birthdate)).toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    diszipline.zip(wertungen).zipWithIndex.map { geraet =>
      val wd = WettkampfdisziplinView(100 + geraet._2, ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1, 0), Disziplin(geraet._2, geraet._1._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
      geraet._1._2.split(",").map(BigDecimal(_)).toList match {
        case List(enote, dnote) =>
          val endnote = enote + dnote
          WertungView(wd.id, a, wd, wk, Some(dnote), Some(enote), Some(endnote), None, None, 0, None, None)
        case _ =>
          WertungView(wd.id, a, wd, wk, None, None, None, None, None, 0, None, None)
      }
    }
  }

  "Gleichstandsregel comparator" should {
    "treat Ohne as equal" in {
      val left = testWertungen("A")
      val right = testWertungen("B")
      Gleichstandsregel("Ohne").compare(left, right) shouldBe 0
    }

    "prefer higher E-Note-Summe" in {
      val left = testWertungen("A", wertungen = List("8.5,3.0", "8.0,3.0", "7.9,3.0", "7.8,3.0", "7.7,3.0", "7.6,3.0"))
      val right = testWertungen("B", wertungen = List("8.4,3.0", "8.0,3.0", "7.9,3.0", "7.8,3.0", "7.7,3.0", "7.6,3.0"))
      Gleichstandsregel("E-Note-Summe").compare(left, right) should be > 0
    }

    "prefer higher D-Note-Best" in {
      val left = testWertungen("A", wertungen = List("7.5,3.6", "7.0,3.0", "6.9,3.0", "6.8,3.0", "6.7,3.0", "6.6,3.0"))
      val right = testWertungen("B", wertungen = List("7.5,3.5", "7.0,3.0", "6.9,3.0", "6.8,3.0", "6.7,3.0", "6.6,3.0"))
      Gleichstandsregel("D-Note-Best").compare(left, right) should be > 0
    }

    "prefer younger athlete for JugendVorAlter" in {
      val younger = testWertungen("A", birthdate = LocalDate.of(2008, 1, 1))
      val older = testWertungen("B", birthdate = LocalDate.of(2002, 1, 1))
      Gleichstandsregel("JugendVorAlter").compare(younger, older) should be > 0
    }

    "respect chain order for nested rules" in {
      val left = testWertungen("A", wertungen = List("8.0,3.0", "7.0,3.0", "7.0,3.0", "7.0,3.0", "7.0,3.0", "7.0,3.0"))
      val right = testWertungen("B", wertungen = List("8.0,3.0", "7.0,3.0", "7.0,3.0", "7.0,3.0", "7.0,3.0", "6.9,3.1"))
      val regel = Gleichstandsregel("E-Note-Summe/D-Note-Summe")
      regel.compare(left, right) should be > 0
    }

    "compare Disziplin in configured order" in {
      val left = testWertungen("A", wertungen = List("7.0,3.0", "7.0,3.0", "7.0,3.0", "8.5,3.0", "7.0,3.0", "7.0,3.0"))
      val right = testWertungen("B", wertungen = List("7.0,3.0", "7.0,3.0", "7.0,3.0", "8.4,3.0", "7.0,3.0", "7.0,3.0"))
      Gleichstandsregel("Disziplin(Sprung,Reck)").compare(left, right) should be > 0
    }

    "treat fully identical values as tie" in {
      val left = testWertungen("A")
      val right = testWertungen("B")
      Gleichstandsregel("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote,Min)").compare(left, right) shouldBe 0
    }

    "keep constructor behavior for defaults and formula serialization" in {
      Gleichstandsregel(20L).toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
      Gleichstandsregel("StreichWertungen").toFormel shouldBe "StreichWertungen(Endnote,Min)"
      Gleichstandsregel("JugendVorAlter/E-Note-Best/E-Note-Summe").toFormel shouldBe "JugendVorAlter/E-Note-Best/E-Note-Summe"
    }
  }
}
