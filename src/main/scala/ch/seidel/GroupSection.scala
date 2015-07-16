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

object GroupSection {
  def mapRang(list: Iterable[(DataObject, Resultat)]) = {
    val rangD = list.toList.map(_._2.noteD).filter(_ > 0).sorted.reverse :+ 0
    val rangE = list.toList.map(_._2.noteE).filter(_ > 0).sorted.reverse :+ 0
    val rangEnd = list.toList.map(_._2.endnote).filter(_ > 0).sorted.reverse :+ 0
    def rang(r: Resultat) = {
      val rd = if (rangD.size > 1) rangD.indexOf(r.noteD) + 1 else 0
      val re = if (rangE.size > 1) rangE.indexOf(r.noteE) + 1 else 0
      val rf = if (rangEnd.size > 1) rangEnd.indexOf(r.endnote) + 1 else 0
      Resultat(rd, re, rf)
    }
    list.map(y => GroupSum(y._1, y._2, rang(y._2)))
  }
}

sealed trait GroupSection {
  val groupKey: DataObject
  val sum: Resultat
  def easyprint: String
}

case class GroupSum(override val groupKey: DataObject, wertung: Resultat, rang: Resultat) extends GroupSection {
  override val sum: Resultat = wertung
  override def easyprint = f"Rang ${rang.easyprint} ${groupKey.easyprint}%40s Punkte ${sum.easyprint}%18s"
}

case class GroupLeaf(override val groupKey: DataObject, list: Iterable[WertungView]) extends GroupSection {
  override val sum: Resultat = list.map(_.resultat).reduce((r1, r2) => r1 + r2)

  def mapToRang(fl: Iterable[WertungView]) = {
    val grouped = fl.groupBy { x => x.athlet }.map { x =>
      val r = x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2)
      (x._1, r)
    }
    GroupSection.mapRang(grouped).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
  }

  lazy val athletRangMap = mapToRang(list)
  lazy val athletDisziplinRangMap = list.groupBy(w => w.wettkampfdisziplin.disziplin.id).map { d => (d._1 -> mapToRang(d._2)) }
  lazy val athletProgrammRangMap = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead.id).map { d => (d._1 -> mapToRang(d._2)) }

  def buildColumns: List[jfxsc.TableColumn[GroupRow, _]] = {
    val groups = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead).map { pw =>
      (pw._1 -> pw._2.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id })
    }
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
        // pro Gruppenkey eine Summenspalte bilden
        groups.toList.map { gr =>
          val (grKey, diszipline) = gr
          val clDnote = new TableColumn[GroupRow, String] {
            text = "D"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "dnote", {
                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0)))
                val best = if (colsum.rang.noteD.toInt == 1) "*" else ""
                best + colsum.sum.formattedD
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEnote = new TableColumn[GroupRow, String] {
            text = "E"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "enote", {
                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0)))
                val best = if (colsum.rang.noteE.toInt == 1) "*" else ""
                best + colsum.sum.formattedE
              })
            }
            prefWidth = 60
            styleClass += "hintdata"
          }
          val clEndnote = new TableColumn[GroupRow, String] {
            text = "Endnote"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "endnote", {
                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0)))
                val best = if (colsum.rang.endnote.toInt == 1) "*" else ""
                best + colsum.sum.formattedEnd
              })
            }
            prefWidth = 60
            styleClass += "valuedata"
          }
          val clRang = new TableColumn[GroupRow, String] {
            text = "Rang"
            cellValueFactory = { x =>
              new ReadOnlyStringWrapper(x.value, "rang", {
                val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0, 0, 0), Resultat(0, 0, 0)))
                colsum.rang.formattedEnd
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
    def mapToGroupSum(athlWertungen: Iterable[WertungView]): IndexedSeq[LeafRow] = {
      athlWertungen.map { w =>
        val rang = athletDisziplinRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet).rang
        val posproz = 100d * rang.endnote / athletDisziplinRangMap.size
        LeafRow(w.wettkampfdisziplin.disziplin.name,
          w.resultat,
          athletDisziplinRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet).rang,
          rang.endnote < 4 || posproz <= w.wettkampf.auszeichnung)
      }.toIndexedSeq
    }
    def mapToRowSummary(athlWertungen: Iterable[WertungView]): (Resultat, Resultat) = {
      (athlWertungen.map(w => w.resultat).reduce((r1, r2) => r1 + r2),
        athletRangMap(athlWertungen.head.athlet).rang)
    }
    list.groupBy { x =>
      x.athlet
    }.map { x =>
      val (sum, rang) = mapToRowSummary(x._2)
      val posproz = 100d * rang.endnote / athletRangMap.size
      GroupRow(x._1, mapToGroupSum(x._2), sum, rang, rang.endnote < 4 || posproz <= x._2.head.wettkampf.auszeichnung)
    }.toList.sortBy(_.rang.endnote)
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
      buffer.append(f"Rang ${athletRangMap(athlet).rang.easyprint}%18s")
      for (w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
        buffer.append(f" ${w.easyprint}%18s")
      }
      buffer.append("\n")
      buffer.append(f"${"Geräterang"}%58s")
      for (w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
        buffer.append(f" ${athletDisziplinRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet).rang.easyprint}%18s")
      }
      buffer.append("\n")
    }
    buffer.toString()
  }
}

case class GroupNode(override val groupKey: DataObject, next: Iterable[GroupSection]) extends GroupSection {
  override val sum: Resultat = next.map(_.sum).reduce((r1, r2) => r1 + r2)
  override def easyprint = {
    val buffer = new StringBuilder()
    buffer.append(groupKey.easyprint).append("\n")
    for (gi <- next) {
      buffer.append(gi.easyprint).append("\n")
    }
    buffer.toString
  }
}
