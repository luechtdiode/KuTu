package ch.seidel

import scala.collection.mutable.StringBuilder
import scala.math.BigDecimal.int2bigDecimal
import javafx.scene.{control => jfxsc}
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control.TableColumn.CellDataFeatures
import ch.seidel.domain._

object GroupSection {
  def programGrouper( w: WertungView): ProgrammView = w.wettkampfdisziplin.programm.aggregatorSubHead
  def disziplinGrouper( w: WertungView): (Int, Disziplin) = (w.wettkampfdisziplin.ord, w.wettkampfdisziplin.disziplin)
  def groupWertungList(list: Iterable[WertungView]) = {
    val groups = list.filter(_.endnote > 0).groupBy(programGrouper).map { pw =>
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
      val rd = if (rangD.size > 1) rangD.indexOf(r.noteD) + 1 else 0
      val re = if (rangE.size > 1) rangE.indexOf(r.noteE) + 1 else 0
      val rf = if (rangEnd.size > 1) rangEnd.indexOf(r.endnote) + 1 else 0
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

case class GroupLeaf(override val groupKey: DataObject, list: Iterable[WertungView]) extends GroupSection {
  override val sum: Resultat = list.map(_.resultat).reduce(_+_)
  override val avg: Resultat = sum / list.size
  override def easyprint = groupKey.easyprint + s" $sum, $avg"
  val groups = GroupSection.groupWertungList(list).filter(_._2.size > 0)
  lazy val anzahWettkaempfe = list.filter(_.endnote > 0).groupBy { w => w.wettkampf }.size // Anzahl Wettkämpfe

  def buildColumns: List[jfxsc.TableColumn[GroupRow, _]] = {
    val athletCols: List[jfxsc.TableColumn[GroupRow, _]] = List(
      new TableColumn[GroupRow, String] {
        text = "Rang"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "rang", {
            if(x.value.auszeichnung) f"${x.value.rang.endnote}%3.0f *" else f"${x.value.rang.endnote}%3.0f"
          })
        }
        prefWidth = 20
        styleClass += "data"
      },
      new TableColumn[GroupRow, String] {
        text = "Athlet"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "athlet", {
            val a = x.value.athlet
            f"${a.vorname} ${a.name}"
          })
        }
        prefWidth = 90
        styleClass += "data"
      },
      new TableColumn[GroupRow, String] {
        text = "Verein"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "verein", {
            val a = x.value.athlet
            s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
          })
        }
        prefWidth = 90
        styleClass += "data"
      }
    )
    val indexer = Iterator.from(0)
    val disziplinCol: List[jfxsc.TableColumn[GroupRow, _]] =
      if (groups.keySet.size > 1) {
        // Mehrere "Programme" => pro Gruppenkey eine Summenspalte bilden
        groups.toList.sortBy(gru => gru._1.ord).map { gr =>
          val (grKey, disziplin) = gr

          def colsum(x: CellDataFeatures[GroupRow, String]) =
            x.value.resultate.find { x =>
              x.title.equals(grKey.easyprint) }.getOrElse(
                  LeafRow(grKey.easyprint, Resultat(0, 0, 0), Resultat(0, 0, 0), false))

          val clDnote = new TableColumn[GroupRow, String] {
            text = "D"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "dnote", {
                val cs = colsum(x)
             		val best = if(cs.sum.noteD > 0 && cs.rang.noteD.toInt == 1) "*" else ""
                best + cs.sum.formattedD
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEnote = new TableColumn[GroupRow, String] {
            text = "E"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "enote", {
                val cs = colsum(x)
                val best = if(cs.sum.noteE > 0 && cs.rang.noteE.toInt == 1) "*" else ""
                best + cs.sum.formattedE
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEndnote = new TableColumn[GroupRow, String] {
            text = "Endnote"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "endnote", {
                val cs = colsum(x)
                val best = if(cs.sum.endnote > 0 && cs.rang.endnote.toInt == 1) "*" else ""
                best + cs.sum.formattedEnd
              })
            }
            prefWidth = 60
            styleClass += "valuedata"
          }
          val clRang = new TableColumn[GroupRow, String] {
            text = "Rang"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "rang", {
                val cs = colsum(x)
                cs.rang.formattedEnd
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val cl: jfxsc.TableColumn[GroupRow, _] = new TableColumn[GroupRow, String] {
            if(anzahWettkaempfe > 1) {
              text = grKey.easyprint + s" (ø aus $anzahWettkaempfe)"
            }
            else {
              text = grKey.easyprint
            }
            prefWidth = 240
            columns ++= Seq(
              clDnote,
              clEnote,
              clEndnote,
              clRang
            )
          }
          cl
        }
      }
      else {
        groups.head._2.map { disziplin =>
          val index = indexer.next
          val clDnote = new TableColumn[GroupRow, String] {
            text = "D"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "dnote", {
                if (x.value.resultate.size > index) {
                  val best = if (x.value.resultate(index).sum.noteD > 0
                              && x.value.resultate.size > index
                              && x.value.resultate(index).rang.noteD.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + x.value.resultate(index).sum.formattedD
                } else ""
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEnote = new TableColumn[GroupRow, String] {
            text = "E"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "enote", {
                if (x.value.resultate.size > index) {
                  val best = if (x.value.resultate(index).sum.noteE > 0
                              && x.value.resultate.size > index
                              && x.value.resultate(index).rang.noteE.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + x.value.resultate(index).sum.formattedE
                } else ""
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEndnote = new TableColumn[GroupRow, String] {
            text = "Endnote"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "endnote", {
                if (x.value.resultate.size > index) {
                  val best = if (x.value.resultate(index).sum.endnote > 0
                              && x.value.resultate.size > index
                              && x.value.resultate(index).rang.endnote.toInt == 1)
                                  "*"
                             else
                                  ""
                  best + x.value.resultate(index).sum.formattedEnd
                } else ""
              })
            }
            prefWidth = 60
            styleClass += "valuedata"
          }
          val clRang = new TableColumn[GroupRow, String] {
            text = "Rang"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "rang", {
                if (x.value.resultate.size > index) f"${x.value.resultate(index).rang.endnote}%3.0f" else ""
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val cl: jfxsc.TableColumn[GroupRow, _] = new TableColumn[GroupRow, String] {
            if(anzahWettkaempfe > 1) {
              text =  disziplin.name + s" (ø aus $anzahWettkaempfe)"
            }
            else {
              text = disziplin.name
            }
            prefWidth = 240
            columns ++= Seq(
              clDnote,
              clEnote,
              clEndnote,
              clRang
            )
          }
          cl
        }.toList
      }
    val sumCol: List[jfxsc.TableColumn[GroupRow, _]] = List(
      new TableColumn[GroupRow, String] {
        if(anzahWettkaempfe > 1) {
          text = s"Total D (ø aus $anzahWettkaempfe)"
        }
        else {
          text = "Total D"
        }
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedD) }
        prefWidth = 80
        styleClass += "hintdata"
      },
      new TableColumn[GroupRow, String] {
        if(anzahWettkaempfe > 1) {
          text = s"Total E (ø aus $anzahWettkaempfe)"
        }
        else {
          text = "Total E"
        }
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedE) }
        prefWidth = 80
        styleClass += "hintdata"
      },
      new TableColumn[GroupRow, String] {
        if(anzahWettkaempfe > 1) {
          text = s"Total Punkte (ø aus $anzahWettkaempfe)"
        }
        else {
          text = "Total Punkte"
        }
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedEnd) }
        prefWidth = 80
        styleClass += "valuedata"
      }
    )
    athletCols ++ disziplinCol ++ sumCol
  }

  def getTableData = {
    def mapToRang(athlWertungen: Iterable[WertungView]) = {
      val grouped = athlWertungen.groupBy { _.athlet }.map { x =>
        val r = x._2.map(y => y.resultat).reduce(_+_)
        (x._1, r, r / x._2.size)
      }
      GroupSection.mapRang(grouped).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
    }
    def mapToAvgRang[A <: DataObject](grp: Iterable[(A, (Resultat, Resultat))]) = {
      GroupSection.mapAvgRang(grp.map { d => (d._1, d._2._1, d._2._2) }).map(r => (r.groupKey.asInstanceOf[A] -> r)).toMap
    }
    def mapToAvgRowSummary(athlWertungen: Iterable[WertungView]): (Resultat, Resultat, Iterable[(Disziplin, Resultat, Resultat, Option[Int])], Iterable[(ProgrammView, Resultat, Resultat, Option[Int])]) = {
      val wks = athlWertungen.filter(_.endnote > 0).groupBy { w => w.wettkampf }
      val wksums = wks.map {wk => wk._2.map(w => w.resultat).reduce(_+_)}
      val sum = if(wksums.nonEmpty) wksums.reduce(_+_) else Resultat(0,0,0)
      val avg = if(wksums.nonEmpty) sum / wksums.size else Resultat(0,0,0)
      val auszeichnung = if(wks.size == 1) Some(wks.head._1.auszeichnung) else None
      val perDisziplinAvgs = (for {
        wettkampf <- wks.keySet.toSeq
        ((ord, disziplin), dwertungen) <- wks(wettkampf).groupBy { x => (x.wettkampfdisziplin.ord, x.wettkampfdisziplin.disziplin) }
      }
      yield {
        val dsums = dwertungen.map {w => w.resultat}
        val dsum = if(dsums.nonEmpty) dsums.reduce(_+_) else Resultat(0,0,0)
        ((ord, disziplin) -> dsum)
      }).groupBy(_._1).map{x =>
        val sum = x._2.map(_._2).reduce(_+_)
        (x._1, sum, sum / x._2.size, auszeichnung)}
      .toList.sortBy(d => d._1._1)
      .map(d => (d._1._2, d._2, d._3, d._4)).toIterable

      val perProgrammAvgs = (for {
        wettkampf <- wks.keySet.toSeq
        (programm, pwertungen) <- wks(wettkampf).groupBy { _.wettkampfdisziplin.programm.aggregatorSubHead }
        psums = pwertungen.map {w => w.resultat}
        psum = if(psums.nonEmpty) psums.reduce(_+_) else Resultat(0,0,0)
      }
      yield {
        (programm, psum)
      }).groupBy(_._1).map{x =>
        val sum = x._2.map(_._2).reduce(_+_)
        (x._1, sum, sum / x._2.size, auszeichnung)}
      .toList.sortBy(d => d._1.ord)

      (sum, avg, perDisziplinAvgs, perProgrammAvgs)
    }

    val avgPerAthlet = list.groupBy { x =>
      x.athlet
    }.map { x =>
      (x._1, mapToAvgRowSummary(x._2))
    }.filter(_._2._1.endnote > 0)

    // Beeinflusst die Total-Rangierung pro Gruppierung
    val rangMap: Map[AthletView, GroupSum] = mapToAvgRang(avgPerAthlet.map(rm => rm._1 -> (rm._2._1, rm._2._2)))
    lazy val avgDisziplinRangMap = avgPerAthlet.foldLeft(Map[Disziplin, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen)) = x
      disziplinwertungen.foldLeft(acc){(accc, disziplin) =>
        accc.updated(disziplin._1, accc.getOrElse(
                     disziplin._1, Map[AthletView, (Resultat, Resultat)]()).updated(athlet, (disziplin._2, disziplin._3)))
      }
    }.map { d => (d._1 -> mapToAvgRang(d._2)) }
    lazy val avgProgrammRangMap = avgPerAthlet.foldLeft(Map[ProgrammView, Map[AthletView, (Resultat, Resultat)]]()){(acc, x) =>
      val (athlet, (sum, avg, disziplinwertungen, programmwertungen)) = x
      programmwertungen.foldLeft(acc){(accc, programm) =>
        accc.updated(programm._1, accc.getOrElse(
                     programm._1, Map[AthletView, (Resultat, Resultat)]()).updated(athlet, (programm._2, programm._3)))
      }
    }.map { d => (d._1 -> mapToAvgRang(d._2)) }

    val teilnehmer = avgPerAthlet.size

    def mapToGroupSum(
        athlet: AthletView,
        disziplinResults: Iterable[(Disziplin, Resultat, Resultat, Option[Int])],
        programmResults: Iterable[(ProgrammView, Resultat, Resultat, Option[Int])]): IndexedSeq[LeafRow] = {

      if(groups.size == 1) {
        disziplinResults.map{w =>
          val ww = avgDisziplinRangMap(w._1)(athlet)
          val rang = ww.rang
          val posproz = 100d * rang.endnote / avgDisziplinRangMap.size
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
        }.filter(_.sum.endnote >= 1).toIndexedSeq
      }
      else {
        programmResults.map{w =>
          val ww = avgProgrammRangMap(w._1)(athlet)
          val posproz = 100d * ww.rang.endnote / avgProgrammRangMap.size
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

    avgPerAthlet.map {x =>
      val (athlet, (sum, avg, wd, wp)) = x
      val gsrang = rangMap(athlet)
      val posproz = 100d * gsrang.rang.endnote / teilnehmer
      GroupRow(athlet, mapToGroupSum(athlet, wd, wp), avg, gsrang.rang,
          gsrang.rang.endnote > 0
          && (gsrang.rang.endnote < 4
              || (wp.head._4 match {
                case Some(auszeichnung) => posproz <= auszeichnung
                case None               => false})))

    }.toList.filter(_.sum.endnote > 0).sortBy(_.rang.endnote)
  }
}

case class GroupNode(override val groupKey: DataObject, next: Iterable[GroupSection]) extends GroupSection {
  override val sum: Resultat = next.map(_.sum).reduce((r1, r2) => r1 + r2)
  override val avg: Resultat = next.map(_.avg).reduce((r1, r2) => r1 + r2) / next.size
  override def easyprint = {
    val buffer = new StringBuilder()
    buffer.append(groupKey.easyprint).append("\n")
    for (gi <- next) {
      buffer.append(gi.easyprint).append("\n")
    }
    buffer.toString
  }
}
