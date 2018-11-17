package ch.seidel.kutu.view

import java.util.UUID
import java.util.concurrent.{ScheduledFuture, TimeUnit}

import ch.seidel.commons._
import ch.seidel.kutu.Config._
import ch.seidel.kutu.{KuTuApp, KuTuServer}
import ch.seidel.kutu.akka.AthletWertungUpdated
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer._
import javafx.scene.{control => jfxsc}
import org.slf4j.LoggerFactory
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.beans.property.{DoubleProperty, ReadOnlyStringWrapper, _}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.geometry._
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{Clipboard, KeyEvent}
import scalafx.scene.layout._
import scalafx.util.converter.{DefaultStringConverter, DoubleStringConverter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{Failure, Success}

trait TCAccess[R, E, IDX] {
  def getIndex: IDX
  def valueEditor(selectedRow: R): E
}

trait WKTCAccess extends TCAccess[IndexedSeq[WertungEditor], WertungEditor, Int] {
}

class WKJFSCTableColumn[T](val index: Int) extends jfxsc.TableColumn[IndexedSeq[WertungEditor], T] with WKTCAccess {
  override def getIndex: Int = index
  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
}

class WKTableColumn[T](val index: Int) extends TableColumn[IndexedSeq[WertungEditor], T] with WKTCAccess {
  override val delegate: jfxsc.TableColumn[IndexedSeq[WertungEditor], T] = new WKJFSCTableColumn[T](index)
  override def getIndex: Int = index
  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
}

class WettkampfWertungTab(wettkampfmode: BooleanProperty, programm: Option[ProgrammView], riege: Option[String], wettkampf: WettkampfView, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
  val logger = LoggerFactory.getLogger(this.getClass)

  import language.implicitConversions
  implicit def doublePropertyToObservableValue(p: DoubleProperty): ObservableValue[Double,Double] = p.asInstanceOf[ObservableValue[Double,Double]]
  private var lazypane: Option[LazyTabPane] = None
  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
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
          if(column.isInstanceOf[WKTCAccess]) {
            val selectedIndex = column.asInstanceOf[WKTCAccess].getIndex
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

  var subscription: Option[Subscription] = None
  var websocketsubscription: Option[Subscription] = None
  override def release {
    websocketsubscription.foreach(_.cancel)
    websocketsubscription = None
    subscription match {
      case Some(s) => 
        s.cancel
        subscription = None
      case None =>
    }
  }
  
  override def isPopulated = {
    subscription match {
      case None =>
        subscription = Some(wettkampfmode.onChange {
          def op = {
            val pop = isPopulated
            refreshLazyPane()
          }
          KuTuApp.invokeWithBusyIndicator(op)
        })
//        logger.debug("was not populated and subscription new accquired "+ programm+ wettkampf + hashCode())
      case _ => 
//        logger.debug("was populated and subscription active "+ programm+ wettkampf + hashCode())
    }
      
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

    var wertungen = reloadWertungen()
 		val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)
    val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
      id = "kutu-table"
      editable = true
    }
    val emptyRiege = GeraeteRiege("", None, 0, None, Seq(), false)
    var riegendurchgaenge: List[GeraeteRiege] = List[GeraeteRiege]()
 		var relevantRiegen: Map[String,(Boolean, Int)] = Map[String,(Boolean, Int)]()
 		var erfasst = false
 		
  	val cmbDurchgangFilter = new ComboBox[GeraeteRiege]() {
      id = "cmbDurchgaenge"
      var okIcon: Image = null
      try {
        okIcon = new Image(getClass().getResourceAsStream("/images/GreenOk.png"))
      }catch{case e: Exception => e.printStackTrace()}
      var nokIcon: Image = null
      try {
        nokIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
      }catch{case e: Exception => e.printStackTrace()}
      
      class GeraeteRiegeListCell extends ListCell[GeraeteRiege] {
        override val delegate: jfxsc.ListCell[GeraeteRiege] = new jfxsc.ListCell[GeraeteRiege] {
          override protected def updateItem(item: GeraeteRiege, empty: Boolean) {
            super.updateItem(item, empty)
            if (item != null) {
            	val imageView = new ImageView {
                image = okIcon
            	}
              item.durchgang match {
                case Some(d) =>
                  setText(s"${item.durchgang.get}: ${item.disziplin.map(d => d.name).getOrElse("")}  (${item.halt + 1}. Gerät)");
                  if(!item.erfasst) {
                    styleClass.add("incomplete")
                    imageView.image = nokIcon
                  }
                  else if (styleClass.indexOf("incomplete") > -1) {
                    styleClass.remove(styleClass.indexOf("incomplete"))
                  }
                case None =>
                  setText(s"Alle")
                  if(!erfasst) {
                    styleClass.add("incomplete")
                    imageView.image = nokIcon
                  }
                  else if (styleClass.indexOf("incomplete") > -1) {
                    styleClass.remove(styleClass.indexOf("incomplete"))
                  }
              }
              graphic = imageView
            }
            else {
              graphic = null
            }
          }
        }
      }
      promptText = "Durchgang-Filter"
      buttonCell = new GeraeteRiegeListCell()
      cellFactory = { p => new GeraeteRiegeListCell() }

      focused.onChange({
        if(!focused.value) {
          val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(wkview)
          Platform.runLater(new Runnable() {
            override def run = {
              focusSetter()
            }
          })
        }
      })
    }

    def rebuildDurchgangFilterList = {
      val kandidaten = service.getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get))
      val ret = RiegenBuilder.mapToGeraeteRiegen(kandidaten).filter(r => wertungen.exists { p => r.kandidaten.exists { k => p.head.init.athlet.id == k.id } })
      erfasst = ret.forall { riege => riege == emptyRiege || riege.erfasst }
      ret
    }

    def computeRelevantRiegen = {
      relevantRiegen = (if(wertungen.size > 0) wertungen.
          flatMap(x => x.flatMap(x => Seq(x.init.riege, x.init.riege2).flatten).toSet).
          groupBy(x => x).map(x => (x._1, x._2.size)).toSet else Set.empty[(String,Int)]).
          map(x => x._1 -> (relevantRiegen.getOrElse(x._1, (true, x._2))._1, x._2)).toMap

//      riegendurchgaenge = service.selectRiegen(wettkampf.id).filter(p => relevantRiegen.contains(p.r)).map(r => r.r -> r.durchgang.getOrElse("")).toMap
       val lastDurchgangSelection = cmbDurchgangFilter.selectionModel.value.getSelectedItem
      cmbDurchgangFilter.items = ObservableBuffer[GeraeteRiege](emptyRiege  +: rebuildDurchgangFilterList)
      if (cmbDurchgangFilter.items.value.contains(lastDurchgangSelection)) {
        cmbDurchgangFilter.selectionModel.value.select(lastDurchgangSelection)
      }
      //riegendurchgaenge.map(_._2).toSet.toList.sorted)
      relevantRiegen
    }
    computeRelevantRiegen

    def riegen(onSelectedChange: (String, Boolean) => Boolean, initial: Boolean): IndexedSeq[RiegeEditor] = {
      service.listRiegenZuWettkampf(wettkampf.id).sortBy(r => r._1).filter{r => relevantRiegen.contains(r._1)}.map(x =>
        RiegeEditor(
            wettkampf.id,
            x._1,
            x._2,
            if(relevantRiegen.contains(x._1)) relevantRiegen(x._1)._2 else 0,
            relevantRiegen.contains(x._1) && (initial || relevantRiegen(x._1)._1),
            x._3,
            x._4,
            Some(onSelectedChange)))
    }
    val riegenFilterModel = ObservableBuffer[RiegeEditor]()

    val editorPane: EditorPane = new EditorPane(wkview)
    val disziplinlist = wertungen.headOption match {case Some(w) => w.map(_.init.wettkampfdisziplin.disziplin) case _ => IndexedSeq[Disziplin]()}
    def disziplinCnt = wertungen.headOption match {case Some(w) => w.size case _ => 0}
    val withDNotes = wertungen.flatMap(w => w.filter(ww => ww.init.wettkampfdisziplin.notenSpez.isDNoteUsed)).nonEmpty
    val withENotes = wettkampf.programm.id != 1
    var lastFilter = ""
    var durchgangFilter = emptyRiege

    var lazyEditorPaneUpdater: Map[String, ScheduledFuture[_]] = Map.empty
   
    def submitLazy(name: String, task: ()=>Unit, delay: Long) {
      println("submitting " + name)
      lazyEditorPaneUpdater.get(name).foreach(_.cancel(true))
      val ft = KuTuApp.lazyExecutor.schedule(new Runnable() { def run = { 
        Platform.runLater{task()}
      }}, delay, TimeUnit.SECONDS)
      
      lazyEditorPaneUpdater = lazyEditorPaneUpdater + (name -> ft)
    }
    
    def updateEditorPane(focusHolder: Option[Node] = None) {
      submitLazy("updateEditorPane", () => {
        if(selected.value) {
          println("updating EditorPane ")
    		  editorPane.adjust
          val model = cmbDurchgangFilter.items.getValue
  		    val raw = emptyRiege +: rebuildDurchgangFilterList
  		    val selected = cmbDurchgangFilter.selectionModel.value.selectedItem.value
  		    model.foreach { x => 
  		      raw.find {_.softEquals(x)} match {
  		        case Some(item) => 
  		          val reselect = selected == x
  		          model.set(model.indexOf(x), item)
  		          if(reselect) {
  		            durchgangFilter = item
  		            cmbDurchgangFilter.selectionModel.value.select(item)
  		          }
  		        case None =>
  		      }
  		    }
      	  val toRemove =
      	    for{
      	      o <- model 
      	      i = raw.find { _ == o}
      	      if(i.isEmpty && o != emptyRiege)
      	    }
      	    yield {model.indexOf(o)}
      	  for{i <- toRemove.sorted.reverse} {
      	    model.remove(i)
      	  }
  
      	  for{
      	    o <- raw 
            i = model.find { _ == o}
      	    if(i.isEmpty)
      	  }{
      	    model.insert(raw.indexWhere { rx => rx.softEquals(o)}, o)
      	  }
      	  focusHolder.foreach(_.requestFocus())
        }        
      }, 5)
    }

    val indexerE = Iterator.from(0)
    val indexerD = Iterator.from(0)
    val indexerF = Iterator.from(0)

    case class DoubleConverter(notenModus: NotenModus) extends DoubleStringConverter {
      override def toString(value: Double) = notenModus.toString(value)
    }
    
    def wertungenCols = if (wertungen.nonEmpty) {
      wertungen.head.map { wertung =>
        lazy val clDnote = new WKTableColumn[Double](indexerD.next) {
          text = "D"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteD else wertung.noteD }
          cellFactory = { _ => new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez))}

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
              service.updateWertung(disciplin.commit)
            }
            evt.tableView.requestFocus()
          }
          onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
          }
        }
        lazy val clEnote = new WKTableColumn[Double](indexerE.next) {
          text = "E"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteE else wertung.noteE }
          cellFactory = { x => new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez)) }

          styleClass += "table-cell-with-value"
          prefWidth = 60
          editable = true
          //logger.debug(text, index)

          onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            val disciplin = evt.rowValue(index)
            disciplin.noteE.value = evt.newValue
            disciplin.endnote.value = wertung.init.wettkampfdisziplin.notenSpez.calcEndnote(disciplin.noteD.value, disciplin.noteE.value)
            val rowIndex = wkModel.indexOf(evt.rowValue)
            if (disciplin.isDirty) {
              service.updateWertung(disciplin.commit)
            }
            evt.tableView.requestFocus()
          }
          onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
          }
        }
        lazy val clEndnote = new WKTableColumn[Double](indexerF.next) {
          text = "Endnote"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).endnote else wertung.endnote }
          styleClass += "table-cell-with-value"
          prefWidth = 80
          editable = false
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
            s"${a.vorname} ${a.name} ${(a.gebdat match {case Some(d) => f"$d%tY " case _ => " "}) }"
          })
        }
