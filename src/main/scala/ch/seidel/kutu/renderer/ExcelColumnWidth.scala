package ch.seidel.kutu.renderer

import org.apache.poi.ss.usermodel.{DataFormatter, Sheet}

import scala.jdk.CollectionConverters.*

/** Headless-safe replacement for Sheet.autoSizeColumn, which needs AWT fonts (fontconfig). */
object ExcelColumnWidth {
  private val formatter = new DataFormatter()
  private val MaxWidthChars = 100

  def autoSize(sheet: Sheet, colIdx: Int): Unit = {
    val longest = sheet.rowIterator().asScala
      .flatMap(row => Option(row.getCell(colIdx)))
      .map(cell => formatter.formatCellValue(cell).split('\n').map(_.length).maxOption.getOrElse(0))
      .maxOption.getOrElse(0)
    sheet.setColumnWidth(colIdx, (math.min(MaxWidthChars, longest + 2) * 256).max(8 * 256))
  }
}
