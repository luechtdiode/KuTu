package ch.seidel.kutu.renderer

trait KategorieTeilnehmerToHtmlRenderer {
  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String,
                      name: String, vorname: String, jahrgang: String, verein: String, riege: String, start: String, diszipline: Seq[String])

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

  private def notenblattForATT(kategorie: String, kandidaten: Seq[Kandidat], logo: String) = {
    val d = kandidaten.map{kandidat =>
      s"""<tr class="athletRow"><td class="large">${kandidat.name} ${kandidat.vorname} (${kandidat.jahrgang})</td><td>${kandidat.verein}</td><td>${kandidat.riege}</td><td>${kandidat.start}</td><td class="totalCol">&nbsp;</td></tr>"""
    }
    val dt = d.mkString("", "\n", "\n")
    s"""<div class=notenblatt>
      <div class=headline>
        <img class=logo src="${logo}" title="Logo"/>
        <div class=programm>${kategorie}</br></div>
        <h4>${kandidaten.head.wettkampfTitel}</h4>
      </div>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Name</td><td>Verein</td><td>Einteilung</td><td>Start</td><td class="totalCol">Bemerkung</td></tr>
          ${dt}
        </table>
      </div>
    </div>
  """
  }

  def toHTMLasKategorienListe(kandidaten: Seq[Kandidat], logo: String): String = {
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${k.name}%s40 ${k.vorname}%s40"
      println(krit)
      krit
    }.groupBy(k => k.programm)
    val rawpages = for {
      kategorie <- kandidatenPerKategorie.keys.toList.sorted
      a4seitenmenge <- kandidatenPerKategorie(kategorie).sliding(30, 30)
    }
    yield {
      notenblattForATT(kategorie, a4seitenmenge, logo)
    }

    val pages = rawpages.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}