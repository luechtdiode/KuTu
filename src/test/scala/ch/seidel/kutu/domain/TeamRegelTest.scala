package ch.seidel.kutu.domain

import ch.seidel.kutu.domain.TeamRegel.predefined
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class TeamRegelTest extends AnyWordSpec with Matchers {
  private val programm = ProgrammView(1L, "K6", 0, None, 1, 0, 99, "", 1, 0)
  private val disziplin = Disziplin(1L, "Boden")
  private val wd = WettkampfdisziplinView(1L, programm, disziplin, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 0, 0, 30, 1)
  private val verein = Verein(1L, "TV Test", Some("Testverband"))

  private def mkWertung(athletId: Long, endnote: BigDecimal, teamRule: String, reserve: Int): WertungView = {
    val wk = Wettkampf(1L, Some("wk-1"), LocalDate.now(), "WK", programm.id, 0, BigDecimal(0), "", Some(""), Some(""), Some(""), Some(""), Some(teamRule))
    val athlet = AthletView(athletId, 0, "M", s"Name$athletId", s"Vorname$athletId", Some(LocalDate.of(2010, 1, 1)), "", "", "", Some(verein), activ = true)
    WertungView(athletId, athlet, wd, wk, Some(0), Some(endnote), Some(endnote), None, None, 1, None, None, reserve = reserve)
  }

  "VereinGerät(3/4)" in {
    val regel = TeamRegel("VereinGerät(3/4)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGerät(3/4)" )
  }
  "VereinGerät(3/*)" in {
    val regel = TeamRegel("VereinGerät(3/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGerät(3/*)" )
  }
  "VereinGesamt(3/4)" in {
    val regel = TeamRegel("VereinGesamt(3/4)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGesamt(3/4)" )
  }
  "VereinGesamt(3/*)" in {
    val regel = TeamRegel("VereinGesamt(3/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGesamt(3/*)" )
  }
  "VerbandGerät(3/4)" in {
    val regel = TeamRegel("VerbandGerät(3/4)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VerbandGerät(3/4)" )
  }
  "VerbandGerät(3/*)" in {
    val regel = TeamRegel("VerbandGerät(3/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VerbandGerät(3/*)" )
  }
  "VerbandGesamt(3/4)" in {
    val regel = TeamRegel("VerbandGesamt(3/4)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VerbandGesamt(3/4)" )
  }
  "VerbandGesamt(3/*)" in {
    val regel = TeamRegel("VerbandGesamt(3/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VerbandGesamt(3/*)" )
  }
  "VereinGerät[K5+K6+K7/KH+KD](3/*)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](3/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](3/*)" )

  }
  "VereinGerät(avg/2/*)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](avg/2/*)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](avg/2/*)" )
  }
  "VereinGerät(avg/*/4)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](avg/*/4)")
    assert(regel.teamsAllowed)
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](avg/*/4)" )
  }
  "Reserve wird bei max Teamgrösse nachrangig aktiviert" in {
    val regel = TeamRegel("VereinGesamt(2/2)")
    val wertungen = List(
      mkWertung(1L, BigDecimal(9.0), "VereinGesamt(2/2)", reserve = 0),
      mkWertung(2L, BigDecimal(8.5), "VereinGesamt(2/2)", reserve = 0),
      mkWertung(3L, BigDecimal(9.5), "VereinGesamt(2/2)", reserve = 1)
    )

    val teams = regel.extractTeams(wertungen)
    teams should have size 1
    teams.head.wertungen.map(_.athlet.id).toSet shouldBe Set(1L, 2L)
  }

  "Reserve zählt bei unbegrenzter Teamgrösse mit" in {
    val regel = TeamRegel("VereinGesamt(2/*)")
    val wertungen = List(
      mkWertung(1L, BigDecimal(9.0), "VereinGesamt(2/*)", reserve = 0),
      mkWertung(2L, BigDecimal(8.5), "VereinGesamt(2/*)", reserve = 0),
      mkWertung(3L, BigDecimal(9.5), "VereinGesamt(2/*)", reserve = 1)
    )

    val teams = regel.extractTeams(wertungen)
    teams should have size 1
    teams.head.wertungen.map(_.athlet.id).toSet shouldBe Set(1L, 2L, 3L)
  }

  "Reserve 0 wird bei toWertung normalisiert" in {
    val wk = Wettkampf(1L, Some("wk-1"), LocalDate.now(), "WK", programm.id, 0, BigDecimal(0), "", Some(""), Some(""), Some(""), Some(""), Some("VereinGesamt(2/2)"))
    val athlet = AthletView(1L, 0, "M", "Name1", "Vorname1", Some(LocalDate.of(2010, 1, 1)), "", "", "", Some(verein), activ = true)
    val wertungView = WertungView(1L, athlet, wd, wk, Some(0), Some(BigDecimal(9.0)), Some(BigDecimal(9.0)), None, None, 1, None, None)

    wertungView.toWertung.reserve shouldBe None
  }

  "VerbandGerät[M+W](devmax/2/*/TeamA+TeamB)" in {
    val regel = TeamRegel("VerbandGerät[M+W](devmax/2/*/TeamA+TeamB)")
    assert(regel.teamsAllowed)
    assert(regel.toFormel == "VerbandGerät[M+W](devmax/2/*/TeamA+TeamB)")
  }

  "Mehrere Regeln werden normalisiert serialisiert" in {
    val regel = TeamRegel("VereinGesamt(3/4), VerbandGesamt(avg/*/*)")
    assert(regel.teamsAllowed)
    assert(regel.toFormel == "VereinGesamt(3/4),VerbandGesamt(avg/*/*)")
  }

  "VereinGesamt[M+W/K6+K7](avg/2/4/Team A+Team B+Team C)" in {
    val regel = TeamRegel("VereinGesamt[M+W/K6+K7](avg/2/4/Team A+Team B+Team C)")
    assert(regel.teamsAllowed)
    assert(regel.toFormel == "VereinGesamt[M+W/K6+K7](avg/2/4/Team A+Team B+Team C)")
  }

  "Test all predefined Rules" in {
    for( definition <- predefined) {
      val (description, formula) = definition
      if (description.equals("Keine Teams")) {
        println(s"test $description / $formula ...")
        val regel = TeamRegel(formula)
        assert(!regel.teamsAllowed)
      }
      else if (!description.equals("Individuell")) {
        println(s"test $description / $formula ...")
        val regel = TeamRegel(formula)
        assert(regel.teamsAllowed)
        assert(regel.toFormel == formula)
      }
    }
  }
}
