package ch.seidel.kutu

import java.awt.Desktop
import java.io.{ByteArrayInputStream, FileInputStream}
import java.net.URI
import java.util.{Base64, Date, UUID}
import java.util.concurrent.Executors

import authentikat.jwt.JsonWebToken
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.akka.KutuAppEvent
import ch.seidel.kutu.data.{CaseObjectMetaUtil, ResourceExchanger, Surname}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.{AuthSupport, JsonSupport, JwtSupport, WebSocketClient}
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.DatePicker
import javafx.util.Callback
import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType
import org.slf4j.LoggerFactory
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, ReadOnlyStringWrapper}
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos, Side}
import scalafx.scene.Node.sfxNode2jfx
import scalafx.scene.{Cursor, Node, Scene}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Label.sfxLabel2jfx
import scalafx.scene.control.MenuItem.sfxMenuItem2jfx
import scalafx.scene.control.ScrollPane.sfxScrollPane2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TextField.sfxTextField2jfx
import scalafx.scene.control.TreeItem.sfxTreeItemToJfx
import scalafx.scene.control.Tab.sfxTab2jfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{Clipboard, ClipboardContent, DataFormat}
import scalafx.scene.layout._
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Screen}
import spray.json._

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object KuTuApp extends JFXApp with KutuService with JsonSupport with JwtSupport {

  import WertungServiceBestenResult._

  private val logger = LoggerFactory.getLogger(this.getClass)
  private lazy val server = KuTuServer

  import scala.concurrent.ExecutionContext.Implicits.global

  val lazyExecutor = Executors.newScheduledThreadPool(1)

  override def stopApp() {
    lazyExecutor.shutdownNow()
    ConnectionStates.disconnected()
    server.shutDown("KuTuApp")
  }

  var tree = AppNavigationModel.create(KuTuApp.this)
  val rootTreeItem = new TreeItem[String]("Dashboard") {
    expanded = true
    children = tree.getTree
  }
  val modelWettkampfModus = new BooleanProperty()
  val selectedWettkampf = new SimpleObjectProperty[WettkampfView]()
  val selectedWettkampfSecret = new SimpleObjectProperty[Option[String]]()

  val btnWettkampfModus = new ToggleButton("Wettkampf-Modus") {
    id = "wettkampfmodusButton"
    selected <==> modelWettkampfModus
    disable = true
  }

  val btnConnectStatus = new ToggleButton() {
    //id = "connected-info"
    disable <== btnWettkampfModus.disable
    onAction = handleAction { action =>
      if (ConnectionStates.connectedProperty.value) {
        ConnectionStates.disconnected
      } else {
        connectAndShare(selectedWettkampf.value, "Share", action)
      }
      Platform.runLater {
        selected.value = ConnectionStates.connectedProperty.value
      }
    }
    //    selected <== ConnectionStates.connectedProperty
    text <== createStringBinding(() => {
      ConnectionStates.connectedWithProperty.value match {
        case "" =>
          selected.value = false
          "nicht verbunden"
        case uuid =>
          selected.value = true
          s"Verbunden mit ${readWettkampf(uuid).easyprint}"
      }
    }, ConnectionStates.connectedWithProperty)
  }
  val lblRemoteAddress = new Label() {
    id = "connected-info"
    text <== createStringBinding(() => {
      ConnectionStates.connectedWithProperty.value match {
        case "" =>
          s"Server: ${Config.remoteBaseUrl} offline\nVersion: ${Config.appFullVersion}, Built: ${Config.builddate}"
        case uuid =>
          s"Server: ${Config.remoteBaseUrl} online\nVersion: ${Config.appFullVersion}, Built: ${Config.builddate}"
      }
      //visible <== ConnectionStates.connectedProperty
    }, ConnectionStates.connectedWithProperty, LocalServerStates.localServerProperty)
  }
  var centerPane = PageDisplayer.choosePage(modelWettkampfModus, None, "dashBoard", tree)

  def updateTree {
    tree = AppNavigationModel.create(KuTuApp.this)
    rootTreeItem.children = tree.getTree
  }

  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R) = new javafx.event.EventHandler[J] {
    def handle(event: J) {
      setCursor(Cursor.Wait)
      try {
        handler(event)
      }
      finally {
        setCursor(Cursor.Default)
      }
    }
  }

  def invokeWithBusyIndicator(task: => Unit) {
    setCursor(Cursor.Wait)
    val f = Future[Boolean] {
      Thread.sleep(10L) // currentThread().wait(1L)
      Platform.runLater {
        try {
          task
        }
        finally {
          setCursor(Cursor.Default)
        }
      }
      true
    }
  }

  def invokeAsyncWithBusyIndicator[R](task: => Future[R]): Future[R] = {
    setCursor(Cursor.Wait)
    val p = Promise[R]
    val f = Future[Boolean] {
      try {
        task.onComplete {
          case Success(ret) =>
            Platform.runLater {
              try {
                p.success(ret)
              }
              catch {
                case e: Exception => p.failure(e)
              }
              finally {
                setCursor(Cursor.Default)
              }
            }
          case Failure(error) =>
            Platform.runLater {
              try {
                p.failure(error)
              }
              finally {
                setCursor(Cursor.Default)
              }
            }

        }
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
          Platform.runLater {
            try {
              p.failure(e)
            }
            finally {
              setCursor(Cursor.Default)
            }
          }
      }
      true
    }
    p.future
  }

  var cursorWaiters = 0

  def setCursor(c: Cursor) {
    val ctoSet = if (c.equals(Cursor.Wait)) {
      cursorWaiters += 1
      c
    }
    else {
      cursorWaiters -= 1
      if (cursorWaiters > 0) {
        Cursor.Wait
      }
      else {
        cursorWaiters = 0
        c
      }
    }
    //logger.debug(cursorWaiters)
    if (getStage() != null) {
      getStage().scene.delegate.getValue.cursor = ctoSet
      getStage().scene.delegate.getValue.root.value.requestLayout()
    }
  }

  class ProgrammListCell extends javafx.scene.control.ListCell[ProgrammView] {
    override def updateItem(item: ProgrammView, empty: Boolean) {
      super.updateItem(item, empty)
      if (item != null) {
        textProperty().setValue(item.easyprint)
      }
    }
  }

  val screen = Screen.primary
  val controlsView = new TreeView[String]() {
    minWidth = 200
    maxWidth = 400
    editable = true
    root = rootTreeItem
    id = "page-tree"
  }
  controlsView.onContextMenuRequested = handle {
    val sel = controlsView.selectionModel().selectedItem
    sel.value.value.value
  }

  type MenuActionHandler = (String, ActionEvent) => Unit

  def makeMenuAction(caption: String)(handler: MenuActionHandler): MenuItem = {
    new MenuItem(caption) {
      onAction = handleAction { e: ActionEvent =>
        handler(caption, e)
      }
    }
  }

  def makeWettkampfBearbeitenMenu(p: WettkampfView): MenuItem = {
    makeMenuAction("Wettkampf bearbeiten") { (caption, action) =>
      implicit val e = action
      val txtDatum = new DatePicker {
        setPromptText("Wettkampf-Datum")
        setPrefWidth(500)
        valueProperty().setValue(sqlDate2ld(p.datum))
      }
      val txtTitel = new TextField {
        prefWidth = 500
        promptText = "Wettkampf-Titel"
        text = p.titel
      }
      val cmbProgramm = new ComboBox(ObservableBuffer[ProgrammView](listRootProgramme)) {
        prefWidth = 500
        buttonCell = new ProgrammListCell
        cellFactory = new Callback[ListView[ProgrammView], ListCell[ProgrammView]]() {
          def call(p: ListView[ProgrammView]): ListCell[ProgrammView] = {
            new ProgrammListCell
          }
        }
        promptText = "Programm"
        selectionModel.value.select(p.programm)
      }
      val txtAuszeichnung = new TextField {
        prefWidth = 500
        promptText = "%-Angabe, wer eine Auszeichnung bekommt"
        if (p.auszeichnung > 100) {
          text = dbl2Str(p.auszeichnung / 100d) + "%"
        }
        else {
          text = s"${p.auszeichnung}%"
        }
      }
      val txtAuszeichnungEndnote = new TextField {
        prefWidth = 500
        promptText = "Auszeichnung bei Erreichung des Mindest-Endwerts"
        text = p.auszeichnungendnote.toString
      }
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(
                new Label(txtDatum.promptText.value), txtDatum,
                new Label(txtTitel.promptText.value), txtTitel,
                new Label(cmbProgramm.promptText.value), cmbProgramm,
                new Label(txtAuszeichnung.promptText.value), txtAuszeichnung,
                new Label(txtAuszeichnungEndnote.promptText.value), txtAuszeichnungEndnote)
            }
          }
        }
      },
        new Button("OK") {
          disable <== when(Bindings.createBooleanBinding(() => {
            cmbProgramm.selectionModel.value.getSelectedIndex == -1 ||
              txtDatum.value.isNull.value || txtTitel.text.isEmpty.value
          },
            cmbProgramm.selectionModel.value.selectedIndexProperty, txtDatum.value, txtTitel.text
          )) choose true otherwise false
          onAction = handleAction { implicit e: ActionEvent =>
            try {
              val w = saveWettkampf(
                p.id,
                ld2SQLDate(txtDatum.valueProperty().value),
                txtTitel.text.value,
                Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
                txtAuszeichnung.text.value.filter(c => c.isDigit || c == '.' || c == ',').toString match {
                  case "" => 0
                  case s: String if (s.indexOf(".") > -1 || s.indexOf(",") > -1) => math.round(str2dbl(s) * 100).toInt
                  case s: String => str2Int(s)
                },
                txtAuszeichnungEndnote.text.value match {
                  case "" => 0
                  case s: String => try {
                    s
                  } catch {
                    case e: Exception => 0
                  }
                },
                p.uuid)
              val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
              if (!dir.exists()) {
                dir.mkdirs();
              }
              updateTree
              val text = s"${w.titel} ${w.datum}"
              tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                case Some(node) =>
                  controlsView.selectionModel().select(node)
                case None =>
              }
            }
            catch {
              case e: IllegalArgumentException =>
                new Alert(AlertType.Error, e.getMessage).showAndWait()
            }
          }
        }
      )
    }
  }

  def makeWettkampfExportierenMenu(p: WettkampfView): MenuItem = {
    makeMenuAction("Wettkampf exportieren") { (caption, action) =>
      implicit val e = action
      val fileChooser = new FileChooser {
        title = "Wettkampf File exportieren"
        initialDirectory = new java.io.File(homedir)
        extensionFilters ++= Seq(
          new ExtensionFilter("Zip-Files", "*.zip"),
          new ExtensionFilter("All Files", "*.*")
        )
        initialFileName.value = p.titel.replace(" ", "_") + "_" + DBService.sdfYear.format(p.datum) + ".zip"
      }
      val selectedFile = fileChooser.showSaveDialog(stage)
      if (selectedFile != null) {
        KuTuApp.invokeWithBusyIndicator {
          val file = if (!selectedFile.getName.endsWith(".zip")) {
            new java.io.File(selectedFile.getAbsolutePath + ".zip")
          }
          else {
            selectedFile
          }
          ResourceExchanger.exportWettkampf(p.toWettkampf, file.getPath);
        }
      }
    }
  }

  def makeWettkampfUploadMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Upload") { (caption, action) =>
      implicit val e = action
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              if (p.toWettkampf.hasSecred(homedir, remoteHostOrigin)) {
                children.addAll(new Label("Die Resultate zu diesem Wettkampf werden im Netzwerk hochgeladen und ersetzen dort die Resultate, die zu diesem Wettkampf erfasst wurden."))
              } else {
                children.addAll(new Label("Die Resultate zu diesem Wettkampf werden neu im Netzwerk bereitgestellt."))
              }
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { implicit e: ActionEvent =>
            val process = KuTuApp.invokeAsyncWithBusyIndicator {
              if (remoteBaseUrl.indexOf("localhost") > -1) {
                server.startServer { uuid => server.sha256(uuid) }
              }
              server.httpUploadWettkampfRequest(p.toWettkampf)
            }
            process.onComplete { resultTry =>
              Platform.runLater {
                val feedback = resultTry match {
                  case Success(response) =>
                    selectedWettkampfSecret.value = p.toWettkampf.readSecret(homedir, remoteHostOrigin)
                    s"Der Wettkampf ${p.easyprint} wurde erfolgreich im Netzwerk bereitgestellt"
                  case Failure(error) => error.getMessage.replace("(", "(\n")
                }
                implicit val e = action
                PageDisplayer.showInDialog(caption, new DisplayablePage() {
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
        })

    }
    item.disable <== when(Bindings.createBooleanBinding(() => p.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !ConnectionStates.connectedProperty.value,
      selectedWettkampfSecret, ConnectionStates.connectedProperty,
      controlsView.selectionModel().selectedItem)) choose true otherwise false
    item
  }

  def makeWettkampfRemoteRemoveMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Wettkampf im Netzwerk entfernen") { (caption, action) =>
      val process = KuTuApp.invokeAsyncWithBusyIndicator {
        Future {
          server.httpRemoveWettkampfRequest(p.toWettkampf)
          ConnectionStates.disconnected()
        }
      }
      process.onComplete { resultTry =>
        Platform.runLater {
          val feedback = resultTry match {
            case Success(response) =>
              selectedWettkampfSecret.value = p.toWettkampf.readSecret(homedir, remoteHostOrigin)
              "Wettkampf im Netzwerk entfernt."
            case Failure(error) => error.getMessage.replace("(", "(\n")
          }
          implicit val e = action
          PageDisplayer.showInDialog(caption, new DisplayablePage() {
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
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin)
        || modelWettkampfModus.value
        || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),

      controlsView.selectionModel().selectedItem,
      ConnectionStates.connectedWithProperty,
      selectedWettkampfSecret,
      modelWettkampfModus,
      ConnectionStates.connectedProperty
    )) choose true otherwise false
    item
  }

  def makeWettkampfDownloadMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Download") { (caption, action) =>
      implicit val e = action
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(new Label("Alle Einteilungen und Resultate zu diesem Wettkampf\nwerden mit denjenigen aus dem Netzwerk ersetzt."))
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { implicit e: ActionEvent =>
            KuTuApp.invokeWithBusyIndicator {
              val url = s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}"
              val response = server.httpDownloadRequest(server.makeHttpGetRequest(url))
              response.onComplete(ft =>
                Platform.runLater {
                  ft match {
                    case Success(w) =>
                      updateTree
                      val text = s"${w.titel} ${w.datum}"
                      tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                        case Some(node) => controlsView.selectionModel().select(node)
                        case None =>
                      }
                    case Failure(error) => PageDisplayer.showErrorDialog(caption)(error)
                  }
                }
              )
              Await.result(response, Duration.Inf)
            }
          }
        })
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  def makeWettkampfDurchfuehrenMenu(p: WettkampfView): MenuItem = {
    if (!modelWettkampfModus.value) {
      makeMenuAction("Wettkampf durchführen") { (caption, action) =>
        modelWettkampfModus.value = true
      }
    }
    else {
      makeMenuAction("Wettkampf-Modus beenden") { (caption, action) =>
        modelWettkampfModus.value = false
      }
    }
  }

  def makeNeuerVereinAnlegenMenu = makeMenuAction("Neuen Verein anlegen ...") { (caption, action) =>
    implicit val e = action

    val txtVereinsname = new TextField {
      prefWidth = 500
      promptText = "Vereinsname"
    }

    val txtVerband = new TextField {
      prefWidth = 500
      promptText = "Verband"
    }
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(
              new Label(txtVereinsname.promptText.value), txtVereinsname,
              new Label(txtVerband.promptText.value), txtVerband
            )
          }
        }
      }
    }, new Button("OK") {
      disable <== when(Bindings.createBooleanBinding(() => {
        txtVereinsname.text.isEmpty.value
      },
        txtVereinsname.text
      )) choose true otherwise false
      onAction = handleAction { implicit e: ActionEvent =>
        val vid = createVerein(
          txtVereinsname.text.value.trim(),
          txtVerband.text.value match {
            case "" => None
            case s => Some(s.trim())
          }
        )
        updateTree
        selectVereine.find {
          _.id == vid
        } match {
          case Some(verein) =>
            val text = s"${verein.name}"
            tree.getLeaves("Athleten").find { item => text.equals(item.value.value) } match {
              case Some(node) =>
                controlsView.selectionModel().select(node)
              case None =>
            }
          case None =>
        }
      }
    })
  }

  def makeFindDuplicteAthletes = makeMenuAction("Doppelt erfasste Athleten finden ...") { (caption, action) =>
    implicit val e = action
    KuTuApp.invokeAsyncWithBusyIndicator {
      Future {

        def mapSexPrediction(athlet: AthletView) = Surname
          .isSurname(athlet.vorname)
          .map { sn => if (sn.isMasculin == sn.isFeminin) athlet.geschlecht else if (sn.isMasculin) "M" else "W" }
          .getOrElse("X")

        val likeFinder = findAthleteLike(new java.util.ArrayList[MatchCode]) _
        for {
          athleteView <- selectAthletesView
          athlete = athleteView.toAthlet
          like = likeFinder(athlete)
          if (athleteView.id != like.id)
        } yield {
          val tupel = List(athleteView, loadAthleteView(like.id)).sortWith { (a, b) =>
            if (a.gebdat.map(_.toLocalDate.getDayOfMonth).getOrElse(0) > b.gebdat.map(_.toLocalDate.getDayOfMonth).getOrElse(0)) true
            else {
              val asp = mapSexPrediction(a)
              val bsp = mapSexPrediction(b)
              if (asp == a.geschlecht && bsp != b.geschlecht) true
              else if (bsp == b.geschlecht && asp != a.geschlecht) false
              else if (a.id - b.id > 0) true
              else false
            }
          }
          (tupel(0), tupel(1), CaseObjectMetaUtil.mergeMissingProperties(tupel(0), tupel(1)))
        }
      }
    }.onComplete {
      case Failure(t) => logger.debug(t.toString)
      case Success(candidates) => Platform.runLater {
        def cleanMirrorTuples = {
          candidates.foldLeft(List[(AthletView, AthletView, AthletView)]()) { (acc, triple) =>
            acc.find(acctriple => acctriple._1 == triple._1 || acctriple._2 == triple._1) match {
              case Some(_) => acc
              case _ => acc :+ triple
            }
          }
        }

        val athletModel = ObservableBuffer[(AthletView, AthletView, AthletView)](cleanMirrorTuples)

        def printAthlet(athlet: AthletView) = athlet.geschlecht match {
          case "W" => s"Ti ${athlet.easyprint}"
          case _ => s"Tu ${athlet.easyprint}"
        }

        val athletTable = new TableView[(AthletView, AthletView, AthletView)](athletModel) {
          columns ++= List(
            new TableColumn[(AthletView, AthletView, AthletView), String] {
              text = "Athlet"
              minWidth = 220
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "athlet", {
                  printAthlet(x.value._1)
                })
              }
              minWidth = 220
            },
            new TableColumn[(AthletView, AthletView, AthletView), String] {
              text = "Athlet-Duplette"
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "athlet", {
                  printAthlet(x.value._2)
                })
              }
            },
            new TableColumn[(AthletView, AthletView, AthletView), String] {
              text = "Zusammenlege-Vorschlag"
              minWidth = 220
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "dbmatch", {
                  printAthlet(x.value._3)
                })
              }
            }
          )
        }
        PageDisplayer.showInDialog(caption, new DisplayablePage() {
          def getPage: Node = {
            new BorderPane {
              hgrow = Priority.Always
              vgrow = Priority.Always
              minWidth = 800
              center = new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                //              top = filter
                center = athletTable
                minWidth = 680
              }
              right = new VBox {
                margin = Insets(0, 0, 0, 10)
                children += new Button("Vorschlag austauschen") {
                  disable <== when(athletTable.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
                  onAction = (event: ActionEvent) => {
                    val index = athletTable.selectionModel.value.getSelectedIndex
                    val (athlet1, athlet2, _) = athletTable.selectionModel.value.selectedItem.value
                    athletModel.update(index, (athlet2, athlet1, CaseObjectMetaUtil.mergeMissingProperties(athlet2, athlet1)))
                  }
                }
              }
            }
          }
        },

          new Button("OK, zusammenlegen") {
            disable <== when(athletTable.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
            onAction = (event: ActionEvent) => {
              val (athlet1, athlet2, athlet3) = athletTable.selectionModel.value.selectedItem.value
              mergeAthletes(athlet2.id, athlet1.id)
              insertAthlete(athlet3.toAthlet)
            }
          },

          new Button("OK alle, zusammenlegen") {
            disable <== when(createBooleanBinding(() => athletModel.isEmpty, athletModel)) choose true otherwise false
            onAction = (event: ActionEvent) => {
              athletModel.foreach { case (athlet1, athlet2, athlet3) => {
                mergeAthletes(athlet2.id, athlet1.id)
              }
                insertAthlete(athlet3.toAthlet)
              }
            }
          })
      }
    }
  }

  def makeStartServerMenu = {
    val item = makeMenuAction("Start local Server") { (caption, action) =>
      KuTuApp.invokeWithBusyIndicator {
        server.startServer { x => server.sha256(x) }
        LocalServerStates.startLocalServer
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() => isLocalHostServer(),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      LocalServerStates.localServerProperty,
      ConnectionStates.connectedWithProperty)) choose true otherwise false
    item
  }

  def makeStopServerMenu = {
    val item = makeMenuAction("Stop local Server") { (caption, action) =>
      KuTuApp.invokeWithBusyIndicator {
        LocalServerStates.stopLocalServer
        server.stopServer("user stops local server")
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() => {
      isLocalHostServer()
    },
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      LocalServerStates.localServerProperty,
      ConnectionStates.connectedWithProperty)) choose false otherwise true
    item
  }

  def makeProxyLoginMenu = {
    val item = makeMenuAction("Internet Proxy ...") { (caption, action) =>
      implicit val e = action
      val txtProxyAddress = new TextField {
        prefWidth = 500
        promptText = "Proxy Adresse"
        text = Config.proxyHost.getOrElse("")
      }
      val txtProxyPort = new TextField {
        prefWidth = 500
        promptText = "Proxy Port"
        text = Config.proxyPort.getOrElse("")
      }
      val txtUsername = new TextField {
        prefWidth = 500
        promptText = "Username"
        text = System.getProperty("user.name")
      }

      val txtPassword = new PasswordField {
        prefWidth = 500
        promptText = "Internet Proxy Passwort"
      }
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(
                new Label(txtProxyAddress.promptText.value), txtProxyAddress,
                new Label(txtProxyPort.promptText.value), txtProxyPort,
                new Label(txtUsername.promptText.value), txtUsername,
                new Label(txtPassword.promptText.value), txtPassword
              )
            }
          }
        }
      }, new Button("OK") {
        disable <== when(Bindings.createBooleanBinding(() => {
          txtUsername.text.isEmpty.value && txtPassword.text.isEmpty().value
        },
          txtUsername.text, txtPassword.text
        )) choose true otherwise false
        onAction = handleAction { implicit e: ActionEvent =>
          server.setProxyProperties(
            host = txtProxyAddress.text.value.trim(),
            port = txtProxyPort.text.value.trim(),
            user = txtUsername.text.value.trim(),
            password = txtPassword.text.value.trim())
        }
      })
    }
    item.disable <== when(Bindings.createBooleanBinding(() => {
      isLocalHostServer()
    },
      LocalServerStates.localServerProperty)) choose true otherwise false
    item
  }

  def makeDisconnectMenu(p: WettkampfView) = {
    val item = makeMenuAction("Verbindung stoppen") {(caption, action) =>
      ConnectionStates.disconnected
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      ConnectionStates.connectedWithProperty
      )) choose true otherwise false
    item
  }

  def connectAndShare(p: WettkampfView, caption: String, action: ActionEvent) = {
    implicit val e = action
    val process = KuTuApp.invokeAsyncWithBusyIndicator{
//      if (remoteBaseUrl.indexOf("localhost") > -1) {
//        server.startServer { uuid => server.sha256(uuid) }
//      }
      if (Config.isLocalHostServer()) {
        if (!p.toWettkampf.hasSecred(homedir, "localhost")) {
          p.toWettkampf.saveSecret(homedir, "localhost",  JsonWebToken(jwtHeader, setClaims(p.uuid.get, Int.MaxValue), jwtSecretKey))
        }
        server.httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", p.uuid.get, p.toWettkampf.readSecret(homedir, "localhost").get)
      } else {
        p.uuid.zip(p.toWettkampf.readSecret(homedir, remoteHostOrigin)).headOption match {
          case Some((uuid, secret)) =>
            server.httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
          case None =>
            server.httpUploadWettkampfRequest(p.toWettkampf)
        }
      }
    }.map(response => {
      (response, WebSocketClient.connect(p.toWettkampf, ResourceExchanger.processWSMessage(p.toWettkampf, (sender: Object, event: KutuAppEvent) => {
        Platform.runLater{
          WebSocketClient.modelWettkampfWertungChanged.setValue(event)
        }
      }), PageDisplayer.showErrorDialog(caption)))
    })
    process.onComplete{
      case Success((response, wspromise)) =>
        ConnectionStates.connectedWith(p.uuid.get, wspromise)
        Platform.runLater{
        PageDisplayer.showInDialog(caption,
          new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                center = new VBox {
                  children.addAll(new Label(s"Verbindung mit ${p.easyprint} erfolgreich hergestellt."))
                }
              }
            }
          }
        )
        }
      case Failure(error) => PageDisplayer.showErrorDialog(caption)(error)
    }
  }

  def makeConnectAndShareMenu(p: WettkampfView) = {
    val item = makeMenuAction("Verbinden ...") {(caption, action) =>
      connectAndShare(p, caption, action)
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      ConnectionStates.connectedWithProperty
    )) choose false otherwise true
    item
  }

  def makeNeuerWettkampfAnlegenMenu: MenuItem = {
    makeMenuAction("Neuen Wettkampf anlegen ...") {(caption, action) =>
      implicit val e = action
      val txtDatum = new DatePicker {
        setPromptText("Wettkampf-Datum")
        setPrefWidth(500)
      }
      val txtTitel = new TextField {
        prefWidth = 500
        promptText = "Wettkampf-Titel"
      }
      val cmbProgramm = new ComboBox(ObservableBuffer[ProgrammView](listRootProgramme)) {
        prefWidth = 500
        buttonCell = new ProgrammListCell
        cellFactory = new Callback[ListView[ProgrammView], ListCell[ProgrammView]]() {
          def call(p: ListView[ProgrammView]): ListCell[ProgrammView] = {
            new ProgrammListCell
          }
        }
        promptText = "Programm"
      }
      val txtAuszeichnung = new TextField {
        prefWidth = 500
        promptText = "%-Angabe, wer eine Auszeichnung bekommt"
        text = "40.00%"
      }
      val txtAuszeichnungEndnote = new TextField {
        prefWidth = 500
        promptText = "Auszeichnung bei Erreichung des Mindest-Gerätedurchschnittwerts"
        text = ""
      }
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(
                  new Label(txtDatum.promptText.value), txtDatum,
                  new Label(txtTitel.promptText.value), txtTitel,
                  new Label(cmbProgramm.promptText.value), cmbProgramm,
                  new Label(txtAuszeichnung.promptText.value), txtAuszeichnung,
                  new Label(txtAuszeichnungEndnote.promptText.value), txtAuszeichnungEndnote)
            }
          }
        }
      }, new Button("OK") {
        disable <== when(Bindings.createBooleanBinding(() => {
                            cmbProgramm.selectionModel.value.getSelectedIndex == -1 ||
                            txtDatum.value.isNull.value || txtTitel.text.isEmpty.value
                          },
                            cmbProgramm.selectionModel.value.selectedIndexProperty, txtDatum.value, txtTitel.text
                          )) choose true otherwise false
        onAction = handleAction {implicit e: ActionEvent =>
          val w = createWettkampf(
              ld2SQLDate(txtDatum.valueProperty().value),
              txtTitel.text.value,
              Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
              txtAuszeichnung.text.value.filter(c => c.isDigit || c == '.' || c == ',').toString match {
                case ""        => 0
                case s: String if(s.indexOf(".") > -1 || s.indexOf(",") > -1) => math.round(str2dbl(s) * 100).toInt
                case s: String => str2Int(s)
              },
              txtAuszeichnungEndnote.text.value match {
                case ""        => 0
                case s: String => try {BigDecimal.valueOf(s)} catch {case e:Exception => 0}
              },
              Some(UUID.randomUUID().toString()))
           val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
           if(!dir.exists()) {
             dir.mkdirs();
           }
           updateTree
           val text = s"${w.titel} ${w.datum}"
           tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
             case Some(node) =>
               controlsView.selectionModel().select(node)
             case None =>
           }
        }
      })
    }
  }
  def makeWettkampfHerunterladenMenu: MenuItem = {
    import DefaultJsonProtocol._
    val item = makeMenuAction("Wettkampf herunterladen") {(caption, action) =>
      implicit val e = action
      val wklist = server.httpGet(s"${remoteAdminBaseUrl}/api/competition").map{
        case entityString: String => entityString.asType[List[Wettkampf]]
        case _ => List[Wettkampf]()
      }
      wklist.onComplete{
        case Success(wkl) =>
          Platform.runLater{
            val filteredModel = ObservableBuffer[Wettkampf](wkl)
            val wkTable = new TableView[Wettkampf](filteredModel) {
              columns ++= List(
                new TableColumn[Wettkampf, String] {
                  text = "Datum"
                  cellValueFactory = { x =>
                    new ReadOnlyStringWrapper(x.value, "datum", {
                      s"${x.value.datum}"
                    })
                  }
                  minWidth = 250
                },
                new TableColumn[Wettkampf, String] {
                  text = "Titel"
                  cellValueFactory = { x =>
                    new ReadOnlyStringWrapper(x.value, "titel", {
                     s"${x.value.titel}"
                    })
                  }
                }
              )
            }
            wkTable.selectionModel.value.setSelectionMode(SelectionMode.Single)
            val filter = new TextField() {
              promptText = "Such-Text"
              text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
                val sortOrder = wkTable.sortOrder.toList;
                filteredModel.clear()
                val searchQuery = newVal.toUpperCase().split(" ")
                for{wettkampf <- wkl
                } {
                  val matches = searchQuery.forall{search =>
                    if(search.isEmpty() || wettkampf.easyprint.toUpperCase().contains(search)) {
                      true
                    }
                    else {
                      false
                    }
                  }

                  if(matches) {
                    filteredModel.add(wettkampf)
                  }
                }
                wkTable.sortOrder.clear()
                val restored = wkTable.sortOrder ++= sortOrder
              }
            }
            PageDisplayer.showInDialog(caption, new DisplayablePage() {
              def getPage: Node = {
                new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  minWidth = 600
                  center = new BorderPane {
                    hgrow = Priority.Always
                    vgrow = Priority.Always
                    top = filter
                    center = wkTable
                    minWidth = 550
                  }

                }
              }
            }, new Button("OK") {
              disable <== when(wkTable.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
              onAction = (event: ActionEvent) => {
                if (!wkTable.selectionModel().isEmpty) {
                  val selectedAthleten = wkTable.items.value.zipWithIndex.filter {
                    x => wkTable.selectionModel.value.isSelected(x._2)
                  }.map {x =>
                    val (wettkampf,idx) = x
                    KuTuApp.invokeWithBusyIndicator {
                      val url=s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}"
                      server.httpDownloadRequest(server.makeHttpGetRequest(url)).onComplete(ft =>
                        Platform.runLater{ ft match {
                            case Success(w) =>
                              updateTree
                              val text = s"${w.titel} ${w.datum}"
                              tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                                case Some(node) => controlsView.selectionModel().select(node)
                                case None =>
                              }
                            case Failure(error) => PageDisplayer.showErrorDialog(caption)(error)
                          }
                        }
                      )
                    }
                  }
                }
              }
            }
          )
        }

        case Failure(error) => PageDisplayer.showErrorDialog(caption)(error)
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() => {
      isLocalHostServer()
    },
      LocalServerStates.localServerProperty)) choose true otherwise false
    item
  }

  def makeNeuerWettkampfImportierenMenu: MenuItem = {
    makeMenuAction("Wettkampf importieren") {(caption, action) =>
      implicit val e = action
      val fileChooser = new FileChooser {
         title = "Wettkampf File importieren"
         initialDirectory = new java.io.File(homedir)
         extensionFilters ++= Seq(
           new ExtensionFilter("Zip-Files", "*.zip"),
           new ExtensionFilter("All Files", "*.*")
         )
        }
        val selectedFile = fileChooser.showOpenDialog(stage)
        import scala.concurrent.ExecutionContext.Implicits._
        if (selectedFile != null) {
          val wf = KuTuApp.invokeAsyncWithBusyIndicator[Wettkampf] {
            Future[Wettkampf]{
              val is = new FileInputStream(selectedFile)
              val w = ResourceExchanger.importWettkampf(is)
              is.close()
              val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
              if(!dir.exists()) {
                dir.mkdirs();
              }
              w
            }
          }
          wf.onComplete {tr =>
            Platform.runLater{ tr match {
              case Failure(f) => logger.debug(f.toString)
                PageDisplayer.showInDialog(caption, new DisplayablePage() {
                  def getPage: Node = {
                    new BorderPane {
                      hgrow = Priority.Always
                      vgrow = Priority.Always
                      center = new VBox {
                        children.addAll(new Label(f.toString))
                      }
                    }
                  }
                })
              case Success(w) =>
                  updateTree
                  val text = s"${w.titel} ${w.datum}"
                  tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                    case Some(node) =>
                      controlsView.selectionModel().select(node)
                    case None =>
                  }
            }}
          }
        }
    }
  }

  val enc = Base64.getUrlEncoder
  def makeShowQRCodeMenu(p: WettkampfView) = {
    val item = makeMenuAction("Mobile App Connections ...") {(caption, action) =>
      showQRCode(caption, p)
    }
    item.disable <== when(Bindings.createBooleanBinding(() => !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
        ConnectionStates.connectedWithProperty, selectedWettkampfSecret, controlsView.selectionModel().selectedItem)) choose true otherwise false
    item
  }

  def showQRCode(caption: String, p: WettkampfView) = {
      val secretOrigin = if (Config.isLocalHostServer()) {
        if (!p.toWettkampf.hasSecred(homedir, "localhost")) {
          p.toWettkampf.saveSecret(homedir, "localhost",  JsonWebToken(jwtHeader, setClaims(p.uuid.get, Int.MaxValue), jwtSecretKey))
        }
        "localhost"
      } else remoteHostOrigin
      p.uuid.zip(AuthSupport.getClientSecret).zip(p.toWettkampf.readSecret(homedir, secretOrigin)).headOption match {
        case Some(((uuid, shortsecret), secret)) =>
          val shorttimeout = getExpiration(shortsecret).getOrElse(new Date())
          val longtimeout = getExpiration(secret).getOrElse(new Date())

          val connectionList = if(Config.isLocalHostServer()) {
            server.listNetworkAdresses.toList
          } else List(remoteBaseUrl)
          val tablist = connectionList.map{address =>
            val connectionString = s"$address/?" + new String(enc.encodeToString((s"c=$uuid&s=$secret").getBytes))
            val shortConnectionString = s"$address/?" + new String(enc.encodeToString((s"c=$uuid&s=$shortsecret").getBytes))
            val lastResultsConnectionString = s"$address/?" + new String(enc.encodeToString((s"last&c=$uuid").getBytes))
            val topResultsConnectionString = s"$address/?" + new String(enc.encodeToString((s"top&c=$uuid").getBytes))

            def formatDateTime(d: Date) = f"$d%td.$d%tm.$d%tY - $d%tH:$d%tM"

            println(("longterm-secret", formatDateTime(longtimeout), connectionString))
            println(("shortterm-secret", formatDateTime(shorttimeout), shortConnectionString))
            val out = QRCode.from(shortConnectionString).to(ImageType.PNG).withSize(500, 500).stream()
            val in = new ByteArrayInputStream(out.toByteArray())

            val image = new Image(in)
            val view = new ImageView(image)
            val urlLabel = new Hyperlink("Link (24h gültig) im Browser öffnen")
            urlLabel.onMouseClicked = handle {
              Clipboard.systemClipboard.content = ClipboardContent(
                DataFormat.PlainText -> shortConnectionString,
                DataFormat.Html -> s"<a href='$shortConnectionString' target='_blank'>Link (24h gültig) im Browser öffnen</a> text"
              )
              Desktop.getDesktop().browse(new URI(shortConnectionString))
            }
            val mailLabel = new Hyperlink("Link (24h gültig) als EMail versenden")
            mailLabel.onMouseClicked = handle {
              val mailURIStr = String.format("mailto:%s?subject=%s&cc=%s&body=%s",
                "", encodeURIParam(s"Link für Datenerfassung im Wettkampf (${p.easyprint})"), "", encodeURIParam(
              s"""  Geschätze(r) Wertungsrichter(in)
                 |
                 |  mit dem folgenden Link kommst Du in die App, in der Du die Wettkampf-Resultate
                 |  für den Wettkampf '${p.easyprint}' erfassen kannst:
                 |  ${shortConnectionString}
                 |
                 |  Wichtig:
                 |  * Dieser Link ist bis am ${formatDateTime(shorttimeout)} Uhr gültig.
                 |  * Bitte den Link vertraulich behandeln - nur Du darfst mit diesem Link einsteigen.
                 |
                 |  Sportliche Grüsse,
                 |  Wertungsrichter-Einsatzplanung
                """.stripMargin))
              val mailURI = new URI(mailURIStr)
              Desktop.getDesktop().mail(mailURI)
            }

            val outLast = QRCode.from(lastResultsConnectionString).to(ImageType.PNG).withSize(500, 500).stream()
            val inLast = new ByteArrayInputStream(outLast.toByteArray())

            val imageLast = new Image(inLast)
            val viewLast = new ImageView(imageLast)
            val urlLabelLast = new Hyperlink("Letzte Resultate Link im Browser öffnen")
            urlLabelLast.onMouseClicked = handle {
              Clipboard.systemClipboard.content = ClipboardContent(
                DataFormat.PlainText -> lastResultsConnectionString,
                DataFormat.Html -> s"<a href='$lastResultsConnectionString' target='_blank'>Letzte Resultate Link im Browser öffnen</a> text"
              )
              Desktop.getDesktop().browse(new URI(lastResultsConnectionString))
            }
            val mailLabelLast = new Hyperlink("Letzte Resultate Link als EMail versenden")
            mailLabelLast.onMouseClicked = handle {
              val mailURIStr = String.format("mailto:%s?subject=%s&cc=%s&body=%s",
                "", encodeURIParam(s"Link auf die Anzeige der letzten Resultate im Wettkampf (${p.easyprint})"), "", encodeURIParam(
                  s"""  Guten Tag
                     |
                     |  mit dem folgenden Link kommst Du in die App, in der Du die aktuellsten Resultate
                     |  für den Wettkampf '${p.easyprint}' verfolgen kannst:
                     |  ${lastResultsConnectionString}
                     |
                     |  Sportliche Grüsse,
                     |  Einsatzplanung
                """.stripMargin))
              val mailURI = new URI(mailURIStr)
              Desktop.getDesktop().mail(mailURI)
            }


            val outTop = QRCode.from(topResultsConnectionString).to(ImageType.PNG).withSize(500, 500).stream()
            val inTop = new ByteArrayInputStream(outTop.toByteArray())

            val imageTop = new Image(inTop)
            val viewTop = new ImageView(imageTop)
            val urlLabelTop = new Hyperlink("Top Resultate Link im Browser öffnen")
            urlLabelTop.onMouseClicked = handle {
              Clipboard.systemClipboard.content = ClipboardContent(
                DataFormat.PlainText -> topResultsConnectionString,
                DataFormat.Html -> s"<a href='$topResultsConnectionString' target='_blank'>Top Resultate im Browser öffnen</a> text"
              )
              Desktop.getDesktop().browse(new URI(topResultsConnectionString))
            }
            val mailLabelTop = new Hyperlink("Top Resultate Link als EMail versenden")
            mailLabelTop.onMouseClicked = handle {
              val mailURIStr = String.format("mailto:%s?subject=%s&cc=%s&body=%s",
                "", encodeURIParam(s"Link Top Resultate im Wettkampf (${p.easyprint})"), "", encodeURIParam(
                  s"""  Guten Tag
                     |
                     |  mit dem folgenden Link kommst Du in die App, in der Du die Top-Resultate (Bestenliste)
                     |  für den Wettkampf '${p.easyprint}' verfolgen kannst:
                     |  ${topResultsConnectionString}
                     |
                     |  Sportliche Grüsse,
                     |  Einsatzplanung
                """.stripMargin))
              val mailURI = new URI(mailURIStr)
              Desktop.getDesktop().mail(mailURI)
            }
            val urlLastLabel = new Hyperlink("Link auf 'Letzte Resultate'")
            urlLastLabel.onMouseClicked = handle {
              Clipboard.systemClipboard.content = ClipboardContent(
                DataFormat.PlainText -> lastResultsConnectionString,
                DataFormat.Html -> s"<a href='$lastResultsConnectionString' target='_blank'>Link auf 'Letzte Resultate'</a> text"
              )
              Desktop.getDesktop().browse(new URI(lastResultsConnectionString))
            }
            val urlTopLabel = new Hyperlink("Link auf 'Top-Resultate'")
            urlTopLabel.onMouseClicked = handle {
              Clipboard.systemClipboard.content = ClipboardContent(
                DataFormat.PlainText -> topResultsConnectionString,
                DataFormat.Html -> s"<a href='$topResultsConnectionString' target='_blank'>Link auf 'Top-Resultate'</a> text"
              )
              Desktop.getDesktop().browse(new URI(topResultsConnectionString))
            }
            view.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
            viewTop.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
            viewLast.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
            new Tab {
              text = address
              closable = false
              content = new TabPane{
                side = Side.Left
                rotateGraphic = false
                tabs.add(new Tab {
                  text = "Mobile-App Link"
                  closable = false
                  content = new VBox {
                    spacing = 15
//                    children += new Label("QR-Code mit Link (24h gültig)")
                    children += view
                    children += urlLabel
                    children += mailLabel
                  }
                })
                tabs.add( new Tab {
                  text = "Letzte Resultate Link"
                  closable = false
                  content = new VBox{
                    spacing = 15
//                    children += new Label("QR-Code mit Link")
                    children += viewLast
                    children += urlLastLabel
                    children += mailLabelLast
                  }
                })
                tabs.add(new Tab {
                  text = "Top Resultate Link"
                  closable = false
                  content = new VBox{
                    spacing = 15
//                    children += new Label("QR-Code mit Link")
                    children += viewTop
                    children += urlTopLabel
                    children += mailLabelTop
                  }
                })
              }
            }
          }

          PageDisplayer.showInDialogFromRoot(caption, new DisplayablePage() {
            def getTabbedPage: Node = new TabPane{
              tablist.foreach(tabs.add(_))
            }
            def getPage: Node = getTabbedPage
          }
          )

        case None =>
          PageDisplayer.showInDialogFromRoot(caption, new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                center = new VBox {
                  children.addAll(new Label("Dieser Wettkampf ist noch nicht geshared und steht nicht im Netzwerk bereit."))
                }
              }
            }
          }
          )
      }
  }

  def makeWettkampfLoeschenMenu(p: WettkampfView) = makeMenuAction("Wettkampf löschen") {(caption, action) =>
    implicit val e = action
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(new Label("Mit dem Wettkampf werden auch die zugehörigen Wettkampfresultate der Athleten gelöscht."))
          }
        }
      }
    },
    new Button("OK") {
        onAction = handleAction {implicit e: ActionEvent =>
          deleteWettkampf(p.id)
          updateTree
        }
      }
    )
  }

  def makeWettkampfDataDirectoryMenu(w: WettkampfView) = makeMenuAction("Wettkampf Verzeichnis öffnen") {(caption, action) =>
    val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
    if(!dir.exists()) {
      dir.mkdirs();
    }
    Desktop.getDesktop().open(dir);
  }

  def makeVereinLoeschenMenu(v: Verein) = makeMenuAction("Verein löschen") {(caption, action) =>
    implicit val e = action
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(new Label("Mit dem Verein werden auch alle dessen Athleten inkl. deren Wettkampfresultaten gelöscht."))
          }
        }
      }
    },
    new Button("OK") {
      onAction = handleAction {implicit e: ActionEvent =>
        deleteVerein(v.id)
        updateTree
      }
    })
  }

  def makeVereinUmbenennenMenu(v: Verein) = makeMenuAction("Verein umbenennen") {(caption, action) =>
    implicit val e = action
    val txtVereinsname = new TextField {
    	prefWidth = 500
    			promptText = "Neuer Vereinsname"
    			text = v.name
    }
    val txtVerband = new TextField {
      prefWidth = 500
      promptText = "Verband"
      text = v.verband.getOrElse("")
    }
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(
                new Label(txtVereinsname.promptText.value), txtVereinsname,
                new Label(txtVerband.promptText.value), txtVerband
            )
          }
        }
      }
    },
    new Button("OK") {
      disable <== when(Bindings.createBooleanBinding(() => {
                            txtVereinsname.text.isEmpty.value
                          },
                            txtVereinsname.text
                          )) choose true otherwise false
      onAction = handleAction {implicit e: ActionEvent =>
        updateVerein(v.copy(
            name = txtVereinsname.text.value.trim(),
            verband = txtVerband.text.value match {
              case "" => None
              case s => Some(s.trim())
            }
            ))
        updateTree
        val text = txtVereinsname.text.value
        tree.getLeaves("Athleten").find { item => item.value.value.contains(text) } match {
          case Some(node) =>
            controlsView.selectionModel().select(node)
          case None =>
        }
      }
    })
  }

  controlsView.selectionModel().selectionMode = SelectionMode.Single
  controlsView.selectionModel().selectedItem.onChange { (_, _, newItem) =>
    btnWettkampfModus.disable.value = true
    if(newItem != null) {
      newItem.value.value match {
        case "Athleten" =>
          controlsView.contextMenu = new ContextMenu() {
            items += makeNeuerVereinAnlegenMenu
            items += makeFindDuplicteAthletes
          }
        case "Wettkämpfe" =>
          controlsView.contextMenu = new ContextMenu() {
            items += makeNeuerWettkampfAnlegenMenu
            items += makeNeuerWettkampfImportierenMenu
            items += new Menu("Netzwerk") {
              //items += makeLoginMenu
              items += makeStartServerMenu
              items += makeStopServerMenu
              items += makeProxyLoginMenu
              items += makeWettkampfHerunterladenMenu
            }
          }
        case _ => (newItem.isLeaf, Option(newItem.getParent)) match {
            case (true, Some(parent)) => {
              tree.getThumbs(parent.getValue).find(p => p.button.text.getValue.equals(newItem.getValue)) match {
                case Some(KuTuAppThumbNail(p: WettkampfView, _, newItem)) =>
                  btnWettkampfModus.disable.value = false
                  selectedWettkampf.value = p
                  selectedWettkampfSecret.value = p.toWettkampf.readSecret(homedir, remoteHostOrigin)
//                  val networkMenu = new Menu("Netzwerk") {
//                    items += makeShowQRCodeMenu(p)
//                    items += makeWettkampfUploadMenu(p)
//                    items += makeConnectAndShareMenu(p)
//                    items += makeWettkampfDownloadMenu(p)    
//                    items += makeDisconnectMenu(p)
//                    items += makeWettkampfRemoteRemoveMenu(p)
//                  }
                  
                  controlsView.contextMenu = new ContextMenu() {
                    items += makeWettkampfDurchfuehrenMenu(p)
                    items += makeWettkampfBearbeitenMenu(p)
                    items += makeWettkampfExportierenMenu(p)
                    items += makeWettkampfDataDirectoryMenu(p)
                    items += makeWettkampfLoeschenMenu(p)
//                    items += networkMenu
                  }
                case Some(KuTuAppThumbNail(v: Verein, _, newItem)) =>
                  controlsView.contextMenu = new ContextMenu() {
                    items += makeVereinUmbenennenMenu(v)
                    items += makeVereinLoeschenMenu(v)
                  }
                case _       => controlsView.contextMenu = new ContextMenu()
              }
            }
            case _  => controlsView.contextMenu = new ContextMenu()
          }

      }
      val centerPane = (newItem.isLeaf, Option(newItem.getParent)) match {
        case (true, Some(parent)) => {
          tree.getThumbs(parent.getValue).find(p => p.button.text.getValue.equals(newItem.getValue)) match {
            case Some(KuTuAppThumbNail(p: WettkampfView, _, newItem)) =>
              PageDisplayer.choosePage(modelWettkampfModus, Some(p), "dashBoard - " + newItem.getValue, tree)
            case Some(KuTuAppThumbNail(v: Verein, _, newItem)) =>
              PageDisplayer.choosePage(modelWettkampfModus, Some(v), "dashBoard - " + newItem.getValue, tree)
            case _ =>
              PageDisplayer.choosePage(modelWettkampfModus, None, "dashBoard - " + newItem.getValue, tree)
          }
        }
        case (false, Some(_)) =>
          PageDisplayer.choosePage(modelWettkampfModus, None, "dashBoard - " + newItem.getValue, tree)
        case (_, _) =>
          PageDisplayer.choosePage(modelWettkampfModus, None, "dashBoard", tree)
      }
      if(splitPane.items.size > 1) {
        splitPane.items.remove(1)
      }
      splitPane.items.add(1, centerPane)
    }
  }

  val scrollPane = new ScrollPane {
    minWidth = 5
    maxWidth = 400
    prefWidth = 200
    fitToWidth = true
    fitToHeight = true
    id = "page-tree"
    content = controlsView
  }

  val splitPane = new SplitPane {
    dividerPositions = 0.2
    id = "page-splitpane"
    items.addAll(scrollPane, centerPane)
  }

  var divider: Option[Double] = None
  
  modelWettkampfModus.onChange {
    if(modelWettkampfModus.value) {
      divider = Some(splitPane.dividerPositions(0))
    }
    if(!modelWettkampfModus.value && (divider match {case Some(_) => true case _ => false})) {
      splitPane.dividerPositions = divider.get
    }
    else {
      splitPane.dividerPositions = 0d
    }
    resetBestenResults
  }
  
  val header = new BorderPane {
    vgrow = Priority.Always
    hgrow = Priority.Always
    prefHeight = 76
    maxHeight = 76
    id = "mainHeader"
    left = new ImageView {
      image = new Image(this.getClass.getResourceAsStream("/images/logo.png"))
      margin = Insets(15, 0, 0, 10)
    }
    right = new ToolBar {
      id = "mainToolBar"
      vgrow = Priority.Always
      hgrow = Priority.Always
      content = List(btnWettkampfModus, btnConnectStatus, lblRemoteAddress)
    }
  }
  
  val invisibleWebView = new WebView()
  val webViewContainer = new ScrollPane() {
    vgrow = Priority.Always
    hgrow = Priority.Always
    prefHeight = 76
    maxHeight = 76
//    maxWidth = 0
//    maxHeight = 0
//    prefHeight = 0
//    prefWidth = 0
    id = "invisible-webView"
    content = invisibleWebView
  }
  val headerContainer = new StackPane() {
    vgrow = Priority.Always
    hgrow = Priority.Always
    prefHeight = 76
    maxHeight = 76    
    children = Seq(webViewContainer, header)
  }

  def getStage() = stage
  //
  // Layout the main stage
  //
  stage = new PrimaryStage {
    //initStyle(StageStyle.TRANSPARENT);
    title = "KuTu Wettkampf-App"
    icons += new Image(this.getClass.getResourceAsStream( "/images/app-logo.png" ))
    scene = new Scene(1200, 750) {
      root = new BorderPane {
        top = headerContainer
        center = new BorderPane {
          center = splitPane
        }
        styleClass += "application"
      }

    }
    val st = this.getClass.getResource("/css/Main.css")
    if(st == null) {
      logger.debug("Ressource /css/main.css not found. Class-Anchor: " + this.getClass)
    }
    else if(scene() == null) {
    	logger.debug("scene() == null")
    }
    else if(scene().getStylesheets == null) {
      logger.debug("scene().getStylesheets == null")
    }
    else {
    	scene().stylesheets.add(st.toExternalForm)
    }
  }
  
}