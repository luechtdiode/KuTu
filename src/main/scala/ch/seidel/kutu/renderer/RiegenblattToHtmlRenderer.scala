package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain._

object RiegenBuilder {

  def mapToGeraeteRiegen(kandidaten: List[Kandidat]): List[GeraeteRiege] = {

    def pickStartformationen(geraete: List[(Option[Disziplin], List[Riege])], durchgang: Option[String], extractKandidatEinteilung: Kandidat => Option[Riege]) = {
      geraete.flatMap{s =>
        val (startdisziplin, _) = s
        val splitpoint = geraete.indexWhere(g => g._1.equals(startdisziplin))
        val shifted = geraete.drop(splitpoint) ++ geraete.take(splitpoint)
        shifted.zipWithIndex.map{ss =>
          val ((disziplin, _), offset) = ss
          val tuti = kandidaten.filter{k => (extractKandidatEinteilung(k) match {
            case Some(einteilung) =>
              einteilung.durchgang.equals(durchgang) &&
              einteilung.start.equals(startdisziplin) &&
              k.diszipline.contains(disziplin.map(_.name).getOrElse(""))
            case None => false
          })}
          (offset, disziplin, (tuti.drop(offset) ++ tuti.take(offset)))
        }
      }.filter(p => p._3.nonEmpty)
    }

    val hauptdurchgaenge = kandidaten
    .flatMap(k => k.einteilung)
    .groupBy(e => e.durchgang).toList
    .sortBy(d => d._1)
    .map{d =>
      val (durchgang, riegen) = d
      val geraete = riegen.groupBy(e => e.start).toList
      val startformationen = pickStartformationen(geraete, durchgang, k => k.einteilung)

      (durchgang, startformationen)
    }
    val nebendurchgaenge = kandidaten
    .flatMap(k => k.einteilung2)
    .groupBy(e => e.durchgang).toList
    .sortBy(d => d._1)
    .map{d =>
      val (durchgang, riegen) = d
      val geraete = riegen.groupBy(e => e.start).toList
      val startformationen = pickStartformationen(geraete, durchgang, k => k.einteilung2)

      (durchgang, startformationen)
    }
    val riegen = (hauptdurchgaenge ++ nebendurchgaenge).flatMap{item =>
      val (durchgang, starts) = item
      starts.map{start =>
        GeraeteRiege(start._3.head.wettkampfTitel, durchgang, start._1, start._2, start._3)
      }
    }.toList

    riegen
  }
}

trait RiegenblattToHtmlRenderer {

  val intro = """<html>
    <head>
      <meta charset="UTF-8" />
      <style>
        @media print {
          ul {
            page-break-inside: avoid;
          }
        }
        .riegenblatt {
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
          height: 50px;
          border-radius: 5px;
        }
        .durchgang {
          text-align: right;
          float: right;
          font-size: 24px;
          font-weight: 600;
        }
        .geraet {
          text-align: right;
          float: right;
          font-size: 12px;
          font-weight: 600;
        }
        .showborder {
          margin-top: 8px;
          padding: 5px;
          border: 1px solid black;
          border-radius: 5px;
        }
        .turnerRow {
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
          padding: 8px;
          padding-top: 10px;
          padding-bottom: 10px;
        }
        body {
          font-family: "Arial", "Verdana", sans-serif;
        }
        h1 {
          font-size: 75%;
        }
        table {
          width: 25em;
          border-collapse:collapse;
          border-spacing:0;
        }
        tr {
          font-size: 11px;
          overflow: hidden;
        }
        td {
          padding: 5px;
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

  private def notenblatt(riegepart: (GeraeteRiege, Int), logo: String) = {
    val (riege, tutioffset) = riegepart
    val d = riege.kandidaten.zip(Range(1, riege.kandidaten.size+1)).map{kandidat =>
      s"""<tr class="turnerRow"><td class="large">${kandidat._2 + tutioffset}. ${kandidat._1.vorname} ${kandidat._1.name}</td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }.mkString("", "\n", "\n")

    s"""<div class=riegenblatt>
      <div class=headline>
        <img class=logo src="${logo}" title="Logo"/>
        <div class=durchgang>${riege.durchgang.getOrElse("")}</br><div class=geraet>${riege.disziplin.map(d => d.easyprint).getOrElse("")} (${riege.halt + 1}. Gerät)</div></div>
      </div>
      <h1>${riege.wettkampfTitel}</h1>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Turner/Turnerin</td><td>1. Wertung</td><td>2. Wertung</td><td class="totalCol">Endnote</td></tr>
          ${d}
        </table>
      </div>
    </div>
    """
  }

  def toHTML(kandidaten: Seq[Kandidat], logo: String): String = {
    def splitToFitPage(riegen: List[GeraeteRiege]) = {
      riegen.foldLeft(List[(GeraeteRiege, Int)]()){(acc, item) =>
        if(item.kandidaten.size > 13) {
          acc ++ item.kandidaten.sliding(13, 13).zipWithIndex.map(k => (item.copy(kandidaten = k._1), k._2 * 13))
        }
        else {
          acc :+ (item, 0)
        }
      }
    }
    val riegendaten = splitToFitPage(RiegenBuilder.mapToGeraeteRiegen(kandidaten.toList))
    val blaetter = riegendaten.map(notenblatt(_, logo))
    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}