package ch.seidel.kutu.view

import java.util.UUID

import ch.seidel.commons.{DisplayablePage, LazyTabPane, PageDisplayer, TabWithService}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.KuTuApp.enc
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{BestenListeToHtmlRenderer, PrintUtil, RiegenBuilder}
import ch.seidel.kutu.{Config, ConnectionStates, KuTuApp, KuTuServer}
import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.TreeTableColumn._
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, Priority, VBox}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait DurchgangItem

case class DurchgangState(wettkampfUUID: String, name: String, complete: Boolean, geraeteRiegen: List[GeraeteRiege], durchgang: Durchgang) extends DurchgangItem {
  val started: Long = durchgang.effectiveStartTime.map(_.getTime).getOrElse(0)
  val finished: Long = durchgang.effectiveEndTime.map(_.getTime).getOrElse(0)

  def update(newGeraeteRiegen: List[GeraeteRiege], dg: Durchgang) = {
    if (this.durchgang == dg && newGeraeteRiegen.forall(gr => geraeteRiegen.contains(gr))) this
    else DurchgangState(wettkampfUUID, name, complete, newGeraeteRiegen, dg)
  }

  def ~(other: DurchgangState) = name == other.name && geraeteRiegen != other.geraeteRiegen

  val updated = System.currentTimeMillis()
  val isRunning = started > 0L && finished < started
  lazy val statsBase = geraeteRiegen.groupBy(_.disziplin).map(_._2.map(_.kandidaten.size).sum)
  lazy val statsCompletedBase = geraeteRiegen.groupBy(gr => gr.disziplin).map { gr =>
    val (disziplin, grd) = gr

    def hasWertungInDisciplin(wertungen: Seq[WertungView]) = wertungen.filter(w => disziplin.contains(w.wettkampfdisziplin.disziplin)).exists(_.endnote.nonEmpty)

    val grdStats = grd.groupBy(grds => grds.halt).map { grdsh =>
      val totalCnt = grdsh._2.map(_.kandidaten.size).sum
      val completedCnt = grdsh._2.map(_.kandidaten.count(k => hasWertungInDisciplin(k.wertungen))).sum
      (grdsh._1, 100 * completedCnt / totalCnt, completedCnt, totalCnt)
    }.toList.sortBy(grdsh => grdsh._1)

    val totalCnt = gr._2.map(_.kandidaten.size).sum
    val completedCnt = gr._2.map(_.kandidaten.count(k => hasWertungInDisciplin(k.wertungen))).sum
    // (geraet, complete%, completeCnt, totalCnt, haltStats(halt, complete%, completeCnt, totalCnt))
    (disziplin, 100 * completedCnt / gr._2.map(_.kandidaten.size).sum, completedCnt, totalCnt, grdStats)
  }

  lazy val anzValue = statsBase.sum

  lazy val anz = s"${anzValue}"
  lazy val min = s"${statsCompletedBase.map(_._2).min}%"
  lazy val max = s"${statsCompletedBase.map(_._2).max}%"
  lazy val avg = s"${100 * statsCompletedBase.map(_._3).sum / anzValue}%"

  lazy val percentPerRiegeComplete = statsCompletedBase
    .map(gr => (gr._1 -> (s"${gr._2}%", gr._5.map(grh => s"Station ${grh._1 + 1}: ${grh._2}%").mkString("\n")))).toMap

  lazy val totalTime = if (started > 0) {
    if (finished > started) finished - started else updated - started
  } else 0
  //  lazy val minTime = s"${statsBase.min}"
  //  lazy val maxTime = s"${statsBase.max}"
  //  lazy val avgTime = s"${anzValue / statsBase.size}"
}

trait DurchgangStationTCAccess extends TCAccess[DurchgangState, Object, Disziplin] {
  def getDisziplin = getIndex
}

class DurchgangStationJFSCTreeTableColumn[T](val index: Disziplin) extends jfxsc.TreeTableColumn[DurchgangState, T] with DurchgangStationTCAccess {
  override def getIndex: Disziplin = index

  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}

class DurchgangStationTreeTableColumn[T](val index: Disziplin) extends TreeTableColumn[DurchgangState, T] with DurchgangStationTCAccess {
  override val delegate: jfxsc.TreeTableColumn[DurchgangState, T] = new DurchgangStationJFSCTreeTableColumn[T](index)

  override def getIndex: Disziplin = index

  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}

