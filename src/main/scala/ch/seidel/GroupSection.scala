package ch.seidel

import scala.collection.mutable.StringBuilder
import scala.math.BigDecimal.int2bigDecimal
import ch.seidel.domain.AthletView
import ch.seidel.domain.DataObject
import ch.seidel.domain.Disziplin
import ch.seidel.domain.GroupRow
import ch.seidel.domain.LeafRow
import ch.seidel.domain.Resultat
import ch.seidel.domain.WertungView
import javafx.scene.{control => jfxsc}
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import ch.seidel.domain.Disziplin
import ch.seidel.domain.ProgrammView
import scalafx.scene.control.TableColumn.CellDataFeatures

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
//    val avg = list.groupBy(_._1).map(w => (w._1, w._2.map(_._2).reduce(_+_) / w._2.size))
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
    val ret = list.map(y => GroupSum(y._1, y._2, y._3, rang(y._3)))
//    println(s"GroupSums size=${list.size} [")
//    println(ret.map(gs => gs.easyprint).mkString("\n"))
//    println("]")
    ret
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
  def mapToRang(fl: Iterable[WertungView]) = {
    val grouped = fl.groupBy { _.athlet }.map { x =>
      val r = x._2.map(y => y.resultat).reduce(_+_)
      (x._1, r, r / x._2.size)
    }
    GroupSection.mapRang(grouped).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
  }
  def mapToAvgRang(fl: Iterable[WertungView]) = {
    val grouped = fl.groupBy { _.athlet }.map { x =>
      val r = x._2.map(y => y.resultat).reduce(_+_)
      (x._1, r, r / x._2.size)
    }
    GroupSection.mapAvgRang(grouped).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
  }

  val groups = GroupSection.groupWertungList(list).filter(_._2.size > 0)

  lazy val athletRangMap = mapToRang(list)
  lazy val athletAvgRangMap = mapToAvgRang(list)
  lazy val athletDisziplinRangMap = list.groupBy(w => w.wettkampfdisziplin.disziplin.id).map { d => (d._1 -> mapToRang(d._2)) }
  lazy val athletProgrammRangMap = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead.id).map { d => (d._1 -> mapToRang(d._2)) }
  lazy val athletDisziplinAvgRangMap = list.groupBy(w => w.wettkampfdisziplin.disziplin.id).map { d => (d._1 -> mapToAvgRang(d._2)) }
  lazy val athletProgrammAvgRangMap = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead.id).map { d => (d._1 -> mapToAvgRang(d._2)) }

  def buildColumns: List[jfxsc.TableColumn[GroupRow, _]] = {
    val athletCols: List[jfxsc.TableColumn[GroupRow, _]] = List(
      new TableColumn[GroupRow, String] {
        text = "Rang"
        cellValueFactory = { x =>
          val w = new ReadOnlyStringWrapper(x.value, "rang", {
            if(x.value.auszeichnung) f"${x.value.rang.endnote}%3.0f *" else f"${x.value.rang.endnote}%3.0f"
            })
          w
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
            x.value.resultate.find { x => x.title.equals(grKey.easyprint) }.getOrElse(LeafRow(grKey.easyprint, Resultat(0, 0, 0), Resultat(0, 0, 0), false))

//          val disziplin = dis map (_._2)
          val clDnote = new TableColumn[GroupRow, String] {
            text = "D"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "dnote", {
                val cs = colsum(x)
             		val best = if(cs.rang.noteD.toInt == 1) "*" else ""
//                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0)))
//                val best = if (colsum.rang.noteD.toInt == 1) "*" else ""
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
                val best = if(cs.rang.noteE.toInt == 1) "*" else ""
//                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0)))
//                val best = if (colsum.rang.noteE.toInt == 1) "*" else ""
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
                val best = if(cs.rang.endnote.toInt == 1) "*" else ""
//                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0)))
//                val best = if (colsum.rang.endnote.toInt == 1) "*" else ""
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
//                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0)))
                cs.rang.formattedEnd
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val cl: jfxsc.TableColumn[GroupRow, _] = new TableColumn[GroupRow, String] {
            text = grKey.easyprint
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
//          val (ord, disziplin) = od
          val index = indexer.next
          val clDnote = new TableColumn[GroupRow, String] {
            text = "D"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "dnote", {
                val best = if (x.value.resultate.size > index && x.value.resultate(index).rang.noteD.toInt == 1) "*" else ""
                if (x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedD else ""
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEnote = new TableColumn[GroupRow, String] {
            text = "E"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "enote", {
                val best = if (x.value.resultate.size > index && x.value.resultate(index).rang.noteE.toInt == 1) "*" else ""
                if (x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedE else ""
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEndnote = new TableColumn[GroupRow, String] {
            text = "Endnote"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "endnote", {
                val best = if (x.value.resultate.size > index && x.value.resultate(index).rang.endnote.toInt == 1) "*" else ""
                if (x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedEnd else ""
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
            text = disziplin.name
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
        text = "Total D"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedD) }
        prefWidth = 80
        styleClass += "hintdata"
      },
      new TableColumn[GroupRow, String] {
        text = "Total E"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedE) }
        prefWidth = 80
        styleClass += "hintdata"
      },
      new TableColumn[GroupRow, String] {
        text = "Total Punkte"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedEnd) }
        prefWidth = 80
        styleClass += "valuedata"
      }
    )
    athletCols ++ disziplinCol ++ sumCol
  }
  def getTableData = {
    def mapToAvgRowSummary(athlWertungen: Iterable[WertungView]): (Resultat, Resultat) = {
      val wks = athlWertungen.filter(_.endnote > 0).groupBy { w => w.wettkampf.id }
      val wksums = wks.map {wk => wk._2.map(w => w.resultat).reduce(_+_)}
      val sum = if(wksums.nonEmpty) wksums.reduce(_+_) else Resultat(0,0,0)
      val avg = if(wksums.nonEmpty) sum / wksums.size else Resultat(0,0,0)
      (sum, avg)
    }
    def mapToGroupSum(athlWertungen: Iterable[WertungView]): IndexedSeq[LeafRow] = {
      val multiwettkampf = athlWertungen.map(_.wettkampf.id).toSet.size > 1
      if(groups.size == 1) {
        athlWertungen.map{w =>
          if(multiwettkampf) {
            val ww = athletDisziplinAvgRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet)
            val rang = ww.rang
            val posproz = 100d * rang.endnote / athletDisziplinAvgRangMap.size
            LeafRow(w.wettkampfdisziplin.disziplin.name,
              ww.avg,
              rang,
              rang.endnote >= 1 && (rang.endnote < 4 || (w.wettkampf.auszeichnung > 0 && posproz <= w.wettkampf.auszeichnung)))
          }
          else {
            val ww = athletDisziplinRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet)
            val rang = ww.rang
            val posproz = 100d * rang.endnote / athletDisziplinRangMap.size
            LeafRow(w.wettkampfdisziplin.disziplin.name,
              ww.sum,
              rang,
              rang.endnote >= 1 && (rang.endnote < 4 || (w.wettkampf.auszeichnung > 0 && posproz <= w.wettkampf.auszeichnung)))
          }
        }.filter(_.sum.endnote >= 1).toIndexedSeq
      }
      else {
    	  val wbpg = athlWertungen.groupBy(GroupSection.programGrouper).filter(_._2.size > 0).toList.sortBy(gru => gru._1.ord)
        wbpg.map{pg =>
          if(multiwettkampf) {
            val wks = pg._2.filter(_.endnote > 0).groupBy { w => w.wettkampf.id }
            val wksums = wks.map {wk => wk._2.map(w => w.resultat).reduce(_+_)}
            val avg = if(wksums.nonEmpty) wksums.reduce(_+_) / wksums.size else Resultat(0,0,0)
            //(avg, athletAvgRangMap(athlWertungen.head.athlet).rang)

            val ww = athletProgrammAvgRangMap(pg._1.id)(pg._2.head.athlet)
            val rang = ww.rang
            LeafRow(pg._1.easyprint,
              avg,
              rang,
              rang.endnote < 4 && rang.endnote >= 1)
          }
          else {
            val ww = athletProgrammRangMap(pg._1.id)(pg._2.head.athlet)
            val rang = ww.rang
            LeafRow(pg._1.easyprint,
              ww.sum,
              rang,
              rang.endnote < 4 && rang.endnote >= 1)
          }
        }.filter(_.sum.endnote >= 1).toIndexedSeq
      }
    }

    val avgPerAthlet = list.groupBy { x =>
      x.athlet
    }.map { x =>
      (x._1, mapToAvgRowSummary(x._2), x._2)
    }.filter(_._2._1.endnote > 0)//.toList.sortBy(sx => sx._2._2.endnote).reverse.zipWithIndex

    // Beeinflusst die Total-Rangierung pro Gruppierung
//    val rangMap = GroupSection.mapAvgRang(avgPerAthlet.map { d => (d._1._1, d._1._2._1, d._1._2._2) }).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
    val rangMap = GroupSection.mapAvgRang(avgPerAthlet.map { d => (d._1, d._2._1, d._2._2) }).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap

    val teilnehmer = avgPerAthlet.size
    avgPerAthlet.map {x =>
//      val ((athlet, (sum, avg), wertungen), prerang) = x
      val (athlet, (sum, avg), wertungen) = x
      val gsrang = rangMap(athlet)
      val posproz = 100d * gsrang.rang.endnote / teilnehmer
      // QS
//      if(gsrang.rang.endnote != prerang+1) {
//        println(s"${wertungen.head.wettkampfdisziplin.programm.head.easyprint} anzahl ${teilnehmer} athlet ${athlet.easyprint} proz ${posproz} gsrang ${gsrang.rang.endnote}, prerang ${prerang+1}")
//      }
      GroupRow(athlet, mapToGroupSum(wertungen), avg, gsrang.rang,
          gsrang.rang.endnote > 0
          && (gsrang.rang.endnote < 4
              || (wertungen.head.wettkampf.auszeichnung > 0
                  && posproz <= wertungen.head.wettkampf.auszeichnung)))

    }.toList.filter(_.sum.endnote > 0).sortBy(_.rang.endnote)
  }

  override def easyprint = {
    val buffer = new StringBuilder()
    buffer.append(groupKey.easyprint).append("\n")
    val ds = list.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id }
    buffer.append(f"${"Disziplin"}%40s").append(f"${"Rang"}%18s")
    for (w <- ds) {
      buffer.append(f" ${w.easyprint}%18s")
    }
    buffer.append("\n")
    buffer.append(f"${"Athlet"}%40s")
    val legendd = "D"
    val legenda = "E"
    val legende = "End"
    val legend = f"${legendd}%6s${legenda}%6s${legende}%6s"
    buffer.append(f" ${legend}%18s")
    for (w <- ds) {
      buffer.append(f" ${legend}%18s")
    }
    buffer.append("\n")
    for (wv <- list.groupBy { x => x.athlet }.map { x => (x._1, x._2, x._2.map(w => w.endnote).sum) }.toList.sortBy(_._3).reverse) {
      val (athlet, wertungen, sum) = wv
      buffer.append(f"${athlet.easyprint}%40s")
      buffer.append(f"Rang ${athletAvgRangMap(athlet).rang.easyprint}%18s")
      for (w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
        buffer.append(f" ${w.easyprint}%18s")
      }
      buffer.append("\n")
      buffer.append(f"${"Geräterang"}%58s")
      for (w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
        buffer.append(f" ${athletDisziplinAvgRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet).rang.easyprint}%18s")
      }
      buffer.append("\n")
    }
    buffer.toString()
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
