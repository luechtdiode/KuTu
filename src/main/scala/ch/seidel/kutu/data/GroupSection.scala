package ch.seidel.kutu.data

import ch.seidel.kutu.data.GroupSection.{STANDARD_SCORE_FACTOR}

import java.time._
import java.time.temporal._
import ch.seidel.kutu.domain._

import scala.collection.mutable
import scala.collection.mutable.StringBuilder
import scala.math.BigDecimal.{double2bigDecimal, int2bigDecimal}

object GroupSection {
  val STANDARD_SCORE_FACTOR = BigDecimal("1000000000000000000000")

  def programGrouper( w: WertungView): ProgrammView = w.wettkampfdisziplin.programm.aggregatorSubHead
  def disziplinGrouper( w: WertungView): (Int, Disziplin) = (w.wettkampfdisziplin.ord, w.wettkampfdisziplin.disziplin)
  def groupWertungList(list: Iterable[WertungView]) = {
    val groups = list.filter(w => w.showInScoreList).groupBy(programGrouper).map { pw =>
      (pw._1 -> pw._2.map(disziplinGrouper).toSet[(Int, Disziplin)].toList.sortBy{ d =>
        d._1 }.map(_._2))
    }
    groups
  }

  def mapRang(list: Iterable[(DataObject, Resultat, Resultat)]) = {
    val rangD = list.toList.map(_._2.noteD).filter(_ > 0).sorted.reverse :+ 0
    val rangE = list.toList.map(_._2.noteE).filter(_ > 0).sorted.reverse :+ 0
    val rangEnd = list.toList.map(_._2.endnote).filter(_ > 0).sorted.reverse :+ 0
    def rang(r: Resultat) = {
      val rd = if (rangD.nonEmpty) rangD.indexOf(r.noteD) + 1 else 0
      val re = if (rangE.nonEmpty) rangE.indexOf(r.noteE) + 1 else 0
      val rf = if (rangEnd.nonEmpty) rangEnd.indexOf(r.endnote) + 1 else 0
      Resultat(rd, re, rf)
    }
    list.map(y => GroupSum(y._1, y._2, y._3, rang(y._2)))
  }
  def mapAvgRang(list: Iterable[(DataObject, Resultat, Resultat)]) = {
    val rangD = list.toList.map(_._3.noteD).filter(_ > 0).sorted.reverse :+ 0
    val rangE = list.toList.map(_._3.noteE).filter(_ > 0).sorted.reverse :+ 0
    val rangEnd = list.toList.map(_._3.endnote).filter(_ > 0).sorted.reverse :+ 0
    def rang(r: Resultat) = {
      val rd = if (rangD.nonEmpty) rangD.indexOf(r.noteD) + 1 else 0
      val re = if (rangE.nonEmpty) rangE.indexOf(r.noteE) + 1 else 0
      val rf = if (rangEnd.nonEmpty) rangEnd.indexOf(r.endnote) + 1 else 0
      Resultat(rd, re, rf)
    }
    list.map(y => GroupSum(y._1, y._2, y._3, rang(y._3)))
  }
}

sealed trait GroupSection {
  val groupKey: DataObject
  val sum: Resultat
  val avg: Resultat
  def easyprint: String
}

case class GroupSum(override val groupKey: DataObject, wertung: Resultat, override val avg: Resultat, rang: Resultat) extends GroupSection {
  override val sum: Resultat = wertung
  override def easyprint = f"Rang ${rang.easyprint} ${groupKey.easyprint}%40s Punkte ${sum.easyprint}%18s øPunkte ${avg.easyprint}%18s"
}

sealed trait WKCol {
  val text: String
  val prefWidth: Int
  val styleClass: Seq[String]
}
case class WKLeafCol[T](override val text: String, override val prefWidth: Int, override val styleClass: Seq[String], valueMapper: T => String) extends WKCol
case class WKGroupCol(override val text: String, override val prefWidth: Int, override val styleClass: Seq[String], cols: Seq[WKCol]) extends WKCol

