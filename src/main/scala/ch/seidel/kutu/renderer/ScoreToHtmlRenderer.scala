package ch.seidel.kutu.renderer

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{GroupRow, _}
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

trait ScoreToHtmlRenderer {
  val logger = LoggerFactory.getLogger(this.getClass)
  
  protected val title: String
  
  def toHTML(gs: List[GroupSection], athletsPerPage: Int = 0, sortAlphabetically: Boolean = false, diszMap: Map[Long,Map[String,List[Disziplin]]], logoFile: File): String = {
    toHTML(gs, "", 0, athletsPerPage, sortAlphabetically, collectFilterTitles(gs, true), diszMap, logoFile)
  }

  val intro = s"""<html lang="de-CH"><head>
          <meta charset="UTF-8" />
          <style type="text/css">
            @media print {
              body {
                -webkit-print-color-adjust: economy;
              }
              ul {
                page-break-inside: avoid;
              }
            }
            @media only screen {
              body {
                 margin: 15px 15px 15px 20px;
              }
            }
            body {
              font-family: "Arial", "Verdana", sans-serif;
              /*-webkit-print-color-adjust: economy;*/
            }
            h1 {
              font-size: 32px;
            }
            h2 {
              font-size: 15px;
            }
            h3 {
              font-size: 14px;
            }
            h4 {
              font-size: 13px;
            }
            p {
              font-size: 12px;
            }
            table{
              border-collapse:collapse;
              border-spacing:0;
              border: 0px; /*1px solid rgb(50,100,150);*/
              /*border-width: thin;*/
            }
            thead {
              border-bottom: 1px solid gray;            
            }
            th {
              background-color: rgb(250,250,200);
              font-size: 9px;
              overflow: hidden;
            }
            td {
              font-size: 10px;
              padding:0.2em;
              overflow: hidden;
              white-space: nowrap;
            }
            tr:not(:last-child) > td {
              border-bottom: solid lightgray;
              border-bottom-width: thin;
            }
            tr .sf1 {
              font-size: 10px;
            }
            tr .sf2 {
              font-size: 10px;
            }
            td .data {
              text-align: right
            }
            td .valuedata {
         		  font-size: 11px;
              text-align: right
            }
            td .hintdata {
              color: rgb(50,100,150);
              font-size: 9px;
              text-align: right
            }
            col:nth-child(1) {
              width: 3em;
            }
            col:nth-child(2) {
              width: 9em;
            }
            col:nth-child(3) {
              width: 3em;
            }
            col:nth-child(4) {
              width: 9em;
            }
            col:nth-last-child(1) {
              width: 4em;
            }
            col:nth-last-child(2) {
              width: 3em;
            }
            tr:nth-child(even) {background: rgba(230, 230, 230, 0.6);}
            tr .blockstart:not(:first-child) {
              border-left: 1px solid lightgray;
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
              width: 100%;
            }
            .headline {
              display: block;
              border: 0px;
              overflow: auto;
            }
            .logo {
              float: right;
              max-height: 100px;
              border-radius: 5px;
            }
            .showborder {
              padding: 1px;
              border: 1px solid rgb(50,100,150);
              border-radius: 5px;
            }
          </style>          
          </head><body><ul><li>
  """
  val fixFirstPageHeaderLines = 6
  def firstSite(title: String, logoFile: File) = intro + (if (logoFile.exists) s"""
      <div class='headline'>
        <img class='logo' src="${logoFile.imageSrcForWebEngine}" title="Logo"/>
        <h1>Rangliste</h1><h2>${escaped(title)}</h2></div>
      </div>\n""" else s"""
      <div class='headline'>
        <h1>Rangliste</h1><h2>${escaped(title)}</h2></div>
      </div>\n""")
        
  val nextSite = "</li></ul><ul><li>\n"
  val outro = """
    </li></ul></body>
    </html>
  """

  def splitToAutonomPages(html: String, printjob: String => Unit): Unit = {
    val pages = html.split(nextSite)
    pages.foreach{p => 
      val partpage = if(!p.startsWith(intro)) intro + p + outro else p + outro
      printjob(partpage)}
  }
  val firstSiteRendered = new AtomicBoolean(false)

  def collectFilterTitles(gss: Iterable[GroupSection], falsePositives: Boolean, titles: Set[String] = Set.empty): Set[String] = {
    val gssList = gss.toList
    gssList.foldLeft(titles) { (acc, item) =>
      val newTitles = if (!falsePositives && item.groupKey.isInstanceOf[NullObject]) {
        val text = item.groupKey.capsulatedprint
        if (text.length < 200) acc + text else acc
      } else if (!falsePositives && gssList.size == 1) {
        acc + item.groupKey.capsulatedprint
      } else if (falsePositives && gssList.size > 1) {
        acc + item.groupKey.capsulatedprint
      } else acc
      item match {
        case gnx: GroupNode => collectFilterTitles(gnx.next, falsePositives, newTitles)
        case _ => newTitles
      }
    }
  }

