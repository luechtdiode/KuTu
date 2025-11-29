package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.actors.{AthletWertungUpdated, AthletWertungUpdatedSequenced, KutuAppEvent, LastResults}
import ch.seidel.kutu.data.*
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.{FilenameDefault, ServerPrintUtil, ScoreToHtmlRenderer}
import javafx.collections.ObservableList
import javafx.scene.text.FontSmoothingType
import org.controlsfx.control.CheckComboBox
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.subscriptions.Subscription
import scalafx.geometry.Insets
import scalafx.print.{PageOrientation, Printer}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.util.StringConverter

import java.io.*
import java.util.concurrent.{ScheduledFuture, TimeUnit}
import scala.concurrent.Promise
import scala.language.implicitConversions
import ch.seidel.kutu.renderer.ServerPrintUtil.*

abstract class DefaultRanglisteTab(wettkampfmode: BooleanProperty, override val service: KutuService) extends Tab with TabWithService with ScoreToHtmlRenderer {

  override val title = ""
  var subscription: List[Subscription] = List.empty

  var lastScoreDef = new ObjectProperty[Option[FilterBy]]()
  lastScoreDef.setValue(None)

  var lastPublishedScoreView = new ObjectProperty[Option[PublishedScoreView]]()
  lastPublishedScoreView.setValue(None)

  override def release: Unit = {
    subscription.foreach(_.cancel())
    subscription = List.empty
  }

  private var lazyPaneUpdater: Map[String, ScheduledFuture[?]] = Map.empty

  def submitLazy(name: String, task: () => Unit, delay: Long): Unit = {
    lazyPaneUpdater.get(name).foreach(_.cancel(true))
    val ft = KuTuApp.lazyExecutor.schedule(new Runnable() {
      override def run(): Unit = {
        Platform.runLater {
          task()
        }
      }
    }, delay, TimeUnit.SECONDS)

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

  def getPublishedScores: List[PublishedScoreView] = List.empty

  def getActionButtons: List[Button] = List.empty

  private val webView = new WebView {
    fontSmoothingType = FontSmoothingType.GRAY

    import ch.seidel.javafx.webview.HyperLinkRedirectListener

    engine.getLoadWorker.stateProperty.addListener(new HyperLinkRedirectListener(this.delegate))
    //zoom = 1.2
    //fontScale = 1.2
  }

  class DataObjectConverter extends StringConverter[DataObject] {
    def fromString(text: String): DataObject = {
      nullFilter
    }

    def toString(d: DataObject): String = if d != null then d.easyprint else ""
  }

  val converter = new DataObjectConverter()

  private var restoring = false
  private val nullFilter = NullObject("alle")
  val cbAvg: CheckBox = new CheckBox {
    text = "Durchschn. Punkte bei mehreren Wettkämpfen"
    selected = true
  }
  private val bestNModel: ObservableBuffer[ScoreListBestN] = ObservableBuffer.from(Seq[ScoreListBestN](
    AlleWertungen,
    BestNWertungen(1),
    BestNWertungen(2),
    BestNWertungen(3),
    BestNWertungen(4),
    BestNWertungen(5),
  ))

  private val cbBestN: ComboBox[ScoreListBestN] = new ComboBox[ScoreListBestN] {
    items = bestNModel
    selectionModel.value.select(AlleWertungen)
  }

  private val cbfSaved = new MenuButton("Gespeicherte Einstellungen") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
  }

  def print(printer: Printer): Unit = {
    PrintUtil.printWebContent(webView.engine, printer, PageOrientation.Portrait)
  }

  def resetFilterPresets(combos: Seq[ComboBox[FilterBy]], scorelistKind: ScoreListKind, counting: ScoreListBestN): Unit = {

  }

