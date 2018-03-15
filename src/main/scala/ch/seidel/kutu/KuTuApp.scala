package ch.seidel.kutu

import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.stage.Screen
import scalafx.scene.Node
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.event.ActionEvent
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.Cursor
import scalafx.application.Platform
import scala.concurrent.Future
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scala.concurrent.ExecutionContext.Implicits
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.Node.sfxNode2jfx
import scalafx.scene.control._
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.ComboBox.sfxComboBox2jfx
import scalafx.scene.control.Label.sfxLabel2jfx
import scalafx.scene.control.MenuItem.sfxMenuItem2jfx
import scalafx.scene.control.ScrollPane.sfxScrollPane2jfx
import scalafx.scene.control.TextField.sfxTextField2jfx
import scalafx.scene.control.TreeItem.sfxTreeItemToJfx
import ch.seidel.kutu.domain._
import ch.seidel.commons.PageDisplayer
import ch.seidel.commons.DisplayablePage
import ch.seidel.kutu.data.ResourceExchanger
import java.awt.Desktop
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import scalafx.stage.StageStyle
import scalafx.beans.property.BooleanProperty
import scalafx.beans.binding.Bindings
import scalafx.scene.web.WebView
import scalafx.beans.property.ReadOnlyStringWrapper
import org.slf4j.LoggerFactory
import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import java.util.UUID
import ch.seidel.kutu.http.Core
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.Config._
import java.util.Base64
import ch.seidel.kutu.http.KuTuAppHTTPServer
import ch.seidel.kutu.akka.KutuAppEvent
import ch.seidel.kutu.http.JsonSupport
import spray.json._

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.DatePicker
import javafx.util.Callback

object KuTuApp extends JFXApp with KutuService with JsonSupport {
  import WertungServiceBestenResult._
  
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val server = KuTuServer
  import scala.concurrent.ExecutionContext.Implicits.global
  
  override def stopApp() {
    ConnectionStates.disconnected()
    server.shutDown("KuTuApp")
  }
  
