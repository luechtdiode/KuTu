package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain.WertungView
import ch.seidel.kutu.renderer.ServerPrintUtil.*
import org.slf4j.{Logger, LoggerFactory}

import java.io.File

trait BestenListeToHtmlRenderer {
  val logger: Logger = LoggerFactory.getLogger(classOf[BestenListeToHtmlRenderer])
  val intro = """<html>
    <head>
      <meta charset="UTF-8" />
      <style>
        @media print {
          ul {
            page-break-inside: avoid;
          }
        }
        .notenblatt {
          width: 100%:
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
          width: 100%
        }
      </style>
    </head>
    <body><ul><li>
  """

  val outro = """
    </li></ul></body>
    </html>
  """

  private def bestenListe(wertungen: Seq[WertungView], logo: File) = {
    val d = wertungen.map{wertung =>
      s"""<tr class="athletRow"><td>${escaped(wertung.athlet.verein.map(_.name).getOrElse(""))}</td><td class="large">${escaped(wertung.athlet.name)} ${escaped(wertung.athlet.vorname)}</td><td class="large">${wertung.wettkampfdisziplin.disziplin.name}, ${wertung.wettkampfdisziplin.programm.name}</td><td class="totalCol">${wertung.endnote}</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    val logoHtml = if logo.exists() then s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <h4>Besten-Noten, ${escaped(wertungen.head.wettkampf.titel)}</h4>
      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Verein</td><td>Name</td><td>Gerät/Disziplin, Programm/Kategorie</td><td class="totalCol">Note</td></tr>
          $dt
        </table>
      </div>
    </div>
  """
  }

  def toHTMListe(wertungen: Seq[WertungView], logo: File): String = {
    val kandidatenPerKategorie = wertungen.sortBy { k =>
      val krit = f"${k.endnote}%-10s ${k.wettkampfdisziplin.ord}%-10s ${k.athlet.easyprint}%-40s"
      //logger.debug(krit)
      krit
    }
    val rawpages = for
      a4seitenmenge <- kandidatenPerKategorie.sliding(28, 28)
    yield {
      bestenListe(a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}