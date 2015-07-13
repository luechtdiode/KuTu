package ch.seidel

import ch.seidel.commons._
import ch.seidel.domain._
import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.beans.binding.BooleanBinding
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.property.StringProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.Pagination
import scalafx.scene.control.Tab
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.FlowPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.VBox
import scalafx.util.converter.StringConverterJavaToJavaDelegate
import scalafx.scene.control.Control
import scalafx.util.converter.DefaultStringConverter

case class WertungEditor(init: WertungView) {
	type WertungChangeListener = (WertungEditor) => Unit
  val noteD = DoubleProperty(init.noteD.toDouble)
  val noteE = DoubleProperty(init.noteE.toDouble)
  val endnote = DoubleProperty(init.endnote.toDouble)
  noteD.onChange {
    listeners.foreach(f => f(this))
  }
  noteE.onChange {
    listeners.foreach(f => f(this))
  }
  endnote.onChange {
    listeners.foreach(f => f(this))
  }
  def isDirty = noteD.value != init.noteD || noteE.value != init.noteE || endnote.value != init.endnote
  var listeners = Set[WertungChangeListener]()
  def addListener(l: WertungChangeListener) {
   listeners += l
  }
  def removeListener(l: WertungChangeListener) {
    listeners -= l
  }
  def reset {
    noteD.value = init.noteD
    noteE.value = init.noteE
    endnote.value = init.endnote
  }
  def commit = Wertung(
    init.id, init.athlet.id, init.wettkampfdisziplin.id, init.wettkampf.id,
    scala.math.BigDecimal(noteD.value),
    scala.math.BigDecimal(noteE.value),
    scala.math.BigDecimal(endnote.value),
    init.riege)
}