  var tree = AppNavigationModel.create(KuTuApp.this)
  val rootTreeItem = new TreeItem[String]("Dashboard") {
    expanded = true
    children = tree.getTree
  }
  val modelWettkampfModus = new BooleanProperty()
  val modelWettkampfWertungChanged = new SimpleObjectProperty[KutuAppEvent]()
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
    onAction = handleAction {action => 
      if (ConnectionStates.connectedProperty.value) {
        ConnectionStates.disconnected
      } else {        
        connectAndShare(selectedWettkampf.value, "Share", action)
      }
      Platform.runLater{
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
           s"Version: ${Config.appVersion}, Built: ${Config.builddate}"
        case uuid =>
           s"Server: ${Config.remoteBaseUrl}\nVersion: ${Config.appVersion}, Built: ${Config.builddate}"
      }      
    //visible <== ConnectionStates.connectedProperty
     }, ConnectionStates.connectedWithProperty)
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
      Thread.sleep(10L)// currentThread().wait(1L)
      Platform.runLater{
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
        task.onComplete{
          case Success(ret) =>
            Platform.runLater{
              try {
                p.success(ret)
              }
              catch {
                case e:Exception => p.failure(e)
              }
              finally {
                setCursor(Cursor.Default)
              }
            }
          case Failure(error) =>
            Platform.runLater{
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
        case e:Exception =>          
          e.printStackTrace()
          Platform.runLater{
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
    val ctoSet = if(c.equals(Cursor.Wait)) {
      cursorWaiters += 1
      c
    }
    else {
      cursorWaiters -= 1
      if(cursorWaiters > 0) {
        Cursor.WAIT
      }
      else {
        cursorWaiters = 0
        c
      }
    }
    //logger.debug(cursorWaiters)
    if(getStage() != null) {
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
    makeMenuAction("Wettkampf bearbeiten") {(caption, action) =>
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
        if(p.auszeichnung > 100) {
          text = dbl2Str(p.auszeichnung / 100d) + "%"
        }
        else {
          text = p.auszeichnung + "%"
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
        onAction = handleAction {implicit e: ActionEvent =>
          try {
            val w = saveWettkampf(
              p.id,
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
                case s: String => try {s} catch {case e:Exception => 0}
              },
              p.uuid)
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
          catch {
            case e: IllegalArgumentException =>
              new Alert(AlertType.Error, e.getMessage).showAndWait()
          }
        }
      }
    )}
  }
  def makeWettkampfExportierenMenu(p: WettkampfView): MenuItem = {
    makeMenuAction("Wettkampf exportieren") {(caption, action) =>
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
          val file = if(!selectedFile.getName.endsWith(".zip")) {
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
    val item = makeMenuAction("Wettkampf hochladen") {(caption, action) =>
      val process = KuTuApp.invokeAsyncWithBusyIndicator{
        if (remoteBaseUrl.indexOf("localhost") > -1) {
          server.startServer { uuid => server.sha256(uuid) }
        }
        server.httpUploadWettkampfRequest(p.toWettkampf)
      }
      process.onComplete{resultTry =>
        Platform.runLater{ 
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
    item.disable <== when(Bindings.createBooleanBinding(() => p.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !ConnectionStates.connectedProperty.value, 
        selectedWettkampfSecret, ConnectionStates.connectedProperty,
        controlsView.selectionModel().selectedItem)) choose true otherwise false 
    item
  }
  
  def makeWettkampfRemoteRemoveMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Wettkampf im Netzwerk entfernen") {(caption, action) =>
      val process = KuTuApp.invokeAsyncWithBusyIndicator{ Future{
          server.httpRemoveWettkampfRequest(p.toWettkampf)
          ConnectionStates.disconnected()
        }
      }
      process.onComplete{resultTry =>
        Platform.runLater{ 
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
    item.disable  <== when(Bindings.createBooleanBinding(() => !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")), 
        controlsView.selectionModel().selectedItem, ConnectionStates.connectedWithProperty,selectedWettkampfSecret, ConnectionStates.connectedProperty)) choose true otherwise false 
    item
  }
  
  def makeWettkampfDownloadMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Wettkampf Resultate lokal aktualisieren") {(caption, action) =>
      KuTuApp.invokeWithBusyIndicator {
        val url=s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}"
        Await.result(server.httpDownloadRequest(server.makeHttpGetRequest(url)), Duration.Inf)
      }
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
    if(!modelWettkampfModus.value) {
      makeMenuAction("Wettkampf durchführen") {(caption, action) =>
        modelWettkampfModus.value = true
      }
    }
    else {
      makeMenuAction("Wettkampf-Modus beenden") {(caption, action) =>
        modelWettkampfModus.value = false
      }
    }
  }

  def makeNeuerVereinAnlegenMenu = makeMenuAction("Neuen Verein anlegen ...") {(caption, action) =>
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
      onAction = handleAction {implicit e: ActionEvent =>
        val vid = createVerein(
            txtVereinsname.text.value.trim(),
            txtVerband.text.value match {
              case "" => None
              case s => Some(s.trim())
            }
            )
        updateTree
        selectVereine.find {_.id == vid } match {
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

  def makeLoginMenu = makeMenuAction("Login ...") {(caption, action) =>
    implicit val e = action

    val txtUsername = new TextField {
      prefWidth = 500
      promptText = "Username"
    }

    val txtPassword = new PasswordField {
      prefWidth = 500
      promptText = "Wettkampf-Passwort"
    }
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(
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
      onAction = handleAction {implicit e: ActionEvent =>
        KuTuApp.invokeWithBusyIndicator {
          server.startServer { x => server.sha256(x) }
          Await.result(server.httpLoginRequest(s"$remoteBaseUrl/api/login", txtUsername.text.value.trim(), txtPassword.text.value.trim()), Duration.Inf)
        }
      }
    })
  }
  
  def makeProxyLoginMenu = makeMenuAction("Internet Proxy ...") {(caption, action) =>
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
      onAction = handleAction {implicit e: ActionEvent =>
        server.setProxyProperties(
            host = txtProxyAddress.text.value.trim(), 
            port = txtProxyPort.text.value.trim(),
            user = txtUsername.text.value.trim(),
            password = txtPassword.text.value.trim())
      }
    })
  }

  def makeDisconnectMenu(p: WettkampfView) = {
    val item = makeMenuAction("Netzwerkmodus stoppen") {(caption, action) =>
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
      if (remoteBaseUrl.indexOf("localhost") > -1) {
        server.startServer { uuid => server.sha256(uuid) }
      }
      p.uuid.zip(p.toWettkampf.readSecret(homedir, remoteHostOrigin)).headOption match {
        case Some((uuid, secret)) =>
          server.httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
        case None => 
          server.httpUploadWettkampfRequest(p.toWettkampf)
      }
    }.map(response => {
      (response, WebSocketClient.connect(p.toWettkampf, ResourceExchanger.processWSMessage((event) => {
        Platform.runLater{
          modelWettkampfWertungChanged.setValue(event)
        }
      })))
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
      case Failure(error) =>
        Platform.runLater{
        PageDisplayer.showInDialog(caption, 
          new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                center = new VBox {
                  children.addAll(new Label(error.getMessage.replace("(", "(\n")))
                }
              }
            }
          }
        )
        }
      }
  }
  
  def makeConnectAndShareMenu(p: WettkampfView) = { 
    val item = makeMenuAction("Connect and share ...") {(caption, action) =>
      connectAndShare(p, caption, action)
    }
    item.disable <== when(Bindings.createBooleanBinding(() => 
      !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret, 
      ConnectionStates.connectedWithProperty
      )) choose true otherwise false 
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
    makeMenuAction("Wettkampf herunterladen") {(caption, action) =>
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
            wkTable.selectionModel.value.setSelectionMode(SelectionMode.SINGLE)
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
                      Await.result(server.httpDownloadRequest(server.makeHttpGetRequest(url)), Duration.Inf)
                    }
                  }
                }
              }
            }
          )
        }

        case _ =>
      }
    }
  }
  
  def makeNeuerWettkampfImportierenMenu: MenuItem = {
    makeMenuAction("Wettkampf importieren") {(caption, action) =>
      implicit val e = action
      val fileChooser = new FileChooser {
         title = "Wettkampf File importieren"
         initialDirectory = new java.io.File(homedir)
         extensionFilters ++= Seq(
           new ExtensionFilter("Zip-Files", "*.zip"),
//           new ExtensionFilter("Image Files", Seq("*.png", "*.jpg", "*.gif")),
//           new ExtensionFilter("Audio Files", Seq("*.wav", "*.mp3", "*.aac")),
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
                },
                new Button("OK") {
                    onAction = handleAction {implicit e: ActionEvent =>
                    }
                  }
                )  
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
  def showQRCode(p: WettkampfView) = {
    val item = makeMenuAction("Kampfrichter Mobile register ...") {(caption, action) =>
      implicit val e = action
      p.uuid.zip(p.toWettkampf.readSecret(homedir, remoteHostOrigin)).headOption match {
        case Some((uuid, secret)) =>
          val connectionString = s"$remoteBaseUrl/?" + new String(enc.encodeToString((s"c=$uuid&s=$secret").getBytes))
          println(connectionString)
          val out = QRCode.from(connectionString).to(ImageType.PNG).withSize(500, 500).stream();
          val in = new ByteArrayInputStream(out.toByteArray());
      
          val image = new Image(in);
          val view = new ImageView(image);
          view.setStyle("-fx-stroke-width: 2; -fx-stroke: blue");
          PageDisplayer.showInDialog(caption, new DisplayablePage() {
            def getPage: Node = view
          }
          )  
          
        case None =>
          PageDisplayer.showInDialog(caption, new DisplayablePage() {
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
    item.disable <== when(Bindings.createBooleanBinding(() => !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")), 
        ConnectionStates.connectedWithProperty, selectedWettkampfSecret, controlsView.selectionModel().selectedItem)) choose true otherwise false 
    item
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
          }
        case "Wettkämpfe" =>
          controlsView.contextMenu = new ContextMenu() {
            items += makeNeuerWettkampfAnlegenMenu
            items += makeNeuerWettkampfImportierenMenu
            items += new Menu("Netzwerk") {
              //items += makeLoginMenu
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
                  val networkMenu = new Menu("Netzwerk") {
                    items += showQRCode(p)
                    items += makeWettkampfUploadMenu(p)
                    items += makeConnectAndShareMenu(p)
                    items += makeWettkampfDownloadMenu(p)    
                    items += makeDisconnectMenu(p)
                    items += makeWettkampfRemoteRemoveMenu(p)
                  }
                  
                  controlsView.contextMenu = new ContextMenu() {
                    items += makeWettkampfDurchfuehrenMenu(p)
                    items += makeWettkampfBearbeitenMenu(p)
                    items += makeWettkampfExportierenMenu(p)
                    items += makeWettkampfDataDirectoryMenu(p)
                    items += makeWettkampfLoeschenMenu(p)
                    items += networkMenu
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
    scene = new Scene(1400, 900) {
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