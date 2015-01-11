package ch.seidel.commons

import java.text.SimpleDateFormat
import scala.collection.mutable.StringBuilder
import javafx.scene.{ control => jfxsc }
import javafx.collections.{ObservableList, ListChangeListener}
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.util.converter.DefaultStringConverter
import scalafx.scene.layout.Region
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.scene.layout.BorderPane
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.StringProperty
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.{Priority, StackPane}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableView, TableColumn}
import scalafx.scene.control.TableColumn._
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.util.converter.DoubleStringConverter
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.ToolBar
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.Group
import scalafx.event.ActionEvent
import scalafx.scene.control.ScrollPane
import scalafx.beans.property.ReadOnlyDoubleWrapper
import scalafx.scene.web.WebView
import ch.seidel.domain._
import scalafx.geometry.Insets
import scalafx.scene.control.ComboBox

object WettkampfPage {

  case class WertungEditor(init: WertungView) {
    private var _noteD = init.noteD.toDouble
    def getNoteD = f"${_noteD}%2.3f"
    def setNoteD(v: String) { _noteD = v.toDouble }
    val noteD = new StringProperty(this, "noteD", getNoteD) {
      override def value = getNoteD
      override def value_=(v: String) {
        _noteD = v.toDouble
      }
    }

    var _noteE = init.noteE.toDouble
    def getNoteE = f"${_noteE}%2.3f"
    def setNoteE(v: String) { _noteE = v.toDouble }
    val noteE = new StringProperty(this, "noteE", getNoteE) {
      override def value = getNoteE
      override def value_=(v: String) {
        _noteE = v.toDouble
      }
    }

    var _endnote = init.endnote.toDouble
    def getEndnote = f"${_endnote}%2.3f"
    def setEndnote(v: String) { _endnote = v.toDouble }
    val endnote = new StringProperty(this, "endnote", getEndnote) {
      override def value = getEndnote
      override def value_=(v: String) {
        _endnote = v.toDouble
      }
    }

    def isDirty = _noteD != init.noteD || _noteE != init.noteE || _endnote != init.endnote

    def reset {
      noteD.value = init.noteD.toString
      noteE.value = init.noteE.toString
      endnote.value = init.endnote.toString
    }

    def commit = Wertung(
        init.id, init.athlet.id, init.wettkampfdisziplin.id, init.wettkampf.id,
        scala.math.BigDecimal(_noteD),
        scala.math.BigDecimal(_noteE),
        scala.math.BigDecimal(_endnote))
  }
  trait TabWithService {
    val service: KutuService
    lazy val populated = isPopulated
    def isPopulated: Boolean
  }

