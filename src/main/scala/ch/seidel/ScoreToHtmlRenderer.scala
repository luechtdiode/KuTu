package ch.seidel

import javafx.scene.{ control => jfxsc }
import scalafx.scene.control.TableColumn.CellDataFeatures
import scalafx.scene.control.TableView
import scalafx.Includes._
import ch.seidel.domain.GroupRow
import scala.annotation.tailrec

/**
 * @author Roland
 */
trait ScoreToHtmlRenderer {
  protected val title: String

  def toHTML(gs: List[GroupSection]): String = {
    toHTML(gs, "", 0)
  }

  private def toHTML(gs: List[GroupSection], openedTitle: String, level: Int): String = {
    val dummyTableView = new TableView[GroupRow]()
    val gsBlock = new StringBuilder()
    if (level == 0) {
      gsBlock.append(s"""<html lang="de-CH"><head>
          <meta charset="UTF-8" />
          <style type="text/css">
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
            col:nth-child(4) {
              width: 7em;
            }
            tr:nth-child(even) {background: rgba(230, 230, 230, 0.6);}
            /*tr:nth-child(odd) {background: rgba(210, 200, 180, 0.6);}*/
            tr .blockstart {
              border-left: 1px dotted gray;
            }
          </style>
          </head><body><h1>Rangliste</h1><p>${title}</p>\n""")
    }
    for (c <- gs) {
      c match {
        case gl: GroupLeaf =>
          if(openedTitle.startsWith("<h")) {
            val closetag = openedTitle.substring(0, openedTitle.indexOf(">")+1).replace("<", "</")
            gsBlock.append(s"${openedTitle + gl.groupKey.easyprint}${closetag}\n<table width='100%'>\n")
          }
          else {
            gsBlock.append(s"<h${level + 2}>${openedTitle + gl.groupKey.easyprint}</h${level + 2}>\n<table width='100%'>\n")
          }
//          gsBlock.append(s"<h${level + 2}>${gl.groupKey.easyprint}</h${level + 2}>\n<table width='100%'>\n")
          val cols = gl.buildColumns
          cols.foreach { th =>
            if (th.columns.size > 0) {
              cols.foreach { thc =>
                gsBlock.append(s"<col/>")
              }
            }
            else {
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
            if (th.columns.size > 0) {
              gsBlock.append(s"<th $style colspan=${th.columns.size}>${th.getText}</th>")
            }
            else {
              gsBlock.append(s"<th $style rowspan=2>${th.getText}</th>")
            }
          }
          gsBlock.append(s"</tr><tr>\n")
          cols.foreach { th =>
            if (th.columns.size > 0) {
              var first = true
              th.columns.foreach { th =>
                if(first) {
                  gsBlock.append(s"<th class='blockstart'>${th.getText}</th>")
                  first = false;
                }
                else {
                  gsBlock.append(s"<th>${th.getText}</th>")
                }
              }
            }
          }
          gsBlock.append(s"</tr></thead><tbody>\n")
          gl.getTableData.foreach { row =>
            gsBlock.append(s"<tr class='data'>")
            cols.foreach { col =>
              if (col.columns.size == 0) {
                val c = col.asInstanceOf[jfxsc.TableColumn[GroupRow, String]]
                val feature = new CellDataFeatures(dummyTableView, c, row)
                if (c.getStyleClass.contains("hintdata")) {
                  gsBlock.append(s"<td class='data, blockstart'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                }
                else if (c.getStyleClass.contains("data")) {
                  gsBlock.append(s"<td class='data, blockstart'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                }
                else {
                  gsBlock.append(s"<td class='data, blockstart'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                }
              }
              else {
                var first = true
                col.columns.foreach { ccol =>
                  val c = ccol.asInstanceOf[jfxsc.TableColumn[GroupRow, String]]
                  val feature = new CellDataFeatures(dummyTableView, c, row)
                  val style = if(first) {
                                first = false
                                "data, blockstart"
                              }
                              else "data"
                  if (c.getStyleClass.contains("hintdata")) {
                    gsBlock.append(s"<td class='$style'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                  }
                  else if (c.getStyleClass.contains("data")) {
                    gsBlock.append(s"<td class='$style'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                  }
                  else {
                    gsBlock.append(s"<td class='$style'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                  }
                }
              }
            }
            gsBlock.append(s"</tr>\n")
          }
          gsBlock.append(s"</tbody></table>\n")

//        case g: GroupNode => gsBlock.append(s"<h${level + 2}>${g.groupKey.easyprint}</h${level + 2}>\n").append(toHTML(g.next.toList, level + 1))
        case g: GroupNode => gsBlock.append(toHTML(g.next.toList, if(openedTitle.length() > 0) openedTitle + s"${g.groupKey.easyprint}, " else s"<h${level + 2}>${g.groupKey.easyprint}, ", level + 1))
//        case g: GroupNode =>
//          @tailrec
//          def collectGNs(s: GroupSection, acc: String): (String, Iterable[GroupSection]) = {
//            if(s.isInstanceOf[GroupNode]) {
//              val gn = s.asInstanceOf[GroupNode]
//              gn.next match {
//                case ss: GroupNode => collectGNs(ss, acc + ", " + gn.easyprint)
//                case _ => (gn.groupKey.easyprint, gn.next)
//              }
//            }
//            else {
//              (acc, Seq(s))
//            }
//          }
//          val (heads, next) = collectGNs(g, "")
//          gsBlock.append(s"<h${level + 2}>${heads}</h${level + 2}>\n").append(toHTML(next.toList, level + 1))
        case s: GroupSum  =>
          gsBlock.append(s.easyprint)
      }
    }
    if (level == 0) {
      gsBlock.append("</body></html>")
    }
    gsBlock.toString()
  }
}