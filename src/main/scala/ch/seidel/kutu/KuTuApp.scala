package ch.seidel.kutu

import ch.seidel.commons.{DisplayablePage, PageDisplayer, ProgressForm, TaskSteps}
import ch.seidel.jwt
import ch.seidel.kutu.Config._
import ch.seidel.kutu.actors.KutuAppEvent
import ch.seidel.kutu.data.{CaseObjectMetaUtil, ResourceExchanger, Surname}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http._
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.view.{MediaController, WettkampfTableView}
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.scene.control.DatePicker
import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType
import org.controlsfx.validation.ValidationSupport
import org.slf4j.LoggerFactory
import scalafx.Includes._
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.beans.binding.Bindings
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.beans.property.{BooleanProperty, ReadOnlyStringWrapper, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Side}
import scalafx.scene.Node.sfxNode2jfx
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Label.sfxLabel2jfx
import scalafx.scene.control.MenuItem.sfxMenuItem2jfx
import scalafx.scene.control.Tab.sfxTab2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TextField.sfxTextField2jfx
import scalafx.scene.control.TreeItem.sfxTreeItemToJfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{Clipboard, ClipboardContent, DataFormat}
import scalafx.scene.layout._
import scalafx.scene.web.WebView
import scalafx.scene.{Cursor, Node, Scene}
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Screen}
import spray.json._

import java.io.{ByteArrayInputStream, File, FileInputStream}
import java.nio.file.Files
import java.util.concurrent.{Executors, ScheduledExecutorService}
import java.util.{Base64, Date, UUID}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

object KuTuApp extends JFXApp3 with KutuService with JsonSupport with JwtSupport {

  import WertungServiceBestenResult._

  private val logger = LoggerFactory.getLogger(this.getClass)
  private lazy val server = KuTuServer
  val enc: Base64.Encoder = Base64.getUrlEncoder

  import scala.concurrent.ExecutionContext.Implicits.global

  val lazyExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  override def stopApp(): Unit = {
    lazyExecutor.shutdownNow()
    server.shutDown("KuTuApp")
    ConnectionStates.disconnected()
  }

  var tree: KuTuAppTree = AppNavigationModel.create(KuTuApp.this)
  val modelWettkampfModus = new BooleanProperty()
  val selectedWettkampf = new SimpleObjectProperty[WettkampfView]()
  val selectedWettkampfSecret = new SimpleObjectProperty[Option[String]]()
  var controlsView: TreeView[String] = null
  var rootTreeItem: TreeItem[String] = null
  var invisibleWebView: WebView = null
  val mediaPlayer = new MediaController()

