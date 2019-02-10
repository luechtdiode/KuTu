package ch.seidel.kutu.renderer

import java.io.File

import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

object RiegenBuilder {
  val logger = LoggerFactory.getLogger(this.getClass)
  def mapToGeraeteRiegen(kandidaten: Seq[Kandidat], printorder: Boolean = false, groupByProgramm: Boolean = true): List[GeraeteRiege] = {

    def pickStartformationen(geraete: Seq[(Option[Disziplin], Seq[Riege])], durchgang: Option[String], extractKandidatEinteilung: Kandidat => (Option[Riege], Seq[Disziplin])) = {
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
          })}.sortBy { x => (if (groupByProgramm) x.programm else "") + x.verein + x.jahrgang + x.geschlecht + x.name}
          
          val completed = tuti.
            flatMap(k => k.wertungen).
            filter(wertung => disziplin.forall(_.equals(wertung.wettkampfdisziplin.disziplin))).
            forall(_.endnote > 0d)
            
          if(tuti.size > 0) {
            val mo = (tuti.size * geraete.size + offset) % tuti.size
            (offset, disziplin, (tuti.drop(mo) ++ tuti.take(mo)), completed)
          }
          else {
            (offset, disziplin, tuti, completed)
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
      if (printorder) {
        (durchgang, startformationen.sortBy(d => d._2.map( dzl.indexOf(_)).getOrElse(0) * 100 + d._1))        
      }
      else {
        (durchgang, startformationen.sortBy(d => d._1 * 100 + d._2.map( dzl.indexOf(_)).getOrElse(0)))
      }
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

      if (printorder) {
        (durchgang, startformationen)        
      }
      else {
        (durchgang, startformationen.sortBy(d => d._1 * 100 + d._2.map( dzl.indexOf(_))))
      }
    }
    val riegen = (hauptdurchgaenge ++ nebendurchgaenge).flatMap{item =>
      val (durchgang, starts) = item
      starts.map{start =>
        GeraeteRiege(start._3.head.wettkampfTitel, durchgang, start._1, start._2, start._3, start._4)
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
          body { -webkit-print-color-adjust: economy; }
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
          font-size: 16px;
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
          width: 35em;
          border-collapse:collapse;
          border-spacing:0;
        }
        tr {
          font-size: 14px;
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

  def shorten(s: String, l: Int = 3) = {
    if (s.length() <= l) {
      s
    } else {
      val words = s.split(" ")
      val ll = words.length + l -1;
      s.take(ll) + "."
    }
  }

  private def notenblatt(riegepart: (GeraeteRiege, Int), logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""
    val (riege, tutioffset) = riegepart
    val d = riege.kandidaten.zip(Range(1, riege.kandidaten.size+1)).map{kandidat =>
      val programm = if(kandidat._1.programm.isEmpty())"" else "(" + shorten(kandidat._1.programm) + ")"
      val verein = if(kandidat._1.verein.isEmpty())"" else shorten(kandidat._1.verein, 15) 
      s"""<tr class="turnerRow"><td class="large">${kandidat._2 + tutioffset}. ${kandidat._1.vorname} ${kandidat._1.name} <span class='sf'>${programm}</span></td><td><span class='sf'>${verein}</span></td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }.mkString("", "\n", "\n")

    s"""<div class=riegenblatt>
      <div class=headline>
        $logoHtml
        <div class=durchgang>${riege.durchgang.getOrElse("")}</br><div class=geraet>${riege.disziplin.map(d => d.easyprint).getOrElse("")} (${riege.halt + 1}. Gerät)</div></div>
      </div>
      <h1>${riege.wettkampfTitel}</h1>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Turner/Turnerin</td><td>Verein</td><td>1. Wertung</td><td>2. Wertung</td><td class="totalCol">Endnote</td></tr>
          ${d}
        </table>
      </div>
    </div>
    """
  }

  val fcs = 20

  def toHTML(kandidaten: Seq[Kandidat], logo: File): String = {
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
              "", "", "", "", None, None, 
              Seq[Disziplin](), Seq[Disziplin](), Seq[WertungView]())))
          , offset)
        }
      }
    }
    val riegendaten = splitToFitPage(RiegenBuilder.mapToGeraeteRiegen(kandidaten.toList, true))
    val blaetter = riegendaten.map(notenblatt(_, logo))
    val pages = blaetter.sliding(1, 1).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}