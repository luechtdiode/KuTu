package ch.seidel.kutu.view

import ch.seidel.commons.{LazyTabPane, TabWithService}
import ch.seidel.kutu.*
import ch.seidel.kutu.Config.*
import ch.seidel.kutu.KuTuApp.{enc, hostServices}
import ch.seidel.kutu.actors.*
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{BestenListeToHtmlRenderer, PrintUtil, RiegenBuilder}
import ch.seidel.kutu.view.player.Player
import javafx.event.EventHandler
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.print.PageOrientation
import scalafx.scene.control.*
import scalafx.scene.control.TreeTableColumn.*
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, Priority, Region}

import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters

trait DurchgangItem

case class DurchgangState(wettkampfUUID: String, name: String, complete: Boolean, geraeteRiegen: List[GeraeteRiege], durchgang: Durchgang) extends DurchgangItem {
  val started: Long = durchgang.effectiveStartTime.map(_.getTime).getOrElse(0)
  val finished: Long = durchgang.effectiveEndTime.map(_.getTime).getOrElse(0)

  def update(newGeraeteRiegen: List[GeraeteRiege], dg: Durchgang): DurchgangState = {
    if this.durchgang == dg && newGeraeteRiegen.forall(gr => geraeteRiegen.contains(gr)) then this
    else DurchgangState(wettkampfUUID, name, complete, newGeraeteRiegen, dg)
  }

  def ~(other: DurchgangState): Boolean = name == other.name && geraeteRiegen != other.geraeteRiegen

  val updated: Long = System.currentTimeMillis()
  val isRunning: Boolean = started > 0L && finished < started
  private lazy val statsBase: immutable.Iterable[Int] = geraeteRiegen.groupBy(_.disziplin).filter(!_._1.get.isPause).map(_._2.map(_.kandidaten.size).sum)
  private lazy val statsCompletedBase: immutable.Iterable[(Option[Disziplin], Int, Int, Int, List[(Int, Int, Int, Int)])] = geraeteRiegen.groupBy(gr => gr.disziplin)
    .filter { d => !d._1.get.isPause }
    .map { gr =>
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
      (disziplin, 100 * completedCnt / totalCnt, completedCnt, totalCnt, grdStats)
    }

  private lazy val anzValue: Int = statsBase.sum

  lazy val anz = s"$anzValue"
  lazy val min = s"${statsCompletedBase.map(_._2).min}%"
  lazy val max = s"${statsCompletedBase.map(_._2).max}%"
  lazy val avg = s"${100 * statsCompletedBase.map(_._3).sum / anzValue}%"
  val lastResultsURL = s"$remoteBaseUrl/last-results?c=$wettkampfUUID&d=${encodeURIParam(name)}&k=gemischt"

  lazy val percentPerRiegeComplete: Map[Option[Disziplin], (String, String)] = statsCompletedBase
    .map(gr => gr._1 -> (s"${gr._2}%", gr._5.map(grh => s"Station ${grh._1 + 1}: ${grh._2}%").mkString("\n"))).toMap

  lazy val totalTime: Long = if started > 0 then {
    if finished > started then finished - started else updated - started
  } else 0
  //  lazy val minTime = s"${statsBase.min}"
  //  lazy val maxTime = s"${statsBase.max}"
  //  lazy val avgTime = s"${anzValue / statsBase.size}"
}

trait DurchgangStationTCAccess extends TCAccess[DurchgangState, Object, Disziplin] {
  def getDisziplin: Disziplin = getIndex
}

class DurchgangStationJFSCTreeTableColumn[T](val index: Disziplin) extends jfxsc.TreeTableColumn[DurchgangState, T] with DurchgangStationTCAccess {
  override def getIndex: Disziplin = index

  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}

class DurchgangStationTreeTableColumn[T](val index: Disziplin) extends TreeTableColumn[DurchgangState, T](new DurchgangStationJFSCTreeTableColumn[T](index)) with DurchgangStationTCAccess {

  override def getIndex: Disziplin = index

  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}

class DurchgangStationView(wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], durchgangModel: ObservableBuffer[TreeItem[DurchgangState]]) extends TreeTableView[DurchgangState] {