case class GroupLeaf(override val groupKey: DataObject, list: Iterable[WertungView]) extends GroupSection {
  override val sum: Resultat = list.map(_.resultat).reduce(_+_)
  override val avg: Resultat = sum / list.size
  override def easyprint = groupKey.easyprint + s" $sum, $avg"
  val groups = GroupSection.groupWertungList(list).filter(_._2.nonEmpty)
  lazy val anzahWettkaempfe = list.filter(_.endnote.nonEmpty).groupBy { w => w.wettkampf }.size // Anzahl Wettkämpfe
  val withDNotes = list.filter(w => w.noteD.sum > 0).nonEmpty
  val withENotes = list.filter(w => w.wettkampf.programmId != 1).nonEmpty
  val isDivided = !(withDNotes || groups.isEmpty)
  val divider = if(!isDivided) 1 else groups.head._2.size

  val gleichstandsregel = Gleichstandsregel(list.head.wettkampf)
  def buildColumns: List[WKCol] = {
    val athletCols: List[WKCol] = List(
      WKLeafCol[GroupRow](text = "Rang", prefWidth = 20, styleClass = Seq("data"), valueMapper = gr => {
        if(gr.auszeichnung) gr.rang.endnote.intValue match {
          case 1 => f"${gr.rang.endnote}%3.0f G"
          case 2 => f"${gr.rang.endnote}%3.0f S"
          case 3 => f"${gr.rang.endnote}%3.0f B"
          case _ => f"${gr.rang.endnote}%3.0f *"
        }
        else f"${gr.rang.endnote}%3.0f"
      }),
      WKLeafCol[GroupRow](text = "Athlet", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
        val a = gr.athlet
        f"${a.vorname} ${a.name}"
      }),
      WKLeafCol[GroupRow](text = "Jahrgang", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
        val a = gr.athlet
        f"${AthletJahrgang(a.gebdat).jahrgang}"
      }),
      WKLeafCol[GroupRow](text = "Verein", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
        val a = gr.athlet
        s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
      })
    )

    val indexer = Iterator.from(0)
    val disziplinCol: List[WKCol] =
      if (groups.keySet.size > 1) {
        // Mehrere "Programme" => pro Gruppenkey eine Summenspalte bilden
        groups.toList.sortBy(gru => gru._1.ord).map { gr =>
          val (grKey, disziplin) = gr

          def colsum(gr: GroupRow) =
            gr.resultate.find { x =>
              x.title.equals(grKey.easyprint) }.getOrElse(
                  LeafRow(grKey.easyprint, Resultat(0, 0, 0), Resultat(0, 0, 0), auszeichnung = false))

          val clDnote = WKLeafCol[GroupRow](text = "D", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
         		val best = if(cs.sum.noteD > 0 && cs.rang.noteD.toInt == 1) "*" else ""
            best + cs.sum.formattedD
          })
          val clEnote = WKLeafCol[GroupRow](text = if(withDNotes) "E" else "ø Gerät", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
         		val best = if(cs.sum.noteE > 0 && cs.rang.noteE.toInt == 1) "*" else ""
         		val div = Math.max(gr.divider, divider)
            if(div == 1) {
              best + cs.sum.formattedE
            }
            else {
            	best + (cs.sum / div).formattedEnd
            }
          })
          val clEndnote = WKLeafCol[GroupRow](text = "Endnote", prefWidth = 60, styleClass = Seq("valuedata"), valueMapper = gr => {
            val cs = colsum(gr)
         		val best = if(cs.sum.endnote > 0 && cs.sum.endnote.toInt == 1) "*" else ""
            best + cs.sum.formattedEnd
          })
          val clRang = WKLeafCol[GroupRow](text = "Rang", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
         		cs.rang.formattedEnd
          })
          val cl: WKCol = WKGroupCol(
              text = if(anzahWettkaempfe > 1) {
                s"ø aus " + grKey.easyprint
              }
              else {
                grKey.easyprint
              }
            , prefWidth = 240, styleClass = Seq("hintdata"), cols = {
              val withDNotes = list.exists(w => w.noteD.sum > 0)
              if(withDNotes) {
                Seq(clDnote, clEnote, clEndnote, clRang)
              }
              else if(withENotes) {
                Seq(clEnote, clEndnote, clRang)
              }
              else {
                Seq(clEndnote, clRang)
              }
            }
            )
          cl
        }
      }
      else {
        groups.head._2.map { disziplin =>
          val index = indexer.next()
          lazy val clDnote = WKLeafCol[GroupRow](text = "D", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
                  val best = if (gr.resultate(index).sum.noteD > 0
                              && gr.resultate(index).rang.noteD.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + gr.resultate(index).sum.formattedD
                } else ""
          })
          lazy val clEnote = WKLeafCol[GroupRow](text = "E", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
                  val best = if (gr.resultate(index).sum.noteE > 0
                              && gr.resultate(index).rang.noteE.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + gr.resultate(index).sum.formattedE
                } else ""
          })
          lazy val clEndnote = WKLeafCol[GroupRow](text = "Endnote", prefWidth = 60, styleClass = Seq("valuedata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
                  val best = if (gr.resultate(index).sum.endnote > 0
                              && gr.resultate(index).rang.endnote.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + gr.resultate(index).sum.formattedEnd
                } else ""
          })
          lazy val clRang = WKLeafCol[GroupRow](text = "Rang", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) f"${gr.resultate(index).rang.endnote}%3.0f" else ""
          })
          val cl = WKGroupCol(text = if(anzahWettkaempfe > 1) {
              s"ø aus " + disziplin.name
            }
            else {
              disziplin.name
            }
          , prefWidth = 240, styleClass = Seq("valuedata"), cols= {
            if(withDNotes) {
              Seq(clDnote, clEnote, clEndnote, clRang)
            }
            else {
              Seq(clEndnote, clRang)
            }
          })
          cl
        }.toList
      }
    val sumColAll: List[WKCol] = List(
      WKLeafCol[GroupRow](
          text = if(anzahWettkaempfe > 1) {
            s"Total ø aus D"
          }
          else {
            "Total D"
          }
          , prefWidth = 80, styleClass = Seq("hintdata"), valueMapper = gr => {
            gr.sum.formattedD
          }
      ),
      WKLeafCol[GroupRow](
          text = if(anzahWettkaempfe > 1) {
            if(!isDivided && withDNotes) {
              s"Total ø aus E"
            }
            else {
              s"ø Gerät"
            }
          }
          else if(!isDivided && withDNotes) {
            "Total E"
          }
          else {
            "ø Gerät"
          }
          , prefWidth = 80, styleClass = Seq("hintdata"), valueMapper = gr => {
            val div = Math.max(gr.divider, divider)
            if (div < 2) {
              if(gr.sum.noteE > 0
                 && gr.rang.noteE.toInt == 1)
                "*" + gr.sum.formattedE
              else
                "" + gr.sum.formattedE
            }
            else {
              (gr.sum / div).formattedEnd
            }
          }
      ),
      WKLeafCol[GroupRow](
          text = if(anzahWettkaempfe > 1) {
            s"Total ø Punkte"
          }
          else {
            "Total Punkte"
          }
          , prefWidth = 80, styleClass = Seq("valuedata"), valueMapper = gr => {
            gr.sum.formattedEnd
          }
      )
    )

    val sumCol: List[WKCol] = List(withDNotes, isDivided || withDNotes, true).zip(sumColAll).filter(v => v._1).map(_._2)
    athletCols ++ disziplinCol ++ sumCol
  }

  def getTableData(sortAlphabetically: Boolean = false) = {

    def mapToAvgRang[A <: DataObject](grp: Iterable[(A, (Resultat, Resultat))]) = {
      GroupSection.mapAvgRang(grp.map { d => (d._1, d._2._1, d._2._2) }).map(r => (r.groupKey.asInstanceOf[A] -> r)).toMap
    }
    def mapToAvgRowSummary(athlWertungen: Iterable[WertungView]): (Resultat, Resultat, Iterable[(Disziplin, Long, Resultat, Resultat, Option[Int], Option[BigDecimal])], Iterable[(ProgrammView, Resultat, Resultat, Option[Int], Option[BigDecimal])], Resultat) = {
      val wks = athlWertungen.filter(_.endnote.nonEmpty).groupBy { w => w.wettkampf }
      val wksums = wks.map {wk => wk._2.map(w => w.resultat).reduce(_+_)}.toList
      val rsum = if(wksums.nonEmpty) wksums.reduce(_+_) else Resultat(0,0,0)

      val gwksums = wks.map {wk => wk._2.map{w =>
        if (anzahWettkaempfe > 1)
          w.resultat
        else
          (w.resultat * STANDARD_SCORE_FACTOR) + (w.resultat * gleichstandsregel.factorize(w, wksums))
        }.reduce(_+_)}
      val gsum = if(gwksums.nonEmpty) gwksums.reduce(_+_) else Resultat(0,0,0)
      val avg = if(wksums.nonEmpty) rsum / wksums.size else Resultat(0,0,0)
      val gavg = if(wksums.nonEmpty) gsum / wksums.size else Resultat(0,0,0)
      val withAuszeichnung = anzahWettkaempfe == 1 && groups.size == 1 && wks.size == 1
      val auszeichnung = if(withAuszeichnung) Some(wks.head._1.auszeichnung) else None
      val auszeichnungEndnote = if(withAuszeichnung && wks.head._1.auszeichnungendnote > 0) Some(wks.head._1.auszeichnungendnote) else None
      val perDisziplinAvgs = (for {
        wettkampf <- wks.keySet.toSeq
        ((ord, disziplin), dwertungen) <- wks(wettkampf).groupBy { x => (x.wettkampfdisziplin.ord, x.wettkampfdisziplin.disziplin) }
      }
      yield {
        val prog = dwertungen.head.wettkampf.programmId
        val dsums = dwertungen.map {w => w.resultat}
        val dsum = if(dsums.nonEmpty) dsums.reduce(_+_) else Resultat(0,0,0)
        ((ord, disziplin, prog) -> dsum)
      }).groupBy(_._1).map{x =>
        val xsum = x._2.map(_._2).reduce(_+_)
        (x._1, xsum, xsum / x._2.size, auszeichnung, auszeichnungEndnote)}
      .toList.sortBy(d => d._1._1)
      .map(d => (d._1._2, d._1._3, d._2, d._3, d._4, d._5))

      val perProgrammAvgs = (for {
        wettkampf <- wks.keySet.toSeq
        (programm, pwertungen) <- wks(wettkampf).groupBy { _.wettkampfdisziplin.programm.aggregatorSubHead }
        psums = pwertungen.map {w => w.resultat}
        psum = if(psums.nonEmpty) psums.reduce(_+_) else Resultat(0,0,0)
      }
      yield {
        (programm, psum)
      }).groupBy(_._1).map{x =>
        val xsum = x._2.map(_._2).reduce(_+_)
        (x._1, xsum, xsum / x._2.size, auszeichnung, auszeichnungEndnote)}
      .toList.sortBy(d => d._1.ord)
      (rsum, avg, perDisziplinAvgs, perProgrammAvgs, gavg)
    }

    val avgPerAthlet = list.groupBy { x =>
      x.athlet
    }.map { x =>
      (x._1, mapToAvgRowSummary(x._2))
    }.filter(_._2._1.endnote > 0)

    // Beeinflusst die Total-Rangierung pro Gruppierung
    val rangMap: Map[AthletView, GroupSum] = mapToAvgRang(avgPerAthlet.map(rm => rm._1 -> (rm._2._1, rm._2._5)))
    lazy val avgDisziplinRangMap = avgPerAthlet.foldLeft(Map[Disziplin, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen, gsum)) = x
      disziplinwertungen.foldLeft(acc){(accc, disziplin) =>
        accc.updated(disziplin._1, accc.getOrElse(
                     disziplin._1, Map[AthletView, (Resultat, Resultat)]()).updated(athlet, (disziplin._3, disziplin._4)))
      }
    }.map { d => (d._1 -> mapToAvgRang(d._2)) }
    lazy val avgProgrammRangMap = avgPerAthlet.foldLeft(Map[ProgrammView, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen, gsum)) = x
      programmwertungen.foldLeft(acc){(accc, programm) =>
        accc.updated(programm._1, accc.getOrElse(
                     programm._1, Map[AthletView, (Resultat, Resultat)]()).updated(athlet, (programm._2, programm._3)))
      }
    }.map { d => (d._1 -> mapToAvgRang(d._2)) }

    val teilnehmer = avgPerAthlet.size

    def mapToGroupSum(
        athlet: AthletView,
        disziplinResults: Iterable[(Disziplin, Long, Resultat, Resultat, Option[Int], Option[BigDecimal])],
        programmResults: Iterable[(ProgrammView, Resultat, Resultat, Option[Int], Option[BigDecimal])]): IndexedSeq[LeafRow] = {

      if(groups.size == 1) {
        val dr = disziplinResults.map{w =>
          val ww = avgDisziplinRangMap(w._1)(athlet)
          val rang = ww.rang
          //val posproz = 100d * ww.rang.endnote / avgDisziplinRangMap.size
          if(anzahWettkaempfe > 1) {
            LeafRow(w._1.name,
              ww.avg,
              rang,
              rang.endnote == 1)
          }
          else {
            LeafRow(w._1.name,
              ww.sum,
              rang,
              rang.endnote == 1)
          }
        }
          .filter(_.sum.endnote > 0)
          .toIndexedSeq
        groups.head._2.toIndexedSeq.map{d =>
          dr.find(lr => lr.title == d.name) match {
            case Some(lr) => lr
            case None => LeafRow(d.name, Resultat(0,0,0), Resultat(0,0,0), auszeichnung = false)
          }
        }.distinct
      }
      else {
        programmResults.map{w =>
          val ww = avgProgrammRangMap(w._1)(athlet)
//          val posproz = 100d * ww.rang.endnote / avgProgrammRangMap.size
          if(anzahWettkaempfe > 1) {
            LeafRow(w._1.easyprint,
              ww.avg,
              ww.rang,
              ww.rang.endnote < 4 && ww.rang.endnote >= 1)
          }
          else {
            LeafRow(w._1.easyprint,
              ww.sum,
              ww.rang,
              ww.rang.endnote < 4 && ww.rang.endnote >= 1)
          }
        }.filter(_.sum.endnote >= 1).toIndexedSeq
      }
    }

    val prepared = avgPerAthlet.map {x =>
      val (athlet, (sum, avg, wd, wp, gsum)) = x
      val gsrang = rangMap(athlet)
      val posproz = 100d * gsrang.rang.endnote / teilnehmer
      val posprom = 10000d * gsrang.rang.endnote / teilnehmer
      val gs = mapToGroupSum(athlet, wd, wp)
      val divider = if(withDNotes || gs.isEmpty) 1 else gs.count{r => r.sum.endnote > 0}

      GroupRow(athlet, gs, avg, gsrang.rang,
          gsrang.rang.endnote > 0
          && (gsrang.rang.endnote < 4
              || (wp.head._4 match {
                case Some(auszeichnung) =>
                  if(auszeichnung > 100) {
                    val ret = posprom <= auszeichnung
                    ret
                  }
                  else {
                    val ret = posproz <= auszeichnung
                    ret
                  }
                case None               => false})
              || (wp.head._5 match {
                case Some(auszeichnung) =>
                  gsrang.sum.endnote / divider >= auszeichnung
                case None               => false})))

    }.toList
    if(sortAlphabetically) {
      prepared.sortBy(_.athlet.easyprint)
    }
    else{
      prepared/*.filter(_.sum.endnote > 0)*/.sortBy(_.rang.endnote)
    }
  }
}

case class GroupNode(override val groupKey: DataObject, next: Iterable[GroupSection]) extends GroupSection {
  override val sum: Resultat = next.map(_.sum).reduce(_ + _)
  override val avg: Resultat = next.map(_.avg).reduce(_ + _) / next.size
  override def easyprint = {
    val buffer = new mutable.StringBuilder()
    buffer.append(groupKey.easyprint).append("\n")
    for (gi <- next) {
      buffer.append(gi.easyprint).append("\n")
    }
    buffer.toString
  }
}
