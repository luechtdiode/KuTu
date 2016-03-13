package ch.seidel.kutu.view

import java.text.SimpleDateFormat
import scala.collection.mutable.StringBuilder
import javafx.scene.{ control => jfxsc }
import javafx.collections.{ ObservableList, ListChangeListener }
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.util.converter.DefaultStringConverter
import scalafx.util.converter.DoubleStringConverter
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.value.ObservableValue
import scalafx.beans.property.ReadOnlyDoubleWrapper
import scalafx.event.ActionEvent
import scalafx.scene.layout.Region
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.scene.layout.BorderPane
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.StringProperty
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{ Tab, TabPane }
import scalafx.scene.layout.{ Priority, StackPane }
import scalafx.scene.control.{ TableView, TableColumn }
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.ToolBar
import scalafx.scene.control.Button
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ComboBox
import scalafx.scene.layout.HBox
import scalafx.scene.Group
import scalafx.scene.web.WebView
import java.io.FileOutputStream
import java.awt.Desktop
import java.io.BufferedOutputStream
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import ch.seidel.commons._
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain._
import ch.seidel.kutu.data._
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer
import ch.seidel.kutu.data.FilterBy
import scalafx.scene.control.ListCell
import javafx.util.Callback
import scalafx.scene.control.ListView
import scalafx.scene.control.CheckBox

abstract class DefaultRanglisteTab(override val service: KutuService) extends Tab with TabWithService with ScoreToHtmlRenderer {
  override val title = ""

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
  def groupers: List[FilterBy] = ???
  def getData: Seq[WertungView] = ???
  case class FilenameDefault(filename: String, dir: java.io.File)
  def getSaveAsFilenameDefault: FilenameDefault = ???

