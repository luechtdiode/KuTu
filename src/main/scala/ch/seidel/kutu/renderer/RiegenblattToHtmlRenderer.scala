package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain._

object RiegenBuilder {

  def mapToGeraeteRiegen(kandidaten: List[Kandidat]): List[GeraeteRiege] = {

    def pickStartformationen(geraete: List[(Option[Disziplin], List[Riege])], durchgang: Option[String], extractKandidatEinteilung: Kandidat => (Option[Riege], Seq[Disziplin])) = {
      geraete.flatMap{s =>
        val (startdisziplin, _) = s
        val splitpoint = geraete.indexWhere(g => g._1.equals(startdisziplin))
        val shifted = geraete.drop(splitpoint) ++ geraete.take(splitpoint)
        shifted.zipWithIndex.map{ss =>
          val ((disziplin, _), offset) = ss
          val tuti = kandidaten.filter{k =>
            val (einteilung, diszipline) = extractKandidatEinteilung(k)
            (einteilung match {
            case Some(einteilung) =>
              einteilung.durchgang.equals(durchgang) &&
              einteilung.start.equals(startdisziplin) &&
              diszipline.map(_.id).contains(disziplin.map(_.id).getOrElse(0))
            case None => false
          })}.sortBy { x => x.verein + x.jahrgang}
          if(tuti.size > 0) {
            val mo = (tuti.size * geraete.size + offset) % tuti.size
            (offset, disziplin, (tuti.drop(mo) ++ tuti.take(mo)))
          }
          else {
            (offset, disziplin, tuti)
          }
        }
      }.filter(p => p._3.nonEmpty)
    }

    val hauptdurchgaenge = kandidaten
    .filter(k => k.einteilung.nonEmpty)
    .map(k => (k, k.einteilung.get))
    .groupBy(e => e._2.durchgang).toList
    .sortBy(d => d._1)
    .map{d =>
      val (durchgang, kandidatriegen) = d
      val riegen1 = kandidatriegen.map(_._2)
      val dzl1 = kandidatriegen.flatMap(_._1.diszipline)
      val riegen = riegen1.sortBy(r => r.start.map( dzl1.indexOf(_)))
      val dzl2 = dzl1.toSet.toList
      val dzl = dzl2.sortBy{dzl1.indexOf(_)}

      //kandidat.diszipline für die Rotationsberechnung verwenden
      val rg = riegen.groupBy(e => e.start).toList.sortBy{d => d._1.map( dzl.indexOf(_))}
      val geraete = dzl.foldLeft(rg){(acc, item) =>
        acc.find(p => p._1.exists { f => f.equals(item) }) match {
          case Some(_) => acc
          case _ => acc :+ (Some(item) -> List[Riege]())
        }
      }
      val startformationen = pickStartformationen(geraete, durchgang, k => (k.einteilung, k.diszipline))

      (durchgang, startformationen)
    }
    val nebendurchgaenge = kandidaten
    .filter(k => k.einteilung2.nonEmpty)
    .map(k => (k, k.einteilung2.get))
    .groupBy(e => e._2.durchgang).toList
    .sortBy(d => d._1)
    .map{d =>
      val (durchgang, kandidatriegen) = d
      val riegen1 = kandidatriegen.map(_._2)
      val dzl1 = kandidatriegen.flatMap(_._1.diszipline2)
      val riegen = riegen1.sortBy(r => r.start.map( dzl1.indexOf(_)))
      val dzl2 = dzl1.toSet.toList
      val dzl = dzl2.sortBy{dzl1.indexOf(_)}

      //kandidat.diszipline für die Rotationsberechnung verwenden
      val rg = riegen.groupBy(e => e.start).toList.sortBy{d => d._1.map( dzl.indexOf(_))}
      val geraete = dzl.foldLeft(rg){(acc, item) =>
        acc.find(p => p._1.exists { f => f.equals(item) }) match {
          case Some(_) => acc
          case _ => acc :+ (Some(item) -> List[Riege]())
        }
      }
      val startformationen = pickStartformationen(geraete, durchgang, k => (k.einteilung2, k.diszipline2))

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
        .sf {
          font-size: 9px;
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

  def shorten(s: String) = if(s.length() > 3) (" " + s.split(" ").map(_.take(3) + ".").mkString(" ")) else s

  private def notenblatt(riegepart: (GeraeteRiege, Int), logo: String) = {
    val (riege, tutioffset) = riegepart
    val d = riege.kandidaten.zip(Range(1, riege.kandidaten.size+1)).map{kandidat =>
      val programm = if(kandidat._1.programm.isEmpty())"" else "(" + shorten(kandidat._1.programm) + ")"
      s"""<tr class="turnerRow"><td class="large">${kandidat._2 + tutioffset}. ${kandidat._1.vorname} ${kandidat._1.name} <span class='sf'>${programm}</span></td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
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

  val fcs = 15

  def toHTML(kandidaten: Seq[Kandidat], logo: String): String = {
    def splitToFitPage(riegen: List[GeraeteRiege]) = {
      riegen.foldLeft(List[(GeraeteRiege, Int)]()){(acc, item) =>
        if(item.kandidaten.size > fcs) {
          acc ++ item.kandidaten.sliding(fcs, fcs)
          .zipWithIndex.map(k => (item.copy(kandidaten = k._1), k._2 * fcs))
        }
        else {
          acc :+ (item, 0)
        }
      }
      .map{r =>
        val (riege, offset) = r
        val full = (fcs + riege.kandidaten.size / fcs * fcs) - riege.kandidaten.size
        if(full % fcs == 0) {
          r
        }
        else {
          (riege.copy(kandidaten = riege.kandidaten ++ (1 to full).map(i => Kandidat(
              riege.kandidaten.head.wettkampfTitel,
              "", "", 0,
              "", "", "", "", None, None, Seq[Disziplin](), Seq[Disziplin]()))), offset)
        }
      }
    }
    val riegendaten = splitToFitPage(RiegenBuilder.mapToGeraeteRiegen(kandidaten.toList))
    val blaetter = riegendaten.map(notenblatt(_, logo))
    val pages = blaetter.sliding(2, 2).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}