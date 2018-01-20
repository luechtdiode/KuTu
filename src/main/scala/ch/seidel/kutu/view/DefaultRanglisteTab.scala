package ch.seidel.kutu.view

import java.awt.Desktop
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream

import scala.language.implicitConversions

import org.controlsfx.control.CheckComboBox

import ch.seidel.commons._
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.data._
import ch.seidel.kutu.data.FilterBy
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer
import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.print.PageOrientation
import scalafx.print.Printer
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.web.WebEngine
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.util.StringConverter
import scala.collection.JavaConverters
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault

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
  def getSaveAsFilenameDefault: FilenameDefault = ???
  val webView = new WebView
  var restoring = false
  val nullFilter = NullObject("alle")
  
  def print(printer: Printer) {
    PrintUtil.printWebContent(webView.engine, printer, PageOrientation.Portrait)
  }
  
  def populate(groupers: List[FilterBy]): Seq[ComboBox[FilterBy]] = {
    val gr1Model = ObservableBuffer[FilterBy](groupers)
    
    val grf1Model = ObservableBuffer[DataObject](Seq())
    val grf2Model = ObservableBuffer[DataObject](Seq())
    val grf3Model = ObservableBuffer[DataObject](Seq())
    val grf4Model = ObservableBuffer[DataObject](Seq())

    class DataObjectConverter extends StringConverter[DataObject] {
      def fromString(text: String) = {
        nullFilter
      }
      def toString(d: DataObject) = if (d != null) d.easyprint else ""
    }
    val converter = new DataObjectConverter()
   
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
    class FileListCell extends ListCell[File] {
      override val delegate: jfxsc.ListCell[File] = new jfxsc.ListCell[File] {
       override protected def updateItem(item: File, empty: Boolean) {
         super.updateItem(item, empty)
         if (item != null) {
           setText(item.getName);
         }
       }
     }
   }

    val cb1 = new ComboBox[FilterBy] {
      maxWidth = 250
      minWidth = 100
      promptText = "erste Gruppierung..."
      items = gr1Model
    }
    val cb2 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 100
        promptText = "zweite Gruppierung..."
        items = gr1Model
      }
    val cb3 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 100
        promptText = "dritte Gruppierung..."
        items = gr1Model
      }
    val cb4 =
      new ComboBox[FilterBy] {
        maxWidth = 250
        minWidth = 100
        promptText = "vierte Gruppierung..."
        items = gr1Model
      }
    val combs = List(cb1, cb2, cb3, cb4)
    val cbf1 = new CheckComboBox[DataObject](grf1Model) {
      setMaxWidth(250)
      setMinWidth(100)
      setConverter(converter)
    }
    val cbf2 = new CheckComboBox[DataObject](grf2Model) {
      setMaxWidth(250)
      setMinWidth(100)
      setConverter(converter)
    }
    val cbf3 = new CheckComboBox[DataObject](grf3Model) {
      setMaxWidth(250)
      setMinWidth(100)
      setConverter(converter)
    }
    val cbf4 = new CheckComboBox[DataObject](grf4Model) {
      setMaxWidth(250)
      setMinWidth(100)
      setConverter(converter)
    }
    val combfs = List(cbf1, cbf2, cbf3, cbf4)
    val fmodels = List(grf1Model, grf2Model, grf3Model, grf4Model)
    
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
      restoring = true
      groupers.foreach { gr => gr.reset }
      val cblist = combs.zip(combfs).filter{cbp =>
        val (cb, cf) = cbp
        val ret = relevantGroup(cb)
        cf.setDisable(!ret)
        ret
      }.map{cbp =>
        val (cb, cf) = cbp
        val grp = cb.selectionModel.value.getSelectedItem

        if(cf.getCheckModel.getCheckedItems.isEmpty() || cf.getCheckModel.getCheckedItems.forall(nullFilter.equals(_))) {
        	grp.reset
        }
        else {
          grp.setFilter(cf.getCheckModel.getCheckedItems.toSet[DataObject])
        }
        grp
      }
      restoring = false

      if (cblist.isEmpty) {
        ByWettkampfProgramm().groupBy(ByGeschlecht)
      }
      else {
        cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy, linesPerPage: Int = 0) = {
      restoring = true
    	val data = getData
//    	logger.debug(query.chainToString)
      val filter = query.asInstanceOf[FilterBy]
      val filterLists = filter.traverse(Seq[Seq[DataObject]]()){ (f, acc) =>
        val allItems = f.asInstanceOf[FilterBy].analyze(data).sortBy { x => x.easyprint}
        acc :+ (if (f.canSkipGrouper) nullFilter +: allItems else allItems)
      }
      combfs.filter(cmb => cmb.disabled.value).foreach(cmb => {
        cmb.getCheckModel.clearChecks()
        cmb.getItems.clear()
      })
    	filterLists.zip(fmodels.zip(combs.zip(combfs)).filter{x => relevantGroup(x._2._1)}.map(x => (x._1, x._2._2))).foreach {x =>
    	  val ( expected, (model, combf)) = x
    	  val checked = combf.getCheckModel.getCheckedItems.toSet
    	  combf.getCheckModel.clearChecks()
    	  model.retainAll(expected)
    	  model.insertAll(model.size, expected.filter(!model.contains(_)))
//    	  for(o <- expected if(!model.contains(o))) {
//    	    model.append(o)
//    	  }
    	  model.sort{case (a, b) => a.easyprint.compareTo(b.easyprint) < 0}
      
    	  checked.filter(model.contains(_)).foreach(combf.getCheckModel.check(_))
    	}
      val combination = query.select(data).toList
      //Map[Long,Map[String,List[Disziplin]]]
      val diszMap = data.groupBy { x => x.wettkampf.programmId }.map{ x =>
        x._1 -> Map(
              "W" -> service.listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("W"))
            , "M" -> service.listDisziplinesZuWettkampf(x._2.head.wettkampf.id, Some("M")))
        }
      val logofile = PrintUtil.locateLogoFile(getSaveAsFilenameDefault.dir)
      val ret = toHTML(combination, linesPerPage, cbModus.selected.value, diszMap, logofile)
      if(linesPerPage == 0){
        webView.engine.loadContent(ret)
      }
      restoring = false
      ret
    }
    
    def restoreGrouper(query: GroupBy) {
      restoring = true
      query.traverse(combs.zip(combfs)){(grp, acc) =>
        logger.debug(grp.toString)        
        if(acc.isEmpty) {
          acc
        }
        else {
          val (cmb, cmbf) = acc.head
//          cmb.selectionModel.value.clearSelection()
//          cmb.selectionModel.value.select(grp.asInstanceOf[FilterBy])
//          cmbf.selectionModel.value.clearSelection()
          grp.asInstanceOf[FilterBy].getFilter.foreach {f =>
            logger.debug(f.toString)
//            cmbf.getSelectionModel.select(f)
          }
          acc.tail
        }
      }
      restoring = false;
      refreshRangliste(buildGrouper)
    }

    combs.zip(combfs).foreach{ case (comb, combfs) =>
      comb.onAction = handle {
        if(!restoring) {
          restoring = true
          combfs.getCheckModel.clearChecks()
          combfs.getItems.clear()
          refreshRangliste(buildGrouper)
        }
      }
      combfs.getCheckModel.getCheckedItems.onChange((b, s) => {
        if(!restoring) {
          refreshRangliste(buildGrouper)
        }
      })        
    }
    
    cbModus.onAction = handle {
      if(!restoring)
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
          title = "Rangliste zum speichern ..."
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
          val toSave = refreshRangliste(buildGrouper).getBytes("UTF-8")
          val os = new BufferedOutputStream(new FileOutputStream(selectedFile))
          os.write(toSave)
          os.flush()
          os.close()
          Desktop.getDesktop().open(selectedFile);
        }
      }
    }
    val btnPrint = PrintUtil.btnPrint(text.value, getSaveAsFilenameDefault, true, (lpp:Int)=>refreshRangliste(buildGrouper, lpp))
    
    def extractFilterText = {
      buildGrouper.traverse(""){(item, filename) =>
        item match {
          case ByNothing => filename
          case _ if(filename.isEmpty()) => filename + item.groupname
          case _ => filename + "-" + item.groupname
        }
      }
    }

    def getFilterSaveAsFilenameDefault: FilenameDefault = {
      val default = getSaveAsFilenameDefault
      FilenameDefault(extractFilterText, default.dir)
    }

    def loadFilter(selectedFile: File) {
      val ios = new ObjectInputStream(new FileInputStream(selectedFile))
      val grouper = ios.readObject().asInstanceOf[GroupBy]
      restoreGrouper(grouper)
    }

    //def listFilter = getFilterSaveAsFilenameDefault.dir.listFiles().filter(f => f.getName.endsWith(".filter")).toList.sortBy { _.getName }

