package ch.seidel.kutu.domain

import org.scalatest.funsuite.AnyFunSuiteLike
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
}