class WettkampfWertungTab(programm: ProgrammView, wettkampf: WettkampfView, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
	implicit def doublePropertyToObservableValue(p: DoubleProperty): ObservableValue[Double,Double] = p.asInstanceOf[ObservableValue[Double,Double]]

  override def isPopulated = {
    def updateWertungen = {
      athleten.filter(wv => wv.wettkampfdisziplin.programm.id == programm.id && wv.wettkampf.id == wettkampf.id).groupBy(wv => wv.athlet).map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
    }
    var wertungen = updateWertungen
    val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)
    val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
      id = "kutu-table"
      editable = true
    }

    var editorPane: EditorPane = null

    def disziplinCnt = wertungen.headOption match {case Some(w) => w.size case _ => 0}

    def updateEditorPane {
    	if(editorPane != null) {
    		editorPane.adjust
    	}
    }
    case class EditorPane(index: Int) extends VBox {
      var lastFocused: Option[Control] = None;
      val lblDisciplin = new Label() {
        styleClass += "toolbar-header"
      }
      val lblAthlet = new Label() {
        styleClass += "toolbar-header"
      }
      val lblHeader = new ToolBar {
        content = List(lblAthlet, lblDisciplin)
      }
      val txtD = new TextField() {
        promptText = "D-Note"
        prefWidth = 100
      }
      val txtE = new AutoFillTextBox[String]() {
        promptText = "E-Note"
        prefWidth = 500
        delegate.setListLimit(20)
//        prefHeight = 50
      }
//      txtE.prefWidth(100)
//      val txtE = new TextField() {
//        promptText = "E-Note"
//        prefWidth = 100
//      }
      val txtEnd = new TextField() {
        promptText = "End-Note"
        prefWidth = 100
      }
      val btnSaveNextDisciplin = new Button() {
        text = "Speichern ->"
        disable <== when(isDirty) choose false otherwise true
      }
      val btnSaveNextAthlet = new Button() {
        text = "Speichern v"
        disable <== when(isDirty) choose false otherwise true
      }
      val actionBox = new HBox() {
        content = List(btnSaveNextAthlet, btnSaveNextDisciplin)
      }
      val noteBox = new FlowPane() {
        alignment = Pos.CENTER
        content = List(txtD, txtE, txtEnd, actionBox)
      }
//      val noteBox = new HBox() {
//        content = List(txtD, txtE, txtEnd, actionBox)
//      }
      content = List(lblHeader, noteBox)
      alignment = Pos.CENTER
      var disciplin: WertungEditor = null

      val listener = (we: WertungEditor) => {
        if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
          txtD.text.value = disciplin.noteD.value
          txtD.delegate.selectAll
        }
        txtE.text.value = disciplin.init.wettkampfdisziplin.notenSpez.toString(disciplin.noteE.value)
        txtE.delegate.getTextbox.selectAll
        txtEnd.text.value = disciplin.endnote.value
      }

      def isDirty: BooleanBinding = new javafx.beans.binding.BooleanBinding() {
        bind(txtD.text, txtE.text, txtEnd.text)
        def computeValue = {
          try {
            disciplin.noteD.value != Wettkampf.fromString(txtD.text.value) ||
            disciplin.noteE.value != disciplin.init.wettkampfdisziplin.notenSpez.fromString(txtE.text.value) ||
            disciplin.endnote.value != Wettkampf.fromString(txtEnd.text.value)
          }
          catch {
            case e: Exception => false
          }
        }
      }

      def lastEditedOffset: Int = {
        if(txtD.focused.value) {
          lastFocused = Some(txtD)
          0
        }
        else if(txtE.focused.value) {
          lastFocused = Some(txtE)
          1
        }
        else 2
//        lastFocused match {
//          case textD => 0
//          case textE => 1
//          case _ =>
//            if(txtD.focused.value) {
//              lastFocused = Some(txtD)
//              0
//            }
//            else if(txtE.focused.value) {
//              lastFocused = Some(txtE)
//              1
//            }
//            else 2
//        }
      }

      def adjust = {
        unbind
//        println("inAdjustSelection at index " + index)
        val selected = wkview.selectionModel().getSelectedItem
        if (selected != null) {
          disciplin = selected(index)
          disciplin.addListener(listener)
          val rowIndex = wkModel.indexOf(selected)
          lblDisciplin.text = disciplin.init.wettkampfdisziplin.disziplin.name
          lblAthlet.text = selected(index).init.athlet.easyprint
          def save {
        	  lastFocused = List(txtD, txtE, txtEnd).find(p => p.isFocused())
            val td = txtD.text.value
            val te = txtE.text.value

            if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
              disciplin.noteD.value = disciplin.init.wettkampfdisziplin.notenSpez.fromString(td)
            }
            disciplin.noteE.value = disciplin.init.wettkampfdisziplin.notenSpez.fromString(te)
            disciplin.endnote.value = disciplin.init.wettkampfdisziplin.notenSpez.calcEndnote(disciplin.noteD.value, disciplin.noteE.value)
            if (disciplin.isDirty) {
              wkModel.update(rowIndex, selected.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              wkview.selectionModel.value.select(rowIndex, wkview.columns(index+3).columns(lastEditedOffset))
              wkview.scrollTo(rowIndex)
              wkview.scrollToColumn(wkview.columns(index+3).columns(lastEditedOffset))
            }
            wkview.selectionModel.value.selectBelowCell
          }
          listener(disciplin)
          //txtD.text.delegate.bindBidirectional(disciplin.noteD, NoteFormatter.asInstanceOf[scalafx.util.StringConverter[Number]])
          //txtD.text <==> disciplin.noteD
          txtEnd.editable = false
          if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
            txtD.editable = true
            txtD.visible = true
            txtD.onAction = () => save
          }
          else {
            txtD.editable = false
            txtD.visible = false
          }
          disciplin.init.wettkampfdisziplin.notenSpez.selectableItems.foreach { x =>
            txtE.delegate.getData().clear()
            for(s <- x) {
              txtE.delegate.getData().add(s)
            }
            txtE.delegate.setItemComparator(disciplin.init.wettkampfdisziplin.notenSpez)
          }
          txtE.onAction = () => save
//          txtEnd.onAction = () => save
          btnSaveNextAthlet.onAction = () => {
            save
            wkview.selectionModel.value.selectBelowCell
          }
          btnSaveNextDisciplin.onAction = () => {
            save
            if(index < disziplinCnt-1) {
              wkview.selectionModel.value.select(rowIndex, wkview.columns(index+3).columns(lastEditedOffset))
              wkview.scrollToColumn(wkview.columns(index+3).columns(lastEditedOffset))
              wkview.scrollTo(rowIndex)
            }
            else if(rowIndex < wkModel.size-1){
              wkview.selectionModel.value.select(rowIndex+1, wkview.columns(3).columns(lastEditedOffset))
              wkview.scrollToColumn(wkview.columns(3).columns(lastEditedOffset))
              wkview.scrollTo(rowIndex + 1)
            }
          }
//          println("binded")
          rowIndex
        }
        else {
          -1
        }
      }
