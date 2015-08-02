package ch.seidel

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
import scalafx.scene.input.ContextMenuEvent
import ch.seidel.domain.KutuService
import ch.seidel.domain.WettkampfView
import ch.seidel.domain.Verein
import ch.seidel.commons.PageDisplayer
import ch.seidel.commons.DisplayablePage
import scalafx.event.ActionEvent
import javafx.scene.control.DatePicker
import scalafx.collections.ObservableBuffer
import ch.seidel.domain.ProgrammView
import javafx.util.Callback
import java.time.temporal.TemporalField
import java.time.ZoneId
import java.util.Date
import java.time.LocalDate

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
      handler(event)
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
      import domain._
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
                  new Label(txtAuszeichnung.promptText.value), txtAuszeichnung)
            }
          }
        }
      },
      new Button("OK") {
        import domain._
        onAction = handleAction {implicit e: ActionEvent =>
          try {
            saveWettkampf(
              p.id,
              ld2SQLDate(txtDatum.valueProperty().value),
              txtTitel.text.value,
              Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
              txtAuszeichnung.text.value.filter(c => c.isDigit).toString match {
                case ""        => 0
                case s: String => str2Int(s)
              })
          }
          catch {
            case e: IllegalArgumentException =>
          }
          updateTree
        }
      }
    )}
  }

  def makeNeuerVereinAnlegenMenu = makeMenuAction("Neuen Verein anlegen ...") {(caption, action) =>
    implicit val e = action
    import domain._

    val txtTitel = new TextField {
      prefWidth = 500
      promptText = "Vereinsname"
    }
    PageDisplayer.showInDialog(caption, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          center = new VBox {
            children.addAll(new Label(txtTitel.promptText.value), txtTitel)
          }
        }
      }
    }, new Button("OK") {
      onAction = handleAction {implicit e: ActionEvent =>
        createVerein(txtTitel.text.value)
        updateTree
      }
    })
  }

  def makeNeuerWettkampfAnlegenMenu = makeMenuAction("Neuen Wettkampf anlegen ...") {(caption, action) =>
    implicit val e = action
    import domain._
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
                new Label(txtAuszeichnung.promptText.value), txtAuszeichnung)
          }
        }
      }
    }, new Button("OK") {
      onAction = handleAction {implicit e: ActionEvent =>
        createWettkampf(
            ld2SQLDate(txtDatum.valueProperty().value),
            txtTitel.text.value,
            Set(cmbProgramm.selectionModel.value.getSelectedItem.id),
            txtAuszeichnung.text.value.filter(c => c.isDigit).toString match {
              case ""        => 0
              case s: String => str2Int(s)
            },
            Some({ (_, _) => false }))
        updateTree
      }
    })
  }
  def makeWettkampfLoeschenMenu(p: WettkampfView) = makeMenuAction("Wettkampf löschen") {(caption, action) =>
    implicit val e = action
    import domain._
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

  def makeVereinLoeschenMenu(v: Verein) = makeMenuAction("Verein löschen") {(caption, action) =>
    implicit val e = action
    import domain._
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

  controlsView.selectionModel().selectionMode = SelectionMode.SINGLE
  controlsView.selectionModel().selectedItem.onChange { (_, _, newItem) =>
    import domain._
    if(newItem != null) {
      newItem.value.value match {
        case "Athleten" =>
          controlsView.contextMenu = new ContextMenu() {
            items += makeNeuerVereinAnlegenMenu
          }
        case "Wettkämpfe" =>
          controlsView.contextMenu = new ContextMenu() {
            items += makeNeuerWettkampfAnlegenMenu
          }
        case _ => (newItem.isLeaf, Option(newItem.getParent)) match {
            case (true, Some(parent)) => {
              tree.getThumbs(parent.getValue).find(p => p.button.text.getValue.equals(newItem.getValue)) match {
                case Some(KuTuAppThumbNail(p: WettkampfView, _, newItem)) =>
                controlsView.contextMenu = new ContextMenu() {
                  items += makeWettkampfBearbeitenMenu(p)
                  items += makeWettkampfLoeschenMenu(p)
                }
                case Some(KuTuAppThumbNail(v: Verein, _, newItem)) =>
                  controlsView.contextMenu = new ContextMenu() {
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
  def getStage() = stage
  //
  // Layout the main stage
  //
  stage = new PrimaryStage {
    title = "KuTu Wettkampf-App"
    scene = new Scene(1020, 700) {
      root = new BorderPane {
        top = new VBox {
          vgrow = Priority.Always
          hgrow = Priority.Always
          content = new ToolBar {
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
        }
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