  id = "durchgang-table"
  //items = durchgangModel
  showRoot = false
  tableMenuButtonVisible = true
  private var lastDizs: Seq[Disziplin] = Seq()

  private def rebuildDiszColumns(disziplinlist: Seq[Disziplin]): Unit = {
    if lastDizs.nonEmpty then {
      val diszCols = lastDizs.map(_.name)
      columns.removeIf(c => diszCols.contains(c.getText))
    }
    lastDizs = disziplinlist
    columns.insertAll(6, lastDizs.map { disziplin =>
      val dc = new TreeTableColumn[DurchgangState, String] {
        text = disziplin.name
        prefWidth = 230
        columns ++= Seq(
          new DurchgangStationTreeTableColumn[String](disziplin) {
            text = "Stationen"
            prefWidth = 120
            cellValueFactory = { x =>
              if x.value.getValue == null then StringProperty("") else
                x.value.getValue.percentPerRiegeComplete.get(Some(disziplin)) match {
                  case Some(re) => StringProperty(re._2)
                  case _ => StringProperty("")
                }
            }
          }
          , new TreeTableColumn[DurchgangState, String] {
            text = "Fertig"
            prefWidth = 80
            cellFactory.value = { (_: Any) =>
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
              if x.value.getValue == null then StringProperty("") else
                x.value.getValue.percentPerRiegeComplete.get(Some(disziplin)) match {
                  case Some(re) => StringProperty(re._1)
                  case _ => StringProperty("0")
                }
            }
          }
        )
      }
      TreeTableColumn.sfxTreeTableColumn2jfx(dc)
    })
  }

  root = new TreeItem[DurchgangState]() {
    durchgangModel.onChange {
      val dml = durchgangModel.toList
      rebuildDiszColumns(disziplinlist().filter { d =>
        dml.exists(ti => ti.value.value.geraeteRiegen.exists(gr => gr.disziplin.contains(d)))
      })
      children = dml
    }
    styleClass.add("parentrow")
    expanded = true
  }

