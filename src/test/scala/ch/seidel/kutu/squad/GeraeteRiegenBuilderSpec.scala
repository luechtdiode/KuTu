package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.domain.given_Conversion_LocalDate_Date
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import scala.collection.mutable

class GeraeteRiegenBuilderSpec extends AnyWordSpec with Matchers {

  object Harness extends GeraeteRiegenBuilder {
    def split(
        turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
        maxPerGeraet: Int): Seq[(String, Seq[WertungViewsZuAthletView])] = {
      given mutable.Map[String, Int] = mutable.Map.empty[String, Int]
      splitTurnerRiegenToMaxSize(turnerRiegen, maxPerGeraet)
    }

    def distribute(programm: String, startgeraete: List[Disziplin], alignedriegen: Seq[RiegeAthletWertungen]): Seq[(String, String, Disziplin, Seq[WertungViewsZuAthletView])] =
      distributeToStartgeraete(programm, startgeraete, alignedriegen)

    def merge(startriegen: GeraeteRiegen, maxRiegenSize: Int, splitSex: SexDivideRule, targetDiff: Int): GeraeteRiegen =
      bringVereineTogether(startriegen, maxRiegenSize, splitSex, targetDiff)

    def build(
        programm: String,
        startgeraete: List[Disziplin],
        turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
        maxRiegenSize: Int,
        splitSex: SexDivideRule,
        jahrgangGroup: Boolean): Seq[(String, String, Disziplin, Seq[WertungViewsZuAthletView])] = {
      given mutable.Map[String, Int] = mutable.Map.empty[String, Int]
      buildGeraeteRiegen(programm, startgeraete, turnerRiegen, maxRiegenSize, splitSex, jahrgangGroup)
    }
  }

  private val vereinA = Verein(1, "TV A", Some("ZH"))
  private val vereinB = Verein(2, "TV B", Some("ZH"))
  private val disziplin1 = Disziplin(1L, "Boden")
  private val disziplin2 = Disziplin(2L, "Sprung")
  private val notenSpez = StandardWettkampf(1d)
  private val program = ProgrammView(1L, "K1", 0, None, RiegeRaw.RIEGENMODE_BY_Program, 0, 99, "", 1, 0)
  private val wettkampf = WettkampfView(1L, None, LocalDate.now(), "WK", program, 0, BigDecimal(0), "", "", "", "", "", "").toWettkampf

  private def athlete(id: Long, sex: String, verein: Verein): AthletView =
    AthletView(id, 0, sex, s"Name$id", s"Vor$id", Some(LocalDate.of(2010, 1, 1)), "", "", "", Some(verein), activ = true)

