package ch.seidel

import ch.seidel.commons._
import ch.seidel.domain._
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.collections.ObservableList
import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.beans.binding.BooleanBinding
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.property.StringProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry._
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.Control
import scalafx.scene.control.Label
import scalafx.scene.control.Pagination
import scalafx.scene.control.Tab
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.FlowPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.VBox
import scalafx.util.converter.DefaultStringConverter
import scalafx.util.converter.StringConverterJavaToJavaDelegate
import scalafx.scene.text.Text
import scalafx.scene.Scene
import scalafx.scene.Group
import scalafx.scene.text.Font
import scalafx.scene.text.FontWeight
import scalafx.scene.control.ComboBox

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

class WettkampfWertungTab(programm: Option[ProgrammView], riege: Option[String], wettkampf: WettkampfView, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
	implicit def doublePropertyToObservableValue(p: DoubleProperty): ObservableValue[Double,Double] = p.asInstanceOf[ObservableValue[Double,Double]]

  private var lazypane: Option[LazyTabPane] = None
  def setLazyPane(pane: LazyTabPane) {
    lazypane = Some(pane);
  }
  def refreshLazyPane() {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _=>
    }
  }
  override def isPopulated = {

    var editorPane: EditorPane = null

    def updateWertungen = {
      programm match {
        case Some(progrm) =>
          athleten.
          filter(wv => wv.wettkampfdisziplin.programm.id == progrm.id && wv.wettkampf.id == wettkampf.id).
          groupBy(wv => wv.athlet).
          map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
        case None =>
          athleten.
          filter(wv => wv.wettkampf.id == wettkampf.id).
          groupBy(wv => wv.athlet).
          map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
      }
    }

    var wertungen = updateWertungen
 		val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)
    var editingEditor: Option[WertungEditor] = None
    val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
      id = "kutu-table"
      editable = true
      onKeyPressed_= {evt: KeyEvent =>
        if(delegate.getEditingCell() == null && (
               Character.isAlphabetic(evt.character.charAt(0))
            || Character.isDigit(evt.character.charAt(0))
            || evt.code.isLetterKey
            || evt.code.isDigitKey)
            || evt.code.equals(KeyCode.DELETE)) {
          //evt.consume()
          val selelctedCell = selectionModel.value.getSelectedCells
          selelctedCell.foreach{tp =>
            edit(tp.row, tp.getTableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], Any]])
            if(evt.code.equals(KeyCode.DELETE)) {
              val column = tp.getTableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], Any]]
              if(column.parentColumn.value != null) {
                items.get.get(tp.row).find { x => x.init.wettkampfdisziplin.disziplin.name.equals(column.parentColumn.value.getText)}
                match {
                  case Some(editor) =>
                    editingEditor = Some(editor)
                    column.text.value match {
                      case "D" => editor.noteD.value = 0d
                      case "E" => editor.noteE.value = 0d
                      case _   =>
                    }

                  case None =>
                }
              }
              else {

              }
            }
          }
        }
      }
    }

    def disziplinCnt = wertungen.headOption match {case Some(w) => w.size case _ => 0}

    def updateEditorPane {
    	if(editorPane != null) {
        if(disziplinCnt > 0)
    		  editorPane.adjust
        else {
          editorPane.unbind();
        }
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
      //styleClass += "breadcrumb-bar"
//      val lblHeader = new ToolBar {
//        content = List(lblAthlet, lblDisciplin)
//      }
//      val txtD = new TextField() {
//        promptText = "D-Note"
//        prefWidth = 100
//      }
//      val txtE = new AutoFillTextBox[String]() {
//        promptText = "E-Note"
//        prefWidth = 500
//        delegate.setListLimit(20)
//      }
//      val txtEnd = new TextField() {
//        promptText = "End-Note"
//        prefWidth = 100
//      }
//      val btnSaveNextDisciplin = new Button() {
//        text = "Speichern ->"
//        disable <== when(isDirty) choose false otherwise true
//      }
//      val btnSaveNextAthlet = new Button() {
//        text = "Speichern v"
//        disable <== when(isDirty) choose false otherwise true
//      }
//      val actionBox = new HBox() {
//        children = List(btnSaveNextAthlet, btnSaveNextDisciplin)
//      }
//      val noteBox = new FlowPane() {
//        alignment = Pos.Center
//        children = List(txtD, txtE, txtEnd, actionBox)
//      }
      children = List(lblAthlet, lblDisciplin/*lblHeader, noteBox*/)
//      alignment = Pos.Center
      VBox.setMargin(lblAthlet, Insets(0d,10d,0d,20d))
      VBox.setMargin(lblDisciplin, Insets(0d,10d,0d,20d))
      var disciplin: WertungEditor = null

//      val listener = (we: WertungEditor) => {
//        if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
//          txtD.text.value = disciplin.noteD.value
//          txtD.delegate.selectAll
//        }
//        txtE.text.value = disciplin.init.wettkampfdisziplin.notenSpez.toString(disciplin.noteE.value)
//        txtE.delegate.getTextbox.selectAll
//        txtEnd.text.value = disciplin.endnote.value
//      }

//      def isDirty: BooleanBinding = new javafx.beans.binding.BooleanBinding() {
//        bind(txtD.text, txtE.text, txtEnd.text)
//        def computeValue = {
//          try {
//            disciplin.noteD.value != Wettkampf.fromString(txtD.text.value) ||
//            disciplin.noteE.value != disciplin.init.wettkampfdisziplin.notenSpez.fromString(txtE.text.value) ||
//            disciplin.endnote.value != Wettkampf.fromString(txtEnd.text.value)
//          }
//          catch {
//            case e: Exception => false
//          }
//        }
//      }
//
//      def lastEditedOffset: Int = {
//        if(txtD.focused.value) {
//          lastFocused = Some(txtD)
//          0
//        }
//        else if(txtE.focused.value) {
//          lastFocused = Some(txtE)
//          1
//        }
//        else 2
//      }

      def adjust: Int = {
       	val selected = wkview.selectionModel().getSelectedItem
        if(selected != null && disciplin != null && selected(index) == disciplin) {
          return wkModel.indexOf(selected)
        }
        unbind
//        println("inAdjustSelection at index " + index)
        if (selected != null && selected.size > index) {
          disciplin = selected(index)
//          txtD.disable = false
//          txtE.disable = false
//          txtEnd.disable = false
//
          val rowIndex = wkModel.indexOf(selected)
//          disciplin.addListener(listener)
          lblDisciplin.text = "Disziplin: " + disciplin.init.wettkampfdisziplin.disziplin.name
          lblAthlet.text = "Athlet: " + selected(index).init.athlet.easyprint
          //new Exception(f"on bind to index $index, ${selected(index).init.athlet.easyprint}").printStackTrace();
//          def save {
//        	  lastFocused = List(txtD, txtE, txtEnd).find(p => p.isFocused())
//            val td = txtD.text.value
//            val te = txtE.text.value
//
//            if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
//              disciplin.noteD.value = disciplin.init.wettkampfdisziplin.notenSpez.fromString(td)
//            }
//            disciplin.noteE.value = disciplin.init.wettkampfdisziplin.notenSpez.fromString(te)
//            disciplin.endnote.value = disciplin.init.wettkampfdisziplin.notenSpez.calcEndnote(disciplin.noteD.value, disciplin.noteE.value)
//            if (disciplin.isDirty) {
//              wkModel.update(rowIndex, selected.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
//              wkview.selectionModel.value.select(rowIndex, wkview.columns(index+3).columns(lastEditedOffset))
//              wkview.scrollTo(rowIndex)
//              wkview.scrollToColumn(wkview.columns(index+3).columns(lastEditedOffset))
//            }
//            //wkview.selectionModel.value.selectBelowCell
//          }
//          listener(disciplin)
//          txtEnd.editable = false
//          if(disciplin.init.wettkampfdisziplin.notenSpez.isDNoteUsed) {
//            txtD.editable = true
//            txtD.visible = true
//            txtD.onAction = () => save
//          }
//          else {
//            txtD.editable = false
//            txtD.visible = false
//          }
//          disciplin.init.wettkampfdisziplin.notenSpez.selectableItems.foreach { x =>
//            txtE.delegate.getData().clear()
//            for(s <- x) {
//              txtE.delegate.getData().add(s)
//            }
//            txtE.delegate.setItemComparator(disciplin.init.wettkampfdisziplin.notenSpez)
//          }
//          txtE.onAction = () => save
////          txtEnd.onAction = () => save
//          btnSaveNextAthlet.onAction = () => {
//            save
//            wkview.selectionModel.value.selectBelowCell
//          }
//          btnSaveNextDisciplin.onAction = () => {
//            save
//            if(index < disziplinCnt-1) {
//              wkview.selectionModel.value.select(rowIndex, wkview.columns(index+4).columns(lastEditedOffset))
//              wkview.scrollToColumn(wkview.columns(index+4).columns(lastEditedOffset))
//              wkview.scrollTo(rowIndex)
//            }
//            else if(rowIndex < wkModel.size-1){
//              wkview.selectionModel.value.select(rowIndex+1, wkview.columns(3).columns(lastEditedOffset))
//              wkview.scrollToColumn(wkview.columns(3).columns(lastEditedOffset))
//              wkview.scrollTo(rowIndex + 1)
//            }
//          }
////          println("binded")
          rowIndex
        }
        else {
          -1
        }
      }
//      adjust
      def unbind() {
//        if (disciplin != null) {
//          disciplin.removeListener(listener)
//          txtD.text.unbind()
//          txtD.onAction.unbind()
//          txtE.text.unbind()
//          txtE.onAction.unbind()
//          txtEnd.text.unbind()
//          txtEnd.onAction.unbind()
////        println(" disciplin unbinded")
//        }
//        txtD.disable = true
//        txtE.disable = true
//        txtEnd.disable = true
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
            editingEditor = None
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
          onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            editingEditor match {
              case Some(editor) =>
                if (editor.isDirty) {
                  editor.reset
                }
                editingEditor = None
              case None =>
            }
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
            editingEditor = None
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
          onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            editingEditor match {
              case Some(editor) =>
                if (editor.isDirty) {
                  editor.reset
                }
                editingEditor = None
              case None =>
            }
          }
        }
        val clEndnote = new TableColumn[IndexedSeq[WertungEditor], Double] {
          val index = indexerF.next
          text = wertung.init.wettkampfdisziplin.disziplin.name
          cellValueFactory = { x => if (x.value.size > index) x.value(index).endnote else wertung.endnote }
          styleClass += "table-cell-with-value"
          prefWidth = 80
        }
        val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] = new TableColumn[IndexedSeq[WertungEditor], String] {
          text = clEndnote.text.value
          clDnote.text = "D"
          clEnote.text = "E"
          clEndnote.text = "Endnote"
          delegate.impl_setReorderable(false)
          val measure = new Text(text.value)
          measure.font.value = Font(measure.font.value.getFamily, FontWeight.BOLD, measure.font.value.size)
          new Scene(new Group(measure))
          measure.applyCss()
          val w = measure.layoutBounds.value.width + 10
          prefWidth = math.min(200, w)
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
        delegate.impl_setReorderable(false) // shame on me??? why this feature should not be a requirement?
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
        delegate.impl_setReorderable(false)
        prefWidth = 100
      },
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Riege"
        styleClass += "table-cell-with-value"
        cellFactory = { x => new TextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter()) }
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "riege", {
            s"${x.value.head.init.riege.getOrElse("keine Einteilung")}"
          })
        }
        delegate.impl_setReorderable(false)
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
                            disciplin.commit.copy(riege = if(evt.newValue.trim.isEmpty()) None else Some(evt.newValue))
                            )
                        )
                    )
                )
          }
          refreshLazyPane()
          updateEditorPane
          evt.tableView.requestFocus()
        }
        onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