class DurchgangStationView(wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], durchgangModel: ObservableBuffer[TreeItem[DurchgangState]]) extends TreeTableView[DurchgangState] {

  id = "durchgang-table"
  //items = durchgangModel
  showRoot = false
  tableMenuButtonVisible = true

  root = new TreeItem[DurchgangState]() {
    durchgangModel.onChange {
      children = durchgangModel
    }
    styleClass.add("parentrow")
    expanded = true
  }

  var okIcon: Image = null
  try {
    okIcon = new Image(getClass().getResourceAsStream("/images/GreenOk.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  columns ++= List(
    new TreeTableColumn[DurchgangState, String] {
      prefWidth = 130
      text = "Durchgang"
      cellValueFactory = { x => StringProperty(if (x.value.getValue != null) x.value.getValue.name else "Durchgänge") }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Start"
      cellValueFactory = { x => StringProperty(if (x.value.getValue == null) "" else toTimeFormat(x.value.getValue.started)) }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Ende"
      cellValueFactory = { x => StringProperty(if (x.value.getValue == null) "" else toTimeFormat(x.value.getValue.finished)) }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Dauer"
      cellValueFactory = { x => StringProperty(if (x.value.getValue == null) "" else toDurationFormat(x.value.getValue.started, x.value.getValue.finished))
      }
    }
    //    , new TreeTableColumn[DurchgangState, String] {
    //      prefWidth = 40
    //      text = "Anzahl"
    //      cellValueFactory = { x => StringProperty(x.value.getValueanz) }
    //    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 110
      text = "Plandauer"
      cellValueFactory = { x =>
        StringProperty(if (x.value.getValue == null) "" else
          s"""Tot:     ${toDurationFormat(x.value.getValue.durchgang.planTotal)}
             |Eint.:    ${toDurationFormat(x.value.getValue.durchgang.planEinturnen)}
             |Gerät.: ${toDurationFormat(x.value.getValue.durchgang.planGeraet)}""".stripMargin)
      }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 80
      text = "Fertig"
      cellFactory = { _ =>
        new TreeTableCell[DurchgangState, String] {
          val image = new ImageView()
          graphic = image
          item.onChange { (_, _, newValue) =>
            text = newValue
            image.image = toIcon(newValue)
          }
        }
      }
      cellValueFactory = { x => StringProperty(if (x.value.getValue == null) "" else x.value.getValue.avg) }
    }
  )

  columns ++= disziplinlist().map { disziplin =>
    val dc = new TreeTableColumn[DurchgangState, String] {
      text = disziplin.name
      prefWidth = 230
      columns ++= Seq(
        new DurchgangStationTreeTableColumn[String](disziplin) {
          text = "Stationen"
          prefWidth = 120
          cellValueFactory = { x =>
            if (x.value.getValue == null) StringProperty("") else
              x.value.getValue.percentPerRiegeComplete.get(Some(disziplin)) match {
                case Some(re) => StringProperty(re._2)
                case _ => StringProperty("")
              }
          }
        }
        , new TreeTableColumn[DurchgangState, String] {
          text = "Fertig"
          prefWidth = 80
          cellFactory = { _ =>
            new TreeTableCell[DurchgangState, String] {
              val image = new ImageView()
              graphic = image
              item.onChange { (_, _, newValue) =>
                text = newValue
                image.image = toIcon(newValue)
              }
            }
          }
          cellValueFactory = { x =>
            if (x.value.getValue == null) StringProperty("") else
              x.value.getValue.percentPerRiegeComplete.get(Some(disziplin)) match {
                case Some(re) => StringProperty(re._1)
                case _ => StringProperty("0")
              }
          }
        }
      )
    }
    TreeTableColumn.sfxTreeTableColumn2jfx(dc)
  }

  private def toIcon(newValue: String) = {
    newValue match {
      case "100%" => okIcon
      case "0%" => null
      case "0" => null
      case "" => null
      case null => null
      case _ => nokIcon
    }
  }
}

class NetworkTab(wettkampfmode: BooleanProperty, override val wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService with ExportFunctions {

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  closable = false
  text = "Netzwerk-Dashboard"

  lazy val disziplinlist = service.listDisziplinesZuWettkampf(wettkampf.id)

  def loadDurchgaenge = {
    val durchgaenge = service.selectDurchgaenge(wettkampf.uuid.map(UUID.fromString(_)).get).map(d => d.name -> d).toMap

    RiegenBuilder.mapToGeraeteRiegen(service.getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get)).toList)
      .filter(gr => gr.durchgang.nonEmpty)
      .groupBy(gr => gr.durchgang.get)
      .map { t =>
        DurchgangState(wettkampf.uuid.getOrElse(""), t._1, t._2.forall { riege => riege.erfasst }, t._2, durchgaenge(t._1))
      }
      .toList.sortBy(_.name)
  }

