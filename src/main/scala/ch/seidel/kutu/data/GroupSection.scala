package ch.seidel.kutu.data

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import ch.seidel.kutu.domain._

import scala.collection.mutable

object GroupSection {
  val STANDARD_SCORE_FACTOR = BigDecimal("100000000000000000000000000000000000000000000000000")
  def programGrouper( w: WertungView): ProgrammView = w.wettkampfdisziplin.programm.aggregatorSubHead
  def disziplinGrouper( w: WertungView): (Int, Disziplin) = (w.wettkampfdisziplin.ord, w.wettkampfdisziplin.disziplin)
  def groupWertungList(list: Iterable[WertungView]) = {
    val groups = list.filter(w => w.showInScoreList).groupBy(programGrouper).map { pw =>
      (pw._1 -> pw._2.map(disziplinGrouper).toSet[(Int, Disziplin)].toList.sortBy{ d =>
        d._1 }.map(_._2))
    }
    groups
  }

  def mapAvgRang(list: Iterable[(DataObject, Resultat, Resultat)]) = {
    val rangD = (0 +: list.toList.map(_._3.noteD).filter(_ != 0).distinct.sorted.reverse).zipWithIndex.toMap
    val rangE = (0 +: list.toList.map(_._3.noteE).filter(_ != 0).distinct.sorted.reverse).zipWithIndex.toMap
    val rangEnd = (0 +: list.toList.map(_._3.endnote).filter(_ != 0).sorted.distinct.reverse).zipWithIndex.toMap
    def rang(r: Resultat) = {
      val rd = rangD.getOrElse(r.noteD, 0)
      val re = rangE.getOrElse(r.noteE, 0)
      val rf = rangEnd.getOrElse(r.endnote, 0)
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
object WKColValue {
  def apply(text: String):WKColValue = WKColValue(text, text, Seq.empty)
}
case class WKColValue(text: String, raw: String, styleClass: Seq[String]) extends DataObject {
  override def easyprint: String = text
}
case class WKLeafCol[T](override val text: String, override val prefWidth: Int, colspan: Int = 1, override val styleClass: Seq[String], valueMapper: T => WKColValue) extends WKCol
case class WKGroupCol(override val text: String, override val prefWidth: Int, colspan: Int = 1, override val styleClass: Seq[String], cols: Seq[WKCol]) extends WKCol

case class GroupLeaf[GK <: DataObject](override val groupKey: GK, list: Iterable[WertungView], diszs: List[Disziplin] = List(), aggreateFun: TeamAggreateFun = Sum, bestOfCountOverride: Option[Int] = None) extends GroupSection {
  val isTeamGroup = groupKey.isInstanceOf[Team]
  override val sum: Resultat = {
    groupKey match {
      //case gk: Team => gk.sum
      case _ => aggreateFun(list.map(_.resultat)) //.reduce(_+_)
    }
  }
  override val avg: Resultat = {
    groupKey match {
      case gk: Team => gk.wertungen.map(_.resultat).reduce(_ + _) / gk.wertungen.size
      case _ => sum / list.size
    }
  }
  override def easyprint = groupKey.easyprint + s" $sum, $avg"
  val groups = if (diszs.nonEmpty) {
    GroupSection.groupWertungList(list).map(t => (t._1, diszs))
  } else GroupSection.groupWertungList(list)
  lazy val anzahWettkaempfe = list.filter(_.endnote.nonEmpty).groupBy { w => w.wettkampf }.size // Anzahl Wettkämpfe
  val withDNotes = list.exists(w => w.noteD.sum > 0)
  val withENotes = list.exists(w => w.wettkampf.programmId != 1)
  val dNoteLabel = list.map(_.wettkampfdisziplin.notenSpez.getDifficultLabel).toList.distinct.sorted.mkString("/")
  val eNoteLabel = list.map(_.wettkampfdisziplin.notenSpez.getExecutionLabel).toList.distinct.sorted.mkString("/")
  val mostCountingGroup = groups.reduce((a, b) => if (a._2.size > b._2.size) a else b)
  val isDivided = !(withDNotes || groups.isEmpty)
  val divider = if(!isDivided) 1 else mostCountingGroup._2.size
  //val divider = if(!isDivided) 1 else groups.head._2.size

  val gleichstandsregel = Gleichstandsregel(list.head.wettkampf)
  def buildColumns(isAvgOnMultipleCompetitions: Boolean = true): List[WKCol] = {
    val athletCols: List[WKCol] = List(
      WKLeafCol[ResultRow](text = "Rang", prefWidth = 20, styleClass = Seq("data"), valueMapper = gr => {
        if (isTeamGroup) {
          WKColValue("", "", Seq())
        } else if(gr.auszeichnung) gr.rang.endnote.intValue match {
          case 1 =>  WKColValue(f"${gr.rang.endnote}%3.0f G", f"${gr.rang.endnote}%3.0f", Seq())
          case 2 =>  WKColValue(f"${gr.rang.endnote}%3.0f S", f"${gr.rang.endnote}%3.0f", Seq())
          case 3 =>  WKColValue(f"${gr.rang.endnote}%3.0f B", f"${gr.rang.endnote}%3.0f", Seq())
          case _ =>  WKColValue(f"${gr.rang.endnote}%3.0f *", f"${gr.rang.endnote}%3.0f", Seq())
        }
        else WKColValue(f"${gr.rang.endnote}%3.0f")
      }),
      if (isTeamGroup) {
        WKLeafCol[GroupRow](text = "Team", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
          val a = gr.athlet
          WKColValue(f"${a.vorname} ${a.name}")
        })
      } else {
        WKLeafCol[GroupRow](text = "Athlet", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
          val a = gr.athlet
          WKColValue(f"${a.vorname} ${a.name}")
        })
      },
      WKLeafCol[GroupRow](text = "Jahrgang", prefWidth = 10, styleClass = Seq("data"), valueMapper = gr => {
        val a = gr.athlet
        WKColValue(s"${AthletJahrgang(a.gebdat).jahrgang}")
      }),
      if (isTeamGroup) {
        WKLeafCol[GroupRow](text = "K", prefWidth = 10, styleClass = Seq("data"), valueMapper = gr => {
          WKColValue(gr.pgm.name)
        })
      } else {
        WKLeafCol[GroupRow](text = "Verein", prefWidth = 90, styleClass = Seq("data"), valueMapper = gr => {
          val a = gr.athlet
          WKColValue(s"${a.verein.map { _.name }.getOrElse("ohne Verein")}")
        })
      }
    )

    val (disziplinCol: List[WKCol], sumCol: List[WKCol]) = buildCommonColumns(isAvgOnMultipleCompetitions)
    athletCols ++ disziplinCol ++ sumCol
  }

  def buildCommonColumns(isAvgOnMultipleCompetitions: Boolean): (List[WKCol], List[WKCol]) = {
    val indexer = Iterator.from(0)
    val disziplinCol: List[WKCol] =
      if (!isTeamGroup && groups.keySet.size > 1) {
        // Mehrere "Programme" => pro Gruppenkey eine Summenspalte bilden
        groups.toList.sortBy(gru => gru._1.ord).map { gr =>
          val (grKey, disziplin) = gr

          def colsum(gr: ResultRow) =
            gr.resultate.find { x =>
              x.title.equals(grKey.easyprint)
            }.getOrElse(
              LeafRow(grKey.easyprint, Resultat(0, 0, 0), Resultat(0, 0, 0), auszeichnung = false, streichwert = false))

          val clDnote = WKLeafCol[ResultRow](text = dNoteLabel, prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
            val best = if (cs.sum.noteD > 0 && cs.rang.noteD.toInt == 1) "*" else ""
            val styles = if (cs.sum.noteD > 0 && cs.rang.noteD.toInt == 1) Seq("best") else Seq()
            val value = cs.sum.formattedD
            WKColValue(best + value, value, styles)
          })
          val clEnote = WKLeafCol[ResultRow](text = if (withDNotes) eNoteLabel else "ø Gerät", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
            val best = if (cs.sum.noteE > 0 && cs.rang.noteE.toInt == 1) "*" else ""
            val styles = if (cs.sum.noteE > 0 && cs.rang.noteE.toInt == 1) Seq("best") else Seq()
            val div = Math.max(gr.divider, divider)
            val value = if (div == 1) cs.sum.formattedE else (cs.sum / div).formattedEnd
            WKColValue(best + value, value, styles)
          })
          val clEndnote = WKLeafCol[ResultRow](text = "Endnote", prefWidth = 60, styleClass = Seq("valuedata"), valueMapper = gr => {
            val cs = colsum(gr)
            val raw = cs.sum.formattedEnd.trim
            val styles = (if (cs.auszeichnung) Seq("best") else Seq()) ++ (if (cs.streichwert) Seq("stroke") else Seq())
            val best = if (cs.auszeichnung) "*" else ""
            val streich = if (cs.streichwert) ("(", ")") else ("", "")
            WKColValue(best + streich._1 + raw + streich._2, raw, styles)
          })
          val clRang = WKLeafCol[ResultRow](text = "Rang", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            val cs = colsum(gr)
            WKColValue(cs.rang.formattedEnd)
          })
          val cl: WKCol = WKGroupCol(
            text = if (isAvgOnMultipleCompetitions && anzahWettkaempfe > 1) {
              s"ø aus " + grKey.easyprint
            }
            else {
              grKey.easyprint
            }
            , prefWidth = 240, styleClass = Seq("hintdata"), cols = {
              val withDNotes = list.exists(w => w.noteD.sum > 0)
              if (withDNotes) {
                Seq(clDnote, clEnote, clEndnote, clRang)
              }
              else if (withENotes) {
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
        mostCountingGroup._2.map { disziplin =>
          val index = indexer.next()
          lazy val clDnote = WKLeafCol[ResultRow](text = dNoteLabel, prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
              val styles = if (gr.resultate(index).sum.noteD > 0&& gr.resultate(index).rang.noteD.toInt == 1) Seq("best") else Seq()
              val best = if (gr.resultate(index).sum.noteD > 0
                && gr.resultate(index).rang.noteD.toInt == 1)
                "*"
              else
                ""
              val value = gr.resultate(index).sum.formattedD
              WKColValue(best + value, value, styles)

            } else WKColValue("")
          })
          lazy val clEnote = WKLeafCol[ResultRow](text = eNoteLabel, prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
              val styles = if (gr.resultate(index).sum.noteE > 0&& gr.resultate(index).rang.noteE.toInt == 1) Seq("best") else Seq()
              val best = if (gr.resultate(index).sum.noteE > 0
                && gr.resultate(index).rang.noteE.toInt == 1)
                "*"
              else
                ""
              val value = gr.resultate(index).sum.formattedE
              WKColValue(best + value, value, styles)
            } else WKColValue("")
          })
          lazy val clEndnote = WKLeafCol[ResultRow](text = "Endnote", prefWidth = 60, styleClass = Seq("valuedata"), valueMapper = gr => {
            if (gr.resultate.size > index) {
              val cs = gr.resultate(index)
              val raw = cs.sum.formattedEnd.trim
              val styles = (if (cs.auszeichnung) Seq("best") else Seq()) ++ (if (cs.streichwert) Seq("stroke") else Seq())
              val best = if (cs.auszeichnung) "*" else ""
              val streich = if (cs.streichwert) ("(", ")") else ("", "")
              WKColValue(best + streich._1 + raw + streich._2, raw, styles)
            } else WKColValue("")
          })
          lazy val clRang = WKLeafCol[ResultRow](text = "Rang", prefWidth = 60, styleClass = Seq("hintdata"), valueMapper = gr => {
            if (gr.resultate.size > index) WKColValue(f"${gr.resultate(index).rang.endnote}%3.0f") else WKColValue("")
          })
          val cl = WKGroupCol(text = if (isAvgOnMultipleCompetitions && anzahWettkaempfe > 1) {
            s"ø aus " + disziplin.name
          }
          else {
            disziplin.name
          }
            , prefWidth = 240, styleClass = Seq("valuedata"), cols = {
              if (withDNotes) {
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
      WKLeafCol[ResultRow](
        text = if (isAvgOnMultipleCompetitions && anzahWettkaempfe > 1) {
          s"Total ø aus $dNoteLabel"
        }
        else {
          s"Total $dNoteLabel"
        }
        , prefWidth = 80, styleClass = Seq("hintdata"), valueMapper = gr => {
          val value = gr.sum.formattedD
          val styles = if (gr.sum.noteD > 0 && gr.rang.noteD.toInt == 1) Seq("best") else Seq()
          val best = if (gr.sum.noteD > 0 ) "*" else ""
          WKColValue(best + value, value, styles)
        }
      ),
      WKLeafCol[ResultRow](
        text = if (isAvgOnMultipleCompetitions && anzahWettkaempfe > 1) {
          if (!isDivided && withDNotes) {
            s"Total ø aus $eNoteLabel"
          }
          else {
            "ø Gerät"
          }
        }
        else if (!isDivided && withDNotes) {
          s"Total $eNoteLabel"
        }
        else {
          "ø Gerät"
        }
        , prefWidth = 80, styleClass = Seq("hintdata"), valueMapper = gr => {
          val value: String = if (isTeamGroup && aggreateFun != Sum) {
            gr.avg.formattedEnd
          } else {
            val div = if (bestOfCountOverride.isEmpty || !isDivided && withDNotes) Math.max(gr.divider, divider) else Math.max(gr.divider, bestOfCountOverride.getOrElse(1))
            if (div < 2) {
              gr.sum.formattedE
            }
            else {
              (gr.sum / div).formattedEnd
            }
          }
          val styles = if (gr.sum.noteE > 0 && gr.rang.noteE.toInt == 1) Seq("best") else Seq()
          val best = if (gr.sum.noteE > 0 && gr.rang.noteE.toInt == 1) "*" else ""
          WKColValue(best + value, value, styles)
        }
      ),
      WKLeafCol[ResultRow](
        text = if (isAvgOnMultipleCompetitions && anzahWettkaempfe > 1) {
          s"Total ø Punkte"
        }
        else {
          "Total Punkte"
        }
        , prefWidth = 80, styleClass = Seq("valuedata"), valueMapper = gr => {
          WKColValue(gr.sum.formattedEnd)
        }
      )
    )

    val sumCol: List[WKCol] = List(withDNotes, isDivided || withDNotes, true).zip(sumColAll).filter(v => v._1).map(_._2)
    (disziplinCol, sumCol)
  }

  def mapToAvgRang[A <: DataObject](grp: Iterable[(A, (Resultat, Resultat))]) = {
    GroupSection.mapAvgRang(grp.map { d => (d._1, d._2._1, d._2._2) }).map(r => (r.groupKey.asInstanceOf[A] -> r)).toMap
  }

  def mapToBestOfCounting(wertungen: Iterable[WertungView]): Iterable[WertungView] = {
    if (wertungen.isEmpty || (wertungen.head.wettkampfdisziplin.programm.bestOfCount < 1 && bestOfCountOverride.isEmpty))
      wertungen
    else {
      val bestOfCount = bestOfCountOverride.getOrElse(wertungen.head.wettkampfdisziplin.programm.bestOfCount)
      val bestOfwertungen = wertungen.toList.sortBy(w => w.resultat.endnote).reverse.take(bestOfCount)
      wertungen.map{ w =>
        if (bestOfwertungen.contains(w)) {
          w
        } else {
          w.copy(isStroked = true)
        }
      }
    }
  }

  def mapToAvgRowSummary(athlWertungen: Iterable[WertungView] = list, avgSumsWithMultiCompetitions: Boolean): (Resultat, Resultat, Iterable[(Disziplin, Long, Resultat, Resultat, Option[Int], Option[BigDecimal])], Iterable[(ProgrammView, Resultat, Resultat, Option[Int], Option[BigDecimal])], Resultat) = {
    val wks = athlWertungen.filter(_.endnote.nonEmpty).groupBy { w => w.wettkampf }.map{
      case (g,l) => (g, mapToBestOfCounting(l))
    }

    val wksums = wks.map { wk => aggreateFun(wk._2.filter(!_.isStroked).map(w => w.resultat)) }.toList
    val rsum = aggreateFun(wksums)

    val gwksums = wks.map { wk =>
      val countingWertungen = (if (bestOfCountOverride.isEmpty) wk._2.filter(!_.isStroked) else wk._2).toList
      val factorShift = gleichstandsregel.factorize(countingWertungen)
      aggreateFun(
        countingWertungen.map { w =>
          if (anzahWettkaempfe > 1)
            w.resultat
          else {
            w.resultat * STANDARD_SCORE_FACTOR + factorShift
          }
        }
      ) * aggreateFun.sortFactor
    }
    val gsum = aggreateFun(gwksums) //if (gwksums.nonEmpty) gwksums.reduce(_ + _) else Resultat(0, 0, 0)
    val wkDivider = if(avgSumsWithMultiCompetitions) wksums.size else 1
    val avg = if (wksums.nonEmpty) rsum / wkDivider else Resultat(0, 0, 0)
    val gavg = if (wksums.nonEmpty) gsum / wkDivider else Resultat(0, 0, 0)
    val withAuszeichnung = anzahWettkaempfe == 1 && groups.size == 1 && wks.size == 1
    val auszeichnung = if (withAuszeichnung) Some(wks.head._1.auszeichnung) else None
    val auszeichnungEndnote = if (withAuszeichnung && wks.head._1.auszeichnungendnote > 0) Some(wks.head._1.auszeichnungendnote) else None
    val perDisziplinAvgs = (for {
      wettkampf <- wks.keySet.toSeq
      ((ord, disziplin), dwertungen) <- wks(wettkampf).groupBy { x => (x.wettkampfdisziplin.ord, x.wettkampfdisziplin.disziplin) }
    }
    yield {
      val prog = dwertungen.head.wettkampf.programmId
      val dsums = dwertungen.map { w => w.resultat }
      val dsum = aggreateFun(dsums)// if (dsums.nonEmpty) dsums.reduce(_ + _) else Resultat(0, 0, 0)
      ((ord, disziplin, prog) -> dsum)
    }).groupBy(_._1).map { x =>
      val xsum = x._2.map(_._2).reduce(_ + _)
      val xasum = aggreateFun(x._2.map(_._2))
      val diszDivider = if(avgSumsWithMultiCompetitions) x._2.size else 1
      (x._1, xasum, xsum / diszDivider, auszeichnung, auszeichnungEndnote)
    }
      .toList.sortBy(d => d._1._1)
      .map(d => (d._1._2, d._1._3, d._2, d._3, d._4, d._5))

    val perProgrammAvgs = (for {
      wettkampf <- wks.keySet.toSeq
      (programm, pwertungen) <- wks(wettkampf).groupBy {
        _.wettkampfdisziplin.programm.aggregatorSubHead
      }
      psums = pwertungen.map { w => w.resultat }
      psum = aggreateFun(psums) //if (psums.nonEmpty) psums.reduce(_ + _) else Resultat(0, 0, 0)
    }
    yield {
      (programm, psum)
    }).groupBy(_._1).map { x =>
      val xsum = x._2.map(_._2).reduce(_ + _)
      val xasum = aggreateFun(x._2.map(_._2))
      val pgmDivider = if(avgSumsWithMultiCompetitions) x._2.size else 1
      (x._1, xasum, xsum / pgmDivider, auszeichnung, auszeichnungEndnote)
    }
      .toList.sortBy(d => d._1.ord)
    (rsum, avg, perDisziplinAvgs, perProgrammAvgs, gavg)
  }

  def getTableData(sortAlphabetically: Boolean = false, avgSumsWithMultiCompetitions: Boolean = true) = {
    val avgPerAthlet = list.groupBy { x =>
      x.athlet
    }.map { x =>
      (x._1, mapToAvgRowSummary(x._2, avgSumsWithMultiCompetitions), x._2.head.wettkampfdisziplin.programm)
    }.filter(_._2._1.endnote > 0)

    // Beeinflusst die Total-Rangierung pro Gruppierung
    val rangMap: Map[AthletView, GroupSum] = mapToAvgRang(avgPerAthlet.map(rm => rm._1 -> (rm._2._1, rm._2._5)))
    lazy val avgDisziplinRangMap = avgPerAthlet.foldLeft(Map[Disziplin, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen, gsum), prg) = x
      disziplinwertungen.foldLeft(acc){(accc, disziplin) =>
        accc.updated(disziplin._1, accc.getOrElse(
                     disziplin._1, Map[AthletView, (Resultat, Resultat)]()).updated(athlet, (disziplin._3, disziplin._4)))
      }
    }.map { d => (d._1 -> mapToAvgRang(d._2)) }
    lazy val avgProgrammRangMap = avgPerAthlet.foldLeft(Map[ProgrammView, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen, gsum), prg) = x
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

      if(groups.size == 1 || isTeamGroup) {
        val dr = disziplinResults.map{w =>
          val ww = avgDisziplinRangMap(w._1)(athlet)
          val rang = ww.rang
          if (isTeamGroup) {
            val team = groupKey.asInstanceOf[Team]
            LeafRow(w._1.easyprint,
              ww.avg,
              ww.rang,
              ww.rang.endnote == 1,
              !team.isRelevantResult(w._1, ww.groupKey.asInstanceOf[AthletView]))
          }
          else if(avgSumsWithMultiCompetitions && anzahWettkaempfe > 1) {
            LeafRow(w._1.name,
              ww.avg,
              rang,
              rang.endnote == 1,
              ww.avg.isStreichwertung)
          }
          else {
            LeafRow(w._1.name,
              ww.sum,
              rang,
              rang.endnote == 1,
              ww.sum.isStreichwertung)
          }
        }
        .filter(_.sum.endnote > 0)
        .toIndexedSeq
        groups.head._2.toIndexedSeq.map{d =>
          dr.find(lr => lr.title == d.name) match {
            case Some(lr) => lr
            case None => LeafRow(d.name, Resultat(0,0,0), Resultat(0,0,0), auszeichnung = false, streichwert = false)
          }
        }.distinct
      }
      else {
        programmResults.map{w =>
          val ww = avgProgrammRangMap(w._1)(athlet)
//          val posproz = 100d * ww.rang.endnote / avgProgrammRangMap.size
          if(avgSumsWithMultiCompetitions && anzahWettkaempfe > 1) {
            LeafRow(w._1.easyprint,
              ww.avg,
              ww.rang,
              ww.rang.endnote < 4 && ww.rang.endnote >= 1,
              ww.avg.isStreichwertung)
          }
          else {
            LeafRow(w._1.easyprint,
              ww.sum,
              ww.rang,
              ww.rang.endnote < 4 && ww.rang.endnote >= 1,
              ww.sum.isStreichwertung)
          }
        }.filter(_.sum.endnote >= 1).toIndexedSeq
      }
    }

    val prepared = avgPerAthlet.map {x =>
      val (athlet, (sum, avg, wd, wp, gsum), pgm) = x
      val gsrang = rangMap(athlet)
      val posproz = 100d * gsrang.rang.endnote / teilnehmer
      val posprom = 10000d * gsrang.rang.endnote / teilnehmer
      val gs = mapToGroupSum(athlet, wd, wp)
      val divider = if(withDNotes || gs.isEmpty) 1 else gs.count{r => !r.streichwert && r.sum.endnote > 0}

      GroupRow(athlet, pgm, gs, avg, gsrang.rang,
          gsrang.rang.endnote > 0
          && !isTeamGroup && (gsrang.rang.endnote < 4
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
//    else if(avgSumsWithMultiCompetitions) {
//    }
    else{
      prepared.sortBy(_.rang.endnote)
      //prepared.sortBy(_.sum.endnote).reverse
    }
  }
}

object TeamSums {
  def apply(teamRows: GroupLeaf[_]): List[TeamSums] = {
    val wkCnt = teamRows.list.map(w => w.wettkampf).toSet.size
    if (wkCnt > 1) {
      List[TeamSums]()
    } else {
      val wk = teamRows.list.map(w => w.wettkampf).head
      val teamregel = TeamRegel(wk)
      if (teamregel.teamsAllowed) {
        teamregel.extractTeams(teamRows.list)
          .groupBy(_.rulename).filter(_._2.size > 1)
          .flatMap {
            case (_, teams) =>
              val diszs = teams.flatMap(_.diszList).distinct
                .groupBy(_._1).toList.sortBy(_._2.head._2).map(_._2.head._1)
              val tms = teams.filter(team => team.wertungen.nonEmpty).map { team =>
                GroupLeaf(team, team.wertungen, diszs, team.aggregateFun)
              }
              if (tms.isEmpty) None else Some(TeamSums(teamRows.groupKey.asInstanceOf[DataObject], tms))
          }
          .toList
      } else List[TeamSums]()
    }
  }

}
case class TeamSums(override val groupKey: DataObject, teamRows: List[GroupLeaf[Team]]) extends GroupSection {
  /**
   * used to filter teams in scores, only if they've results. Therefore, the aggregateFn isn't relevant for that.
   */
  override val sum: Resultat = teamRows.map(_.sum).reduce(_ + _)
  override val avg: Resultat = sum / teamRows.size

  def getTeam(gl: GroupLeaf[Team]) = gl.groupKey
  def getTeamGroupLeaf(team: Team): GroupLeaf[Team] = teamRows.find(t => t.groupKey.equals(team)).get

  private val glGroup: GroupLeaf[Team] = teamRows.reduce((a, b) => if (a.divider > b.divider) a else b)
  def buildColumns: List[WKCol] = {
    val teamCols: List[WKCol] = List(
      WKLeafCol[TeamRow](text = "Rang", prefWidth = 20, styleClass = Seq("data"), valueMapper = gr => {
        WKColValue(if (gr.auszeichnung) gr.rang.endnote.intValue match {
          case 1 => f"${gr.rang.endnote}%3.0f G"
          case 2 => f"${gr.rang.endnote}%3.0f S"
          case 3 => f"${gr.rang.endnote}%3.0f B"
          case _ => f"${gr.rang.endnote}%3.0f *"
        }
        else f"${gr.rang.endnote}%3.0f")
      }),
      WKLeafCol[TeamRow](text = "Team/Athlet", prefWidth = 90, colspan = 3, styleClass = Seq("heading"), valueMapper = gr => {
        WKColValue(s"${gr.team.name}")
      }),
      WKLeafCol[TeamRow](text = "Jahrgang", prefWidth = 90, colspan = 0, styleClass = Seq("data"), valueMapper = gr => {
        WKColValue("")
      }),
      WKLeafCol[TeamRow](text = "K", prefWidth = 90, colspan = 0, styleClass = Seq("data"), valueMapper = gr => {
        WKColValue("")
      })
    )
    val (disziplinCol: List[WKCol], sumCol: List[WKCol]) = glGroup.buildCommonColumns(true)
    teamCols ++ disziplinCol ++ sumCol
  }

  def getTableData(): List[TeamRow] = {
    val avgPerTeams = teamRows.map { x =>
      (getTeam(x), x)
    }.map { x =>
      val (team, teamRow) = x
      val teamwertungen = team.countingWertungen.flatMap(_._2)
      (team, teamRow.mapToAvgRowSummary(teamwertungen, avgSumsWithMultiCompetitions = true))
    }

    (for (teamRuleGroup <- avgPerTeams.groupBy(_._1.rulename)) yield {
      val (rulename, avgPerTeam) = teamRuleGroup
      val rangMap: Map[Team, GroupSum] = glGroup.mapToAvgRang(avgPerTeam.map(rm => rm._1 -> (rm._2._1, rm._2._5)))
      lazy val avgDisziplinRangMap = avgPerTeam.foldLeft(Map[Disziplin, Map[Team, (Resultat, Resultat)]]()) { (acc, x) =>
        val (team, (sum, avg, disziplinwertungen, programmwertungen, gsum)) = x
        disziplinwertungen.foldLeft(acc) { (accc, disziplin) =>
          accc.updated(disziplin._1, accc.getOrElse(
            disziplin._1, Map[Team, (Resultat, Resultat)]()).updated(team, (disziplin._3, disziplin._4)))
        }
      }.map { d => (d._1 -> glGroup.mapToAvgRang(d._2)) }

      def mapToTeamSum(team: Team, disziplinResults: Iterable[(Disziplin, Long, Resultat, Resultat, Option[Int], Option[BigDecimal])]): IndexedSeq[LeafRow] = {
        disziplinResults.map { w =>
          val ww = avgDisziplinRangMap(w._1)(team)
          LeafRow(w._1.easyprint,
            ww.avg,
            ww.rang,
            ww.rang.endnote == 1,
            ww.avg.isStreichwertung)
        }
          .toIndexedSeq
          .distinct
      }

      avgPerTeam.map { x =>
        val (team, (sum, teamTotalScore, wd, wp, gsum)) = x
        val gsrang = rangMap(team)
        val gs = mapToTeamSum(team, wd)

        TeamRow(team, gs, teamTotalScore, gsrang.rang,
          gsrang.rang.endnote > 0 && gsrang.rang.endnote < 4
        )

      }.sortBy(_.rang.endnote)
    }).flatten[TeamRow].toList
  }
  override def easyprint = s"Teams ${groupKey.easyprint} $sum, $avg"
}

case object GroupNode {
  def apply(groupKey: DataObject): GroupNode = {
    val dummyList = Seq(GroupSum(groupKey, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0)))
    GroupNode(groupKey, dummyList)
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
