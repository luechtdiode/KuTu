package ch.seidel.commons

import javafx.scene.{ control => jfxsc }
import javafx.{ scene => jfxs }
import scalafx.stage.Stage
import scalafx.geometry.Insets
import scalafx.scene.layout.{Priority, VBox}
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.Button
import ch.seidel.KuTuAppTree
import ch.seidel.domain.WettkampfView
import scalafx.scene.control.ToolBar
import scalafx.event.ActionEvent
import scalafx.stage.Modality
import scalafx.stage.Window
import scalafx.scene.Node
import scalafx.scene.layout.HBox
import scalafx.geometry.Pos
import ch.seidel.WettkampfPage
import ch.seidel.TurnerPage
import ch.seidel.domain.Verein
import scalafx.scene.control.TreeItem

/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {

  def showInDialog(tit: String, nodeToAdd: DisplayablePage, commands: Button*)(implicit event: ActionEvent) {
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WINDOW_MODAL)
        val node = event.source.asInstanceOf[jfxs.Node]
        delegate.initOwner(node.getScene.getWindow)
        title = tit
        scene = new Scene {
          root = new BorderPane {
            padding = Insets(15)
            center = nodeToAdd.getPage
            bottom = new HBox {
              prefHeight = 50
              alignment = Pos.BOTTOM_RIGHT
              hgrow = Priority.ALWAYS
              content = commands
            }
            var first = false
            commands.foreach { btn =>
              if(!first) {
                first = true
                btn.defaultButton = true
              }
              btn.minWidth = 100
              btn.filterEvent(ActionEvent.ACTION) { () => outer.close()}}
          }
        }
      }
    }
    // Show dialog and wait till it is closed
    dialogStage.showAndWait()
  }

  def choosePage(context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _           => context match {
        case Some(w: WettkampfView) => chooseWettkampfPage(w, tree)
        case Some(v: Verein)        => chooseVereinPage(v, tree)
        case _                      => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
      }
    }
  }
  private def chooseWettkampfPage(wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    displayPage(WettkampfPage.buildTab(wettkampf, tree.getService))
  }
  private def chooseVereinPage(verein: Verein, tree: KuTuAppTree): Node = {
    displayPage(TurnerPage.buildTab(verein, tree.getService))
  }

  private def displayPage(nodeToAdd: DisplayablePage): Node = {
    new VBox {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
      content = nodeToAdd.getPage
    }
  }
}
