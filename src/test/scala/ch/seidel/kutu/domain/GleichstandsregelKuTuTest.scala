package ch.seidel.kutu.domain

import ch.seidel.kutu.Config
import ch.seidel.kutu.data.{ByGeschlecht, ByProgramm}
import ch.seidel.kutu.renderer.{PrintUtil, ScoreToHtmlRenderer}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

class GleichstandsregelKuTuTest extends AnyWordSpec with Matchers {
  val logodir = new java.io.File(Config.homedir)
  val logofile = PrintUtil.locateLogoFile(logodir)

  val wk = Wettkampf(1L, None, LocalDate.of(2023, 3, 3), "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, Some("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)"), None, None)
  val renderer = new ScoreToHtmlRenderer() {
    override val title: String = wk.titel
  }
  val diszipline = List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck")
  val testprogramm = ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1, 0)

  def testWertungen(name: String, wertungen: List[(BigDecimal,BigDecimal)]) = {
    val a = Athlet(1L).copy(vorname = name, name = "Muster", gebdat = Some(LocalDate.of(2004, 3, 2)), geschlecht = "M").toAthletView(Some(Verein(1L, "Testverein", Some("Testverband"))))
    for (
      geraet <- diszipline.zip(wertungen).zipWithIndex
    )
    yield {
      val wd = WettkampfdisziplinView(100 + geraet._2, testprogramm, Disziplin(geraet._2, geraet._1._1), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
      val (dnote, enote) = geraet._1._2
      val endnote = enote + dnote
      println(s"athlet $a, disziplin ${wd.disziplin} note  d${dnote} e${enote} = ${endnote}")
      WertungView(wd.id, a, wd, wk, Some(dnote.setScale(1)), Some(enote.setScale(3)), Some(endnote), None, None, 0, None, None)
    }
  }

  "test constructors" in {
    Gleichstandsregel("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote)")
    Gleichstandsregel("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote)/StreichWertungen(E-Note)")
    Gleichstandsregel("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote)/StreichWertungen(E-Note)/StreichWertungen(D-Note)")
  }

  "test assigned Gleichstandsregel KuTu STV" in {
    assert(Gleichstandsregel(wk.punktegleichstandsregel.get).toFormel === "E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)")
  }

  "test with GroupLeaf" in {
                                                 // "Boden",       "Pauschen",     "Ring",    "Sprung",   "Barren",   "Reck"
    val athlet1 = testWertungen("Erstrangiert",  List((1.1, 9.410), (1.2, 9.420), (1.3, 9.430),(1.4, 9.440),(1.4, 9.550),(1.5, 9.560))) // best E
    val athlet2 = testWertungen("Zweitrangiert", List((1.1, 9.410), (1.2, 9.420), (1.3, 9.430),(1.4, 9.440),(2.5, 8.450),(2.6, 8.460))) // best D
    val athlet3 = testWertungen("Drittrangiert", List((1.2, 9.420), (1.2, 9.420), (1.2, 9.420),(1.4, 9.440),(2.5, 8.450),(2.6, 8.460))) // best min-endnote
    val athlet4 = testWertungen("Vietrangiert",  List((1.2, 9.430), (1.2, 9.430), (1.2, 9.430),(1.4, 9.430),(2.5, 8.430),(2.6, 8.460))) // best min enote
    val athlet5 = testWertungen("Fünftrangiert", List((1.3, 9.430), (1.3, 9.430), (1.3, 9.430),(1.3, 9.430),(2.3, 8.430),(2.6, 8.460))) // last
    val wertungen = athlet1 ++ athlet2 ++ athlet3 ++ athlet4 ++ athlet5
    val athletFilter = (line: String) => wertungen.find(w => line.contains(w.athlet.vorname)).map(_.athlet.vorname)
    val query =  ByProgramm() / ByGeschlecht()
    val html = renderer
      .toHTML(query.select(wertungen).toList, athletsPerPage = 0, sortAlphabetically = false, isAvgOnMultipleCompetitions = true, logofile)
    println(html)
    val extract = html.split("\n").flatMap(athletFilter)
    val expected = Array(
      "Erstrangiert",
      "Zweitrangiert",
      "Drittrangiert",
      "Vietrangiert",
      "Fünftrangiert"
    )

    assert(extract === expected)
  }
}
