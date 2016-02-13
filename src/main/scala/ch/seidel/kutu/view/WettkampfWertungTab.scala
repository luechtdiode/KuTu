package ch.seidel.kutu.view

import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry._
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.Tab
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.VBox
import scalafx.util.converter.DefaultStringConverter
import scalafx.scene.control.ComboBox
import scalafx.scene.input.Clipboard
import scala.io.Source
import java.text.SimpleDateFormat
import scalafx.scene.control.SelectionMode
import scalafx.application.Platform
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.awt.Desktop
import javafx.scene.{control => jfxsc}
import scala.IndexedSeq
import scalafx.beans.property._
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.layout.StackPane
import scalafx.scene.web.WebView
import ch.seidel.commons._
import ch.seidel.kutu.renderer.NotenblattToHtmlRenderer
import ch.seidel.kutu.domain._
import scalafx.scene.control.CheckBox
import scalafx.scene.control.SplitPane
import ch.seidel.kutu.renderer.KategorieTeilnehmerToHtmlRenderer
import scalafx.scene.control.cell.ComboBoxTableCell
import scalafx.util.StringConverter
import ch.seidel.kutu.KuTuApp
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import ch.seidel.kutu.data.ResourceExchanger

object RiegeEditor {
  def apply(wettkampfid: Long, anz: Int, enabled: Boolean, riege: Riege, onNameChange: (String, String) => Unit, onSelectedChange: (String, Boolean) => Boolean): RiegeEditor =
    RiegeEditor(wettkampfid, riege.r, anz, enabled, riege.durchgang, riege.start, onNameChange, onSelectedChange)
}
case class RiegeEditor(wettkampfid: Long, initname: String, initanz: Int, enabled: Boolean, initdurchgang: Option[String], initstart: Option[Disziplin], onNameChange: (String, String) => Unit, onSelectedChange: (String, Boolean) => Boolean) {
  val selected = BooleanProperty(enabled)
  val name = StringProperty(initname)
  val anz = IntegerProperty(initanz)
  val durchgang = StringProperty(initdurchgang.getOrElse(""))
  val start = ObjectProperty(initstart.getOrElse(null))
//  name onChange {
//    onNameChange(initname, name.value)
//  }
  selected onChange {
    selected.value = onSelectedChange(initname, selected.value)
  }
//  durchgang onChange {
//    durchgang.value = onSelectedChange(initdurchgang.getOrElse(""), durchgang.value)
//  }
//  start onChange {
//    selected.value = onSelectedChange(initname, selected.value)
//  }
  def reset {
    name.value = initname
    durchgang.value = initdurchgang.getOrElse("")
    start.value = initstart.getOrElse(null)
  }
  def commit = RiegeRaw (
      wettkampfId = wettkampfid,
      r = name.value,
      durchgang = if(durchgang.value.trim.length > 0) Some(durchgang.value.trim) else None,
      start = if(start.value != null && start.value.isInstanceOf[Disziplin]) Some(start.value.asInstanceOf[Disziplin].id) else None
    )
}
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
    init.riege,
    init.riege2)
}

trait TCAccess {
  def getIndex: Int
  def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor
}

class WKJFSCTableColumn[T](val index: Int) extends jfxsc.TableColumn[IndexedSeq[WertungEditor], T] with TCAccess {
  override def getIndex: Int = index
  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
}