  var okIcon: Image = null
  try {
    okIcon = new Image(getClass.getResourceAsStream("/images/GreenOk.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass.getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  columns ++= List(
    new TreeTableColumn[DurchgangState, String] {
      prefWidth = 130
      text = "Durchgang"
      val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")
      cellValueFactory = { x =>
        StringProperty(
          if x.value.getValue == null then "Durchgänge"
          else if x.value.getValue.durchgang.planStartOffset != 0 && x.value.getValue.name.equals(x.value.getValue.durchgang.title) then {
            s"""${x.value.getValue.durchgang.name}
               |Plan-Start: ${x.value.getValue.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate).format(formatter)}
               |Plan-Ende: ${x.value.getValue.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate).format(formatter)}""".stripMargin
          } else {
            x.value.getValue.name
          })
      }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Start"
      cellValueFactory = { x => StringProperty(if x.value.getValue == null then "" else toTimeFormat(x.value.getValue.started)) }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Ende"
      cellValueFactory = { x => StringProperty(if x.value.getValue == null then "" else toTimeFormat(x.value.getValue.finished)) }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Dauer"
      cellValueFactory = { x => StringProperty(if x.value.getValue == null then "" else toDurationFormat(x.value.getValue.started, x.value.getValue.finished))
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
        StringProperty(if x.value.getValue == null then "" else
          s"""Tot:     ${toDurationFormat(x.value.getValue.durchgang.planTotal)}
             |Eint.:    ${toDurationFormat(x.value.getValue.durchgang.planEinturnen)}
             |Gerät.: ${toDurationFormat(x.value.getValue.durchgang.planGeraet)}""".stripMargin)
      }
    }
    , new TreeTableColumn[DurchgangState, String] {
      prefWidth = 80
      text = "Fertig"
      cellFactory.value = { (_: Any) =>
        new TreeTableCell[DurchgangState, String] {
          val image = new ImageView()
          graphic = image
          item.onChange { (_, _, newValue) =>
            text = newValue
            image.image = toIcon(newValue)
          }
        }
      }
      cellValueFactory = { x => StringProperty(if x.value.getValue == null then "" else x.value.getValue.avg) }
    }
  )

  rebuildDiszColumns(disziplinlist())

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

object DeferredPanelRefresher {
  private val refresherPool = new ScheduledThreadPoolExecutor(1)
  private var pendingUpdateTask: List[ScheduledFuture[?]] = List.empty
  sys.addShutdownHook(refresherPool.shutdownNow)


  def submitUpdateTask(task: () => Unit): Unit = {
    pendingUpdateTask = (pendingUpdateTask.filter(t => !t.isDone) match {
      case Nil => List()
      case h :: t => t.foreach(_.cancel(true))
        List(h)
    }) :+ DeferredPanelRefresher.refresherPool.schedule(new Runnable() { def run(): Unit = {
        Platform.runLater { task() }
      }},10L, TimeUnit.SECONDS)
  }
}

class NetworkTab(wettkampfmode: BooleanProperty, override val wettkampfInfo: WettkampfInfo, override val service: KutuService) extends Tab with TabWithService with ExportFunctions {

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  closable = false
  text = "Netzwerk-Dashboard"

  Player.setWettkampf(wettkampfInfo.wettkampf.toWettkampf, service)

  val disziplinlist: List[Disziplin] = wettkampfInfo.disziplinList

  private def loadDurchgaenge: List[DurchgangState] = {
    val durchgaenge = service.selectDurchgaenge(wettkampf.uuid.map(UUID.fromString).get).map(d => d.name -> d).toMap

    RiegenBuilder.mapToGeraeteRiegen(service.getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get)).toList)
      .filter(gr => gr.durchgang.nonEmpty)
      .groupBy(gr => gr.durchgang.get)
      .map { t =>
        DurchgangState(wettkampf.uuid.getOrElse(""), t._1, t._2.forall { riege => riege.erfasst }, t._2, durchgaenge.getOrElse(t._1, Durchgang(wettkampf.id, t._1)))
      }
      .toList.sortBy(_.name)
  }

  val model: ObservableBuffer[TreeItem[DurchgangState]] = ObservableBuffer[TreeItem[DurchgangState]]()

  private val isRunning: BooleanProperty = BooleanProperty(false)

  private def refreshData(): Unit = {
      val expandedStates = model
        .filter(_.isExpanded)
        .map(_.value.value.durchgang.title)
        .toSet
      val selected = view.selectionModel.value.selectedCells.toList.headOption
      val newList: immutable.Seq[DurchgangState] = loadDurchgaenge

      import CollectionConverters.*

      val groupMap = newList.groupBy(d => d.durchgang.title)
      val items: List[TreeItem[DurchgangState]] = for group <- groupMap.keySet.toList.sorted yield {
        val dgList = groupMap(group).sortBy(_.name)
        if dgList.size > 1 || dgList.head.name != dgList.head.durchgang.title then {
          val dgh = dgList.foldLeft(Durchgang())((acc, dg) => {
            if acc.planTotal > dg.durchgang.planTotal then acc else dg.durchgang
          })
          val groupDurchgangState = DurchgangState(
            wettkampfUUID = dgList.head.wettkampfUUID,
            name = dgh.title,
            complete = dgList.forall(_.complete),
            geraeteRiegen = dgList.flatMap(_.geraeteRiegen).toList,
            durchgang = dgh)
          new TreeItem[DurchgangState](groupDurchgangState) {
            for d <- dgList do {
              children.add(new TreeItem[DurchgangState](d))
            }
            expanded = expandedStates.contains(group)
          }
        } else {
          new TreeItem[DurchgangState](dgList.head) {
            expanded = false
          }
        }
      }
      if model.isEmpty then {
        model.setAll(items.asJavaCollection)
      } else {
        items.zip(model).foreach(pair => syncTreeItems(pair._1, pair._2))
        if items.size < model.size then {
          model.removeRange(items.size, model.size - 1)
        }
        if model.isEmpty then {
          val collection = items.asJavaCollection
          model.setAll(collection)
        }
        if items.size > model.size then {
          items.drop(model.size).foreach(model.add)
        }
      }

      isRunning.set(model.exists(_.getValue.isRunning))
      selected.foreach(selection => {
        if selection.column > -1 && view.getColumns.size() > selection.column then {
          val column = view.getColumns.get(selection.column)
          view.selectionModel.value.select(selection.row, column)
        }
      })
  }

  private def syncTreeItems(source: TreeItem[DurchgangState], target: TreeItem[DurchgangState]): Unit = {
    if !target.getValue.equals(source.getValue) then {
      target.value = source.getValue
    }
    source.children.zip(target.children).foreach(pair => syncTreeItems(pair._1, pair._2))
    if source.children.size < target.children.size then {
      target.children.removeRange(source.children.size, target.children.size - 1)
    }
    if source.children.size > target.children.size then {
      target.children.addAll(source.children.drop(target.children.size))
    }
    target.expanded = source.isExpanded
  }

  var subscriptions: List[Subscription] = List.empty

  override def release: Unit = {
    subscription.cancel()
    subscriptions.foreach(_.cancel())
    subscriptions = List.empty
  }

  onSelectionChanged = _ => {
    if selected.value then {
      refreshData()
      updateButtons()
    }
  }

  val view = new DurchgangStationView(
    wettkampf, service,
    () => {
      disziplinlist
    },
    model)

  type MenuActionHandler = (String, ActionEvent) => Unit

  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R): EventHandler[J] = (event: J) => {
    handler(event)
  }

