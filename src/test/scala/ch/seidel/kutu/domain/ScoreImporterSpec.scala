package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScoreImporterSpec extends AnyWordSpec with Matchers {

  "ScoreImporter.mapStructuredRows" should {
    "map full, partial and single D rows to the same rowfields shape" in {
      val importer = new ScoreImporter()
      val tableData = Seq(
        Seq("1.", "Muster Max", "2010", "TV Test ZH", "4.20", "13.500", "4.40", "4.10", "13.800", "4.00", "13.000", "4.30", "4.50", "14.000", "3.90", "12.800", "3.80", "12.900", "4.90", "13.000"),
        Seq("2.", "Meier Tom", "2011", "STV Demo AG", "3.80", "12.900", "3.90", "12.700", "3.70", "12.600", "3.80", "4.00", "13.200", "3.60", "12.400", "3.50", "12.30", "2.30", "12.100"),
        Seq("3.", "Keller Jan", "2012", "TV Unit LU", "3.50", "12.100", "3.40", "11.900", "3.30", "11.700", "3.20", "11.600", "3.10", "11.400", "3.00", "11.300", "9.50", "13.000"),
        Seq("Rang", "Name", "JG", "Verein")
      )

      val rowfields = importer.mapStructuredRows(tableData)(
        (geschlecht: String, name: String, jahrgang: String, verein: String) => (s"verein:$verein", s"athlet:$name/$jahrgang"),
        (geraet: String, valueD: String, valueE: String) => s"$geraet:$valueD/$valueE"
      )

      rowfields should have size 3

      rowfields.head._1 shouldBe "athlet:Muster Max/2010"
      rowfields.head._2 shouldBe "verein:TV Test ZH"
      rowfields.head._3 shouldBe List(
        "Boden:4.20/13.500",
        "Pferd Pauschen:4.40,4.10/13.800",
        "Ring:4.00/13.000",
        "Sprung:4.30,4.50/14.000",
        "Barren:3.90/12.800",
        "Reck:3.80/12.900"
      )

      rowfields(1)._3(1) shouldBe "Pferd Pauschen:3.90/12.700"
      rowfields(1)._3(3) shouldBe "Sprung:3.80,4.00/13.200"

      rowfields(2)._3(1) shouldBe "Pferd Pauschen:3.40/11.900"
      rowfields(2)._3(3) shouldBe "Sprung:3.20/11.600"
    }

    "split merged numeric cells before applying row structure matching" in {
      val importer = new ScoreImporter()
      val tableData = Seq(
        Seq("1.", "Muster Max", "2010", "TV Test ZH", "4.20 13.500", "4.00", "2.6715.999", "4.00", "13.000", "4.304.50", "14.000", "3.90", "12.800", "3.80", "12.900", "4.90", "15.000")
      )

      val rowfields = importer.mapStructuredRows(tableData)(
        (name: String, jahrgang: String, verein: String, _: String) => (s"verein:$verein", s"athlet:$name/$jahrgang"),
        (geraet: String, valueD: String, valueE: String) => s"$geraet:$valueD/$valueE"
      )

      rowfields should have size 1
      rowfields.head._3 shouldBe List(
        "Boden:4.20/13.500",
        "Pferd Pauschen:4.00,2.67/15.999",
        "Ring:4.00/13.000",
        "Sprung:4.30,4.50/14.000",
        "Barren:3.90/12.800",
        "Reck:3.80/12.900"
      )
    }

    "split concatenated decimal score values without whitespace" in {
      val importer = new ScoreImporter()
      val tableData = Seq(
        Seq("1.", "Muster Max", "2010", "TV Test ZH", "4.2113.500", "4.404.10", "13.800", "4.00", "13.000", "4.304.50", "14.000", "3.90", "12.800", "3.80", "12.900", "4.9", "80.000")
      )

      val rowfields = importer.mapStructuredRows(tableData)(
        (name: String, jahrgang: String, verein: String, _: String) => (s"verein:$verein", s"athlet:$name/$jahrgang"),
        (geraet: String, valueD: String, valueE: String) => s"$geraet:$valueD/$valueE"
      )

      rowfields should have size 1
      rowfields.head._3 shouldBe List(
        "Boden:4.21/13.500",
        "Pferd Pauschen:4.40,4.10/13.800",
        "Ring:4.00/13.000",
        "Sprung:4.30,4.50/14.000",
        "Barren:3.90/12.800",
        "Reck:3.80/12.900"
      )
    }

    "prefer plausible split when multiple concatenated decimal splits are possible" in {
      val importer = new ScoreImporter()
      val normalized = importer.normalizeRowCells(
        Seq("1.", "Muster Max", "2010", "TV Test ZH", "3.5013.200", "4.444.12", "13.823", "4.01", "13.034", "4.364.50", "14.000", "3.91", "12.821", "3.84", "12.955", "2.9016.150")
      )

      normalized shouldBe Seq(
        "1.", "Muster Max", "2010", "TV Test ZH",
        "3.50", "13.200", "4.44", "4.12", "13.823", "4.01", "13.034", "4.36", "4.50", "14.000", "3.91", "12.821", "3.84", "12.955", "2.90", "16.150"
      )

      val tableData = Seq(normalized)

      val rowfields = importer.mapStructuredRows(tableData)(
        (name: String, jahrgang: String, verein: String, _: String) => (s"verein:$verein", s"athlet:$name/$jahrgang"),
        (geraet: String, valueD: String, valueE: String) => s"$geraet:$valueD/$valueE"
      )

      rowfields should have size 1
      rowfields.head._3 shouldBe List(
        "Boden:3.50/13.200",
        "Pferd Pauschen:4.44,4.12/13.823",
        "Ring:4.01/13.034",
        "Sprung:4.36,4.50/14.000",
        "Barren:3.91/12.821",
        "Reck:3.84/12.955"
      )
    }
  }
}

