package ch.seidel.kutu.domain

import ch.seidel.kutu.domain.Terms.{Block, BlockKind}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TermsSpec extends AnyWordSpec with Matchers {

  private val sample =
    """<!-- Kommentar: nicht rendern -->
      |version: 1.0
      |stand: Juli 2026
      |
      |# Titel der Bedingungen
      |
      |## 1. Erster Abschnitt
      |
      |Erster Absatz mit zwei
      |Zeilen.
      |
      |## 2. Zweiter Abschnitt
      |
      |Zweiter Absatz.
      |""".stripMargin

  "the terms parser" should {

    "read version, stand and title from the header" in {
      val document = Terms.parse(sample)
      document.version shouldBe "1.0"
      document.stand shouldBe "Juli 2026"
      document.title shouldBe "Titel der Bedingungen"
    }

    "classify title, headings and paragraphs" in {
      Terms.parse(sample).blocks shouldBe Seq(
        Block(BlockKind.Title, "Titel der Bedingungen"),
        Block(BlockKind.Heading, "1. Erster Abschnitt"),
        Block(BlockKind.Paragraph, "Erster Absatz mit zwei Zeilen."),
        Block(BlockKind.Heading, "2. Zweiter Abschnitt"),
        Block(BlockKind.Paragraph, "Zweiter Absatz.")
      )
    }

    "join wrapped lines of a paragraph into one block" in {
      val paragraph = Terms.parse(sample).blocks.collect { case Block(BlockKind.Paragraph, text) => text }.head
      paragraph should not include "\n"
    }

    "skip comments" in {
      Terms.parse(sample).text should not include "Kommentar"
    }

    "render text without markup and with the stand date" in {
      val text = Terms.parse(sample).text
      text should include("Titel der Bedingungen")
      text should include("1. Erster Abschnitt")
      text should not include "#"
      text should endWith("Stand: Juli 2026")
    }

    "accept CRLF line endings" in {
      Terms.parse(sample.replace("\n", "\r\n")).blocks shouldBe Terms.parse(sample).blocks
    }

    "fail fast without a version" in {
      assertThrows[IllegalArgumentException](Terms.parse("# Nur ein Titel"))
    }

    "fail fast without content" in {
      assertThrows[IllegalArgumentException](Terms.parse("version: 1.0"))
    }
  }

  "the shipped terms resource" should {

    "expose a version, a title and at least one section" in {
      Terms.version should not be empty
      Terms.title should include("Nutzungsbedingungen")
      Terms.blocks.count(_.kind == BlockKind.Heading) should be >= 1
    }

    "not leak markdown markup into the plain text rendering" in {
      Terms.text should not include "#"
      Terms.text should include("Haftungsausschluss")
      Terms.text should include("24 Stunden")
    }

    "end the plain text rendering with the stand date" in {
      Terms.text should endWith(s"Stand: ${Terms.stand}")
    }
  }
}