//      adjust
      def unbind() {
        if (disciplin != null) {
          disciplin.removeListener(listener)
          txtD.text.unbind()
          txtD.onAction.unbind()
          txtE.text.unbind()
          txtE.onAction.unbind()
          txtEnd.text.unbind()
          txtEnd.onAction.unbind()
//          println("unbinded")
//          disciplin.noteD.unbind()
          //disciplin.noteE.unbind(txtE.text)
          //disciplin.endnote.unbind(txtEnd.text)
//        println(" disciplin unbinded")
        }
        disciplin = null
      }
    }

    val indexerE = Iterator.from(0)
    val indexerD = Iterator.from(0)
    val indexerF = Iterator.from(0)

    def wertungenCols = if (wertungen.nonEmpty) {
      wertungen.head.map { wertung =>
        val clDnote = new TableColumn[IndexedSeq[WertungEditor], Double] {
          val index = indexerD.next
          text = wertung.init.wettkampfdisziplin.disziplin.name
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteD else wertung.noteD }
          cellFactory = { _ => new TextFieldTableCell[IndexedSeq[WertungEditor], Double](wertung.init.wettkampfdisziplin.notenSpez)}

          styleClass += "table-cell-with-value"
          prefWidth = if(wertung.init.wettkampfdisziplin.notenSpez.isDNoteUsed) 60 else 0
          editable = wertung.init.wettkampfdisziplin.notenSpez.isDNoteUsed
          visible = wertung.init.wettkampfdisziplin.notenSpez.isDNoteUsed
          onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            val disciplin = evt.rowValue(index)
            disciplin.noteD.value = evt.newValue
            disciplin.endnote.value = wertung.init.wettkampfdisziplin.notenSpez.calcEndnote(disciplin.noteD.value, disciplin.noteE.value)
            val rowIndex = wkModel.indexOf(evt.rowValue)
            if (disciplin.isDirty) {
              wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              updateEditorPane
            }
            evt.tableView.requestFocus()
          }
        }
        val clEnote = new TableColumn[IndexedSeq[WertungEditor], Double] {
          val index = indexerE.next
          text = wertung.init.wettkampfdisziplin.disziplin.name
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteE else wertung.noteE }
          cellFactory = { x => new TextFieldTableCell[IndexedSeq[WertungEditor], Double](wertung.init.wettkampfdisziplin.notenSpez) }

          styleClass += "table-cell-with-value"
          prefWidth = 60
          editable = true
          //println(text, index)
          onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            val disciplin = evt.rowValue(index)
            disciplin.noteE.value = evt.newValue
            disciplin.endnote.value = wertung.init.wettkampfdisziplin.notenSpez.calcEndnote(disciplin.noteD.value, disciplin.noteE.value)
            val rowIndex = wkModel.indexOf(evt.rowValue)
            if (disciplin.isDirty) {
              wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              updateEditorPane
            }
            evt.tableView.requestFocus()
          }
        }
        val clEndnote = new TableColumn[IndexedSeq[WertungEditor], Double] {
          val index = indexerF.next
          text = wertung.init.wettkampfdisziplin.disziplin.name
          cellValueFactory = { x => if (x.value.size > index) x.value(index).endnote else wertung.endnote }
          //cellFactory = { x => new TextFieldTableCell[IndexedSeq[WertungEditor], Double](NoteFormatter) }

          styleClass += "table-cell-with-value"
          prefWidth = 80
//          editable = true
//          onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
//            val disciplin = evt.rowValue(index)
//            disciplin.endnote.value = evt.newValue
//            val rowIndex = wkModel.indexOf(evt.rowValue)
//            if (disciplin.isDirty) {
//              wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
//              evt.tableView.selectionModel.value.select(rowIndex, this)
//              updateEditorPane
//            }
//            evt.tableView.requestFocus()
//          }
        }
        val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] = new TableColumn[IndexedSeq[WertungEditor], String] {
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
      IndexedSeq[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]()
    }

    val athletCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Athlet"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "athlet", {
            val a = x.value.head.init.athlet
            s"${a.vorname} ${a.name}"
          })
        }
        prefWidth = 150
      },
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Verein"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "verein", {
            val a = x.value.head.init.athlet
            s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
          })
        }
        prefWidth = 100
      },
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Riege"
        styleClass += "table-cell-with-value"
