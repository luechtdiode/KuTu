package ch.seidel.kutu.renderer

import java.io.File
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain
import ch.seidel.kutu.domain.{Disziplin, GroupRow, NullObject, ResultRow, TeamRow}
import ch.seidel.kutu.renderer.PrintUtil.{ImageFile, escaped}
import org.slf4j.LoggerFactory

object ScoreToJsonRenderer {
  val logger = LoggerFactory.getLogger(this.getClass)
  
  def toJson(title: String, gs: List[GroupSection], sortAlphabetically: Boolean = false, logoFile: File): String = {
    val jsonString = toJsonString(title, gs, "", 0, sortAlphabetically, logoFile)
    jsonString
  }

  val intro = s"""{
  """
  def firstSite(title: String, logoFile: File) = intro + (if (logoFile.exists) s"""
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

  def renderListHead(gsBlock: StringBuilder, level: Int, openedTitle: String) = {
    gsBlock.append("{")
    if (openedTitle.startsWith("\"title\":{")) {
      gsBlock.append(s"""${openedTitle}"},""")
    }
    else {
      gsBlock.append(s""""title":{"level":"${level + 2}", "text":"${openedTitle}"},""")
    }
  }

  def renderListRows[T <: ResultRow](list: List[T], gsBlock: StringBuilder, cols: List[WKCol]) = {
    gsBlock.append("\"rows\":[")
    list.foreach { row =>
      gsBlock.append("{")
      if (row.athletId.nonEmpty) {
        gsBlock.append(s""""athletID": "${row.athletId.getOrElse(0)}",""")
      }
      cols.foreach {
        case ccol: WKLeafCol[_] =>
          val c = ccol.asInstanceOf[WKLeafCol[T]]
          gsBlock.append(s""""${escaped(c.text)}":"${escaped(c.valueMapper(row))}",""")
        case gc: WKGroupCol =>
          gsBlock.append(s""""${escaped(gc.text)}":{""")
          gc.cols.foreach { ccol =>
            val c = ccol.asInstanceOf[WKLeafCol[T]]
            gsBlock.append(s""""${escaped(c.text)}":"${escaped(c.valueMapper(row))}",""")
          }
          gsBlock.deleteCharAt(gsBlock.size - 1)
          gsBlock.append("},")
      }
      gsBlock.deleteCharAt(gsBlock.size - 1)
      gsBlock.append("},")
    }
    gsBlock.deleteCharAt(gsBlock.size - 1)
    gsBlock.append("],")
  }

  def renderListEnd(gsBlock: StringBuilder) = {
    gsBlock.deleteCharAt(gsBlock.size - 1)
    gsBlock.append("},")
  }

  private def toJsonString(title: String, gs: List[GroupSection], openedTitle: String, level: Int, sortAlphabetically: Boolean, logoFile: File): String = {
    val gsBlock = new StringBuilder()
    if (level == 0) {
      gsBlock.append(firstSite(title, logoFile))
    }
    val gsSize = gs.size
    for (c <- gs) {
      val levelText = if (gsSize == 1 && c.groupKey.isInstanceOf[NullObject]) "" else escaped(c.groupKey.capsulatedprint)

      c match {
        case gl: GroupLeaf[_] =>
          renderGroupLeaf(openedTitle, level, sortAlphabetically, gsBlock, gl)

        case ts: TeamSums =>
          renderTeamLeaf(openedTitle, level,gsBlock, levelText, ts)

        case g: GroupNode => gsBlock.append(
            toJsonString(title, g.next.toList, if(openedTitle.nonEmpty)
                              openedTitle + s"${escaped(g.groupKey.capsulatedprint)}, "
                            else
                              s""""title":{"level":"${level + 2}", "text":"${escaped(g.groupKey.capsulatedprint)}, """, level + 1, sortAlphabetically, logoFile))

        case s: GroupSum  =>
          gsBlock.append(s.easyprint)
      }
    }
    if (level == 0) {
      gsBlock.setCharAt(gsBlock.size-1, '\n')
      //gsBlock.deleteCharAt(gsBlock.size-1)
      gsBlock.append(outro)
    }
    gsBlock.toString
  }

  private def renderGroupLeaf(openedTitle: String, level: Int, sortAlphabetically: Boolean, gsBlock: StringBuilder, gl: GroupLeaf[_ <: domain.DataObject]): Unit = {
    val cols = gl.buildColumns

    val alldata = gl.getTableData(sortAlphabetically)
    val pagedata = alldata.sliding(alldata.size, alldata.size)
    pagedata.foreach { section =>
      renderListHead(gsBlock, level, openedTitle + escaped(gl.groupKey.capsulatedprint))
      renderListRows(section, gsBlock, cols)
      renderListEnd(gsBlock)
    }
  }

  private def renderTeamLeaf(openedTitle: String, level: Int, gsBlock: StringBuilder, levelText: String, gl: TeamSums): Unit = {
    for (teamRuleGroup <- gl.getTableData().groupBy(_.team.rulename)) {
      val (rulename, alldata) = teamRuleGroup
      val pretitle = (openedTitle + levelText).replace("<h2>", "").trim
      val pretitleprint = (openedTitle + levelText).trim.reverse.dropWhile(p => p.equals(',')).reverse
      renderListHead(gsBlock, level, escaped(s"$pretitleprint${if (pretitle.isEmpty) rulename else s": $rulename"}"))
      val cols = gl.buildColumns
      renderListRows(alldata, gsBlock, cols)

      /*alldata.foreach { row =>
        val teamGroupLeaf = gl.getTeamGroupLeaf(row.team)
        val teamGroupCols = teamGroupLeaf.buildColumns.tail
        val allMemberdata = teamGroupLeaf.getTableData()
        renderListHead(gsBlock, level+1, row.team.name)
        renderListRows(allMemberdata, gsBlock, teamGroupCols)
        renderListEnd(gsBlock)
      }*/
      renderListEnd(gsBlock)
    }
  }

}