//        delegate.impl_setReorderable(false) // shame on me??? why this feature should not be a requirement?
        prefWidth = 150
        editable = false
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
        editable = false
      }
    )
    val leafprograms = wertungen.flatMap(a => a.map(w => w.init.wettkampfdisziplin.programm)).filter(p => p.aggregate != 0).toSet.toList.sortWith((a, b) => a.ord > b.ord)
    
    val riegeCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = if(leafprograms.size < 2) {
      List(new WKTableColumn[String](-1) {
          text = "Riege"
          if(!wettkampfmode.value) {
            cellFactory = { x =>
              new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
            }
          }
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "riege", {
              s"${x.value.head.init.riege.getOrElse("keine Einteilung")}"
            })
          }
  //        delegate.impl_setReorderable(false)
          editable = !wettkampfmode.value
          visible = !wettkampfmode.value
          prefWidth = 100
          if(!wettkampfmode.value) {
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val rowIndex = wkModel.indexOf(evt.rowValue)
              val newRiege = if(evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None 
            	        else Some(evt.newValue)
            	logger.debug("start riege-rename")
            	service.updateAllWertungenAsync(
            	    evt.rowValue.map(wertung => 
            	      wertung.commit.copy(riege = newRiege))).andThen {
                case Success(ws) => logger.debug("saved riege-rename")
                  KuTuApp.invokeWithBusyIndicator{
                    val selected = wkview.selectionModel.value.selectedCells
                    refreshOtherLazyPanes()
                    wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                    selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                    updateEditorPane(Some(evt.tableView))
                    logger.debug("finished riege-rename")
                  }
                case Failure(e) => logger.error("not saved", e)
              }
              
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
            onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
    //          logger.debug(evt)
            }
          }
        },
        new WKTableColumn[String](-1) {
          text = "Riege 2"
          if(!wettkampfmode.value) {
            cellFactory = { x =>
              new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
            }
          }
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "riege2", {
              s"${x.value.head.init.riege2.getOrElse("keine Einteilung")}"
            })
          }
  //        delegate.impl_setReorderable(false)
          editable = !wettkampfmode.value
          visible = !wettkampfmode.value
          prefWidth = 100
          if(!wettkampfmode.value) {
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val rowIndex = wkModel.indexOf(evt.rowValue)
              val newRiege = if(evt.newValue.trim.isEmpty || evt.newValue.equals("keine Einteilung")) None
            	        else Some(evt.newValue)
            	logger.debug("start riege-rename")
            	service.updateAllWertungenAsync(
            	    evt.rowValue.map(wertung => 
            	      wertung.commit.copy(riege2 = newRiege))).andThen {
                case Success(ws) => logger.debug("saved riege-rename")
                  KuTuApp.invokeWithBusyIndicator{
                    val selected = wkview.selectionModel.value.selectedCells
                    refreshOtherLazyPanes()
                    wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                    selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                    updateEditorPane(Some(evt.tableView))
                    logger.debug("finished riege-rename")
                  }
                case Failure(e) => logger.error("not saved", e)
              }
              
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
            onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
    //          logger.debug(evt)
            }
          }
        })
    } else { 
      val cols: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = leafprograms.map{p => 
        val col: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] = new TableColumn[IndexedSeq[WertungEditor], String] {
          text = s"${p.name}"
//            delegate.impl_setReorderable(false)
          columns ++= Seq(
            new WKTableColumn[String](-1) {
              text = "Riege"
              if(!wettkampfmode.value) {
                cellFactory = { x =>
                  new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
                }
              }
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "riege", {
                  s"${x.value.find(we => we.init.wettkampfdisziplin.programm == p).flatMap(we => we.init.riege).getOrElse("keine Einteilung")}"
                })
              }
      //        delegate.impl_setReorderable(false)
              editable = !wettkampfmode.value
              visible = !wettkampfmode.value
              prefWidth = 100
              if(!wettkampfmode.value) {
                onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
                  val rowIndex = wkModel.indexOf(evt.rowValue)
                  val newRiege = if(evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None 
                	        else Some(evt.newValue)
                	logger.debug("start riege-rename")
                	service.updateAllWertungenAsync(
                	    evt.rowValue.map(wertung => 
                	      wertung.commit.copy(riege = newRiege))).andThen {
                    case Success(ws) => logger.debug("saved riege-rename")
                      KuTuApp.invokeWithBusyIndicator{
                        val selected = wkview.selectionModel.value.selectedCells
                        refreshOtherLazyPanes()
                        wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                        selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                        updateEditorPane(Some(evt.tableView))
                        logger.debug("finished riege-rename")
                      }
                    case Failure(e) => logger.error("not saved", e)
                  }
                  
                  evt.tableView.selectionModel.value.select(rowIndex, this)
                  evt.tableView.requestFocus()
                }
                onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
        //          logger.debug(evt)
                }
              }
            },
            new WKTableColumn[String](-1) {
              text = "Riege 2"
              if(!wettkampfmode.value) {
                cellFactory = { x =>
                  new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
                }
              }
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "riege2", {
                  s"${x.value.find(we => we.init.wettkampfdisziplin.programm == p).flatMap(we => we.init.riege2).getOrElse("keine Einteilung")}"
                })
              }
      //        delegate.impl_setReorderable(false)
              editable = !wettkampfmode.value
              visible = !wettkampfmode.value
              prefWidth = 100
              if(!wettkampfmode.value) {
                onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
                  val rowIndex = wkModel.indexOf(evt.rowValue)
                  val newRiege = if(evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None 
                	        else Some(evt.newValue)
                	logger.debug("start riege-rename")
                	service.updateAllWertungenAsync(
                	    evt.rowValue.map(wertung => 
                	      wertung.commit.copy(riege2 = newRiege))).andThen {
                    case Success(ws) => logger.debug("saved riege-rename")
                      KuTuApp.invokeWithBusyIndicator{
                        val selected = wkview.selectionModel.value.selectedCells
                        refreshOtherLazyPanes()
                        wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                        selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                        updateEditorPane(Some(evt.tableView))
                        logger.debug("finished riege-rename")
                      }
                    case Failure(e) => logger.error("not saved", e)
                  }
                  
                  evt.tableView.selectionModel.value.select(rowIndex, this)
                  evt.tableView.requestFocus()
                }
                onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
        //          logger.debug(evt)
                }
              }
            }
           )
          }
        col
      }.toList
      cols
    }
      
    val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
      new WKTableColumn[String](-1) {
        text = "Punkte"
        cellValueFactory = { x => new ReadOnlyStringWrapper(x.value, "punkte", { f"${x.value.map(w => w.endnote.value.toDouble).sum}%3.3f" })}
        prefWidth = 100
        delegate.setReorderable(false)
        styleClass += "table-cell-with-value"
        editable = false
      })
    wkview.columns ++= athletCol ++ riegeCol ++ wertungenCols ++ sumCol
    var isFilterRefreshing = false
    
    wkModel.onChange{(seq1, seq2) => 
      import scalafx.collections.ObservableBuffer._
      if(!isFilterRefreshing) {
        def updateWertungen(index: Int) {
          val changed = wkModel.get(index)
          val idx = wertungen.indexWhere { x => x.head.init.athlet.id == changed.head.init.athlet.id }
          wertungen = wertungen.updated(idx, changed)
        }
        seq2.foreach { change => 
          change match {
            case Remove(position, removed) => 
            case Add(position, added) => 
              updateWertungen(position)
            case Reorder(start, end, permutation) =>
            case Update(from, to) => 
          }
        }
      }
    }
    def updateFilteredList(newVal: String, newDurchgang: GeraeteRiege) {
      val wkListHadFocus = wkview.focused.value
      val selected = wkview.selectionModel.value.selectedCells
      //if(!newVal.equalsIgnoreCase(lastFilter)) {
        lastFilter = newVal
        durchgangFilter = newDurchgang
        val sortOrder = wkview.sortOrder.toList
        isFilterRefreshing = true
        wkModel.clear()
        val searchQuery = newVal.toUpperCase().split(" ")
//        val rd = riegendurchgaenge.values.toList
        def restoreVisibility(col: TableColumn[_, _]) {
          col.sortable.value = true
          if(col.delegate.isInstanceOf[WKTCAccess]) {
            val tca = col.delegate.asInstanceOf[WKTCAccess]
            if(tca.getIndex > -1 && !col.isVisible()) {
              col.setVisible(true)
            }
          }
          col.columns.foreach(restoreVisibility(_))
        }
        def hideIfNotUsed(col: TableColumn[_, _]) {
          col.sortable.value = false
          if(col.delegate.isInstanceOf[WKTCAccess]) {
            val tca = col.delegate.asInstanceOf[WKTCAccess]
            if(tca.getIndex > -1) {
              col.setVisible(durchgangFilter.disziplin.isDefined && tca.getIndex == disziplinlist.indexOf(durchgangFilter.disziplin.get))
            }
          }
          col.columns.foreach(hideIfNotUsed(_))
        }
        val orderedWertungen = if(durchgangFilter.equals(emptyRiege)){
          wkview.columns.foreach{restoreVisibility(_)}
          wertungen}
        else {
          wkview.columns.foreach{hideIfNotUsed(_)}
          wertungen.sortBy { w => durchgangFilter.kandidaten.indexWhere { x => x.id == w.head.init.athlet.id } }
        }
        for{athlet <- orderedWertungen} {
          def isRiegenFilterConform(athletRiegen: Set[Option[String]]) = {
            val undefined = athletRiegen.forall{case None => true case _ => false}
            val durchgangKonform = durchgangFilter.equals(emptyRiege) ||
               durchgangFilter.kandidaten.filter { k => athletRiegen.contains(k.einteilung.map(_.r))}.nonEmpty
            durchgangKonform && (undefined || !athletRiegen.forall{case Some(riege) => !relevantRiegen.getOrElse(riege, (false, 0))._1 case _ => true})
          }
          val matches = athlet.nonEmpty && isRiegenFilterConform(athlet.flatMap(a => Set(a.init.riege, a.init.riege2)).toSet) &&
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
        if (wkListHadFocus) {
          wkview.requestFocus()
          selected.foreach(s => wkview.selectionModel.value.selectedCells.add(s))          
        }
        isFilterRefreshing = false
      //}
  	}
  	val txtUserFilter = new TextField() {
      promptText = "Athlet-Filter"
      text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
        if(!lastFilter.equalsIgnoreCase(newVal)) {
          updateFilteredList(newVal, durchgangFilter)
        }
      }
    }
    txtUserFilter.focused.onChange({
      if(!txtUserFilter.focused.value) {
        val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(wkview)
        Platform.runLater(new Runnable() {
          override def run = {
            focusSetter()
          }
        })
      }
    })
    wkview.focused.onChange({
      if(wkview.focused.value) {
        txtUserFilter.promptText = "Athlet-Filter (CTRL+F)"
      }
      else {
        txtUserFilter.promptText = "Athlet-Filter"
      }
    })

  	val teilnehmerCntLabel = new Label {
  	  margin = Insets(5, 0, 5, 5)
  	}
    val alleRiegenCheckBox = new CheckBox {
      focusTraversable = false
      text = "Alle Riegen" + programm.map(" im " + _.name).getOrElse("")
      margin = Insets(5, 0, 5, 5)
    }
    def updateAlleRiegenCheck(toggle: Boolean = false) {
      val allselected = relevantRiegen.values.forall{x => x._1}
      val newAllSelected = if(toggle) !allselected else allselected
      if(toggle) {
        relevantRiegen = relevantRiegen.map(r => (r._1, (newAllSelected, r._2._2)))
        updateFilteredList(lastFilter, durchgangFilter)
        updateRiegen(false)
      }
      else {
        alleRiegenCheckBox.selected.value = newAllSelected
      }
      val counttotriegen = riegenFilterModel.foldLeft(0)((sum, r) => sum + r.initanz)
      val counttotprogramm = wertungen.size
      //val counttotprogramm = riegenFilterModel.foldLeft(0)((sum, r) => sum + r.initviewanz)
      val countsel = riegenFilterModel.filter(r => relevantRiegen(r.initname)._1).foldLeft(0)((sum, r) => sum + r.initviewanz)
      val rc = relevantRiegen.size
      teilnehmerCntLabel.text = s"$rc Riegen mit $counttotriegen Riegenmitglieder/-innen, $countsel von $counttotprogramm" + programm.map(" im " + _.name).getOrElse("")
    }
    alleRiegenCheckBox onAction = (event: ActionEvent) => {
      updateAlleRiegenCheck(true)
    }
    cmbDurchgangFilter.onAction = handle {
      val d = if(!cmbDurchgangFilter.selectionModel.value.isEmpty) {
        cmbDurchgangFilter.selectionModel.value.getSelectedItem
      }
      else {
        emptyRiege
      }
      if(!durchgangFilter.equals(d)) {
        updateFilteredList(lastFilter, d)
      }
    }

    def updateRiegen(initial: Boolean) {
      def onSelectedChange(name: String, selected: Boolean) = {
        if(relevantRiegen.contains(name)) {
          relevantRiegen = relevantRiegen.updated(name, (selected, relevantRiegen(name)._2))
          updateFilteredList(lastFilter, durchgangFilter)
          updateAlleRiegenCheck()
          selected
        }
        else {
          false
        }
      }
      riegenFilterModel.clear()
      riegen(onSelectedChange, initial).foreach(riegenFilterModel.add(_))
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
      isFilterRefreshing = true
      wkModel.clear()
      wertungen = reloadWertungen()

      relevantRiegen = computeRelevantRiegen

      updateRiegen(false)

      lastFilter = ""
      updateFilteredList(lastFilter, durchgangFilter)
      txtUserFilter.text.value = lastFilter
//      cmbDurchgangFilter.text.value = durchgangFilter
      if(columnrebuild) {
        wkview.columns.clear()
        wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
      }

      try {
        for(ts <- coords) {
          if(ts._2 < -100) {
            val toSelectParent = wkview.columns(ts._2 / -100)
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
      updateEditorPane(if (wkview.focused.value) Some(wkview) else None)
      isFilterRefreshing = false
    }
    
    websocketsubscription = Some(WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
      if (selected.value) {
        newItem match {
          case a @ AthletWertungUpdated(_, wertung, _, _, _, _) =>
            val tableSelected = if (wkview.focused.value) Some(wkview) else None
            wertungen = wertungen.map{aw => 
              val index = wkModel.indexOf(aw)
              val newWertungen = aw.map{ w => 
                if (w.init.id == wertung.id && w.endnote != wertung.endnote) {
                  //KuTuApp.invokeWithBusyIndicator(reloadData())
                  WertungEditor(w.init.updatedWertung(wertung))
                } else {
                  w
                }
              }
              if (index > -1 && wkModel(index).map(_.init.endnote).sum != newWertungen.map(_.init.endnote).sum) {
                isFilterRefreshing = true
                val selected = wkview.selectionModel.value.selectedCells
                wkModel.update(index, newWertungen)
                //selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                isFilterRefreshing = false
              }
              newWertungen
            }
            updateEditorPane(tableSelected)

            //updateFilteredList(lastFilter, durchgangFilter)
            
          case _ =>
        }
      }
    })

    val riegenFilterView = new RiegenFilterView(!wettkampfmode.value,
        wettkampf, service,
        () => {disziplinlist},
        true,
        riegenFilterModel){
    }

    riegenFilterView.addListener((editor: RiegeEditor) => {
      refreshLazyPane()
      reloadData()
    })

    updateRiegen(true)

    def doPasteFromExcel(progrm: Option[ProgrammView])(implicit event: ActionEvent) = {
      import scala.concurrent.ExecutionContext.Implicits._
      import scala.util.{Failure, Success}
      val athletModel = ObservableBuffer[(Long, Athlet, AthletView)]()
      val vereineList = service.selectVereine
      val vereineMap = vereineList.map(v => v.id -> v).toMap
      val vereine = ObservableBuffer[Verein](vereineList)
      val cbVereine = new ComboBox[Verein] {
        items = vereine
        //selectionModel.value.selectFirst()
      }
      val programms = programm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
      val clipboardlines = Source.fromString(Clipboard.systemClipboard.getString + "").getLines()
      val cache = new java.util.ArrayList[MatchCode]()
      val cliprawf = KuTuApp.invokeAsyncWithBusyIndicator { Future {
                     clipboardlines.
                     map    { line   => line.split("\\t").map(_.trim()) }.
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
                            programms.filter(pgm => pgm.name.equalsIgnoreCase(fields(3))).headOption match {
                              case Some(p) => p.id
                              case _ => progrm match {
                                case Some(p) => p.id
                                case None => 0L
                              }
                            }
                        }
                       (progId, parsed, AthletView(
                           candidate.id, candidate.js_id,
                           candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
                           candidate.strasse, candidate.plz, candidate.ort,
                           candidate.verein.map(vereineMap), true))
                    }.toList
        }
      }
      import scala.concurrent.ExecutionContext.Implicits._
      cliprawf.onComplete {
        case Failure(t) => logger.debug(t.toString)
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
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList
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
                  service.assignAthletsToWettkampf(wettkampf.id, Set(progId), athletes.toSet)
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
                val pgathl = clip.groupBy(_._1).map(x => (x._1, x._2.map(_._2.id)))
                logger.debug(pgathl.toString)
                for((progId, athletes) <- pgathl) {
                  service.assignAthletsToWettkampf(wettkampf.id, Set(progId), athletes.toSet)
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
          logger.debug(programme.toString)
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
            ,AthletJahrgang(athlet.gebdat).jahrgang
            ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
            ,einsatz.riege.getOrElse("")
            ,riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._2
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
          val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs()
          }
          val file = new java.io.File(dir.getPath + "/" + filename)
          def generate(lpp: Int) = toHTMLasKategorienListe(seriendaten, PrintUtil.locateLogoFile(dir))
          PrintUtil.printDialog(text.value,FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
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
          logger.debug(programme.toString)
          val riegendurchgaenge = service.selectRiegen(wettkampf.id).map(r => r.r-> r).toMap
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
            ,athlet.id
            ,athlet.name
            ,athlet.vorname
            ,AthletJahrgang(athlet.gebdat).jahrgang
            ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
            ,riegendurchgaenge.get(einsatz.riege.getOrElse(""))
            ,None
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
            }.map(_.init.wettkampfdisziplin.disziplin)
            ,
            Seq.empty,
            athletwertungen.toSeq.map(_.view)
            )
          }
          val filename = "Notenblatt_" + wettkampf.easyprint.replace(" ", "_") + programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") + riege.map("_Riege_" + _.replace(" ", "_")).getOrElse("") + ".html"
          val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs()
          }
          val logofile = PrintUtil.locateLogoFile(dir)
          def generate(lpp: Int) = wettkampf.programm.head.id match {
            case 20 => toHTMLasGeTu(seriendaten, logofile)
            case n if(n == 11 || n == 31) => toHTMLasKuTu(seriendaten, logofile)
            case _ => toHTMLasATT(seriendaten, logofile)
          }
          PrintUtil.printDialog(text.value,FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Landscape)(event)
        }
      }
    }
    val generateBestenliste = new Button with BestenListeToHtmlRenderer {
      text = "Bestenliste erstellen"
      minWidth = 75

      onAction = (event: ActionEvent) => {
        if (!WebSocketClient.isConnected) {          
          val filename = "Bestenliste_" + wettkampf.easyprint.replace(" ", "_") + ".html"
          val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if(!dir.exists()) {
            dir.mkdirs()
          }
          val logofile = PrintUtil.locateLogoFile(dir)
          
          def generate(lpp: Int) = toHTMListe(WertungServiceBestenResult.getBestenResults, logofile)
          PrintUtil.printDialog(text.value,FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
        } else {
          Await.result(KuTuServer.finishDurchgangStep(wettkampf), Duration.Inf)          
        }
        WertungServiceBestenResult.resetBestenResults
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
  							  reloadData()
  						  }
  					  }
  				  })
  			  }
    }
    
    val removeButton = new Button {
      text = "Athlet entfernen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (!wkview.selectionModel().isEmpty) {
      	  val wertungEditor = wkview.selectionModel().getSelectedItem.head
          val athletwertungen = wkview.selectionModel().getSelectedItem.map(_.init.id).toSet
          implicit val impevent = event
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new HBox {
                prefHeight = 50
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(
                    new Label(
                        s"Soll '${wertungEditor.init.athlet.easyprint}' wirklich aus der Einteilung im ${wertungEditor.init.wettkampfdisziplin.programm.name} entfernt werden?"))
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              service.unassignAthletFromWettkampf(athletwertungen)
              wkModel.remove(wkview.selectionModel().getSelectedIndex)
            }
          })
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
              service.moveToProgram(wettkampf.id, cbProgramms.selectionModel().selectedItem.value.id, wkview.selectionModel().getSelectedItem.head.init.athlet.id)
              reloadData()
            }
          }
        })
      }
    }
    val setRiege2ForAllButton = new Button {
      text = "2. Riege"
      tooltip = "2. Riegenzuteilung für alle in der Liste angezeigten Tu/Ti"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        implicit val impevent = event
        val selectedRiege = wkModel
				val txtRiegenName = new TextField
			  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
				  def getPage: Node = {
				  new HBox {
					  prefHeight = 50
							  alignment = Pos.BottomRight
							  hgrow = Priority.Always
							  children = Seq(new Label("Neuer Riegenname für die zweite Riegenzuteilung "), txtRiegenName)
				  }
			  }
			  }, new Button("OK") {
				  onAction = (event: ActionEvent) => {
					  KuTuApp.invokeWithBusyIndicator {
					    val ws = selectedRiege.map(wl => wl.map(w => w.init.copy(riege2 = if(txtRiegenName.text.value.nonEmpty) Some(txtRiegenName.text.value) else None).toWertung))
						  ws.foreach { x => x.foreach(service.updateWertungSimple(_))}
						  reloadData()
					  }
				  }
			  })
      }
    }

    //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
    val moveAvaillable = programm.forall { p => p.head.id != 1l }
    moveToOtherProgramButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose moveAvaillable otherwise false
    setRiege2ForAllButton.disable <== when(Bindings.createBooleanBinding(() => {
      wkModel.isEmpty
    },
      wkModel
    )) choose true otherwise false
    removeButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

    val actionButtons = programm match {
      case None => wettkampf.programm.id match {
        case 1 => // Athletiktest
          val addButton = new Button {
            text = "Athlet hinzufügen"
            minWidth = 75
            onAction = (event: ActionEvent) => {
              new AthletSelectionDialog(
                text.value, wettkampf.programm, wertungen.map(w => w.head.init.athlet), service,
                (selection: Set[Long]) => {
                  service.assignAthletsToWettkampf(wettkampf.id, Set(2, 3), selection)
                  reloadData()
                }
              ).execute(event)
            }
          }
          List[Button](addButton, removeButton, setRiege2ForAllButton, riegeRenameButton, riegenRemoveButton, generateTeilnehmerListe, generateNotenblaetter)
          
        case _ => // andere
          val pasteFromExcel = new Button("Aus Excel einfügen ...") {
            onAction = (event: ActionEvent) => {
              doPasteFromExcel(Some(wettkampf.programm))(event)
            }
          }
          List[Button](pasteFromExcel, removeButton, setRiege2ForAllButton, riegeRenameButton, riegenRemoveButton, generateTeilnehmerListe, generateNotenblaetter)
      }
      case Some(progrm) =>
        val addButton = new Button {
          text = "Athlet hinzufügen"
          minWidth = 75
          onAction = (event: ActionEvent) => {
            new AthletSelectionDialog(
              text.value, progrm, wertungen.map(w => w.head.init.athlet), service,
              (selection: Set[Long]) => {
                service.assignAthletsToWettkampf(wettkampf.id, Set(progrm.id), selection)
                reloadData()
              }
            ).execute(event)
          }
        }
        val pasteFromExcel = new Button("Aus Excel einfügen ...") {
          onAction = (event: ActionEvent) => {
            doPasteFromExcel(Some(progrm))(event)
          }
        }
        List(addButton, pasteFromExcel, moveToOtherProgramButton, removeButton, setRiege2ForAllButton, riegenRemoveButton, generateTeilnehmerListe, generateNotenblaetter).filter(btn => !btn.text.value.equals("."))
    }

    val clearButton = new Button {
      text = "Athlet zurücksetzen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (!wkview.selectionModel().isEmpty) {
          val selected = wkview.selectionModel().getSelectedItem
          implicit val impevent = event
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new HBox {
                prefHeight = 50
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(
                  new Label(
                    s"Sollen wirklich die in diesem Wettkampf bereits erfassten Resultate für '${selected.head.init.athlet.easyprint}' zurückgesetzt werden?"))
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
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
          })
        }
      }
    }
    clearButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

    wkview.selectionModel.value.setCellSelectionEnabled(true)
    wkview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(wkview, true, txtUserFilter)(ke)
    }

    if(!wettkampfmode.value) {
      riegenFilterView.selectionModel.value.setCellSelectionEnabled(true)
      riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
        AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(riegenFilterView, false, txtUserFilter)(ke)
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
      center = new StackPane {
//        children += webView
        children += wkview
      }
    }

    val filterControl = new HBox {
   		children += teilnehmerCntLabel
    }

    val riegenHeader = new VBox {
      focusTraversable = false
      maxWidth = Double.MaxValue
      minHeight = Region.USE_PREF_SIZE
      val title = new Label {
        text = "Riegen-Filter"
        styleClass += "toolbar-header"
      }
      children += title
      children += filterControl
      children += alleRiegenCheckBox
    }
    riegenFilterView.focusTraversable = false
    val riegenFilterPane = new BorderPane {
      focusTraversable = false
      hgrow = Priority.Always
      vgrow = Priority.Always
      prefWidth = 500
      margin = Insets(0, 0, 0, 10)
      top = riegenHeader
      center = new BorderPane {
        center = riegenFilterView
      }
      bottom = filterControl
    }

    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
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
        ) ++ (if(wettkampfmode.value) List(cmbDurchgangFilter, txtUserFilter, generateBestenliste) else actionButtons :+ clearButton :+ cmbDurchgangFilter :+ txtUserFilter)
      }
      center = new SplitPane {
        orientation = Orientation.Horizontal
     		items += riegenFilterPane
        items += editTablePane

        setDividerPosition(0, 0.3d)

        SplitPane.setResizableWithParent(riegenFilterPane, false)
      }
    }

    content = cont

    true
  }
}