  def makeMenuAction(caption: String)(handler: MenuActionHandler): MenuItem = {
    new MenuItem(caption) {
      onAction = handleAction { (e: ActionEvent) =>
        handler(caption, e)
      }
    }
  }

  private def getSelectedDruchgangStates: List[DurchgangState] = {
    view.selectionModel().selectedItems.flatMap {
      case treeItem if treeItem.getChildren.nonEmpty =>
        treeItem.getChildren.toList.map(_.getValue)
      case treeItem if treeItem.getChildren.isEmpty =>
        List(treeItem.getValue)
    }.toList
  }

  private def makeDurchgangStartenMenu(p: WettkampfView): MenuItem = {

    val item = makeMenuAction("Durchgang starten") { (_, _) =>
      getSelectedDruchgangStates.foreach { d =>
        KuTuApp.invokeWithBusyIndicator {
          Await.result(KuTuServer.startDurchgang(p, d.name), Duration.Inf)
        }
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.getOrElse(""))
        || getSelectedDruchgangStates.forall((d: DurchgangState) => d.started > 0 && d.finished == 0),
      view.selectionModel().selectedItem,
      isRunning,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  private def makeDurchgangAbschliessenMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Durchgang abschliessen") { (_, _) =>
      getSelectedDruchgangStates.foreach { d =>
        KuTuApp.invokeWithBusyIndicator {
          Await.result(KuTuServer.finishDurchgang(p, d.name), Duration.Inf)
        }
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.getOrElse(""))
        || getSelectedDruchgangStates.forall((d: DurchgangState) => !(d.started > 0 && d.finished == 0)),
      view.selectionModel().selectedItem,
      isRunning,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  private def makeDurchgangResetMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Durchgang zurücksetzen") { (_, _) =>
      getSelectedDruchgangStates.foreach { d =>
        KuTuApp.invokeWithBusyIndicator {
          Await.result(KuTuServer.resetDurchgang(p, d.name), Duration.Inf)
        }
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.getOrElse(""))
        || getSelectedDruchgangStates.forall((d: DurchgangState) => d.started == 0 || (d.started > 0 && d.finished == 0)),
      view.selectionModel().selectedItem,
      isRunning,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  var okIcon: Image = null
  try {
    okIcon = new Image(getClass.getResourceAsStream("/images/GreenOk.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass.getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  private def navigateToDurchgangResultView(): Menu = {
    val selDGStates = getSelectedDruchgangStates
    new Menu {
      text = "Resultat-Anzeige im Browser öffnen"
      updateItems()
      reprintItems.onChange {
        updateItems()
      }

      private def updateItems(): Unit = {
        items.clear()
        selDGStates.foreach(state => {
          items += KuTuApp.makeMenuAction(s"Für Durchgang ${state.name}") { (caption: String, action: ActionEvent) =>
            println(s"navigating to ${state.lastResultsURL}")
            hostServices.showDocument(state.lastResultsURL)
          }
        })
        disable.value = items.isEmpty
      }
    }
  }

  def makeSelectedDurchgangTeilnehmerExport(): Menu = {
    val option = getSelectedDruchgangStates
    val selectedDurchgaenge = option.toSet.map((_: DurchgangState).name)
    new Menu {
      text = "Durchgang-Teilnehmerliste erstellen"
      updateItems()
      reprintItems.onChange {
        updateItems()
      }

      private def updateItems(): Unit = {
        items.clear()
        val affectedDurchgaenge: Set[String] = reprintItems.get.map(_.durchgang)
        if selectedDurchgaenge.nonEmpty then {
          items += KuTuApp.makeMenuAction(s"Aus Durchgang ${selectedDurchgaenge.mkString(", ")}") { (caption: String, action: ActionEvent) =>
            doSelectedTeilnehmerExport(text.value, selectedDurchgaenge)(action)
          }
        }
        if affectedDurchgaenge.nonEmpty && selectedDurchgaenge.nonEmpty then {
          items += new SeparatorMenuItem()
        }
        if affectedDurchgaenge.nonEmpty then {
          val allItem = KuTuApp.makeMenuAction(s"Alle betroffenen (${affectedDurchgaenge.size})") { (caption: String, action: ActionEvent) =>
            doSelectedTeilnehmerExport(text.value, affectedDurchgaenge)(using action)
          }
          items += allItem
          items += new SeparatorMenuItem()
          affectedDurchgaenge.toList.sorted.foreach { durchgang =>
            items += KuTuApp.makeMenuAction(s"$durchgang") { (caption: String, action: ActionEvent) =>
              doSelectedTeilnehmerExport(text.value, Set(durchgang))(using action)
            }
          }
        }
        disable.value = items.isEmpty
      }
    }
  }

  def makeSelectedRiegenBlaetterExport(): Menu = {
    val option = getSelectedDruchgangStates
    val selectedDurchgaenge = option.toSet.map((_: DurchgangState).name)
    new Menu {
      text = "Riegenblätter nachdrucken"
      updateItems()
      reprintItems.onChange {
        updateItems()
      }

      private def updateItems(): Unit = {
        items.clear()
        val affectedDurchgaenge: Set[String] = reprintItems.get.map(_.durchgang)
        if selectedDurchgaenge.nonEmpty then {
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
        if affectedDurchgaenge.nonEmpty && selectedDurchgaenge.nonEmpty then {
          items += new SeparatorMenuItem()
        }
        if affectedDurchgaenge.nonEmpty then {
          val allItem = KuTuApp.makeMenuAction(s"Alle betroffenen (${affectedDurchgaenge.size})") { (caption: String, action: ActionEvent) =>
            doSelectedRiegenBelatterExport(text.value, affectedDurchgaenge)(action)
          }
          items += allItem
          items += new SeparatorMenuItem()
          affectedDurchgaenge.toList.sorted.foreach { durchgang =>
            items += KuTuApp.makeMenuAction(s"$durchgang") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, Set(durchgang))(action)
            }
          }
        }
        disable.value = items.isEmpty
      }
    }
  }

  private def makeNavigateToMenu(p: WettkampfView): Menu = {
    new Menu("Gehe zu Riege ...") {
      def addRiegenMenuItems(durchgang: DurchgangState, column: DurchgangStationTCAccess): Unit = {
        val disziplin = column.getDisziplin
        val selection = durchgang.geraeteRiegen.filter {
          _.disziplin.contains(disziplin)
        }
        items = selection.map { geraeteRiege =>
          val menu = KuTuApp.makeMenuAction(geraeteRiege.caption) { (caption, action) =>
            lazypane match {
              case Some(pane) =>
                val pgmFilter = geraeteRiege.kandidaten.flatMap(_.wertungen.map(w => w.wettkampfdisziplin.programm)).distinct
                val progs = service.readWettkampfLeafs(wettkampf.programm.id) map (p => p.id)
                val wertungTab: WettkampfWertungTab = new WettkampfWertungTab(wettkampfmode, None, Some(geraeteRiege), wettkampfInfo, service, {
                  service.listAthletenWertungenZuProgramm(progs, wettkampf.id)
                    .filter(wertung => pgmFilter.contains(wertung.wettkampfdisziplin.programm))
                }) {
                  text = geraeteRiege.caption
                  closable = true
                }
                pane.tabs.add(pane.tabs.size() + (if wettkampfmode.value then -2 else -1), wertungTab.asInstanceOf[Tab])
                pane.selectionModel.value.select(wertungTab)
                wertungTab.tabPaneProperty.onChange {
                  if wertungTab.getTabPane == null then {
                    wertungTab.release
                  }
                }
                wertungTab.populated
              case _ =>
            }
          }
          menu.graphic = new ImageView {
            image = if geraeteRiege.erfasst then okIcon else nokIcon
          }
          menu
        }
      }

      items.clear()
      view.selectionModel.value.selectedCells.toList.headOption match {
        case None =>
        case Some(cell) if cell.treeItem != null && cell.treeItem.getValue != null =>
          cell.getTableColumn match {
            case column: DurchgangStationTCAccess =>
              addRiegenMenuItems(cell.treeItem.getValue, column)
            case _ => if cell.getTableColumn.parentColumn.value != null && cell.getTableColumn.parentColumn.value.columns.size == 2 then {
              val col = cell.getTableColumn.getParentColumn.getColumns.head
              col match {
                case column: DurchgangStationJFSCTreeTableColumn[?] =>
                  addRiegenMenuItems(cell.treeItem.getValue, column)
                case column: DurchgangStationTCAccess =>
                  addRiegenMenuItems(cell.treeItem.getValue, column)
                case _ =>
              }
            }
          }
        case _ =>
      }
      disable = items.size() == 0
    }
  }

  private def makePlayMediaMenu(p: WettkampfView, cleanCache: Boolean = false): Menu = {
    new Menu("Playlist ...") {
      def addMediaPlaylistItems(durchgang: DurchgangState, column: DurchgangStationTCAccess): Unit = {
        val disziplin = column.getDisziplin
        val selection = durchgang.geraeteRiegen.filter {
          _.disziplin.contains(disziplin)
        }
        items = selection.filter(gr => !gr.erfasst).take(1).flatMap {
          geraeteRiege =>
            if cleanCache then {
              geraeteRiege.resetMediaListCache()
            }
            val mediaList = geraeteRiege.getMediaList(p.toWettkampf, service.loadMedia)
            mediaList.map { case (_, wertung, title, _) =>
              makeMenuAction(title + " ...") { (caption, action) =>
                Player.clearPlayList()
                mediaList.foreach { case (_, _, title, mediaURI) =>
                  Player.addToPlayList(title, mediaURI.toASCIIString.toLowerCase)
                }
                Player.load(title, AthletMediaAquire(wertung.wettkampf.uuid.get, wertung.athlet, wertung.toWertung))
              }
            }
        }.take(3)
      }

      items.clear()
      view.selectionModel.value.selectedCells.toList.headOption match {
        case None =>
        case Some(cell) if cell.treeItem != null && cell.treeItem.getValue != null =>
          cell.getTableColumn match {
            case column: DurchgangStationTCAccess =>
              addMediaPlaylistItems(cell.treeItem.getValue, column)
            case _ => if cell.getTableColumn.parentColumn.value != null && cell.getTableColumn.parentColumn.value.columns.size == 2 then {
              val col = cell.getTableColumn.getParentColumn.getColumns.head
              col match {
                case column: DurchgangStationJFSCTreeTableColumn[?] =>
                  addMediaPlaylistItems(cell.treeItem.getValue, column)
                case column: DurchgangStationTCAccess =>
                  addMediaPlaylistItems(cell.treeItem.getValue, column)
                case _ =>
              }
            }
          }
        case _ =>
      }
      disable = items.size() == 0
    }
  }

  private val qrcodeMenu: MenuItem = KuTuApp.makeShowQRCodeMenu(wettkampf)
  private val connectAndShareMenu: MenuItem = KuTuApp.makeConnectAndShareMenu(wettkampf)

  val uploadMenu: MenuItem = KuTuApp.makeWettkampfUploadMenu(wettkampf,
    when(Bindings.createBooleanBinding(() =>
      Config.isLocalHostServer
        || (wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !ConnectionStates.connectedProperty.value)
        || isRunning.value,

      KuTuApp.selectedWettkampfSecret,
      LocalServerStates.localServerProperty,
      ConnectionStates.connectedProperty,
      ConnectionStates.remoteServerProperty,
      isRunning,
    )) choose true otherwise false)

  private val downloadMenu: MenuItem = KuTuApp.makeWettkampfDownloadMenu(wettkampf)

  private val disconnectMenu: MenuItem = KuTuApp.makeDisconnectMenu(wettkampf)
  private val removeRemoteMenu: MenuItem = KuTuApp.makeWettkampfRemoteRemoveMenu(wettkampf)

  val generateBestenliste: Button & BestenListeToHtmlRenderer = new Button with BestenListeToHtmlRenderer {
    text = "Bestenliste erstellen"
    minWidth = 75
    disable.value = wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    onAction = (event: ActionEvent) => {
      if !WebSocketClient.isConnected then {
        val filename = "Bestenliste_" + encodeFileName(wettkampf.easyprint) + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if !dir.exists() then {
          dir.mkdirs()
        }
        val logofile = PrintUtil.locateLogoFile(dir)

        def generate(lpp: Int) = toHTMListe(WertungServiceBestenResult.getBestenResults, logofile)

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), adjustLinesPerPage = false, generate, orientation = PageOrientation.Portrait)(event)
      } else {
        Await.result(KuTuServer.finishDurchgangStep(wettkampf), Duration.Inf)
        val topResults = s"${Config.remoteBaseUrl}/?" + new String(enc.encodeToString(s"top&c=${wettkampf.uuid.get}".getBytes))
        KuTuApp.hostServices.showDocument(topResults)
      }
      WertungServiceBestenResult.resetBestenResults()
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
    items += makeDurchgangResetMenu(wettkampf)
  }
  private val toolbar: ToolBar = new ToolBar {
  }
  val rootpane: BorderPane = new BorderPane {
    hgrow = Priority.Always
    vgrow = Priority.Always
    top = toolbar
    center = view
  }
  private val btnEditRiege: MenuButton = new MenuButton("Gehe zu ...") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
  }
  private val btnMediaPlayer: MenuButton = new MenuButton("Mediaplayer ...") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
    delegate.showingProperty().addListener((_, _, newvalue) =>  {
      if newvalue then updateButtons(refreshToolbar = false)
    })
  }
  private val btnDurchgang: MenuButton = new MenuButton("Durchgang ...") {
    disable <== when(createBooleanBinding(() => items.isEmpty, items)) choose true otherwise false
    delegate.showingProperty().addListener((_, _, newvalue) =>  {
      if newvalue then updateButtons(refreshToolbar = false, refreshMedia = false)
    })
  }
  view.onContextMenuRequested = _ => {
    updateButtons(refreshToolbar = false)
  }

