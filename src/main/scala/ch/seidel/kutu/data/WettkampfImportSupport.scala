package ch.seidel.kutu.data

import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io.File
import java.io.FileOutputStream
import java.net.URI
import scala.io.{Codec, Source}
import scala.util.Using

object WettkampfImportSupport {
  private val YearPattern = ".*(\\d{4}).*".r

  val ImportExcelHeaders: Seq[String] = Seq("NAME", "VORNAME", "JAHRGANG", "GESCHLECHT", "VEREIN", "VERBAND", "KATEGORIE", "TEAM", "RLZ_TZ", "VERBAND_RLZ")

  val CsvDefaultFieldMapping: Map[String, Seq[String]] = Map(
    "NAME" -> Seq("NAME", "NAME_TURNER", "NACHNAME"),
    "VORNAME" -> Seq("VORNAME", "VORNAME_TURNER", "SURNAME"),
    "JAHRGANG" -> Seq("JAHRGANG", "JG", "JG_TURNER", "GEBURTSDATUM"),
    "KATEGORIE" -> Seq("KATEGORIE", "PROGRAMM", "WETTKAMPF_TEIL"),
    "TEAM" -> Seq("TEAM", "MANNSCHAFT"),
    "VERBAND" -> Seq("VERBAND"),
    "VEREIN" -> Seq("VEREIN", "CLUB"),
    "RLZ_TZ" -> Seq("RLZ_TZ", "LEISTUNGSZENTRUM", "POOL"),
    "VERBAND_RLZ" -> Seq("VERBAND_RLZ"),
    "GESCHLECHT" -> Seq("GESCHLECHT", "SEX")
  )

  val DefaultGenderValueMappingRaw: String =
    """M=M
      |F=W
      |W=W
      |m=M
      |f=W
      |w=W
      |male=M
      |female=W""".stripMargin

  def normalizeGender(value: String, mapping: Map[String, String]): String = {
    val normalized = Option(value).map(_.trim).getOrElse("")
    if normalized.isEmpty then "M"
    else {
      val mapped = mapping.get(normalized.toUpperCase)
      mapped.getOrElse(normalized.toUpperCase match {
        case "M" => "M"
        case "W" | "F" => "W"
        case _ => "M"
      })
    }
  }

  private def parseGenderValueMapping(raw: String): Map[String, String] = {
    raw
      .linesIterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .flatMap { line =>
        line.split("=", 2) match {
          case Array(source, target) if source.trim.nonEmpty && target.trim.nonEmpty =>
            val normalizedTarget = target.trim.toUpperCase match {
              case "F" => "W"
              case "M" => "M"
              case "W" => "W"
              case _ => "W"
            }
            Some(source.trim.toUpperCase -> normalizedTarget)
          case _ => None
        }
      }
      .toMap
  }

  def effectiveGenderValueMapping(raw: String): Map[String, String] = {
    val parsedGenderMap = parseGenderValueMapping(raw)
    if parsedGenderMap.nonEmpty then parsedGenderMap else parseGenderValueMapping(DefaultGenderValueMappingRaw)
  }

  def mapCsvRows(csvHeaders: Seq[String], rows: Seq[String], fieldMapping: Map[String, Option[String]]): Seq[Map[String, String]] = {
    mapFieldRows(parseCsvRows(csvHeaders, rows), fieldMapping)
  }

  def mapFieldRows(rows: Seq[Map[String, String]], fieldMapping: Map[String, Option[String]]): Seq[Map[String, String]] = {
    rows.map { row =>
      fieldMapping.map { case (logicalField, sourceField) =>
        logicalField -> sourceField.map(m => row.getOrElse(m, "")).getOrElse("")
      }
    }
  }

  def readCsvFile(filename: URI): Option[(Seq[String], Seq[String])] = {
    val source = Source.fromFile(new File(filename))(using Codec.ISO8859)
    val lines = try source.getLines().toList finally source.close()
    lines match {
      case Nil => None
      case header :: rows => Some((header.split("[;\\t]").map(_.trim).toSeq, rows))
    }
  }

  def readTabularFile(filename: URI): Option[(Seq[String], Seq[Map[String, String]])] = {
    val file = new File(filename)
    if isExcelFile(file) then readExcelFile(file)
    else readCsvFile(filename).map { case (headers, rows) =>
      (headers, parseCsvRows(headers, rows))
    }
  }

