package ch.seidel.kutu.renderer

import java.io.File

import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory
import scala.collection.immutable._

trait WettkampfOverviewToHtmlRenderer {
  val logger = LoggerFactory.getLogger(classOf[WettkampfOverviewToHtmlRenderer])
  val intro2 = """<html><body><ul><li>"""
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
              font-size: 10px;
              overflow: hidden;
            }
            td {
              font-size: 10px;
              padding:0.2em;
              overflow: hidden;
              white-space: nowrap;
            }
            td.data {
              //text-align: right
            }
            td.valuedata, td.valuedata.blockstart {
              font-size: 11px;
              text-align: right
            }
            td.hintdata {
              color: rgb(50,100,150);
              font-size: 9px;
              text-align: right
            }
            tbody tr:not(:last-child) > td {
              border-bottom: solid lightgray;
              border-bottom-width: thin;
            }
            tfoot {
              border-top: 1px solid gray;
            }
            tfoot td, tfoot td.tuti.blockstart {
              background-color: rgb(250,250,200);
              font-size: 10px;
              font-weight: bold;
              border-top: 0px
              border-bottom: 0px
              overflow: hidden;
              text-align: center;
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

  val outro = """
    </li></ul></body>
    </html>
  """

  private def blatt(wettkampf: WettkampfView, programme: Seq[(String, Int, Int, Int)], vereinRows: List[(String, Map[String, (Int, Int)], Int, Int)], logo: File) = {
    val rows = vereinRows.map(v =>
      s"""<tr><td class='data'>${v._1}</td>${programme.map(p => s"""${v._2.getOrElse(p._1, (0,0))._2}</td><td class='valuedata'>${v._2.getOrElse(p._1, (0,0))._1}</td>""").mkString("<td class='valuedata blockstart'>", "</td><td class='valuedata blockstart'>", "</td>")}<td class='valuedata blockstart'>${v._4}</td><td class='valuedata'>${v._3}</td><td class='valuedata'>${v._3 + v._4}</td></tr>"""
    ).mkString("")

    val logoHtml = (if (logo.exists) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else s"")
    val auszSchwelle = (if (wettkampf.auszeichnung > 100) {
      wettkampf.auszeichnung / 100d
    } else {
      wettkampf.auszeichnung * 1d
    }) /100
    val auszeichnung = if (wettkampf.auszeichnung > 100) {
      dbl2Str(wettkampf.auszeichnung / 100d) + "%"
    }
    else {
      s"${wettkampf.auszeichnung}%"
    }
    val medallienbedarf = programme.map{p =>
      (p._1, if (p._4 > 0) 1 else 0, if (p._4 > 1) 1 else 0, if (p._4 > 2) 1 else 0, Math.max(Math.floor(p._4*auszSchwelle-3), 0).toInt,
             if (p._3 > 0) 1 else 0, if (p._3 > 1) 1 else 0, if (p._3 > 2) 1 else 0, Math.max(Math.floor(p._3*auszSchwelle-3), 0).toInt)
    }
    s"""<div class=blatt>
      <div class=headline>
        $logoHtml
        <h1>Wettkampf-Übersicht</h1><h2>${wettkampf.easyprint}</h2></div>
      </div>
      <h2>Anmeldungen</h2>
      <div class="showborder">
        <table width="100%">
          <thead>
            <tr class='head'><th>&nbsp;</th>${programme.map(p => s"""${p._1}""").mkString("<th class='blockstart' colspan='2'>", "</th><th class='blockstart' colspan='2'>", "</th>")}<th class='blockstart' colspan="3">Total</th></tr>
            <tr class='head'><th>Verein</th>${programme.map(p => s"""Ti</th><th>Tu""").mkString("<th class='blockstart'>", "</th><th class='blockstart'>", "</th>")}<th class='blockstart'>Ti</th><th>Tu</th><th>Total</th></tr>
          </thead>
          <tbody>
          ${rows}
          </tbody>
          <tfoot>
          <tr><td class="data">Total</td>${programme.map(p => s"""${p._4}</td><td class='valuedata'>${p._3}""").mkString("<td class='valuedata blockstart'>", "</td><td class='valuedata blockstart'>", "</td>")}<td class='valuedata blockstart'>${programme.map(_._4).sum}</td><td class="valuedata">${programme.map(_._3).sum}</td><td class="valuedata">${programme.map(s => s._3 + s._4).sum}</td></tr>
          <tr><td class="data">Total Ti & Tu</td>${programme.map(p => s"""${p._3 + p._4}""").mkString("<td class='tuti blockstart' colspan='2'>", "</td><td class='tuti blockstart' colspan='2'>", "</td>")}<td class='tuti blockstart' colspan='3'>&nbsp;</td></tr>
          </tfoot>
        </table>
      </div>
      <h2>Medallien-Bedarf</h2>
        ${if (wettkampf.auszeichnungendnote > 0) "<em>Auszeichnungs-Mindes-Notenschnitt: ${wettkampf.auszeichnungendnote}</em>" else s"<em>Auszeichnungs-Schwelle: ${auszeichnung}</em>"}      <div class="showborder">
        <table width="100%">
          <thead>
            <tr class='head'><th>&nbsp;</th>${medallienbedarf.map(p => s"""${p._1}""").mkString("<th class='blockstart' colspan='2'>", "</th><th class='blockstart' colspan='2'>", "</th>")}<th class='blockstart'>&nbsp;</th></tr>
            <tr class='head'><th>Auszeichnung</th>${medallienbedarf.map(p => s"""Ti</th><th>Tu""").mkString("<th class='blockstart'>", "</th><th class='blockstart'>", "</th>")}<th class='blockstart'>Total</th></tr>
          </thead>
            <tbody>
              <tr><td class='data'>Goldmedallie</td>${medallienbedarf.map(p => s"""${p._2}</td><td class='valuedata'>${p._6}""").mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")}<td class='blockstart valuedata'>${medallienbedarf.map(p => p._2 + p._6).sum}</td></tr>
              <tr><td class='data'>Silberdallie</td>${medallienbedarf.map(p => s"""${p._3}</td><td class='valuedata'>${p._7}""").mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")}<td class='blockstart valuedata'>${medallienbedarf.map(p => p._3 + p._7).sum}</td></tr>
              <tr><td class='data'>Bronzemedallie</td>${medallienbedarf.map(p => s"""${p._4}</td><td class='valuedata'>${p._8}""").mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")}<td class='blockstart valuedata'>${medallienbedarf.map(p => p._4 + p._8).sum}</td></tr>
              <tr><td class='data'>Ab 4. Rang</td>${medallienbedarf.map(p => s"""${p._5}</td><td class='valuedata'>${p._9}""").mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")}<td class='blockstart valuedata'>${medallienbedarf.map(p => p._5 + p._9).sum}</td></tr>
            </tbody>
        </table>
      </div><br>
      <em>(Ohne Reserven)</em>
    </div>
    """
  }

  val nextSite = "</li></ul><ul><li>\n"
  
  val pageIntro = "<table width='100%'><tr><td>"
  val pageOutro = "</td></tr></table>"
  def toHTML(wettkampf: WettkampfView, stats: List[OverviewStatTuple], logo: File): String = {
    val programme: Seq[(String, Int, Int, Int)] = stats
      .groupBy(t => (t._2, t._3))
      .map(t => (t._1._1, t._1._2, t._2.map(_._4).sum, t._2.map(_._5).sum))
      .toList.sortBy(_._2)
    val vereinRows: List[(String, Map[String, (Int, Int)], Int, Int)] = stats
      .groupBy(_._1)
      .mapValues(_.groupBy(_._2).mapValues(stats => (stats.head._4, stats.head._5)))
      .map(t => (t._1, t._2, t._2.values.map(_._1).sum, t._2.values.map(_._2).sum))
      .toList
      .sortBy(_._1)

    intro + blatt(wettkampf, programme, vereinRows, logo) + outro
  }

}