class WKTableColumn[T](val index: Int) extends TableColumn[IndexedSeq[WertungEditor], T] with TCAccess {
  override val delegate: jfxsc.TableColumn[IndexedSeq[WertungEditor], T] = new WKJFSCTableColumn[T](index)
  override def getIndex: Int = index
  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
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
  def refreshOtherLazyPanes() {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _=>
    }
  }

  case class EditorPane(wkview: TableView[IndexedSeq[WertungEditor]]) extends HBox {
    var index = -1
//    var lastFocused: Option[Control] = None;
    var selected: IndexedSeq[WertungEditor] = IndexedSeq()

    val lblDisciplin = new Label() {
      styleClass += "toolbar-header"
    }
    val lblAthlet = new Label() {
      styleClass += "toolbar-header"
    }

    wkview.selectionModel.value.selectedItemProperty().onChange(
        (model: scalafx.beans.value.ObservableValue[IndexedSeq[WertungEditor], IndexedSeq[WertungEditor]],
         oldSelection: IndexedSeq[WertungEditor],
         newSelection: IndexedSeq[WertungEditor]) => {
      if(newSelection != null && selected != newSelection) {
        selected = newSelection
        adjust
      }
      else if(newSelection == null) {
        selected = null
        index = -1
        adjust
      }
    })

    wkview.focusModel.value.focusedCell.onChange {(focusModel, oldTablePos, newTablePos) =>
      if(newTablePos != null && selected != null) {
        val column = newTablePos.tableColumn
        val selrow = newTablePos.getRow
        if(column != null && selrow > -1) {
          if(column.isInstanceOf[TCAccess]) {
            val selectedIndex = column.asInstanceOf[TCAccess].getIndex
            if(selectedIndex > -1 && selectedIndex != index) {
              index = selectedIndex
              adjust
            }
            else if(selectedIndex < 0) {
              index = -1
              adjust
            }
          }
          else {
            index = -1
            adjust
          }
        }
        else {
          index = -1
          adjust
        }
      }
    }

    children = List(lblAthlet, lblDisciplin/*lblHeader, noteBox*/)
    VBox.setMargin(lblAthlet, Insets(0d,10d,0d,20d))
    VBox.setMargin(lblDisciplin, Insets(0d,10d,0d,40d))

    def adjust {
      if(selected != null && index > -1 && index < selected.size) {
        lblAthlet.text.value = selected(index).init.athlet.easyprint
        lblDisciplin.text.value = " : " + selected(index).init.wettkampfdisziplin.easyprint
      }
      else if(selected != null && selected.size > 0) {
        lblAthlet.text.value = selected(0).init.athlet.easyprint
        lblDisciplin.text.value = " : " + Seq(selected(0).init.riege, selected(0).init.riege2).map(_.getOrElse("")).filter { _.length() > 0 }.mkString(", ")
      }
      else {
        lblAthlet.text.value = ""
        lblDisciplin.text.value = ""
      }
    }
  }

  import scalafx.print._
  import scalafx.scene.web.WebEngine
  def printHtml(html: String, engine: WebEngine, datadirectory: File) {
    val printer = Printer.defaultPrinter;
    val pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.Landscape, Printer.MarginType.HardwareMinimum);

    val job = PrinterJob.createPrinterJob;
    if (job != null) {
      job.jobSettings.pageLayout = pageLayout
      if(job.showPrintDialog(null)) {
        engine.loadContent(html)
        engine.print(job)
        engine.userDataDirectory.value = datadirectory
        job.endJob
      }
    }
  }

  override def isPopulated = {
    def defaultFilter: (WertungView) => Boolean = {wertung =>
      programm match {
        case Some(progrm) =>
          wertung.wettkampfdisziplin.programm.id == progrm.id
        case None =>
          true
      }
    }
    def reloadWertungen(extrafilter: (WertungView) => Boolean = defaultFilter) = {
      athleten.
      filter(wv => wv.wettkampf.id == wettkampf.id).
      filter(extrafilter).
      groupBy(wv => wv.athlet).
      map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
    }

    val webView = new WebView
    var wertungen = reloadWertungen()
 		val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)
    var editingEditor: Option[WertungEditor] = None
    val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
      id = "kutu-table"
      editable = true
    }
    var relevantRiegen: Map[String,Boolean] = (if(wertungen.size > 0) wertungen.map(x => x.head).flatMap(x => Seq(x.init.riege, x.init.riege2).flatten).toSet else Set.empty[String]).map(x => x -> true).toMap
    def riegen(onNameChange: (String, String) => Unit, onSelectedChange: (String, Boolean) => Boolean, initial: Boolean): IndexedSeq[RiegeEditor] = {
      service.listRiegenZuWettkampf(wettkampf.id).sortBy(r => r._1).map(x =>
        RiegeEditor(
            wettkampf.id,
            x._1,
            x._2,
            relevantRiegen.contains(x._1) && (initial || relevantRiegen(x._1)),
            x._3,
            x._4,
            onNameChange, onSelectedChange))
    }
    val riegenFilterModel = ObservableBuffer[RiegeEditor]()

    val editorPane: EditorPane = new EditorPane(wkview)
    val disziplinlist = wertungen.headOption match {case Some(w) => w.map(_.init.wettkampfdisziplin.disziplin) case _ => IndexedSeq[Disziplin]()}
    def disziplinCnt = wertungen.headOption match {case Some(w) => w.size case _ => 0}
    val withDNotes = wertungen.flatMap(w => w.filter(ww => ww.init.wettkampfdisziplin.notenSpez.isDNoteUsed)).nonEmpty
    val withENotes = wettkampf.programm.id != 1

    def updateEditorPane {
		  editorPane.adjust
    }

    val indexerE = Iterator.from(0)
    val indexerD = Iterator.from(0)
    val indexerF = Iterator.from(0)

    def wertungenCols = if (wertungen.nonEmpty) {
      wertungen.head.map { wertung =>
        lazy val clDnote = new WKTableColumn[Double](indexerD.next) {
          text = "D"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteD else wertung.noteD }
          cellFactory = { _ => new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](wertung.init.wettkampfdisziplin.notenSpez)}

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
        lazy val clEnote = new WKTableColumn[Double](indexerE.next) {
          text = "E"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteE else wertung.noteE }
          cellFactory = { x => new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](wertung.init.wettkampfdisziplin.notenSpez) }

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
        lazy val clEndnote = new WKTableColumn[Double](indexerF.next) {
          text = "Endnote"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).endnote else wertung.endnote }
          styleClass += "table-cell-with-value"
          prefWidth = 80
        }
        val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] =  if(withDNotes) {
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            delegate.impl_setReorderable(false)
            columns ++= Seq(clDnote, clEnote, clEndnote)
          }
        }
        else {
          clEnote.text = wertung.init.wettkampfdisziplin.disziplin.name
          clEnote
        }
        cl
      }
    }
    else {
      IndexedSeq[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]()
    }

    val athletCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new WKTableColumn[String](-1) {
        text = "Athlet"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "athlet", {
            val a = x.value.head.init.athlet
            s"${a.vorname} ${a.name} ${(a.gebdat match {case Some(d) => f"$d%tY "; case _ => " "}) }"
          })
        }
