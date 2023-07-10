package ch.seidel.kutu.renderer

import java.io.File

import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Disziplin, GroupRow}
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

  private def toJsonString(title: String, gs: List[GroupSection], openedTitle: String, level: Int, sortAlphabetically: Boolean, logoFile: File): String = {
    val gsBlock = new StringBuilder()
    if (level == 0) {
      gsBlock.append(firstSite(title, logoFile))
    }
    for (c <- gs) {
      c match {
        case gl: GroupLeaf[_] =>
          val cols = gl.buildColumns
          def renderListHead = {
            gsBlock.append("{")
            if(openedTitle.startsWith("\"title\":{")) {
              gsBlock.append(s"""${openedTitle + escaped(gl.groupKey.capsulatedprint)}"},""")
            }
            else {
              gsBlock.append(s""""title":{"level":"${level + 2}", "text":"${openedTitle + escaped(gl.groupKey.capsulatedprint)}"},""")
            }
          }

          def renderListRows(list: List[GroupRow]) = {
            gsBlock.append("\"rows\":[")
            list.foreach{ row =>
              gsBlock.append("{")
              gsBlock.append(s""""athletID": "${row.athlet.id}",""")
              cols.foreach {
                case ccol: WKLeafCol[_] =>
                  val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                  gsBlock.append(s""""${escaped(c.text)}":"${escaped(c.valueMapper(row))}",""")
                case gc: WKGroupCol =>
                  gsBlock.append(s""""${escaped(gc.text)}":{""")
                  gc.cols.foreach { ccol =>
                    val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                    gsBlock.append(s""""${escaped(c.text)}":"${escaped(c.valueMapper(row))}",""")
                  }
                  gsBlock.deleteCharAt(gsBlock.size - 1)
                  gsBlock.append("},")
              }
              gsBlock.deleteCharAt(gsBlock.size-1)
              gsBlock.append("},")
            }
            gsBlock.deleteCharAt(gsBlock.size-1)
            gsBlock.append("],")
          }
          
          def renderListEnd = {
            gsBlock.deleteCharAt(gsBlock.size-1)
            gsBlock.append("},")
          }

          val alldata = gl.getTableData(sortAlphabetically)
          val pagedata = alldata.sliding(alldata.size, alldata.size) 
          pagedata.foreach {section =>
            renderListHead
            renderListRows(section)
            renderListEnd
          }
        case g: GroupNode => gsBlock.append(
            toJsonString(title, g.next.toList, if(openedTitle.length() > 0)
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
}