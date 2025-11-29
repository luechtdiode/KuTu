package ch.seidel.kutu.domain

import ch.seidel.kutu.Config
import ch.seidel.kutu.data.{ByGeschlecht, ByProgramm}
import ch.seidel.kutu.renderer.{ScoreToHtmlRenderer, ServerPrintUtil}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelGeTuTest extends AnyWordSpec with Matchers {
  val logodir = new java.io.File(Config.homedir)
  val logofile = ServerPrintUtil.locateLogoFile(logodir)

  val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 20L, 0, BigDecimal(0d), "", None, None, Some("Disziplin(Schaukelringe,Sprung,Reck)"), None, None)
  val renderer = new ScoreToHtmlRenderer() {
    override val title: String = wk.titel
  }
  val diszipline = List("Reck", "Boden", "Schaukelringe", "Sprung")
  val testprogramm = ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1, 0)

  def testWertungen(name: String, wertungen: List[Double]) = {
    val a = Athlet(1L).copy(vorname = name, name = "Muster", gebdat = Some(LocalDate.of(2004, 3, 2)), geschlecht = "W").toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    for (
      geraet <- diszipline.zip(wertungen).zipWithIndex
    )
    yield {
      val wd = WettkampfdisziplinView(100 + geraet._2, testprogramm, Disziplin(geraet._2, geraet._1._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
      val enote = geraet._1._2
      val endnote = enote
      println(s"athlet $a, disziplin ${wd.disziplin} note ${enote} ${endnote}")
      WertungView(wd.id, a, wd, wk, None, Some(enote), Some(endnote), None, None, 0, None, None)
    }
  }

  "test assigned Gleichstandsregel Disziplin(Schaukelringe,Sprung,Reck)" in {
    assert(Gleichstandsregel(wk.punktegleichstandsregel.get).toFormel === "Disziplin(Schaukelringe,Sprung,Reck)")
  }

  "test with GroupLeaf" in {
                                                 //   3                    1             2
                                                 // "Reck", "Boden", "Schaukelringe", "Sprung"
    val athlet1 = testWertungen("Erstrangiert",  List(9.40,    9.15,            1.55,    5.30)) // 1’550’000 + 53’000 + 940 + 1’603’940
    val athlet2 = testWertungen("Zweitrangiert", List(9.40,    9.15,            1.50,    5.35)) // 1’500’000 + 53’500 + 940 + 1’554’440
    val athlet3 = testWertungen("Drittrangiert", List(9.45,    9.15,            1.50,    5.30)) // 1’500’000 + 53’000 + 945 + 1’553’945
    val athlet4 = testWertungen("Vietrangiert",  List(9.40,    9.20,            1.50,    5.30)) // 1’500’000 + 53’000 + 940 + 1’553’940
    val wertungen = athlet1 ++ athlet2 ++ athlet3 ++ athlet4
    val athletFilter = (line: String) => wertungen.find(w => line.contains(w.athlet.vorname)).map(_.athlet.vorname)
    val query =  ByProgramm() / ByGeschlecht()
    val html = renderer
      .toHTML(query.select(wertungen).toList, athletsPerPage = 0, sortAlphabetically = false, isAvgOnMultipleCompetitions = true, logofile)
    val extract = html.split("\n").flatMap(athletFilter)
    val expected = List(
      "Erstrangiert",
      "Zweitrangiert",
      "Drittrangiert",
      "Vietrangiert")

    assert(extract === expected)
  }


  "test2 with GroupLeaf" in {
                                                  //   3                    1             2
                                                  // "Reck", "Boden", "Schaukelringe", "Sprung"
    val athlet1 = testWertungen("Erstrangiert",  List(9.40,    9.15,            1.51,    5.30))
    val athlet2 = testWertungen("Zweitrangiert", List(9.40,    9.14,            1.50,    5.32))
    val athlet3 = testWertungen("Drittrangiert", List(9.43,    9.13,            1.50,    5.30))
    val athlet4 = testWertungen("Vietrangiert",  List(9.40,    9.16,            1.50,    5.30))
    val wertungen = athlet1 ++ athlet2 ++ athlet3 ++ athlet4
    val athletFilter = (line: String) => wertungen.find(w => line.contains(w.athlet.vorname)).map(_.athlet.vorname)
    val query =  ByProgramm() / ByGeschlecht()
    val html = renderer
      .toHTML(query.select(wertungen).toList, athletsPerPage = 0, sortAlphabetically = false, isAvgOnMultipleCompetitions = true, logofile)
    val extract = html.split("\n").flatMap(athletFilter)
    val expected = List(
      "Erstrangiert",
      "Zweitrangiert",
      "Drittrangiert",
      "Vietrangiert")

    assert(extract === expected)
  }
}