  class LazyLoadingTab(programm: ProgrammView, wettkampf: WettkampfView, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
    override def isPopulated = {
      def updateWertungen = {
        athleten.filter(wv => wv.wettkampfdisziplin.programm.id == programm.id).groupBy(wv => wv.athlet).map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
      }
      var wertungen = updateWertungen
      val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)

      val indexerE = Iterator.from(0)
      val indexerD = Iterator.from(0)
      val indexerF = Iterator.from(0)
      def wertungenCols = if(wertungen.nonEmpty) {
        wertungen.head.map{wertung =>
          val clDnote/*: jfxsc.TableColumn[IndexedSeq[WertungEditor],_]*/ = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerD.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).noteD else wertung.noteD}
            cellFactory = {_ => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 60
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
         		  disciplin.noteD.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val clEnote/*: jfxsc.TableColumn[IndexedSeq[WertungEditor],_]*/ = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerE.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).noteE else wertung.noteE}
            cellFactory = {x => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 60
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
              disciplin.noteE.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val clEndnote = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerF.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).endnote else wertung.endnote}
            cellFactory = {x => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 80
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
              disciplin.endnote.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor],_] = new TableColumn[IndexedSeq[WertungEditor], String] {
            text = clEndnote.text.value
            clDnote.text = "D"
            clEnote.text = "E"
            clEndnote.text = "Endnote"
            prefWidth = 200
            columns ++= Seq(
                clDnote,
                clEnote,
                clEndnote
                )
          }
          cl
        }
      }
      else {
        IndexedSeq[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]]()
      }

      val athletCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]] = List(
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Athlet"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", {
                val a = x.value.head.init.athlet
                s"${a.vorname} ${a.name}"
              })
            }
            prefWidth = 150
          },
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Verein"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "verein", {
                val a = x.value.head.init.athlet
                s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
              })
            }
            prefWidth = 100
          })

      val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]] = List(
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Punkte"
//              cellValueFactory = {x => new ReadOnlyFloatWrapper(x.value, "punkte", { x.value.map(w => w.endnote).sum.toFloat})}
            cellValueFactory = {x =>
              val w = new ReadOnlyStringWrapper(x.value, "punkte", {f"${x.value.map(w => w._endnote).sum}%3.3f"})

              w
            }
            prefWidth = 80
            styleClass +=  "table-cell-with-value"
          })

      val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
        columns ++= athletCol ++ wertungenCols ++ sumCol
        id = "kutu-table"
        editable = true
      }

      val addButton = new Button {
              text = "Athlet hinzufügen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                disable = true
                val athletModel = ObservableBuffer[AthletView](
                  service.selectAthletesView.filter(service.altersfilter(programm, _)).filter{p => wertungen.forall { wp => wp.head.init.athlet.id != p.id}}
                )
                val athletTable = new TableView[AthletView](athletModel) {
                  columns ++= List(
                    new TableColumn[AthletView, String] {
                      text = "Athlet"
                      cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", {
                        s"${x.value.vorname} ${x.value.name} (${x.value.verein.map { _.name }.getOrElse("ohne Verein")})"})}
                      //prefWidth = 150
                    }
                  )
                }
                implicit val impevent = event
                PageDisplayer.showInDialog(text.value, new DisplayablePage() {
                  def getPage: Node = {
                    new BorderPane {
                      hgrow = Priority.ALWAYS
                      vgrow = Priority.ALWAYS
                      center = athletTable
                    }
                  }
                }, new Button("OK") {
                  onAction = (event: ActionEvent) => {
                    if(!athletTable.selectionModel().isEmpty) {
                      val athlet = athletTable.selectionModel().getSelectedItem
                      def filter(progId: Long, a: Athlet): Boolean = a.id == athlet.id
                      service.assignAthletsToWettkampf(wettkampf.id, Set(programm.id), Some(filter))
                      wkModel.clear
                      wertungen = updateWertungen
                      wkModel.appendAll(wertungen)
                      wkview.columns.clear()
                      wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
                    }
                  }
                }, new Button("OK, Alle") {
                  onAction = (event: ActionEvent) => {
                    if(!athletTable.selectionModel().isEmpty) {
                      val athlet = athletTable.selectionModel().getSelectedItem
                      def filter(progId: Long, a: Athlet): Boolean = athletModel.exists { x => x.id == a.id }
                      service.assignAthletsToWettkampf(wettkampf.id, Set(programm.id), Some(filter))
                      wkModel.clear
                      wertungen = updateWertungen
                      wkModel.appendAll(wertungen)
                      wkview.columns.clear()
                      wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
                    }
                  }
                })
                disable = false
              }
            }
      val removeButton = new Button {
              text = "Athlet entfernen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                    if(!wkview.selectionModel().isEmpty) {
                      val athletwertungen = wkview.selectionModel().getSelectedItem.map(_.init.id).toSet
                      service.unassignAthletFromWettkampf(athletwertungen)
                      wkModel.remove(wkview.selectionModel().getSelectedIndex)
                    }
                  }
            }
      val clearButton = new Button {
              text = "Athlet zurücksetzen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                    if(!wkview.selectionModel().isEmpty) {
                      val selected = wkview.selectionModel().getSelectedItem
                      for(disciplin <- selected) {
                        disciplin.noteD.value = "0"
                        disciplin.noteE.value = "0"
                        disciplin.endnote.value = "0"
                        val rowIndex = wkModel.indexOf(selected)
                        if(disciplin.isDirty) {
                          wkModel.update(rowIndex, selected.updated(selected.indexOf(disciplin), WertungEditor(service.updateWertung(disciplin.commit))))
                        }
                      }
                    }
                  }
            }
      //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      removeButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      clearButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

      content = new BorderPane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS
        center = wkview
        top = new ToolBar {
          content = List(
            new Label {
              text = s"Programm ${programm.name}"
              maxWidth = Double.MaxValue
              minHeight = Region.USE_PREF_SIZE
              styleClass += "toolbar-header"
            },
            addButton, removeButton, clearButton
          )
        }
      }
      /*content =new StackPane {
      alignmentInParent = Pos.TOP_LEFT*/

      true
    }
  }

  class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
        /*
         * combo 1. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 2. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 3. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 4. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         */

