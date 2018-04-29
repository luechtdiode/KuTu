package ch.seidel.kutu.renderer

import java.io.File

import org.slf4j.LoggerFactory

import PrintUtil.ImageFile
import ch.seidel.kutu.data.GroupLeaf
import ch.seidel.kutu.data.GroupNode
import ch.seidel.kutu.data.GroupSection
import ch.seidel.kutu.data.GroupSum
import ch.seidel.kutu.data.WKGroupCol
import ch.seidel.kutu.data.WKLeafCol
import ch.seidel.kutu.domain.Disziplin
import ch.seidel.kutu.domain.GroupRow

object ScoreToJsonRenderer {
  val logger = LoggerFactory.getLogger(this.getClass)
  
  def toJson(title: String, gs: List[GroupSection], sortAlphabetically: Boolean = false, diszMap: Map[Long,Map[String,List[Disziplin]]], logoFile: File): String = {
    val jsonString = toJsonString(title, gs, "", 0, sortAlphabetically, diszMap, logoFile)
    jsonString
  }

  val intro = s"""{
  """
  def firstSite(title: String, logoFile: File) = intro + (if (logoFile.exists) s"""
      "logo":"${logoFile.imageSrcForWebEngine}",
      "title":"${title}",
      "scoreblocks":[
      """
    else s"""
      "title":"${title}",
      "scoreblocks":[
      """)
        
  val nextSite = "},\n{"
  val outro = "]}"

  private def toJsonString(title: String, gs: List[GroupSection], openedTitle: String, level: Int, sortAlphabetically: Boolean, diszMap: Map[Long,Map[String,List[Disziplin]]], logoFile: File): String = {
    val gsBlock = new StringBuilder()
    if (level == 0) {
      gsBlock.append(firstSite(title, logoFile))
    }
    for (c <- gs) {
      c match {
        case gl: GroupLeaf =>
          val cols = gl.buildColumns
          def renderListHead = {
            gsBlock.append("{")
            if(openedTitle.startsWith("\"title\":{")) {
              gsBlock.append(s"""${openedTitle + gl.groupKey.easyprint}"},""")
            }
            else {
              gsBlock.append(s""""title":{"level":"${level + 2}", "text":"${openedTitle + gl.groupKey.easyprint}"},""")
            }
          }

          def renderListRows(list: List[GroupRow]) = {
            gsBlock.append("\"rows\":[")
            list.foreach{ row =>
              gsBlock.append("{")
              cols.foreach { col =>
                col match {
                  case ccol: WKLeafCol[_] =>
                    val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                    gsBlock.append(s""""${c.text}":"${c.valueMapper(row)}",""")
                  case gc: WKGroupCol =>
                    gsBlock.append(s""""${gc.text}":{""")
                    gc.cols.foreach { ccol =>
                      val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                      gsBlock.append(s""""${c.text}":"${c.valueMapper(row)}",""")
                    }
                    gsBlock.deleteCharAt(gsBlock.size-1)
                    gsBlock.append("},")
                }
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

          val alldata = gl.getTableData(sortAlphabetically, diszMap)
          val pagedata = alldata.sliding(alldata.size, alldata.size) 
          pagedata.foreach {section =>
            renderListHead
            renderListRows(section)
            renderListEnd
          }


        case g: GroupNode => gsBlock.append(
            toJsonString(title, g.next.toList,
                if(openedTitle.length() > 0)
                  openedTitle + s"${g.groupKey.easyprint}, "
                else
                  s""""title":{"level":"${level + 2}", "text":"${g.groupKey.easyprint}, """,
                level + 1, sortAlphabetically, diszMap, logoFile))

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