  private def updateButtons(refreshToolbar: Boolean = true, refreshMedia: Boolean = true): Unit = {
    val navigate = makeNavigateToMenu(wettkampf)
    val mediaItems = makePlayMediaMenu(wettkampf, refreshMedia)
    btnEditRiege.items.clear()
    btnEditRiege.items.addAll(navigate.items)

    btnMediaPlayer.items.clear()
    btnMediaPlayer.items += KuTuApp.makeMenuAction("Media Player anzeigen ...") { (caption, action) =>
      Player.show()
    }
    // graphic for displaying checkmark// graphic for displaying checkmark
    if !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) then {
      val checkmark = new Region()
      checkmark.getStyleClass.add("check-mark")
      checkmark.visible <== Player.isNetworkMediaPlayer

      val checkIsUseMyMediaPlayerMenuItem = new MenuItem("Media Player den Wertungsrichtern freigeben", checkmark) {
        onAction = handleAction { (_: ActionEvent) =>
          Player.useMyMediaPlayerAsNetworkplayer(!Player.isNetworkMediaPlayer.getValue)
        }
      }

      btnMediaPlayer.items += checkIsUseMyMediaPlayerMenuItem
    }
    btnMediaPlayer.items += new SeparatorMenuItem()
    btnMediaPlayer.items.addAll(mediaItems.items)