  val model = ObservableBuffer[TreeItem[DurchgangState]]()

  val isRunning = BooleanProperty(false)

  def refreshData(event: Option[KutuAppEvent] = None) {
    val expandedStates = model
      .filter(_.isExpanded)
      .map(_.value.value.durchgang.title)
      .toSet
    val newList: immutable.Seq[DurchgangState] = loadDurchgaenge
    model.clear()

    val groupMap = newList.groupBy(d => d.durchgang.title)
    for (group <- groupMap.keySet.toList.sorted) {
      val dgList = groupMap(group).sortBy(_.name)
      if (dgList.size > 1 || dgList.head.name != dgList.head.durchgang.title) {
        val dgh = dgList.foldLeft(Durchgang())((acc, dg) => {
          if (acc.planTotal > dg.durchgang.planTotal) acc else dg.durchgang
        })
        val groupDurchgangState = DurchgangState(
          wettkampfUUID = dgList.head.wettkampfUUID,
          name = dgh.title,
          complete = dgList.forall(_.complete),
          geraeteRiegen = dgList.flatMap(_.geraeteRiegen).toList,
          durchgang = dgh)
        model.add(new TreeItem[DurchgangState](groupDurchgangState) {
          for (d <- dgList) {
            children.add(new TreeItem[DurchgangState](d))
          }
          expanded = expandedStates.contains(group)
        })
      } else {
        model.add(new TreeItem[DurchgangState](dgList.head) {
          expanded = false
        })
      }
    }
    isRunning.set(!model.forall(!_.getValue.isRunning))
    updateButtons
  }

  var subscriptions: List[Subscription] = List.empty

  override def release {
    subscription.cancel()
    subscriptions.foreach(_.cancel)
    subscriptions = List.empty
  }

  onSelectionChanged = handle {
    if (selected.value) {
      refreshData()
    }
  }

  def uploadResults(caption: String) {
    import scala.concurrent.ExecutionContext.Implicits.global
    val process = KuTuApp.invokeAsyncWithBusyIndicator {
      if (remoteBaseUrl.indexOf("localhost") > -1) {
        KuTuServer.startServer { uuid => KuTuServer.sha256(uuid) }
      }
      KuTuServer.httpUploadWettkampfRequest(wettkampf.toWettkampf)
    }
    process.onComplete { resultTry =>
      Platform.runLater {
        val feedback = resultTry match {
          case Success(response) =>
            KuTuApp.selectedWettkampfSecret.value = wettkampf.toWettkampf.readSecret(homedir, remoteHostOrigin)
            s"Der Wettkampf ${wettkampf.easyprint} wurde erfolgreich im Netzwerk bereitgestellt"
          case Failure(error) => error.getMessage.replace("(", "(\n")
        }
        PageDisplayer.showInDialogFromRoot(caption, new DisplayablePage() {
          def getPage: Node = {
            new BorderPane {
              hgrow = Priority.Always
              vgrow = Priority.Always
              center = new VBox {
                children.addAll(new Label(feedback))
              }
            }
          }
        })
      }
    }
  }

  val view = new DurchgangStationView(
    wettkampf, service,
    () => {
      disziplinlist
    },
    model)

