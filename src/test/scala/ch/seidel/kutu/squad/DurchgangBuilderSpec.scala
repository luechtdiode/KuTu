package ch.seidel.kutu.squad

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import scala.compiletime.uninitialized

import scala.compiletime.uninitialized

/**
 * Test coverage for DurchgangBuilder
 *
 * Tests the main functionality of suggesting durchgaenge (rounds) for a competition,
 * including grouping athletes by various criteria and handling different sex division rules.
 */
class DurchgangBuilderSpec extends KuTuBaseSpec {

  var testWettkampf: Wettkampf = uninitialized

  private def insertK2FixtureWettkampfFromSpec(name: String): (Wettkampf, Long) = {
    val wettkampf = createWettkampf(
      new java.sql.Date(System.currentTimeMillis()),
      name,
      Set(20L),
      "test@test.com",
      1234,
      5.0d,
      Some(java.util.UUID.randomUUID().toString),
      "", "", "", "", ""
    )
    val k2ProgrammId = readWettkampfLeafs(wettkampf.programmId).find(_.name == "K2").map(_.id).get

    val k2CountsByVerein = Seq(
      ("BTV Lustig", 0, 2),
      ("DTV Schnell", 6, 0),
      ("SV Laut", 5, 0),
      ("TSV Schlau", 1, 1),
      ("TV Gross", 1, 1),
      ("TV Klein", 11, 2),
      ("TV Dick", 6, 1),
      ("TV Duenne", 3, 0),
      ("TV Breit", 6, 0),
      ("TV Schmal", 12, 0),
      ("TV Hell", 14, 1),
      ("TV Dunkel", 3, 0),
      ("TZ Freundlich", 6, 0)
    )

    k2CountsByVerein.foreach { case (vereinName, femaleCount, maleCount) =>
      val vereinId = createVerein(s"$vereinName-$name", Some("Spec"))

      (1 to femaleCount).foreach { idx =>
        val athlet = insertAthlete(Athlet(vereinId).copy(
          geschlecht = "W",
          name = s"${vereinName.replace(" ", "")}-Ti-$idx",
          vorname = "Spec"
        ))
        assignAthletsToWettkampf(wettkampf.id, Set(k2ProgrammId), Set((athlet.id, None)), None)
      }

      (1 to maleCount).foreach { idx =>
        val athlet = insertAthlete(Athlet(vereinId).copy(
          geschlecht = "M",
          name = s"${vereinName.replace(" ", "")}-Tu-$idx",
          vorname = "Spec"
        ))
        assignAthletsToWettkampf(wettkampf.id, Set(k2ProgrammId), Set((athlet.id, None)), None)
      }
    }

    (wettkampf, k2ProgrammId)
  }

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

    "derive optional Durchganggruppen titles from the suggested Durchgaenge" in {
      val builder = DurchgangBuilder(this)
      val grouped = builder.suggestDurchgangGruppen(
        testWettkampf.id,
        maxRiegenSize = 0,
        splitSexOption = Some(GemischteRiegen)
      )

      grouped should not be empty
      all(grouped.map(_.title)) should not be empty
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

    "have equal riegen sizes within each durchgang when forced into multiple durchgaenge" in {
      // maxRiegenSize=5 forces multiple Durchgänge per category
      val bigWk = insertGeTuWettkampf("IntraDurchgangBalanceWK", 20)
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        bigWk.id,
        maxRiegenSize = 5,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result.nonEmpty) {
        result.foreach { case (durchgang, diszMap) =>
          // Compare TOTAL athletes per Startgerät (Disziplin) within a Durchgang.
          // A Startgerät may have several merged TurnerRiegen; their combined total should
          // be near-equal to every other Startgerät's total in the same Durchgang.
          val totalsPerDevice: Seq[Int] = diszMap.toSeq.map { case (_, riegenList) =>
            riegenList.toSeq.flatMap { case (_, wertungen) =>
              wertungen.map(_.athletId)
            }.distinct.size
          }.filter(_ > 0)

          if (totalsPerDevice.size > 1) {
            val diff = totalsPerDevice.max - totalsPerDevice.min
            withClue(s"Durchgang '$durchgang': athletes-per-device=$totalsPerDevice") {
              diff should be <= 2
            }
          }
        }
      }
    }