  private def wertung(athlet: AthletView, disziplin: Disziplin = disziplin1): WertungView = {
    val wkd = WettkampfdisziplinView(disziplin.id, program, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    WertungView(100L + athlet.id + disziplin.id, athlet, wkd, wettkampf, None, None, None, None, None, 0, None, None)
  }

  private def team(name: String, verein: Verein, ids: Seq[Long]): (String, Seq[WertungViewsZuAthletView]) =
    name -> ids.map(id => {
      val a = athlete(id, "M", verein)
      a -> Seq(wertung(a))
    })

  private def teamWithSex(name: String, sex: String, verein: Verein, ids: Seq[Long]): (String, Seq[WertungViewsZuAthletView]) =
    name -> ids.map(id => {
      val a = athlete(id, sex, verein)
      a -> Seq(wertung(a))
    })

  "GeraeteRiegenBuilder" should {
    "split oversized Turner-Riegen according to the max size" in {
      val result = Harness.split(Seq(team("R1", vereinA, Seq(1L, 2L, 3L, 4L, 5L))), maxPerGeraet = 3)

      result should have size 2
      result.map(_._2.size).sorted shouldBe Seq(2, 3)
      result.map(_._1) should contain allOf ("R1#01", "R1#02")
    }

    "return empty distribution when no aligned riegen are provided" in {
      Harness.distribute("K0", List(disziplin1), Seq.empty) shouldBe empty
    }

    "return empty distribution when no start devices are provided" in {
      val aligned = Seq(Map("R1" -> team("R1", vereinA, Seq(1L, 2L))._2))
      Harness.distribute("K0", Nil, aligned) shouldBe empty
    }

    "return no result when no athletes are available" in {
      val result = Harness.build(
        programm = "K-empty",
        startgeraete = List(disziplin1, disziplin2),
        turnerRiegen = Seq.empty,
        maxRiegenSize = 8,
        splitSex = GemischteRiegen,
        jahrgangGroup = false
      )

      result shouldBe empty
    }

    "add empty Geräteriegen when a Durchgang has fewer starts than devices" in {
      val aligned = Seq(Map("R1" -> team("R1", vereinA, Seq(1L, 2L))._2))

      val distributed = Harness.distribute("K1", List(disziplin1, disziplin2), aligned)

      distributed should have size 2
      distributed.exists(_._2.startsWith("Leere Riege K1/")) shouldBe true
    }

    "fill missing start devices in every generated Durchgang" in {
      val d3 = Disziplin(3L, "Reck")
      val d4 = Disziplin(4L, "Ring")
      val startgeraete = List(disziplin1, disziplin2, d3, d4)
      val aligned = Seq(
        Map("R1" -> team("R1", vereinA, Seq(21L))._2),
        Map("R2" -> team("R2", vereinA, Seq(22L))._2),
        Map("R3" -> team("R3", vereinA, Seq(23L))._2),
        Map("R4" -> team("R4", vereinA, Seq(24L))._2),
        Map("R5" -> team("R5", vereinA, Seq(25L))._2)
      )

      val distributed = Harness.distribute("Kmulti", startgeraete, aligned)
      val byDurchgang = distributed.groupBy(_._1)

      byDurchgang.keySet shouldBe Set("Kmulti (1)", "Kmulti (2)")
      byDurchgang.foreach { case (_, rows) =>
        rows.map(_._3.id).toSet shouldBe startgeraete.map(_.id).toSet
      }
      byDurchgang("Kmulti (2)").count(_._2.startsWith("Leere Riege Kmulti/")) should be >= 1
    }

    "merge clubs when that improves cohesion without exceeding the max size" in {
      val riegeA1 = ch.seidel.kutu.squad.GeraeteRiege(Set(TurnerRiege("A1", Some(vereinA), "M", 2), TurnerRiege("B1", Some(vereinB), "M", 1)))
      val riegeA2 = ch.seidel.kutu.squad.GeraeteRiege(Set(TurnerRiege("A2", Some(vereinA), "M", 2), TurnerRiege("B2", Some(vereinB), "M", 1)))

      val merged = Harness.merge(Set(riegeA1, riegeA2), maxRiegenSize = 4, GemischteRiegen, targetDiff = 2)

      merged.exists(r => r.turnerriegen.flatMap(_.verein).toSet == Set(vereinA)) shouldBe true
      merged.exists(r => r.turnerriegen.flatMap(_.verein).toSet == Set(vereinB)) shouldBe true
    }

    "keep mixed genders in one Durchgang for GemischterDurchgang when max size is unlimited" in {
      val turnerRiegen = Seq(
        teamWithSex("W-Team", "W", vereinA, Seq(1L, 2L, 3L, 4L, 5L, 6L, 7L)),
        teamWithSex("M-Team", "M", vereinB, Seq(8L))
      )

      val result = Harness.build(
        programm = "Kmix",
        startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring")),
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 0,
        splitSex = GemischterDurchgang,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("Kmix (1)")
      val sexesInDurchgang = result.filter(_._1 == "Kmix (1)").flatMap(_._4.map(_._1.geschlecht)).toSet
      sexesInDurchgang shouldBe Set("M", "W")
    }

    "keep single-gender assignments in one Durchgang for GemischterDurchgang when max size is unlimited" in {
      val turnerRiegen = Seq(
        teamWithSex("W-Team", "W", vereinA, Seq(11L, 12L, 13L, 14L, 15L))
      )

      val result = Harness.build(
        programm = "Kwomen",
        startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring")),
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 0,
        splitSex = GemischterDurchgang,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("Kwomen (1)")
      result.flatMap(_._4.map(_._1.geschlecht)).toSet shouldBe Set("W")
    }

    "produce exactly one Durchgang for separated assignments in unlimited mode and keep device loads balanced" in {
      val startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring"))
      val turnerRiegen = Seq(
        teamWithSex("M-1", "M", vereinA, Seq(101L, 102L, 103L)),
        teamWithSex("M-2", "M", vereinA, Seq(104L, 105L, 106L)),
        teamWithSex("M-3", "M", vereinA, Seq(107L, 108L, 109L)),
        teamWithSex("M-4", "M", vereinA, Seq(110L, 111L, 112L)),
        teamWithSex("M-5", "M", vereinA, Seq(113L, 114L, 115L)),
        teamWithSex("M-6", "M", vereinA, Seq(116L, 117L, 118L))
      )

      val result = Harness.build(
        programm = "Ksep-Tu",
        startgeraete = startgeraete,
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 0,
        splitSex = GetrennteDurchgaenge,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("Ksep-Tu (1)")

      val athletesPerDevice = result
        .groupBy(_._3)
        .values
        .map(rows => rows.flatMap(_._4.map(_._1.id)).toSet.size)
        .filter(_ > 0)
        .toSeq

      athletesPerDevice.nonEmpty shouldBe true
      (athletesPerDevice.max - athletesPerDevice.min) should be <= 2
    }

    "avoid creating an extra round with a tiny leftover group when max size still allows merging" in {
      val startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring"))
      val turnerRiegen = Seq(
        teamWithSex("R1", "M", vereinA, Seq(201L, 202L, 203L, 204L)),
        teamWithSex("R2", "M", vereinA, Seq(205L, 206L, 207L, 208L)),
        teamWithSex("R3", "M", vereinA, Seq(209L, 210L, 211L, 212L)),
        teamWithSex("R4", "M", vereinA, Seq(213L, 214L, 215L, 216L)),
        teamWithSex("R5", "M", vereinA, Seq(217L, 218L, 219L, 220L))
      )

      val result = Harness.build(
        programm = "Kmerge",
        startgeraete = startgeraete,
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 8,
        splitSex = GemischteRiegen,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("Kmerge (1)")
    }

    "fully use round capacity before creating another round for many 6er groups plus tiny remainder" in {
      val startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring"))
      val sixer = (1 to 12).map { i =>
        val base = 300L + i * 10
        teamWithSex(s"S$i", "W", vereinA, Seq(base + 1, base + 2, base + 3, base + 4, base + 5, base + 6))
      }
      val tiny = teamWithSex("Tiny", "M", vereinB, Seq(999L))

      val result = Harness.build(
        programm = "K2",
        startgeraete = startgeraete,
        turnerRiegen = sixer.toSeq :+ tiny,
        maxRiegenSize = 8,
        splitSex = GemischteRiegen,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("K2 (1)", "K2 (2)", "K2 (3)")
      val lastRoundParticipants = result.filter(_._1 == "K2 (3)").flatMap(_._4.map(_._1.id)).toSet.size
      lastRoundParticipants should be > 1
    }

    "plan K2 table dataset with max 8 using exactly three rounds without singleton leftovers" in {
      val startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring"))
      val k2CountsByVerein = Seq(
        ("BTV Lustig", 0, 2),
        ("DTV Schnell", 6, 0),
        ("SV Laut", 5, 0),
        ("TSV Schlau", 1, 1),
        ("TV Gross", 1, 1),
        ("TV Klein", 11, 2),
        ("TV Dick", 6, 1),
        ("TV Dünn", 3, 0),
        ("TV Breit", 6, 0),
        ("TV Schmal", 12, 0),
        ("TV Hell", 14, 1),
        ("TV Dunkel", 3, 0),
        ("TZ Freundlich", 6, 0)
      )

      var nextVereinId = 1000L
      var nextAthletId = 50000L
      def newAthletIds(count: Int): Seq[Long] = {
        val ids = (0 until count).map(_ => {
          nextAthletId += 1
          nextAthletId
        })
        ids
      }

      val turnerRiegen = k2CountsByVerein.flatMap { case (name, femaleCnt, maleCnt) =>
        nextVereinId += 1
        val verein = Verein(nextVereinId, name, Some("ZH"))
        Seq(
          if femaleCnt > 0 then Some(teamWithSex(s"$name-Ti", "W", verein, newAthletIds(femaleCnt))) else None,
          if maleCnt > 0 then Some(teamWithSex(s"$name-Tu", "M", verein, newAthletIds(maleCnt))) else None
        ).flatten
      }

      val result = Harness.build(
        programm = "K2",
        startgeraete = startgeraete,
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 8,
        splitSex = GemischteRiegen,
        jahrgangGroup = false
      )

      // 82 participants, 4 start devices, max 8 => 3 rounds are sufficient (capacity 3*4*8=96).
      result.map(_._1).toSet shouldBe Set("K2 (1)", "K2 (2)", "K2 (3)")

      val participantsByRound = result.groupBy(_._1).view.mapValues { rows =>
        rows.flatMap(_._4.map(_._1.id)).toSet.size
      }.toMap

      participantsByRound("K2 (3)") should be > 1
      participantsByRound.values.sum shouldBe 82
    }

    "keep start-device groups balanced in unlimited mode for the K2 table dataset" in {
      val startgeraete = List(disziplin1, disziplin2, Disziplin(3L, "Reck"), Disziplin(4L, "Ring"))
      val allDisziplines = startgeraete
      def teamWithSexAllStarts(name: String, sex: String, verein: Verein, ids: Seq[Long]): (String, Seq[WertungViewsZuAthletView]) =
        name -> ids.map(id => {
          val a = athlete(id, sex, verein)
          a -> allDisziplines.map(d => wertung(a, d))
        })

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

      var nextVereinId = 2000L
      var nextAthletId = 80000L
      def newAthletIds(count: Int): Seq[Long] = {
        val ids = (0 until count).map(_ => {
          nextAthletId += 1
          nextAthletId
        })
        ids
      }

      val turnerRiegen = k2CountsByVerein.flatMap { case (vereinName, femaleCnt, maleCnt) =>
        nextVereinId += 1
        val verein = Verein(nextVereinId, vereinName, Some("Spec"))
        Seq(
          if femaleCnt > 0 then Some(teamWithSexAllStarts(s"$vereinName-Ti", "W", verein, newAthletIds(femaleCnt))) else None,
          if maleCnt > 0 then Some(teamWithSexAllStarts(s"$vereinName-Tu", "M", verein, newAthletIds(maleCnt))) else None
        ).flatten
      }

      val result = Harness.build(
        programm = "K2Unlimited",
        startgeraete = startgeraete,
        turnerRiegen = turnerRiegen,
        maxRiegenSize = 0,
        splitSex = GemischteRiegen,
        jahrgangGroup = false
      )

      result.map(_._1).toSet shouldBe Set("K2Unlimited (1)")
      val athletesPerDevice = result
        .groupBy(_._3.id)
        .values
        .map(rows => rows.flatMap(_._4.map(_._1.id)).toSet.size)
        .filter(_ > 0)
        .toSeq

      athletesPerDevice.nonEmpty shouldBe true
      (athletesPerDevice.max - athletesPerDevice.min) should be <= 2
    }
  }
}



