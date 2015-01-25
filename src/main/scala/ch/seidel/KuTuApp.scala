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

object KuTuApp extends JFXApp with KutuService {
  val tree = AppNavigationModel.create(KuTuApp.this)
  val rootTreeItem = new TreeItem[String]("Dashboard") {
    expanded = true
    children = tree.getTree
  }
  var centerPane = PageDisplayer.choosePage(None, "dashBoard", tree)

  def handleAction[J <: javafx.event.ActionEvent, R](handler: scalafx.event.ActionEvent => R) = new javafx.event.EventHandler[J] {
    def handle(event: J) {
      handler(event)
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
  controlsView.selectionModel().selectionMode = SelectionMode.SINGLE
  controlsView.selectionModel().selectedItem.onChange {
    (_, _, newItem) => {
      if(newItem != null) {
        newItem.value.value match {
          case "Wettkämpfe" =>
            controlsView.contextMenu = new ContextMenu() {
              items += new javafx.scene.control.MenuItem("neu anlegen ...") {
                onAction = handleAction { implicit e: ActionEvent =>
                  PageDisplayer.showInDialog(getText, new DisplayablePage() {
                    def getPage: Node = {
                      new BorderPane {
                        hgrow = Priority.ALWAYS
                        vgrow = Priority.ALWAYS
                        //createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long]
                        //center = athletTable
                      }
                    }
                  }, new Button("OK") {
                    onAction = handleAction {implicit e: ActionEvent =>
                    }
                  }, new Button("OK, Alle") {
                    onAction = handleAction {implicit e: ActionEvent =>
                    }
                  })
                }
              }
            }
          case _ => controlsView.contextMenu = new ContextMenu()
        }
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