//    val cbfSaved =
//      new ComboBox[File] {
//        maxWidth = 250
//        minWidth = 250
//        promptText = ""
//        items = ObservableBuffer[File](listFilter)
//        buttonCell = new FileListCell()
//        cellFactory = { p => new FileListCell() }
//        onAction = handle {
//          if(selectionModel.value.getSelectedItem != null) {
//            loadFilter(selectionModel.value.getSelectedItem)
//          }
//        }
//      }
//
//    val _btnSaveFilter = new Button {
//      text = "Filter speichern als ..."
//      onAction = handle {
//          val defaults = getFilterSaveAsFilenameDefault
//          val filename = defaults.filename
//          val dir = defaults.dir
//          if(!dir.exists()) {
//            dir.mkdirs();
//          }
//          val fileChooser = new FileChooser() {
//          initialDirectory = dir
//          title = "Filtereinstellung speichern ..."
//          extensionFilters.addAll(
//                 new ExtensionFilter("Filtereinstellung", "*.filter"),
//                 new ExtensionFilter("All Files", "*.*"))
//          initialFileName = filename
//        }
//        val selectedFile = fileChooser.showSaveDialog(KuTuApp.getStage())
//        if (selectedFile != null) {
//          val file = if(!selectedFile.getName.endsWith(".filter") && !selectedFile.getName.endsWith(".filter")) {
//            new java.io.File(selectedFile.getAbsolutePath + ".filter")
//          }
//          else {
//            selectedFile
//          }
//          val os = new ObjectOutputStream(new FileOutputStream(selectedFile))
//          os.writeObject(buildGrouper)
//          os.flush()
//          os.close()
//          cbfSaved.items = ObservableBuffer[File](listFilter)
//        }
//      }
//    }

    onSelectionChanged = handle {
      if(selected.value) {
        refreshRangliste(buildGrouper)
      }
    }
    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      //              children = new Label("Filter:") :+ combfs
      val label = new Label("Gruppierungen:") {
        padding = Insets(7,0,0,0)
      }
      val labelfilter = new Label("Filter:") {
        padding = Insets(7,0,0,0)
      }
      val topBox = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = List(label, labelfilter)
      }
      val topCombos = combs.zip(combfs).map{ccs =>
        new VBox {
          vgrow = Priority.Always
          hgrow = Priority.Always
          val filterControl: Control = ccs._2
          children = List(ccs._1, filterControl)
        }
      }
      val topActions = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = List(btnPrint, cbModus)
      }
      top = new ToolBar {
        content = (topBox +: topCombos :+ topActions)
      }
      center = webView
    }
    combs
  }
}
