package ch.seidel.kutu.renderer

import ch.seidel.kutu.data.*
import ch.seidel.kutu.domain
import ch.seidel.kutu.domain.{NullObject, ResultRow, TeamRow}
import ch.seidel.kutu.renderer.PrintUtil.{ImageFile, escaped}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File

object ScoreToJsonRenderer {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  
  def toJson(title: String, gs: List[GroupSection], sortAlphabetically: Boolean = false, isAvgOnMultipleCompetitions: Boolean = true, logoFile: File): String = {
    val jsonString = toJsonString(title, gs, "", 0, sortAlphabetically, isAvgOnMultipleCompetitions, logoFile)
    jsonString
  }

  val intro = s"""{
  """
  def firstSite(title: String, logoFile: File): String = intro + (if logoFile.exists then s"""
      "logo":"${logoFile.imageSrcForWebEngine}",
      "title":"${escaped(title)}",
      "scoreblocks":[
      """
    else s"""
      "title":"${escaped(title)}",
      "scoreblocks":[
      """)
        
  val nextSite = "},\n{"
  val outro = "]}"

  def renderListHead(gsBlock: StringBuilder, level: Int, openedTitle: String): gsBlock.type = {
    gsBlock.append("{")
    if openedTitle.startsWith("\"title\":{") then {
      gsBlock.append(s"""$openedTitle"},""")
    }
    else {
      gsBlock.append(s""""title":{"level":"${level + 2}", "text":"$openedTitle"},""")
    }
  }

  def renderListRows[T <: ResultRow](list: List[T], gsBlock: StringBuilder, cols: List[WKCol], ts: Option[TeamSums]): StringBuilder = {
    gsBlock.append("\"rows\":[")
    list.foreach { row =>
      gsBlock.append("{")
      row match {
        case tr: TeamRow =>
          val teamGroupLeaf = ts.get.getTeamGroupLeaf(tr.team)
          val teamGroupCols = teamGroupLeaf.buildColumns().tail
          val allMemberdata = teamGroupLeaf.getTableData()
          renderListRows(allMemberdata, gsBlock, teamGroupCols, None)
        case _ =>
          gsBlock.append(s""""athletID":"${row.athletId.getOrElse(0)}",""")
          gsBlock.append(s""""rows":[],""")
      }
      cols.foreach {
        case ccol: WKLeafCol[?] =>
          val c = ccol.asInstanceOf[WKLeafCol[T]]
          val value = c.valueMapper(row)
          gsBlock.append(s""""${escaped(c.text)}":"${escaped(value.text)}",""")
          gsBlock.append(s""""${escaped(c.text)}-raw":"${value.raw}",""")
          gsBlock.append(s""""${escaped(c.text)}-styles": "${value.styleClass.mkString("[",",","]")}",""")
        case gc: WKGroupCol =>
          gsBlock.append(s""""${escaped(gc.text)}":{""")
          gc.cols.foreach { ccol =>
            val c = ccol.asInstanceOf[WKLeafCol[T]]
            val value = c.valueMapper(row)
            gsBlock.append(s""""${escaped(c.text)}":"${escaped(value.text)}",""")
            gsBlock.append(s""""${escaped(c.text)}-raw":"${value.raw}",""")
            gsBlock.append(s""""${escaped(c.text)}-styles": "${value.styleClass.mkString("[",",","]")}",""")
          }
          gsBlock.deleteCharAt(gsBlock.size - 1)
          gsBlock.append("},")
      }
      gsBlock.deleteCharAt(gsBlock.size - 1)
      gsBlock.append("},")
    }
    if list.nonEmpty then gsBlock.deleteCharAt(gsBlock.size - 1)
    gsBlock.append("],")
  }

  def renderListEnd(gsBlock: StringBuilder): gsBlock.type = {
    gsBlock.deleteCharAt(gsBlock.size - 1)
    gsBlock.append("},")
  }

  private def toJsonString(title: String, gs: List[GroupSection], openedTitle: String, level: Int, sortAlphabetically: Boolean, isAvgOnMultipleCompetitions: Boolean, logoFile: File): String = {
    val gsBlock = new StringBuilder()
    if level == 0 then {
      gsBlock.append(firstSite(title, logoFile))
    }
    for c <- gs do {
      c match {
        case gl: GroupLeaf[?] =>
          renderGroupLeaf(openedTitle, level, sortAlphabetically, isAvgOnMultipleCompetitions, gsBlock, gl)

        case ts: TeamSums =>
          renderTeamLeaf(openedTitle, level,gsBlock, ts)

        case g: GroupNode => gsBlock.append(
            toJsonString(title, g.next.toList, if openedTitle.nonEmpty then
                              openedTitle + s"${escaped(g.groupKey.capsulatedprint)}, "
                            else
                              s""""title":{"level":"${level + 2}", "text":"${escaped(g.groupKey.capsulatedprint)}, """, level + 1, sortAlphabetically, isAvgOnMultipleCompetitions, logoFile))

        case s: GroupSum  =>
          gsBlock.append(s.easyprint)
      }
    }
    if level == 0 then {
      gsBlock.setCharAt(gsBlock.size-1, '\n')
      gsBlock.append(outro)
    }
    gsBlock.toString
  }

  private def renderGroupLeaf(openedTitle: String, level: Int, sortAlphabetically: Boolean, isAvgOnMultipleCompetitions: Boolean, gsBlock: StringBuilder, gl: GroupLeaf[? <: domain.DataObject]): Unit = {
    val cols = gl.buildColumns(isAvgOnMultipleCompetitions)

    val alldata = gl.getTableData(sortAlphabetically, isAvgOnMultipleCompetitions)
    val pagedata = alldata.sliding(alldata.size, alldata.size)
    pagedata.foreach { section =>
      renderListHead(gsBlock, level, openedTitle + escaped(gl.groupKey.capsulatedprint))
      renderListRows(section, gsBlock, cols, None)
      renderListEnd(gsBlock)
    }
  }

  private def renderTeamLeaf(openedTitle: String, level: Int, gsBlock: StringBuilder, gl: TeamSums): Unit = {
    val levelText = if gl.groupKey.isInstanceOf[NullObject] then "" else escaped(gl.groupKey.capsulatedprint)

    for teamRuleGroup <- gl.getTableData().groupBy(_.team.rulename) do {
      val (rulename, alldata) = teamRuleGroup
      val sanitizedRulename = escaped(rulename)
      val pretitle = (openedTitle + levelText).replace("<h2>", "").trim
      val pretitleprint = (openedTitle + levelText).trim.reverse.dropWhile(p => p.equals(',')).reverse
      renderListHead(gsBlock, level, s"$pretitleprint${if pretitle.isEmpty then sanitizedRulename else s": $sanitizedRulename"}")
      val cols = gl.buildColumns
      renderListRows(alldata, gsBlock, cols, Some(gl))
      renderListEnd(gsBlock)
    }
  }

}