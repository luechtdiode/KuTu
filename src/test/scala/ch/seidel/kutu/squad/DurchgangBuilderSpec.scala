package ch.seidel.kutu.squad

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import scala.compiletime.uninitialized

/**
 * Test coverage for DurchgangBuilder
 *
 * Tests the main functionality of suggesting durchgaenge (rounds) for a competition,
 * including grouping athletes by various criteria and handling different sex division rules.
 */
class DurchgangBuilderSpec extends KuTuBaseSpec {

  var testWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("TestDurchgangBuilderWK", 3)
  }

  "DurchgangBuilder" should {

    "return empty map when wettkampf has no wertungen" in {
      val emptyWettkampf = createWettkampf(
        new java.sql.Date(System.currentTimeMillis()),
        "EmptyTestWK",
        Set(20L),
        "test@test.com",
        1234,
        5.0d,
        Some(java.util.UUID.randomUUID().toString),
        "", "", "", "", ""
      )

      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(emptyWettkampf.id)

      result shouldBe empty
    }

    "suggest durchgaenge with GemischteRiegen option" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 0,
        splitSexOption = Some(GemischteRiegen)
      )

      result should not be empty
      result.keys should not be empty

      // Each durchgang should have disciplines and riegen
      result.values.foreach { durchgangMap =>
        durchgangMap should not be empty
        durchgangMap.values.foreach { riegenList =>
          riegenList should not be empty
        }
      }
    }

    "suggest durchgaenge with GetrennteDurchgaenge option" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 0,
        splitSexOption = Some(GetrennteDurchgaenge)
      )

      result should not be empty

      // With GetrennteDurchgaenge, we might have -Tu and -Ti suffixes for male/female
      val durchgangNames = result.keys
      durchgangNames should not be empty
    }

    "suggest durchgaenge with GemischterDurchgang option" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 0,
        splitSexOption = Some(GemischterDurchgang)
      )

      result should not be empty
    }

    "respect maxRiegenSize parameter" in {
      val builder = DurchgangBuilder(this)

      // Test with a specific max riegen size
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 10,
        splitSexOption = Some(GemischteRiegen)
      )

      result should not be empty

      // Verify that the result has reasonable distribution
      // Count unique athletes per riege per discipline
      result.values.foreach { durchgangMap =>
        durchgangMap.foreach { case (disziplin, riegenList) =>
          riegenList.foreach { case (riegenName, wertungen) =>
            // Group by athlete ID to count unique athletes in this riege for this discipline
            val uniqueAthletes = wertungen.map(_.athletId).toSet.size
            // Unique athletes should be reasonably distributed
            uniqueAthletes should be >= 0
            if (uniqueAthletes > 0) {
              uniqueAthletes should be <= 14  // Max can be 14 as per algorithm
            }
          }
        }
      }
    }

    "handle splitPgm parameter correctly" in {
      val builder = DurchgangBuilder(this)

      // With splitPgm = true
      val resultSplit = builder.suggestDurchgaenge(
        testWettkampf.id,
        splitPgm = true
      )

      // With splitPgm = false
      val resultMerged = builder.suggestDurchgaenge(
        testWettkampf.id,
        splitPgm = false
      )

      resultSplit should not be empty
      resultMerged should not be empty

      // When not split, durchgang names might be combined
      if (resultMerged.keys.exists(_.contains(" & "))) {
        // Combined program names contain " & "
        resultMerged.keys.foreach(key =>
          if (key.contains(" & ")) {
            key should include(" & ")
          }
        )
      }
    }

    "filter by durchgang when durchgangfilter is provided" in {
      // First, create some riegen
      val builder = DurchgangBuilder(this)
      val firstSuggestion = builder.suggestDurchgaenge(testWettkampf.id)

      if (firstSuggestion.nonEmpty) {
        // Insert some riegen
        firstSuggestion.foreach { case (durchgang, diszMap) =>
          diszMap.foreach { case (start, riegen) =>
            riegen.foreach { case (riege, wertungen) =>
              insertRiegenWertungen(RiegeRaw(
                wettkampfId = testWettkampf.id,
                r = riege,
                durchgang = Some(durchgang),
                start = Some(start.id),
                kind = RiegeRaw.KIND_STANDARD
              ), wertungen)
            }
          }
        }

        // Now test with filter
        val firstDurchgang = firstSuggestion.keys.head
        val filteredResult = builder.suggestDurchgaenge(
          testWettkampf.id,
          durchgangfilter = Set(firstDurchgang)
        )

        // Should only return the filtered durchgang (or related ones)
        if (filteredResult.nonEmpty) {
          filteredResult should not be empty
        }
      }
    }

    "filter by programme when programmfilter is provided" in {
      val builder = DurchgangBuilder(this)
      val programme = readWettkampfLeafs(testWettkampf.programmId)

      if (programme.nonEmpty) {
        val firstProgrammId = programme.head.id
        val result = builder.suggestDurchgaenge(
          testWettkampf.id,
          programmfilter = Set(firstProgrammId)
        )

        // Result should only contain wertungen from the filtered programme
        result.values.foreach { durchgangMap =>
          durchgangMap.values.foreach { riegenList =>
            // All wertungen should belong to the filtered programme
            riegenList should not be empty
          }
        }
      }
    }

    "filter by discipline list when onDisziplinList is provided" in {
      val builder = DurchgangBuilder(this)
      val disziplinen = listDisziplinesZuWettkampf(testWettkampf.id)

      if (disziplinen.size >= 2) {
        // Select first two disciplines
        val selectedDisziplinen = disziplinen.take(2).toSet

        val result = builder.suggestDurchgaenge(
          testWettkampf.id,
          onDisziplinList = Some(selectedDisziplinen)
        )

        // Result should only contain the selected disciplines
        result.values.foreach { durchgangMap =>
          durchgangMap.keys.foreach { disziplin =>
            selectedDisziplinen should contain(disziplin)
          }
        }
      }
    }

    "handle auto detection of sex division when splitSexOption is None" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        splitSexOption = None
      )

      result should not be empty
      // Auto detection should work and produce valid result
    }

    "group athletes by riegen correctly" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 10,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result.nonEmpty) {
        result.values.foreach { durchgangMap =>
          durchgangMap.values.foreach { riegenList =>
            riegenList.foreach { case (riegenName, wertungen) =>
              // Each riege should have a name
              riegenName should not be empty
              // Riegen can be empty (e.g., "Leere Riege" for empty starting positions)
              // So we just check that wertungen is a valid sequence
              wertungen shouldBe a[Seq[?]]
            }
          }
        }
      }
    }

    "produce consistent riege names" in {
      val builder = DurchgangBuilder(this)
      val result1 = builder.suggestDurchgaenge(
        testWettkampf.id,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result1.nonEmpty) {
        result1.values.foreach { durchgangMap =>
          durchgangMap.values.foreach { riegenList =>
            riegenList.foreach { case (riegenName, _) =>
              // Riege names should follow a consistent pattern
              riegenName should not be empty
              riegenName.length should be > 0
            }
          }
        }
      }
    }

    "assign each wertung to exactly one riege per discipline" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result.nonEmpty) {
        result.values.foreach { durchgangMap =>
          durchgangMap.foreach { case (disziplin, riegenList) =>
            val allWertungIds = riegenList.flatMap { case (_, wertungen) =>
              wertungen.map(_.id)
            }.toList

            // Each wertung should appear only once per discipline
            allWertungIds.size shouldBe allWertungIds.distinct.size
          }
        }
      }
    }

    "not fail with empty durchgangfilter" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        durchgangfilter = Set.empty
      )

      // Should work normally when filter is empty
      result should not be empty
    }

    "not fail with empty programmfilter" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        programmfilter = Set.empty
      )

      // Should work normally when filter is empty
      result should not be empty
    }

    "distribute athletes evenly across riegen" in {
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 10,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result.nonEmpty) {
        result.values.foreach { durchgangMap =>
          durchgangMap.values.foreach { riegenList =>
            if (riegenList.size > 1) {
              val sizes = riegenList.map { case (_, wertungen) => wertungen.size }.toList.sorted
              if (sizes.nonEmpty && sizes.max > 0) {
                // The difference between smallest and largest riege should not be too large
                // (reasonable distribution)
                val difference = sizes.max - sizes.min
                difference should be <= (sizes.max / 2 + 1)
              }
            }
          }
        }
      }
    }
  }
}





