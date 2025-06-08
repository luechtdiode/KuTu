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
  
  def toHTML(gs: List[GroupSection], athletsPerPage: Int = 0, sortAlphabetically: Boolean = false, isAvgOnMultipleCompetitions: Boolean = true, logoFile: File): String = {
    toHTML(gs, "", 0, athletsPerPage, sortAlphabetically, isAvgOnMultipleCompetitions, collectFilterTitles(gs, true), logoFile)
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
            td .stroke {
              color: rgb(150,50,50);
              text-decoration: line-through;
            }
            td .best::before {
              content: '* ';
            }
            td .hintdata {
              color: rgb(50,100,150);
              font-size: 9px;
              text-align: right
            }
            .heading {
              font-size: 12px;
              text-align: left;
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
            footer {
              em {
                color: grey;
              }
              a {
                color: cornflowerblue;
              }
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
    </li></ul><footer><p><em>Erstellt mit der Opensource Software <a href="https://github.com/luechtdiode/KuTu">KuTu Wettkampf-App</a></em></p></footer></body>
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

  private def toHTML(gs: List[GroupSection], openedTitle: String, level: Int, athletsPerPage: Int, sortAlphabetically: Boolean, isAvgOnMultipleCompetitions: Boolean, falsePositives: Set[String], logoFile: File): String = {
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
        case gl: GroupLeaf[_] =>
          renderGroupLeaf(openedTitle, level, athletsPerPage, sortAlphabetically, isAvgOnMultipleCompetitions, gsBlock, levelText, gl)

        case ts: TeamSums =>
          renderTeamLeaf(openedTitle, level, athletsPerPage, gsBlock, levelText, ts)

        case g: GroupNode => gsBlock.append(
            toHTML(g.next.toList,
                if(openedTitle.length() > 0)
                  openedTitle + s"${if (levelText.isEmpty) "" else (levelText + ", ")}"
                else
                  s"<h${level + 2}>${if (levelText.isEmpty) "" else (levelText + ", ")}",
                level + 1, athletsPerPage, sortAlphabetically, isAvgOnMultipleCompetitions, falsePositives, logoFile))

        case s: GroupSection  =>
          gsBlock.append(s.easyprint)
      }
    }
    if (level == 0) {
      gsBlock.append(outro)
    }
    gsBlock.toString()
  }

  private def renderListHead(gsBlock: StringBuilder, cols: List[WKCol], framed: Boolean = true) = {
    if (framed) {
      gsBlock.append("\n<div class='showborder'><table width='100%'>\n")
    } else {
      gsBlock.append("\n<table width='100%'>\n")
    }
    cols.foreach { th =>
      th match {
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
      val style = if (first) {
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
            if (first) {
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

  private def renderListRows[T <: ResultRow](list: List[T], gsBlock: StringBuilder, cols: List[WKCol]) = {
    list.foreach { row =>
      gsBlock.append(s"<tr>")
      cols.foreach { col =>
        col match {
          case ccol: WKLeafCol[_] =>
            val c = ccol.asInstanceOf[WKLeafCol[T]]
            val value = c.valueMapper(row)
            val t = escaped(value.raw)
            val h = escaped(value.text)
            val smallfont = if (t.length() > 17) " sf2" else if (t.length() > 13) " sf1" else ""
            if (c.styleClass.contains("hintdata")) {
              gsBlock.append(s"<td class='data blockstart$smallfont'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
            }
            else if (c.styleClass.contains("heading")) {
              gsBlock.append(s"<td class='heading blockstart'>$h</td>")
            }
            else if (c.styleClass.contains("data")) {
              gsBlock.append(s"<td class='data blockstart$smallfont'>$h</td>")
            }
            else {
              gsBlock.append(s"<td class='data blockstart$smallfont'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
            }
          case gc: WKGroupCol =>
            var first = true
            gc.cols.foreach { ccol =>
              val c = ccol.asInstanceOf[WKLeafCol[T]]
              val style = if (first) {
                first = false
                "data blockstart"
              }
              else "data"
              val value = c.valueMapper(row)
              val t = escaped(value.raw)
              val h = escaped(value.text)
              if (c.styleClass.contains("hintdata")) {
                gsBlock.append(s"<td class='$style'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
              }
              else if (c.styleClass.contains("data")) {
                gsBlock.append(s"<td class='$style'>$h</td>")
              }
              else if (c.styleClass.contains("heading")) {
                gsBlock.append(s"<td class='$style heading'>$h</td>")
                //gsBlock.append(s"<td class='$style'><div class='heading'>$t</div></td>")
              }
              else {
                gsBlock.append(s"<td class='$style'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
                //gsBlock.append(s"<td class='$style'><div class='valuedata'>$t</div></td>")
              }
            }
        }
      }
      gsBlock.append(s"</tr>\n")
    }
  }

  private def renderListEnd(gsBlock: StringBuilder) = gsBlock.append(s"</tbody></table></div>\n")
  private def renderGroupLeaf(openedTitle: String, level: Int, athletsPerPage: Int, sortAlphabetically: Boolean, isAvgOnMultipleCompetitions: Boolean = true, gsBlock: StringBuilder, levelText: String, gl: GroupLeaf[_]): Unit = {
    val pretitleprint = (openedTitle + levelText).trim.reverse.dropWhile(p => p.equals(',')).reverse
    if (openedTitle.startsWith("<h")) {
      val closetag = openedTitle.substring(0, openedTitle.indexOf(">") + 1).replace("<", "</")
      gsBlock.append(s"$pretitleprint$closetag")
    }
    else {
      gsBlock.append(s"<h${level + 2}>$pretitleprint</h${level + 2}>")
    }
    val cols = gl.buildColumns(isAvgOnMultipleCompetitions)

    val alldata = gl.getTableData(sortAlphabetically, isAvgOnMultipleCompetitions)
    val pagedata = if (athletsPerPage == 0) alldata.sliding(alldata.size, alldata.size)
    else if (firstSiteRendered.get) {
      alldata.sliding(athletsPerPage, athletsPerPage)
    }
    else {
      firstSiteRendered.set(true)
      List(alldata.take(athletsPerPage - fixFirstPageHeaderLines)) ++
        alldata.drop(athletsPerPage - fixFirstPageHeaderLines).sliding(athletsPerPage, athletsPerPage)
    }
    pagedata.foreach { section =>
      renderListHead(gsBlock, cols)
      renderListRows(section, gsBlock, cols)
      renderListEnd(gsBlock)
      gsBlock.append(nextSite)
    }
  }

  private def countTableColumns(cols: List[WKCol]): Int = {
    cols.map{
      case gc: WKGroupCol =>
        gc.cols.size
      case _ =>
        1
    }.sum
  }
  private def renderTeamLeaf(openedTitle: String, level: Int, athletsPerPage: Int, gsBlock: StringBuilder, levelText: String, gl: TeamSums): Unit = {
    for (teamRuleGroup <- gl.getTableData().groupBy(_.team.rulename)) {
      val (rulename, alldata) = teamRuleGroup
      val blockSize = alldata.map(_.team.blockrows).max
      val pretitle = (openedTitle + levelText).replace("<h2>", "").trim
      val pretitleprint = (openedTitle + levelText).trim.reverse.dropWhile(p => p.equals(',')).reverse
      if (openedTitle.startsWith("<h")) {
        val closetag = openedTitle.substring(0, openedTitle.indexOf(">") + 1).replace("<", "</")
        gsBlock.append(s"$pretitleprint${if (pretitle.isEmpty) rulename else s": $rulename"} ${closetag}")
      }
      else {
        gsBlock.append(s"<h${level + 2}>$pretitleprint${if (pretitle.isEmpty) rulename else s": $rulename"}</h${level + 2}>")
      }
      val cols = gl.buildColumns

      def renderTeamHead = {
        gsBlock.append("\n<div class='showborder'><table width='100%'>\n")
        cols.foreach { th =>
          th match {
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
          val style = if (first) {
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
                if (first) {
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

      def renderTeamEnd = gsBlock.append(s"</tbody></table></div>\n")

      def renderTeamRows(list: List[TeamRow]) = {
        list.foreach { row =>
          val teamGroupLeaf = gl.getTeamGroupLeaf(row.team)
          val teamGroupCols = teamGroupLeaf.buildColumns().tail//.dropRight(2)
          val allMemberdata = teamGroupLeaf.getTableData()
          var rowspans = if (allMemberdata.size % 2 == 0) {
            allMemberdata.size + 1
          } else {
            allMemberdata.size + 2
          }
          def getRowSpans: Int = {
            val spans = rowspans
            rowspans = 1
            spans
          }
          gsBlock.append(s"<tr>")
          cols.foreach { col =>
            col match {
              case ccol: WKLeafCol[_] if (ccol.colspan > 0)=>
                val c = ccol.asInstanceOf[WKLeafCol[TeamRow]]
                val value = c.valueMapper(row)
                val t = escaped(value.raw)
                val h = escaped(value.text)
                val smallfont = if (t.length() > 17) " sf2" else if (t.length() > 13) " sf1" else ""
                if (c.styleClass.contains("hintdata")) {
                  gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='data blockstart$smallfont'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
                }
                else if (c.styleClass.contains("data")) {
                  gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='data blockstart$smallfont'>$h</td>")
                }
                else if (c.styleClass.contains("heading")) {
                  gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='heading blockstart'>$h</td>")
                }
                else {
                  gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='data blockstart$smallfont'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
                }
              case gc: WKGroupCol if (gc.colspan > 0) =>
                var first = true
                gc.cols.foreach { ccol =>
                  val c = ccol.asInstanceOf[WKLeafCol[TeamRow]]
                  val style = if (first) {
                    first = false
                    "data blockstart"
                  }
                  else "data"
                  val value = c.valueMapper(row)
                  val t = escaped(value.raw)
                  val h = escaped(value.text)
                  if (c.styleClass.contains("hintdata")) {
                    gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='$style'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
                  }
                  else if (c.styleClass.contains("data")) {
                    gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='$style'>$h</td>")
                  }
                  else if (c.styleClass.contains("heading")) {
                    gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='$style heading'>$h</td>")
                  }
                  else {
                    gsBlock.append(s"<td rowspan=$getRowSpans colspan=${c.colspan} class='$style'><div class=${(c.styleClass ++ value.styleClass).mkString("'", " ", "'")}>$t</div></td>")
                  }
                }

              case _ =>
            }
          }
          gsBlock.append(s"</tr>\n")
          //gsBlock.append(s"<tr><td></td><td colspan=${countTableColumns(cols)-3}>")
          //renderListHead(gsBlock, teamGroupCols, false)
          //gsBlock.append(s"<tr><td rowspan=${allMemberdata.size + 1}></td><td colspan=${countTableColumns(cols)-3}>")
          renderListRows(allMemberdata, gsBlock, teamGroupCols)
          if (allMemberdata.size % 2 != 0) {
            gsBlock.append(s"<tr><td colspan=${countTableColumns(teamGroupCols)}>&nbsp;</td></tr>\n")
          }
          //renderListEnd(gsBlock)
          //gsBlock.append("</td></tr>\n")
          gsBlock.append(s"<tr><td colspan=${countTableColumns(cols)}></td></tr>\n")
        }
      }

      val teamsPerPage = athletsPerPage / (blockSize + 2) // 6 lines per team, 1 for the team, 4 for the members, 1 for the spacer
      val pagedata = if (athletsPerPage == 0) alldata.sliding(alldata.size, alldata.size)
      else if (firstSiteRendered.get) {
        alldata.sliding(teamsPerPage, teamsPerPage)
      }
      else {
        firstSiteRendered.set(true)
        val headerOffset = fixFirstPageHeaderLines / (blockSize + 1)
        val teamsFirstSite = teamsPerPage - headerOffset
        List(alldata.take(teamsFirstSite)) ++
          alldata.drop(teamsFirstSite).sliding(teamsPerPage, teamsPerPage)
      }
      pagedata.foreach { section =>
        renderTeamHead
        renderTeamRows(section)
        renderTeamEnd
        gsBlock.append(nextSite)
      }
    }
  }
}