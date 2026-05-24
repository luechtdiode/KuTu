package ch.seidel.kutu.domain

import org.apache.pdfbox.pdmodel.PDDocument
import technology.tabula.detectors.NurminenDetectionAlgorithm
import technology.tabula.extractors.{BasicExtractionAlgorithm, SpreadsheetExtractionAlgorithm}
import technology.tabula.{ObjectExtractor, Table}

import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.util.Using

class ScoreImporter {

  def extractTableData(pdfLocation: Path): Seq[Seq[String]] = {
    Using.resource(PDDocument.load(pdfLocation.toFile)) { document =>
      val extractor = new ObjectExtractor(document)
      val pages = extractor.extract()
      val detectionAlgorithm = new NurminenDetectionAlgorithm()
      val spreadsheetExtractor = new SpreadsheetExtractionAlgorithm()
      val basicExtractor = new BasicExtractionAlgorithm()
      val rows = ListBuffer.empty[Seq[String]]

      while pages.hasNext do {
        val page = pages.next()
        val detectedTableAreas = detectionAlgorithm.detect(page).asScala.toSeq
        val pageTables =
          if detectedTableAreas.nonEmpty then {
            detectedTableAreas.flatMap(area => spreadsheetExtractor.extract(page.getArea(area)).asScala.toSeq)
          } else {
            spreadsheetExtractor.extract(page).asScala.toSeq
          }

        val resolvedTables =
          if pageTables.nonEmpty then pageTables
          else basicExtractor.extract(page).asScala.toSeq

        resolvedTables.foreach { table =>
          rows ++= rowsFromTable(table)
        }
      }

      rows.toSeq
    }
  }

  def mapStructuredRows[A, V, W](tableData: Seq[Seq[String]])(
      mapAthlet: (String, String, String, String) => (V, A),
      mapWertung: (String, String, String) => W): Seq[(A, V, List[W])] = {
    tableData
      .flatMap(parseScoreRow)
      .map { row =>
        val (verein, athlet) = mapAthlet(row.geschlecht, row.name, row.jahrgang, row.verein)
        val wertungen = List(
          mapWertung("Boden", row.bodenD, row.bodenE),
          mapWertung("Pferd Pauschen", row.pferdD.mkString(","), row.pferdE),
          mapWertung("Ring", row.ringeD, row.ringeE),
          mapWertung("Sprung", row.sprungD.mkString(","), row.sprungE),
          mapWertung("Barren", row.barrenD, row.barrenE),
          mapWertung("Reck", row.reckD, row.reckE)
        )
        (athlet, verein, wertungen)
      }
  }

  private[domain] def normalizeRowCells(cells: Seq[String]): Seq[String] = normalizeScoreCells(cells)

  private def rowsFromTable(table: Table): Seq[Seq[String]] = {
    table.getRows.asScala.toSeq
      .map(_.asScala.toSeq.map(cell => cleanCellValue(cell.getText)))
      .map(trimOuterEmptyCells)
      .filter(_.exists(_.nonEmpty))
  }

  private def cleanCellValue(value: String): String = {
    value
      .replace("\n", " ")
      .replaceAll("\\s+", " ")
      .trim
  }

  private def trimOuterEmptyCells(cells: Seq[String]): Seq[String] = {
    cells.dropWhile(_.isEmpty).reverse.dropWhile(_.isEmpty).reverse
  }

  private def parseScoreRow(cells: Seq[String]): Option[StructuredScoreRow] = {
    val cleaned = normalizeScoreCells(cells)
    cleaned.length match {
      case 20 => parseFullDFields(cleaned)
      case 19 => parsePartialDFields(cleaned)
      case 18 => parseSingleDFields(cleaned)
      case _ => None
    }
  }

  private def normalizeScoreCells(cells: Seq[String]): Seq[String] = {
    val trimmed = trimOuterEmptyCells(cells)
    if trimmed.size <= 4 then trimmed
    else {
      val athletePrefix = trimmed.take(4)
      val scoreCells = trimmed.drop(4).flatMap(splitMergedScoreCell)
      athletePrefix ++ scoreCells
    }
  }

  private def splitMergedScoreCell(value: String): Seq[String] = {
    val tokens = value.split("\\s+").toSeq.map(_.trim).filter(_.nonEmpty)
    if tokens.size > 1 then {
      if tokens.forall(isScoreValue) then
        tokens
      else
        tokens.flatMap(splitMergedScoreCell)
    }
    else {
      splitAdjacentDecimalScores(value)
    }
  }

  private def splitAdjacentDecimalScores(value: String): Seq[String] = {
    val trimmed = value.trim
    val pairedScoresPattern23 = "^([0-9]+[0-9]{0,3}[.,][0-9]{2})[\\s]?([1-9]+[0-9]{0,3}[.,]?[0-9]{3})$".r
    val pairedScoresPattern22 = "^([0-9]+[0-9]{0,3}[.,][0-9]{2})[\\s]?([1-9]+[0-9]{0,3}[.,]?[0-9]{2})$".r
    val pairedScoresPattern12 = "^([0-9]+[0-9]{0,3}[.,][0-9]{1})[\\s]?([1-9]+[0-9]{0,3}[.,]?[0-9]{2})$".r
    val pairedScoresPattern01 = "^([0-9]+[0-9]{0,3})[\\s]?([1-9]+[0-9]{0,3}[.,]?[0-9]{1})$".r
    val singleScorePattern = "^([0-9]{1,3}[.,]?[0-9]{0,3})$".r

    trimmed match {
      case singleScorePattern(score) if isScoreValue(score) =>
        Seq(score)
      case pairedScoresPattern23(score1, score2) if isScoreValue(score1) && isScoreValue(score2) =>
        Seq(score1, score2)
      case pairedScoresPattern22(score1, score2) if isScoreValue(score1) && isScoreValue(score2) =>
        Seq(score1, score2)
      case pairedScoresPattern12(score1, score2) if isScoreValue(score1) && isScoreValue(score2) =>
        Seq(score1, score2)
      case pairedScoresPattern01(score1, score2) if isScoreValue(score1) && isScoreValue(score2) =>
        Seq(score1, score2)
      case _ =>
        if (isScoreValue(trimmed)) then Seq(trimmed)
        else Seq()
    }
  }

