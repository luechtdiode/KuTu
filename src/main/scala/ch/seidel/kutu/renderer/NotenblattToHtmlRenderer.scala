package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain._
import java.io.File
import PrintUtil._

trait NotenblattToHtmlRenderer {

  val intro = """<html>
    <head>
      <meta charset="UTF-8" />
      <style>
        @media print {
          body { -webkit-print-color-adjust: economy; }
          ul {
            page-break-inside: avoid;
          }
        }
        .notenblatt {
          max-width: 35em;
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
        .geschlecht {
          float: right;
          font-size: 12px;
          font-weight: 600;
        }
        .showborder {
          margin-top: 10px;
          padding: 5px;
          border: 1px solid black;
          border-radius: 5px;
        }
        .geraeteRow {
          border-bottom: 1px solid #ddd;
        }
        .totalRow {
          border-bottom: 1px solid #000;
        }
        .heavyRow {
          font-weight: bolder;
        }
        .totalCol {
          border-left: 1px solid #000;
        }
        .large {
          padding: 10px;
          padding-top: 18px;
          padding-bottom: 18px;
        }
        .rang {
          text-align: right;
          padding-top: 15px;
          font-size: 12px;
          font-weight: 600;
        }
        body {
          font-family: "Arial", "Verdana", sans-serif;
        }
        h1 {
          font-size: 75%;
        }
        .dataTable {
          border-collapse:collapse;
          border-spacing:0;
        }
        table {
          width: 100%;
        }
        tr {
          font-size: 12px;
          overflow: hidden;
        }
        td { vertical-align: top; }
        .dataTd {
          padding: 6px;
        }
        ul {
          margin: 0px;
          padding: 0px;
          border: 0px;
          list-style: none;
          overflow: auto;
        }
        li {
          /*float: left;*/
        }
      </style>
    </head>
    <body><ul><li>
  """

  val outro = """
    </li></ul></body>
    </html>
  """
// for loading logo see https://stackoverflow.com/questions/26447451/javafx-in-webview-img-tag-is-not-loading-local-images
  private def notenblattForGeTu(kandidat: Kandidat, logo: File) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large">${dis._2}. ${dis._1.easyprint}</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.updated(d.size-1, d.last.replace("geraeteRow", "totalRow")).mkString("", "\n", "\n")
    val logoHtml = (if (logo.exists) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else s"")
    s"""<div class=notenblatt>
      <div class=headline>
        $logoHtml
        <div class='programm'>${kandidat.programm}</br><div class='geschlecht'>${kandidat.geschlecht}</div></div>
      </div>
      <h1>${kandidat.wettkampfTitel}</h1>
      <table class='dataTable' width="100%">
        <tr><td class='dataTd' width="30%">Name:</td><td>${kandidat.name}</td></tr>
        <tr><td class='dataTd'>Vorname:</td><td>${kandidat.vorname}</td></tr>
        <tr><td class='dataTd'>Jahrgang:</td><td>${kandidat.jahrgang}</td></tr>
        <tr><td class='dataTd'>Verein:</td><td>${kandidat.verein}</td></tr>
      </table>
      <div class="showborder">
        <table class='dataTable' width="100%">
          <tr class="totalRow heavyRow"><td class='dataTd'>Gerät</td><td class='dataTd'>1. Wertung</td><td class='dataTd'>2. Wertung</td><td class="dataTd totalCol">Endnote</td></tr>
          ${dt}
          <tr class="heavyRow"><td class="dataTd large">Total</td><td class='dataTd'>&nbsp;</td><td class='dataTd'>&nbsp;</td><td class="dataTd totalCol">&nbsp;</td></tr>
        </table>
      </div>
      <div class="rang">Rang: __________</div>
    </div>
    """
  }

  private def notenblattForATT(kandidat: Kandidat, logo: File) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large dataTd">${dis._2}. ${dis._1.easyprint}</td><td class="totalCol dataTd">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        <div class="logo" style="height: 10px;"><h1>${kandidat.wettkampfTitel}</h1></div>
        <div class=programm>${kandidat.programm}</br>
          <div class=geschlecht>${kandidat.geschlecht}</div></div>
      </div>
      <table width="100%">
        <tr><td class='dataTd' width="15%">Name:</td><td class='dataTd'>${kandidat.name}</td><td class='dataTd' width="15%">Vorname:</td><td class='dataTd'>${kandidat.vorname}</td><td class='dataTd' width="10%">Jahrgang:</td><td class='dataTd'>${kandidat.jahrgang}</td></tr>
      </table>
      <div class="showborder">
        <table class="dataTable" width="100%">
          <tr class="totalRow heavyRow"><td class='dataTd'>Disziplin</td><td class="dataTd totalCol">Punkte</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  private def notenblattForKuTu(kandidat: Kandidat, logo: File) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large">${dis._2}. ${dis._1.easyprint}</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.updated(d.size-1, d.last.replace("geraeteRow", "totalRow")).mkString("", "\n", "\n")
    val logoHtml = (if (logo.exists) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else s"")
    s"""<div class=notenblatt>
      <div class=headline>
        ${logoHtml}
        <div class=programm>${kandidat.programm}</br><div class=geschlecht>${kandidat.geschlecht}</div></div>
      </div>
      <h1>${kandidat.wettkampfTitel}</h1>
      <table width="100%">
        <tr><td class='dataTd' width="20%">Name:</td><td class='dataTd'>${kandidat.name}</td><td class='dataTd' width="20%">Vorname:</td><td class='dataTd'>${kandidat.vorname}</td></tr>
        <tr><td class='dataTd'>Verein:</td><td class='dataTd'>${kandidat.verein}</td><td class='dataTd'>Jahrgang:</td><td class='dataTd'>${kandidat.jahrgang}</td></tr>
      </table>
      <div class="showborder">
        <table class="dataTable" width="100%">
          <tr class="totalRow heavyRow"><td class='dataTd'>Gerät</td><td class='dataTd'>D-Wert</td><td class='dataTd'>E-Wert</td><td class="dataTd totalCol">Endnote</td></tr>
          ${dt}
          <tr class="heavyRow"><td class="dataTd large">Total</td><td class='dataTd'>&nbsp;</td><td class='dataTd'>&nbsp;</td><td class="dataTd totalCol">&nbsp;</td></tr>
        </table>
      </div>
    </div>
    """
  }
  val nextSite = "</li></ul><ul><li>\n"
  
  val pageIntro = "<table width='100%'><tr><td>"
  val pageOutro = "</td></tr></table>"
  def toHTMLasGeTu(kandidaten: Seq[Kandidat], logo: File): String = {
    val blaetter = kandidaten.map(notenblattForGeTu(_, logo))
    val pages = blaetter.sliding(2, 2).map { _.mkString(pageIntro, "</td><td>", pageOutro) }.mkString(nextSite)
    intro + pages + outro
  }

  def toHTMLasKuTu(kandidaten: Seq[Kandidat], logo: File): String = {
    val blaetter = kandidaten.map(notenblattForKuTu(_, logo))
//    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString(nextSite)
    val pages = blaetter.sliding(2, 2).map { _.mkString(pageIntro, "</td><td>", pageOutro) }.mkString(nextSite)
    intro + pages + outro
  }

  def toHTMLasATT(kandidaten: Seq[Kandidat], logo: File): String = {
    val blaetter = kandidaten.map(notenblattForATT(_, logo))
//    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString(nextSite)
    val pages = blaetter.sliding(2, 2).map { _.mkString(pageIntro, "</td><td>", pageOutro) }.mkString(nextSite)
    intro + pages + outro
  }
}