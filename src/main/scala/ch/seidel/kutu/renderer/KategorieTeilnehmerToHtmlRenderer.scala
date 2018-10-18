package ch.seidel.kutu.renderer

import java.io.File
import PrintUtil._
import org.slf4j.LoggerFactory

trait KategorieTeilnehmerToHtmlRenderer {
  val logger = LoggerFactory.getLogger(classOf[KategorieTeilnehmerToHtmlRenderer])
  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String,
                      name: String, vorname: String, jahrgang: String, verein: String, riege: String, durchgang: String, start: String, diszipline: Seq[String])

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

  private def anmeldeListe(kategorie: String, kandidaten: Seq[Kandidat], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""
      
    val d = kandidaten.map{kandidat =>
      s"""<tr class="athletRow"><td>${kandidat.verein}</td><td class="large">${kandidat.name} ${kandidat.vorname} (${kandidat.jahrgang})</td><td>${kandidat.durchgang}</td><td>${kandidat.start}</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=programm>${kategorie}</br></div>
        <h4>${kandidaten.head.wettkampfTitel}</h4>
      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Verein</td><td>Name</td><td>Einteilung</td><td>Start</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  def toHTMLasKategorienListe(kandidaten: Seq[Kandidat], logo: File): String = {
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${k.verein}%-40s ${k.name}%-40s ${k.vorname}%-40s"
      //logger.debug(krit)
      krit
    }.groupBy(k => k.programm)
    val rawpages = for {
      kategorie <- kandidatenPerKategorie.keys.toList.sorted
      a4seitenmenge <- kandidatenPerKategorie(kategorie).sliding(28, 28)
    }
    yield {
      anmeldeListe(kategorie, a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}