  def updateTree: Unit = {
    def selectionPathToRoot(node: TreeItem[String]): List[TreeItem[String]] = {
      if (node == null) {
        List.empty
      }
      else if (node.parent.value != null) {
        selectionPathToRoot(node.parent.value) :+ node
      } else {
        List(node)
      }
    }

    val oldselected = selectionPathToRoot(controlsView.selectionModel().getSelectedItem)

      lastSelected.value = "Updating"
      var lastNode = rootTreeItem
      try {
        tree = AppNavigationModel.create(KuTuApp.this)
        rootTreeItem.children = tree.getTree
        for {node <- oldselected} {
          lastNode.setExpanded(true)
          lastNode.children.find(n => n.value.value.equals(node.value.value)) match {
            case Some(n) => lastNode = n
            case _ =>
          }
        }
      } finally {
        lastSelected.value = oldselected.lastOption.map(_.value.value).getOrElse("")
      }
      controlsView.selectionModel().select(lastNode)
  }

  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R) = new javafx.event.EventHandler[J] {
    def handle(event: J): Unit = {
      setCursor(Cursor.Wait)
      try {
        handler(event)
      } catch {
        case e: Throwable =>
          logger.error("Unerwarteter Fehler", e)
          throw e
      }
      finally {
        setCursor(Cursor.Default)
      }
    }
  }

  def invokeWithBusyIndicator(task: => Unit): Unit = {
    setCursor(Cursor.Wait)
    Future[Boolean] {
      Thread.sleep(10L) // currentThread().wait(1L)
      Platform.runLater {
        try {
          task
        } catch {
          case e: Throwable =>
            logger.error("Unerwarteter Fehler", e)
            Platform.runLater(() => PageDisplayer.showErrorDialog("Unerwarteter Fehler")(e))
            throw e
        }
        finally {
          setCursor(Cursor.Default)
        }
      }
      true
    }
  }

  def invokeAsyncWithBusyIndicator[R](title: String = "Bitte warten ...", seconds: Int = 0)(task: => Future[R]): Future[R] = {
    //setCursor(Cursor.Wait)
    val pForm = new ProgressForm
    val timerTask = new Task[Void] {
      @throws[InterruptedException]
      override def call: Void = {
        for (i <- 0 until 20) {
          updateProgress(i, 20)
          if (seconds > 0) Thread.sleep(seconds * 50)
        }
        updateProgress(20, 20)
        null
      }
    }
    pForm.activateProgressBar(title, if (seconds > 0) timerTask else null)

    val p = Promise[R]()
    Future[Boolean] {
      try {
        task.onComplete {
          case Success(ret) =>
            Platform.runLater {
              try {
                p.success(ret)
              }
              catch {
                case e: Throwable =>
                  logger.error("Fehler beim Abschliessen von " + title, e)
                  p.failure(e)
              }
              finally {
                //setCursor(Cursor.Default)
                pForm.getDialogStage.close()
              }
            }
          case Failure(error) =>
            logger.error("Fehler bei " + title, error)
            Platform.runLater {
              try {
                p.failure(error)
              }
              finally {
                //setCursor(Cursor.Default)
                pForm.getDialogStage.close()
              }
            }

        }
        timerTask.run()
      }
      catch {
        case e: Throwable =>
          logger.error("Fehler bei " + title, e)
          Platform.runLater {
            try {
              p.failure(e)
            }
            finally {
              //setCursor(Cursor.Default)
              pForm.getDialogStage.close()
            }
          }
      }
      true
    }

    p.future
  }

  var cursorWaiters = 0

  def setCursor(c: Cursor): Unit = {
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
    override def updateItem(item: ProgrammView, empty: Boolean): Unit = {
      super.updateItem(item, empty)
      if (item != null) {
        textProperty().setValue(item.easyprint)
      }
    }
  }

  val lastSelected = StringProperty("")

  type MenuActionHandler = (String, ActionEvent) => Unit

  def makeMenuAction(caption: String)(handler: MenuActionHandler): MenuItem = {
    new MenuItem(caption) {
      onAction = handleAction { e: ActionEvent =>
        handler(caption, e)
      }
    }
  }
  def makeWettkampfKopierenMenu(copyFrom: WettkampfView): MenuItem = {
    makeNeuerWettkampfAnlegenMenu(Some(copyFrom))
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
      val cmbProgramm = new ComboBox[ProgrammView] {
        prefWidth = 500
        buttonCell = new ProgrammListCell
        cellFactory.value = {_:Any => new ProgrammListCell}
        promptText = "Programm"
        items = ObservableBuffer.from(listRootProgramme().sorted)
        selectionModel.value.select(p.programm)
      }
      val txtNotificationEMail = new TextField {
        prefWidth = 500
        promptText = "EMail für die Notifikation von Online-Mutationen"
        text = p.notificationEMail
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
      val cmbRiegenRotationsregel = new ComboBox[String]() {
        prefWidth = 500
        RiegenRotationsregel.predefined.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Riegenrotationsregel"
      }
      val txtRiegenRotationsregel = new TextField {
        prefWidth = 500
        promptText = "z.B. Kategorie/Verein/Geschlecht/Alter/Name/Vorname/Rotierend/AltInv"
        text = RiegenRotationsregel(p.toWettkampf).toFormel
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbRiegenRotationsregel.value.value) || cmbRiegenRotationsregel.selectionModel.value.isEmpty
        },
          cmbRiegenRotationsregel.selectionModel,
          cmbRiegenRotationsregel.value
        )

        cmbRiegenRotationsregel.value.onChange {
          text.value = RiegenRotationsregel.predefined(cmbRiegenRotationsregel.value.value)
          if (text.value.isEmpty && !"Einfach".equals(cmbRiegenRotationsregel.value.value)) {
            text.value = p.rotation
          }
        }
      }
      val cmbTeamRegel = new ComboBox[String]() {
        prefWidth = 500
        TeamRegel.predefined.keySet.toList.sorted.foreach(definition => {
          items.value.add(definition)
        })
        promptText = "Definition für Teams"
      }
      val txtTeamRegel = new TextField {
        prefWidth = 500
        promptText = """z.B. VereinGerät(3/4) oder VerbandGesamt(4/*)"""
        text = TeamRegel(p.toWettkampf).toFormel
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbTeamRegel.value.value) || cmbTeamRegel.selectionModel.value.isEmpty
        },
          cmbTeamRegel.selectionModel,
          cmbTeamRegel.value
        )

        cmbTeamRegel.value.onChange {
          if (cmbTeamRegel.value.value.equals("Individuell")) {
            text.value = p.teamrule
          } else {
            text.value = TeamRegel.predefined(cmbTeamRegel.value.value)
            if (text.value.isEmpty && !"Keine Teams".equals(cmbTeamRegel.value.value)) {
              text.value = p.teamrule
            }
          }
        }
      }
      val cmbPunktgleichstandsregel = new ComboBox[String]() {
        prefWidth = 500
        Gleichstandsregel.predefined.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Rangierungsregel bei Punktegleichstand"
      }
      val txtPunktgleichstandsregel = new TextField {
        prefWidth = 500
        promptText = "z.B. E-Note-Summe/E-NoteBest/Disziplin(Boden,Sprung)/JugendVorAlter"
        text = Gleichstandsregel(p.toWettkampf).toFormel
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbPunktgleichstandsregel.value.value) || cmbPunktgleichstandsregel.selectionModel.value.isEmpty
        },
          cmbPunktgleichstandsregel.selectionModel,
          cmbPunktgleichstandsregel.value
        )

        cmbPunktgleichstandsregel.value.onChange {
          text.value = Gleichstandsregel.predefined(cmbPunktgleichstandsregel.value.value)
          if (text.value.isEmpty && !"Ohne".equals(cmbPunktgleichstandsregel.value.value)) {
            text.value = p.punktegleichstandsregel
          }
        }
        cmbProgramm.value.onChange {
          text.value = Gleichstandsregel(p.toWettkampf.copy(programmId = cmbProgramm.selectionModel.value.getSelectedItem.id)).toFormel
        }

      }
      val validationSupport = new ValidationSupport
      validationSupport.registerValidator(txtPunktgleichstandsregel, false, Gleichstandsregel.createValidator)
      val cmbAltersklassen = new ComboBox[String]() {
        prefWidth = 500
        Altersklasse.predefinedAKs.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Altersklassen"

      }
      val txtAltersklassen = new TextField {
        prefWidth = 500
        promptText = "Alersklassen (z.B. 6,7,9-10,AK11-20*2,25-100/10)"
        text = p.altersklassen
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbAltersklassen.value.value) || cmbAltersklassen.selectionModel.value.isEmpty
        },
          cmbAltersklassen.selectionModel,
          cmbAltersklassen.value
        )

        cmbAltersklassen.value.onChange{
          text.value = Altersklasse.predefinedAKs(cmbAltersklassen.value.value)
          if (text.value.isEmpty && !"Ohne".equals(cmbAltersklassen.value.value)) {
            text.value = p.altersklassen
          }
        }
      }
      val cmbJGAltersklassen = new ComboBox[String]() {
        prefWidth = 500
        Altersklasse.predefinedAKs.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Jahrgang Altersklassen"

      }
      val txtJGAltersklassen = new TextField {
        prefWidth = 500
        promptText = "Jahrgangs Altersklassen (z.B. AK6,AK7,AK9-10,AK11-20*2,AK25-100/10)"
        text = p.jahrgangsklassen
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbJGAltersklassen.value.value) || cmbJGAltersklassen.selectionModel.value.isEmpty
        },
          cmbJGAltersklassen.selectionModel,
          cmbJGAltersklassen.value
        )

        cmbJGAltersklassen.value.onChange{
          text.value = Altersklasse.predefinedAKs(cmbJGAltersklassen.value.value)
          if (text.value.isEmpty && !"Ohne".equals(cmbJGAltersklassen.value.value)) {
            text.value = p.jahrgangsklassen
          }
        }
      }
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              spacing = 5.0
              children.addAll(
                new Label(txtDatum.promptText.value), txtDatum,
                new Label(txtTitel.promptText.value), txtTitel,
                new Label(cmbProgramm.promptText.value), cmbProgramm,
                new Label(txtNotificationEMail.promptText.value), txtNotificationEMail,
                new Label(txtAuszeichnung.promptText.value), txtAuszeichnung,
                new Label(txtAuszeichnungEndnote.promptText.value), txtAuszeichnungEndnote,
                cmbRiegenRotationsregel, txtRiegenRotationsregel,
                cmbPunktgleichstandsregel, txtPunktgleichstandsregel,
                cmbAltersklassen, txtAltersklassen,
                cmbJGAltersklassen, txtJGAltersklassen,
                cmbTeamRegel, txtTeamRegel
              )
            }
          }
        }
      },
        new Button("OK") {
          disable <== when(Bindings.createBooleanBinding(() => {
              txtDatum.value.isNull.value || txtTitel.text.value.isEmpty
          },
            txtDatum.value,
            txtTitel.text
          )) choose true otherwise false
          onAction = handleAction { implicit e: ActionEvent =>
            try {
              val w = saveWettkampf(
                p.id,
                ld2SQLDate(txtDatum.valueProperty().value),
                txtTitel.text.value,
                Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
                txtNotificationEMail.text.value,
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
                p.uuid,
                txtAltersklassen.text.value,
                txtJGAltersklassen.text.value,
                txtPunktgleichstandsregel.text.value,
                txtRiegenRotationsregel.text.value,
                txtTeamRegel.text.value
              )

              w.prepareFilePath(homedir, readOnly = false, Some(p.toWettkampf))
              updateTree
              val text = s"${w.titel} ${w.datum}"
              tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                case Some(node) =>
                  controlsView.selectionModel().select(node.parent.value)
                  controlsView.selectionModel().select(node)
                case None =>
              }
            }
            catch {
              case e: IllegalArgumentException =>
                PageDisplayer.showErrorDialog(caption)(e)
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
        initialFileName.value = p.titel.replace(" ", "_") + "_" + sdfYear.format(p.datum) + ".zip"
      }
      val selectedFile = fileChooser.showSaveDialog(getStage())
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

  def uploadResults(wettkampf: WettkampfView, caption: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val process = KuTuApp.invokeAsyncWithBusyIndicator(caption) {
      if (remoteBaseUrl.indexOf("localhost") > -1) {
        KuTuServer.startServer()
      }
      KuTuServer.httpUploadWettkampfRequest(wettkampf.toWettkampf)
    }
    process.onComplete { resultTry =>
      Platform.runLater {
        resultTry match {
          case Success(response) =>
            selectedWettkampfSecret.value = wettkampf.toWettkampf.readSecret(homedir, remoteHostOrigin)
            PageDisplayer.showInDialogFromRoot(caption, new DisplayablePage() {
              def getPage: Node = {
                new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  center = new VBox {
                    children.addAll(new Label(s"Der Wettkampf ${wettkampf.easyprint} wurde erfolgreich im Netzwerk bereitgestellt"))
                  }
                }
              }
            })
          case Failure(error) =>
            PageDisplayer.showErrorDialog(caption)(error)
        }
      }
    }
  }

  def makeWettkampfUploadMenu(p: WettkampfView, disableBindings: scalafx.beans.binding.BooleanBinding): MenuItem = {
    val item = makeMenuAction("Upload") { (_, action) =>
      implicit val e: ActionEvent = action
      validateUpload(p, s"Wettkampf auf $remoteHostOrigin hochladen ...", action) { caption =>
        uploadResults(p, caption)
      }
    }
    item.disable <== disableBindings
    item
  }

  def makeWettkampfRemoteRemoveMenu(p: WettkampfView): MenuItem = {
    val item = makeMenuAction("Wettkampf im Netzwerk entfernen") { (caption, action) =>
      implicit val e = action
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(new Label("Der Wettkampf wird inklusive allen Resultaten im Netzwerk entfernt und steht danach nicht mehr für andere Teilnehmer zur Verfügung."))
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { implicit e: ActionEvent =>
            val process = KuTuApp.invokeAsyncWithBusyIndicator(caption) {
              server.httpRemoveWettkampfRequest(p.toWettkampf)
            }
            process.onComplete {
              resultTry =>
                ConnectionStates.disconnected()
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
        }
      )
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      Config.isLocalHostServer ||
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
            val url = s"$remoteAdminBaseUrl/api/competition/${p.uuid.get}"
            invokeAsyncWithBusyIndicator[Wettkampf](s"Wettkampf ${p.easyprint} herunterladen") {
              val pr = Promise[Wettkampf]()
              server.httpDownloadRequest(server.makeHttpGetRequest(url)).onComplete(ft =>
                Platform.runLater {
                  ft match {
                    case Success(w) =>
                      pr.success(w)
                      PageDisplayer.showMessageDialog("Download", "Erfolgreich heruntergeladen.")
                      updateTree
                      controlsView.selectionModel().select(controlsView.root.value)
                      val text = s"${w.titel} ${w.datum}"
                      tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                        case Some(node) => controlsView.selectionModel().select(node)
                        case None =>
                      }
                    case Failure(error) =>
                      pr.failure(error)
                      PageDisplayer.showErrorDialog(caption)(error)
                  }
                }
              )
              pr.future
            }
          }
        })
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      Config.isLocalHostServer ||
        (!p.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !p.toWettkampf.hasRemote(homedir, remoteHostOrigin)) ||
        !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
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

  private def makeFindDuplicteAthletes = makeMenuAction("Doppelt erfasste Athleten finden ...") { (caption, action) =>
    implicit val e = action
    KuTuApp.invokeAsyncWithBusyIndicator(caption) {
      Future {

        def mapSexPrediction(athlet: AthletView) = Surname
          .isSurname(athlet.vorname)
          .map { sn => if (sn.isMasculin == sn.isFeminin) athlet.geschlecht else if (sn.isMasculin) "M" else "W" }
          .getOrElse("X")

        val likeFinder = findAthleteLike(cache = new java.util.ArrayList[MatchCode], exclusive = true) _
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
              else if (a.id - b.id < 0) true
              else false
            }
          }
          (tupel(0), tupel(1), CaseObjectMetaUtil.mergeMissingProperties(tupel(0), tupel(1)))
        }
      }
    }.onComplete {
      case Failure(t) => logger.debug(t.toString)
      case Success(candidates) => Platform.runLater {
        def cleanMirrorTuples(): List[(AthletView, AthletView, AthletView)] = {
          candidates.foldLeft(List[(AthletView, AthletView, AthletView)]()) { (acc, triple) =>
            acc.find(acctriple => acctriple._1 == triple._1 || acctriple._2 == triple._1) match {
              case Some(_) => acc
              case _ => acc :+ triple
            }
          }
        }

        val athletModel = ObservableBuffer.from(cleanMirrorTuples())

        def printAthlet(athlet: AthletView) = athlet.extendedprint

        val athletTable = new TableView[(AthletView, AthletView, AthletView)](athletModel) {
          columns ++= List(
            new TableColumn[(AthletView, AthletView, AthletView), String] {
              text = "#1 Name Vorname Geb.Dat"
              minWidth = 220
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "athlet", {
                  printAthlet(x.value._1)
                })
              }
              minWidth = 220
            },
            new TableColumn[(AthletView, AthletView, AthletView), String] {
              text = "#2 Name Vorname Geb.Dat"
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
        athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
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
              val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
                x => athletTable.selectionModel.value.isSelected(x._2)
              }.map(_._1).toList
              selectedAthleten.foreach { case (athlet1, athlet2, athlet3) => {
                mergeAthletes(athlet2.id, athlet1.id)
              }
                insertAthlete(athlet3.toAthlet)
              }
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
        server.startServer()
        LocalServerStates.startLocalServer(() => server.listNetworkAdresses)
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() => isLocalHostServer,
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      LocalServerStates.localServerProperty,
      ConnectionStates.connectedWithProperty)) choose true otherwise false
    item
  }

  def makeStopServerMenu = {
    val item = makeMenuAction("Stop local Server") { (caption, action) =>
      KuTuApp.invokeWithBusyIndicator {
        LocalServerStates.stopLocalServer()
        server.stopServer("user stops local server")
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() => {
      isLocalHostServer
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
      isLocalHostServer
    },
      LocalServerStates.localServerProperty)) choose true otherwise false
    item
  }

  def makeDisconnectMenu(p: WettkampfView) = {
    val item = makeMenuAction("Verbindung stoppen") { (caption, action) =>
      ConnectionStates.disconnected()
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      ConnectionStates.connectedWithProperty
    )) choose true otherwise false
    item
  }

  def makeSelectBackendMenu = {
    val item = makeMenuAction("Server auswählen") { (caption, action) =>
      implicit val e = action
      val filteredModel = ObservableBuffer.from(Config.getRemoteHosts)
      val header = new VBox {
        spacing = 5
        children.addAll(
          new Label{
            text = s"Server Origin"
            style = "-fx-font-size: 1.2em;-fx-font-weight: bold;-fx-padding: 8px 0 2px 0;-fx-text-fill: #0072aa;"
            styleClass += "toolbar-header"},
            new Label(s"   Aktuell: ${Config.remoteHost}"),
            new Label(s"   Default: ${Config.defaultRemoteHost}"),
            new Label("Auswahl:")
        )
      }
      val serverList = new ListView[String](filteredModel) {
        prefHeight = 150
      }
      serverList.selectionModel.value.setSelectionMode(SelectionMode.Single)
      serverList.selectionModel.value.select(Config.remoteHost)
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            minWidth = 400
            center = new BorderPane {
              hgrow = Priority.Always
              vgrow = Priority.Always
              top = header
              center = serverList
              minWidth = 350
            }

          }
        }
      }, new Button("Default") {
        onAction = (_: ActionEvent) => {
          ConnectionStates.switchRemoteHost(Config.defaultRemoteHost)
        }
      }, new Button("OK") {
        disable <== when(serverList.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
        onAction = (_: ActionEvent) => {
          if (!serverList.selectionModel().isEmpty) {
            serverList.items.value.zipWithIndex.filter {
              x => serverList.selectionModel.value.isSelected(x._2)
            }.collectFirst{ selected =>
              val (server, _) = selected
              ConnectionStates.switchRemoteHost(server)
            }
          }
        }
      })
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      ConnectionStates.connectedWithProperty.value.nonEmpty
        || modelWettkampfModus.value
        || LocalServerStates.localServerProperty.value,
      ConnectionStates.connectedWithProperty,
      ConnectionStates.remoteServerProperty,
      modelWettkampfModus,
      LocalServerStates.localServerProperty
    )) choose true otherwise false
    item
  }

  def validateUpload(p: WettkampfView, caption: String, action: ActionEvent)(handler: String=>Unit): Unit = {
    implicit val e: ActionEvent = action
    if(Config.isLocalHostServer) {
      handler(caption)
    } else if (
      ConnectionStates.connectedProperty.value ||
        p.toWettkampf.hasSecred(homedir, remoteHostOrigin)) {
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(new Label("Die Resultate zu diesem Wettkampf werden im Netzwerk hochgeladen und ersetzen dort die Resultate, die zu diesem Wettkampf erfasst wurden."))
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { e: ActionEvent =>
            handler(caption)
          }
        })
    } else {
      PageDisplayer.showInDialog(s"Wettkampf auf $remoteHostOrigin hochladen ...", new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              if (p.titel.toLowerCase.contains("test") && !remoteHostOrigin.contains("test")) {
                children.addAll(new Label("Sofern es sich um ein Testwettkampf handelt bitte zuerst mit dem Test-Server verbinden."))
              }
              children.addAll(
                new Label("Die Resultate zu diesem Wettkampf werden neu im Netzwerk bereitgestellt."),
                new Label(s"Die angegebene EMail-Adresse (${p.notificationEMail}) wird verwendet, um ein Bestätigungsmail zu versenden.\n "),
                new Label("Wenn die Bestätigung nicht innerhalb 1h bestätigt wird, wird der hochgeladene Wettkampf wieder gelöscht.")
              )
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { e: ActionEvent =>
            handler(s"Wettkampf auf $remoteHostOrigin hochladen ...")
          }
        })
    }
  }

  def validateConnect(p: WettkampfView, caption: String, action: ActionEvent)(handler: String=>Unit): Unit = {
    implicit val e: ActionEvent = action
    if(Config.isLocalHostServer ||
      ConnectionStates.connectedProperty.value ||
      p.toWettkampf.hasSecred(homedir, remoteHostOrigin) ||
      p.toWettkampf.hasRemote(homedir, remoteHostOrigin)) {
      handler(caption)
    } else {
      validateUpload(p, caption, action)(handler)
    }
  }
  def connectAndShare(p: WettkampfView, caption: String, action: ActionEvent) = {
    implicit val e = action
    val process = KuTuApp.invokeAsyncWithBusyIndicator(caption) {
      if (Config.isLocalHostServer) {
        if (!p.toWettkampf.hasSecred(homedir, "localhost")) {
          p.toWettkampf.saveSecret(homedir, "localhost", jwt.JsonWebToken(jwtHeader, setClaims(p.uuid.get, Int.MaxValue), jwtSecretKey))
        }
        server.httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", p.uuid.get, p.toWettkampf.readSecret(homedir, "localhost").get)
      } else if (!p.toWettkampf.hasRemote(homedir, remoteHostOrigin)) {
        server.httpUploadWettkampfRequest(p.toWettkampf, server.Connect)
      } else if (p.toWettkampf.hasSecred(homedir, remoteHostOrigin)) {
        server.httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", p.uuid.get, p.toWettkampf.readSecret(homedir, remoteHostOrigin).get)
      } else {
        Future{EmptyResponse()}
      }
    }.map(_ => {
      WebSocketClient.connect(p.toWettkampf, ResourceExchanger.processWSMessage(p.toWettkampf, (sender: Object, event: KutuAppEvent) => {
        Platform.runLater {
          WebSocketClient.modelWettkampfWertungChanged.setValue(event)
        }
      }), PageDisplayer.showErrorDialog(caption))
    })
    process.onComplete {
      case Success(wspromise) =>
        if (!wspromise.isCompleted) {
          logger.info(s"share: completed upload-operation. Show success-message ...")
          ConnectionStates.connectedWith(p.uuid.get, wspromise)
          Platform.runLater {
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
        }
        else {
          logger.info(s"share: upload-operation didn't complete. Disconnect ...")
          ConnectionStates.disconnected()
        }

      case Failure(error) =>
        logger.info(s"share: upload-operation failed with $error.!")
        PageDisplayer.showErrorDialog(caption)(error)
    }
  }

  def makeConnectAndShareMenu(p: WettkampfView) = {
    val item = makeMenuAction("Verbinden ...") { (caption, action) =>
      validateConnect(p, caption, action) { title =>
        connectAndShare(selectedWettkampf.value, title, action)
      }
    }
    item.disable <== when(Bindings.createBooleanBinding(() =>
      !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      controlsView.selectionModel().selectedItem,
      selectedWettkampfSecret,
      ConnectionStates.connectedWithProperty
    )) choose false otherwise true
    item
  }

  def makeNeuerWettkampfAnlegenMenu(copyFrom: Option[WettkampfView] = None): MenuItem = {
    val menutext = if (copyFrom.isEmpty) "Neuen Wettkampf anlegen ..." else s"Neuen Wettkampf wie ${copyFrom.get.easyprint} anlegen ..."
    makeMenuAction(menutext) { (caption, action) =>
      implicit val e = action
      val txtDatum = new DatePicker {
        setPromptText("Wettkampf-Datum")
        setPrefWidth(500)
      }
      val txtTitel = new TextField {
        prefWidth = 500
        promptText = "Wettkampf-Titel"
        text.value = copyFrom.map(_.titel).getOrElse("")
      }
      val pgms = ObservableBuffer.from(listRootProgramme().sorted)
      val cmbProgramm = new ComboBox(pgms) {
        prefWidth = 500
        buttonCell = new ProgrammListCell
        cellFactory.value = {_:Any => new ProgrammListCell}
        promptText = "Programm"
        copyFrom.map(_.programm).foreach(pgm => {
          val pgmIndex = pgms.indexOf(pgm)
          selectionModel.value.select(pgmIndex)
          selectionModel.value.select(pgm)
        })
      }
      val txtNotificationEMail = new TextField {
        prefWidth = 500
        promptText = "EMail für die Notifikation von Online-Mutationen"
        text.value = copyFrom.map(_.notificationEMail).getOrElse("")
      }
      val txtAuszeichnung = new TextField {
        prefWidth = 500
        promptText = "%-Angabe, wer eine Auszeichnung bekommt"
        text.value = "40.00%"
        copyFrom.map(_.auszeichnung).foreach(auszeichnung => {
          if (auszeichnung > 100) {
            text = dbl2Str(auszeichnung / 100d) + "%"
          }
          else {
            text = s"${auszeichnung}%"
          }
        })
      }
      val txtAuszeichnungEndnote = new TextField {
        prefWidth = 500
        promptText = "Auszeichnung bei Erreichung des Mindest-Gerätedurchschnittwerts"
        text = ""
        copyFrom.map(_.auszeichnungendnote).foreach(auszeichnungendnote => {
          text = auszeichnungendnote.toString()
        })
      }
      val cmbRiegenRotationsregel = new ComboBox[String]() {
        prefWidth = 500
        RiegenRotationsregel.predefined.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Riegenrotationsregel"
      }
      val txtRiegenRotationsregel = new TextField {
        prefWidth = 500
        promptText = "z.B. Kategorie/Verein/Alter/Name/Vorname/Rotierend/AltInv"

        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbRiegenRotationsregel.value.value) || cmbRiegenRotationsregel.selectionModel.value.isEmpty
        },
          cmbRiegenRotationsregel.selectionModel,
          cmbRiegenRotationsregel.value
        )

        cmbRiegenRotationsregel.value.onChange {
          text = RiegenRotationsregel.predefined(cmbRiegenRotationsregel.value.value)
        }
        text.value = RiegenRotationsregel("Einfach/Rotierend/AltInvers").toFormel
        copyFrom.map(_.rotation).foreach(rotation => {
          text = rotation
        })
      }
      val cmbTeamRegel = new ComboBox[String]() {
        prefWidth = 500
        TeamRegel.predefined.keySet.toList.sorted.foreach(definition => {
          items.value.add(definition)
        })
        promptText = "Definition für Teams"
      }
      val txtTeamRegel = new TextField {
        prefWidth = 500
        promptText = """z.B. VereinGerät(3/4) oder VerbandGesamt(4/*)"""
        copyFrom.map(_.teamrule).foreach(teamrule => {
          text = teamrule
        })

        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbTeamRegel.value.value) || cmbTeamRegel.selectionModel.value.isEmpty
        },
          cmbTeamRegel.selectionModel,
          cmbTeamRegel.value
        )

        cmbTeamRegel.value.onChange {
          text.value = TeamRegel.predefined(cmbTeamRegel.value.value)
          if (text.value.isEmpty && !"Keine Teams".equals(cmbTeamRegel.value.value)) {
            text.value = ""
          }
        }
      }
      val cmbPunktgleichstandsregel = new ComboBox[String]() {
        prefWidth = 500
        Gleichstandsregel.predefined.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Rangierungsregel bei Punktegleichstand"
      }
      val txtPunktgleichstandsregel = new TextField {
        prefWidth = 500
        promptText = "z.B. E-Note-Summe/E-NoteBest/Disziplin(Boden,Sprung)/JugendVorAlter"
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbPunktgleichstandsregel.value.value) || cmbPunktgleichstandsregel.selectionModel.value.isEmpty
        },
          cmbPunktgleichstandsregel.selectionModel,
          cmbPunktgleichstandsregel.value
        )

        cmbPunktgleichstandsregel.value.onChange {
          text = Gleichstandsregel.predefined(cmbPunktgleichstandsregel.value.value)
        }
        cmbProgramm.value.onChange {
          text = Gleichstandsregel(cmbProgramm.selectionModel.value.getSelectedItem.id).toFormel
        }
        copyFrom.map(_.punktegleichstandsregel).foreach(punktegleichstandsregel => {
          text = punktegleichstandsregel
        })
      }
      val validationSupport = new ValidationSupport
      validationSupport.registerValidator(txtPunktgleichstandsregel, false, Gleichstandsregel.createValidator)
      val cmbAltersklassen = new ComboBox[String]() {
        prefWidth = 500
        Altersklasse.predefinedAKs.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Altersklassen"
      }
      val txtAltersklassen = new TextField {
        prefWidth = 500
        promptText = "Alersklassen (z.B. 6,7,9-10,AK11-20*2,25-100/10)"
        text = ""
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbAltersklassen.value.value) || cmbAltersklassen.selectionModel.value.isEmpty
        },
          cmbAltersklassen.selectionModel,
          cmbAltersklassen.value
        )

        cmbAltersklassen.value.onChange {
          text = Altersklasse.predefinedAKs(cmbAltersklassen.value.value)
        }
        copyFrom.map(_.altersklassen).foreach(altersklassen => {
          text = altersklassen
        })
      }
      val cmbJGAltersklassen = new ComboBox[String]() {
        prefWidth = 500
        Altersklasse.predefinedAKs.foreach(definition => {
          items.value.add(definition._1)
        })
        promptText = "Jahrgang Altersklassen"
      }
      val txtJGAltersklassen = new TextField {
        prefWidth = 500
        promptText = "Jahrgangs Altersklassen (z.B. AK6,AK7,AK9-10,AK11-20*2,AK25-100/10)"
        text = ""
        editable <== Bindings.createBooleanBinding(() => {
          "Individuell".equals(cmbJGAltersklassen.value.value) || cmbJGAltersklassen.selectionModel.value.isEmpty
        },
          cmbJGAltersklassen.selectionModel,
          cmbJGAltersklassen.value
        )

        cmbJGAltersklassen.value.onChange{
          text = Altersklasse.predefinedAKs(cmbJGAltersklassen.value.value)
        }
        copyFrom.map(_.jahrgangsklassen).foreach(jahrgangsklassen => {
          text = jahrgangsklassen
        })
      }
      PageDisplayer.showInDialog(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              spacing = 5.0
              children.addAll(
                new Label(txtDatum.promptText.value), txtDatum,
                new Label(txtTitel.promptText.value), txtTitel,
                new Label(cmbProgramm.promptText.value), cmbProgramm,
                new Label(txtNotificationEMail.promptText.value), txtNotificationEMail,
                new Label(txtAuszeichnung.promptText.value), txtAuszeichnung,
                new Label(txtAuszeichnungEndnote.promptText.value), txtAuszeichnungEndnote,
                cmbRiegenRotationsregel, txtRiegenRotationsregel,
                cmbPunktgleichstandsregel, txtPunktgleichstandsregel,
                cmbAltersklassen, txtAltersklassen,
                cmbJGAltersklassen, txtJGAltersklassen,
                cmbTeamRegel, txtTeamRegel
              )
            }
          }
        }
      }, new Button("OK") {
        disable <== when(Bindings.createBooleanBinding(() => {
          cmbProgramm.selectionModel.value.getSelectedIndex == -1 ||
            txtDatum.value.isNull.value ||
            txtTitel.text.isEmpty.value ||
            txtNotificationEMail.text.isEmpty.value
        },
          cmbProgramm.selectionModel.value.selectedIndexProperty,
          txtDatum.value,
          txtNotificationEMail.text,
          txtTitel.text
        )) choose true otherwise false
        onAction = handleAction { implicit e: ActionEvent =>
          val w = createWettkampf(
            ld2SQLDate(txtDatum.valueProperty().value),
            txtTitel.text.value,
            Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
            txtNotificationEMail.text.value,
            txtAuszeichnung.text.value.filter(c => c.isDigit || c == '.' || c == ',') match {
              case "" => 0
              case s: String if (s.indexOf(".") > -1 || s.indexOf(",") > -1) => math.round(str2dbl(s) * 100).toInt
              case s: String => str2Int(s)
            },
            txtAuszeichnungEndnote.text.value match {
              case "" => 0
              case s: String => try {
                BigDecimal.valueOf(s)
              } catch {
                case e: Exception => 0
              }
            },
            Some(UUID.randomUUID().toString),
            txtAltersklassen.text.value,
            txtJGAltersklassen.text.value,
            txtPunktgleichstandsregel.text.value,
            txtRiegenRotationsregel.text.value,
            txtTeamRegel.text.value
          )
          val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
          if (!dir.exists()) {
            dir.mkdirs();
          }
          copyFrom.foreach(wkToCopy => {
            // Ranglisten (scoredef), Planzeiten und Logo kopieren ...
            val sourceFolder = new File(homedir + "/" + encodeFileName(copyFrom.get.easyprint))
            val targetFolder = new File(homedir + "/" + encodeFileName(w.easyprint))
            val sourceLogo = PrintUtil.locateLogoFile(sourceFolder)
            if (!targetFolder.equals(sourceFolder) && sourceLogo.exists()) {
              val logofileCopyTo = targetFolder.toPath.resolve(sourceLogo.getName)
              if (!logofileCopyTo.toFile.exists()) {
                Files.copy(sourceLogo.toPath, logofileCopyTo)
              }
            }
            if (wkToCopy.programm.id == w.programmId) {
              updateOrInsertPlanTimes(loadWettkampfDisziplinTimes(wkToCopy.id).map(_.toWettkampfPlanTimeRaw.copy(wettkampfId = w.id)))
              updateScoreCalcTemplates(loadScoreCalcTemplates(wkToCopy.id).map(_.copy(wettkampfId = Some(w.id))))
            }

            if (!targetFolder.equals(sourceFolder)) {
              sourceFolder
                .listFiles()
                .filter(f => f.getName.endsWith(".scoredef"))
                .toList
                .sortBy {
                  _.getName
                }
                .foreach(scoreFileSource => {
                  val targetFilePath = targetFolder.toPath.resolve(scoreFileSource.getName)
                  if (!targetFilePath.toFile.exists()) {
                    Files.copy(scoreFileSource.toPath, targetFilePath)
                  }
                })
            }
            val scores = Await.result(listPublishedScores(UUID.fromString(wkToCopy.uuid.get)), Duration.Inf)
            scores.foreach(score => {
              savePublishedScore(wettkampfId = w.id, title = score.title, query = score.query, published = false, propagate = false)
            })
          })
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
    val item = makeMenuAction("Wettkampf herunterladen") { (caption, action) =>
      implicit val e = action
      val wklist = server.httpGet(s"${remoteAdminBaseUrl}/api/competition").map {
        case entityString: String => entityString.asType[List[Wettkampf]]
        case _ => List[Wettkampf]()
      }
      wklist.onComplete {
        case Success(wkl: List[Wettkampf]) =>
          val wklviews = wkl
            .map(wk => wk.toView(readProgramm(wk.programmId)))
          Platform.runLater {
            val wkTable = WettkampfTableView(wklviews)
            PageDisplayer.showInDialog(caption, new DisplayablePage() {
              def getPage: Node = {
                new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  minWidth = 600
                  center = new BorderPane {
                    hgrow = Priority.Always
                    vgrow = Priority.Always
                    top = wkTable.filter
                    center = wkTable
                    minWidth = 550
                  }

                }
              }
            }, new Button("OK") {
              disable <== when(wkTable.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false
              onAction = (event: ActionEvent) => {
                if (!wkTable.selectionModel.value.isEmpty) {
                  wkTable.items.value.zipWithIndex.filter {
                    x => wkTable.selectionModel.value.isSelected(x._2)
                  }.map { x =>
                    val (wettkampf, _) = x
                    val url = s"$remoteAdminBaseUrl/api/competition/${wettkampf.uuid.get}"
                    invokeAsyncWithBusyIndicator[Wettkampf](s"Wettkampf ${wettkampf.easyprint} herunterladen") {
                      val pr = Promise[Wettkampf]()
                      server.httpDownloadRequest(server.makeHttpGetRequest(url)).onComplete(ft =>
                        Platform.runLater {
                          ft match {
                            case Success(w) =>
                              pr.success(w)
                              updateTree
                              val text = s"${w.titel} ${w.datum}"
                              tree.getLeaves("Wettkämpfe").find { item => text.equals(item.value.value) } match {
                                case Some(node) => controlsView.selectionModel().select(node)
                                case None =>
                              }
                            case Failure(error) =>
                              pr.failure(error)
                              PageDisplayer.showErrorDialog(caption)(error)
                          }
                        }
                      )
                      pr.future
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
      isLocalHostServer
    },
      LocalServerStates.localServerProperty)) choose true otherwise false
    item
  }

  def makeNeuerWettkampfImportierenMenu: MenuItem = {
    makeMenuAction("Wettkampf importieren") { (caption, action) =>
      implicit val e = action
      val fileChooser = new FileChooser {
        title = "Wettkampf File importieren"
        initialDirectory = new java.io.File(homedir)
        extensionFilters ++= Seq(
          new ExtensionFilter("Zip-Files", "*.zip"),
          new ExtensionFilter("All Files", "*.*")
        )
      }
      val selectedFile = fileChooser.showOpenDialog(getStage())
      import scala.concurrent.ExecutionContext.Implicits._
      if (selectedFile != null) {
        val wf = KuTuApp.invokeAsyncWithBusyIndicator[Wettkampf](caption) {
          Future[Wettkampf] {
            val is = new FileInputStream(selectedFile)
            val w = ResourceExchanger.importWettkampf(is)
            is.close()
            val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
            if (!dir.exists()) {
              dir.mkdirs();
            }
            w
          }
        }
        wf.onComplete { tr =>
          Platform.runLater {
            tr match {
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
            }
          }
        }
      }
    }
  }

  def makeShowQRCodeMenu(p: WettkampfView) = {
    val item = makeMenuAction("Mobile App Connections ...") { (caption, action) =>
      showQRCode(caption, p)
    }
    item.disable <== when(Bindings.createBooleanBinding(() => !p.toWettkampf.hasSecred(homedir, remoteHostOrigin) || !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")),
      ConnectionStates.connectedWithProperty, selectedWettkampfSecret, controlsView.selectionModel().selectedItem)) choose true otherwise false
    item
  }

  def showQRCode(caption: String, p: WettkampfView) = {
    val secretOrigin = if (Config.isLocalHostServer) {
      if (!p.toWettkampf.hasSecred(homedir, "localhost")) {
        p.toWettkampf.saveSecret(homedir, "localhost", jwt.JsonWebToken(jwtHeader, setClaims(p.uuid.get, Int.MaxValue), jwtSecretKey))
      }
      "localhost"
    } else remoteHostOrigin
    p.uuid
      .zip(AuthSupport.getClientSecret)
      .zip(p.toWettkampf.readSecret(homedir, secretOrigin)) match {
      case Some(((uuid, shortsecret), secret)) =>
        val shorttimeout = getExpiration(shortsecret).getOrElse(new Date())
        val longtimeout = getExpiration(secret).getOrElse(new Date())

        val connectionList = if (Config.isLocalHostServer) {
          List(Config.remoteBaseUrl) //server.listNetworkAdresses.toList
        } else List(remoteBaseUrl)
        val tablist = connectionList.map { address =>
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
          val urlLabel = new Hyperlink(s"Link (gültig bis ${formatDateTime(shorttimeout)} UTC) im Browser öffnen")
          urlLabel.onMouseClicked = _ => {
            Clipboard.systemClipboard.content = ClipboardContent(
              DataFormat.PlainText -> shortConnectionString,
              DataFormat.Html -> s"<a href='$shortConnectionString' target='_blank'>Link (gültig bis ${formatDateTime(shorttimeout)} UTC) im Browser öffnen</a> text"
            )
            hostServices.showDocument(shortConnectionString)
          }
          val mailLabel = new Hyperlink(s"Link (gültig bis ${formatDateTime(shorttimeout)} UTC) als EMail versenden")
          mailLabel.onMouseClicked = _ => {
            val judges = KuTuServer.getAllJudgesRemote(p.toWettkampf)
              .flatMap(_._2)
              .map(_.mail)
              .mkString(";")
            val mailURIStr = String.format("mailto:%s?subject=%s&bcc=%s&body=%s",
              p.notificationEMail,
              encodeURIParam(s"Link für Datenerfassung im Wettkampf (${p.easyprint})"),
              judges,
              encodeURIParam(
                s"""  Geschätze(r) Wertungsrichter(in)
                   |
                   |  mit dem folgenden Link kommst Du in die App, in der Du die Wettkampf-Resultate
                   |  für den Wettkampf '${p.easyprint}' erfassen kannst:
                   |  ${shortConnectionString}
                   |
                   |  Wichtig:
                   |  * Dieser Link ist bis am ${formatDateTime(shorttimeout)} UTC gültig.
                   |  * Der Link kann bis dahin beliebig of verwendet werden, um die Berechtigung
                   |    für die Erfassung von Wertungen freizuschalten.
                   |  * Bitte den Link vertraulich behandeln - nur Du darfst mit diesem Link einsteigen.
                   |
                   |  Sportliche Grüsse,
                   |  Wertungsrichter-Einsatzplanung
                """.stripMargin))
            hostServices.showDocument(mailURIStr)
          }

          val outLast = QRCode.from(lastResultsConnectionString).to(ImageType.PNG).withSize(500, 500).stream()
          val inLast = new ByteArrayInputStream(outLast.toByteArray())

          val imageLast = new Image(inLast)
          val viewLast = new ImageView(imageLast)
          val urlLabelLast = new Hyperlink("Letzte Resultate Link im Browser öffnen")
          urlLabelLast.onMouseClicked = _ => {
            Clipboard.systemClipboard.content = ClipboardContent(
              DataFormat.PlainText -> lastResultsConnectionString,
              DataFormat.Html -> s"<a href='$lastResultsConnectionString' target='_blank'>Letzte Resultate Link im Browser öffnen</a> text"
            )
            hostServices.showDocument(lastResultsConnectionString)
          }
          val mailLabelLast = new Hyperlink("Letzte Resultate Link als EMail versenden")
          mailLabelLast.onMouseClicked = _ => {
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
            hostServices.showDocument(mailURIStr)
          }


          val outTop = QRCode.from(topResultsConnectionString).to(ImageType.PNG).withSize(500, 500).stream()
          val inTop = new ByteArrayInputStream(outTop.toByteArray())

          val imageTop = new Image(inTop)
          val viewTop = new ImageView(imageTop)
          val urlLabelTop = new Hyperlink("Top Resultate Link im Browser öffnen")
          urlLabelTop.onMouseClicked = _ => {
            Clipboard.systemClipboard.content = ClipboardContent(
              DataFormat.PlainText -> topResultsConnectionString,
              DataFormat.Html -> s"<a href='$topResultsConnectionString' target='_blank'>Top Resultate im Browser öffnen</a> text"
            )
            hostServices.showDocument(topResultsConnectionString)
          }
          val mailLabelTop = new Hyperlink("Top Resultate Link als EMail versenden")
          mailLabelTop.onMouseClicked = _ => {
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
            hostServices.showDocument(mailURIStr)
          }
          val urlLastLabel = new Hyperlink("Link auf 'Letzte Resultate'")
          urlLastLabel.onMouseClicked = _ => {
            Clipboard.systemClipboard.content = ClipboardContent(
              DataFormat.PlainText -> lastResultsConnectionString,
              DataFormat.Html -> s"<a href='$lastResultsConnectionString' target='_blank'>Link auf 'Letzte Resultate'</a> text"
            )
            hostServices.showDocument(lastResultsConnectionString)
          }
          val urlTopLabel = new Hyperlink("Link auf 'Top-Resultate'")
          urlTopLabel.onMouseClicked = _ => {
            Clipboard.systemClipboard.content = ClipboardContent(
              DataFormat.PlainText -> topResultsConnectionString,
              DataFormat.Html -> s"<a href='$topResultsConnectionString' target='_blank'>Link auf 'Top-Resultate'</a> text"
            )
            hostServices.showDocument(topResultsConnectionString)
          }
          view.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
          viewTop.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
          viewLast.setStyle("-fx-stroke-width: 2; -fx-stroke: blue")
          new Tab {
            text = address
            closable = false
            content = new TabPane {
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
              tabs.add(new Tab {
                text = "Letzte Resultate Link"
                closable = false
                content = new VBox {
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
                content = new VBox {
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
          def getTabbedPage: Node = new TabPane {
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

  def makeWettkampfLoeschenMenu(p: WettkampfView) = makeMenuAction("Wettkampf löschen") { (caption, action) =>
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
        onAction = handleAction { implicit e: ActionEvent =>
          deleteRegistrations(p.uuid.map(UUID.fromString).get)
          deleteWettkampf(p.id)
          updateTree
        }
      }
    )
  }

  def makeWettkampfDataDirectoryMenu(w: WettkampfView) = makeMenuAction("Wettkampf Verzeichnis öffnen") { (caption, action) =>
    val dir = new java.io.File(homedir + "/" + encodeFileName(w.easyprint))
    if (!dir.exists()) {
      dir.mkdirs()
    }

    hostServices.showDocument(dir.toURI.toString)
  }

  def makeVereinLoeschenMenu(v: Verein) = makeMenuAction("Verein löschen") { (caption, action) =>
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
        onAction = handleAction { implicit e: ActionEvent =>
          deleteVerein(v.id)
          updateTree
        }
      })
  }

  def makeVereinUmbenennenMenu(v: Verein) = makeMenuAction("Verein umbenennen") { (caption, action) =>
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
        onAction = handleAction { implicit e: ActionEvent =>
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

  def getStage() = stage

  override def start(): Unit = {
    val pForm = new ProgressForm(Some(new PrimaryStage {
      icons += new Image(this.getClass.getResourceAsStream("/images/app-logo.png"))
    }))
    val startSteps = TaskSteps("")
    pForm.activateProgressBar("Wettkampf App startet ...", startSteps, startUI)
    startSteps.nextStep("Starte die Datenbank ...", startDB)
    startSteps.nextStep("Bereinige veraltete Daten ...", cleanupDB)
    new Thread(startSteps).start()
  }

  def startDB(): Unit = {
    logger.info(database.source.toString)
  }
  
  def cleanupDB(): Unit = {
    markAthletesInactiveOlderThan(3)
  }

  def startUI(): Unit = {
    logger.info("starte das UI ...")
    rootTreeItem = new TreeItem[String]("Dashboard") {
      expanded = true
      children = tree.getTree
    }
    controlsView = new TreeView[String]() {
      minWidth = 5
      maxWidth = 400
      prefWidth = 200
      editable = true
      root = rootTreeItem
      id = "page-tree"
    }
    val btnWettkampfModus = new ToggleButton("Wettkampf-Modus") {
      id = "wettkampfmodusButton"
      styleClass += "toggle-button2"
      selected <==> modelWettkampfModus
      disable = true
    }

    val btnConnectStatus = new ToggleButton() {
      //id = "connected-info"
      styleClass += "toggle-button2"

      disable <== btnWettkampfModus.disable
      onAction = handleAction { action =>
        if (ConnectionStates.connectedProperty.value) {
          ConnectionStates.disconnected()
        } else {
          validateConnect(selectedWettkampf.value, "Share", action) { caption =>
            connectAndShare(selectedWettkampf.value, caption, action)
          }
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
    val lblRemoteAddress = new MenuButton() {
      id = "connected-info"
      text <== createStringBinding(() => {
        ConnectionStates.connectedWithProperty.value match {
          case "" =>
            s"Server: ${Config.remoteBaseUrl}offline\nVersion: ${Config.appFullVersion}, Built: ${Config.builddate}"
          case uuid =>
            s"Server: ${Config.remoteBaseUrl}online\nVersion: ${Config.appFullVersion}, Built: ${Config.builddate}"
        }
      }, ConnectionStates.connectedWithProperty, LocalServerStates.localServerProperty, ConnectionStates.remoteServerProperty)
      items += makeSelectBackendMenu
      items += makeStartServerMenu
      items += makeStopServerMenu
      items += makeProxyLoginMenu
      items += makeWettkampfHerunterladenMenu
    }
    val centerPane = PageDisplayer.choosePage(modelWettkampfModus, None, "dashBoard", tree)
    val splitPane = new SplitPane {
      dividerPositions = 0.2
      id = "page-splitpane"
      //items.addAll(scrollPane, centerPane)
      items.addAll(controlsView, centerPane)
    }
    val header = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      prefHeight = 76
      maxHeight = 76
      minWidth = 5
      id = "mainHeader"
      left = new ImageView {
        image = new Image(this.getClass.getResourceAsStream("/images/logo.png"))
        margin = Insets(15, 0, 0, 10)
      }
      right = new ToolBar {
        id = "mainToolBar"
        minWidth = 5
        vgrow = Priority.Always
        hgrow = Priority.Always
        content = List(btnWettkampfModus, btnConnectStatus, lblRemoteAddress)
      }
    }
    invisibleWebView = new WebView()
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
      minWidth = 5
      children = Seq(webViewContainer, header)
    }
    controlsView.onContextMenuRequested = _ => {
      val sel = controlsView.selectionModel().selectedItem
      sel.value.value.value
    }
    controlsView.selectionModel().selectionMode = SelectionMode.Single
    controlsView.selectionModel().selectedItem.onChange { (_, _, newItem) =>
      if (!lastSelected.value.equals("Updating")) {
        btnWettkampfModus.disable.value = true
        if (newItem != null && !newItem.value.value.equals(lastSelected.value)) {
          lastSelected.value = newItem.value.value
          newItem.value.value match {
            case "Athleten" =>
              controlsView.contextMenu = new ContextMenu() {
                items += makeNeuerVereinAnlegenMenu
                items += makeFindDuplicteAthletes
              }
            case "Wettkämpfe" =>
              controlsView.contextMenu = new ContextMenu() {
                items += makeNeuerWettkampfAnlegenMenu()
                items += makeNeuerWettkampfImportierenMenu
                items += new Menu("Netzwerk") {
                  //items += makeLoginMenu
                  items += makeSelectBackendMenu
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
                        controlsView.contextMenu = new ContextMenu() {
                          items += makeWettkampfDurchfuehrenMenu(p)
                          items += makeWettkampfBearbeitenMenu(p)
                          items += makeWettkampfKopierenMenu(p)
                          items += makeWettkampfExportierenMenu(p)
                          items += makeWettkampfDataDirectoryMenu(p)
                          items += makeWettkampfLoeschenMenu(p)
                        }
                      case Some(KuTuAppThumbNail(v: Verein, _, newItem)) =>
                        controlsView.contextMenu = new ContextMenu() {
                          items += makeVereinUmbenennenMenu(v)
                          items += makeVereinLoeschenMenu(v)
                        }
                      case _ => controlsView.contextMenu = new ContextMenu()
                    }
                  }
                  case _ => controlsView.contextMenu = new ContextMenu()
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
              if (splitPane.items.size > 1) {
                splitPane.items.remove(1)
              }
              splitPane.items.add(1, centerPane)
            }
          }
        }

    var divider: Option[Double] = None
    modelWettkampfModus.onChange {
      if (modelWettkampfModus.value) {
        divider = Some(splitPane.dividerPositions(0))
      }
      if (!modelWettkampfModus.value && (divider match {
        case Some(_) => true
        case _ => false
      })) {
        splitPane.dividerPositions = divider.get
      }
      else {
        splitPane.dividerPositions = 0d
      }
      resetBestenResults
    }

    //
    // Layout the main stage
    //
    stage = new PrimaryStage {
      //initStyle(StageStyle.TRANSPARENT);
      title = "KuTu Wettkampf-App"
      icons += new Image(this.getClass.getResourceAsStream("/images/app-logo.png"))
      val sceneWidth = 1200
      val sceneHeigth = 750
      scene = new Scene(sceneWidth, sceneHeigth) {
        root = new BorderPane {
          top = headerContainer
          center = new BorderPane {
            center = splitPane
          }
          styleClass += "application"
        }

      }
      val bounds = Screen.primary.bounds
      x = bounds.minX + bounds.width / 2 - sceneWidth / 2
      y = bounds.minY + bounds.height / 2 - sceneHeigth / 2
      val st = this.getClass.getResource("/css/Main.css")
      if (st == null) {
        logger.debug("Ressource /css/main.css not found. Class-Anchor: " + this.getClass)
      }
      else if (scene() == null) {
        logger.debug("scene() == null")
      }
      else if (scene().getStylesheets == null) {
        logger.debug("scene().getStylesheets == null")
      }
      else {
        scene().stylesheets.add(st.toExternalForm)
      }
    }
    logger.info("UI ready")
  }
}