//    val daten = service.selectWertungen().groupBy { x =>
//      x.wettkampf }.map(x => (x._1, x._2.groupBy { x =>
//        x.wettkampfdisziplin.programm }.map(x => (x._1, x._2.groupBy { x =>
//          x.athlet }))))
        object GroupSection {
          def mapRang(list: Iterable[(DataObject, Resultat)]) = {
              val rangD = list.toList.map(_._2.noteD).filter(_ > 0).sorted.reverse :+ 0
              val rangE = list.toList.map(_._2.noteE).filter(_ > 0).sorted.reverse :+ 0
              val rangEnd = list.toList.map(_._2.endnote).filter(_ > 0).sorted.reverse :+ 0
              def rang(r: Resultat) = {
                val rd = if(rangD.size > 1) rangD.indexOf(r.noteD) + 1 else 0
                val re = if(rangE.size > 1) rangE.indexOf(r.noteE) + 1 else 0
                val rf = if(rangEnd.size > 1) rangEnd.indexOf(r.endnote) + 1 else 0
                Resultat(rd, re, rf)
              }
              list.map(y => GroupSum(y._1, y._2, rang(y._2)))
          }
        }
        sealed trait GroupSection {
          val groupKey: DataObject
          val sum: Resultat
//          def aggregate: GroupSum = GroupSum(groupKey, sum)
          def easyprint: String
//          def buildColumns: List[jfxsc.TableColumn[LeafRow,_]]
//          def getTableData: List[LeafRow]
        }
        sealed trait DataRow {}
        case class LeafRow(athlet: AthletView, resultate: IndexedSeq[GroupSum], sum: Resultat, rang: Resultat) extends DataRow
        case class GroupRow(title: String, sum: Resultat, rang: Resultat) extends DataRow
        case class GroupSum(override val groupKey: DataObject, wertung: Resultat, rang: Resultat) extends GroupSection {
          override val sum: Resultat = wertung
          override def easyprint = f"Rang ${rang.easyprint} ${groupKey.easyprint}%40s Punkte ${sum.easyprint}%18s"
//          override def buildColumns: List[jfxsc.TableColumn[LeafRow,_]] = {
//            List()
//          }
//          override def getTableData: List[LeafRow] = {
//            List()
//          }
        }
        case class GroupLeaf(override val groupKey: DataObject, list: Iterable[WertungView]) extends GroupSection {
          override val sum: Resultat = list.map(_.resultat).reduce((r1, r2) => r1 + r2)

          def mapToRang(fl: Iterable[WertungView]) = {
            val grouped = fl.groupBy { x => x.athlet }.map{ x =>
              val r = x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2)
              (x._1, r)
            }
            GroupSection.mapRang(grouped).map(r => (r.groupKey.asInstanceOf[AthletView] -> r)).toMap
          }

          lazy val athletRangMap = mapToRang(list)
          lazy val athletDisziplinRangMap = list.groupBy(w => w.wettkampfdisziplin.disziplin.id).map{d => (d._1 -> mapToRang(d._2))}
          lazy val athletProgrammRangMap = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead.id).map{d => (d._1 -> mapToRang(d._2))}

          def buildColumns: List[jfxsc.TableColumn[LeafRow,_]] = {
//            val ds = list.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id }
            val groups = list.groupBy(w => w.wettkampfdisziplin.programm.aggregatorSubHead).map{pw =>
              (pw._1 -> pw._2.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id })
            }
            val athletCols: List[jfxsc.TableColumn[LeafRow,_]] = List(
              new TableColumn[LeafRow, String] {
                  text = "Rang"
                  cellValueFactory = {x =>
                    val w = new ReadOnlyStringWrapper(x.value, "rang", {f"${x.value.rang.endnote}%3.0f"})
                    w
                  }
                  prefWidth = 20
                styleClass +=  "data"
              },
              new TableColumn[LeafRow, String] {
                text = "Athlet"
                cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", {
                    val a = x.value.athlet
                    f"${a.vorname} ${a.name}"
                  })
                }
                prefWidth = 90
                styleClass +=  "data"
              },
              new TableColumn[LeafRow, String] {
                text = "Verein"
                cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "verein", {
                    val a = x.value.athlet
                    s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
                  })
                }
                prefWidth = 90
                styleClass +=  "data"
              }
              )
            val indexer = Iterator.from(0)
            val disziplinCol: List[jfxsc.TableColumn[LeafRow,_]] =
            if(groups.keySet.size > 1) {
              // pro Gruppenkey eine Summenspalte bilden
              groups.toList.map{gr =>
                val (grKey, diszipline) = gr
                val clDnote = new TableColumn[LeafRow, String] {
                  text = "D"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "dnote", {
                      val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0,0,0), Resultat(0,0,0)))
                      val best = if(colsum.rang.noteD.toInt == 1) "*" else ""
                      best + colsum.sum.formattedD
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val clEnote = new TableColumn[LeafRow, String] {
                  text = "E"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "enote", {
                      val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0,0,0), Resultat(0,0,0)))
                      val best = if(colsum.rang.noteE.toInt == 1) "*" else ""
                      best + colsum.sum.formattedE
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val clEndnote = new TableColumn[LeafRow, String] {
                  text = "Endnote"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "endnote", {
                      val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0,0,0), Resultat(0,0,0)))
                      val best = if(colsum.rang.endnote.toInt == 1) "*" else ""
                      best + colsum.sum.formattedEnd
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "valuedata"
                }
                val clRang = new TableColumn[LeafRow, String] {
                  text = "Rang"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "rang", {
                      val colsum = athletProgrammRangMap(grKey.id).getOrElse(x.value.athlet, GroupSum(x.value.athlet, Resultat(0,0,0), Resultat(0,0,0)))
                      colsum.rang.formattedEnd
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val cl: jfxsc.TableColumn[LeafRow,_] = new TableColumn[LeafRow, String] {
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
              groups.head._2.map{disziplin =>
                val index = indexer.next
                val clDnote = new TableColumn[LeafRow, String] {
                  text = "D"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "dnote", {
                      val best = if(x.value.resultate.size > index && x.value.resultate(index).rang.noteD.toInt == 1) "*" else ""
                      if(x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedD else ""
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val clEnote = new TableColumn[LeafRow, String] {
                  text = "E"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "enote", {
                      val best = if(x.value.resultate.size > index && x.value.resultate(index).rang.noteE.toInt == 1) "*" else ""
                      if(x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedE else ""
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val clEndnote = new TableColumn[LeafRow, String] {
                  text = "Endnote"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "endnote", {
                      val best = if(x.value.resultate.size > index && x.value.resultate(index).rang.endnote.toInt == 1) "*" else ""
                      if(x.value.resultate.size > index) best + x.value.resultate(index).sum.formattedEnd else ""
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "valuedata"
                }
                val clRang = new TableColumn[LeafRow, String] {
                  text = "Rang"
                  cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "rang", {
                      if(x.value.resultate.size > index) f"${x.value.resultate(index).rang.endnote}%3.0f" else ""
                    })
                  }
                  prefWidth = 60
                  styleClass +=  "hintdata"
                }
                val cl: jfxsc.TableColumn[LeafRow,_] = new TableColumn[LeafRow, String] {
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
            val sumCol: List[jfxsc.TableColumn[LeafRow,_]] = List(
              new TableColumn[LeafRow, String] {
                text = "Total D"
                cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedD)}
                prefWidth = 80
                styleClass +=  "hintdata"
              },
              new TableColumn[LeafRow, String] {
                text = "Total E"
                cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedE)}
                prefWidth = 80
                styleClass +=  "hintdata"
              },
              new TableColumn[LeafRow, String] {
                text = "Total Punkte"
                cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "punkte", x.value.sum.formattedEnd)}
                prefWidth = 80
                styleClass +=  "valuedata"
              }
            )
            athletCols ++ disziplinCol ++ sumCol
          }

          def getTableData = {
            def mapToGroupSum(athlWertungen: Iterable[WertungView]): IndexedSeq[GroupSum] = {
              athlWertungen.map{w =>
                GroupSum(w.wettkampfdisziplin.disziplin,
                         w.resultat,
                         athletDisziplinRangMap(w.wettkampfdisziplin.disziplin.id)(w.athlet).rang)
              }.toIndexedSeq
            }
            def mapToRowSummary(athlWertungen: Iterable[WertungView]): (Resultat, Resultat) = {
              (athlWertungen.map(w => w.resultat).reduce((r1, r2) => r1 + r2),
               athletRangMap(athlWertungen.head.athlet).rang)
            }
            list.groupBy {x =>
                x.athlet
              }.map{x =>
                val (sum, rang) = mapToRowSummary(x._2)
                LeafRow(x._1, mapToGroupSum(x._2), sum, rang)
              }.toList.sortBy(_.rang.endnote)
          }

          override def easyprint = {
            val buffer = new StringBuilder()
            buffer.append(groupKey.easyprint).append("\n")
            val ds = list.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id }
            buffer.append(f"${"Disziplin"}%40s").append(f"${"Rang"}%18s")
            for(w <- ds) {
              buffer.append(f" ${w.easyprint}%18s")
            }
            buffer.append("\n")
            buffer.append(f"${"Athlet"}%40s")
            val legendd = "D"
            val legenda = "E"
            val legende = "End"
            val legend = f"${legendd}%6s${legenda}%6s${legende}%6s"
            buffer.append(f" ${legend}%18s")
            for(w <- ds) {
              buffer.append(f" ${legend}%18s")
            }
            buffer.append("\n")
            for(wv <- list.groupBy { x => x.athlet }.map{x => (x._1, x._2, x._2.map(w => w.endnote).sum)}.toList.sortBy(_._3).reverse) {
              val (athlet, wertungen, sum) = wv
              buffer.append(f"${athlet.easyprint}%40s")
              buffer.append(f"Rang ${athletRangMap(athlet).rang.easyprint}%18s")
              for(w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
                buffer.append(f" ${w.easyprint}%18s")
              }
              buffer.append("\n")
              buffer.append(f"${"Geräterang"}%58s")
              for(w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
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
            for(gi <- next) {
              buffer.append(gi.easyprint).append("\n")
            }
            buffer.toString
          }
        }

        sealed trait GroupBy {
          val groupname: String
          private var next: Option[GroupBy] = None
          protected val grouper: (WertungView) => DataObject
          protected val sorter: Option[(GroupSection, GroupSection) => Boolean] //= leafsorter
          protected val leafsorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.sum.endnote > gs2.sum.endnote
          })
          override def toString = groupname

          def /(next: GroupBy): GroupBy = groupBy(next)
          def groupBy(next: GroupBy): GroupBy = {
            this.next match {
              case Some(n) => n.groupBy(next)
              case None    => this.next = Some(next)
            }
            this
          }
          def reset {
            next = None
          }

          def select(wvlist: Seq[WertungView] = service.selectWertungen()): Iterable[GroupSection] = {
            val grouped = wvlist groupBy grouper
            next match {
              case Some(ng) => mapAndSortNode(ng, grouped)
              case None     => mapAndSortLeaf(grouped)
            }
          }
          private def mapAndSortLeaf(grouped: Map[DataObject, Seq[WertungView]]) = {
            def x(switch: DataObject, list: Seq[WertungView]) = {
              val grouped = list.groupBy { x => x.athlet }.map{ x =>
                val r = x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2)
                (x._1, r)
              }
              GroupSection.mapRang(grouped).toSeq
            }
            def reduce(switch: DataObject, list: Seq[WertungView]):Seq[GroupSection] = {
              // TODO Aggregation bei ATT: Umkehr von Programm auf Disziplin (Diszipline werden zusammengefasst pro aggregiertem Programm)
              list.toList match {
//                case head :: _ if(head.wettkampfdisziplin.programm.aggregate > 0) =>
//                  Seq(GroupNode(switch, sort(x(switch, list), leafsorter)))
                case _ =>
                  Seq(GroupLeaf(switch, list))
              }
            }
            sort(grouped.flatMap(x => reduce(x._1, x._2)), sorter)
          }

          private def mapAndSortNode(ng: GroupBy, grouped: Map[DataObject, Seq[WertungView]]) = {
            sort(grouped.map{x =>
              val (grp, seq) = x
              GroupNode(grp, ng.select(seq))
            }, sorter)
          }

          private def sort(mapped: Iterable[GroupSection], sorter: Option[(GroupSection, GroupSection) => Boolean]) = {
            sorter match {
              case Some(s) => mapped.toList.sortWith(s)
              case None    => mapped
            }
          }
        }

        case object ByNothing extends GroupBy {
          override val groupname = "keine"
          protected override val grouper = (v: WertungView) => {
            v
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.sum.endnote < gs2.sum.endnote
          })
        }

        case object ByProgramm extends GroupBy {
          override val groupname = "Programm"
          protected override val grouper = (v: WertungView) => {
            v.wettkampfdisziplin.programm.aggregatorHead
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
          })
        }
        case object ByJahrgang extends GroupBy {
          override val groupname = "Jahrgang"
          private val extractYear = new SimpleDateFormat("YYYY")
          protected override val grouper = (v: WertungView) => {
            v.athlet.gebdat match {
              case Some(d) => AthletJahrgang(extractYear.format(d))
              case None    => AthletJahrgang("unbekannt")
            }
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[AthletJahrgang].hg.compareTo(gs2.groupKey.asInstanceOf[AthletJahrgang].hg) < 0
          })
        }
        case object ByDisziplin extends GroupBy {
          override val groupname = "Disziplin"
          protected override val grouper = (v: WertungView) => {
            v.wettkampfdisziplin.disziplin
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[Disziplin].ord.compareTo(gs2.groupKey.asInstanceOf[Disziplin].ord) < 0
          })
        }
        case object ByVerein extends GroupBy {
          override val groupname = "Verein"
          protected override val grouper = (v: WertungView) => {
            v.athlet.verein match {
              case Some(v) => v
              case _ => Verein(0, "kein")
            }
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[Verein].name.compareTo(gs2.groupKey.asInstanceOf[Verein].name) < 0
          })
        }

    override def isPopulated = {
      val dummyTableView = new TableView[LeafRow]()
      val groupers = List(ByNothing, ByProgramm, ByJahrgang, ByVerein, ByDisziplin)
      val gr1Model = ObservableBuffer[GroupBy](groupers)
      val cb1 = new ComboBox[GroupBy] {
          maxWidth = 200
          promptText = "erste gruppierung..."
          items = gr1Model
        }
      val cb2 =
        new ComboBox[GroupBy] {
          maxWidth = 200
          promptText = "zweite gruppierung..."
          items = gr1Model
        }
      val cb3 =
        new ComboBox[GroupBy] {
          maxWidth = 200
          promptText = "dritte gruppierung..."
          items = gr1Model
        }
      val cb4 =
        new ComboBox[GroupBy] {
          maxWidth = 200
          promptText = "vierte gruppierung..."
          items = gr1Model
        }
      val combs = List(cb1, cb2, cb3, cb4)
      val webView = new WebView

      def buildQuery = {
        groupers.foreach { gr => gr.reset }
        val cblist = combs.filter(cb => !cb.selectionModel.value.isEmpty).map(cb => cb.selectionModel.value.getSelectedItem).filter(x => x != ByNothing)
        if(cblist.isEmpty) {
          ByProgramm
        }
        else {
          cblist.foldLeft(cblist.head)((acc, cb) => if(acc != cb) acc.groupBy(cb) else acc)
        }
      }

      def refreshRangliste(query: GroupBy) {
        val combination = query.select(service.selectWertungen().filter(p => p.wettkampf.id == wettkampf.id)).toList
        webView.engine.loadContent(toHTML(combination, 0))
      }

      def toHTML(gs: List[GroupSection], level: Int): String = {
        val gsBlock = new StringBuilder()
        if(level == 0) {
          gsBlock.append(s"""<html><head>
            <style type="text/css">
              body {
                font-family: "Arial", "Verdana", sans-serif;
              }
              table{
                  /*table-layout:fixed;*/
                  border-collapse:collapse;
                  border-spacing:0;
                  border-style:hidden;
              }
              th {
                background-color: rgb(250,250,200);
                font-size: 9px;
              }
              td {
                padding:0.25em;
              }
              td .data {
                text-align: right
              }
              td .valuedata {
                text-align: right
              }
              td .hintdata {
                color: rgb(50,100,150);
                font-size: 9px;
                text-align: right
              }
              col:first-child {
                background: rgb(250, 250, 200, 0.6);
              }
              col:nth-child(4n+6) {
                background: rgba(150, 150, 150, 0.6);
              }
              col:nth-child(4n+4) {
                border-left: 1px solid black;
              }
              tr:nth-child(even) .data {background: rgba(230, 230, 230, 0.6);}
              tr:nth-child(odd) .data {background: rgba(210, 200, 180, 0.6);}
              /*.disziplin {
                -webkit-transform: rotate(90deg);
                -moz-transform: rotate(90deg);
                -o-transform: rotate(90deg);
                writing-mode: lr-tb;
              }*/
            </style>
            </head><body><h1>Rangliste ${wettkampf.easyprint}</h1>\n""")
        }
        for(c <- gs) {
          c match {
            case gl: GroupLeaf =>
              gsBlock.append(s"<h${level + 2}>${gl.groupKey.easyprint}</h${level + 2}>\n<table width='100%'>\n")
              val cols = gl.buildColumns
              cols.foreach{th =>
                if(th.columns.size > 0) {
                  cols.foreach{thc =>
                    gsBlock.append(s"<col/>")
                  }
                }
                else {
                  gsBlock.append(s"<col/>")
                }
              }
              gsBlock.append(s"\n<thead><tr class='head'>\n")
              cols.foreach{th =>
                if(th.columns.size > 0) {
                  gsBlock.append(s"<th colspan=${th.columns.size}>${th.getText}</th>")
                }
                else {
                  gsBlock.append(s"<th rowspan=2>${th.getText}</th>")
                }
              }
              gsBlock.append(s"</tr><tr>\n")
              cols.foreach{th =>
                if(th.columns.size > 0) {
                  th.columns.foreach{th =>
                    gsBlock.append(s"<th>${th.getText}</th>")
                  }
                }
              }
              gsBlock.append(s"</tr></thead><tbody>\n")
              gl.getTableData.foreach { row =>
                gsBlock.append(s"<tr class='data'>")
                cols.foreach{col =>
                  if(col.columns.size == 0) {
                    val c = col.asInstanceOf[jfxsc.TableColumn[LeafRow, String]]
                    val feature = new CellDataFeatures(dummyTableView, c, row)
                    if(c.getStyleClass.contains("hintdata")) {
                      gsBlock.append(s"<td class='data'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                    }
                    else if(c.getStyleClass.contains("data")) {
                      gsBlock.append(s"<td class='data'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                    }
                    else  {
                      gsBlock.append(s"<td class='data'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                    }
                  }
                  else {
                    col.columns.foreach{ccol =>
                      val c = ccol.asInstanceOf[jfxsc.TableColumn[LeafRow, String]]
                      val feature = new CellDataFeatures(dummyTableView, c, row)
                      if(c.getStyleClass.contains("hintdata")) {
                        gsBlock.append(s"<td class='data'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                      }
                      else if(c.getStyleClass.contains("data")) {
                        gsBlock.append(s"<td class='data'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                      }
                      else  {
                        gsBlock.append(s"<td class='data'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                      }
                    }
                  }
                }
                gsBlock.append(s"</tr>\n")
              }
              gsBlock.append(s"</tbody></table>\n")

            case g: GroupNode => gsBlock.append(s"<h${level + 2}>${g.groupKey.easyprint}</h${level + 2}>\n").append(toHTML(g.next.toList, level + 1))
            case s: GroupSum => gsBlock.append(s.easyprint)
          }
        }
        gsBlock.append("</body></html>")
        gsBlock.toString()
      }

      val btnRefresh = new Button {
        text = "refresh"
        onAction = handle {
          refreshRangliste(buildQuery)
        }
      }

      val controlbox = new HBox {
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        spacing = 15
        padding = Insets(20)
        content = combs :+ btnRefresh
      }
      val bp = new BorderPane {
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        top = controlbox
        center = webView
      }
      content = bp
////        val combination = ByProgramm.groupBy(ByJahrgang).groupBy(ByVerein).select(
//        val combination = ByProgramm.groupBy(ByJahrgang).select(
////        val combination = ByProgramm.select(
////        val combination = ByDisziplin / ByProgramm select(
////        val combination = ByProgramm.groupBy(ByDisziplin).select(
//          service.selectWertungen().filter(p => p.wettkampf.id == wettkampf.id))
//
//        val webView = new WebView {
//
//          //engine.location.onChange((_, _, newValue) => locationField.setText(newValue))
//          val html = toHTML(combination.toList, 0)
//          println(html)
//          engine.loadContent(html)
//        }

//        content = webView//new ScrollPane{content = new VBox{content=drillDown(combination.toList)}}
      true
    }
  }

  class LazyTabPane(progSites: Seq[Tab]) extends TabPane {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    id = "source-tabs"
    tabs = progSites

    def init {
      progSites.foreach(_.asInstanceOf[TabWithService].populated)
    }
  }

  def buildTab(wettkampf: WettkampfView, service: KutuService) = {
    val progs = service.readWettkampfLeafs(wettkampf.programm.id)

    val progSites: Seq[Tab] = progs map {v =>
      new LazyLoadingTab(v, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id))
        }) {
        text = v.name
        closable = false
      }
    }
    val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )

    new WettkampfPage( new LazyTabPane(progSites ++ ranglisteSite))
  }
}

class WettkampfPage(tabPane: WettkampfPage.LazyTabPane)
  extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init
    tabPane
  }
}