  def populate(groupers: List[FilterBy]): Seq[ComboBox[FilterBy]] = {
    val gr1Model = ObservableBuffer.from(groupers)
    val kindModel = ObservableBuffer.from(Seq[ScoreListKind](Einzelrangliste, Teamrangliste, Kombirangliste))

    val grf1Model = ObservableBuffer.empty[DataObject]
    val grf2Model = ObservableBuffer.empty[DataObject]
    val grf3Model = ObservableBuffer.empty[DataObject]
    val grf4Model = ObservableBuffer.empty[DataObject]

    val cbKind = new ComboBox[ScoreListKind] {
      promptText = "Rangliste-Typ"
      items = kindModel
      value.value = groupers.head.getKind
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
      if !cb.selectionModel.value.isEmpty then {
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
      val cblist = combos.filter { cbp =>
        val (cb, cf) = cbp
        val ret = relevantGroup(cb)
        cf.setDisable(!ret)
        ret
      }.map { cbp =>
        val (cb, cf) = cbp
        val grp = cb.selectionModel.value.getSelectedItem

        if cf.getCheckModel.getCheckedItems.isEmpty || cf.getCheckModel.getCheckedItems.forall(nullFilter.equals(_)) then {
          grp.reset
        }
        else {
          grp.setFilter(cf.getCheckModel.getCheckedItems.toSet[DataObject])
        }
        grp
      }
      val akg = groupers.find(p => p.isInstanceOf[ByAltersklasse] && p.groupname.startsWith("Wettkampf"))
      val jakg = groupers.find(p => p.isInstanceOf[ByJahrgangsAltersklasse] && p.groupname.startsWith("Wettkampf"))
      val groupBy = if cblist.nonEmpty then {
        cblist.foldLeft(cblist.head.asInstanceOf[GroupBy])((acc, cb) => if acc != cb then acc.groupBy(cb) else acc)
      } else if akg.nonEmpty then {
        ByProgramm().groupBy(akg.get).groupBy(ByGeschlecht())
      } else if jakg.nonEmpty then {
        ByProgramm().groupBy(jakg.get).groupBy(ByGeschlecht())
      }
      else {
        ByProgramm().groupBy(ByGeschlecht())
      }
      groupBy.setAlphanumericOrdered(cbModus.selected.value)
      groupBy.setAvgOnMultipleCompetitions(cbAvg.selected.value)
      groupBy.setBestNCounting(cbBestN.value.value)
      groupBy.setKind(cbKind.value.value)
      restoring = false

      groupBy
    }

    def refreshRangliste(query: GroupBy, linesPerPage: Int = 0) = {
      restoring = true
      val data = getData
      val filter = query.asInstanceOf[FilterBy]
      val filterLists = filter.traverse(Seq[Seq[DataObject]]()) { (f, acc) =>
        val allItems = f.asInstanceOf[FilterBy].analyze(data).sortBy { x => x.easyprint }
        acc :+ (if f.canSkipGrouper then nullFilter +: allItems else allItems)
      }
      combfs.filter(cmb => cmb.disabled.value).foreach(cmb => {
        cmb.getCheckModel.clearChecks()
        cmb.getItems.clear()
      })
      filterLists.zip(fmodels.zip(combos).filter { x => relevantGroup(x._2._1) }.map(x => (x._1, x._2._2))).foreach { x =>
        val (expected, (model, combf)) = x
        val checked = combf.getCheckModel.getCheckedItems.toSet
        combf.getCheckModel.clearChecks()
        model.retainAll(expected)
        model.insertAll(model.size, expected.filter(!model.contains(_)))
        model.sort { (a, b) => a.compareTo(b) < 0 }

        checked.filter(model.contains(_)).foreach(combf.getCheckModel.check(_))
      }
      query.setAlphanumericOrdered(cbModus.selected.value)
      query.setAvgOnMultipleCompetitions(cbAvg.selected.value)
      query.setBestNCounting(cbBestN.value.value)
      query.setKind(cbKind.value.value)

      val combination = query.select(data).toList
      lastScoreDef.setValue(Some(query.asInstanceOf[FilterBy]))

      val logofile = ServerPrintUtil.locateLogoFile(getSaveAsFilenameDefault.dir)
      val ret = toHTML(combination, linesPerPage, query.isAlphanumericOrdered, query.isAvgOnMultipleCompetitions, logofile)
      if linesPerPage == 0 then {
        webView.engine.loadContent(ret)
      }
      restoring = false
      ret
    }

    def restoreGrouper(query: GroupBy): Unit = {
      restoring = true

      combos.foreach { cb =>
        val (cmb, cmbf) = cb
        cmb.selectionModel.value.clearSelection()
        cmbf.getCheckModel.clearChecks()
        cmbf.getItems.clear()
        cmbf.setDisable(true)
      }

      query.traverse(combos) { (grp, acc) =>
        logger.debug(grp.toString)
        if acc.isEmpty then {
          acc
        }
        else {
          val (cmb, cmbf) = acc.head

          cmb.selectionModel.value.select(grp.asInstanceOf[FilterBy])
          cmbf.setDisable(false)
          val expected: List[DataObject] = grp.asInstanceOf[FilterBy].filterItems
            .sortWith { case (a, b) => a.easyprint.compareTo(b.easyprint) < 0 }

          expected.foreach(item => cmbf.getItems.add(item))
          grp.asInstanceOf[FilterBy].getFilter.foreach(cmbf.getCheckModel.check(_))

          acc.tail
        }
      }
      cbModus.selected.value = query.isAlphanumericOrdered
      cbAvg.selected.value = query.isAvgOnMultipleCompetitions
      cbKind.value.value = query.getKind
      cbBestN.value = query.getBestNCounting
      restoring = false
      refreshRangliste(query)
    }

    combos.foreach { case (comb, combfs) =>
      comb.onAction = _ => {
        if !restoring then {
          restoring = true
          combfs.getCheckModel.clearChecks()
          combfs.getItems.clear()
          refreshRangliste(buildGrouper)
        }
      }
      combfs.getCheckModel.getCheckedItems.onChange((b, s) => {
        if !restoring then {
          refreshRangliste(buildGrouper)
        }
      })
    }

    cbModus.onAction = _ => {
      if !restoring then
        refreshRangliste(buildGrouper)
    }
    cbAvg.onAction = _ => {
      if !restoring then
        refreshRangliste(buildGrouper)
    }
    cbBestN.onAction = _ => {
      if !restoring then
        refreshRangliste(buildGrouper)
    }
    cbBestN.disable <== createBooleanBinding(() => cbKind.value.value == Teamrangliste, cbKind.value)
    cbKind.onAction = _ => {
      if !restoring then {
        restoring = true
        if cbKind.value.value == Teamrangliste then {
          cbBestN.value.value = AlleWertungen
        }
        resetFilterPresets(combs, cbKind.value.value, cbBestN.value.value)
        refreshRangliste(buildGrouper)
      }
    }


    val btnPrint = PrintUtil.btnPrintFuture(text.value, getSaveAsFilenameDefault, true,
      (lpp: Int) => {
        val retProm = Promise[String]()
        if Platform.isFxApplicationThread then {
          retProm.success(refreshRangliste(buildGrouper, lpp))
        } else Platform.runLater(() => {
          retProm.success(refreshRangliste(buildGrouper, lpp))
        })
        retProm.future
      }
    )

    def extractFilterText = normalizeFilterText(buildGrouper.toRestQuery)

    def getFilterSaveAsFilenameDefault: FilenameDefault = {
      val default = getSaveAsFilenameDefault
      FilenameDefault(extractFilterText + ".scoredef", default.dir)
    }

    def loadFilter(selectedFile: File): Unit = {
      val ios = new ObjectInputStream(new FileInputStream(selectedFile))
      val grouper = GroupBy(ios.readObject().toString, getData, groupers)
      restoreGrouper(grouper)
    }

    def listFilter = {
      getFilterSaveAsFilenameDefault.dir match {
        case null => List.empty
        case dir if !dir.exists() => List.empty
        case dir if !dir.isDirectory => List.empty
        case dir if dir.listFiles().isEmpty => List.empty
        case dir => dir.listFiles()
          .filter(f => f != null && f.isFile && f.getName.endsWith(".scoredef"))
          .toList
          .sortBy {
            _.getName
          }
      }
    }

    def addPredefinedFilter(items: ObservableList[javafx.scene.control.MenuItem])(filter: File): Unit = {
      val menu = KuTuApp.makeMenuAction(filter.getName
        .replace(".scoredef", "")
        .replace("-", " ")
        .capitalize) { (caption, action) =>
        lastPublishedScoreView.setValue(None)
        loadFilter(filter)
      }
      if !items.exists(m => m.text.value != null && m.text.value.equalsIgnoreCase(menu.text.value)) then {
        items.add(menu)
      }
    }

    def addPublishedFilter(items: ObservableList[javafx.scene.control.MenuItem])(filter: PublishedScoreView): Unit = {
      val menu = KuTuApp.makeMenuAction(toMenuText(filter)) { (caption, action) =>
        val grouper = GroupBy(filter.query, getData, groupers)
        restoreGrouper(grouper)
        lastPublishedScoreView.setValue(Some(filter))
      }
      items.add(menu)
    }

    def refreshScorePresets(items: ObservableList[javafx.scene.control.MenuItem]): Unit = {
      items.clear()
      val addPublished: PublishedScoreView => Unit = addPublishedFilter(items)
      val addSaved: File => Unit = addPredefinedFilter(items)
      getPublishedScores.foreach(addPublished(_))
      if items.nonEmpty then {
        items.add(new SeparatorMenuItem())
      }
      listFilter.foreach(addSaved(_))
    }

    val btnSaveFilter = new Button {
      text = "Einstellung speichern als ..."
      visible <== when(wettkampfmode) choose false otherwise true
      onAction = _ => {
        val defaults = getFilterSaveAsFilenameDefault
        val filename = defaults.filename
        val dir = defaults.dir
        if !dir.exists() then {
          dir.mkdirs()
        }
        val fileChooser = new FileChooser() {
          initialDirectory = dir
          this.title = "Filtereinstellung speichern ..."
          extensionFilters.addAll(
            new ExtensionFilter("Filtereinstellung", "*.scoredef"))
          initialFileName = filename
        }
        val selectedFile = fileChooser.showSaveDialog(KuTuApp.getStage)
        if selectedFile != null then {
          val file = if !selectedFile.getName.endsWith(".scoredef") then {
            new java.io.File(selectedFile.getAbsolutePath + ".scoredef")
          }
          else {
            selectedFile
          }
          val os = new ObjectOutputStream(new FileOutputStream(file))
          val query = buildGrouper.toRestQuery
          os.writeObject(query)
          os.flush()
          os.close()
          addPredefinedFilter(cbfSaved.items)(file)
        }
      }
    }
    lastPublishedScoreView.onChange {
      lastPublishedScoreView.value match {
        case Some(filter) =>
          cbfSaved.text = toMenuText(filter)
        case _ =>
          cbfSaved.text = "Gespeicherte Einstellungen"
      }
    }

    onSelectionChanged = _ => {
      if selected.value then {
        refreshRangliste(buildGrouper)
        refreshScorePresets(cbfSaved.items)
      }
    }

    if logger.isDebugEnabled() then {
      logger.debug("subscribing for refreshing from websocket")
    }

    def updateFromWebsocket(newItem: KutuAppEvent): Unit = {
      submitLazy("refreshRangliste", () => if selected.value then {
        if logger.isDebugEnabled() then {
          logger.debug("refreshing rangliste from websocket", newItem)
        }
        refreshRangliste(buildGrouper)
        refreshScorePresets(cbfSaved.items)
      }, 5)
    }

    subscription = subscription :+ WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
      if selected.value then {
        newItem match {
          case LastResults(_) => updateFromWebsocket(newItem)
          case _: AthletWertungUpdated => updateFromWebsocket(newItem)
          case _: AthletWertungUpdatedSequenced => updateFromWebsocket(newItem)
          case _ =>
        }
      }
    }

    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      //              children = new Label("Filter:") :+ combfs
      val label: Label = new Label("Gruppierungen:") {
        padding = Insets(7, 0, 0, 0)
      }
      val labelfilter: Label = new Label("Filter:") {
        padding = Insets(7, 0, 0, 0)
      }
      val topBox: VBox = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = List(label, labelfilter)
      }
      val topCombos: List[VBox] = combos.map { ccs =>
        new VBox {
          vgrow = Priority.Always
          hgrow = Priority.Always
          val filterControl: Control = ccs._2
          children = List(ccs._1, filterControl)
        }
      }

