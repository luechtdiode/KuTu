package ch.seidel.kutu.domain

import ch.seidel.kutu.domain.TeamRegel.predefined
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TeamRegelTest extends AnyWordSpec with Matchers {
  "VereinGerät(3/4)" in {
    val regel = TeamRegel("VereinGerät(3/4)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGerät(3/4)" )
  }
  "VereinGerät(3/*)" in {
    val regel = TeamRegel("VereinGerät(3/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGerät(3/*)" )
  }
  "VereinGesamt(3/4)" in {
    val regel = TeamRegel("VereinGesamt(3/4)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGesamt(3/4)" )
  }
  "VereinGesamt(3/*)" in {
    val regel = TeamRegel("VereinGesamt(3/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGesamt(3/*)" )
  }
  "VerbandGerät(3/4)" in {
    val regel = TeamRegel("VerbandGerät(3/4)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VerbandGerät(3/4)" )
  }
  "VerbandGerät(3/*)" in {
    val regel = TeamRegel("VerbandGerät(3/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VerbandGerät(3/*)" )
  }
  "VerbandGesamt(3/4)" in {
    val regel = TeamRegel("VerbandGesamt(3/4)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VerbandGesamt(3/4)" )
  }
  "VerbandGesamt(3/*)" in {
    val regel = TeamRegel("VerbandGesamt(3/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VerbandGesamt(3/*)" )
  }
  "VereinGerät[K5+K6+K7/KH+KD](3/*)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](3/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](3/*)" )

  }
  "VereinGerät(avg/2/*)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](avg/2/*)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](avg/2/*)" )
  }
  "VereinGerät(avg/*/4)" in {
    val regel = TeamRegel("VereinGerät[K5+K6+K7/KH+KD](avg/*/4)")
    assert( regel.teamsAllowed == true )
    assert( regel.toFormel == "VereinGerät[K5+K6+K7/KH+KD](avg/*/4)" )
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