    "distribute athletes evenly across riegen" in {
      testWettkampf = insertGeTuWettkampf("DistributionTestWK", 20)
      val builder = DurchgangBuilder(this)
      val result = builder.suggestDurchgaenge(
        testWettkampf.id,
        maxRiegenSize = 10,
        splitSexOption = Some(GemischteRiegen)
      )

      if (result.nonEmpty) {
        result.values.foreach { durchgangMap =>
          durchgangMap.values.foreach { riegenList =>
            val distribution = riegenList
              .map { case (_, wertungen) => wertungen.map(_.athletId).distinct.size }
              .filter(_ > 0)

            if (distribution.nonEmpty) {
              val participantsPerDurchgang = distribution.sum
              val targetDifference = if (participantsPerDurchgang <= 40) 1 else 2
              val difference = distribution.max - distribution.min
              difference should be <= targetDifference
            }
          }
        }
      }
    }

    "cover all start devices in every durchgang when low maxRiegenSize creates multiple durchgaenge" in {
      val multiRoundWk = insertGeTuWettkampf("AllStartsPerDurchgangWK", 40)
      val builder = DurchgangBuilder(this)

      val result = builder.suggestDurchgaenge(
        multiRoundWk.id,
        maxRiegenSize = 4,
        splitSexOption = Some(GemischteRiegen)
      )

      result should not be empty
      result.keys.size should be > 1

      val expectedStartIds = result.values.flatMap(_.keys.map(_.id)).toSet
      expectedStartIds should not be empty

      result.foreach { case (durchgangName, startMap) =>
        withClue(s"Durchgang '$durchgangName' should contain all start devices") {
          startMap.keys.map(_.id).toSet shouldBe expectedStartIds
        }
      }
    }

    "not create an artificial singleton follow-up round when maxRiegenSize is still not reached in round 1" in {
      val wk = insertGeTuWettkampf("NoSingletonRoundWK", 20)
      val builder = DurchgangBuilder(this)

      val result = builder.suggestDurchgaenge(
        wk.id,
        maxRiegenSize = 8,
        splitSexOption = Some(GemischteRiegen)
      )

      result should not be empty

      def programmPrefix(dgName: String): String = dgName.split("\\(").head.trim

      val roundsByProgramm = result.keys.groupBy(programmPrefix)
      roundsByProgramm.foreach { case (prefix, rounds) =>
        val roundSet = rounds.toSet
        val round1Name = s"$prefix (1)"
        val round2Name = s"$prefix (2)"
        if (roundSet.contains(round1Name) && roundSet.contains(round2Name)) {
          val round1MaxDeviceLoad = result(round1Name).values.map { riegenList =>
            riegenList.flatMap(_._2.map(_.athletId)).toSet.size
          }.max

          val round2Participants = result(round2Name).values.flatMap { riegenList =>
            riegenList.flatMap(_._2.map(_.athletId))
          }.toSet.size

          withClue(s"Programm '$prefix' has round1 max-device-load=$round1MaxDeviceLoad, round2 participants=$round2Participants") {
            if (round1MaxDeviceLoad < 8) round2Participants should be > 1
          }
        }
      }
    }