      top = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        buildToolbars()
        wettkampfmode.onChange {
          buildToolbars()
        }

        private def buildToolbars(): Unit = {
          if wettkampfmode.value then {
            children = List(
              new ToolBar {
                content = List(cbfSaved) ++ getActionButtons ++ List(btnPrint)
              },
              new ToolBar {
                content = List(cbKind, cbBestN, cbModus, cbAvg)
              },
              new ToolBar {
                content = topBox +: topCombos
              })
          } else {
            children = List(
              new ToolBar {
                content = List(cbfSaved, btnSaveFilter) ++ getActionButtons ++ List(btnPrint)
              },
              new ToolBar {
                content = List(cbKind, cbBestN, cbModus, cbAvg)
              },
              new ToolBar {
                content = topBox +: topCombos
              })
          }
        }
      }
      center = webView
    }
    combs
  }

  private def toMenuText(filter: PublishedScoreView) = {
    (if filter.published then "Publiziert: " else "Bereitgestellt: ") + filter.title
  }

  def normalizeFilterText(text: String): String = encodeFileName(
    text
      .replace("groupby", "Gruppiert")
      .replace("filter", "Gefiltert")
      .replace("alphanumeric", "Sortierung_nach_Namen")
      .replace("avg=true", "Durchschnittwertung")
      .replace("avg=false", "Summenwertung")
      .replace("kind=", "")
      .replace("counting", "Zaehlend")
      .replace("/", "_")
      .replace(".", "_")
      .replace("?", "_")
      .replace("&", "+")
      .replace("=", "-")
      .replace(":", "-")
      .replace("!", "-")
  )

}
