package ch.seidel.kutu

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
import javafx.scene.control.DatePicker
import scalafx.collections.ObservableBuffer
import javafx.util.Callback
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

object KuTuApp extends JFXApp with KutuService {
  var tree = AppNavigationModel.create(KuTuApp.this)
  val rootTreeItem = new TreeItem[String]("Dashboard") {
    expanded = true
    children = tree.getTree
  }

  var centerPane = PageDisplayer.choosePage(None, "dashBoard", tree)

  def updateTree {
    tree = AppNavigationModel.create(KuTuApp.this)
    rootTreeItem.children = tree.getTree
  }
  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R) = new javafx.event.EventHandler[J] {
    def handle(event: J) {
      setCursor(Cursor.WAIT)
      try {
        handler(event)
      }
      finally {
        setCursor(Cursor.DEFAULT)
      }
    }
  }

  def invokeWithBusyIndicator(task: => Unit) {
    setCursor(Cursor.WAIT)
    import scala.concurrent.ExecutionContext.Implicits.global
    val f = Future[Boolean] {
      Thread.sleep(10L)// currentThread().wait(1L)
      Platform.runLater{
        try {
          task
        }
        finally {
          setCursor(Cursor.DEFAULT)
        }
      }
      true
    }
  }

  def invokeAsyncWithBusyIndicator[R](task: => R): Future[R] = {
    setCursor(Cursor.WAIT)
    val p = Promise[R]
    import scala.concurrent.ExecutionContext.Implicits.global
    val f = Future[Boolean] {
//      Thread.sleep(100L)// currentThread().wait(1L)
      try {
        val ret = task
        Platform.runLater{
          try {
            p.success(ret)
          }
          catch {
            case e:Exception => p.failure(e)
          }
          finally {
            setCursor(Cursor.DEFAULT)
          }
        }
      }
      catch {
        case e:Exception =>
          setCursor(Cursor.DEFAULT)
          p.failure(e)
      }
      true
    }
    p.future
  }
  var cursorWaiters = 0
  def setCursor(c: Cursor) {
    val ctoSet = if(c.equals(Cursor.WAIT)) {
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
    //println(cursorWaiters)
    if(getStage() != null) {
      getStage().scene.root.value.cursor.value = ctoSet
      getStage().scene.root.value.requestLayout()
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
        text = p.auszeichnung + "%"
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
        onAction = handleAction {implicit e: ActionEvent =>
          try {
            val w = saveWettkampf(
              p.id,
              ld2SQLDate(txtDatum.valueProperty().value),
              txtTitel.text.value,
              Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
              txtAuszeichnung.text.value.filter(c => c.isDigit).toString match {
                case ""        => 0
                case s: String => str2Int(s)
              },
              txtAuszeichnungEndnote.text.value match {
                case ""        => 0
                case s: String => try {BigDecimal.valueOf(s)} catch {case e:Exception => 0}
              })
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
         initialFileName.value = p.titel.replace(" ", "_") + ".zip"
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

  def makeNeuerVereinAnlegenMenu = makeMenuAction("Neuen Verein anlegen ...") {(caption, action) =>
    implicit val e = action

    val txtTitel = new TextField {
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
                new Label(txtTitel.promptText.value), txtTitel,
                new Label(txtVerband.promptText.value), txtVerband
                )
          }
        }
      }
    }, new Button("OK") {
      onAction = handleAction {implicit e: ActionEvent =>
        val vid = createVerein(
            txtTitel.text.value.trim(),
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
        text = "40%"
      }
      val txtAuszeichnungEndnote = new TextField {
        prefWidth = 500
        promptText = "Auszeichnung bei Erreichung des Mindest-Endwerts"
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
        onAction = handleAction {implicit e: ActionEvent =>
          val w = createWettkampf(
              ld2SQLDate(txtDatum.valueProperty().value),
              txtTitel.text.value,
              Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
              txtAuszeichnung.text.value.filter(c => c.isDigit).toString match {
                case ""        => 0
                case s: String => str2Int(s)
              },
              txtAuszeichnungEndnote.text.value match {
                case ""        => 0
                case s: String => try {BigDecimal.valueOf(s)} catch {case e:Exception => 0}
              },
              Some({ (_, _) => false }))
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
        if (selectedFile != null) {
          val wf = KuTuApp.invokeAsyncWithBusyIndicator {
            val w = ResourceExchanger.importWettkampf(selectedFile.getAbsolutePath)
            val dir = new java.io.File(homedir + "/" + w.easyprint.replace(" ", "_"))
            if(!dir.exists()) {
              dir.mkdirs();
            }
            w
          }
          import scala.concurrent.ExecutionContext.Implicits._
          wf.andThen {
            case Failure(f) => println(f)
            case Success(w) =>
              Platform.runLater {
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

  controlsView.selectionModel().selectionMode = SelectionMode.SINGLE
  controlsView.selectionModel().selectedItem.onChange { (_, _, newItem) =>
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
          }
        case _ => (newItem.isLeaf, Option(newItem.getParent)) match {
            case (true, Some(parent)) => {
              tree.getThumbs(parent.getValue).find(p => p.button.text.getValue.equals(newItem.getValue)) match {
                case Some(KuTuAppThumbNail(p: WettkampfView, _, newItem)) =>
                controlsView.contextMenu = new ContextMenu() {
                  items += makeWettkampfBearbeitenMenu(p)
                  items += makeWettkampfExportierenMenu(p)
                  items += makeWettkampfDataDirectoryMenu(p)
                  items += makeWettkampfLoeschenMenu(p)
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
              PageDisplayer.choosePage(Some(p), "dashBoard - " + newItem.getValue, tree)
            case Some(KuTuAppThumbNail(v: Verein, _, newItem)) =>
              PageDisplayer.choosePage(Some(v), "dashBoard - " + newItem.getValue, tree)
            case _ =>
              PageDisplayer.choosePage(None, "dashBoard - " + newItem.getValue, tree)
          }
        }
        case (false, Some(_)) =>
          PageDisplayer.choosePage(None, "dashBoard - " + newItem.getValue, tree)
        case (_, _) =>
          PageDisplayer.choosePage(None, "dashBoard", tree)
      }
      if(splitPane.items.size > 1) {
        splitPane.items.remove(1)
      }
      splitPane.items.add(1, centerPane)
    }
  }

  val scrollPane = new ScrollPane {
    minWidth = 200
    maxWidth = 400
    fitToWidth = true
    fitToHeight = true
    id = "page-tree"
    content = controlsView
  }

  val splitPane = new SplitPane {
    dividerPositions = 0
    id = "page-splitpane"
    items.addAll(scrollPane, centerPane)
  }

  val header = new ToolBar {
          vgrow = Priority.Always
          hgrow = Priority.Always
          prefHeight = 76
          maxHeight = 76
          id = "mainToolBar"
          content = List(
            new ImageView {
              image = new Image(
                this.getClass.getResourceAsStream("/images/logo.png"))
              margin = Insets(0, 0, 0, 10)
            })
          }

  def getStage() = stage
  //
  // Layout the main stage
  //
  stage = new PrimaryStage {
    title = "KuTu Wettkampf-App"
    scene = new Scene(1020, 700) {
      root = new BorderPane {
        top = header
        center = new BorderPane {
          center = splitPane
        }
        styleClass += "application"
      }

    }
    val st = this.getClass.getResource("/css/Main.css")
    if(st == null) {
      println("Ressource /css/main.css not found. Class-Anchor: " + this.getClass)
    }
    else if(scene() == null) {
    	println("scene() == null")
    }
    else if(scene().getStylesheets == null) {
      println("scene().getStylesheets == null")
    }
    else {
    	scene().stylesheets.add(st.toExternalForm)
    }
  }
}