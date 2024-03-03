package ch.seidel.kutu.domain

import java.time.LocalDate
import java.util

import org.scalatest.funsuite.AnyFunSuite

class AthletServiceTest extends AnyFunSuite with AthletService {

  val gebdat1 = java.sql.Date.valueOf(LocalDate.of(2012, 1, 1))
  val gebdat2 = java.sql.Date.valueOf(LocalDate.of(2013, 1, 1))
  val gebdat3 = java.sql.Date.valueOf(LocalDate.of(2012, 4, 28))

  val verein = 33
  val athleteList = List(
    Athlet(verein).copy(id = 1L, name = "Bolliquèr", vorname = "Sophia"),
    Athlet(verein).copy(id = 2L, name = "Boliger", vorname = "Sofia"),
    Athlet(verein).copy(id = 3L, name = "Gwerder", vorname = "Noelia", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 4L, name = "Gwerder", vorname = "Alicia", gebdat = Some(gebdat2)),
    Athlet(verein).copy(id = 5L, name = "Wittwer", vorname = "Noelia", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 6L, name = "Wittwer", vorname = "Alicia", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 7L, name = "Wittwer", vorname = "Noelia", gebdat = Some(gebdat2)),
    Athlet(verein).copy(id = 8L, name = "Witwer", vorname = "Noelja", gebdat = Some(gebdat2)),
    Athlet(verein).copy(id = 9L, name = "Müller", vorname = "Lilian", gebdat = Some(gebdat2)),
    Athlet(verein).copy(id = 10L, name = "Muller", vorname = "Lilly", gebdat = Some(gebdat2)),
    Athlet(verein).copy(id = 11L, name = "Maier", vorname = "Maria", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 12L, name = "Müller", vorname = "Maria", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 13L, name = "Huber", vorname = "Lina", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 14L, name = "Huber", vorname = "Mia", gebdat = Some(gebdat1)),
    Athlet(verein).copy(id = 15L, name = "Villiger", vorname = "Zoé", gebdat = Some(gebdat3)),
    Athlet(verein).copy(id = 16L, name = "Villiger", vorname = "Freia", gebdat = Some(gebdat3)),
    Athlet(verein).copy(id = 17L, name = "Villiger", vorname = "Anina", gebdat = Some(gebdat3)),
    Athlet(verein).copy(id = 18L, name = "Rutishuser", vorname = "Lena", gebdat = Some(gebdat3)),
    Athlet(verein).copy(id = 19L, name = "Rutishuser", vorname = "Lisa", gebdat = Some(gebdat3))
  )

  var athletes = athleteList.map(a => a.id -> a).toMap

  override def loadAthlet(key: Long) = {
    athletes.get(key)
  }

  test("testStartsSame") {
    assert(startsSameInPercent("Liliane", "Lilly") === 42)
  }

  test("testFindAthleteLike") {
    val cache = new util.ArrayList[MatchCode]
    athletes.values
      .foreach(a =>
        cache.add(MatchCode(a.id, a.name, a.vorname, a.gebdat, a.verein.getOrElse(0))))

    assert(findAthleteLike(cache, exclusive = false)(athletes(2L)) === athletes(2L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(4L)) === athletes(4L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(6L)) === athletes(6L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(8L)) === athletes(8L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(10L)) === athletes(10L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(12L)) === athletes(12L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(14L)) === athletes(14L))

    assert(findAthleteLike(cache, exclusive = false)(athletes(1L)) === athletes(1L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(3L)) === athletes(3L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(5L)) === athletes(5L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(7L)) === athletes(7L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(9L)) === athletes(9L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(11L)) === athletes(11L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(13L)) === athletes(13L))

    // trillings
    assert(findAthleteLike(cache, exclusive = false)(athletes(15L)) === athletes(15L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(16L)) === athletes(16L))
    assert(findAthleteLike(cache, exclusive = false)(athletes(17L)) === athletes(17L))

    assert(findAthleteLike(cache, exclusive = false)(athletes(18)) === athletes(18))
    assert(findAthleteLike(cache, exclusive = false)(athletes(19L)) === athletes(19))

  }

  test("testFindAthleteLike-exclusive") {
    val cache = new util.ArrayList[MatchCode]
    athletes.values
      .foreach(a =>
        cache.add(MatchCode(a.id, a.name, a.vorname, a.gebdat, a.verein.getOrElse(0))))

    assert(findAthleteLike(cache, exclusive = true)(athletes(2L)) === athletes(1L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(4L)) !== athletes(3L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(6L)) !== athletes(5L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(8L)) === athletes(7L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(10L)) === athletes(9L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(12L)) !== athletes(11L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(14L)) !== athletes(13L))

    // trillings
    assert(findAthleteLike(cache, exclusive = true)(athletes(15L)) !== athletes(16L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(16L)) !== athletes(17L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(17L)) !== athletes(15L))

    assert(findAthleteLike(cache, exclusive = true)(athletes(18)) !== athletes(19L))
    assert(findAthleteLike(cache, exclusive = true)(athletes(19L)) !== athletes(18L))

  }

  test("Lena and Lisa") {
    val athleteList = List(athletes(18), athletes(19))
    athletes = athleteList.map(a => a.id -> a).toMap
    val cache = new util.ArrayList[MatchCode]
    athleteList
      .foreach(a =>
        cache.add(MatchCode(a.id, a.name, a.vorname, a.gebdat, a.verein.getOrElse(0))))

    assert(findAthleteLike(cache, exclusive = true)(athletes(18)) !== athletes(19))
  }

  test("Sophia and Simone") {
    val gebdat1 = java.sql.Date.valueOf(LocalDate.of(2012, 7, 20))
    val gebdat2 = java.sql.Date.valueOf(LocalDate.of(2012, 7, 28))
    val a1 = Athlet(verein).copy(id = 15L, name = "Brodbeck", vorname = "Simone", gebdat = Some(gebdat1))
    val a2 = Athlet(verein).copy(id = 16L, name = "Brodbeck", vorname = "Sophia", gebdat = Some(gebdat2))
    val athleteList = List(a1, a2)
    athletes = athleteList.map(a => a.id -> a).toMap
    val cache = new util.ArrayList[MatchCode]
    athleteList
      .foreach(a =>
        cache.add(MatchCode(a.id, a.name, a.vorname, a.gebdat, a.verein.getOrElse(0))))

    assert(findAthleteLike(cache, exclusive = true)(a1) !== a2)
  }


}
