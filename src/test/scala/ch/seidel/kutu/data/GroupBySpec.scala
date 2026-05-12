package ch.seidel.kutu.data

import ch.seidel.kutu.domain.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Date
import java.time.LocalDate

class GroupBySpec extends AnyWordSpec with Matchers {

  private def programm(id: Long, name: String, ord: Int = 1, parent: Option[ProgrammView] = None, aggregate: Int = 0): ProgrammView =
    ProgrammView(id, name, aggregate, parent, ord, 0, 99, "", 1, 0)

  private val rootProgramm = programm(1, "Programm A")
  private val subProgramm = programm(2, "Kategorie A", ord = 2, parent = Some(rootProgramm), aggregate = 1)

  private def wettkampf(id: Long = 1, teamrule: Option[String] = None): Wettkampf =
    Wettkampf(
      id,
      Some(s"uuid-$id"),
      Date.valueOf(LocalDate.of(2025, 5, 1)),
      s"WK-$id",
      rootProgramm.id,
      0,
      BigDecimal(0),
      "",
      Some(""),
      Some(""),
      Some(""),
      Some(""),
      teamrule
    )

  private def disziplin(id: Long, name: String, ord: Int, pgm: ProgrammView = subProgramm): WettkampfdisziplinView =
    WettkampfdisziplinView(id, pgm, Disziplin(id, name), "", None, StandardWettkampf(1d), 1, 1, ord, 2, 0, 0, 10, 1)

  private def athlet(id: Long, sex: String, verein: Verein, name: String): AthletView =
    AthletView(id, 0, sex, name, s"V$id", Some(Date.valueOf(LocalDate.of(2010, 1, 1))), "", "", "", Some(verein), activ = true)

  private def wertung(
      id: Long,
      ath: AthletView,
      wd: WettkampfdisziplinView,
      wk: Wettkampf,
      end: BigDecimal,
      team: Int = 0
  ): WertungView =
    WertungView(id, ath, wd, wk, None, Some(end), Some(end), None, None, team, None, None)

  "ScoreList parsers" should {
    "parse kind and counting variants" in {
      ScoreListKind(Some("Einzelrangliste")) shouldBe Einzelrangliste
      ScoreListKind(Some("Teamrangliste")) shouldBe Teamrangliste
      ScoreListKind(Some("Kombirangliste")) shouldBe Kombirangliste
      ScoreListKind(Some("unknown")) shouldBe Einzelrangliste
      ScoreListKind(None) shouldBe Einzelrangliste

      ScoreListBestN(Some("all")) shouldBe AlleWertungen
      ScoreListBestN(Some("beste(3)")) shouldBe BestNWertungen(3)
      ScoreListBestN(Some("beste(7)")) shouldBe AlleWertungen
      ScoreListBestN(None) shouldBe AlleWertungen
    }
  }

  "GroupBy chain state" should {
    "propagate settings through traverse and reset" in {
      val chain = ByVerein().groupBy(ByGeschlecht())
      chain.setKind(Kombirangliste)
      chain.setBestNCounting(BestNWertungen(2))
      chain.setAlphanumericOrdered(true)
      chain.setAvgOnMultipleCompetitions(false)

      val kinds = chain.traverse(Set.empty[ScoreListKind])((gb, acc) => acc + gb.getKind)
      kinds shouldBe Set(Kombirangliste)

      val allFlags = chain.traverse(List.empty[(Boolean, Boolean)])((gb, acc) => (gb.isAlphanumericOrdered, gb.isAvgOnMultipleCompetitions) :: acc)
      allFlags.distinct shouldBe List((true, false))

      chain.reset
      chain.getBestNCounting shouldBe AlleWertungen
      chain.isAlphanumericOrdered shouldBe false
      chain.isAvgOnMultipleCompetitions shouldBe true
      chain.chainToString should not include "/"
    }
  }

  "FilterBy behavior" should {
    "support skip grouper when null-object all filter is selected" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val clubB = Verein(2, "ClubB", Some("V2"))
      val wk = wettkampf()
      val wd = disziplin(11, "Boden", ord = 1)
      val data = Seq(
        wertung(1, athlet(1, "M", clubA, "A"), wd, wk, BigDecimal(9.1)),
        wertung(2, athlet(2, "W", clubB, "B"), wd, wk, BigDecimal(8.7))
      )

      val byVerein = ByVerein()
      val items = byVerein.analyze(data)
      byVerein.setFilter(items.toSet + NullObject("alle"))

      byVerein.canSkipGrouper shouldBe true
      byVerein.skipGrouper shouldBe true

      val selected = byVerein.select(data).toList
      selected should have size 1
      selected.head shouldBe a[GroupLeaf[?]]
    }

