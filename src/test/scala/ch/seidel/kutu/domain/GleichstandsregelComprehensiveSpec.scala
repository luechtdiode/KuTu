package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

/**
 * Comprehensive test coverage for all Gleichstandsregel subclasses.
 * Covers all case classes, case objects, edge cases, and formula parsing.
 */
class GleichstandsregelComprehensiveSpec extends AnyWordSpec with Matchers {

  private val testDate = LocalDate.of(2023, 3, 3)
  private val wk = Wettkampf(1L, None, testDate, "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None, None, None)
  private val testVerein = Verein(1L, "Testverein", Some("Testverband"))
  private val testProgramm = ProgrammView(44L, "Testprogramm", 0, None, 1, 0, 100, UUID.randomUUID().toString, 1, 0)

  private val diszipline = List("Boden", "Pauschen", "Ring", "Sprung", "Barren", "Reck")

  private def createAthlet(name: String, birthdate: LocalDate = LocalDate.of(2010, 5, 15)): AthletView = {
    Athlet(1L).copy(
      name = name,
      vorname = "Test",
      gebdat = Some(birthdate),
      geschlecht = "M"
    ).toAthletView(Some(testVerein))
  }

  private def createWertung(
      athletView: AthletView,
      disziplinIndex: Int,
      disziplinName: String,
      dnote: BigDecimal,
      enote: BigDecimal
  ): WertungView = {
    val wd = WettkampfdisziplinView(
      100 + disziplinIndex,
      testProgramm,
      Disziplin(disziplinIndex, disziplinName),
      "",
      None,
      StandardWettkampf(1.0),
      1, 1, disziplinIndex, 3, 1, 0, 30, 1
    )
    val endnote = enote + dnote
    WertungView(wd.id, athletView, wd, wk, Some(dnote), Some(enote), Some(endnote), None, None, 0, None, None)
  }

  private def createWertungen(
      name: String,
      birthdate: LocalDate = LocalDate.of(2010, 5, 15),
      scores: List[(BigDecimal, BigDecimal)]
  ): List[WertungView] = {
    val athletView = createAthlet(name, birthdate)
    diszipline.zip(scores).zipWithIndex.map { case ((diszName, (dnote, enote)), idx) =>
      createWertung(athletView, idx, diszName, dnote, enote)
    }
  }

  "GleichstandsregelDefault" should {
    "always return 0 for any comparison" in {
      val left = createWertungen("A", scores = List((1.0, 8.0), (2.0, 7.0), (1.5, 7.5), (1.0, 8.0), (2.0, 7.0), (1.5, 7.5)))
      val right = createWertungen("B", scores = List((3.0, 5.0), (4.0, 4.0), (3.5, 4.5), (3.0, 5.0), (4.0, 4.0), (3.5, 4.5)))
      GleichstandsregelDefault.compare(left, right) shouldBe 0
    }

    "have toFormel return 'Ohne'" in {
      GleichstandsregelDefault.toFormel shouldBe "Ohne"
    }

    "handle empty lists" in {
      GleichstandsregelDefault.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelENoteSumme" should {
    "prefer higher E-Note sum" in {
      val left = createWertungen("A", scores = List((1.0, 9.0), (1.0, 8.5), (1.0, 8.0), (1.0, 7.5), (1.0, 7.0), (1.0, 6.5)))
      val right = createWertungen("B", scores = List((1.0, 9.0), (1.0, 8.5), (1.0, 8.0), (1.0, 7.5), (1.0, 7.0), (1.0, 6.4)))
      GleichstandsregelENoteSumme.compare(left, right) should be > 0
    }

    "return 0 for equal E-Note sums" in {
      val left = createWertungen("A", scores = List((1.0, 8.0), (2.0, 7.0), (1.5, 7.5), (1.0, 8.0), (2.0, 7.0), (1.5, 7.5)))
      val right = createWertungen("B", scores = List((3.0, 8.0), (4.0, 7.0), (3.5, 7.5), (3.0, 8.0), (4.0, 7.0), (3.5, 7.5)))
      GleichstandsregelENoteSumme.compare(left, right) shouldBe 0
    }

    "prefer lower E-Note sum when reversed" in {
      val left = createWertungen("A", scores = List((1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0)))
      val right = createWertungen("B", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0)))
      GleichstandsregelENoteSumme.compare(left, right) should be < 0
    }

    "have toFormel return 'E-Note-Summe'" in {
      GleichstandsregelENoteSumme.toFormel shouldBe "E-Note-Summe"
    }

    "handle empty lists" in {
      GleichstandsregelENoteSumme.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelENoteBest" should {
    "prefer higher best E-Note" in {
      val left = createWertungen("A", scores = List((1.0, 9.5), (1.0, 7.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0)))
      val right = createWertungen("B", scores = List((1.0, 9.4), (1.0, 7.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0)))
      GleichstandsregelENoteBest.compare(left, right) should be > 0
    }

    "return 0 for equal best E-Notes" in {
      val left = createWertungen("A", scores = List((1.0, 9.5), (1.0, 7.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0)))
      val right = createWertungen("B", scores = List((1.0, 8.0), (1.0, 9.5), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0), (1.0, 6.0)))
      GleichstandsregelENoteBest.compare(left, right) shouldBe 0
    }

    "have toFormel return 'E-Note-Best'" in {
      GleichstandsregelENoteBest.toFormel shouldBe "E-Note-Best"
    }

    "handle empty lists" in {
      GleichstandsregelENoteBest.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelDNoteSumme" should {
    "prefer higher D-Note sum" in {
      val left = createWertungen("A", scores = List((2.0, 8.0), (2.5, 7.5), (3.0, 7.0), (2.0, 8.0), (2.5, 7.5), (3.0, 7.0)))
      val right = createWertungen("B", scores = List((2.0, 8.0), (2.5, 7.5), (3.0, 7.0), (2.0, 8.0), (2.5, 7.5), (2.9, 7.0)))
      GleichstandsregelDNoteSumme.compare(left, right) should be > 0
    }

    "return 0 for equal D-Note sums" in {
      val left = createWertungen("A", scores = List((2.0, 8.0), (2.5, 7.5), (3.0, 7.0), (2.0, 8.0), (2.5, 7.5), (3.0, 7.0)))
      val right = createWertungen("B", scores = List((2.0, 9.0), (2.5, 8.5), (3.0, 8.0), (2.0, 9.0), (2.5, 8.5), (3.0, 8.0)))
      GleichstandsregelDNoteSumme.compare(left, right) shouldBe 0
    }

    "have toFormel return 'D-Note-Summe'" in {
      GleichstandsregelDNoteSumme.toFormel shouldBe "D-Note-Summe"
    }

    "handle empty lists" in {
      GleichstandsregelDNoteSumme.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelDNoteBest" should {
    "prefer higher best D-Note" in {
      val left = createWertungen("A", scores = List((3.5, 7.0), (2.0, 8.0), (1.5, 8.5), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((3.4, 7.0), (2.0, 8.0), (1.5, 8.5), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      GleichstandsregelDNoteBest.compare(left, right) should be > 0
    }

    "return 0 for equal best D-Notes" in {
      val left = createWertungen("A", scores = List((3.5, 7.0), (2.0, 8.0), (1.5, 8.5), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((2.0, 8.0), (3.5, 7.0), (1.5, 8.5), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      GleichstandsregelDNoteBest.compare(left, right) shouldBe 0
    }

    "have toFormel return 'D-Note-Best'" in {
      GleichstandsregelDNoteBest.toFormel shouldBe "D-Note-Best"
    }

    "handle empty lists" in {
      GleichstandsregelDNoteBest.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelJugendVorAlter" should {
    "prefer younger athlete" in {
      val younger = createWertungen("Younger", birthdate = LocalDate.of(2012, 6, 15),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      val older = createWertungen("Older", birthdate = LocalDate.of(2008, 3, 20),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      GleichstandsregelJugendVorAlter.compare(younger, older) should be > 0
    }

    "return 0 for same age athletes" in {
      val athlete1 = createWertungen("A", birthdate = LocalDate.of(2010, 5, 15),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      val athlete2 = createWertungen("B", birthdate = LocalDate.of(2010, 5, 15),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      GleichstandsregelJugendVorAlter.compare(athlete1, athlete2) shouldBe 0
    }

    "prefer older when ages are reversed" in {
      val younger = createWertungen("Younger", birthdate = LocalDate.of(2015, 1, 1),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      val older = createWertungen("Older", birthdate = LocalDate.of(2005, 1, 1),
        scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      GleichstandsregelJugendVorAlter.compare(older, younger) should be < 0
    }

    "have toFormel return 'JugendVorAlter'" in {
      GleichstandsregelJugendVorAlter.toFormel shouldBe "JugendVorAlter"
    }

    "handle empty lists" in {
      GleichstandsregelJugendVorAlter.compare(List.empty, List.empty) shouldBe 0
    }
  }

  "GleichstandsregelDisziplin" should {
    "compare by specified discipline order" in {
      val left = createWertungen("A", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 9.5), (1.0, 7.0), (1.0, 7.0)))
      val right = createWertungen("B", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 9.4), (1.0, 7.0), (1.0, 7.0)))
      val regel = GleichstandsregelDisziplin(List("Sprung", "Reck"))
      regel.compare(left, right) should be > 0
    }

    "check secondary discipline when primary is equal" in {
      val left = createWertungen("A", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 8.0), (1.0, 7.0), (1.0, 9.3)))
      val right = createWertungen("B", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 8.0), (1.0, 7.0), (1.0, 9.2)))
      val regel = GleichstandsregelDisziplin(List("Sprung", "Reck"))
      regel.compare(left, right) should be > 0
    }

    "return 0 when all specified disciplines are equal" in {
      val left = createWertungen("A", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 8.0), (1.0, 7.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 8.0), (1.0, 7.0), (1.0, 9.0)))
      val regel = GleichstandsregelDisziplin(List("Sprung", "Reck"))
      regel.compare(left, right) shouldBe 0
    }

    "have correct toFormel format" in {
      val regel = GleichstandsregelDisziplin(List("Boden", "Sprung", "Reck"))
      regel.toFormel shouldBe "Disziplin(Boden,Sprung,Reck)"
    }

    "handle single discipline" in {
      val regel = GleichstandsregelDisziplin(List("Boden"))
      regel.toFormel shouldBe "Disziplin(Boden)"
    }
  }

  "GleichstandsregelStreichDisziplin" should {
    "compare by removing specified disciplines" in {
      // Left has better total when Boden (first discipline) is removed
      val left = createWertungen("A", scores = List((1.0, 5.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 9.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val regel = GleichstandsregelStreichDisziplin(List("Boden"))
      regel.compare(left, right) should be > 0
    }

    "progressively remove disciplines" in {
      // When Boden is removed, equal. When Pauschen is also removed, left wins
      val left = createWertungen("A", scores = List((1.0, 5.0), (1.0, 7.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 9.0), (1.0, 8.0), (1.0, 8.5), (1.0, 8.5), (1.0, 8.5), (1.0, 8.5)))
      val regel = GleichstandsregelStreichDisziplin(List("Boden", "Pauschen"))
      regel.compare(left, right) should be > 0
    }

    "return 0 when remaining scores are equal" in {
      val left = createWertungen("A", scores = List((1.0, 5.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val right = createWertungen("B", scores = List((1.0, 9.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val regel = GleichstandsregelStreichDisziplin(List("Boden"))
      regel.compare(left, right) shouldBe 0
    }

    "have correct toFormel format" in {
      val regel = GleichstandsregelStreichDisziplin(List("Boden", "Reck"))
      regel.toFormel shouldBe "StreichDisziplin(Boden,Reck)"
    }

    "handle removing all but one discipline" in {
      val left = createWertungen("A", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 9.5), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0)))
      val right = createWertungen("B", scores = List((1.0, 7.0), (1.0, 7.0), (1.0, 9.4), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0)))
      val regel = GleichstandsregelStreichDisziplin(List("Boden", "Pauschen", "Sprung", "Barren", "Reck"))
      regel.compare(left, right) should be > 0
    }
  }

  "GleichstandsregelStreichWertungen" should {
    "compare by removing minimum Endnote" in {
      // After removing worst score, left has better total
      val left = createWertungen("A", scores = List((1.0, 5.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 6.0), (2.0, 8.5), (2.0, 8.5), (2.0, 8.5), (2.0, 8.5), (2.0, 8.5)))
      val regel = GleichstandsregelStreichWertungen("Endnote", "Min")
      regel.compare(left, right) should be > 0
    }

    "compare by removing maximum Endnote" in {
      // After removing best score, right has better total
      val left = createWertungen("A", scores = List((1.0, 10.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0), (1.0, 7.0)))
      val right = createWertungen("B", scores = List((1.0, 9.5), (1.0, 7.5), (1.0, 7.5), (1.0, 7.5), (1.0, 7.5), (1.0, 7.5)))
      val regel = GleichstandsregelStreichWertungen("Endnote", "Max")
      regel.compare(left, right) should be < 0
    }

    "compare by removing minimum E-Note" in {
      val left = createWertungen("A", scores = List((1.0, 5.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0), (1.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 6.0), (1.0, 8.5), (1.0, 8.5), (1.0, 8.5), (1.0, 8.5), (1.0, 8.5)))
      val regel = GleichstandsregelStreichWertungen("E-Note", "Min")
      regel.compare(left, right) should be > 0
    }

    "compare by removing minimum D-Note" in {
      val left = createWertungen("A", scores = List((0.5, 8.0), (3.0, 8.0), (3.0, 8.0), (3.0, 8.0), (3.0, 8.0), (3.0, 8.0)))
      val right = createWertungen("B", scores = List((1.0, 8.0), (2.5, 8.0), (2.5, 8.0), (2.5, 8.0), (2.5, 8.0), (2.5, 8.0)))
      val regel = GleichstandsregelStreichWertungen("D-Note", "Min")
      regel.compare(left, right) should be > 0
    }

    "progressively remove multiple scores" in {
      // First removal equal, second removal left wins
      val left = createWertungen("A", scores = List((1.0, 5.0), (1.0, 6.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0)))
      val right = createWertungen("B", scores = List((1.0, 5.5), (1.0, 6.5), (2.0, 8.5), (2.0, 8.5), (2.0, 8.5), (2.0, 8.5)))
      val regel = GleichstandsregelStreichWertungen("Endnote", "Min")
      regel.compare(left, right) should be > 0
    }

    "have correct toFormel format for Endnote Min" in {
      val regel = GleichstandsregelStreichWertungen("Endnote", "Min")
      regel.toFormel shouldBe "StreichWertungen(Endnote,Min)"
    }

    "have correct toFormel format for E-Note Max" in {
      val regel = GleichstandsregelStreichWertungen("E-Note", "Max")
      regel.toFormel shouldBe "StreichWertungen(E-Note,Max)"
    }

    "have correct toFormel format for D-Note Min" in {
      val regel = GleichstandsregelStreichWertungen("D-Note", "Min")
      regel.toFormel shouldBe "StreichWertungen(D-Note,Min)"
    }

    "use Min as default when minmax is empty" in {
      val regel = GleichstandsregelStreichWertungen("Endnote", "")
      regel.toFormel shouldBe "StreichWertungen(Endnote,Min)"
    }

    "use Min as default when minmax is null" in {
      val regel = GleichstandsregelStreichWertungen("Endnote", null)
      regel.toFormel shouldBe "StreichWertungen(Endnote,Min)"
    }
  }

  "GleichstandsregelList" should {
    "apply rules in sequence" in {
      val left = createWertungen("A", scores = List((1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val right = createWertungen("B", scores = List((2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0), (2.0, 8.0)))
      // E-Note sum equal (48.0 vs 48.0), D-Note sum different (6.0 vs 12.0), so right has better D-Note sum
      val regel = GleichstandsregelList(List(GleichstandsregelENoteSumme, GleichstandsregelDNoteSumme))
      regel.compare(left, right) should be < 0
    }

    "stop at first non-zero result" in {
      val left = createWertungen("A", scores = List((1.0, 9.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val right = createWertungen("B", scores = List((1.0, 8.5), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      // E-Note sum decides, D-Note sum never checked
      val regel = GleichstandsregelList(List(GleichstandsregelENoteSumme, GleichstandsregelDNoteSumme))
      regel.compare(left, right) should be > 0
    }

    "return 0 when all rules return 0" in {
      val left = createWertungen("A", scores = List((1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val right = createWertungen("B", scores = List((1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val regel = GleichstandsregelList(List(GleichstandsregelENoteSumme, GleichstandsregelDNoteSumme))
      regel.compare(left, right) shouldBe 0
    }

    "have correct toFormel format" in {
      val regel = GleichstandsregelList(List(
        GleichstandsregelENoteSumme,
        GleichstandsregelDNoteSumme,
        GleichstandsregelJugendVorAlter
      ))
      regel.toFormel shouldBe "E-Note-Summe/D-Note-Summe/JugendVorAlter"
    }

    "handle empty rule list" in {
      val regel = GleichstandsregelList(List.empty)
      val left = createWertungen("A", scores = List((1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0), (1.0, 8.0)))
      val right = createWertungen("B", scores = List((2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0), (2.0, 9.0)))
      regel.compare(left, right) shouldBe 0
    }
  }

  "Gleichstandsregel factory" should {
    "parse 'Ohne' correctly" in {
      val regel = Gleichstandsregel("Ohne")
      regel shouldBe a [GleichstandsregelList]
      regel.toFormel shouldBe "Ohne"
    }

    "parse 'E-Note-Summe' correctly" in {
      val regel = Gleichstandsregel("E-Note-Summe")
      regel.toFormel shouldBe "E-Note-Summe"
    }

    "parse 'E-Note-Best' correctly" in {
      val regel = Gleichstandsregel("E-Note-Best")
      regel.toFormel shouldBe "E-Note-Best"
    }

    "parse 'D-Note-Summe' correctly" in {
      val regel = Gleichstandsregel("D-Note-Summe")
      regel.toFormel shouldBe "D-Note-Summe"
    }

    "parse 'D-Note-Best' correctly" in {
      val regel = Gleichstandsregel("D-Note-Best")
      regel.toFormel shouldBe "D-Note-Best"
    }

    "parse 'JugendVorAlter' correctly" in {
      val regel = Gleichstandsregel("JugendVorAlter")
      regel.toFormel shouldBe "JugendVorAlter"
    }

    "parse 'Disziplin(...)' correctly" in {
      val regel = Gleichstandsregel("Disziplin(Boden,Sprung,Reck)")
      regel.toFormel shouldBe "Disziplin(Boden,Sprung,Reck)"
    }

    "parse 'StreichDisziplin(...)' correctly" in {
      val regel = Gleichstandsregel("StreichDisziplin(Boden,Reck)")
      regel.toFormel shouldBe "StreichDisziplin(Boden,Reck)"
    }

    "parse 'StreichWertungen' with default parameters" in {
      val regel = Gleichstandsregel("StreichWertungen")
      regel.toFormel shouldBe "StreichWertungen(Endnote,Min)"
    }

    "parse 'StreichWertungen(Endnote,Min)' correctly" in {
      val regel = Gleichstandsregel("StreichWertungen(Endnote,Min)")
      regel.toFormel shouldBe "StreichWertungen(Endnote,Min)"
    }

    "parse 'StreichWertungen(E-Note,Max)' correctly" in {
      val regel = Gleichstandsregel("StreichWertungen(E-Note,Max)")
      regel.toFormel shouldBe "StreichWertungen(E-Note,Max)"
    }

    "parse 'StreichWertungen(D-Note,Min)' correctly" in {
      val regel = Gleichstandsregel("StreichWertungen(D-Note,Min)")
      regel.toFormel shouldBe "StreichWertungen(D-Note,Min)"
    }

    "parse combined rules with /" in {
      val regel = Gleichstandsregel("E-Note-Summe/D-Note-Summe/JugendVorAlter")
      regel.toFormel shouldBe "E-Note-Summe/D-Note-Summe/JugendVorAlter"
    }

    "parse complex formula" in {
      val regel = Gleichstandsregel("E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)")
      regel.toFormel shouldBe "E-Note-Summe/D-Note-Summe/StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)"
    }

    "parse GeTu formula" in {
      val regel = Gleichstandsregel("Disziplin(Schaukelringe,Sprung,Reck)")
      regel.toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
    }

    "handle empty string" in {
      val regel = Gleichstandsregel("")
      regel shouldBe a [GleichstandsregelList]
    }

    "handle unknown rule type defaults to GleichstandsregelDefault" in {
      val regel = Gleichstandsregel("UnknownRule")
      regel.toFormel shouldBe "Ohne"
    }
  }

  "Gleichstandsregel programm-based factory" should {
    "return default for Athletiktest (id 1-3)" in {
      Gleichstandsregel(1L) shouldBe GleichstandsregelDefault
      Gleichstandsregel(2L) shouldBe GleichstandsregelDefault
      Gleichstandsregel(3L) shouldBe GleichstandsregelDefault
    }

    "return KuTu STV rule for KuTu Programm" in {
      val regel = Gleichstandsregel(11L)
      regel.toFormel should include("StreichWertungen")
    }

    "return GeTu rule for GeTu Kategorie (20-26)" in {
      val regel = Gleichstandsregel(20L)
      regel.toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
    }

    "return GeTu rule for GeTu Kategorie (74-83)" in {
      val regel = Gleichstandsregel(75L)
      regel.toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
    }

    "return default for other ids" in {
      Gleichstandsregel(999L) shouldBe GleichstandsregelDefault
    }
  }

  "Gleichstandsregel wettkampf-based factory" should {
    "use wettkampf punktegleichstandsregel if defined" in {
      val wk = Wettkampf(1L, None, testDate, "Test", 44L, 0, BigDecimal(0d), "", None, None,
        Some("E-Note-Summe/D-Note-Summe"), None, None)
      val regel = Gleichstandsregel(wk)
      regel.toFormel shouldBe "E-Note-Summe/D-Note-Summe"
    }

    "fall back to programm-based rule if punktegleichstandsregel is empty" in {
      val wk = Wettkampf(1L, None, testDate, "Test", 20L, 0, BigDecimal(0d), "", None, None,
        Some(""), None, None)
      val regel = Gleichstandsregel(wk)
      regel.toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
    }

    "fall back to programm-based rule if punktegleichstandsregel is None" in {
      val wk = Wettkampf(1L, None, testDate, "Test", 20L, 0, BigDecimal(0d), "", None, None,
        None, None, None)
      val regel = Gleichstandsregel(wk)
      regel.toFormel shouldBe "Disziplin(Schaukelringe,Sprung,Reck)"
    }
  }

  "Gleichstandsregel predefined rules" should {
    "include 'Ohne'" in {
      Gleichstandsregel.predefined should contain key "Ohne - Punktgleichstand => gleicher Rang"
    }

    "include 'GeTu Punktgleichstandsregel'" in {
      Gleichstandsregel.predefined should contain key "GeTu Punktgleichstandsregel"
    }

    "include 'KuTu Punktgleichstandsregel'" in {
      Gleichstandsregel.predefined should contain key "KuTu Punktgleichstandsregel"
    }

    "include 'KuTu STV Punktgleichstandsregel'" in {
      Gleichstandsregel.predefined should contain key "KuTu STV Punktgleichstandsregel"
    }

    "include 'Individuell'" in {
      Gleichstandsregel.predefined should contain key "Individuell"
    }
  }
}