//        delegate.impl_setReorderable(false) // shame on me??? why this feature should not be a requirement?
        prefWidth = 150
      },
      new WKTableColumn[String](-1) {
        text = "Verein"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "verein", {
            val a = x.value.head.init.athlet
            s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
          })
        }
//        delegate.impl_setReorderable(false)
        prefWidth = 100
      },
      new WKTableColumn[String](-1) {
        text = "Riege"
        cellFactory = { x =>
          new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
        }
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "riege", {
            s"${x.value.head.init.riege.getOrElse("keine Einteilung")}"
          })
        }
//        delegate.impl_setReorderable(false)
        prefWidth = 100
        editable = true
        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
          if(!evt.newValue.equals("keine Einteilung")) {
          	val rowIndex = wkModel.indexOf(evt.rowValue)
            for(wertung <- evt.rowValue) {
              wkModel.update(rowIndex,
                  evt.rowValue.updated(
                      evt.rowValue.indexOf(wertung),
                      WertungEditor(
                          service.updateWertung(
                              wertung.commit.copy(riege = if(evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None else Some(evt.newValue))
                              )
                          )
                      )
                  )
            }
            refreshOtherLazyPanes()
            updateEditorPane
            evt.tableView.requestFocus()
          }
        }
        onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
//          println(evt)
        }
      },
      new WKTableColumn[String](-1) {
        text = "Riege 2"
        cellFactory = { x =>
          new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
        }
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "riege2", {
            s"${x.value.head.init.riege2.getOrElse("keine Einteilung")}"
          })
        }
//        delegate.impl_setReorderable(false)
        prefWidth = 100
        editable = true
        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
        	if(!evt.newValue.equals("keine Einteilung")) {
            val rowIndex = wkModel.indexOf(evt.rowValue)
            for(disciplin <- evt.rowValue) {
              wkModel.update(rowIndex,
                  evt.rowValue.updated(
                      evt.rowValue.indexOf(disciplin),
                      WertungEditor(
                          service.updateWertung(
                              disciplin.commit.copy(riege2 = if(evt.newValue.trim.isEmpty()) None else Some(evt.newValue))
                              )
                          )
                      )
                  )
            }
            refreshOtherLazyPanes()
            updateEditorPane
            evt.tableView.requestFocus()
        	}
        }
        onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
