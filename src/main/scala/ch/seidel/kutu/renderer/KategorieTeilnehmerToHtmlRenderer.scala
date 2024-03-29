package ch.seidel.kutu.renderer

import java.io.File
import ch.seidel.kutu.domain.GeraeteRiege
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.{Logger}

trait KategorieTeilnehmerToHtmlRenderer {
  val logger: Logger// = LoggerFactory.getLogger(classOf[KategorieTeilnehmerToHtmlRenderer])
  val intro = """<html>
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

  val outro = """
    </li></ul></body>
    </html>
  """

  private def anmeldeListeProKategorie(kategorie: String, kandidaten: Seq[Kandidat], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""

    val d = kandidaten.map{kandidat =>
      if (kandidat.team.nonEmpty) {
        s"""<tr class="athletRow"><td>${escaped(kandidat.team)}</td><td class="large">${escaped(kandidat.name)} ${escaped(kandidat.vorname)} (${escaped(kandidat.jahrgang)})</td><td>${escaped(kandidat.durchgang)}</td><td>${escaped(kandidat.start)}</td><td class="totalCol">&nbsp;</td></tr>"""
      } else {
        s"""<tr class="athletRow"><td>${escaped(kandidat.verein)}</td><td class="large">${escaped(kandidat.name)} ${escaped(kandidat.vorname)} (${escaped(kandidat.jahrgang)})</td><td>${escaped(kandidat.durchgang)}</td><td>${escaped(kandidat.start)}</td><td class="totalCol">&nbsp;</td></tr>"""
      }
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=title><h4>${escaped(kandidaten.head.wettkampfTitel)}</h4></div>
        <div class=programm>${escaped(kategorie)}</br></div>

      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>${if (kandidaten.head.team.nonEmpty) "Team" else "Verein"}</td><td>Name</td><td>Einteilung</td><td>Start</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  private def anmeldeListeProVerein(verein: String, kandidaten: Seq[Kandidat], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""

    val d = kandidaten.map{kandidat =>
        s"""<tr class="athletRow"><td>${escaped(kandidat.programm)}</td><td class="large">${escaped(kandidat.name)} ${escaped(kandidat.vorname)} (${escaped(kandidat.jahrgang)})</td><td>${escaped(kandidat.durchgang)}</td><td>${escaped(kandidat.start)}</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=title><h4>${escaped(kandidaten.head.wettkampfTitel)}</h4></div>
        <div class=programm>${escaped(verein)}</br></div>

      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td></td><td>Name</td><td>Einteilung</td><td>Start</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  private def anmeldeListeProDurchgangVerein(durchgang: String, kandidaten: Seq[Kandidat], logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""

    val d = kandidaten.map{kandidat =>
      if (kandidat.team.nonEmpty) {
        s"""<tr class="athletRow"><td>${escaped(kandidat.team)}</td><td class="large">${escaped(kandidat.name)} ${escaped(kandidat.vorname)} (${escaped(kandidat.jahrgang)})</td><td>${escaped(kandidat.programm)}</td><td>${escaped(kandidat.start)}</td><td class="totalCol">&nbsp;</td></tr>"""
      } else {
        s"""<tr class="athletRow"><td>${escaped(kandidat.verein)}</td><td class="large">${escaped(kandidat.name)} ${escaped(kandidat.vorname)} (${escaped(kandidat.jahrgang)})</td><td>${escaped(kandidat.programm)}</td><td>${escaped(kandidat.start)}</td><td class="totalCol">&nbsp;</td></tr>"""
      }
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class=title><h4>${escaped(kandidaten.head.wettkampfTitel)}</h4></div>
        <div class=programm>${escaped(durchgang)}</br></div>

      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>${if (kandidaten.head.team.nonEmpty) "Team" else "Verein"}</td><td>Name</td><td>Einteilung</td><td>Start</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  def riegenToKategorienListeAsHTML(riegen: Seq[GeraeteRiege], logo: File): String = {
    toHTMLasKategorienListe(Kandidaten(riegen), logo, 0)
  }
  def riegenToDurchgangListeAsHTML(riegen: Seq[GeraeteRiege], logo: File): String = {
    toHTMLasDurchgangListe(Kandidaten(riegen), logo, 0)
  }

  def riegenToVereinListeAsHTML(riegen: Seq[GeraeteRiege], logo: File): String = {
    toHTMLasVereinsListe(Kandidaten(riegen), logo, 0)
  }

  def toHTMLasKategorienListe(kandidaten: Seq[Kandidat], logo: File, rowsPerPage: Int = 28): String = {
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = if (k.team.nonEmpty) f"${k.team}%-40s ${k.name}%-40s ${k.vorname}%-40s" else f"${k.verein}%-40s ${k.name}%-40s ${k.vorname}%-40s"
      krit
    }.groupBy(k => k.programm)
    val rawpages = for {
      kategorie <- kandidatenPerKategorie.keys.toList.sorted
      a4seitenmenge <- if(rowsPerPage == 0) kandidatenPerKategorie(kategorie).sliding(kandidatenPerKategorie(kategorie).size, kandidatenPerKategorie(kategorie).size) else kandidatenPerKategorie(kategorie).sliding(rowsPerPage, rowsPerPage)
    }
    yield {
      anmeldeListeProKategorie(kategorie, a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }

  def toHTMLasDurchgangListe(kandidaten: Seq[Kandidat], logo: File, rowsPerPage: Int = 28): String = {
    val kandidatenPerDurchgang = kandidaten.sortBy { k =>
      val krit = if (k.team.nonEmpty) f"${k.team}%-40s ${k.name}%-40s ${k.vorname}%-40s" else f"${k.verein}%-40s ${k.name}%-40s ${k.vorname}%-40s"
      krit
    }.groupBy(k => k.durchgang)
    val rawpages = for {
      durchgang <- kandidatenPerDurchgang.keys.toList.sorted
      a4seitenmenge <- if(rowsPerPage == 0) kandidatenPerDurchgang(durchgang).sliding(kandidatenPerDurchgang(durchgang).size, kandidatenPerDurchgang(durchgang).size) else kandidatenPerDurchgang(durchgang).sliding(rowsPerPage, rowsPerPage)
    }
    yield {
      anmeldeListeProDurchgangVerein(durchgang, a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }

  def toHTMLasVereinsListe(kandidaten: Seq[Kandidat], logo: File, rowsPerPage: Int = 28): String = {
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${escaped(k.programm)}%-40s ${escaped(k.name)}%-40s ${escaped(k.vorname)}%-40s"
      //logger.debug(krit)
      krit
    }.groupBy(k => if (k.team.nonEmpty) k.team else k.verein)
    val rawpages = for {
      verein <- kandidatenPerKategorie.keys.toList.sorted
      a4seitenmenge <- if(rowsPerPage == 0) kandidatenPerKategorie(verein).sliding(kandidatenPerKategorie(verein).size, kandidatenPerKategorie(verein).size) else kandidatenPerKategorie(verein).sliding(rowsPerPage, rowsPerPage)
    }
      yield {
        anmeldeListeProVerein(verein, a4seitenmenge, logo)
      }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}