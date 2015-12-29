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
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.awt.Desktop
import javafx.scene.{control => jfxsc}
import scala.IndexedSeq
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx

import ch.seidel.commons._
import ch.seidel.kutu.renderer.NotenblattToHtmlRenderer
import ch.seidel.kutu.domain._

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
          }
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
      else {
        lblAthlet.text.value = ""
        lblDisciplin.text.value = ""
      }
    }
  }

  override def isPopulated = {

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

    }

    val editorPane: EditorPane = new EditorPane(wkview)
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
        styleClass += "table-cell-with-value"
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
      },
      new WKTableColumn[String](-1) {
        text = "Riege 2"
        styleClass += "table-cell-with-value"
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
          refreshLazyPane()
          updateEditorPane
          evt.tableView.requestFocus()
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
          val matches = athlet.nonEmpty && searchQuery.forall{search =>
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

    def reloadData() = {
      val selectionstore = wkview.selectionModel.value.getSelectedCells
      val coords = for(ts <- selectionstore) yield {
        if(ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
          //(ts.getRow, wkview.columns.indexOf(ts.getTableColumn.getParentColumn))
          (ts.getRow, (3 + editorPane.index) * -100)
        else
          (ts.getRow, ts.getColumn)
      }

      val columnrebuild = wertungen.isEmpty
      wkModel.clear()
      wertungen = updateWertungen
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

    def doPasteFromExcel(progrm: Option[ProgrammView])(implicit event: ActionEvent) {
      val athletModel = ObservableBuffer[(Long, Athlet, AthletView)]()
      val vereineList = service.selectVereine
      val vereineMap = vereineList.map(v => v.id -> v).toMap
      val vereine = ObservableBuffer[Verein](vereineList)
      val cbVereine = new ComboBox[Verein] {
        items = vereine
        //selectionModel.value.selectFirst()
      }
      val sdfYYYY = new SimpleDateFormat("YYYY")
      val sdfYY = new SimpleDateFormat("YY")
      val programms = programm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
      val clipraw = Source.fromString(Clipboard.systemClipboard.getString).getLines().
                     map    { line   => line.split("\\t") }.
                     filter { fields => fields.length > 2 }.
                     map    { fields =>
                        val parsed = Athlet(
                            id = 0,
                            js_id = "",
                            geschlecht = if(!"".equals(fields(4))) "W" else "M",
                            name = fields(0),
                            vorname = fields(1),
                            gebdat = if(fields(2).length > 4) Some(service.getSQLDate(fields(2))) else None,
                            strasse = "",
                            plz = "",
                            ort = "",
                            verein = None,
                            activ = true
                            )
                        val candidate = service.findAthleteLike(parsed)
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
                s"${x.value._2.name} ${x.value._2.vorname}, ${x.value._2.gebdat.map(sdfYYYY.format(_)) match {case None => "" case Some(t) => t}}"
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
              val id = if(candidateView.id > 0) {
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
              val athlet = if(candidateView.id > 0) {
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
    val generateNotenblaetter = new Button with NotenblattToHtmlRenderer {
      text = "Notenblätter erstellen"
      minWidth = 75

      onAction = (event: ActionEvent) => {
        if (wkModel.nonEmpty) {
          val driver = wkModel.toSeq
          val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()){(acc, pgm) =>
              if(!acc.exists { x => x.id == pgm.id }) {
                println(pgm)
                acc :+ pgm } else { acc }
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
          val file = new java.io.File(service.homedir + "/" + filename)
          val toSave = wettkampf.programm.head.id match {
            case 20 => toHTMLasGeTu(seriendaten).getBytes("UTF-8")
            case n if(n == 11 || n == 31) => toHTMLasKuTu(seriendaten).getBytes("UTF-8")
            case _ => toHTMLasATT(seriendaten).getBytes("UTF-8")
          }
          val os = new BufferedOutputStream(new FileOutputStream(file))
          os.write(toSave)
          os.flush()
          os.close()
          Desktop.getDesktop().open(file);
        }
      }
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
            service.updateWertung(w.commit.copy(riege = None))
          }
          refreshLazyPane()
          reloadData()
        }
      }
      val riegeRenameButton = new Button {
        text = "Riege umbenennen"
        minWidth = 75
        onAction = (event: ActionEvent) => {
          implicit val impevent = event
          val txtRiegenName = new TextField {
            text.value = riege.getOrElse("")
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
              for{
                wl <- wertungen
                if(wl.head.init.riege.equals(riege))
                w <- wl
              } {
                service.updateWertung(w.commit.copy(riege = Some(txtRiegenName.text.value)))
              }
              refreshLazyPane()
              reloadData()
            }
          })
        }
      }
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

      List(addButton, pasteFromExcel, moveToOtherProgramButton, generateNotenblaetter, removeButton, riegensuggestButton).filter(btn => !btn.text.value.equals("."))
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

    wkview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      ke.code match {
        case KeyCode.TAB if(!ke.controlDown) =>
          val action = new Runnable() {
            override def run = {
              if(ke.shiftDown)
                wkview.selectionModel.value.selectPrevious()
              else
                wkview.selectionModel.value.selectNext()
            }
          }
          val wasEditing = wkview.delegate.getEditingCell() != null
          action.run()
          if(wasEditing) {
            ke.consume()
            Platform.runLater(action)
          }
          else {
            ke.consume()
          }

        case KeyCode.ENTER /*if(ke.controlDown || wkview.delegate.getEditingCell() != null)*/ =>
          val action = new Runnable() {
            override def run = {
              if(ke.shiftDown) {
                val index = wkview.selectionModel.value.getSelectedIndex
                if(index == 0) {
                  wkview.selectionModel.value.selectLast()
                }
                else {
                  wkview.selectionModel.value.selectAboveCell()
                }
              }
              else {
                val index = wkview.selectionModel.value.getSelectedIndex
                if(index == wkview.items.value.size()-1) {
                  wkview.selectionModel.value.selectFirst()
                }
                else {
                  wkview.selectionModel.value.selectBelowCell()
                }
              }
            }
          }
          val wasEditing = wkview.delegate.getEditingCell() != null
          action.run()
          if(wasEditing) {
            ke.consume()
            Platform.runLater(action)
          }
          else {
            ke.consume()
          }

        case KeyCode.DELETE if(wkview.delegate.getEditingCell() == null) =>
          val fc = wkview.focusModel.value.focusedCell.value
          val tc = fc.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], Any]]

          wkview.edit(fc.row, tc)

          val textKey = if(tc.parentColumn.value != null) tc.parentColumn.value.getText else tc.getText
          wkview.items.get.get(fc.row).find {
            x => x.init.wettkampfdisziplin.disziplin.name.equals(textKey)
          } match {
            case Some(editor) =>
              editingEditor = Some(editor)
              tc.text.value match {
                case "D" => editor.noteD.value = 0d
                case "E" => editor.noteE.value = 0d
                case s if(s.equals(textKey)) => editor.noteE.value = 0d
                case _ =>
              }

            case None =>
          }
          ke.consume()

        // Paste via CTRL+V or SHIFT+INSERT
        case c if(ke.shiftDown && c == KeyCode.INSERT) || (ke.controlDown && ke.text.equals("v")) =>
          doPasteFromExcel(programm)(new ActionEvent())
          ke.consume()

        case c if((c.isLetterKey || c.isDigitKey) && wkview.editingCell.value == null) =>
          val fc = wkview.focusModel.value.focusedCell.value
          wkview.edit(fc.row, fc.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], Any]])

        case _ =>
      }
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
        ) ++ actionButtons :+ clearButton :+ txtUserFilter
      }
    }

    content = cont

    true
  }
}
