package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

class VereinSpec extends KuTuBaseSpec {
  "verein" should {
    "create" in {
      val verein = createVerein("Testverein", Some("Verband"))
      assert(verein > 0L)
    }
    "findVereinLike" in {
      val verein = Verein(0, "TestVerein", Some("Verband"))
      findVereinLike(verein) match {
        case Some(id) => assert(id > 0L)
        case _ =>
          fail("should find Verein with case insensitive name")
      }
    }
    "insertVerein" in {
      val verein = Verein(0, "TESTVerein", Some("Verband"))
      findVereinLike(verein) match {
        case Some(id) =>
          val updated = insertVerein(verein)
          updated.id should ===(id)
          updated.name should ===(verein.name)
          
        case _ =>
          fail("should find Verein with case insensitive name")
      }
    }
    "insertNewVerein" in {
      val newverein = Verein(0, "NewVerein", Some("Verband"))
      findVereinLike(newverein) match {
        case Some(id) if (id > 0) =>
          println(id, selectVereine.map(_.id), newverein)
          fail("should not find Verein with unique name")
        case _ =>
          assert(insertVerein(newverein).id > 0L)
      }
    }
    "delete" in {
      val vereinToDelete = selectVereine.sortBy(_.id).toList.last
      val idToDelete = vereinToDelete.id
      val athlet = Athlet(vereinToDelete)
      val persistedAthlet = insertAthlete(athlet)
      val w = updateOrinsertWertung(Wertung(0, athlet.id, 1, 1, "",
        Some(scala.math.BigDecimal(1.0)), Some(scala.math.BigDecimal(1.0)), Some(scala.math.BigDecimal(1.0)),
        Some("R1"), Some("R2")))
      deleteVerein(idToDelete)
      val remainingId = selectVereine.sortBy(_.id).toList.last.id
      remainingId should !==(idToDelete)
      
      assert(selectAthletesOfVerein(idToDelete).isEmpty)
      
      assert(selectWertungen(athletId = Some(athlet.id)).isEmpty)
    }
  }  
}