  def populate(groupers: List[FilterBy]): Seq[ComboBox[FilterBy]] = {
    val gr1Model = ObservableBuffer[FilterBy](groupers)
    val nullFilter = NullObject("alle")
    val grf1Model = ObservableBuffer[DataObject](Seq(nullFilter))
    val grf2Model = ObservableBuffer[DataObject](Seq(nullFilter))
    val grf3Model = ObservableBuffer[DataObject](Seq(nullFilter))
    val grf4Model = ObservableBuffer[DataObject](Seq(nullFilter))

    class DataObjectListCell extends ListCell[DataObject] {
      override val delegate: jfxsc.ListCell[DataObject] = new jfxsc.ListCell[DataObject] {
        override protected def updateItem(item: DataObject, empty: Boolean) {
          super.updateItem(item, empty)
          if (item != null) {
              setText(item.easyprint);
          }
        }
      }
    }
    val cb1 = new ComboBox[FilterBy] {
      maxWidth = 250
        minWidth = 250
      promptText = "erste Gruppierung..."
      items = gr1Model
    }
    val cb2 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 250
        promptText = "zweite Gruppierung..."
        items = gr1Model
      }
    val cb3 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 250
        promptText = "dritte Gruppierung..."
        items = gr1Model
      }
    val cb4 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 250
        promptText = "vierte Gruppierung..."
        items = gr1Model
      }
    val combs = List(cb1, cb2, cb3, cb4)
    val cbf1 = new ComboBox[DataObject] {
      maxWidth = 250
      minWidth = 250
      promptText = "alle"
      items = grf1Model
      buttonCell = new DataObjectListCell()
      cellFactory = { p => new DataObjectListCell() }
    }
    val cbf2 =
      new ComboBox[DataObject] {
        maxWidth = 250
        minWidth = 250
        promptText = "alle"
        items = grf2Model
        buttonCell = new DataObjectListCell()
        cellFactory = { p => new DataObjectListCell() }
      }
    val cbf3 =
      new ComboBox[DataObject] {
        maxWidth = 250
        minWidth = 250
        promptText = "alle"
        items = grf3Model
        buttonCell = new DataObjectListCell()
        cellFactory = { p => new DataObjectListCell() }
      }
    val cbf4 =
      new ComboBox[DataObject] {
        maxWidth = 250
        minWidth = 250
        promptText = "alle"
        items = grf4Model
        buttonCell = new DataObjectListCell()
        cellFactory = { p => new DataObjectListCell() }
      }
    val combfs = List(cbf1, cbf2, cbf3, cbf4)
    val fmodels = List(grf1Model, grf2Model, grf3Model, grf4Model)
    val webView = new WebView
    val cbModus: CheckBox = new CheckBox {
      text = "Sortierung alphabetisch"
      selected = false
    }

    def relevantGroup(cb: ComboBox[FilterBy]): Boolean = {
      if(!cb.selectionModel.value.isEmpty) {
        val grp = cb.selectionModel.value.getSelectedItem
        grp != ByNothing
      }
      else {
        false
      }
    }
    def buildGrouper = {
      groupers.foreach { gr => gr.reset }
      val cblist = combs.zip(combfs).filter{cbp =>
        val (cb, cf) = cbp
        val ret = relevantGroup(cb)
        cf.disable.value = !ret
        ret
      }.map{cbp =>
        val (cb, cf) = cbp
        val grp = cb.selectionModel.value.getSelectedItem

        if(cf.selectionModel.value.isEmpty() || cf.selectionModel.value.selectedItem.value.equals(nullFilter)) {
        	grp.setFilter(None)
        }
        else {
          grp.setFilter(Some(cf.selectionModel.value.selectedItem.value))
        }
        grp
      }

      if (cblist.isEmpty) {
        ByWettkampfProgramm().groupBy(ByGeschlecht)
      }
      else {
        cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy, forPrint: Boolean = false) = {
    	val data = getData

      val filter = query.asInstanceOf[FilterBy]
      val filterLists = filter.traverse(Seq[Seq[DataObject]]()){ (f, acc) =>
        acc :+ f.asInstanceOf[FilterBy].analyze(data).sortBy { x => x.easyprint}
      }
    	filterLists.zip(fmodels.zip(combs).filter{x => relevantGroup(x._2)}.map(_._1)).foreach {x =>
    	  val (raw, model) = x
    	  val toRemove = for(o <- model if(!raw.contains(o) && o != nullFilter)) yield { o }
    	  for(i <- toRemove) {model.remove(model.indexOf(i))}
    	  for(o <- raw if(!model.contains(o))) {
    	    model.append(o)
    	  }
    	  model.sortBy { x => x.easyprint}
    	}
      val combination = query.select(data).toList
      val ret = toHTML(combination, if(forPrint) 40 else 0, cbModus.selected.value)
      if(!forPrint) webView.engine.loadContent(ret)
      ret
    }

    combs.foreach{c =>
      c.onAction = handle {
        refreshRangliste(buildGrouper)
      }
    }
    combfs.foreach{c =>
      c.onAction = handle {
//        val selected = c.selectionModel.value.selectedItem.value
        refreshRangliste(buildGrouper)
      }
    }
    cbModus.onAction = handle {
      refreshRangliste(buildGrouper)
    }

    val btnSave = new Button {
      text = "Speichern als ..."
      onAction = handle {
          val defaults = getSaveAsFilenameDefault
          val filename = defaults.filename
          val dir = defaults.dir
          if(!dir.exists()) {
            dir.mkdirs();
          }
          val fileChooser = new FileChooser() {
          initialDirectory = dir
          title = "Rangliste zum drucken speichern ..."
          extensionFilters.addAll(
                 new ExtensionFilter("Web-Datei", "*.html"),
                 new ExtensionFilter("All Files", "*.*"))
          initialFileName = filename
        }
        val selectedFile = fileChooser.showSaveDialog(KuTuApp.getStage())
        if (selectedFile != null) {
          val file = if(!selectedFile.getName.endsWith(".html") && !selectedFile.getName.endsWith(".htm")) {
            new java.io.File(selectedFile.getAbsolutePath + ".html")
          }
          else {
            selectedFile
          }
//          val logofile = if(new java.io.File(dir.getPath + "/logo.jpg").exists()) {
//            "logo.jpg"
//          }
//          else {
//            "../logo.jpg"
//          }
          val toSave = refreshRangliste(buildGrouper, true).getBytes("UTF-8")
          val os = new BufferedOutputStream(new FileOutputStream(selectedFile))
          os.write(toSave)
          os.flush()
          os.close()
          Desktop.getDesktop().open(selectedFile);
//          new ProcessBuilder().command("explorer.exe", selectedFile.getAbsolutePath).start()
        }
      }
    }
    onSelectionChanged = handle {
      if(selected.value) {
        refreshRangliste(buildGrouper)
      }
    }
    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      //              children = new Label("Filter:") :+ combfs

      val topPanel1 = new HBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        spacing = 15
        children = new Label("Gruppierungen:") {
          padding = Insets(4,0,0,0)
          prefWidth = 120
        } +: combs :+ btnSave
      }
      val topPanel2 = new HBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        spacing = 15
        padding = Insets(4,0,0,0)
        children = new Label("Filter:") {
          padding = Insets(4,0,0,0)
          prefWidth = 120
        } +: combfs :+ cbModus
      }
      top = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        padding = Insets(15)
        children = Seq(topPanel1, topPanel2)
      }
      center = webView
    }
    combs
  }
}
