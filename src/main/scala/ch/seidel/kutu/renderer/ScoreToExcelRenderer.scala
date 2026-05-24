package ch.seidel.kutu.renderer

import ch.seidel.kutu.data.*
import ch.seidel.kutu.domain.{GroupRow, ResultRow, TeamRow}
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFFont, XSSFWorkbook}

import java.io.ByteArrayOutputStream

object ScoreToExcelRenderer {
  def toExcel(gs: List[GroupSection], sortAlphabetically: Boolean, avgOnMultiCompetitions: Boolean): Array[Byte] = {
    new ScoreToExcelRenderer().toExcel(gs, sortAlphabetically, avgOnMultiCompetitions)
  }
}

class ScoreToExcelRenderer {

  private val styleCache = new scala.collection.mutable.HashMap[String, XSSFCellStyle]()
  private[renderer] val workbook = new XSSFWorkbook()
  private val dataFormat = workbook.createDataFormat()

  private val titleFont = workbook.createFont()
  titleFont.setBold(true)

  private val headerFont = workbook.createFont()
  headerFont.setBold(true)

  private val titleStyle = style(titleFont, HorizontalAlignment.LEFT, IndexedColors.GREY_25_PERCENT)
  private val headerStyle = style(titleFont, HorizontalAlignment.LEFT, IndexedColors.LIGHT_CORNFLOWER_BLUE)
  private val subHeaderStyle = style(titleFont, HorizontalAlignment.LEFT, IndexedColors.LIGHT_BLUE)
  private val textStyle = style(workbook.createFont(), HorizontalAlignment.LEFT, IndexedColors.AUTOMATIC)