    if !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) then {
      btnDurchgang.items.clear()
      btnDurchgang.items += makeDurchgangStartenMenu(wettkampf)
      btnDurchgang.items += navigateToDurchgangResultView()
      btnDurchgang.items += makeSelectedDurchgangTeilnehmerExport()
      btnDurchgang.items += new SeparatorMenuItem()
      btnDurchgang.items += makeSelectedRiegenBlaetterExport()
      btnDurchgang.items += makeMenuAction("Bestenliste erstellen") { (_, action) =>
        generateBestenliste.onAction.value.handle(action)
      }
      btnDurchgang.items += new SeparatorMenuItem()
      btnDurchgang.items += makeDurchgangAbschliessenMenu(wettkampf)
      btnDurchgang.items += makeDurchgangResetMenu(wettkampf)

      view.contextMenu = new ContextMenu() {
        items += makeDurchgangStartenMenu(wettkampf)
        items += navigateToDurchgangResultView()
        items += new SeparatorMenuItem()
        items += makeSelectedDurchgangTeilnehmerExport()
        items += new SeparatorMenuItem()
        items += makeSelectedRiegenBlaetterExport()
        if mediaItems.items.nonEmpty then items += mediaItems
        items += navigate
        items += makeMenuAction("Bestenliste erstellen") { (_, action) =>
          generateBestenliste.onAction.value.handle(action)
        }
        items += new SeparatorMenuItem()
        items += makeDurchgangAbschliessenMenu(wettkampf)
        items += makeDurchgangResetMenu(wettkampf)
      }
      if refreshToolbar then {
        toolbar.content = List(
          new Button {
            onAction = connectAndShareMenu.onAction.get
            text <== connectAndShareMenu.text
            disable <== connectAndShareMenu.disable
          }, new Button {
            onAction = qrcodeMenu.onAction.get
            text <== qrcodeMenu.text
            disable <== qrcodeMenu.disable
          }, btnDurchgang, btnEditRiege, btnMediaPlayer, new Button {
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
      }
    } else {
      view.contextMenu = new ContextMenu() {
        items += navigate
      }
      if refreshToolbar then {
        toolbar.content = List(
          new Button {
            onAction = connectAndShareMenu.onAction.get
            text <== connectAndShareMenu.text
            disable <== connectAndShareMenu.disable
          }, btnEditRiege, btnMediaPlayer, new Button {
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
  }

  //    val showQRCode = make
  view.selectionModel().setSelectionMode(SelectionMode.Single)
  view.selectionModel().setCellSelectionEnabled(true)
  view.selectionModel().getSelectedCells.onChange { (_, _) =>
    updateButtons(refreshToolbar = false, refreshMedia = false)
  }
  content = rootpane

  override def isPopulated: Boolean = {

    if subscriptions.isEmpty then {
      println("subscribing for network modus changes")
      subscriptions = subscriptions :+ KuTuApp.modelWettkampfModus.onChange { (_, _, newItem) =>
        println(("refreshing Wettkampfmodus", newItem))
        updateButtons()
      }
      println("subscribing for refreshing from websocket")
      //subscriptions = subscriptions :+ WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) => refreshData(scope=Some(newItem)) }
      subscriptions = subscriptions :+ WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
       // if (selected.value) {
       // println("refreshing network-dashboard from websocket", newItem)

        newItem match {
          case be: BulkEvent if be.events.forall {
            case DurchgangStarted(_, _, _) => true
            case DurchgangFinished(_, _, _) => true
            case DurchgangResetted(_, _) => true
            case _ => false
          } =>
            refreshData()

          case ds: DurchgangStarted =>
            refreshData()
          //      case StationWertungenCompleted(wertungen: List[UpdateAthletWertung]) =>
          case df: DurchgangFinished =>
            refreshData()
          case dr: DurchgangResetted =>
            refreshData()
          case AthletWertungUpdated(ahtlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String) =>
            if selected.value then {
              //DeferredPanelRefresher.submitUpdateTask(() => refreshData()) //
              refreshData()
            }
          case AthletWertungUpdatedSequenced(ahtlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String, sequenceId) =>
            if selected.value then {
              //DeferredPanelRefresher.submitUpdateTask(() => refreshData()) //
              refreshData()
            }
          case _ =>
            DeferredPanelRefresher.submitUpdateTask(() => refreshData()) //
        }
      }
    }
    refreshData()
    updateButtons()
    true
  }
}
 