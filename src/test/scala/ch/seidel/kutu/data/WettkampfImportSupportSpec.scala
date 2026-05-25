package ch.seidel.kutu.data

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.io.FileOutputStream

class WettkampfImportSupportSpec extends AnyWordSpec with Matchers {

  "WettkampfImportSupport.effectiveGenderValueMapping" should {
    "fall back to default mapping when input is empty" in {
      val mapping = WettkampfImportSupport.effectiveGenderValueMapping("")

      mapping("M") shouldBe "M"
      mapping("F") shouldBe "W"
      mapping("W") shouldBe "W"
    }

    "parse custom mapping and normalize F target to W" in {
      val mapping = WettkampfImportSupport.effectiveGenderValueMapping(
        """x = M
          |y = F
          |z = W""".stripMargin
      )

      mapping("X") shouldBe "M"
      mapping("Y") shouldBe "W"
      mapping("Z") shouldBe "W"
    }
  }

  "WettkampfImportSupport.normalizeGender" should {
    "use mapping first and then fallback normalization" in {
      val mapping = Map("BOY" -> "M", "GIRL" -> "W")

      WettkampfImportSupport.normalizeGender("boy", mapping) shouldBe "M"
      WettkampfImportSupport.normalizeGender("girl", mapping) shouldBe "W"
      WettkampfImportSupport.normalizeGender("F", mapping) shouldBe "W"
      WettkampfImportSupport.normalizeGender("", mapping) shouldBe "M"
      WettkampfImportSupport.normalizeGender("unknown", mapping) shouldBe "M"
    }
  }

  "WettkampfImportSupport.mapCsvRows" should {
    "map source CSV headers to logical import fields" in {
      val headers = Seq("SURNAME", "FIRST", "YEAR", "CAT")
      val rows = Seq("Muster;Max;2010;P4", "Meier;Tom;2011;P5")
      val fieldMapping = Map(
        "NAME" -> Some("SURNAME"),
        "VORNAME" -> Some("FIRST"),
        "JAHRGANG" -> Some("YEAR"),
        "KATEGORIE" -> Some("CAT")
      )

      val mapped = WettkampfImportSupport.mapCsvRows(headers, rows, fieldMapping)

      mapped should have size 2
      mapped.head("NAME") shouldBe "Muster"
      mapped.head("VORNAME") shouldBe "Max"
      mapped.head("JAHRGANG") shouldBe "2010"
      mapped.head("KATEGORIE") shouldBe "P4"
      mapped(1)("NAME") shouldBe "Meier"
    }

    "fallback to empty value when mapped source column is missing" in {
      val headers = Seq("SURNAME", "FIRST")
      val rows = Seq("Muster;Max")
      val fieldMapping = Map(
        "NAME" -> Some("SURNAME"),
        "KATEGORIE" -> Some("CAT")
      )

      val mapped = WettkampfImportSupport.mapCsvRows(headers, rows, fieldMapping)

      mapped should have size 1
      mapped.head("NAME") shouldBe "Muster"
      mapped.head("KATEGORIE") shouldBe ""
    }
  }

  "WettkampfImportSupport.readCsvFile" should {
    "return None for an empty file" in {
      val tempFile = Files.createTempFile("kutu-empty", ".csv")
      try {
        val read = WettkampfImportSupport.readCsvFile(tempFile.toUri)
        read shouldBe None
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }

    "read header and rows from a non-empty file" in {
      val tempFile = Files.createTempFile("kutu-csv", ".csv")
      try {
        Files.writeString(tempFile, "NAME;VORNAME\nMuster;Max\n", StandardCharsets.ISO_8859_1)

        val read = WettkampfImportSupport.readCsvFile(tempFile.toUri)

        read should not be empty
        val (headers, rows) = read.get
        headers shouldBe Seq("NAME", "VORNAME")
        rows shouldBe Seq("Muster;Max")
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  "WettkampfImportSupport.readTabularFile" should {
    "read CSV file content as source-row maps" in {
      val tempFile = Files.createTempFile("kutu-tabular", ".csv")
      try {
        Files.writeString(tempFile, "NAME;KATEGORIE\nMuster;P4\n", StandardCharsets.ISO_8859_1)

        val read = WettkampfImportSupport.readTabularFile(tempFile.toUri)

        read should not be empty
        val (headers, rows) = read.get
        headers shouldBe Seq("NAME", "KATEGORIE")
        rows should have size 1
        rows.head("NAME") shouldBe "Muster"
        rows.head("KATEGORIE") shouldBe "P4"
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }

    "read XLSX file content as source-row maps" in {
      val tempFile = Files.createTempFile("kutu-tabular", ".xlsx")
      try {
        val workbook = new XSSFWorkbook()
        try {
          val sheet = workbook.createSheet("Import")
          val header = sheet.createRow(0)
          header.createCell(0).setCellValue("NAME")
          header.createCell(1).setCellValue("KATEGORIE")

          val row = sheet.createRow(1)
          row.createCell(0).setCellValue("Meier")
          row.createCell(1).setCellValue("P5")

          val out = new FileOutputStream(tempFile.toFile)
          try workbook.write(out)
          finally out.close()
        } finally workbook.close()

        val read = WettkampfImportSupport.readTabularFile(tempFile.toUri)

        read should not be empty
        val (headers, rows) = read.get
        headers shouldBe Seq("NAME", "KATEGORIE")
        rows should have size 1
        rows.head("NAME") shouldBe "Meier"
        rows.head("KATEGORIE") shouldBe "P5"
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  "WettkampfImportSupport.mapExcelClipboardRows" should {
    "convert tab-separated clipboard rows into logical import rows" in {
      val clipboard =
        """Muster	Max	01.02.2010	1	X
          |Meier	Tom	2011	P5	""".stripMargin

      val mapped = WettkampfImportSupport.mapExcelClipboardRows(clipboard)

      mapped should have size 2
      mapped.head("NAME") shouldBe "Muster"
      mapped.head("VORNAME") shouldBe "Max"
      mapped.head("JAHRGANG") shouldBe "2010"
      mapped.head("KATEGORIE") shouldBe "1"
      mapped.head("GESCHLECHT") shouldBe "W"

      mapped(1)("NAME") shouldBe "Meier"
      mapped(1)("JAHRGANG") shouldBe "2011"
      mapped(1)("GESCHLECHT") shouldBe "M"
      mapped(1)("VERBAND") shouldBe ""
      mapped(1)("VEREIN") shouldBe ""
    }
  }
}