  private def parseFullDFields(values: Seq[String]): Option[StructuredScoreRow] = {
    if !hasValidAthletPrefix(values) || !matchesFullDFields(values.drop(4)) then None
    else
      Some(StructuredScoreRow(
        geschlecht = "", // Geschlecht is not explicitly provided in the PDF, can be derived from other data if needed
        rang = values(0),
        name = values(1),
        jahrgang = values(2),
        verein = values(3),
        bodenD = values(4),
        bodenE = values(5),
        pferdD = List(values(6), values(7)),
        pferdE = values(8),
        ringeD = values(9),
        ringeE = values(10),
        sprungD = List(values(11), values(12)),
        sprungE = values(13),
        barrenD = values(14),
        barrenE = values(15),
        reckD = values(16),
        reckE = values(17),
        totalD = values(18),
        totalE = values(19)
      ))
  }

  private def parsePartialDFields(values: Seq[String]): Option[StructuredScoreRow] = {
    if !hasValidAthletPrefix(values) || !matchesPartialDFields(values.drop(4)) then None
    else
      Some(StructuredScoreRow(
        geschlecht = "", // Geschlecht is not explicitly provided in the PDF, can be derived from other data if needed
        rang = values(0),
        name = values(1),
        jahrgang = values(2),
        verein = values(3),
        bodenD = values(4),
        bodenE = values(5),
        pferdD = List(values(6)),
        pferdE = values(7),
        ringeD = values(8),
        ringeE = values(9),
        sprungD = List(values(10), values(11)),
        sprungE = values(12),
        barrenD = values(13),
        barrenE = values(14),
        reckD = values(15),
        reckE = values(16),
        totalD = values(17),
        totalE = values(18)
      ))
  }

  private def parseSingleDFields(values: Seq[String]): Option[StructuredScoreRow] = {
    if !hasValidAthletPrefix(values) || !matchesSingleDFields(values.drop(4)) then None
    else
      Some(StructuredScoreRow(
        rang = values(0),
        geschlecht = "", // Geschlecht is not explicitly provided in the PDF, can be derived from other data if needed
        name = values(1),
        jahrgang = values(2),
        verein = values(3),
        bodenD = values(4),
        bodenE = values(5),
        pferdD = List(values(6)),
        pferdE = values(7),
        ringeD = values(8),
        ringeE = values(9),
        sprungD = List(values(10)),
        sprungE = values(11),
        barrenD = values(12),
        barrenE = values(13),
        reckD = values(14),
        reckE = values(15),
        totalD = values(16),
        totalE = values(17)
      ))
  }

  private def hasValidAthletPrefix(values: Seq[String]): Boolean = {
    isRangValue(values.head) && isJahrgang(values(2)) && values(1).nonEmpty && values(3).nonEmpty
  }

  private def isJahrgang(value: String): Boolean = value.matches("^[0-9]{4}$")

  private def isRangValue(value: String): Boolean = value.matches("^[0-9.]+$")

  private def isScoreValue(value: String): Boolean = value.matches("^[0-9]{1,3}([.,][0-9]{1,3})?$")

  private def matchesScoreLength(value: String, min: Int, max: Int): Boolean = {
    value.matches("^[0-9.*]{" + min + "," + max + "}$")
  }

  private def matchesScoreLayout(values: Seq[String], layout: Seq[(Int, Int)]): Boolean = {
    values.size == layout.size && values.zip(layout).forall { case (value, (min, max)) =>
      val bool = matchesScoreLength(value, min, max)
      bool
    }
  }

  private val fullDLayout: Seq[(Int, Int)] = Seq(
    (3, 4), (5, 9),
    (3, 4), (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 4), (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 5), (5, 9)
  )

  private val partialDLayout: Seq[(Int, Int)] = Seq(
    (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 4), (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 4), (5, 9),
    (3, 5), (5, 9)
  )

  private def matchesFullDFields(values: Seq[String]): Boolean = matchesScoreLayout(values, fullDLayout)

  private def matchesPartialDFields(values: Seq[String]): Boolean = matchesScoreLayout(values, partialDLayout)

  private def matchesSingleDFields(values: Seq[String]): Boolean = values.nonEmpty && values.forall(v => v.matches("^[0-9.*]+$"))
}

private case class StructuredScoreRow(
    rang: String,
    geschlecht: String,
    name: String,
    jahrgang: String,
    verein: String,
    bodenD: String,
    bodenE: String,
    pferdD: List[String],
    pferdE: String,
    ringeD: String,
    ringeE: String,
    sprungD: List[String],
    sprungE: String,
    barrenD: String,
    barrenE: String,
    reckD: String,
    reckE: String,
    totalD: String,
    totalE: String
)


