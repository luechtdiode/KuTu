package ch.seidel.kutu.renderer

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import ch.seidel.kutu.domain.{GeraeteRiege, Registration, Wettkampf, toTimeFormat}
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

trait CompetitionsClubsToHtmlRenderer {

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

  private def anmeldeListeProVerein(wettkampf: Wettkampf, vereine: List[Registration], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""

    val d = vereine.map{ registration =>
      s"""<tr class="athletRow"><td>${escaped(registration.vereinname)}</td><td>(${escaped(registration.verband)})</td><td class="large">${escaped(registration.respName)} ${escaped(registration.respVorname)}</td><td>${escaped(registration.mobilephone)}</td><td>${escaped(registration.mail)}</td><td>${
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").format(new java.sql.Timestamp(registration.registrationTime).toLocalDateTime)
      }</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=title><h4>${escaped(wettkampf.easyprint)}</h4></div>
        <div class=programm>Vereinsverantwortliche</br></div>
      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Vereinname</td><td>Verband</td><td>Verantwortlicher</td><td>Mobil-Telefon</td><td>EMail</td><td>Registriert am</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  def toHTMLasClubRegistrationsList(wettkampf: Wettkampf, vereine: List[Registration], logo: File, rowsPerPage: Int = 28): String = {
    val sortedList = vereine.sortBy(_.vereinname)
    val rawpages = for {
      a4seitenmenge <- if(rowsPerPage == 0) sortedList.sliding(sortedList.size, sortedList.size) else sortedList.sliding(rowsPerPage, rowsPerPage)
    }
      yield {
        anmeldeListeProVerein(wettkampf, a4seitenmenge, logo)
      }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}