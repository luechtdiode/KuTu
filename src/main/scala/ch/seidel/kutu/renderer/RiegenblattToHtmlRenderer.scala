package ch.seidel.kutu.renderer

import java.io.File
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.KategorieTeilnehmerToHtmlRenderer.getDurchgangFullName
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

import java.time.LocalDateTime

object RiegenBuilder {
  val logger = LoggerFactory.getLogger(this.getClass)
  def mapToGeraeteRiegen(kandidaten: Seq[Kandidat], printorder: Boolean = false, durchgangFilter: Set[String] = Set.empty, haltsFilter: Set[Int] = Set.empty): List[GeraeteRiege] = {
    val sorter: RiegenRotationsregel = if (kandidaten.nonEmpty && kandidaten.head.wertungen.nonEmpty)
      RiegenRotationsregel(kandidaten.head.wertungen.head.wettkampf)
    else RiegenRotationsregel("")

    def pickStartformationen(geraete: Seq[(Option[Disziplin], Seq[Riege], Int)], durchgang: Option[String], extractKandidatEinteilung: Kandidat => (Option[Riege], Seq[Disziplin])): Seq[(Int, Option[Disziplin], Seq[Kandidat], Boolean)] = {
      /*
        stationmap[Disziplin,Int](
          // Disziplin -> Halt
          Boden -> 1,
          Barren -> 2,
          Balken -> 2,
          Minitramp -> 3,
          Sprung -> 4
        )
       */
      val stationmap: Map[Disziplin, Int] = geraete.foldLeft(Map[Disziplin, Int]()) { (acc, item) =>
        acc.updated(item._1.get, item._3)
      }
      val splitmerged = stationmap.values.toList.distinct.size < stationmap.size
      //println(splitmerged, stationmap.toList.map(x => (x._2, x._1)).sortBy(_._1).mkString("\n"))
      /*
      1. Halt: Riege Startgerät (1W,Boden), (2M,Barren), (3W,Balken), (4W,Minitramp) (5M,Minitramp), (6M,Sprung)

      2. Halt: Riege Startgerät                          (1W,Balken), (2M,Minitramp),                (3W,Sprung),
                                (4W,Boden) (5M,Boden), (6M,Barren)

      3. Halt: Riege Startgerät                                       (1W,Minitramp),                (2M,Sprung),
                                (3W,Boden), (4W,Balken) (5M,Barren), (6M,Minitramp)

      4. Halt: Riege Startgerät                                                                      (1W,Sprung)
               ,                (2M,Boden), (3W,Balken),             (4W,Minitramp) (5M,Minitramp), (6M,Sprung)
      */
      val athletdevicemap: Seq[(Kandidat, List[Disziplin])] = geraete.flatMap(d => kandidaten
        .map { kandidat =>
          (kandidat, extractKandidatEinteilung(kandidat))
        }
        .filter { riegeneinteilung =>
          val (_, (einteilung, diszipline)) = riegeneinteilung
          val disziplin = d._1.get
          einteilung match {
            case Some(einteilung) =>
              einteilung.durchgang.equals(durchgang) &&
                einteilung.start.contains(disziplin) &&
                (!splitmerged || diszipline.map(_.id).contains(disziplin.id))
            case None =>
              false
          }
        }
        .map { riegeneinteilung =>
          val (kandidat, (_, diszipline)) = riegeneinteilung
          val disziplin = d._1.get
          if (splitmerged)
            (kandidat, (diszipline.dropWhile(d => d.id != disziplin.id) ++ diszipline.takeWhile(d => d.id != disziplin.id)).toList)
          else {
            val diszs = geraete.map(_._1.get).map{d =>
              if (diszipline.contains(d)) d else d.asPause
            }
            (kandidat, (diszs.dropWhile(d => !d.equalsOrPause(disziplin)) ++ diszs.takeWhile(d => !d.equalsOrPause(disziplin))).toList)
          }
        }
      )

      stationmap.values.toList.distinct.sorted.flatMap { station =>
        athletdevicemap
          .filter{ _._2.size > station }
          .map{ kandidat => (kandidat._2(station), kandidat) }
          .groupBy(_._1)
          .map{gr =>
            val (disziplin: Disziplin, candidates: Seq[(Disziplin, (Kandidat, List[Disziplin]))]) = gr
            val completed = candidates.flatMap(k => k._2._1.wertungen)
              .filter(wertung => disziplin.equals(wertung.wettkampfdisziplin.disziplin))
              .forall(_.endnote.nonEmpty)
            val sortedCandidates = candidates.map(x => x._2._1).sortBy(sorter.sort)
            val scWithOffset = sortedCandidates.drop(station) ++ sortedCandidates.take(station)
            (station, Some(gr._1), scWithOffset, completed)
          }
      }.filter(p => p._3.nonEmpty)
    }

    val hauptdurchgaenge = kandidaten
    .filter(k => k.einteilung.nonEmpty)
    .map(k => (k, k.einteilung.get))
    .groupBy(e => e._2.durchgang).toList
    .sortBy(d => d._1).zipWithIndex
    .map{d =>
      val ((durchgang, kandidatriegen), durchgangIndex) = d
      val dzlmap: Map[Disziplin,Int] = kandidatriegen.flatMap(_._1.diszipline.zipWithIndex).foldLeft(Map[Disziplin,Int]()){(acc, item) =>
        acc.updated(item._1, item._2)
      }
      val dzl = dzlmap.keys.toList.sortBy{dzlmap(_)}
      val riegen = kandidatriegen.map(_._2).sortBy(r => r.start.map( dzl.indexOf(_)))
      //kandidat.diszipline für die Rotationsberechnung verwenden
      val rg = riegen.groupBy(e => e.start).toList.sortBy{d => d._1.map( dzl.indexOf(_))}
      val geraete: Seq[(Option[Disziplin], Seq[Riege], Int)] = dzl.foldLeft(rg) { (acc, item) =>
          acc.find(p => p._1.exists { f => f.equals(item) }) match {
            case Some(_) => acc
            case _ => acc :+ (Some(item) -> List[Riege]())
          }
        }
        .filter(pair => dzlmap.exists(p => pair._1.contains(p._1)))
        .sortBy(geraet => geraet._1.map(g => dzl.indexOf(g)))
        .map(geraet => (geraet._1, geraet._2, dzlmap(geraet._1.get)))
      //println(geraete.map(x => (x._3, s"M:${x._2.count(_.easyprint.contains("M"))},W:${x._2.count(_.easyprint.contains("W"))}", x._1.get)).mkString("\n"))
      val startformationen = pickStartformationen(geraete, durchgang, k => (k.einteilung, k.diszipline))
        .zipWithIndex
        .map(x => {
          val (sf, index) = x
          (sf._1, sf._2, sf._3, sf._4, durchgangIndex * 100 + index + 1)
        })
      if (printorder) {
        (durchgang, startformationen.sortBy(d => d._2.map(x => x.normalizedOrdinal(dzl)).getOrElse(0) * 100 + d._1))
      }
      else {
        (durchgang, startformationen.sortBy(d => d._1 * 100 + d._2.map(x => x.normalizedOrdinal(dzl)).getOrElse(0)))
      }
    }

    val nebendurchgaenge = kandidaten
    .filter(k => k.einteilung2.nonEmpty)
    .map(k => (k, k.einteilung2.get))
    .groupBy(e => e._2.durchgang).toList
    .sortBy(d => d._1).zipWithIndex
    .map{d =>
      val ((durchgang, kandidatriegen), durchgangIndex) = d
      val riegen1 = kandidatriegen.map(_._2)
      val dzlmap: Map[Disziplin, Int] = kandidatriegen.flatMap(_._1.diszipline2.zipWithIndex).foldLeft(Map[Disziplin, Int]()) { (acc, item) =>
        acc.updated(item._1, item._2)
      }
      val dzl = dzlmap.keys.toList.sortBy {
        dzlmap(_)
      }
      val riegen = riegen1.sortBy(r => r.start.map( dzlmap(_)))

      //kandidat.diszipline für die Rotationsberechnung verwenden
      val rg = riegen.groupBy(e => e.start).toList.sortBy{d => d._1.map( dzl.indexOf(_))}
      val geraete = dzl.foldLeft(rg){(acc, item) =>
        acc.find(p => p._1.exists { f => f.equals(item) }) match {
          case Some(_) => acc
          case _ => acc :+ (Some(item) -> List[Riege]())
        }
      }
        .filter(pair => dzlmap.exists(p => pair._1.contains(p._1)))
        .sortBy(geraet => geraet._1.map(g => dzl.indexOf(g)))
        .map(geraet => (geraet._1, geraet._2, dzlmap(geraet._1.get)))

      val startformationen = pickStartformationen(geraete, durchgang, k => (k.einteilung2, k.diszipline2))
        .zipWithIndex
        .map(x => {
          val (sf: (Int, Option[Disziplin], Seq[Kandidat], Boolean), index) = x
          (sf._1, sf._2, sf._3, sf._4, durchgangIndex*100 + 100 - index)
        })

      if (printorder) {
        (durchgang, startformationen)        
      }
      else {
        (durchgang, startformationen.sortBy(d => d._1 * 100 + d._2.map(x => x.normalizedOrdinal(dzl))))
      }
    }
    val riegen = (hauptdurchgaenge ++ nebendurchgaenge).flatMap { item =>
      val (durchgang: Option[String], starts: Seq[(Int, Option[Disziplin], Seq[Kandidat], Boolean, Int)]) = item
      val isND = nebendurchgaenge.exists(nd => nd._1 == durchgang && nd._2.exists(disz => disz._2 == starts.head._2))
      durchgang match {
        case Some(dg) if durchgangFilter.nonEmpty && !durchgangFilter.contains(dg) => Seq.empty
        case _ =>
          starts
            .filter(start => haltsFilter.isEmpty || (!isND && haltsFilter.contains(start._1)) || haltsFilter.exists(halt => halt < 0 && (math.abs(halt) <= start._1 || isND)))
            .filter(start => !(printorder && start._2.exists(_.isPause)))
            .map { start =>
            GeraeteRiege(start._3.head.wettkampfTitel, start._3.head.wertungen.head.wettkampf.uuid.get,
              durchgang, start._1, start._2, start._3, start._4, f"R${start._5}%04d")

          }
      }
    }

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

  def shorten(s: String, l: Int = 10) = {
    if (s.length() <= l) {
      s.trim
    } else {
      val words = s.split("[ ,]")
      words.map(_.take(l)).mkString(" ").trim
      /*if (words.length > 2) {
        if(words(words.length - 1).length < l) {
          s"${words(0)}..${words(words.length - 2)} ${words(words.length - 1)}"
        } else {
          s"${words(0)}..${words(words.length - 1)}"
        }
      } else {
        words.map(_.take(l)).mkString("", " ", ".")
      }*/
    }
  }

  private def notenblatt(riegepart: (GeraeteRiege, Int), logo: File, baseUrl: String, dgMapping: Map[String, (SimpleDurchgang, LocalDateTime)]) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""
    val (riege, tutioffset) = riegepart
    val d = riege.kandidaten.zip(Range(1, riege.kandidaten.size+1)).map{kandidat =>
      val einteilung: String = kandidat._1.einteilung
        .map(r => r.r.replaceAll( s",${kandidat._1.verein}", ""))
        .getOrElse(kandidat._1.programm)
      val programm = if(einteilung.isEmpty()) "" else "(" + shorten(einteilung) + ")"
      val verein = if(kandidat._1.verein.isEmpty())"" else shorten(kandidat._1.verein, 15)
      s"""<tr class="turnerRow"><td class="large">${kandidat._2 + tutioffset}. ${escaped(kandidat._1.vorname)} ${escaped(kandidat._1.name)} <span class='sf'>${escaped(programm)}</span></td><td><span class='sf'>${escaped(verein)}</span></td><td>&nbsp;</td><td>&nbsp;</td><td class="totalCol">&nbsp;</td></tr>"""
    }.mkString("", "\n", "\n")

    val stationlink = WertungsrichterQRCode.toURI(baseUrl, riege)
    val imagedata = s"<a href='$stationlink' target='_blank'><img title='${stationlink}' width='140px' height='140px' src='${PrintUtil.toQRCodeImage(stationlink)}'></a>"
    s"""<div class=riegenblatt>
      <div class=headline>
        $logoHtml $imagedata
        <div class=durchgang>${escaped(riege.durchgang.map(d => getDurchgangFullName(dgMapping, d)).getOrElse(""))}</br><div class=geraet>${escaped(riege.disziplin.map(d => d.easyprint).getOrElse(""))} (${riege.halt + 1}. Gerät)<br>Riegencode: ${riege.sequenceId}</div></div>
      </div>
      <h1>${escaped(riege.wettkampfTitel)}</h1>
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

  def toHTML(kandidaten: Seq[Kandidat], logo: File, baseUrl: String, durchgangFilter: Set[String] = Set.empty, haltsFilter: Set[Int] = Set.empty, dgMapping: Seq[(SimpleDurchgang, LocalDateTime)]): String = {
    val dgmap = dgMapping.map(dg => dg._1.name -> dg).toMap
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
    val riegendaten = splitToFitPage(RiegenBuilder.mapToGeraeteRiegen(kandidaten.toList, printorder = true, durchgangFilter = durchgangFilter, haltsFilter = haltsFilter))
    val blaetter = riegendaten.map(notenblatt(_, logo, baseUrl, dgmap))
    val pages = blaetter.sliding(1, 1).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}