//          println(evt)
        }
      })

    val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new WKTableColumn[String](-1) {
        text = "Punkte"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", { f"${x.value.map(w => w.endnote.value.toDouble).sum}%3.3f" })}
        prefWidth = 100
        delegate.impl_setReorderable(false)
        styleClass += "table-cell-with-value"
      })

    wkview.columns ++= athletCol ++ wertungenCols ++ sumCol

    def setEditorPaneToDiscipline(index: Int): Node = {
      editorPane.adjust
//      if(rowIndex > -1) {
//        wkview.scrollTo(rowIndex)
//        val datacolcnt = (wkview.columns.size - 4)
//        val dcgrp = wkModel.headOption match {
//          case Some(wertung) => if(wertung.head.init.wettkampfdisziplin.notenSpez.isDNoteUsed) 2 else 1
//          case None => 2
//        }
//        wkview.scrollToColumn(wkview.columns(3 + index).columns(dcgrp))
//      }
      editorPane.requestLayout()
      editorPane
    }

    var lastFilter = ""

    def updateFilteredList(newVal: String) {
      //if(!newVal.equalsIgnoreCase(lastFilter)) {
        lastFilter = newVal
        val sortOrder = wkview.sortOrder.toList;
        wkModel.clear()
        val searchQuery = newVal.toUpperCase().split(" ")
        for{athlet <- wertungen
        } {
          def isRiegenFilterConform(wertung: WertungView) = {
            val athletRiegen = Seq(wertung.riege, wertung.riege2)
            val undefined = athletRiegen.forall{case None => true case _ => false}
            undefined || !athletRiegen.forall{case Some(riege) => !relevantRiegen.getOrElse(riege, false) case _ => true}
          }
          val matches = athlet.nonEmpty && isRiegenFilterConform(athlet(0).init) &&
            searchQuery.forall{search =>
            if(search.isEmpty() || athlet(0).init.athlet.name.toUpperCase().contains(search)) {
              true
            }
            else if(athlet(0).init.athlet.vorname.toUpperCase().contains(search)) {
              true
            }
            else if(athlet(0).init.athlet.verein match {case Some(v) => v.name.toUpperCase().contains(search) case None => false}) {
              true
            }
            else if(athlet(0).init.riege match {case Some(r) => r.toUpperCase().contains(search) case None => false}) {
              true
            }
            else if(athlet(0).init.riege2 match {case Some(r) => r.toUpperCase().contains(search) case None => false}) {
              true
            }
            else {
              false
            }
          }

          if(matches) {
            wkModel.add(athlet)
          }
        }
        wkview.sortOrder.clear()
        val restored = wkview.sortOrder ++= sortOrder
      //}
  	}
  	val txtUserFilter = new TextField() {
      promptText = "Athlet-Filter"
      text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
        if(!lastFilter.equalsIgnoreCase(newVal)) {
          updateFilteredList(newVal)
        }
      }
    }

    val alleRiegenCheckBox = new CheckBox {
      text = "Alle Riegen" + programm.map(" im " + _.name).getOrElse("")
      margin = Insets(5, 0, 5, 5)
    }
    def updateAlleRiegenCheck(toggle: Boolean = false) {
      val allselected = relevantRiegen.values.forall{x => x}
      val newAllSelected = if(toggle) !allselected else allselected
      if(toggle) {
        relevantRiegen = relevantRiegen.map(r => (r._1, newAllSelected))
        updateFilteredList(lastFilter)
        updateRiegen(false)
      }
      else {
        alleRiegenCheckBox.selected.value = newAllSelected
      }
    }
    alleRiegenCheckBox onAction = (event: ActionEvent) => {
      updateAlleRiegenCheck(true)
    }

    def updateRiegen(initial: Boolean) {
      def onNameChange(name1: String, name2: String) = {
        reloadData()
      }
      def onSelectedChange(name: String, selected: Boolean) = {
        if(relevantRiegen.contains(name)) {
          relevantRiegen = relevantRiegen.updated(name, selected)
          updateFilteredList(lastFilter)
          updateAlleRiegenCheck()
          selected
        }
        else {
          false
        }
      }
      riegenFilterModel.clear()
      riegen(onNameChange, onSelectedChange, initial).foreach(riegenFilterModel.add(_))
      if(initial) {
        updateAlleRiegenCheck()
      }
    }

    def reloadData() = {
      val selectionstore = wkview.selectionModel.value.getSelectedCells
      val coords = for(ts <- selectionstore) yield {
        if(ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
          (ts.getRow, (3 + editorPane.index) * -100)
        else
          (ts.getRow, ts.getColumn)
      }

      val columnrebuild = wertungen.isEmpty
      wkModel.clear()
      wertungen = reloadWertungen()
      relevantRiegen = (if(wertungen.size > 0) wertungen.map(x => x.head).flatMap(x => Seq(x.init.riege, x.init.riege2).flatten).toSet else Set.empty[String]).map(x => x -> relevantRiegen.getOrElse(x, true)).toMap

      updateRiegen(false)

      lastFilter = ""
      updateFilteredList(lastFilter)
      txtUserFilter.text.value = lastFilter
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

    val riegenFilterView = new TableView[RiegeEditor](riegenFilterModel) {
      id = "riege-table"
      editable = true
      columns ++= List(
          new TableColumn[RiegeEditor, Boolean] {
            text = "Filter"
            cellValueFactory = { x => x.value.selected.asInstanceOf[ObservableValue[Boolean,Boolean]] }
            cellFactory = { x => new CheckBoxTableCell[RiegeEditor, Boolean]() }
            editable = true
          }
         ,new TableColumn[RiegeEditor, String] {
            text = "Riege"
            cellValueFactory = { x => x.value.name }
            cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
              val editor = evt.rowValue
              editor.name.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              service.renameRiege(wettkampf.id, evt.rowValue.initname, evt.newValue)
              refreshLazyPane()
              reloadData()
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          }
         ,new TableColumn[RiegeEditor, Int] {
            text = "Anz"
            cellValueFactory = { x => x.value.anz.asInstanceOf[ObservableValue[Int,Int]] }
          }
         ,new TableColumn[RiegeEditor, String] {
            text = "Durchgang"
            prefWidth = 150
            cellValueFactory = { x => x.value.durchgang }
            cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
              val editor = evt.rowValue
              editor.durchgang.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              riegenFilterModel.update(rowIndex, RiegeEditor(
                  evt.rowValue.wettkampfid,
                  evt.rowValue.initanz,
                  evt.rowValue.enabled,
                  service.updateOrinsertRiege(evt.rowValue.commit),
                  evt.rowValue.onNameChange,
                  evt.rowValue.onSelectedChange))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          }
         ,new TableColumn[RiegeEditor, Disziplin] {
            text = "Start"
            prefWidth = 150
            val converter = new StringConverter[Disziplin] {
              override def toString(d: Disziplin) = if(d != null) d.easyprint else ""
              override def fromString(s: String) = if(s != null) disziplinlist.find { d => d.name.equals(s) }.getOrElse(null) else null
            }
            val list = ObservableBuffer[Disziplin](disziplinlist)
            cellValueFactory = { x => x.value.start.asInstanceOf[ObservableValue[Disziplin,Disziplin]] }
            cellFactory = { _ => new ComboBoxTableCell[RiegeEditor, Disziplin](converter, list) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, Disziplin]) => {
              val editor = evt.rowValue
              editor.start.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              riegenFilterModel.update(rowIndex, RiegeEditor(
                  evt.rowValue.wettkampfid,
                  evt.rowValue.initanz,
                  evt.rowValue.enabled,
                  service.updateOrinsertRiege(evt.rowValue.commit),
                  evt.rowValue.onNameChange,
                  evt.rowValue.onSelectedChange))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          })
    }
    updateRiegen(true)

    def doPasteFromExcel(progrm: Option[ProgrammView])(implicit event: ActionEvent) = {
      import scala.util.{Try, Success, Failure}
      val athletModel = ObservableBuffer[(Long, Athlet, AthletView)]()
      val vereineList = service.selectVereine
      val vereineMap = vereineList.map(v => v.id -> v).toMap
      val vereine = ObservableBuffer[Verein](vereineList)
      val cbVereine = new ComboBox[Verein] {
        items = vereine
        //selectionModel.value.selectFirst()
      }
      val programms = programm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
      val clipboardlines = Source.fromString(Clipboard.systemClipboard.getString).getLines()
      val cache = new java.util.ArrayList[MatchCode]()
      val cliprawf = KuTuApp.invokeAsyncWithBusyIndicator {
                     clipboardlines.
                     map    { line   => line.split("\\t") }.
                     filter { fields => fields.length > 2 }.
                     map    { fields =>
                        val parsed = Athlet(
                            id = 0,
                            js_id = "",
                            geschlecht = if(!"".equals(fields(4))) "W" else "M",
                            name = fields(0),
                            vorname = fields(1),
                            gebdat = if(fields(2).length > 4)
                                Some(service.getSQLDate(fields(2)))
                              else if(fields(2).length == 4) {
                                Some(service.getSQLDate("01.01." + fields(2)))
                              }
                              else {
                                None
                              },
                            strasse = "",
                            plz = "",
                            ort = "",
                            verein = None,
                            activ = true
                            )
                        val candidate = service.findAthleteLike(cache)(parsed)
                        val progId: Long = try {
                          programms(Integer.valueOf(fields(3))-1).id
                        }
                        catch {
                          case d: Exception =>
                            progrm match {
                              case Some(p) => p.id
                              case None => 0L
                            }
                        }
                       (progId, parsed, AthletView(
                           candidate.id, candidate.js_id,
                           candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
                           candidate.strasse, candidate.plz, candidate.ort,
                           candidate.verein.map(vereineMap), true))
                    }.toList
      }
      import scala.concurrent.ExecutionContext.Implicits._
      cliprawf.andThen {
        case Failure(t) => println(t)
        case Success(clipraw) => Platform.runLater{
//        val clipraw = Await.result(cliprawf, Duration.Inf)
        if(clipraw.nonEmpty) {
          athletModel.appendAll(clipraw)
          clipraw.find(a => a._3.verein match{case Some(v) => true case _ => false}) match {
                          case Some((id, athlet, candidate)) =>
                            cbVereine.selectionModel.value.select(candidate.verein.get)
                          case _ =>
                        }
          val filteredModel = ObservableBuffer[(Long, Athlet, AthletView)](athletModel)
          val athletTable = new TableView[(Long, Athlet, AthletView)](filteredModel) {
            columns ++= List(
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = "Athlet"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    s"${x.value._2.name} ${x.value._2.vorname}, ${x.value._2.gebdat.map(d => f"$d%tY") match {case None => "" case Some(t) => t}}"
                  })
                }
                minWidth = 250
              },
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = programm.map(p => p.head.id match {case 20 => "Kategorie" case 1  => "." case _ => "Programm"}).getOrElse(".")
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "programm", {
                   programms.find { p => p.id == x.value._1 || p.aggregatorHead.id == x.value._1} match {case Some(programm)=> programm.name case _ => "unbekannt"}
                  })
                }
              },
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = "Import-Vorschlag"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "dbmatch", {
                    if(x.value._3.id > 0) "als " + x.value._3.easyprint else "wird neu importiert"
                  })
                }
              }
            )
          }
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.MULTIPLE)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList;
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for{(progrid, athlet, vorschlag) <- athletModel
              } {
                val matches = searchQuery.forall{search =>
                  if(search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if(athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if(vorschlag.verein match {case Some(v) => v.name.toUpperCase().contains(search) case None => false}) {
                    true
                  }
                  else {
                    false
                  }
                }

                if(matches) {
                  filteredModel.add((progrid, athlet, vorschlag))
                }
              }
              athletTable.sortOrder.clear()
              val restored = athletTable.sortOrder ++= sortOrder
            }
          }
          PageDisplayer.showInDialog("Aus Excel einfügen ...", new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                minWidth = 600
                top = new HBox {
                  prefHeight = 50
                  alignment = Pos.BottomRight
                  hgrow = Priority.Always
                  children = Seq(new Label("Turner/-Innen aus dem Verein  "), cbVereine)
                }
                center = new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  top = filter
                  center = athletTable
                  minWidth = 550
                }

              }
            }
          }, new Button("OK") {
            disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
            onAction = (event: ActionEvent) => {
              if (!athletTable.selectionModel().isEmpty) {
                val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
                  x => athletTable.selectionModel.value.isSelected(x._2)
                }.map {x =>
                  val ((progrId, importathlet, candidateView),idx) = x
                  val id = if(candidateView.id > 0 &&
                             (importathlet.gebdat match {
                               case Some(d) =>
                                 candidateView.gebdat match {
                                   case Some(cd) => f"${cd}%tF".endsWith("-01-01")
                                   case _        => true
                                 }
                               case _ => false
                               })) {
                    val athlet = service.insertAthlete(Athlet(
                        id = candidateView.id,
                        js_id = candidateView.js_id,
                        geschlecht = candidateView.geschlecht,
                        name = candidateView.name,
                        vorname = candidateView.vorname,
                        gebdat = importathlet.gebdat,
                        strasse = candidateView.strasse,
                        plz = candidateView.plz,
                        ort = candidateView.ort,
                        verein = Some(cbVereine.selectionModel.value.selectedItem.value.id),
                        activ = true
                        ))
                    athlet.id
                  }
                  else if(candidateView.id > 0) {
                    candidateView.id
                  }
                  else {
                    val athlet = service.insertAthlete(Athlet(
                        id = 0,
                        js_id = candidateView.js_id,
                        geschlecht = candidateView.geschlecht,
                        name = candidateView.name,
                        vorname = candidateView.vorname,
                        gebdat = candidateView.gebdat,
                        strasse = candidateView.strasse,
                        plz = candidateView.plz,
                        ort = candidateView.ort,
                        verein = Some(cbVereine.selectionModel.value.selectedItem.value.id),
                        activ = true
                        ))
                    athlet.id
                  }
                  (progrId, id)
                }

                for((progId, athletes) <- selectedAthleten.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))) {
                  def filter(progId: Long, a: Athlet): Boolean = athletes.exists { _ == a.id }
                  service.assignAthletsToWettkampf(wettkampf.id, Set(progId), Some(filter))
                }
                reloadData()
              }
            }
          }, new Button("OK Alle") {
            disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
            onAction = (event: ActionEvent) => {
              val clip = filteredModel.map { raw =>
                  val (progId, importAthlet, candidateView) = raw
                  val athlet = if(candidateView.id > 0 &&
                             (importAthlet.gebdat match {
                               case Some(d) =>
                                 candidateView.gebdat match {
                                   case Some(cd) => f"${cd}%tF".endsWith("-01-01")
                                   case _        => true
                                 }
                               case _ => false
                               })) {
                    val athlet = service.insertAthlete(Athlet(
                        id = candidateView.id,
                        js_id = candidateView.js_id,
                        geschlecht = candidateView.geschlecht,
                        name = candidateView.name,
                        vorname = candidateView.vorname,
                        gebdat = importAthlet.gebdat,
                        strasse = candidateView.strasse,
                        plz = candidateView.plz,
                        ort = candidateView.ort,
                        verein = Some(cbVereine.selectionModel.value.selectedItem.value.id),
                        activ = true
                        ))
                    AthletView(athlet.id, athlet.js_id, athlet.geschlecht, athlet.name, athlet.vorname, athlet.gebdat, athlet.strasse, athlet.plz, athlet.ort, Some(cbVereine.selectionModel.value.selectedItem.value), true)
                  }
                  else if(candidateView.id > 0) {
                    candidateView
                  }
                  else {
                    val candidate =  Athlet(
                        id = candidateView.id,
                        js_id = candidateView.js_id,
                        geschlecht = candidateView.geschlecht,
                        name = candidateView.name,
                        vorname = candidateView.vorname,
                        gebdat = candidateView.gebdat,
                        strasse = candidateView.strasse,
                        plz = candidateView.plz,
                        ort = candidateView.ort,
                        verein = Some(cbVereine.selectionModel.value.selectedItem.value.id),
                        activ = true
                        )
                    val athlet = service.insertAthlete(candidate)
                    AthletView(athlet.id, athlet.js_id, athlet.geschlecht, athlet.name, athlet.vorname, athlet.gebdat, athlet.strasse, athlet.plz, athlet.ort, Some(cbVereine.selectionModel.value.selectedItem.value), true)
                  }
                 (progId, athlet)
              }.toList
              if (!athletModel.isEmpty) {
                for((progId, athletes) <- clip.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))) {
                  def filter(progId: Long, a: Athlet): Boolean = athletes.exists { x => x.id == a.id }
                  service.assignAthletsToWettkampf(wettkampf.id, Set(progId), Some(filter))
                }
                reloadData()
              }
            }
          })
        }
        }
      }
    }
    val generateTeilnehmerListe = new Button with KategorieTeilnehmerToHtmlRenderer {
      text = "Teilnehmerliste erstellen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (wkModel.nonEmpty) {
          val driver = wkModel.toSeq
          val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()){(acc, pgm) =>
            if(!acc.exists { x => x.id == pgm.id }) {
              acc :+ pgm
            }
            else {
              acc
            }
          }
          println(programme)
          val riegen = service.selectRiegen(wettkampf.id).map(r => r.r -> (r.start.map(_.name).getOrElse(""), r.durchgang.getOrElse(""))).toMap
          val seriendaten = for {
            programm <- programme

            athletwertungen <- driver.map(we => we.filter { x => x.init.wettkampfdisziplin.programm.id == programm.id})
            if(athletwertungen.nonEmpty)
          }
          yield {
            val einsatz = athletwertungen.head.init
            val athlet = einsatz.athlet
            Kandidat(
            einsatz.wettkampf.easyprint
            ,athlet.geschlecht match {case "M" => "Turner"  case _ => "Turnerin"}
            ,einsatz.wettkampfdisziplin.programm.easyprint
            ,athlet.name
            ,athlet.vorname
            ,AthletJahrgang(athlet.gebdat).hg
            ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
            ,einsatz.riege.getOrElse("") + " " + riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._2
            ,riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._1
            ,athletwertungen.filter{wertung =>
              if(wertung.init.wettkampfdisziplin.feminim == 0 && !wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else if(wertung.init.wettkampfdisziplin.masculin == 0 && wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else {
                true
              }
            }.map(_.init.wettkampfdisziplin.disziplin.easyprint)
            )
          }
          val filename = "Teilnehmerliste_" + wettkampf.easyprint.replace(" ", "_") + programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") + riege.map("_Riege_" + _.replace(" ", "_")).getOrElse("") + ".html"
          val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs();
          }
          val file = new java.io.File(dir.getPath + "/" + filename)
          val logofile = if(new java.io.File(dir.getPath + "/logo.jpg").exists()) {
            "logo.jpg"
          }
          else {
            "../logo.jpg"
          }
          val toSave = toHTMLasKategorienListe(seriendaten, logofile)
          val os = new BufferedOutputStream(new FileOutputStream(file))
          os.write(toSave.getBytes("UTF-8"))
          os.flush()
          os.close()
          Desktop.getDesktop().open(file);
          //printHtml(toSave, webView.engine)
        }
      }
    }
    val generateNotenblaetter = new Button with NotenblattToHtmlRenderer {
      text = "Notenblätter erstellen"
      minWidth = 75

      onAction = (event: ActionEvent) => {
        if (wkModel.nonEmpty) {
          val driver = wkModel.toSeq
          val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()){(acc, pgm) =>
            if(!acc.exists { x => x.id == pgm.id }) {
              acc :+ pgm
            }
            else {
              acc
            }
          }
          println(programme)
          val seriendaten = for {
            programm <- programme

            athletwertungen <- driver.map(we => we.filter { x => x.init.wettkampfdisziplin.programm.id == programm.id})
            if(athletwertungen.nonEmpty)
          }
          yield {
            val einsatz = athletwertungen.head.init
            val athlet = einsatz.athlet
            Kandidat(
            einsatz.wettkampf.easyprint
            ,athlet.geschlecht match {case "M" => "Turner"  case _ => "Turnerin"}
            ,einsatz.wettkampfdisziplin.programm.easyprint
            //,einsatz.riege
            ,athlet.name
            ,athlet.vorname
            ,AthletJahrgang(athlet.gebdat).hg
            ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
            ,athletwertungen.filter{wertung =>
              if(wertung.init.wettkampfdisziplin.feminim == 0 && !wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else if(wertung.init.wettkampfdisziplin.masculin == 0 && wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else {
                true
              }
            }.map(_.init.wettkampfdisziplin.disziplin.easyprint)
            )
          }
          val filename = "Notenblatt_" + wettkampf.easyprint.replace(" ", "_") + programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") + riege.map("_Riege_" + _.replace(" ", "_")).getOrElse("") + ".html"
          val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs();
          }
          val file = new java.io.File(dir.getPath + "/" + filename)
          val logofile = if(new java.io.File(dir.getPath + "/logo.jpg").exists()) {
            "logo.jpg"
          }
          else {
            "../logo.jpg"
          }
          val toSave = wettkampf.programm.head.id match {
            case 20 => toHTMLasGeTu(seriendaten, logofile)
            case n if(n == 11 || n == 31) => toHTMLasKuTu(seriendaten, logofile)
            case _ => toHTMLasATT(seriendaten, logofile)
          }
          val os = new BufferedOutputStream(new FileOutputStream(file))
          os.write(toSave.getBytes("UTF-8"))
          os.flush()
          os.close()
          Desktop.getDesktop().open(file);
          //printHtml(toSave, webView.engine)
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
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = Seq(new Label("Stationen (wenn mehr wie eine Rotation, dann pro Rotation, getrennt mit Komma)  "), stationen)
              // Rotation mit [], Kategorie-Gruppe mit Kx(), Geräte-Gruppe mit G<Gerätnummer>k1(4)
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            if (!stationen.text.value.isEmpty) {
              KuTuApp.invokeWithBusyIndicator {
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
          }
        })
      }
    }
    val einheitenExportButton = new Button {
      text = "Riegen Einheiten export"
      minWidth = 75
      val stationen = new TextField()
      onAction = (event: ActionEvent) => {
        implicit val impevent = event
        KuTuApp.invokeWithBusyIndicator {
          val filename = "Einheiten.csv"
          val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs();
          }
          val file = new java.io.File(dir.getPath + "/" + filename)

          ResourceExchanger.exportEinheiten(wkModel.head.init.head.init.wettkampf, file.getPath)
          Desktop.getDesktop().open(file);
        }
      }
    }
    val riegenRemoveButton = new Button {
      text = "Riege löschen"
      minWidth = 75
      disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
      onAction = (event: ActionEvent) => {
        KuTuApp.invokeWithBusyIndicator {
          val selectedRiege = riegenFilterView.selectionModel.value.getSelectedItem.name.value
          for{
            wl <- reloadWertungen{wertung =>
                wertung.riege match {case Some(r) => r.equals(selectedRiege) case _ => false}
              }
            w <- wl
          } {
            service.updateWertung(w.commit.copy(riege = None))
          }
          for{
            wl <- reloadWertungen{wertung =>
                wertung.riege2 match {case Some(r) => r.equals(selectedRiege) case _ => false}
              }
            w <- wl
          } {
            service.updateWertung(w.commit.copy(riege2 = None))
          }
          refreshLazyPane()
          reloadData()
        }
      }
    }
    val riegeRenameButton = new Button {
      text = "Riege umbenennen"
      minWidth = 75
      disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
      onAction = (event: ActionEvent) => {
        implicit val impevent = event
        val selectedRiege = riegenFilterView.selectionModel.value.getSelectedItem.name.value
        val txtRiegenName = new TextField {
          text.value = selectedRiege
        }
        PageDisplayer.showInDialog(text.value, new DisplayablePage() {
          def getPage: Node = {
            new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = Seq(new Label("Neuer Riegenname  "), txtRiegenName)
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            KuTuApp.invokeWithBusyIndicator {
              service.renameRiege(wettkampf.id, selectedRiege, txtRiegenName.text.value)
              refreshLazyPane()
              reloadData()
            }
          }
        })
      }
    }

    val actionButtons = programm match {
      case None =>
      List[Button](generateNotenblaetter, riegeRenameButton, riegenRemoveButton)
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
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.MULTIPLE)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList;
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for{athlet <- athletModel
              } {
                val matches = searchQuery.forall{search =>
                  if(search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if(athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if(athlet.verein match {case Some(v) => v.name.toUpperCase().contains(search) case None => false}) {
                    true
                  }
                  else {
                    false
                  }
                }

                if(matches) {
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
                val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
                  x => athletTable.selectionModel.value.isSelected(x._2)
                }.map(x => x._1.id)

                def filter(progId: Long, a: Athlet): Boolean = selectedAthleten.contains(a.id)
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
      val pasteFromExcel = new Button("Aus Excel einfügen ...") {
        onAction = (event: ActionEvent) => {
          doPasteFromExcel(Some(progrm))(event)
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

      //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      val moveAvaillable = programm.forall { p => p.head != 1 }
      moveToOtherProgramButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose moveAvaillable otherwise false
      removeButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

      List(addButton, pasteFromExcel, moveToOtherProgramButton, generateTeilnehmerListe, generateNotenblaetter, removeButton).filter(btn => !btn.text.value.equals("."))
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
    clearButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

    wkview.selectionModel.value.setCellSelectionEnabled(true)
    wkview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(wkview, true)(ke)
    }

    riegenFilterView.selectionModel.value.setCellSelectionEnabled(true)
    riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(riegenFilterView, false)(ke)
    }

    onSelectionChanged = handle {
      if(selected.value) {
        reloadData()
      }
    }

    val editTablePane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      top = editorPane
      center = new StackPane {
        children += webView
        children += wkview
      }
    }

    val riegenFilterControl = new ToolBar {
      content = List(
          riegensuggestButton
        , einheitenExportButton
        , riegeRenameButton
        , riegenRemoveButton
      )
    }

    val riegenHeader = new VBox {
      maxWidth = Double.MaxValue
      minHeight = Region.USE_PREF_SIZE
      val title = new Label {
        text = "Riegen-Einteilung"
        styleClass += "toolbar-header"
      }
      children += title
      children += riegenFilterControl
      children += alleRiegenCheckBox
    }

    val riegenFilterPane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      prefWidth = 500
      margin = Insets(0, 0, 0, 10)
      top = riegenHeader
      center = new BorderPane {
        center = riegenFilterView
      }
    }

    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
//      center = new SplitPane {
//        orientation = Orientation.HORIZONTAL
//        items += editTablePane
//        items += riegenFilterPane
//        //setDividerPosition(0, 0.7d)
//        SplitPane.setResizableWithParent(riegenFilterPane, false)
//      }
      center = new BorderPane {
        center = editTablePane
        left = riegenFilterPane
      }
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
        ) ++ actionButtons :+ clearButton :+ txtUserFilter
      }
    }

    content = cont

    true
  }
}
