package ch.seidel.kutu.data

import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}

import java.io.File
import java.net.URI
import scala.io.{Codec, Source}
import scala.util.Using

object WettkampfImportSupport {
  private val YearPattern = ".*(\\d{4}).*".r

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

