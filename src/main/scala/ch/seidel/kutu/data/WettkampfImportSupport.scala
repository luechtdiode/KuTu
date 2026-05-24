package ch.seidel.kutu.data

import java.io.File
import java.net.URI
import scala.io.{Codec, Source}

object WettkampfImportSupport {
  private val YearPattern = ".*(\\d{4}).*".r

  val CsvDefaultFieldMapping: Map[String, String] = Map(
    "NAME" -> "NAME",
    "VORNAME" -> "VORNAME",
    "JAHRGANG" -> "JAHRGANG",
    "KATEGORIE" -> "KATEGORIE",
    "VERBAND" -> "VERBAND",
    "VEREIN" -> "VEREIN",
    "RLZ_TZ" -> "RLZ_TZ",
    "VERBAND_RLZ" -> "VERBAND_RLZ",
    "GESCHLECHT" -> "GESCHLECHT"
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

  def mapCsvRows(csvHeaders: Seq[String], rows: Seq[String], fieldMapping: Map[String, String]): Seq[Map[String, String]] = {
    val fieldnames = csvHeaders.zipWithIndex.map { case (name, idx) => idx.toString.trim -> name }.toMap
    rows
      .map(r => r.split(";", -1).zipWithIndex.flatMap {
        case (value, idx) => fieldnames.get(idx.toString.trim).map(_ -> value.replace("\"", "").trim)
      }.toMap)
      .map { row =>
        fieldMapping.map { case (logicalField, sourceField) =>
          logicalField -> row.getOrElse(sourceField, "")
        }
      }
  }

  def readCsvFile(filename: URI): Option[(Seq[String], Seq[String])] = {
    val source = Source.fromFile(new File(filename))(using Codec.ISO8859)
    val lines = try source.getLines().toList finally source.close()
    lines match {
      case Nil => None
      case header :: rows => Some((header.split(";").map(_.trim).toSeq, rows))
    }
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
          "VERBAND" -> "",
          "VEREIN" -> "",
          "RLZ_TZ" -> "",
          "VERBAND_RLZ" -> ""
        )
      }
      .toSeq
  }

  private def extractYear(raw: String): String = {
    raw.trim match {
      case YearPattern(year) => year
      case _ => ""
    }
  }
}