  private def decoratedCellStyle(
      isNumeric: Boolean,
      formatIndex: Option[Short],
      styleClasses: Seq[String]
  ): XSSFCellStyle = {
    val classes = styleClasses.toSet
    val key = {
      val base = s"${formatIndex.getOrElse(-1)}|${if isNumeric || classes.contains("hintdata") || classes.contains("valuedata") then "R" else "L"}"
      val best = if classes.contains("best") then "|B" else ""
      val stroke = if classes.contains("stroke") then "|S" else ""
      val detail = if classes.contains("detail") then "|D" else ""
      base + best + stroke + detail
    }
    styleCache.getOrElseUpdate(key, {
      val cellStyle = workbook.createCellStyle()
      cellStyle.cloneStyleFrom(textStyle)
      if isNumeric || classes.contains("hintdata") || classes.contains("valuedata") then {
        cellStyle.setAlignment(HorizontalAlignment.RIGHT)
      }
      if classes.contains("detail") then {
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex)
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      }
      formatIndex.foreach(cellStyle.setDataFormat)

      if classes.contains("best") || classes.contains("stroke") then {
        val decoratedFont = workbook.createFont()
        decoratedFont.setBold(classes.contains("best"))
        decoratedFont.setStrikeout(classes.contains("stroke"))
        if classes.contains("stroke") then decoratedFont.setColor(IndexedColors.RED.getIndex)
        cellStyle.setFont(decoratedFont)
      }
      cellStyle
    })
  }

  private def style(font: XSSFFont, alignment: HorizontalAlignment, color: IndexedColors): CellStyle = {
    val s = workbook.createCellStyle()
    s.setFont(font)
    s.setAlignment(alignment)
    s.setFillForegroundColor(color.getIndex)
    s.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    applyTableBorders(s)
    s
  }

  private def flattenCols(cols: Seq[WKCol], parentGroup: Option[String] = None): List[WKLeafCol[ResultRow]] = {
    cols.toList.flatMap {
      case c: WKLeafCol[?] =>
        val label = parentGroup.map(p => s"$p ${c.text}").getOrElse(c.text)
        List(c.copy(text = label).asInstanceOf[WKLeafCol[ResultRow]])
      case gc: WKGroupCol =>
        flattenCols(gc.cols, Some(gc.text))
    }
  }

  private def parseNumeric(value: String) = {
    val trimmed = Option(value).map(_.trim).getOrElse("")
    if trimmed.isEmpty then None
    else {
      // Accept common score notations from DE/EN exports (e.g. 12,55 or 1.234,56).
      val normalizedBase = trimmed.replace("\u00A0", "").replace("'", "")
        .replace("1 G", "1")
        .replace("2 S", "2")
        .replace("3 B", "3")
        .replace(" ", "")
      val normalized = {
        val hasComma = normalizedBase.contains(',')
        val hasDot = normalizedBase.contains('.')
        if hasComma && hasDot then {
          if normalizedBase.lastIndexOf(',') > normalizedBase.lastIndexOf('.') then
            normalizedBase.replace(".", "").replace(',', '.')
          else
            normalizedBase.replace(",", "")
        }
        else if hasComma then normalizedBase.replace(',', '.')
        else normalizedBase
      }
      // analyse normalizedBase, how many digits after comma/dot, to determine the right formatter pattern
      val derivedFormat = {
        val parts = normalized.split("\\.")
        val integerPart = parts.headOption.getOrElse("").length
        val decimalPart = parts.drop(1).headOption.getOrElse("").length
        if integerPart == 4 && decimalPart == 0 then
          "###0" // special case for 4-digit integers (e.g. year) to avoid scientific notation
        else {
          if decimalPart > 0 then
            s"#,##0.${"0" * decimalPart}"
          else "#,##0"
        }
      }
      scala.util.Try(BigDecimal(normalized)).toOption.map(n => (n, dataFormat.getFormat(derivedFormat)))
    }
  }

  private def writeCell(row: org.apache.poi.ss.usermodel.Row, index: Int, value: String, style: CellStyle | Null = null): Cell = {
    val cell = row.createCell(index)
    cell.setCellValue(Option(value).getOrElse(""))
    if style != null then {
      cell.setCellStyle(style)
    }
    cell
  }

  private def writeDataCell(row: Row, index: Int, value: WKColValue, colStyleClasses: Seq[String]) = {
    val cell = row.createCell(index)
    val mergedStyleClasses = (colStyleClasses ++ value.styleClass).distinct
    val renderDecoratedValue = value.styleClass.nonEmpty
    val displayValue = if value.raw.nonEmpty then value.raw else value.text

    parseNumeric(displayValue) match {
      case Some((n, formatIndex)) if !renderDecoratedValue =>
        cell.setCellValue(n.toDouble)
        val cellStyle = decoratedCellStyle(isNumeric = true, Some(formatIndex), mergedStyleClasses)
        cell.setCellStyle(cellStyle)
      case _ =>
        cell.setCellValue(Option(displayValue).getOrElse(""))
        cell.setCellStyle(decoratedCellStyle(isNumeric = false, None, mergedStyleClasses))
    }
    cell
  }

  private def applyTableBorders(style: CellStyle): Unit = {
    style.setBorderTop(BorderStyle.THIN)
    style.setBorderBottom(BorderStyle.THIN)
    style.setBorderLeft(BorderStyle.THIN)
    style.setBorderRight(BorderStyle.THIN)
    style.setVerticalAlignment(VerticalAlignment.CENTER)
  }

  private[renderer] def writeBlockRows[T <: ResultRow](
      sheet: org.apache.poi.ss.usermodel.Sheet,
      title: String,
      cols: List[WKLeafCol[T]],
      rows: List[T]
  ): Unit = {
    if rows.nonEmpty then {
      var rowIndex = 0

      val titleRow = sheet.createRow(rowIndex)
      writeCell(titleRow, 0, "Rangliste", titleStyle)
      writeCell(titleRow, 1, title, titleStyle)
      rowIndex += 1

      val headerRow = sheet.createRow(rowIndex)
      cols.zipWithIndex.foreach { case (col, colIdx) =>
        writeCell(headerRow, colIdx, col.text, headerStyle)
      }
      rowIndex += 1

      rows.foreach { rowData =>
        val row = sheet.createRow(rowIndex)
        cols.zipWithIndex.foreach { case (c, colIdx) =>
          val v = c.valueMapper(rowData)
          writeDataCell(row, colIdx, v, c.styleClass)
        }
        rowIndex += 1
      }

      // Autosize columns for this sheet
      cols.indices.foreach(sheet.autoSizeColumn)

      // Freeze title + first header row
      if rowIndex > 2 then sheet.createFreezePane(0, 2)
    }
  }

  private def sanitizeSheetName(name: String): String = {
    // Excel sheet name rules: max 31 chars, no [ ] : * ? / \
    val sanitized = name.replaceAll("""[\[\]:*?/\\]""", "")
    val trimmed = sanitized.take(31)
    if trimmed.isEmpty then "Rangliste" else trimmed
  }

  private def createSheetName(path: List[String]): String = {
    val fullName = path.filter(_.nonEmpty).mkString(" / ")
    sanitizeSheetName(fullName)
  }

  private def pathText(path: List[String]): String = path.filter(_.nonEmpty).mkString(" / ")

  def toExcel(gs: List[GroupSection], sortAlphabetically: Boolean, avgOnMultiCompetitions: Boolean): Array[Byte] = {
    try {
      def appendSections(items: List[GroupSection], path: List[String]): Unit = {
        items.foreach {
          case gl: GroupLeaf[?] =>
            val title = pathText(path :+ gl.groupKey.capsulatedprint)
            val sheetName = createSheetName(path :+ gl.groupKey.capsulatedprint)
            val sheet = workbook.createSheet(sheetName)
            val cols = flattenCols(gl.buildColumns(avgOnMultiCompetitions)).asInstanceOf[List[WKLeafCol[GroupRow]]]
            val rows = gl.getTableData(sortAlphabetically, avgOnMultiCompetitions)
            writeBlockRows(sheet, title, cols, rows)
          case ts: TeamSums =>
            val teamCols = flattenCols(ts.buildColumns).asInstanceOf[List[WKLeafCol[TeamRow]]]
            ts.getTableData().groupBy(_.team.rulename).foreach { case (ruleName, teamRows) =>
              renderTeams(path, ts, teamCols, ruleName, teamRows)
              renderTeamsWithDetails(path, ts, teamCols, ruleName, teamRows)
            }

          case gn: GroupNode =>
            appendSections(gn.next.toList, path :+ gn.groupKey.capsulatedprint)

          case _ =>
        }
      }

      appendSections(gs, List.empty)


      val out = new ByteArrayOutputStream()
      workbook.write(out)
      out.toByteArray
    }
    finally {
      workbook.close()
    }
  }

  private def makeFullPath(path: List[String], ts: TeamSums, ruleName: String) = {
    path :+ ts.groupKey.capsulatedprint :+ ruleName
  }

  private def renderTeams(path: List[String], ts: TeamSums, teamCols: List[WKLeafCol[TeamRow]], ruleName: String, teamRows: List[TeamRow]): Unit = {
    val fullPath = makeFullPath(path, ts, ruleName)
    val sheetName = createSheetName(fullPath)
    val sheet = workbook.createSheet(sheetName)
    val title = pathText(fullPath)
    writeBlockRows(sheet, title, teamCols, teamRows)
  }

  private def renderTeamsWithDetails(path: List[String], ts: TeamSums, teamCols: List[WKLeafCol[TeamRow]], ruleName: String, teamRows: List[TeamRow]): Unit = {
    val fullPath = makeFullPath(path :+ "Details", ts, ruleName)
    val sheetName = createSheetName(fullPath)
    val sheet = workbook.createSheet(sheetName)
    val title = pathText(fullPath)

    if teamRows.nonEmpty then {
      var rowIndex = 0

      // Title row
      val titleRow = sheet.createRow(rowIndex)
      writeCell(titleRow, 0, "Rangliste", titleStyle)
      writeCell(titleRow, 1, title, titleStyle)
      rowIndex += 1

      // Team header row
      val headerRow = sheet.createRow(rowIndex)
      teamCols.zipWithIndex.foreach { case (col, colIdx) =>
        writeCell(headerRow, colIdx, col.text, headerStyle)
      }
      rowIndex += 1

      // For each team: team row + member rows
      teamRows.foreach { teamRow =>
        // Write team summary row
        val row = sheet.createRow(rowIndex)
        teamCols.zipWithIndex.foreach { case (c, colIdx) =>
          val v = c.valueMapper(teamRow)
          writeDataCell(row, colIdx, v, c.styleClass)
        }
        rowIndex += 1

        // Write member rows
        val teamGroupLeaf = ts.getTeamGroupLeaf(teamRow.team)
        val memberCols = flattenCols(teamGroupLeaf.buildColumns().tail).asInstanceOf[List[WKLeafCol[GroupRow]]]
        val memberData = teamGroupLeaf.getTableData()

        // Member sub-header (optional but helps readability)
        /*val memberHeaderRow = sheet.createRow(rowIndex)
        memberCols.zipWithIndex.foreach { case (col, colIdx) =>
          writeCell(memberHeaderRow, colIdx + 1, col.text, headerStyle) // +1 to indent
        }
        rowIndex += 1
*/
        memberData.foreach { memberRow =>
          val mRow = sheet.createRow(rowIndex)
          memberCols.zipWithIndex.foreach { case (c, colIdx) =>
            val v = c.valueMapper(memberRow)
            writeDataCell(mRow, colIdx + 1, v, c.styleClass :+ "detail") // +1 to indent, add "detail" style class for visual distinction
          }
          rowIndex += 1
        }

        // Blank spacer row between teams
        rowIndex += 1
      }

      teamCols.indices.foreach(sheet.autoSizeColumn)
      if rowIndex > 2 then sheet.createFreezePane(0, 2)
    }
  }
}