  private def createExcelHeaderStyle(workbook: XSSFWorkbook) = {
    val headerFont = workbook.createFont()
    headerFont.setBold(true)

    val headerStyle = workbook.createCellStyle()
    headerStyle.setFont(headerFont)
    headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT)
    headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER)
    headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex)
    headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND)
    headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN)
    headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN)
    headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN)
    headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN)
    headerStyle
  }

  def writeExcelFile(targetFile: File, headers: Seq[String], rows: Seq[Map[String, String]]): Unit = {
    val parent = targetFile.getParentFile
    if parent != null && !parent.exists() then {
      parent.mkdirs()
    }

    Using.resources(new XSSFWorkbook(), new FileOutputStream(targetFile)) { (workbook, outputStream) =>
      val sheet = workbook.createSheet("Teilnehmerliste")
      val headerStyle = createExcelHeaderStyle(workbook)
      val headerRow = sheet.createRow(0)
      headers.zipWithIndex.foreach { case (header, idx) =>
        val cell = headerRow.createCell(idx)
        cell.setCellValue(header)
        cell.setCellStyle(headerStyle)
      }

      rows.zipWithIndex.foreach { case (rowValues, rowIdx) =>
        val row = sheet.createRow(rowIdx + 1)
        headers.zipWithIndex.foreach { case (header, colIdx) =>
          row.createCell(colIdx).setCellValue(rowValues.getOrElse(header, ""))
        }
      }

      // Keep the header visible while scrolling, like ranking Excel exports.
      sheet.createFreezePane(0, 1)
      headers.zipWithIndex.foreach { case (_, colIdx) =>
        sheet.autoSizeColumn(colIdx)
      }
      workbook.write(outputStream)
    }
  }

  private def parseCsvRows(csvHeaders: Seq[String], rows: Seq[String]): Seq[Map[String, String]] = {
    val fieldnames = csvHeaders.zipWithIndex.map { case (name, idx) => idx.toString.trim -> name }.toMap
    rows.map { r =>
      r.split("[;\\t]", -1).zipWithIndex.flatMap {
        case (value, idx) => fieldnames.get(idx.toString.trim).map(_ -> value.replace("\"", "").trim)
      }.toMap
    }
  }

  private def readExcelFile(file: File): Option[(Seq[String], Seq[Map[String, String]])] = {
    Using.resource(WorkbookFactory.create(file)) { workbook =>
      if workbook.getNumberOfSheets < 1 then None
      else {
        val sheet = workbook.getSheetAt(0)
        val formatter = new DataFormatter()
        val headerRow = Option(sheet.getRow(sheet.getFirstRowNum))
        headerRow
          .map { row =>
            val headers = (0 until row.getLastCellNum).map { cellIdx =>
              Option(row.getCell(cellIdx)).map(cell => formatter.formatCellValue(cell)).getOrElse("").trim
            }.toSeq

            val normalizedHeaders = headers.map(_.trim)
            val rows = ((sheet.getFirstRowNum + 1) to sheet.getLastRowNum)
              .flatMap { rowIdx =>
                Option(sheet.getRow(rowIdx)).map { row =>
                  normalizedHeaders.zipWithIndex.map { case (header, cellIdx) =>
                    val value = Option(row.getCell(cellIdx)).map(cell => formatter.formatCellValue(cell)).getOrElse("").trim
                    header -> value
                  }.toMap
                }
              }
              .filter(_.values.exists(_.nonEmpty))

            (normalizedHeaders, rows)
          }
          .filter(_._1.nonEmpty)
      }
    }
  }

  private def isExcelFile(file: File): Boolean = {
    val name = file.getName.toLowerCase
    name.endsWith(".xlsx") || name.endsWith(".xls")
  }

  def mapExcelClipboardRows(tsvContent: String): Seq[Map[String, String]] = {
    tsvContent
      .linesIterator
      .map(_.split("\\t", -1).map(_.trim))
      .filter(_.length > 2)
      .map { fields =>
        Map(
          "NAME" -> fields.headOption.getOrElse(""),
          "VORNAME" -> fields.lift(1).getOrElse(""),
          "JAHRGANG" -> extractYear(fields.lift(2).getOrElse("")),
          "KATEGORIE" -> fields.lift(3).getOrElse(""),
          "GESCHLECHT" -> (if fields.lift(4).exists(_.nonEmpty) then "W" else "M"),
          "TEAM" -> "",
          "VERBAND" -> "",
          "VEREIN" -> "",
          "RLZ_TZ" -> "",
          "VERBAND_RLZ" -> ""
        )
      }
      .toVector
  }

  private def extractYear(raw: String): String = {
    raw.trim match {
      case YearPattern(year) => year
      case _ => ""
    }
  }
}
