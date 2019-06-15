package ch.seidel.kutu.view

import java.io._
import java.net.URI
import java.util.concurrent.{ScheduledFuture, TimeUnit}

import ch.seidel.commons._
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.KuTuApp.hostServices
import ch.seidel.kutu.data.{FilterBy, _}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{PrintUtil, ScoreToHtmlRenderer}
import javafx.collections.ObservableList
import javafx.scene.text.FontSmoothingType
import javafx.scene.{control => jfxsc}
import org.controlsfx.control.CheckComboBox
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.event.subscriptions.Subscription
import scalafx.geometry.Insets
import scalafx.print.{PageOrientation, Printer}
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.util.StringConverter

import scala.language.implicitConversions

abstract class DefaultRanglisteTab(override val service: KutuService) extends Tab with TabWithService with ScoreToHtmlRenderer {

  override val title = ""
  var subscription: Option[Subscription] = None

  override def release() {
    subscription.foreach(_.cancel)
  }
  
  var lazyPaneUpdater: Map[String, ScheduledFuture[_]] = Map.empty
 
  def submitLazy(name: String, task: ()=>Unit, delay: Long) {
    lazyPaneUpdater.get(name).foreach(_.cancel(true))
    val ft = KuTuApp.lazyExecutor.schedule(new Runnable() { def run = { 
      Platform.runLater{task()}
    }}, delay, TimeUnit.SECONDS)
    
    lazyPaneUpdater = lazyPaneUpdater + (name -> ft)
  }
  

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
  val webView = new WebView {
    fontSmoothingType = FontSmoothingType.GRAY

    //zoom = 1.2
    //fontScale = 1.2
  }
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
        grp != ByNothing()
      }
      else {
        false
      }
    }

    val combos = combs.zip(combfs)

    def buildGrouper = {
      restoring = true
      groupers.foreach { gr => gr.reset }
      val cblist = combos.filter{cbp =>
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
        ByWettkampfProgramm().groupBy(ByGeschlecht())
      }
      else {
        cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy, linesPerPage: Int = 0) = {
      restoring = true
    	val data = getData

      val filter = query.asInstanceOf[FilterBy]
      val filterLists = filter.traverse(Seq[Seq[DataObject]]()){ (f, acc) =>
        val allItems = f.asInstanceOf[FilterBy].analyze(data).sortBy { x => x.easyprint}
        acc :+ (if (f.canSkipGrouper) nullFilter +: allItems else allItems)
      }
      combfs.filter(cmb => cmb.disabled.value).foreach(cmb => {
        cmb.getCheckModel.clearChecks()
        cmb.getItems.clear()
      })
    	filterLists.zip(fmodels.zip(combos).filter{x => relevantGroup(x._2._1)}.map(x => (x._1, x._2._2))).foreach {x =>
    	  val ( expected, (model, combf)) = x
    	  val checked = combf.getCheckModel.getCheckedItems.toSet
    	  combf.getCheckModel.clearChecks()
    	  model.retainAll(expected)
    	  model.insertAll(model.size, expected.filter(!model.contains(_)))
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

      combos.foreach{cb =>
        val (cmb, cmbf) = cb
        cmb.selectionModel.value.clearSelection()
        cmbf.getCheckModel.clearChecks
        cmbf.getItems.clear
        cmbf.setDisable(true)
      }

      query.traverse(combos){(grp, acc) =>
        logger.debug(grp.toString)
        if(acc.isEmpty) {
          acc
        }
        else {
          val (cmb, cmbf) = acc.head

          cmb.selectionModel.value.select(grp.asInstanceOf[FilterBy])
          cmbf.setDisable(false)
          val expected: List[DataObject] = grp.asInstanceOf[FilterBy].filterItems
            .sortWith{case (a, b) => a.easyprint.compareTo(b.easyprint) < 0}

          expected.foreach(item => cmbf.getItems.add(item))
          grp.asInstanceOf[FilterBy].getFilter.foreach(cmbf.getCheckModel.check(_))

          acc.tail
        }
      }
      restoring = false
      refreshRangliste(query)
    }
  
    combos.foreach{ case (comb, combfs) =>
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
    if (logger.isDebugEnabled()) {
      logger.debug("subscribing for refreshing from websocket")
    }
    subscription = Some(WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
      if (selected.value) {
        submitLazy("refreshRangliste", () => if (selected.value) {
            if (logger.isDebugEnabled()) {
              logger.debug("refreshing rangliste from websocket", newItem)
            }
            refreshRangliste(buildGrouper)
          }, 5
        )
      }
    })

    val btnPrint = PrintUtil.btnPrint(text.value, getSaveAsFilenameDefault, true, (lpp:Int)=>refreshRangliste(buildGrouper, lpp))
    
    def extractFilterText = {
      buildGrouper.toRestQuery
        .replace("/", "_")
        .replace(".", "_")
        .replace("?", "_")
        .replace("&", "+")
        .replace("=", "-")
        .replace(":", "-")
        .replace("!", "-")
        .replace("groupby", "Gruppiert")
        .replace("filter", "Gefiltert")
    }

    def getFilterSaveAsFilenameDefault: FilenameDefault = {
      val default = getSaveAsFilenameDefault
      FilenameDefault(extractFilterText + ".scoredef", default.dir)
    }

    def loadFilter(selectedFile: File) {
      val ios = new ObjectInputStream(new FileInputStream(selectedFile))
      val grouper = GroupBy(ios.readObject().toString, getData)
      restoreGrouper(grouper)
    }

    def listFilter = getFilterSaveAsFilenameDefault.dir
      .listFiles()
      .filter(f => f.getName.endsWith(".scoredef"))
      .toList
      .sortBy { _.getName }

    def addPredefinedFilter(items: ObservableList[javafx.scene.control.MenuItem])(filter: File): Unit = {
      val menu = KuTuApp.makeMenuAction(filter.getName
        .replace(".scoredef", "")
        .replace("-", " ")
        .capitalize) { (caption, action) => loadFilter(filter) }
      items.add(menu)
    }

    val cbfSaved = new MenuButton("Gespeicherte Einstellungen") {
      val add: File => Unit = addPredefinedFilter(items)
      listFilter.foreach(add(_))
      disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
    }

    val btnSaveFilter = new Button {
      text = "Einstellung speichern als ..."
      onAction = handle {
          val defaults = getFilterSaveAsFilenameDefault
          val filename = defaults.filename
          val dir = defaults.dir
          if(!dir.exists()) {
            dir.mkdirs()
          }
          val fileChooser = new FileChooser() {
          initialDirectory = dir
          title = "Filtereinstellung speichern ..."
          extensionFilters.addAll(
                 new ExtensionFilter("Filtereinstellung", "*.scoredef"))
          initialFileName = filename
        }
        val selectedFile = fileChooser.showSaveDialog(KuTuApp.getStage())
        if (selectedFile != null) {
          val file = if(!selectedFile.getName.endsWith(".scoredef")) {
            new java.io.File(selectedFile.getAbsolutePath + ".scoredef")
          }
          else {
            selectedFile
          }
          val os = new ObjectOutputStream(new FileOutputStream(file))
          os.writeObject(buildGrouper.toRestQuery)
          os.flush()
          os.close()
          addPredefinedFilter(cbfSaved.items)(file)
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
      val topCombos = combos.map{ccs =>
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
      top = new VBox{
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = List(
          new ToolBar {
            content = List(cbfSaved, btnSaveFilter, btnPrint, cbModus)
          },
          new ToolBar {
            content = (topBox +: topCombos)
          })

      }
      center = webView
    }
    combs
  }
}
