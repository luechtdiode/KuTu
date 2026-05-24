package ch.seidel.kutu.renderer

import ch.seidel.kutu.data.{WKColValue, WKLeafCol}
import ch.seidel.kutu.domain.{LeafRow, ResultRow, Resultat}
import org.apache.poi.ss.usermodel.{CellType, IndexedColors}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class ScoreToExcelRendererTest extends AnyWordSpec with Matchers {
  class TestScoreToExcelRenderer extends ScoreToExcelRenderer {
    def renderBlockForTest(cols: List[WKLeafCol[ResultRow]], rows: List[ResultRow]): Array[Byte] = {
      try {
        val sheet = workbook.createSheet("Rangliste-Test")
        writeBlockRows(sheet, "Test", cols, rows)
        val out = new ByteArrayOutputStream()
        workbook.write(out)
        out.toByteArray
      }
      finally {
        workbook.close()
      }
    }
  }

  private val sampleRow: ResultRow =
    LeafRow("Test", Resultat(0, 0, 0), Resultat(0, 0, 0), auszeichnung = false, streichwert = false)

  "ScoreToExcelRenderer" should {
    "keep numeric cells numeric when no decoration is present" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Punkte", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("12.30", "12.30", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.NUMERIC
        math.abs(cell.getNumericCellValue - 12.30d) should be < 0.001d
      }
      finally {
        workbook.close()
      }
    }

    "apply best and stroke decorations to Excel font style" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Best", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("*13.45", "13.45", Seq("best"))
        ),
        new WKLeafCol[ResultRow]("Streich", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("(11.10)", "11.10", Seq("stroke"))
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val row = workbook.getSheetAt(0).getRow(2)

        val bestCell = row.getCell(0)
        bestCell.getCellType shouldBe CellType.STRING
        bestCell.getStringCellValue shouldBe "13.45"
        val bestFont = workbook.getFontAt(bestCell.getCellStyle.getFontIndex)
        bestFont.getBold shouldBe true
        bestFont.getStrikeout shouldBe false

        val strokeCell = row.getCell(1)
        strokeCell.getCellType shouldBe CellType.STRING
        strokeCell.getStringCellValue shouldBe "11.10"
        val strokeFont = workbook.getFontAt(strokeCell.getCellStyle.getFontIndex)
        strokeFont.getBold shouldBe false
        strokeFont.getStrikeout shouldBe true
        strokeFont.getColor shouldBe IndexedColors.RED.getIndex
      }
      finally {
        workbook.close()
      }
    }

    "render decorated numeric-looking values as text to preserve markers" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Best", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("13.45", "13.45", Seq("best"))
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.STRING
        cell.getStringCellValue shouldBe "13.45"
        val font = workbook.getFontAt(cell.getCellStyle.getFontIndex)
        font.getBold shouldBe true
      }
      finally {
        workbook.close()
      }
    }

    "merge column and value styles when creating decorated fonts" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Kombi", prefWidth = 60, colspan = 1, styleClass = Seq("stroke", "valuedata"), valueMapper = _ =>
          WKColValue("10.10", "10.10", Seq("best"))
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        val font = workbook.getFontAt(cell.getCellStyle.getFontIndex)
        font.getBold shouldBe true
        font.getStrikeout shouldBe true
        font.getColor shouldBe IndexedColors.RED.getIndex
      }
      finally {
        workbook.close()
      }
    }

    "right-align non-numeric valuedata text cells" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Text", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("n/a", "n/a", Seq.empty)
        )
      )
      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.STRING
        cell.getCellStyle.getAlignment shouldBe org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT
      }
      finally {
        workbook.close()
      }
    }

    "handle empty and null cell values gracefully" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Empty", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("", "", Seq.empty)
        ),
        new WKLeafCol[ResultRow]("Whitespace", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("   ", "   ", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val row = workbook.getSheetAt(0).getRow(2)
        val emptyCell = row.getCell(0)
        emptyCell.getStringCellValue shouldBe ""
        val wsCell = row.getCell(1)
        wsCell.getStringCellValue shouldBe "   "
      }
      finally {
        workbook.close()
      }
    }

    "parse numeric values with German formatting (comma as decimal separator)" in {
      val cols = List(
        new WKLeafCol[ResultRow]("German", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("12,50", "12,50", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.NUMERIC
        math.abs(cell.getNumericCellValue - 12.50d) should be < 0.001d
      }
      finally {
        workbook.close()
      }
    }

    "parse numeric values with thousands separator" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Thousands", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("1'234,50", "1'234,50", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.NUMERIC
        math.abs(cell.getNumericCellValue - 1234.50d) should be < 0.001d
      }
      finally {
        workbook.close()
      }
    }

    "handle specialty notations for award marks (1G, 2B, 3S) as numeric" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Gold", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("1 G", "1 G", Seq.empty)
        ),
        new WKLeafCol[ResultRow]("Silver", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("3 B", "3 B", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val row = workbook.getSheetAt(0).getRow(2)
        val goldCell = row.getCell(0)
        goldCell.getCellType shouldBe CellType.NUMERIC
        math.abs(goldCell.getNumericCellValue - 1.0d) should be < 0.001d
        val silverCell = row.getCell(1)
        silverCell.getCellType shouldBe CellType.NUMERIC
        math.abs(silverCell.getNumericCellValue - 3.0d) should be < 0.001d
      }
      finally {
        workbook.close()
      }
    }

    "preserve non-numeric strings as text" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Text", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("DNS", "DNS", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.STRING
        cell.getStringCellValue shouldBe "DNS"
      }
      finally {
        workbook.close()
      }
    }

    "handle hintdata cells with right alignment" in {
      val cols = List(
        new WKLeafCol[ResultRow]("Hint", prefWidth = 60, colspan = 1, styleClass = Seq("hintdata"), valueMapper = _ =>
          WKColValue("Info", "Info", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellStyle.getAlignment shouldBe org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT
      }
      finally {
        workbook.close()
      }
    }

    "use raw value when no decoration and raw is provided" in {
      val cols = List(
        new WKLeafCol[ResultRow]("WithRaw", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("Display Text", "9.99", Seq.empty)
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        cell.getCellType shouldBe CellType.NUMERIC
        math.abs(cell.getNumericCellValue - 9.99d) should be < 0.001d
      }
      finally {
        workbook.close()
      }
    }

    "apply both best and stroke styles when merged" in {
      val cols = List(
        new WKLeafCol[ResultRow]("BestAndStroke", prefWidth = 60, colspan = 1, styleClass = Seq("valuedata"), valueMapper = _ =>
          WKColValue("*10.00*", "10.00", Seq("best", "stroke"))
        )
      )

      val bytes = new TestScoreToExcelRenderer().renderBlockForTest(cols, List(sampleRow))
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        val cell = workbook.getSheetAt(0).getRow(2).getCell(0)
        val font = workbook.getFontAt(cell.getCellStyle.getFontIndex)
        font.getBold shouldBe true
        font.getStrikeout shouldBe true
        font.getColor shouldBe IndexedColors.RED.getIndex
      }
      finally {
        workbook.close()
      }
    }
  }
}