//        cellValueFactory = { x => x.value.head.init.riege.getOrElse("keine Einteilung") }
        cellFactory = { x => new TextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter()) }
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "riege", {
            s"${x.value.head.init.riege.getOrElse("keine Einteilung")}"
          })
        }
        prefWidth = 100
        editable = true
        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
        	val rowIndex = wkModel.indexOf(evt.rowValue)
          for(disciplin <- evt.rowValue) {
            wkModel.update(rowIndex,
                evt.rowValue.updated(
                    evt.rowValue.indexOf(disciplin),
                    WertungEditor(
                        service.updateWertung(
                            disciplin.commit.copy(riege = if(evt.newValue.isEmpty()) None else Some(evt.newValue))
                            )
                        )
                    )
                )
          }
          updateEditorPane
          evt.tableView.requestFocus()
        }
      })

    val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Punkte"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", { f"${x.value.map(w => w.endnote.value.toDouble).sum}%3.3f" })}
        prefWidth = 80
        styleClass += "table-cell-with-value"
      })

    wkview.columns ++= athletCol ++ wertungenCols ++ sumCol

    val addButton = new Button {
      text = "Athlet hinzufügen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        disable = true
        val athletModel = ObservableBuffer[AthletView](
          service.selectAthletesView.filter(service.altersfilter(programm, _)).filter { p => wertungen.forall { wp => wp.head.init.athlet.id != p.id } }
        )
        val athletTable = new TableView[AthletView](athletModel) {
          columns ++= List(
            new TableColumn[AthletView, String] {
              text = "Athlet"
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "athlet", {
                  s"${x.value.vorname} ${x.value.name} (${x.value.verein.map { _.name }.getOrElse("ohne Verein")})"
                })
              }
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
            if (!athletTable.selectionModel().isEmpty) {
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
            if (!athletTable.selectionModel().isEmpty) {
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
        if (!wkview.selectionModel().isEmpty) {
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
        if (!wkview.selectionModel().isEmpty) {
          val selected = wkview.selectionModel().getSelectedItem
          var index = 0
          val rowIndex = wkModel.indexOf(selected)
          if(rowIndex > -1) {
            for (disciplin <- selected) {
              disciplin.noteD.value = 0
              disciplin.noteE.value = 0
              disciplin.endnote.value = 0
              if (disciplin.isDirty) {
                wkModel.update(rowIndex, selected.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
                wkview.requestFocus()
              }
              index = index + 1
            }
          }
        }
      }
    }
    val riegensuggestButton = new Button {
      text = "Riegen einteilen"
      minWidth = 75
      val stationen = new TextField()
      onAction = (event: ActionEvent) => {
        implicit val impevent = event
        stationen.text = wkModel.head.size.toString()
        PageDisplayer.showInDialog(text.value, new DisplayablePage() {
          def getPage: Node = {
            new HBox {
              prefHeight = 50
              alignment = Pos.BOTTOM_RIGHT
              hgrow = Priority.ALWAYS
              content = Seq(new Label("Stationen (wenn mehr wie eine Rotation, dann pro Rotation, getrennt mit Komma)  "), stationen)
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            if (!stationen.text.value.isEmpty) {
              val riegenzuteilungen = service.suggestRiegen(
                  wkModel.head.init.head.init.wettkampf.id,
                  stationen.text.value.split(",").foldLeft(Seq[Int]())((acc, s) => acc :+ str2Int(s))
                  )
              for{
                pair <- riegenzuteilungen
                w <- pair._2
              } {
                service.updateWertung(w)
              }
              wkModel.clear
              wertungen = updateWertungen
              wkModel.appendAll(wertungen)
              wkview.columns.clear()
              wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
            }
          }
        })
      }
    }
    //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
    removeButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
    clearButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
    wkview.selectionModel.value.setCellSelectionEnabled(true)

    val cont = new BorderPane {
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
          addButton, removeButton, clearButton, riegensuggestButton
        )
      }
      //bottom = pagination
    }

    val pagination = new Pagination(disziplinCnt, 0) {
      pageFactory = (index: Int) => {
        if(editorPane != null) {
          editorPane.unbind
        }
        editorPane = new EditorPane(index)
        val rowIndex = editorPane.adjust
        if(rowIndex > -1) {
          wkview.scrollTo(rowIndex)
          val datacolcnt = (wkview.columns.size - 4)
          val dcgrp = wkModel.headOption match {
            case Some(wertung) => if(wertung.head.init.wettkampfdisziplin.notenSpez.isDNoteUsed) 2 else 1
            case None => 2
          } //if(datacolcnt % 3 == 0) 2 else 1
          wkview.scrollToColumn(wkview.columns(3 + index).columns(dcgrp))
        }
        editorPane.requestLayout()
        editorPane
      }
    }

    wkview.focusModel.value.focusedCell.onChange {(focusModel, oldTablePos, newTablePos) =>
      if(newTablePos != null) {
        val datacolcnt = (wkview.columns.size - 4)
        val dcgrp = wkModel.headOption match {
            case Some(wertung) => if(wertung.head.init.wettkampfdisziplin.notenSpez.isDNoteUsed) 3 else 2
            case None => 3
          }//if(datacolcnt % 3 == 0) 3 else 2
        val idx = math.min(datacolcnt * dcgrp, math.max(0, (newTablePos.getColumn-3) / dcgrp))
//        println("Adjust pagination at " +idx)
        val oldIdx = pagination.currentPageIndex.value
        pagination.currentPageIndex.value = idx
        if(oldIdx == idx) {
          updateEditorPane
        }
      }
      else {
        cont.bottom = null
      }
    }
    cont.bottom = pagination

    content = cont
    /*content =new StackPane {
      alignmentInParent = Pos.TOP_LEFT*/

    true
  }
}
