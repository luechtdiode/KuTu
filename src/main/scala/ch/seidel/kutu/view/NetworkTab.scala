package ch.seidel.kutu.view

import java.util.UUID
import javafx.scene.{ control => jfxsc }

import scalafx.Includes._
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.TableView
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.Priority
import scalafx.geometry.Insets
import scalafx.event.subscriptions.Subscription
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.StringProperty
import scalafx.beans.value.ObservableValue

import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.Config._
import ch.seidel.commons.TabWithService
import ch.seidel.kutu.renderer.RiegenBuilder
import scalafx.scene.control.TableCell
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView
import java.util.Date
import scala.concurrent.duration.Duration
import scalafx.event.ActionEvent
import scalafx.scene.control.MenuItem
import scala.concurrent.Await
import ch.seidel.kutu.KuTuServer
import scalafx.beans.binding.Bindings
import ch.seidel.kutu.ConnectionStates
import scalafx.scene.control.MenuButton
import scalafx.scene.control.ContextMenu
import scalafx.application.Platform
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import ch.seidel.kutu.http.Core
import scalafx.beans.property.ObjectProperty
import ch.seidel.kutu.akka._
import ch.seidel.kutu.http.WebSocketClient
import scala.util.Success
import ch.seidel.commons.PageDisplayer
import scala.util.Failure
import ch.seidel.commons.DisplayablePage
import scalafx.scene.Node
import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.scene.control.ToolBar
import scalafx.scene.control.Button

case class DurchgangState(wettkampfUUID: String, name: String, started: Long, complete: Boolean, finished: Long, geraeteRiegen: List[GeraeteRiege]) {
  def start(time: Long = 0) = DurchgangState(wettkampfUUID, name, if (started == 0) if (time == 0) System.currentTimeMillis() else time else started, complete, 0, geraeteRiegen)
  def finish(time: Long = 0) = DurchgangState(wettkampfUUID, name, started, complete, if (time == 0) System.currentTimeMillis() else time, geraeteRiegen)
  def update(newGeraeteRiegen: List[GeraeteRiege]) = {
    if (newGeraeteRiegen.forall(gr => geraeteRiegen.exists(egr => egr == gr))) this 
    else DurchgangState(wettkampfUUID, name, started, complete, finished, newGeraeteRiegen)
  }
  def ~ (other: DurchgangState) = name == other.name && geraeteRiegen != other.geraeteRiegen
  
  val updated = System.currentTimeMillis()
  val isRunning = started > 0L && finished < started
  lazy val statsBase = geraeteRiegen.groupBy(_.disziplin).map(_._2.map(_.kandidaten.size).sum)
  lazy val statsCompletedBase = geraeteRiegen.groupBy(gr => gr.disziplin).map{gr =>  
    val (disziplin, grd) = gr
    def hasWertungInDisciplin(wertungen: Seq[WertungView]) = wertungen.filter(w => disziplin.exists(d => d ==  w.wettkampfdisziplin.disziplin)).exists(_.endnote > 0)
    
    val grdStats = grd.groupBy(grds => grds.halt).map{grdsh => 
        val totalCnt = grdsh._2.map(_.kandidaten.size).sum
        val completedCnt = grdsh._2.map(_.kandidaten.filter(k => hasWertungInDisciplin(k.wertungen)).size).sum
        (grdsh._1, 100 * completedCnt / totalCnt, completedCnt, totalCnt)
      }.toList.sortBy(grdsh => grdsh._1)
    
    val totalCnt = gr._2.map(_.kandidaten.size).sum
    val completedCnt = gr._2.map(_.kandidaten.filter(k => hasWertungInDisciplin(k.wertungen)).size).sum    
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
    
  lazy val totalTime = if (started > 0) {if (finished > started) finished - started else updated - started} else 0
//  lazy val minTime = s"${statsBase.min}"
//  lazy val maxTime = s"${statsBase.max}"
//  lazy val avgTime = s"${anzValue / statsBase.size}"
}

object NetworkTab {
  var activeDurchgaenge: Map[WettkampfView, Set[DurchgangState]] = Map.empty
  val activeDurchgaengeProp = new ObjectProperty[Set[DurchgangState]]()
  val activeDurchgaengeStepFinishedProp = new ObjectProperty[FinishDurchgangStep]()
   
  def startDurchgang(w: WettkampfView, d: DurchgangState, t: Long) = {
    val started = d.start(t)
    val newset = activeDurchgaenge.get(w).orElse(Some(Set[DurchgangState]())).get.filter(_.name != d.name) + started
    val newEntry = (w -> newset)
    activeDurchgaenge = activeDurchgaenge + newEntry
    activeDurchgaengeProp.set(activeDurchgaenge.flatMap(x => x._2).toSet)
    started
  }
  def finishDurchgangStep(w: WettkampfView) = {
    activeDurchgaengeStepFinishedProp.set(FinishDurchgangStep(w.id));
  }
  def finishDurchgang(w: WettkampfView, d: DurchgangState, t: Long) = {
    val finished = d.finish(t)
    val newset = activeDurchgaenge.get(w).orElse(Some(Set[DurchgangState]())).get.filter(_.name != d.name) + finished
    val newEntry = (w -> newset)
    activeDurchgaenge = activeDurchgaenge + newEntry
    activeDurchgaengeProp.set(activeDurchgaenge.flatMap(x => x._2).toSet)
    finished    
//    activeDurchgaenge.get(w) match {
//      case Some(dl) => 
//        val dln = dl.filter(dx => dx != d)
//        if (dln.isEmpty) {
//          activeDurchgaenge - w
//        } else {
//          activeDurchgaenge += (w -> dln)
//        }
//      case _ => activeDurchgaenge
//    }
//    d.finish
  }
  
  def isRunning(wettkampf: WettkampfView) = {
    val running = activeDurchgaenge.filter(_._1 == wettkampf).exists(_._2.exists(_.isRunning))
    running
  }
  
  def getDurchgang(w: WettkampfView, d: String) = {
    activeDurchgaenge.get(w) match {
      case Some(dl) => dl.find(dx => dx.name == d)
      case _ => None
    }
  }
}

trait DurchgangStationTCAccess extends TCAccess[DurchgangState, Object, Disziplin] {
  def getDisziplin = getIndex
}
class DurchgangStationJFSCTableColumn[T](val index: Disziplin) extends jfxsc.TableColumn[DurchgangState, T] with DurchgangStationTCAccess {
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}
class DurchgangStationTableColumn[T](val index: Disziplin) extends TableColumn[DurchgangState, T] with DurchgangStationTCAccess {
  override val delegate: jfxsc.TableColumn[DurchgangState, T] = new DurchgangStationJFSCTableColumn[T](index)
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangState): Object = new Object()
}

