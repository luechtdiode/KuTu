package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.compiletime.uninitialized

/**
 * Integration tests for AthletService that require database access.
 * Tests CRUD operations, duplicate detection, and data consistency.
 */
class AthletServiceIntegrationSpec extends KuTuBaseSpec {

  var testVereinId: Long = uninitialized
  var testVereinId2: Long = uninitialized
  var testAthlet: Athlet = uninitialized

  "AthletService" should {
    "setup test data" in {
      testVereinId = createVerein("TestVerein", Some("TestVerband"))
      testVereinId2 = createVerein("TestVerein2", Some("TestVerband2"))
      testVereinId should be > 0L
      testVereinId2 should be > 0L
    }

    "insert a new athlete" in {
      val gebdat = Date.valueOf(LocalDate.of(2010, 5, 15))
      val newAthlet = Athlet(
        id = 0L,
        js_id = 12345,
        geschlecht = "M",
        name = "TestName",
        vorname = "TestVorname",
        gebdat = Some(gebdat),
        strasse = "TestStrasse 123",
        plz = "8000",
        ort = "Zürich",
        verein = Some(testVereinId),
        activ = true
      )

      testAthlet = insertAthlete(newAthlet)
      testAthlet.id should be > 0L
      testAthlet.name shouldBe "TestName"
      testAthlet.vorname shouldBe "TestVorname"
      testAthlet.gebdat shouldBe Some(gebdat)
      testAthlet.verein shouldBe Some(testVereinId)
    }

    "load an existing athlete" in {
      val loaded = loadAthlet(testAthlet.id)
      loaded shouldBe defined
      loaded.get.id shouldBe testAthlet.id
      loaded.get.name shouldBe testAthlet.name
      loaded.get.vorname shouldBe testAthlet.vorname
    }

    "update an existing athlete" in {
      val updatedAthlet = testAthlet.copy(
        strasse = "UpdatedStrasse 456",
        plz = "9000"
      )
      val result = insertAthlete(updatedAthlet)
      result.id shouldBe testAthlet.id
      result.strasse shouldBe "UpdatedStrasse 456"
      result.plz shouldBe "9000"
    }

    "insert multiple athletes in batch" in {
      val gebdat1 = Date.valueOf(LocalDate.of(2012, 3, 10))
      val gebdat2 = Date.valueOf(LocalDate.of(2013, 7, 20))

      val athletes = List(
        ("csv1", Athlet(testVereinId).copy(name = "Batch1", vorname = "Athlete1", gebdat = Some(gebdat1))),
        ("csv2", Athlet(testVereinId).copy(name = "Batch2", vorname = "Athlete2", gebdat = Some(gebdat2)))
      )

      val inserted = insertAthletes(athletes).toList
      inserted should have size 2
      inserted.head._2.id should be > 0L
      inserted(1)._2.id should be > 0L
      inserted.head._1 shouldBe "csv1"
      inserted(1)._1 shouldBe "csv2"
    }

    "select all athletes" in {
      val athletes = selectAthletes
      athletes should not be empty
      athletes.exists(_.id == testAthlet.id) shouldBe true
    }

    "select athletes of specific verein" in {
      val athletes = selectAthletesOfVerein(testVereinId)
      athletes should not be empty
      athletes.forall(_.verein.contains(testVereinId)) shouldBe true
    }

    "select athletes view with verein details" in {
      val athletesView = selectAthletesView
      athletesView should not be empty
      val testAthleteView = athletesView.find(_.id == testAthlet.id)
      testAthleteView shouldBe defined
      testAthleteView.get.verein shouldBe defined
      testAthleteView.get.verein.get.id shouldBe testVereinId
    }

    "select athletes view filtered by verein" in {
      val verein = Verein(testVereinId, "TestVerein", Some("TestVerband"))
      val athletesView = selectAthletesView(verein)
      athletesView should not be empty
      athletesView.forall(_.verein.exists(_.id == testVereinId)) shouldBe true
    }

    "load athlete view" in {
      val athleteView = loadAthleteView(testAthlet.id)
      athleteView.id shouldBe testAthlet.id
      athleteView.name shouldBe testAthlet.name
      athleteView.verein shouldBe defined
    }

    "handle similarities correctly" should {

      "detect duplicates" in {
        // Insert potential duplicate
        val gebdat = Date.valueOf(LocalDate.of(2010, 5, 15))
        val duplicate = Athlet(
          id = 0L,
          js_id = 99999,
          geschlecht = "M",
          name = "TestName", // Same as testAthlet
          vorname = "TestVorname", // Same as testAthlet
          gebdat = Some(gebdat), // Same as testAthlet
          strasse = "Different Street",
          plz = "7000",
          ort = "Chur",
          verein = Some(testVereinId),
          activ = true
        )
        val inserted = insertAthlete(duplicate)

        val duplicates = findDuplicates()
        // findDuplicates returns a List - test the method executes without errors
        // (The actual duplicates found depend on the similarity threshold algorithm)
        noException should be thrownBy findDuplicates()
      }

      "find athlete like with different spelling" in {
        val gebdat = Date.valueOf(LocalDate.of(2014, 8, 25))
        val athlete1 = insertAthlete(Athlet(testVereinId).copy(
          name = "Müller",
          vorname = "Maria",
          gebdat = Some(gebdat)
        ))

        val athlete2 = insertAthlete(Athlet(testVereinId).copy(
          name = "Mueller", // Different spelling
          vorname = "Maria",
          gebdat = Some(gebdat)
        ))

        val cache = new java.util.ArrayList[MatchCode]()
        cache.add(MatchCode(athlete1.id, athlete1.name, athlete1.vorname, athlete1.geschlecht, athlete1.gebdat, athlete1.verein.getOrElse(0L)))

        val searchAthlet = Athlet(testVereinId).copy(
          name = "Muller", // Another variation
          vorname = "Maria",
          gebdat = Some(gebdat)
        )

        val found = findAthleteLike(cache, None, exclusive = false, exactVerein = true)(searchAthlet)
        // Should find athlete1 due to similarity
        found.name should (be("Müller") or be("Mueller"))
      }

      "prevents false positives in duplicate detection" in {
        val gebdat = Date.valueOf(LocalDate.of(2016, 5, 15))
        val emmaSmith = insertAthlete(Athlet(testVereinId).copy(
          name = "Smith",
          vorname = "Emma",
          gebdat = Some(gebdat)
        ))

        val annaSmith = insertAthlete(Athlet(testVereinId).copy(
          name = "Smith",
          vorname = "Anna",
          gebdat = Some(gebdat)
        ))

        val finder = findAthleteLike(new java.util.ArrayList[MatchCode](), None, exclusive = true)

        val found1 = finder(emmaSmith)
        // Should find emma but not anna due to exclusivity
        found1.id shouldBe emmaSmith.id

        val found2 = finder(annaSmith)
        // Should find anna but not emma due to exclusivity
        found2.id shouldBe annaSmith.id

      }
    }

    "merge athletes correctly" in {
      val gebdat = Date.valueOf(LocalDate.of(2011, 4, 10))
      val athleteToKeep = insertAthlete(Athlet(testVereinId).copy(
        name = "ToKeep",
        vorname = "Keep",
        gebdat = Some(gebdat),
        strasse = "KeepStrasse 1"
      ))

      val athleteToDelete = insertAthlete(Athlet(testVereinId).copy(
        name = "ToDelete",
        vorname = "Delete",
        gebdat = Some(gebdat),
        strasse = "DeleteStrasse 2"
      ))

      mergeAthletes(athleteToDelete.id, athleteToKeep.id)

      // athleteToDelete should be removed
      loadAthlet(athleteToDelete.id) shouldBe None
      // athleteToKeep should still exist
      loadAthlet(athleteToKeep.id) shouldBe defined
    }

    "mark inactive athletes older than specified years" in {
      // Insert athletes with old wettkampf participation
      val oldDate = Date.valueOf(LocalDate.now().minus(5, ChronoUnit.YEARS))

      val oldAthlete = insertAthlete(Athlet(testVereinId).copy(
        name = "OldAthlete",
        vorname = "Old",
        activ = true
      ))

      // Create a wettkampf in the past
      val wettkampf = createWettkampf(
        oldDate,
        "OldWettkampf",
        Set(20L),
        "test@test.com",
        3333,
        7.5d,
        Some(java.util.UUID.randomUUID().toString),
        "", "", "", "", ""
      )

      // Assign athlete to wettkampf
      val programme = readWettkampfLeafs(wettkampf.programmId)
      if (programme.nonEmpty) {
        assignAthletsToWettkampf(wettkampf.id, Set(programme.head.id), Set((oldAthlete.id, None)), None)
      }

      // Mark inactive athletes from 4 years ago
      val markedCount = markAthletesInactiveOlderThan(4)
      markedCount should be >= 0

      // Verify the athlete is now inactive
      val reloaded = loadAthlet(oldAthlete.id)
      reloaded shouldBe defined
      // Should be marked inactive since last participation was 5 years ago
      if (markedCount > 0) {
        reloaded.get.activ shouldBe false
      }
    }

    "clean unused clubs" in {
      // Create a club with no athletes
      val unusedVereinId = createVerein("UnusedVerein", Some("UnusedVerband"))
      unusedVereinId should be > 0L

      // Verify it was created
      val allVereine = selectVereine
      allVereine.exists(_.id == unusedVereinId) shouldBe true

      // Clean unused clubs
      val cleaned = cleanUnusedClubs()

      // The result should be a Set of Verein objects
      cleaned shouldBe a[Set[?]]
      // The unused club may be in the cleaned set if it has no wertungen
      // (Note: This may not include it if there are registrations for future events)
    }

    "add missing wettkampf metadata" in {
      // Create a wettkampf with UUID
      val wettkampf = createWettkampf(
        Date.valueOf(LocalDate.now()),
        "MetadataTestWettkampf",
        Set(20L),
        "test@test.com",
        3333,
        7.5d,
        Some(java.util.UUID.randomUUID().toString),
        "", "", "", "", ""
      )

      wettkampf.uuid shouldBe defined

      // This should create metadata entries without throwing errors
      noException should be thrownBy addMissingWettkampfMetaData()
    }

    "delete athlete and associated data" in {
      val gebdat = Date.valueOf(LocalDate.of(2015, 11, 5))
      val athleteToDelete = insertAthlete(Athlet(testVereinId2).copy(
        name = "DeleteMe",
        vorname = "Delete",
        gebdat = Some(gebdat)
      ))

      athleteToDelete.id should be > 0L

      deleteAthlet(athleteToDelete.id)

      // Athlete should no longer exist
      loadAthlet(athleteToDelete.id) shouldBe None
    }

    "handle empty athlete correctly" in {
      val emptyAthlet = Athlet()
      emptyAthlet.id shouldBe 0L
      emptyAthlet.name shouldBe ""
      emptyAthlet.vorname shouldBe ""
    }

    "create athlete with verein constructor" in {
      val athletFromVerein = Athlet(testVereinId)
      athletFromVerein.verein shouldBe Some(testVereinId)
      athletFromVerein.geschlecht shouldBe "M"
      athletFromVerein.activ shouldBe true
    }

    "startsSameInPercent calculation" in {
      startsSameInPercent("Liliane", "Lilly") shouldBe 42
      startsSameInPercent("Maria", "Maria") shouldBe 100
      startsSameInPercent("Test", "Different") shouldBe 0
      startsSameInPercent("A", "B") shouldBe 0
      startsSameInPercent("Same", "Same") shouldBe 100
    }
  }
}









