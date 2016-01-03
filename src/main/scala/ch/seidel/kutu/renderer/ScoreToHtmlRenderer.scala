package ch.seidel.kutu.renderer

import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.GroupRow
import javafx.scene.{ control => jfxsc }
import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.scene.control.TableColumn.CellDataFeatures
import scalafx.scene.control.TableView

trait ScoreToHtmlRenderer {
  protected val title: String

  def toHTML(gs: List[GroupSection], athletsPerPage: Int = 0): String = {
    toHTML(gs, "", 0, athletsPerPage)
  }

  val intro = s"""<html lang="de-CH"><head>
          <meta charset="UTF-8" />
          <style type="text/css">
            @media print {
              ul {
                page-break-inside: avoid;
              }
            }
            body {
              font-family: "Arial", "Verdana", sans-serif;
            }
            table{
                /*table-layout:fixed;*/
                border-collapse:collapse;
                border-spacing:0;
                /*border-style:hidden;*/
                border: 1px solid rgb(50,100,150);
            }
            th {
              background-color: rgb(250,250,200);
              font-size: 9px;
              overflow: hidden;
            }
            /*tr {
              border-bottom: 1px solid rgb(50,100,150);
            }*/
            td {
              font-size: 12px;
              padding:0.2em;
              overflow: hidden;
            }
            td .data {
              text-align: right
            }
            td .valuedata {
              text-align: right
            }
            td .hintdata {
              color: rgb(50,100,150);
              font-size: 9px;
              text-align: right
            }
            col:nth-child(1) {
              width: 2em;
            }
            col:nth-child(2) {
              width: 8em;
            }
            col:nth-child(3) {
              width: 4em;
            }
            col:nth-child(4) {
              width: 10em;
            }
            col:nth-last-child(1) {
              width: 6em;
            }
            col:nth-last-child(2) {
              width: 3em;
            }
            tr:nth-child(even) {background: rgba(230, 230, 230, 0.6);}
            /*tr:nth-child(odd) {background: rgba(210, 200, 180, 0.6);}*/
            tr .blockstart {
              border-left: 1px dotted gray;
            }
            ul {
              margin: 0px;
              padding: 0px;
              border: 0px;
              list-style: none;
              overflow: auto;
            }
            li {
              float: left;
              width: 100%
            }
          </style>
          </head><body><ul><li>
  """
  def firstSite(title: String) = intro + s"<h1>Rangliste</h1><p>${title}</p>\n"
  val nextSite = "</li></ul><ul><li>\n"
  val outro = """
    </li></ul></body>
    </html>
  """

  private def toHTML(gs: List[GroupSection], openedTitle: String, level: Int, athletsPerPage: Int): String = {
    val gsBlock = new StringBuilder()
    if (level == 0) {
      gsBlock.append(firstSite(title))
    }
    var athletCounter = 0
    for (c <- gs) {
      c match {
        case gl: GroupLeaf =>
          if(openedTitle.startsWith("<h")) {
            val closetag = openedTitle.substring(0, openedTitle.indexOf(">")+1).replace("<", "</")
            gsBlock.append(s"${openedTitle + gl.groupKey.easyprint}${closetag}")
          }
          else {
            gsBlock.append(s"<h${level + 2}>${openedTitle + gl.groupKey.easyprint}</h${level + 2}>")
          }
          val cols = gl.buildColumns
          def renderListHead = {
            gsBlock.append("\n<table width='100%'>\n")
            cols.foreach { th => th match {
              case gc: WKGroupCol =>
                gc.cols.foreach { thc =>
                  gsBlock.append(s"<col/>")
                }
              case _ =>
                gsBlock.append(s"<col/>")
              }
            }

            gsBlock.append(s"\n<thead><tr class='head'>\n")
            var first = true
            cols.foreach { th =>
              val style = if(first) {
                first = false
                ""
              }
              else {
                "class='blockstart'"
              }
              th match {
                case gc: WKGroupCol =>
                  gsBlock.append(s"<th $style colspan=${gc.cols.size}>${gc.text}</th>")
                case _ =>
                  gsBlock.append(s"<th $style rowspan=2>${th.text}</th>")
              }
            }
            gsBlock.append(s"</tr><tr>\n")
            cols.foreach { th =>
              th match {
                case gc: WKGroupCol =>
                  var first = true
                  gc.cols.foreach { thc =>
                    if(first) {
                      gsBlock.append(s"<th class='blockstart'>${thc.text}</th>")
                      first = false;
                    }
                    else {
                      gsBlock.append(s"<th>${thc.text}</th>")
                    }
                  }
                case _ =>
              }
            }
            gsBlock.append(s"</tr></thead><tbody>\n")
          }

          def renderListEnd = gsBlock.append(s"</tbody></table>\n")

          def renderListRows(list: List[GroupRow]) = {
            list.foreach{ row =>
              gsBlock.append(s"<tr class='data'>")
              cols.foreach { col =>
                col match {
                  case ccol: WKLeafCol[_] =>
                    val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                    if (c.styleClass.contains("hintdata")) {
                      gsBlock.append(s"<td class='data, blockstart'><div class='hintdata'>${c.valueMapper(row)}</div></td>")
                    }
                    else if (c.styleClass.contains("data")) {
                      gsBlock.append(s"<td class='data, blockstart'>${c.valueMapper(row)}</td>")
                    }
                    else {
                      gsBlock.append(s"<td class='data, blockstart'><div class='valuedata'>${c.valueMapper(row)}</div></td>")
                    }
                  case gc: WKGroupCol =>
                    var first = true
                    gc.cols.foreach { ccol =>
                      val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                      val style = if(first) {
                          first = false
                          "data, blockstart"
                        }
                        else "data"
                      if (c.styleClass.contains("hintdata")) {
                        gsBlock.append(s"<td class='$style'><div class='hintdata'>${c.valueMapper(row)}</div></td>")
                      }
                      else if (c.styleClass.contains("data")) {
                        gsBlock.append(s"<td class='$style'>${c.valueMapper(row)}</td>")
                      }
                      else {
                        gsBlock.append(s"<td class='$style'><div class='valuedata'>${c.valueMapper(row)}</div></td>")
                      }
                    }
                }
              }
              gsBlock.append(s"</tr>\n")
            }
          }

          val alldata = gl.getTableData
          val pagedata = if(athletsPerPage == 0) alldata.sliding(alldata.size, alldata.size) else alldata.sliding(athletsPerPage, athletsPerPage)
          pagedata.foreach {section =>
            renderListHead
            renderListRows(section)
            renderListEnd
            gsBlock.append(nextSite)
          }

        case g: GroupNode => gsBlock.append(toHTML(g.next.toList, if(openedTitle.length() > 0) openedTitle + s"${g.groupKey.easyprint}, " else s"<h${level + 2}>${g.groupKey.easyprint}, ", level + 1, athletsPerPage))
        case s: GroupSum  =>
          gsBlock.append(s.easyprint)
      }
    }
    if (level == 0) {
      gsBlock.append(outro)
    }
    gsBlock.toString()
  }
}