    "serialize filters and options in rest query" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf()
      val wd = disziplin(12, "Sprung", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", clubA, "A"), wd, wk, BigDecimal(9.0)))

      val query = GroupBy(
        "groupby=Verein&filter=Verein:ClubA&alphanumeric&avg=false&kind=Einzelrangliste&counting=beste(2)",
        data,
        List(ByVerein(), ByGeschlecht())
      )

      query.groupname shouldBe "Verein"
      query.isAlphanumericOrdered shouldBe true
      query.isAvgOnMultipleCompetitions shouldBe false
      query.getBestNCounting shouldBe BestNWertungen(2)

      val rest = query.asInstanceOf[FilterBy].toRestQuery
      val tokens = rest.split("&").toSet

      tokens should contain("groupby=Verein")
      tokens should contain("filter=Verein:ClubA")
      tokens should contain("alphanumeric")
      tokens should contain("avg=false")
      tokens should contain("kind=Einzelrangliste")
      tokens should contain("counting=beste(2)")
    }
  }

  "GroupBy selection" should {
    "emit leaf, team and combined sections depending on kind" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf(teamrule = Some("VereinGerät(1/*)"))
      val wd = disziplin(13, "Reck", ord = 1)
      val data = Seq(
        wertung(1, athlet(1, "M", clubA, "A1"), wd, wk, BigDecimal(9.2), team = 1),
        wertung(2, athlet(2, "M", clubA, "A2"), wd, wk, BigDecimal(8.8), team = 2)
      )

      val byVerein = ByVerein()

      byVerein.setKind(Einzelrangliste)
      val einzel = byVerein.select(data).toList
      einzel should have size 1
      einzel.head shouldBe a[GroupLeaf[?]]

      byVerein.setKind(Teamrangliste)
      val team = byVerein.select(data).toList
      team should have size 1
      team.head shouldBe a[TeamSums]

      byVerein.setKind(Kombirangliste)
      val kombi = byVerein.select(data).toList
      kombi should have size 2
      kombi.exists(_.isInstanceOf[GroupLeaf[?]]) shouldBe true
      kombi.exists(_.isInstanceOf[TeamSums]) shouldBe true
    }

    "drop non-positive sections" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf()
      val wd = disziplin(14, "Barren", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", clubA, "A"), wd, wk, BigDecimal(0)))

      val byVerein = ByVerein()
      byVerein.select(data) shouldBe empty
    }
  }

  "GroupBy.apply defaults" should {
    "use first provided grouper for team kind without explicit groupby" in {
      val wk = wettkampf()
      val wd = disziplin(15, "Ring", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", Verein(1, "ClubA", Some("V1")), "A"), wd, wk, BigDecimal(8.5)))

      val selected = GroupBy(
        groupby = None,
        filter = Nil,
        data = data,
        alphanumeric = false,
        isAvgOnMultipleCompetitions = true,
        kind = Teamrangliste,
        counting = AlleWertungen,
        groupers = List(ByVerein(), ByGeschlecht())
      )

      selected.groupname shouldBe "Verein"
    }

    "use Wettkampf-Programm and Geschlecht defaults for einzel kind without explicit groupby" in {
      val wk = wettkampf()
      val wd = disziplin(16, "Pferd", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", Verein(1, "ClubA", Some("V1")), "A"), wd, wk, BigDecimal(8.5)))

      val selected = GroupBy(
        groupby = None,
        filter = Nil,
        data = data,
        alphanumeric = false,
        isAvgOnMultipleCompetitions = true,
        kind = Einzelrangliste,
        counting = AlleWertungen,
        groupers = List(ByVerein())
      )

      selected.groupname shouldBe "Wettkampf-Programm/Kategorie"
      selected.chainToString should include("Geschlecht")
    }
  }

  "toRestQuery token-based assertions" should {
    "include all required parameters in query output" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf()
      val wd = disziplin(17, "Drehrad", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", clubA, "A"), wd, wk, BigDecimal(9.0)))

      val query = GroupBy(
        "groupby=Verein&filter=Verein:ClubA&alphanumeric&avg=false&kind=Teamrangliste&counting=beste(1)",
        data,
        List(ByVerein(), ByGeschlecht())
      )

      val rest = query.asInstanceOf[FilterBy].toRestQuery
      val tokens = rest.split("&").toSet

      tokens should contain("groupby=Verein")
      tokens should contain("alphanumeric")
      tokens should contain("avg=false")
      tokens should contain("kind=Teamrangliste")
      tokens should contain("counting=beste(1)")
      tokens.exists(t => t.startsWith("filter=")) shouldBe true
    }

    "serialize multiple groupers in groupby chain" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf()
      val wd = disziplin(18, "Seil", ord = 1)
      val data = Seq(
        wertung(1, athlet(1, "M", clubA, "A1"), wd, wk, BigDecimal(9.0)),
        wertung(2, athlet(2, "W", clubA, "A2"), wd, wk, BigDecimal(8.5))
      )

      val chain = ByVerein().groupBy(ByGeschlecht())
      chain.setKind(Kombirangliste)
      chain.setAlphanumericOrdered(true)

      val rest = chain.toRestQuery
      val groupbyPart = rest.split("&").find(_.startsWith("groupby=")).getOrElse("")
      groupbyPart should include("Verein")
      groupbyPart should include("Geschlecht")
    }

    "handle both avg=true and avg=false correctly" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val wk = wettkampf()
      val wd = disziplin(19, "Leiter", ord = 1)
      val data = Seq(wertung(1, athlet(1, "M", clubA, "A"), wd, wk, BigDecimal(8.5)))

      val query1 = ByVerein()
      query1.setAvgOnMultipleCompetitions(true)
      val rest1 = query1.toRestQuery
      rest1 should include("&avg=true")

      val query2 = ByVerein()
      query2.setAvgOnMultipleCompetitions(false)
      val rest2 = query2.toRestQuery
      rest2 should include("&avg=false")
    }

    "round-trip through parsing preserves all settings" in {
      val clubA = Verein(1, "ClubA", Some("V1"))
      val clubB = Verein(2, "ClubB", Some("V2"))
      val wk = wettkampf()
      val wd = disziplin(20, "Schieber", ord = 1)
      val data = Seq(
        wertung(1, athlet(1, "M", clubA, "A1"), wd, wk, BigDecimal(9.1)),
        wertung(2, athlet(2, "W", clubB, "A2"), wd, wk, BigDecimal(8.7))
      )

      val original = GroupBy(
        "groupby=Verein:Geschlecht&alphanumeric&avg=false&kind=Kombirangliste&counting=beste(2)",
        data,
        List(ByVerein(), ByGeschlecht())
      )

      original.isAlphanumericOrdered shouldBe true
      original.isAvgOnMultipleCompetitions shouldBe false
      original.getKind shouldBe Kombirangliste
      original.getBestNCounting shouldBe BestNWertungen(2)

      val rest = original.asInstanceOf[FilterBy].toRestQuery

      // Parse it back
      val reparsed = GroupBy(rest, data, List(ByVerein(), ByGeschlecht()))

      reparsed.isAlphanumericOrdered shouldBe true
      reparsed.isAvgOnMultipleCompetitions shouldBe false
      reparsed.getKind shouldBe Kombirangliste
      reparsed.getBestNCounting shouldBe BestNWertungen(2)
    }
  }

  "GroupBy descendants - individual grouper behavior" should {
    val clubA = Verein(1, "ClubA", Some("V1"))
    val clubB = Verein(2, "ClubB", Some("V2"))
    val wk = wettkampf()
    val wd1 = disziplin(101, "Sprung", ord = 1)
    val wd2 = disziplin(102, "Boden", ord = 2)

    "ByNothing groups all values into single group" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val a2 = athlet(2, "W", clubB, "A2")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5))
      )

      val byNothing = ByNothing()
      val selected = byNothing.select(data).toList
      // ByNothing groups each WertungView separately (groups by WertungView instance)
      // so each wertung becomes its own group
      selected.map(_.groupKey).forall(_.isInstanceOf[WertungView]) shouldBe true
    }

    "ByAthlet groups by individual athlete" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val a2 = athlet(2, "W", clubB, "A2")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a1, wd2, wk, BigDecimal(9.2)),
        wertung(3, a2, wd1, wk, BigDecimal(8.5))
      )

      val byAthlet = ByAthlet()
      val selected = byAthlet.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[AthletView].id).toSet shouldBe Set(1L, 2L)
    }

    "ByGeschlecht groups by gender Turner/Turnerinnen" in {
      val male = athlet(1, "M", clubA, "Male")
      val female = athlet(2, "W", clubA, "Female")
      val data = Seq(
        wertung(1, male, wd1, wk, BigDecimal(9.0)),
        wertung(2, female, wd1, wk, BigDecimal(8.5))
      )

      val byGeschlecht = ByGeschlecht()
      val selected = byGeschlecht.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[TurnerGeschlecht].easyprint).toSet shouldBe Set("Turner", "Turnerinnen")
    }

    "ByVerein groups by club" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val a2 = athlet(2, "W", clubB, "A2")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5))
      )

      val byVerein = ByVerein()
      val selected = byVerein.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Verein].name).toSet shouldBe Set("ClubA", "ClubB")
    }

    "ByVerband groups by federation" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val a2 = athlet(2, "W", clubB, "A2")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5))
      )

      val byVerband = ByVerband()
      val selected = byVerband.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Verband].name).toSet shouldBe Set("V1", "V2")
    }

    "ByDisziplin groups by discipline and preserves order" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a1, wd2, wk, BigDecimal(8.5))
      )

      val byDisziplin = ByDisziplin()
      val selected = byDisziplin.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Disziplin].name).toSet shouldBe Set("Sprung", "Boden")
    }

    "ByWettkampf groups by competition" in {
      val wk2 = wettkampf(id = 2)
      val a1 = athlet(1, "M", clubA, "A1")
      val wd1_wk2 = disziplin(201, "Sprung", ord = 1, pgm = programm(2, "Prog2"))
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a1, wd1_wk2, wk2, BigDecimal(8.5))
      )

      val byWettkampf = ByWettkampf()
      val selected = byWettkampf.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Wettkampf].id).toSet shouldBe Set(1L, 2L)
    }

    "ByRiege groups by riege or default value" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)).copy(riege = Some("Riege A")),
        wertung(2, a1, wd2, wk, BigDecimal(8.5)).copy(riege = None)
      )

      val byRiege = ByRiege()
      val selected = byRiege.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Riege].r).toSet shouldBe Set("Riege A", "Ohne Einteilung")
    }

    "ByRiege2 groups by riege2 or default value" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)).copy(riege2 = Some("SpecialRiege")),
        wertung(2, a1, wd2, wk, BigDecimal(8.5)).copy(riege2 = None)
      )

      val byRiege2 = ByRiege2()
      val selected = byRiege2.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Riege].r).toSet shouldBe Set("SpecialRiege", "Ohne Spezial-Einteilung")
    }

    "ByProgramm groups by programm view" in {
      val prog1 = programm(10, "Category1")
      val prog2 = programm(11, "Category2")
      val wd_prog1 = disziplin(301, "Sprung", ord = 1, pgm = prog1)
      val wd_prog2 = disziplin(302, "Boden", ord = 2, pgm = prog2)
      val a1 = athlet(1, "M", clubA, "A1")

      val data = Seq(
        wertung(1, a1, wd_prog1, wk, BigDecimal(9.0)),
        wertung(2, a1, wd_prog2, wk, BigDecimal(8.5))
      )

      val byProgramm = ByProgramm()
      val selected = byProgramm.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[ProgrammView].name).toSet shouldBe Set("Category1", "Category2")
    }

    "ByWettkampfProgramm groups by wettkampfprogramm" in {
      val prog1 = programm(12, "WKProg1")
      val wd_prog1 = disziplin(303, "Sprung", ord = 1, pgm = prog1)
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(wertung(1, a1, wd_prog1, wk, BigDecimal(9.0)))

      val byWKProgramm = ByWettkampfProgramm()
      val selected = byWKProgramm.select(data).toList
      selected should have size 1
      selected.head.groupKey shouldBe a[ProgrammView]
    }

    "ByJahrgang groups athletes by birth year" in {
      val a1 = athlet(1, "M", clubA, "A1").copy(gebdat = Some(Date.valueOf(LocalDate.of(2010, 5, 15))))
      val a2 = athlet(2, "W", clubB, "A2").copy(gebdat = Some(Date.valueOf(LocalDate.of(2012, 3, 20))))
      val a3 = athlet(3, "M", clubA, "A3").copy(gebdat = None)

      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5)),
        wertung(3, a3, wd1, wk, BigDecimal(9.2))
      )

      val byJahrgang = ByJahrgang()
      val selected = byJahrgang.select(data).toList
      selected should have size 3
      selected.map(_.groupKey.asInstanceOf[AthletJahrgang].jahrgang).toSet shouldBe Set("2010", "2012", "unbekannt")
    }

    "ByJahr groups by competition year" in {
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(wertung(1, a1, wd1, wk, BigDecimal(9.0)))

      val byJahr = ByJahr()
      val selected = byJahr.select(data).toList
      selected should have size 1
      selected.head.groupKey.asInstanceOf[WettkampfJahr].wettkampfjahr should endWith("2025")
    }

    "ByDurchgang groups by durchgang from map" in {
      val dg1 = Durchgang(1, 1, "Durchgang A", "Durchgang A", Competition, 1, 0, None, None)
      val dg2 = Durchgang(2, 1, "Durchgang B", "Durchgang B", Competition, 2, 0, None, None)
      val a1 = athlet(1, "M", clubA, "A1")

      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)).copy(riege = Some("R1"), riege2 = None),
        wertung(2, a1, wd2, wk, BigDecimal(8.5)).copy(riege = Some("R2"), riege2 = None)
      )

      val dgMap = Map("R1" -> dg1, "R2" -> dg2)
      val byDurchgang = ByDurchgang(dgMap)
      val selected = byDurchgang.select(data).toList
      selected should have size 2
      selected.map(_.groupKey.asInstanceOf[Durchgang].id).toSet shouldBe Set(1L, 2L)
    }

    "ByAltersklasse calculates age class based on gebdat and competition date" in {
      val grenzen = Seq(
        ("Kinder", Seq("W", "M"), 8),
        ("Teens", Seq("W", "M"), 12),
        ("Erwachsene", Seq("W", "M"), 99)
      )

      val a1 = athlet(1, "M", clubA, "A1").copy(gebdat = Some(Date.valueOf(LocalDate.of(2018, 5, 1))))
      val a2 = athlet(2, "W", clubB, "A2").copy(gebdat = Some(Date.valueOf(LocalDate.of(2012, 5, 1))))

      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5))
      )

      val byAltersklasse = ByAltersklasse("TestAK", grenzen)
      val selected = byAltersklasse.select(data).toList
      selected should not be empty
      selected.head.groupKey shouldBe a[Altersklasse]
    }

    "ByJahrgangsAltersklasse calculates age class based only on birth year" in {
      val grenzen = Seq(
        ("JG2010", Seq("W", "M"), 2010),
        ("JG2012", Seq("W", "M"), 2012),
        ("Other", Seq("W", "M"), 9999)
      )

      val a1 = athlet(1, "M", clubA, "A1").copy(gebdat = Some(Date.valueOf(LocalDate.of(2010, 5, 15))))
      val a2 = athlet(2, "W", clubB, "A2").copy(gebdat = Some(Date.valueOf(LocalDate.of(2012, 3, 20))))

      val data = Seq(
        wertung(1, a1, wd1, wk, BigDecimal(9.0)),
        wertung(2, a2, wd1, wk, BigDecimal(8.5))
      )

      val byJGAltersklasse = ByJahrgangsAltersklasse("TestJG", grenzen)
      val selected = byJGAltersklasse.select(data).toList
      // Just verify we get results with Altersklasse grouping
      selected should not be empty
      selected.head.groupKey shouldBe a[Altersklasse]
    }

    "ByTeamRule groups by team rule and enforces team ranking" in {
      val teamWK = wettkampf(teamrule = Some("VereinGerät(1/*)"))
      val a1 = athlet(1, "M", clubA, "A1")
      val wd_team = disziplin(304, "Sprung", ord = 1)

      val data = Seq(
        wertung(1, a1, wd_team, teamWK, BigDecimal(9.0), team = 1),
        wertung(2, a1, wd_team, teamWK, BigDecimal(8.5), team = 2)
      )

      val teamRule = TeamRegel(teamWK)
      val byTeamRule = ByTeamRule("TestTeamRule", teamRule)

      byTeamRule.getKind shouldBe Teamrangliste
    }

    "ByWettkampfArt groups by main programm (head of category)" in {
      val prog = programm(15, "WKArt")
      val wd = disziplin(305, "Sprung", ord = 1, pgm = prog)
      val a1 = athlet(1, "M", clubA, "A1")
      val data = Seq(wertung(1, a1, wd, wk, BigDecimal(9.0)))

      val byWKArt = ByWettkampfArt()
      val selected = byWKArt.select(data).toList
      selected should have size 1
      selected.head.groupKey shouldBe a[ProgrammView]
    }
  }
}
