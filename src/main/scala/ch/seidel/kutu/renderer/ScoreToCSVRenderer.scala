package ch.seidel.kutu.renderer

import ch.seidel.kutu.data.*
import ch.seidel.kutu.domain.{GroupRow, ResultRow, TeamRow}

object ScoreToCSVRenderer {

  private val sep = ";"

  private def csvEsc(value: String): String = {
    val plain = Option(value).getOrElse("")
    val escaped = plain.replace("\"", "\"\"")
    if escaped.exists(ch => ch == ';' || ch == '\n' || ch == '\r' || ch == '"') then s"\"$escaped\"" else escaped
  }

  private def csvRow(values: Seq[String]): String = values.map(csvEsc).mkString(sep)

  def flattenCols(cols: Seq[WKCol], parentGroup: Option[String] = None): List[WKLeafCol[ResultRow]] = {
    cols.toList.flatMap {
      case c: WKLeafCol[?] =>
        val label = parentGroup.map(p => s"$p ${c.text}").getOrElse(c.text)
        List(c.copy(text = label).asInstanceOf[WKLeafCol[ResultRow]])
      case gc: WKGroupCol =>
        flattenCols(gc.cols, Some(gc.text))
    }
  }

  private def writeBlockRows[T <: ResultRow](
      out: StringBuilder,
      title: String,
      cols: List[WKLeafCol[T]],
      rows: List[T]
  ): Unit = {
    if rows.nonEmpty then {
      out.append(csvRow(Seq("Rangliste", title))).append("\n")
      out.append(csvRow(cols.map(_.text))).append("\n")
      rows.foreach { row =>
        val vals = cols.map { c =>
          val v = c.valueMapper(row)
          if v.raw.nonEmpty then v.raw else v.text
        }
        out.append(csvRow(vals)).append("\n")
      }
      out.append("\n")
    }
  }

  def toCsv(gs: List[GroupSection], sortAlphabetically: Boolean, avgOnMultiCompetitions: Boolean): String = {
    val out = new StringBuilder()
    out.append(s"sep=$sep\n")
    def pathText(path: List[String]): String = path.filter(_.nonEmpty).mkString(" / ")
    def appendSections(items: List[GroupSection], path: List[String]): Unit = {
      items.foreach {
        case gl: GroupLeaf[?] =>
          val title = pathText(path :+ gl.groupKey.capsulatedprint)
          val cols = flattenCols(gl.buildColumns(avgOnMultiCompetitions)).asInstanceOf[List[WKLeafCol[GroupRow]]]
          val rows = gl.getTableData(sortAlphabetically, avgOnMultiCompetitions)
          writeBlockRows(out, title, cols, rows)
        case ts: TeamSums =>
          val base = pathText(path :+ ts.groupKey.capsulatedprint)
          val cols = flattenCols(ts.buildColumns).asInstanceOf[List[WKLeafCol[TeamRow]]]
          ts.getTableData().groupBy(_.team.rulename).foreach { case (ruleName, teamRows) =>
            writeBlockRows(out, pathText(List(base, ruleName)), cols, teamRows.toList)
          }
        case gn: GroupNode =>
          appendSections(gn.next.toList, path :+ gn.groupKey.capsulatedprint)
        case _ =>
      }
    }
    appendSections(gs, List.empty)
    out.toString()
  }
}