class DurchgangStationView(wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], durchgangModel: ObservableBuffer[DurchgangState]) extends TableView[DurchgangState] {

  id = "durchgang-table"
  items = durchgangModel
  var okIcon: Image = null
  try {
    okIcon = new Image(getClass().getResourceAsStream("/images/GreenOk.png"))
  }catch{case e: Exception => e.printStackTrace()}
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
  }catch{case e: Exception => e.printStackTrace()}
  
  columns ++= List(
    new TableColumn[DurchgangState, String] {
      prefWidth = 130
      text = "Durchgang"
      cellValueFactory = { x => StringProperty(x.value.name) }
    }
    , new TableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Start"
      cellValueFactory = { x => StringProperty(toTimeFormat(x.value.started))}
    }
    , new TableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Ende"
      cellValueFactory = { x => StringProperty(toTimeFormat(x.value.finished))}
    }
    , new TableColumn[DurchgangState, String] {
      prefWidth = 85
      text = "Dauer"
      cellValueFactory = { x => StringProperty(toDurationFormat(x.value.started, x.value.finished))
      }
    }
    , new TableColumn[DurchgangState, String] {
      prefWidth = 40
      text = "Anzahl"
      cellValueFactory = { x => StringProperty(x.value.anz)}
    }
    , new TableColumn[DurchgangState, String] {
      prefWidth = 80
      text = "Fertig"
      cellFactory = { _ => new TableCell[DurchgangState, String] {
        val image = new ImageView()
        graphic = image
          item.onChange { (_, _, newValue) => 
            text = newValue
            image.image = 
              if ("100%" == newValue || "0%" == newValue || "0" == newValue || "" == newValue)
                okIcon    
              else
                nokIcon
          }
        }
      }
      cellValueFactory = { x => StringProperty(x.value.avg)}
    }
  )

  columns ++= disziplinlist().map {disziplin =>    
    val dc = new TableColumn[DurchgangState, String] {
      text = disziplin.name
      prefWidth = 230
      columns ++= Seq(
          new DurchgangStationTableColumn[String](disziplin) {
            text = "Stationen"
            prefWidth = 120
            cellValueFactory = { x =>
              x.value.percentPerRiegeComplete.get(Some(disziplin)) match {
                case Some(re) => StringProperty(re._2)
                case _ => StringProperty("")
              }
            }
          }
          , new TableColumn[DurchgangState, String] {
            text = "Fertig"
            prefWidth = 80
            cellFactory = { _ => new TableCell[DurchgangState, String] {
              val image = new ImageView()
              graphic = image
                item.onChange { (_, _, newValue) => 
                  text = newValue
                  image.image = 
                    if ("100%" == newValue || "0%" == newValue || "0" == newValue || "" == newValue)
                      okIcon    
                    else
                      nokIcon
                }
              }
            }
            cellValueFactory = { x =>
              x.value.percentPerRiegeComplete.get(Some(disziplin)) match {
                case Some(re) => StringProperty(re._1)
                case _ => StringProperty("0")
              }
            }
          }
        )
    }
    TableColumn.sfxTableColumn2jfx(dc)
  }

}

class NetworkTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
  import NetworkTab._
  
  closable = false
  text = "Netzwerk-Dashboard"
  
  lazy val disziplinlist = service.listDisziplinesZuWettkampf(wettkampf.id)
  
  def loadDurchgaenge = RiegenBuilder.mapToGeraeteRiegen(service.getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get)).toList)
    .filter(gr => gr.durchgang.nonEmpty) 
    .groupBy(gr => gr.durchgang.get)
    .map{t => getDurchgang(wettkampf, t._1)
      .map(d => d.update(t._2))
      .getOrElse(DurchgangState(wettkampf.uuid.getOrElse(""), t._1, 0, t._2.forall { riege => riege.erfasst }, 0, t._2))
    }
    .toList.sortBy(_.name)

  val model = ObservableBuffer[DurchgangState]()
  
  def refreshData(event: Option[KutuAppEvent] = None) {
    val newList = loadDurchgaenge.map{d =>
      event match {
        case Some(DurchgangStarted(wettkampfUUID: String, durchgang: String, time: Long)) if (d.wettkampfUUID == wettkampfUUID && d.name == durchgang) =>
          startDurchgang(wettkampf, d, time)
//      caseSome( StationWertungenCompleted(wertungen: List[UpdateAthletWertung])) =>
        case Some(DurchgangFinished(wettkampfUUID: String, durchgang: String, time: Long)) if (d.wettkampfUUID == wettkampfUUID && d.name == durchgang) =>
          finishDurchgang(wettkampf, d, time)
        case _ => d
      }
    }
    
    val toRemove = model.toSeq.filter(d => newList.exists(p => p.name == d.name))
    toRemove.foreach(model -= _)

    val toUpdate = model.toSeq.zipWithIndex.map(zd => (zd._2, newList.find(p => p ~ zd._1))).filter(zd => zd._2.nonEmpty)
    toUpdate.foreach(zd => model.set(zd._1, zd._2.get))
    
    newList.filter(d => !model.exists(p => p.name == d.name)).foreach(d => model += d)
  }
    
  var subscriptions: List[Subscription] = List.empty

  override def release() {
    subscriptions.foreach(_.cancel)
    subscriptions = List.empty
  }
  
 
  println("subscribing for network modus changes")
  subscriptions = subscriptions :+ KuTuApp.modelWettkampfModus.onChange { (_, _, newItem) =>
    println("refreshing Wettkampfmodus", newItem)
  }
  
  onSelectionChanged = handle {
    if(selected.value) {
      refreshData()
    }
  }
  
  println("subscribing for refreshing from websocket")
  subscriptions = subscriptions :+ WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
    newItem match {
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
      case _ =>
    }
  }
    
  def uploadResults(caption: String) {
    import scala.concurrent.ExecutionContext.Implicits.global
    val process = KuTuApp.invokeAsyncWithBusyIndicator{
      if (remoteBaseUrl.indexOf("localhost") > -1) {
        KuTuServer.startServer { uuid => KuTuServer.sha256(uuid) }
      }
      KuTuServer.httpUploadWettkampfRequest(wettkampf.toWettkampf)
    }
    process.onComplete{resultTry =>
      Platform.runLater{ 
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
  
  override def isPopulated = {
    refreshData()
    val view = new DurchgangStationView(
        wettkampf, service,
        () => {disziplinlist},
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
    def makeDurchgangStartenMenu(p: WettkampfView): MenuItem = {
      val item = makeMenuAction("Durchgang starten") {(caption, action) =>
        val actSelection = view.selectionModel().selectedItems.headOption match {
          case Some(d: DurchgangState) =>
            KuTuApp.invokeWithBusyIndicator {
              Await.result(KuTuServer.startDurchgang(p, d.name), Duration.Inf)
              startDurchgang(p, d, 0)
              refreshData()
            }
          case _ =>
        }
      }
      item.disable <== when(Bindings.createBooleanBinding(() => 
        !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) 
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse(""))
        || (view.selectionModel().selectedItems.headOption match { 
            case Some(d) => (d.started > 0 && d.finished == 0)  
            case _ => true
          }),
        view.selectionModel().selectedItem,
        activeDurchgaengeProp,
        ConnectionStates.connectedWithProperty
        )) choose true otherwise false 
      item
    }
    
    def makeDurchgangAbschliessenMenu(p: WettkampfView): MenuItem = {
      val item = makeMenuAction("Durchgang abschliessen") {(caption, action) =>
        val actSelection = view.selectionModel().selectedItems.headOption match {
          case Some(d: DurchgangState) =>
            KuTuApp.invokeWithBusyIndicator {
              Await.result(KuTuServer.finishDurchgang(p, d.name), Duration.Inf)
              finishDurchgang(p, d, 0)
              refreshData()
            }
          case _ =>
        }
      }
      item.disable <== when(Bindings.createBooleanBinding(() => 
        !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) 
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse(""))
        || (view.selectionModel().selectedItems.headOption match { 
            case Some(d) => (d.started == 0 || d.finished > 0)  
            case _ => true
          }),        
        view.selectionModel().selectedItem,
        activeDurchgaengeProp,
        ConnectionStates.connectedWithProperty
        )) choose true otherwise false 
      item
    }

    val qrcodeMenu: MenuItem = KuTuApp.makeShowQRCodeMenu(wettkampf)
    val connectAndShareMenu = KuTuApp.makeConnectAndShareMenu(wettkampf)
    
    val uploadMenu: MenuItem = {
      val item = makeMenuAction("Upload") {(caption, action) =>
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
          onAction = handleAction {implicit e: ActionEvent =>
            uploadResults(caption)
          }
        })
      }
      item.disable <== when(Bindings.createBooleanBinding(() => 
        
        (wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin) 
        && !ConnectionStates.connectedProperty.value)
        || isRunning(wettkampf),
        
        KuTuApp.selectedWettkampfSecret, 
        ConnectionStates.connectedProperty,
        activeDurchgaengeProp
      )) choose true otherwise false 
      item
    }

    val downloadMenu: MenuItem = KuTuApp.makeWettkampfDownloadMenu(wettkampf)
    
    val disconnectMenu = KuTuApp.makeDisconnectMenu(wettkampf)
    val removeRemoteMenu = KuTuApp.makeWettkampfRemoteRemoveMenu(wettkampf)
    
    view.contextMenu = new ContextMenu() {
      items += makeDurchgangStartenMenu(wettkampf)
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
    def updateButtons {
      val dgs = makeDurchgangStartenMenu(wettkampf)
      val dga = makeDurchgangAbschliessenMenu(wettkampf)
      view.contextMenu = new ContextMenu() {
        items += dgs
        items += dga
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
          }, new Button {
            onAction = dgs.onAction.get
            text <== dgs.text
            disable <== dgs.disable
          }, new Button {
            val act = makeDurchgangAbschliessenMenu(wettkampf)
            onAction = dga.onAction.get
            text <== dga.text
            disable <== dga.disable
          }, new Button {
            onAction = uploadMenu.onAction.get
            text <== uploadMenu.text
            disable <== uploadMenu.disable
          }, new Button {
            onAction = downloadMenu.onAction.get
            text <== downloadMenu.text
            disable <== downloadMenu.disable
          }, new Button {
            onAction = disconnectMenu.onAction.get
            text <== disconnectMenu.text
            disable <== disconnectMenu.disable
          }, new Button {
            onAction = removeRemoteMenu.onAction.get
            text <== removeRemoteMenu.text
            disable <== removeRemoteMenu.disable
          })      
    }
    updateButtons
//    val showQRCode = make
    view.selectionModel().selectedItem.onChange{
      updateButtons
    }
    content = rootpane
    true
  }
}
 