package ch.seidel.kutu.renderer

trait NotenblattToHtmlRenderer {
  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String,
                      name: String, vorname: String, jahrgang: String, verein: String, diszipline: Seq[String])

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
        table {
          width: 27em;
          border-collapse:collapse;
          border-spacing:0;
        }
        tr {
          font-size: 12px;
          overflow: hidden;
        }
        td {
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
          float: left;
        }
      </style>
    </head>
    <body><ul><li>
  """

  val outro = """
    </li></ul></body>
    </html>
  """

  private def notenblattForGeTu(kandidat: Kandidat) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large">${dis._2}. ${dis._1}</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.updated(d.size-1, d.last.replace("geraeteRow", "totalRow")).mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        <img class=logo src="logo.jpg" title="Logo"/>
        <div class=programm>${kandidat.programm}</br><div class=geschlecht>${kandidat.geschlecht}</div></div>
      </div>
      <h1>${kandidat.wettkampfTitel}</h1>
      <table width="100%">
        <tr><td width="30%">Name:</td><td>${kandidat.name}</td></tr>
        <tr><td>Vorname:</td><td>${kandidat.vorname}</td></tr>
        <tr><td>Jahrgang:</td><td>${kandidat.jahrgang}</td></tr>
        <tr><td>Verein:</td><td>${kandidat.verein}</td></tr>
      </table>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Gerät</td><td>1. Wertung</td><td>2. Wertung</td><td class="totalCol">Endnote</td></tr>
          ${dt}
          <tr class="heavyRow"><td class="large">Total</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>
        </table>
      </div>
      <div class="rang">Rang: __________</div>
    </div>
    """
  }

  private def notenblattForATT(kandidat: Kandidat) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large">${dis._2}. ${dis._1}</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        <div class="logo" style="height: 10px;"><h1>${kandidat.wettkampfTitel}</h1></div>
        <div class=programm>${kandidat.programm}</br>
          <div class=geschlecht>${kandidat.geschlecht}</div></div>
      </div>
      <table width="100%">
        <tr><td width="15%">Name:</td><td>${kandidat.name}</td><td width="15%">Vorname:</td><td>${kandidat.vorname}</td><td width="10%">Jahrgang:</td><td>${kandidat.jahrgang}</td></tr>
      </table>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Disziplin</td><td class="totalCol">Punkte</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  private def notenblattForKuTu(kandidat: Kandidat) = {
    val d = kandidat.diszipline.zip(Range(1, kandidat.diszipline.size+1)).map{dis =>
      s"""<tr class="geraeteRow"><td class="large">${dis._2}. ${dis._1}</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.updated(d.size-1, d.last.replace("geraeteRow", "totalRow")).mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        <img class=logo src="logo.jpg" title="Logo"/>
        <div class=programm>${kandidat.programm}</br><div class=geschlecht>${kandidat.geschlecht}</div></div>
      </div>
      <h1>${kandidat.wettkampfTitel}</h1>
      <table width="100%">
        <tr><td width="20%">Name:</td><td>${kandidat.name}</td><td width="20%">Vorname:</td><td>${kandidat.vorname}</td></tr>
        <tr><td>Verein:</td><td>${kandidat.verein}</td><td>Jahrgang:</td><td>${kandidat.jahrgang}</td></tr>
      </table>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Gerät</td><td>D-Wert</td><td>E-Wert</td><td class="totalCol">Endnote</td></tr>
          ${dt}
          <tr class="heavyRow"><td class="large">Total</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>
        </table>
      </div>
    </div>
    """
  }

  def toHTMLasGeTu(kandidaten: Seq[Kandidat]): String = {
    val blaetter = kandidaten.map(notenblattForGeTu(_))
    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }

  def toHTMLasKuTu(kandidaten: Seq[Kandidat]): String = {
    val blaetter = kandidaten.map(notenblattForKuTu(_))
    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }

  def toHTMLasATT(kandidaten: Seq[Kandidat]): String = {
    val blaetter = kandidaten.map(notenblattForATT(_))
    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}