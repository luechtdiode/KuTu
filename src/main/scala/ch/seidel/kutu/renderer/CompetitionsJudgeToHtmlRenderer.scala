package ch.seidel.kutu.renderer

import java.io.File
import java.time.format.DateTimeFormatter

import ch.seidel.kutu.domain.{JudgeRegistration, Registration, Verein, Wettkampf}
import ch.seidel.kutu.renderer.PrintUtil._

trait CompetitionsJudgeToHtmlRenderer {

  private val intro = """<html>
    <head>
      <meta charset="UTF-8" />
      <style type="text/css">
        @media print {
          ul {
            page-break-inside: avoid;
          }
        }
        .notenblatt {
          display: block;
          padding: 15px;
          padding-left: 40px;
          margin-top: 5px;
          margin-left: 5px;
        }
        .headline {
          display: block;
          border: 0px;
          overflow: auto;
        }
        .logo {
          float: left;
          height: 100px;
          border-radius: 5px;
          padding-right: 15px;
        }
        .title {
          float: left;
        }
        .programm {
          float: right;
          font-size: 24px;
          font-weight: 600;
        }
        .showborder {
          margin-top: 10px;
          padding: 5px;
          border: 1px solid black;
          border-radius: 5px;
        }
        .athletRow {
          border-bottom: 1px solid #ddd;
        }
        .totalRow {
          border-bottom: 1px solid #000;
        }
        .heavyRow {
          font-weight: bolder;
        }
        .totalCol {
          border-left: 1px solid #ddd;
        }
        .large {
          padding: 4px;
          padding-bottom: 6px;
        }
        body {
          font-family: "Arial", "Verdana", sans-serif;
        }
        h1 {
          font-size: 75%;
        }
        table {
          border-collapse:collapse;
          border-spacing:0;
        }
        tr {
          font-size: 12px;
          overflow: hidden;
        }
        td {
          padding: 2px;
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
      </style>
    </head>
    <body><ul><li>
  """

  private val outro = """
    </li></ul></body>
    </html>
  """

  private def anmeldeListeProVerein(wettkampf: Wettkampf, verein: Registration, anmeldungenCnt: Int, wrs: Seq[JudgeRegistration], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""

    val d = wrs.map{ registration =>
      s"""<tr class="athletRow"><td class="large">${registration.name} ${registration.vorname}</td><td>${registration.mobilephone}</td><td>${registration.mail}</td><td class="totalCol">${
        registration.comment.split("\n").toList.map(xml.Utility.escape(_)).mkString("", "<br>", "")}</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=title><h4>${wettkampf.easyprint}</h4></div>
        <div class=programm>${anmeldungenCnt} Wertungsrichter, gestellt durch ${verein.toVerein.easyprint}</br></div>
      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Wertungsrichter/-in</td><td>Mobil-Telefon</td><td>EMail</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  def toHTMLasJudgeRegistrationsList(wettkampf: Wettkampf, vereine: Map[Registration,Seq[JudgeRegistration]], logo: File, rowsPerPage: Int = 28): String = {
    val sortedList = vereine.keys.toList.sortBy(_.vereinname)
    val rawpages = for {
      verein <- sortedList
      a4seitenmenge <- if(rowsPerPage == 0) vereine(verein).sliding(vereine(verein).size, vereine(verein).size) else vereine(verein).sliding(rowsPerPage, rowsPerPage)
    } yield {
      anmeldeListeProVerein(wettkampf, verein, vereine(verein).size, a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}