//          println(evt)
        }
      })

    val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new TableColumn[IndexedSeq[WertungEditor], String] {
        text = "Punkte"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", { f"${x.value.map(w => w.endnote.value.toDouble).sum}%3.3f" })}
        prefWidth = 100
        delegate.impl_setReorderable(false)
        styleClass += "table-cell-with-value"
      })

    wkview.columns ++= athletCol ++ wertungenCols ++ sumCol

    def setEditorPaneToDiscipline(index: Int): Node = {
      if(editorPane != null) {
        if(editorPane.index == index) {
          updateEditorPane
          return editorPane
        }
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
        }
        wkview.scrollToColumn(wkview.columns(3 + index).columns(dcgrp))
      }
      editorPane.requestLayout()
      editorPane
    }

    val pagination = new Pagination(disziplinCnt, 0) {
      disable = disziplinCnt == 0
      pageFactory = (index: Int) => {
        setEditorPaneToDiscipline(index)
      }
    }

    def reloadData() = {
      val selectionstore = wkview.selectionModel.value.getSelectedCells
      val coords = for(ts <- selectionstore) yield {
        if(ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
          //(ts.getRow, wkview.columns.indexOf(ts.getTableColumn.getParentColumn))
          (ts.getRow, (3 + editorPane.index) * -100)
        else
          (ts.getRow, ts.getColumn)
      }
      val idx = pagination.currentPageIndex.value

      val columnrebuild = wertungen.isEmpty
      wkModel.clear()
      wertungen = updateWertungen
      wkModel.appendAll(wertungen)
      if(columnrebuild) {
        wkview.columns.clear()
        wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
      }
      try {
        for(ts <- coords) {
          if(ts._2 < -100) {
            val toSelectParent = wkview.columns(ts._2 / -100);
            val firstVisible = toSelectParent.getColumns.find(p => p.width.value > 50d).getOrElse(toSelectParent.columns(0))
            wkview.selectionModel.value.select(ts._1, firstVisible)
            wkview.scrollToColumn(firstVisible)
          }
          else {
            wkview.selectionModel.value.select(ts._1, wkview.columns(ts._2))
            wkview.scrollToColumn(wkview.columns(ts._2))
          }
          wkview.scrollTo(ts._1)

        }
      }
      catch {
        case e: Exception =>
      }
//      setEditorPaneToDiscipline(idx)
      updateEditorPane
    }

    val actionButtons = programm match {
      case None =>
      val riegenRemoveButton = new Button {
        text = "Riege löschen"
        minWidth = 75
        onAction = (event: ActionEvent) => {
          for{
            wl <- wertungen
            if(wl.head.init.riege.equals(riege))
            w <- wl
          } {
            w.commit.copy(riege = None)
            service.updateWertung(w.commit.copy(riege = None))
          }
          refreshLazyPane()
          reloadData()
        }
      }
        List[Button](riegenRemoveButton)
      case Some(progrm) =>
      val addButton = new Button {
        text = "Athlet hinzufügen"
        minWidth = 75
        onAction = (event: ActionEvent) => {
          disable = true
          val athletModel = ObservableBuffer[AthletView](
            service.selectAthletesView.filter(service.altersfilter(progrm, _)).
            filter { p => /*p.activ &&*/ wertungen.forall { wp => wp.head.init.athlet.id != p.id } }.
            sortBy { a => (a.activ match {case true => "A" case _ => "X"}) + ":" + a.name + ":" + a.vorname }
          )
          val filteredModel = ObservableBuffer[AthletView](athletModel)
          val athletTable = new TableView[AthletView](filteredModel) {
            columns ++= List(
              new TableColumn[AthletView, String] {
                text = "Athlet"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    s"${x.value.vorname} ${x.value.name}"
                  })
                }
                //prefWidth = 150
              },
              new TableColumn[AthletView, String] {
                text = "Verein"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "verein", {
                    s"${x.value.verein.map { _.name }.getOrElse("ohne Verein")}"
                  })
                }
                //prefWidth = 150
              },
              new TableColumn[AthletView, String] {
                text = "Status"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "status", {
                    x.value.activ match {case true => "Aktiv" case _ => "Inaktiv"}
                  })
                }
                //prefWidth = 150
              }

            )
          }
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList;
              filteredModel.clear()
              val search = newVal.toUpperCase()
              for(athlet <- athletModel) {
                if(search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                  filteredModel.add(athlet)
                }
                else if(athlet.vorname.toUpperCase().contains(search)) {
                  filteredModel.add(athlet)
                }
                else if(athlet.verein match {case Some(v) => v.name.toUpperCase().contains(search) case None => false}) {
                  filteredModel.add(athlet)
                }
              }
              athletTable.sortOrder.clear()
              val restored = athletTable.sortOrder ++= sortOrder
            }
          }
          implicit val impevent = event
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                top = filter
                center = athletTable
                minWidth = 350
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              if (!athletTable.selectionModel().isEmpty) {
                val athlet = athletTable.selectionModel().getSelectedItem
                def filter(progId: Long, a: Athlet): Boolean = a.id == athlet.id
                service.assignAthletsToWettkampf(wettkampf.id, Set(progrm.id), Some(filter))
                reloadData()
              }
            }
          }, new Button("OK, Alle") {
            onAction = (event: ActionEvent) => {
              if (!filteredModel.isEmpty) {
                val athlet = athletTable.selectionModel().getSelectedItem
                def filter(progId: Long, a: Athlet): Boolean = filteredModel.exists { x => x.id == a.id }
                service.assignAthletsToWettkampf(wettkampf.id, Set(progrm.id), Some(filter))
                reloadData()
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
      val moveToOtherProgramButton = new Button {
        text = programm.map(p => p.head.id match {case 20 => "Turner Kategorie wechseln ..." case 1  => "." case _ => "Turner Programm wechseln ..."}).getOrElse(".")
        minWidth = 75
        onAction = (event: ActionEvent) => {
          implicit val impevent = event
          val programms = programm.map(p => service.readWettkampfLeafs(p.head.id)).get
          val prmodel = ObservableBuffer[ProgrammView](programms)
          val cbProgramms = new ComboBox[ProgrammView] {
            items = prmodel
          }
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new HBox {
                prefHeight = 50
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(new Label("Neue Zuteilung  "), cbProgramms)
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              if (!wkview.selectionModel().isEmpty) {
                val athletwertungen = wkview.selectionModel().getSelectedItem.map(_.init.id).toSet
                val athlet = wkview.selectionModel().getSelectedItem.map(_.init.athlet).head
                service.unassignAthletFromWettkampf(athletwertungen)
                wkModel.remove(wkview.selectionModel().getSelectedIndex)
                def filter(progId: Long, a: Athlet): Boolean = a.id == athlet.id
                service.assignAthletsToWettkampf(wettkampf.id, Set(cbProgramms.selectionModel().selectedItem.value.id), Some(filter))
              }
            }
          })
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
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(new Label("Stationen (wenn mehr wie eine Rotation, dann pro Rotation, getrennt mit Komma)  "), stationen)
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              if (!stationen.text.value.isEmpty) {
                val riegenzuteilungen = service.suggestRiegen(
                    wkModel.head.init.head.init.wettkampf.id,
                    stationen.text.value.split(",").foldLeft(Seq[Int]()){(acc, s) => acc :+ str2Int(s)}
                )
                for{
                  pair <- riegenzuteilungen
                  w <- pair._2
                } {
                  service.updateWertung(w)
                }
                refreshLazyPane()
                reloadData()
              }
            }
          })
        }
      }
      //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      val moveAvaillable = programm.forall { p => p.head != 1 }
      moveToOtherProgramButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose moveAvaillable otherwise false
      removeButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      riegensuggestButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

      List(addButton, moveToOtherProgramButton, removeButton, riegensuggestButton).filter(btn => !btn.text.value.equals("."))
    }

    wkview.selectionModel.value.setCellSelectionEnabled(true)
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
    clearButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

    onSelectionChanged = handle {
      if(selected.value) {
        reloadData()
      }
    }
    val editTablePane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      center = wkview
    }
    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      center = editTablePane
      top = new ToolBar {
        content = List(
          new Label {
            text = (programm match {
              case Some(progrm) =>
                s"Programm ${progrm.name}  "
              case None => ""
            }) + " " + (riege match {
              case Some(r) =>
                s"Riege ${r}  "
              case None => ""
            }).trim
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          }
          //addButton, removeButton, clearButton, riegensuggestButton
        ) ++ actionButtons :+ clearButton
      }
      //bottom = pagination
    }
    wkview.selectionModel.value.selectedItemProperty().onChange(
        (model: scalafx.beans.value.ObservableValue[IndexedSeq[ch.seidel.WertungEditor], IndexedSeq[ch.seidel.WertungEditor]],
         oldSelection: IndexedSeq[ch.seidel.WertungEditor],
         newSelection: IndexedSeq[ch.seidel.WertungEditor]) => {
      if(newSelection == null /*|| editorPane == null || editorPane.disciplin == null*/) {
        updateEditorPane
      }
    })

    wkview.focusModel.value.focusedCell.onChange {(focusModel, oldTablePos, newTablePos) =>
      if(newTablePos != null) {
        if(newTablePos.row < 0) {
          if(editorPane != null) editorPane.unbind()
          editTablePane.top = null
        }
        else {
          val datacolcnt = (wkview.columns.size - 4)
          val dcgrp = wkModel.headOption match {
              case Some(wertung) => if(wertung.head.init.wettkampfdisziplin.notenSpez.isDNoteUsed) 3 else 2
              case None => 3
            }//if(datacolcnt % 3 == 0) 3 else 2
          val idx = math.min(datacolcnt * dcgrp, math.max(0, (newTablePos.getColumn-3) / dcgrp))
  //        println("Adjust pagination at " +idx)
          val oldIdx = pagination.currentPageIndex.value
//          pagination.currentPageIndex.value = idx
          editTablePane.top = setEditorPaneToDiscipline(idx)
          if(oldIdx == idx) {
            updateEditorPane
          }
        }
      }
      else {
        cont.bottom = null
      }
    }
    //cont.bottom = pagination

    content = cont
    /*content =new StackPane {
      alignmentInParent = Pos.TOP_LEFT*/

    true
  }
}
