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

    "findVereinLike with non-exact match using like on verband" in {
      // Create a test verein with specific verband
      val testId = createVerein("FuzzySearchTest", Some("TestVerband123"))

      // Search with partial verband match
      val searchVerein = Verein(0, "FuzzySearchTest", Some("Verband"))
      findVereinLike(searchVerein, exact = false) match {
        case Some(id) if id > 0 =>
          id should ===(testId)
        case _ =>
          fail("should find Verein with partial verband match")
      }
    }

    "findVereinLike should be case-insensitive for exact match" in {
      // Create a verein with mixed case
      val testId = createVerein("MixedCaseVerein", Some("TestVerband"))

      // Search with different case
      val searchVerein = Verein(0, "mixedcaseverein", Some("testverband"))
      findVereinLike(searchVerein) match {
        case Some(id) if id > 0 =>
          id should ===(testId)
        case _ =>
          fail("should find Verein with case-insensitive exact match")
      }
    }

    "selectVereine should return all vereine sorted by name" in {
      val vereinsBefore = selectVereine
      val newVerein = createVerein("ZZZ_xxxxTestVerein", Some("TestVerband"))
      val vereinsAfter = selectVereine

      assert(vereinsAfter.size > vereinsBefore.size)
      assert(vereinsAfter.map(_.id).contains(newVerein))

      // Verify sorting by name
      val names = vereinsAfter.map(_.name)
      names should ===(names.sorted)
    }

    "findVereinLike should return None when no match found" in {
      val nonExistentVerein = Verein(0, "ThisVereinDoesNotExist12345", Some("NoVerband"))
      findVereinLike(nonExistentVerein, exact = false) match {
        case Some(id) if id > 0 =>
          fail("should not find non-existent Verein")
        case _ =>
          // Expected behavior - no match found
          succeed
      }
    }

    "findVereinLike with exact=false documents fuzzy matching behavior" should {
      // This test documents that findVereinLike with exact=false attempts multiple matching strategies:
      // 1. exact name + LIKE verband
      // 2. LIKE name + LIKE verband
      // 3. If name has commas and above fail: exact name-part (>3 chars) + LIKE verband
      // 4. If above fails: LIKE name-part + LIKE verband
      // The algorithm reports each attempt via println (see console logs)

      // Test: Should match with exact name + partial verband (step 1 succeeds)
      "findVereinLike with exact=false should find with exact name + partial verband" in {
        val testId = createVerein("ExactClubName", Some("Long Verband Federation Name"))
        val search = Verein(0, "ExactClubName", Some("Verband"))
        findVereinLike(search, exact = false) match {
          case Some(id) if id > 0 => id should ===(testId)
          case _ => fail("should find with exact name + partial verband")
        }
      }
      "findVereinLike with exact=false should find with partial name + exact verband" in {
        val testId = createVerein("TV ExactClubName", Some("Verband Federation Name"))
        val search = Verein(0, "TV ExactClubName,TZ Thierstein Dorneck", Some("Verband Federation Name"))
        findVereinLike(search, exact = false) match {
          case Some(id) if id > 0 => id should ===(testId)
          case _ => fail("should find with partial name + exact verband")
        }
      }
      "findVereinLike with exact=false should find with partial name-like + exact verband" in {
        val testId = createVerein("TV ExactClubName2", Some("Verband Federation Name"))
        val search = Verein(0, "ExactClubName2,TZ Thierstein Dorneck", Some("Verband Federation Name"))
        findVereinLike(search, exact = false) match {
          case Some(id) if id > 0 => id should ===(testId)
          case _ => fail("should find with partial name + exact verband")
        }
      }
    }

    "insertVerein should update existing verein" in {
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
        case Some(id) if id > 0 =>
          println((id, selectVereine.map(_.id), newverein))
          fail("should not find Verein with unique name")
        case _ =>
          assert(insertVerein(newverein).id > 0L)
      }
    }

    "updateVerein should update name and verband" in {
      // Create a new verein
      val vereinId = createVerein("OriginalName", Some("OriginalVerband"))
      val originalVerein = selectVereine.find(_.id == vereinId).get

      // Update the verein
      val updatedVerein = originalVerein.copy(name = "UpdatedName", verband = Some("UpdatedVerband"))
      updateVerein(updatedVerein)

      // Verify the update
      val retrievedVerein = selectVereine.find(_.id == vereinId).get
      retrievedVerein.name should ===("UpdatedName")
      retrievedVerein.verband should ===(Some("UpdatedVerband"))
    }

    "updateVerein should handle None verband" in {
      // Create a new verein with verband
      val vereinId = createVerein("VereinWithVerband", Some("SomeVerband"))

      // Update to have no verband
      val updatedVerein = Verein(vereinId, "VereinWithVerband", None)
      updateVerein(updatedVerein)

      // Verify the update
      val retrievedVerein = selectVereine.find(_.id == vereinId).get
      retrievedVerein.verband should ===(None)
    }

    "delete verein with all associated data" in {
      val vereinToDelete = selectVereine.maxBy(_.id)
      val idToDelete = vereinToDelete.id
      val athlet = Athlet(vereinToDelete)
      val persistedAthlet = insertAthlete(athlet)
      val w: Unit = updateOrinsertWertung(Wertung(0, athlet.id, 1, 1, "",
        Some(scala.math.BigDecimal(1.0)), Some(scala.math.BigDecimal(1.0)), Some(scala.math.BigDecimal(1.0)),
        Some("R1"), Some("R2"), Some(0), None, None))
      deleteVerein(idToDelete)
      val remainingId = selectVereine.maxBy(_.id).id
      remainingId should !==(idToDelete)
      
      assert(selectAthletesOfVerein(idToDelete).isEmpty)
      
      assert(selectWertungen(athletId = Some(athlet.id)).isEmpty)
    }
  }  
}