  private def toHTML(gs: List[GroupSection], openedTitle: String, level: Int, athletsPerPage: Int, sortAlphabetically: Boolean, falsePositives: Set[String], diszMap: Map[Long,Map[String,List[Disziplin]]], logoFile: File): String = {
    val gsBlock = new StringBuilder()

    if (level == 0) {
      gsBlock.append(firstSite(title, logoFile))
      val subtitles = collectFilterTitles(gs, false) -- falsePositives
      if (subtitles.nonEmpty) {
        gsBlock.append(s"<em>${escaped(subtitles.mkString(", "))}</em><br>")
      }
      firstSiteRendered.set(false)
    }
    val gsSize = gs.size
    for (c <- gs) {
      val levelText = if ((gsSize == 1 && !falsePositives.contains(escaped(c.groupKey.capsulatedprint))) || c.groupKey.isInstanceOf[NullObject]) "" else escaped(c.groupKey.capsulatedprint)
      c match {
        case gl: GroupLeaf =>
          if(openedTitle.startsWith("<h")) {
            val closetag = openedTitle.substring(0, openedTitle.indexOf(">")+1).replace("<", "</")
            gsBlock.append(s"${openedTitle + levelText}${closetag}")
          }
          else {
            gsBlock.append(s"<h${level + 2}>${openedTitle + levelText}</h${level + 2}>")
          }
          val cols = gl.buildColumns
          def renderListHead = {
            gsBlock.append("\n<div class='showborder'><table width='100%'>\n")
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
                  gsBlock.append(s"<th $style colspan=${gc.cols.size}>${escaped(gc.text)}</th>")
                case _ =>
                  gsBlock.append(s"<th $style rowspan=2>${escaped(th.text)}</th>")
              }
            }
            gsBlock.append(s"</tr><tr>\n")
            cols.foreach { th =>
              th match {
                case gc: WKGroupCol =>
                  var first = true
                  gc.cols.foreach { thc =>
                    if(first) {
                      gsBlock.append(s"<th class='blockstart'>${escaped(thc.text)}</th>")
                      first = false
                    }
                    else {
                      gsBlock.append(s"<th>${escaped(thc.text)}</th>")
                    }
                  }
                case _ =>
              }
            }
            gsBlock.append(s"</tr></thead><tbody>\n")
          }

          def renderListEnd = gsBlock.append(s"</tbody></table></div>\n")

          def renderListRows(list: List[GroupRow]) = {
            list.foreach{ row =>
              gsBlock.append(s"<tr>")
              cols.foreach { col =>
                col match {
                  case ccol: WKLeafCol[_] =>
                    val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                    val t = c.valueMapper(row)
                    val smallfont = if(t.length() > 17) " sf2" else if(t.length() > 13) " sf1" else ""
                    if (c.styleClass.contains("hintdata")) {
                      gsBlock.append(s"<td class='data blockstart$smallfont'><div class='hintdata'>${escaped(t)}</div></td>")
                    }
                    else if (c.styleClass.contains("data")) {
                      gsBlock.append(s"<td class='data blockstart$smallfont'>${escaped(t)}</td>")
                    }
                    else {
                      gsBlock.append(s"<td class='data blockstart$smallfont'><div class='valuedata'>${escaped(t)}</div></td>")
                    }
                  case gc: WKGroupCol =>
                    var first = true
                    gc.cols.foreach { ccol =>
                      val c = ccol.asInstanceOf[WKLeafCol[GroupRow]]
                      val style = if(first) {
                          first = false
                          "data blockstart"
                        }
                        else "data"
                      val t = escaped(c.valueMapper(row))
                      if (c.styleClass.contains("hintdata")) {
                        gsBlock.append(s"<td class='$style'><div class='hintdata'>$t</div></td>")
                      }
                      else if (c.styleClass.contains("data")) {
                        gsBlock.append(s"<td class='$style'>$t</td>")
                      }
                      else {
                        gsBlock.append(s"<td class='$style'><div class='valuedata'>$t</div></td>")
                      }
                    }
                }
              }
              gsBlock.append(s"</tr>\n")
            }
          }

          val alldata = gl.getTableData(sortAlphabetically, diszMap)
          val pagedata = if(athletsPerPage == 0) alldata.sliding(alldata.size, alldata.size) 
          else if(firstSiteRendered.get) {
            alldata.sliding(athletsPerPage, athletsPerPage)
          }
          else {
        	  firstSiteRendered.set(true)
            List(alldata.take(athletsPerPage-fixFirstPageHeaderLines)) ++
            alldata.drop(athletsPerPage-fixFirstPageHeaderLines).sliding(athletsPerPage, athletsPerPage)
          }
          pagedata.foreach {section =>
            renderListHead
            renderListRows(section)
            renderListEnd
            gsBlock.append(nextSite)
          }

        case g: GroupNode => gsBlock.append(
            toHTML(g.next.toList,
                if(openedTitle.length() > 0)
                  openedTitle + s"${if (levelText.isEmpty) "" else (levelText + ", ")}"
                else
                  s"<h${level + 2}>${if (levelText.isEmpty) "" else (levelText + ", ")}",
                level + 1, athletsPerPage, sortAlphabetically, falsePositives, diszMap, logoFile))

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