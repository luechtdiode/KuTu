package ch.seidel.commons

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import javafx.scene.{ control => jfxsc }
import javafx.{ scene => jfxs }
import scalafx.stage.Stage
import scalafx.geometry.Insets
import scalafx.scene.layout.{Priority, VBox}
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.Button
import scalafx.scene.control.ToolBar
import scalafx.event.ActionEvent
import scalafx.stage.Modality
import scalafx.stage.Window
import scalafx.scene.Node
import scalafx.scene.layout.HBox
import scalafx.geometry.Pos
import scalafx.scene.control.TreeItem
import scalafx.scene.Cursor
import scalafx.scene.control.Label
import ch.seidel.kutu.view._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.KuTuAppTree
import ch.seidel.kutu.KuTuApp
import scalafx.beans.property.BooleanProperty

/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {

  def showInDialog(tit: String, nodeToAdd: DisplayablePage, commands: Button*)(implicit event: ActionEvent) {
    val buttons = commands :+ new Button(if(commands.length == 0) "Schliessen" else "Abbrechen")
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WINDOW_MODAL)
        event.source match {
          case n: jfxs.Node if(n.getScene.getRoot == KuTuApp.getStage.getScene.getRoot) =>
            delegate.initOwner(n.getScene.getWindow)
          case _ =>
            delegate.initOwner(KuTuApp.getStage.getScene.getWindow)
        }

        title = tit
        scene = new Scene {
          root = new BorderPane {
            padding = Insets(15)
            center = nodeToAdd.getPage
            bottom = new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = buttons
            }
            var first = false
            buttons.foreach { btn =>
              if(!first) {
                first = true
                btn.defaultButton = true
              }
              btn.minWidth = 100
              btn.filterEvent(ActionEvent.Action) { () => outer.close()}}
          }
        }
      }
    }
    // Show dialog and wait till it is closed
    dialogStage.showAndWait()
  }

  def choosePage(wettkampfmode: BooleanProperty, context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _           => context match {
        case Some(w: WettkampfView) => chooseWettkampfPage(wettkampfmode, w, tree)
        case Some(v: Verein)        => chooseVereinPage(v, tree)
        case _                      => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
      }
    }
  }
  private def chooseWettkampfPage(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    displayPage(WettkampfPage.buildTab(wettkampfmode, wettkampf, tree.getService))
  }
  private def chooseVereinPage(verein: Verein, tree: KuTuAppTree): Node = {
    displayPage(TurnerPage.buildTab(verein, tree.getService))
  }

  var activePage: Option[DisplayablePage] = None
  
  private def displayPage(nodeToAdd: DisplayablePage): Node = {
    activePage match {
      case Some(p) if(p != nodeToAdd) => 
        p.release()
      case _ =>
    }
    activePage = Some(nodeToAdd)
    val ret = new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      val indicator = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = Seq(new Label("loading ..."))
        styleClass += "category-page"
      }
      children = Seq(indicator)
    }
    def op = {
      val p = nodeToAdd.getPage
      ret.children = p
      ret.requestLayout()
    }
    KuTuApp.invokeWithBusyIndicator(op)
    ret
  }
}
