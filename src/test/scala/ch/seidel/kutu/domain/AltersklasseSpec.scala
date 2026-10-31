package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AltersklasseSpec extends AnyWordSpec with Matchers {
  private def programm(name: String, parent: Option[ProgrammView] = None): ProgrammView =
    ProgrammView(1L, name, 0, parent, 0, 0, 0, "", 0, 0)

  "Altersklasse.parseGrenzen" should {
    "parse single ages, ranges, and stepped ranges while inheriting labels" in {
      Altersklasse.parseGrenzen("AK6-8,10,AK12-16/2") shouldBe Seq(
        ("AK", Seq.empty, 6),
        ("AK", Seq.empty, 9),
        ("AK", Seq.empty, 10),
        ("AK", Seq.empty, 12),
        ("AK", Seq.empty, 14),
        ("AK", Seq.empty, 16)
      )
    }

    "parse gender and program qualifiers" in {
      Altersklasse.parseGrenzen("AK(W+BS)8-10") shouldBe Seq(
        ("AK", Seq("W", "BS"), 8),
        ("AK", Seq("W", "BS"), 11)
      )
    }

    "use the fallback label for an unlabeled first entry" in {
      Altersklasse.parseGrenzen("6-8", "Jugend") shouldBe Seq(
        ("Jugend", Seq.empty, 6),
        ("Jugend", Seq.empty, 9)
      )
    }
  }

  "Altersklasse construction" should {
    "create contiguous classes from age boundaries, including open ends" in {
      Altersklasse("AK6,AK10") shouldBe Seq(
        Altersklasse("AK", 0, 5, Seq.empty),
        Altersklasse("AK", 6, 9, Seq.empty),
        Altersklasse("AK", 10, 0, Seq.empty)
      )
    }

    "create no classes for empty boundaries" in {
      Altersklasse(Seq.empty) shouldBe empty
    }
  }

  "Altersklasse matching" should {
    "include both age boundaries and treat zero as an open end" in {
      val klasse = Altersklasse("AK", 6, 10, Seq.empty)
      val openEnded = Altersklasse("AK", 11, 0, Seq.empty)

      klasse.matchesAlter(6) shouldBe true
      klasse.matchesAlter(10) shouldBe true
      klasse.matchesAlter(5) shouldBe false
      klasse.matchesAlter(11) shouldBe false
      openEnded.matchesAlter(11) shouldBe true
      openEnded.matchesAlter(80) shouldBe true
      Altersklasse("AK", 0, 5, Seq.empty).matchesAlter(5) shouldBe true
    }

    "match only configured genders and allow every gender when none is configured" in {
      val femaleClass = Altersklasse("AK", 6, 10, Seq("W"))
      val unrestrictedClass = Altersklasse("AK", 6, 10, Seq.empty)

      femaleClass.matchesGeschlecht("W") shouldBe true
      femaleClass.matchesGeschlecht("M") shouldBe false
      unrestrictedClass.matchesGeschlecht("M") shouldBe true
    }

    "match program qualifiers anywhere in the program path" in {
      val root = programm("BS")
      val child = programm("Boden", Some(root))
      val qualifiedClass = Altersklasse("AK", 6, 10, Seq("BS"))

      qualifiedClass.matchesProgramm(child) shouldBe true
      qualifiedClass.matchesProgramm(programm("Kuer")) shouldBe false
      Altersklasse("AK", 6, 10, Seq.empty).matchesProgramm(child) shouldBe true
    }
  }

  "Altersklasse.apply" should {
    "select a class matching age, gender, and a program ancestor" in {
      val classes = Altersklasse("AK(M+BS)6-10,AK(W+BS)6-10")
      val program = programm("Boden", Some(programm("BS")))

      Altersklasse(classes, 8, "M", program) shouldBe Altersklasse("AK", 6, 10, Seq("M", "BS"))
      Altersklasse(classes, 8, "W", program) shouldBe Altersklasse("AK", 6, 10, Seq("W", "BS"))
    }

    "return a single-age fallback when no configured class matches" in {
      val classes = Altersklasse("AK(M+BS)6-10")
      val program = programm("Kuer")

      Altersklasse(classes, 8, "M", program) shouldBe Altersklasse("AK", 8, 8, Seq("M", "Kuer"))
    }
  }
}