  type MenuActionHandler = (String, ActionEvent) => Unit

  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R) = new javafx.event.EventHandler[J] {
    def handle(event: J) {
      handler(event)
    }
  }

  def makeMenuAction(caption: String)(handler: MenuActionHandler): MenuItem = {
    new MenuItem(caption) {
      onAction = handleAction { e: ActionEvent =>
        handler(caption, e)
      }
    }
  }

  def getSelectedDruchgangStates: List[DurchgangState] = {
    view.selectionModel().selectedItems.flatMap{
      case treeItem if (treeItem.getChildren.nonEmpty) =>
        treeItem.getChildren.toList.map(_.getValue)
      case treeItem if (treeItem.getChildren.isEmpty) =>
        List(treeItem.getValue)
    }.toList
  }

  def makeDurchgangStartenMenu(p: WettkampfView): MenuItem = {

    val item = makeMenuAction("Durchgang starten") { (_, _) =>
      getSelectedDruchgangStates.foreach { d =>
        KuTuApp.invokeWithBusyIndicator {
          Await.result(KuTuServer.startDurchgang(p, d.name), Duration.Inf)
        }
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse(""))
        || getSelectedDruchgangStates.forall{ case d => d.started > 0 && d.finished == 0},
      view.selectionModel().selectedItem,
      isRunning,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  def makeDurchgangAbschliessenMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Durchgang abschliessen") { (_, _) =>
      getSelectedDruchgangStates.foreach { d =>
        KuTuApp.invokeWithBusyIndicator {
          Await.result(KuTuServer.finishDurchgang(p, d.name), Duration.Inf)
        }
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse(""))
        || getSelectedDruchgangStates.forall{ case d => !(d.started > 0 && d.finished == 0)},
      view.selectionModel().selectedItem,
      isRunning,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  var okIcon: Image = null
  try {
    okIcon = new Image(getClass().getResourceAsStream("/images/GreenOk.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  def makeSelectedRiegenBlaetterExport(): Menu = {
    val option = getSelectedDruchgangStates
    val selectedDurchgaenge = option.toSet.map((_: DurchgangState).name)
    new Menu {
      text = "Riegenblätter nachdrucken"
      updateItems
      reprintItems.onChange {
        updateItems
      }

      private def updateItems = {
        items.clear()
        val affectedDurchgaenge: Set[String] = reprintItems.get.map(_.durchgang)
        if (selectedDurchgaenge.nonEmpty) {
          items += KuTuApp.makeMenuAction(s"Alle Stationen im Durchgang") { (caption: String, action: ActionEvent) =>
            doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge)(action)
          }
          items += KuTuApp.makeMenuAction(s"Nur 1. Station pro Gerät im Durchgang") { (caption: String, action: ActionEvent) =>
            doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge, Set(0))(action)
          }
          items += KuTuApp.makeMenuAction(s"Alle ab 2. Station pro Gerät im Durchgang") { (caption: String, action: ActionEvent) =>
            doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge, Set(-1))(action)
          }
        }
        if (affectedDurchgaenge.nonEmpty && selectedDurchgaenge.nonEmpty) {
          items += new SeparatorMenuItem()
        }
        if (affectedDurchgaenge.nonEmpty) {
          val allItem = KuTuApp.makeMenuAction(s"Alle betroffenen (${affectedDurchgaenge.size})") { (caption: String, action: ActionEvent) =>
            doSelectedRiegenBelatterExport(text.value, affectedDurchgaenge)(action)
          }
          items += allItem
          items += new SeparatorMenuItem()
          affectedDurchgaenge.toList.sorted.foreach { durchgang =>
            items += KuTuApp.makeMenuAction(s"${durchgang}") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, Set(durchgang))(action)
            }
          }
        }
        disable.value = items.isEmpty
      }
    }
  }

  def makeNavigateToMenu(p: WettkampfView): Menu = {
    new Menu("Gehe zu Riege ...") {
      def addRiegenMenuItems(durchgang: DurchgangState, column: DurchgangStationTCAccess) = {
        val disziplin = column.getDisziplin
        val selection = durchgang.geraeteRiegen.filter {
          _.disziplin.contains(disziplin)
        }
        items = selection.map { r =>
          val menu = KuTuApp.makeMenuAction(r.caption) { (caption, action) =>
            lazypane match {
              case Some(pane) =>
                val wertungTab: WettkampfWertungTab = new WettkampfWertungTab(wettkampfmode, None, Some(r), wettkampf, service, {
                  val progs = service.readWettkampfLeafs(wettkampf.programm.id)
                  service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
                }) {
                  text = r.caption
                  closable = true
                }
                pane.tabs.add(pane.tabs.size() + (if (wettkampfmode.value) -2 else -1), wertungTab.asInstanceOf[Tab])
                pane.selectionModel.value.select(wertungTab)
                wertungTab.tabPaneProperty.onChange {
                  if (wertungTab.getTabPane == null) {
                    wertungTab.release
                  }
                }
                wertungTab.populated
              case _ =>
            }
          }
          menu.graphic = new ImageView {
            image = if (r.erfasst) okIcon else nokIcon
          }
          menu
        }
      }

      items.clear
      view.selectionModel.value.selectedCells.toList.headOption match {
        case None =>
        case Some(cell) =>
          cell.getTableColumn match {
            case column: DurchgangStationTCAccess =>
              addRiegenMenuItems(cell.treeItem.getValue, column)
            case _ => if (cell.getTableColumn.parentColumn.value != null && cell.getTableColumn.parentColumn.value.columns.size == 2) {
              val col = cell.getTableColumn.getParentColumn().getColumns().head
              col match {
                case column: DurchgangStationJFSCTreeTableColumn[_] =>
                  addRiegenMenuItems(cell.treeItem.getValue, column)
                case column: DurchgangStationTCAccess =>
                  addRiegenMenuItems(cell.treeItem.getValue, column)
                case _ =>
              }
            }
          }
      }
      disable = items.size() == 0
    }
  }

  val qrcodeMenu: MenuItem = KuTuApp.makeShowQRCodeMenu(wettkampf)
  val connectAndShareMenu = KuTuApp.makeConnectAndShareMenu(wettkampf)

  val uploadMenu: MenuItem = {
    val item = makeMenuAction("Upload") { (caption, action) =>
      implicit val e = action
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              if (wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin)) {
                children.addAll(new Label("Die Resultate zu diesem Wettkampf werden im Netzwerk hochgeladen und\nersetzen dort die Resultate, die zu diesem Wettkampf erfasst wurden."))
              } else {
                children.addAll(new Label("Die Resultate zu diesem Wettkampf werden neu im Netzwerk bereitgestellt."))
              }
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { implicit e: ActionEvent =>
            uploadResults(caption)
          }
        })
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      Config.isLocalHostServer()
        || (wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !ConnectionStates.connectedProperty.value)
        || isRunning.value,

      KuTuApp.selectedWettkampfSecret,
      isRunning,
      ConnectionStates.connectedProperty,
    )) choose true otherwise false
    item
  }

  val downloadMenu: MenuItem = KuTuApp.makeWettkampfDownloadMenu(wettkampf)

  val disconnectMenu = KuTuApp.makeDisconnectMenu(wettkampf)
  val removeRemoteMenu = KuTuApp.makeWettkampfRemoteRemoveMenu(wettkampf)

  val generateBestenliste = new Button with BestenListeToHtmlRenderer {
    text = "Bestenliste erstellen"
    minWidth = 75
    disable.value = wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    onAction = (event: ActionEvent) => {
      if (!WebSocketClient.isConnected) {
        val filename = "Bestenliste_" + wettkampf.easyprint.replace(" ", "_") + ".html"
        val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
        if (!dir.exists()) {
          dir.mkdirs()
        }
        val logofile = PrintUtil.locateLogoFile(dir)

        def generate(lpp: Int) = toHTMListe(WertungServiceBestenResult.getBestenResults, logofile)

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
      } else {
        Await.result(KuTuServer.finishDurchgangStep(wettkampf), Duration.Inf)
        val topResults = s"${Config.remoteBaseUrl}/?" + new String(enc.encodeToString((s"top&c=${wettkampf.uuid.get}").getBytes))
        KuTuApp.hostServices.showDocument(topResults)
      }
      WertungServiceBestenResult.resetBestenResults
    }
  }

  view.contextMenu = new ContextMenu() {
    items += makeDurchgangStartenMenu(wettkampf)
    items += new SeparatorMenuItem()
    items += makeMenuAction("Bestenliste erstellen") { (_, action) =>
      generateBestenliste.onAction.value.handle(action)
    }
    items += new SeparatorMenuItem()
    items += makeDurchgangAbschliessenMenu(wettkampf)
  }
  val toolbar = new ToolBar {
  }
  val rootpane = new BorderPane {
    hgrow = Priority.Always
    vgrow = Priority.Always
    top = toolbar
    center = view
  }
  val btnEditRiege = new MenuButton("Gehe zu ...") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
  }
  val btnDurchgang = new MenuButton("Durchgang ...") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
  }

  def updateButtons {
    val navigate = makeNavigateToMenu(wettkampf)
    btnEditRiege.items.clear()
    btnEditRiege.items.addAll(navigate.items)

    if (!wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)) {
      btnDurchgang.items.clear()
      btnDurchgang.items += makeDurchgangStartenMenu(wettkampf)
      btnDurchgang.items += new SeparatorMenuItem()
      btnDurchgang.items += makeSelectedRiegenBlaetterExport()
      btnDurchgang.items += makeMenuAction("Bestenliste erstellen") { (_, action) =>
        generateBestenliste.onAction.value.handle(action)
      }
      btnDurchgang.items += new SeparatorMenuItem()
      btnDurchgang.items += makeDurchgangAbschliessenMenu(wettkampf)

      view.contextMenu = new ContextMenu() {
        items += makeDurchgangStartenMenu(wettkampf)
        items += new SeparatorMenuItem()
        items += makeSelectedRiegenBlaetterExport()
        items += navigate
        items += makeMenuAction("Bestenliste erstellen") { (_, action) =>
          generateBestenliste.onAction.value.handle(action)
        }
        items += new SeparatorMenuItem()
        items += makeDurchgangAbschliessenMenu(wettkampf)
      }
      toolbar.content = List(
        new Button {
          onAction = connectAndShareMenu.onAction.get
          text <== connectAndShareMenu.text
          disable <== connectAndShareMenu.disable
        }, new Button {
          onAction = qrcodeMenu.onAction.get
          text <== qrcodeMenu.text
          disable <== qrcodeMenu.disable
        }, btnDurchgang, btnEditRiege, new Button {
          onAction = uploadMenu.onAction.get
          text <== uploadMenu.text
          disable <== uploadMenu.disable
          visible <== when(wettkampfmode) choose false otherwise true
        }, new Button {
          onAction = downloadMenu.onAction.get
          text <== downloadMenu.text
          disable <== downloadMenu.disable
          visible <== when(wettkampfmode) choose false otherwise true
        }, new Button {
          onAction = disconnectMenu.onAction.get
          text <== disconnectMenu.text
          disable <== disconnectMenu.disable
        }, new Button {
          onAction = removeRemoteMenu.onAction.get
          text <== removeRemoteMenu.text
          disable <== removeRemoteMenu.disable
          visible <== when(wettkampfmode) choose false otherwise true
        }).filter(_.isVisible)
    } else {
      view.contextMenu = new ContextMenu() {
        items += navigate
      }
      toolbar.content = List(
        new Button {
          onAction = connectAndShareMenu.onAction.get
          text <== connectAndShareMenu.text
          disable <== connectAndShareMenu.disable
        }, btnEditRiege, new Button {
          onAction = downloadMenu.onAction.get
          text <== downloadMenu.text
          disable <== downloadMenu.disable
        }, new Button {
          onAction = disconnectMenu.onAction.get
          text <== disconnectMenu.text
          disable <== disconnectMenu.disable
        })
    }
  }

  //    val showQRCode = make
  view.selectionModel().setSelectionMode(SelectionMode.Single)
  view.selectionModel().setCellSelectionEnabled(true);
  view.selectionModel().getSelectedCells().onChange { (_, newItem) =>
    updateButtons
  }
  content = rootpane

  override def isPopulated = {
    if (subscriptions.isEmpty) {
      println("subscribing for network modus changes")
      subscriptions = subscriptions :+ KuTuApp.modelWettkampfModus.onChange { (_, _, newItem) =>
        println("refreshing Wettkampfmodus", newItem)
        updateButtons
      }
      println("subscribing for refreshing from websocket")
      subscriptions = subscriptions :+ WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
        newItem match {
          case be: BulkEvent if be.events.forall {
            case DurchgangStarted(_, _, _) => true
            case DurchgangFinished(_, _, _) => true
            case _ => false
          } =>
            println("refreshing network-dashboard from websocket", newItem)
            refreshData()

          case ds: DurchgangStarted =>
            println("refreshing network-dashboard from websocket", newItem)
            refreshData(Some(ds))
          //      case StationWertungenCompleted(wertungen: List[UpdateAthletWertung]) =>
          case df: DurchgangFinished =>
            println("refreshing network-dashboard from websocket", newItem)
            refreshData(Some(df))
          case AthletWertungUpdated(ahtlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String) =>
            if (selected.value) {
              println("refreshing network-dashboard from websocket", newItem)
              refreshData()
            }
          case AthletWertungUpdatedSequenced(ahtlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String, sequenceId) =>
            if (selected.value) {
              println("refreshing network-dashboard from websocket", newItem)
              refreshData()
            }
          case _ =>
        }
      }
    }
    refreshData()
    true
  }
}
 