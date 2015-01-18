package ch.seidel

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import ch.seidel.commons.PageDisplayer
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout._
import scalafx.stage.Screen
import ch.seidel.domain.KutuService
import ch.seidel.domain.WettkampfView
import ch.seidel.domain.Verein

object KuTuApp extends JFXApp with KutuService {
  val tree = AppNavigationModel.create(KuTuApp.this)
  val rootTreeItem = new TreeItem[String]("Dashboard") {
    expanded = true
    children = tree.getTree
  }
  var centerPane = PageDisplayer.choosePage(None, "dashBoard", tree)

  val screen = Screen.primary
  val controlsView = new TreeView[String]() {
    minWidth = 200
    maxWidth = 400
    editable = true
    root = rootTreeItem
    id = "page-tree"
  }
  controlsView.selectionModel().selectionMode = SelectionMode.SINGLE
  controlsView.selectionModel().selectedItem.onChange {
    (_, _, newItem) => {
      val centerPane = (newItem.isLeaf, Option(newItem.getParent)) match {
        case (true, Some(parent)) => {
          tree.getThumbs(parent.getValue).find(p => p.button.text.getValue.equals(newItem.getValue)) match {
            case Some(KuTuAppThumbNail(p: WettkampfView, _, newItem)) => PageDisplayer.choosePage(Some(p), "dashBoard - " + newItem.getValue, tree)
            case Some(KuTuAppThumbNail(v: Verein, _, newItem)) => PageDisplayer.choosePage(Some(v), "dashBoard - " + newItem.getValue, tree)
            case _       => PageDisplayer.choosePage(None, "dashBoard - " + newItem.getValue, tree)
          }
        }
        case (false, Some(_))     => PageDisplayer.choosePage(None, "dashBoard - " + newItem.getValue, tree)
        case (_, _)               => PageDisplayer.choosePage(None, "dashBoard", tree)
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

  //
  // Layout the main stage
  //
  stage = new PrimaryStage {
    title = "KuTu Wettkampf-App"
    //icons += new Image("/images/ScalaFX-icon-64x64.png")
    scene = new Scene(1020, 700) {
      root = new BorderPane {
        top = new VBox {
          vgrow = Priority.ALWAYS
          hgrow = Priority.ALWAYS
          content = new ToolBar {
            prefHeight = 76
            maxHeight = 76
            id = "mainToolBar"
            content = List(
              new ImageView {
                image = new Image(
                  this.getClass.getResourceAsStream("/images/logo.png"))
                margin = Insets(0, 0, 0, 10)
              }/*,
              new Region {
                minWidth = 300
              },
              new Button {
                minWidth = 120
                minHeight = 66
                id = "newButton"
              }*/)
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