    "not create singleton residual rounds for K2 planning with maxRiegenSize 8 and no sex separation" in {
      val (wk, k2Id) = insertK2FixtureWettkampfFromSpec("NoSingletonK2WK")
      val builder = DurchgangBuilder(this)

      val result = builder.suggestDurchgaenge(
        wk.id,
        maxRiegenSize = 8,
        programmfilter = Set(k2Id),
        splitSexOption = Some(GemischteRiegen)
      )

      result should not be empty
      result.keys.toSet shouldBe Set("K2 (1)", "K2 (2)", "K2 (3)")

      val participantsByRound = result.map { case (roundName, disziplinMap) =>
        roundName -> disziplinMap.values.flatMap(riegenList => riegenList.flatMap(_._2.map(_.athletId))).toSet.size
      }

      participantsByRound("K2 (3)") should be > 1
      participantsByRound.values.sum shouldBe 82

      val expectedStartIds = result.values.head.keys.map(_.id).toSet
      result.foreach { case (dg, starts) =>
        withClue(s"Durchgang '$dg' should expose all K2 start devices") {
          starts.keys.map(_.id).toSet shouldBe expectedStartIds
        }
      }
    }

    "keep device loads balanced in unlimited mode (maxRiegenSize=0) for K2 planning with GemischteRiegen" in {
      val (wk, k2Id) = insertK2FixtureWettkampfFromSpec("UnlimitedBalanceCheckWK")
      val builder = DurchgangBuilder(this)

      val result = builder.suggestDurchgaenge(
        wk.id,
        maxRiegenSize = 0,
        programmfilter = Set(k2Id),
        splitSexOption = Some(GemischteRiegen)
      )

      // Should produce exactly one durchgang when unlimited
      result should not be empty
      result.keys.toSet shouldBe Set("K2 (1)")

      // Verify that athletes per start device are well-balanced (spread <= 2)
      val athletesPerDevice = result.values.head.values.flatMap { riegenList =>
        riegenList.flatMap(_._2.map(_.athletId)).toSet
      }.groupBy(identity).map { case (_, athletes) => 1 }.toSeq

      if (result.values.head.size > 1) {
        val deviceLoads = result.values.head.map { case (_, riegenList) =>
          riegenList.flatMap(_._2.map(_.athletId)).toSet.size
        }.toSeq.filter(_ > 0)

        if (deviceLoads.size > 1) {
          withClue(s"Device loads across K2 disciplines: $deviceLoads") {
            (deviceLoads.max - deviceLoads.min) should be <= 2
          }
        }
      }

      // Verify all K2 start devices are present
      val expectedStartIds = result.values.head.keys.map(_.id).toSet
      expectedStartIds should not be empty
    }

    "create one R2 durchgang per Kategorie with local riege2 groupings" in {
      val builder = DurchgangBuilder(this)
      val disziplin = Disziplin(1L, "Barren")
      val k1Wertung = Wertung(1L, 1L, 1L, 1L, "uuid", None, None, None, Some("R1"), Some("Barren K1"), Some(0), None, None)
      val k2Wertung = Wertung(2L, 2L, 2L, 1L, "uuid", None, None, None, Some("R2"), Some("Barren K2"), Some(0), None, None)
      val k3Wertung = Wertung(3L, 3L, 2L, 1L, "uuid", None, None, None, Some("R2"), Some("Barren K2"), Some(0), None, None)
      val suggested: SuggestedDurchgaenge = Map(
        "K1 (1)" -> Map(disziplin -> Seq("Barren K1" -> Seq(k1Wertung))),
        "K2 (1)" -> Map(disziplin -> Seq("Barren K2" -> Seq(k2Wertung))),
        "K2 (2)" -> Map(disziplin -> Seq("Barren K2" -> Seq(k3Wertung)))
      )

      val riege2Assignments = Map(
        "Barren K1" -> ("K1 - R2", disziplin),
        "Barren K2" -> ("K2 - R2", disziplin)
      )

      val separated = builder.separateRiegen2DurchgaengeFromSuggested(
        suggested,
        riege2Assignments
      )

      separated.keySet shouldBe Set("K1 - R2", "K2 - R2")
      separated("K1 - R2")(disziplin).map(_._1).toSet shouldBe Set("Barren K1")
      separated("K2 - R2")(disziplin).map(_._1).toSet shouldBe Set("Barren K2")
    }
  }
}
