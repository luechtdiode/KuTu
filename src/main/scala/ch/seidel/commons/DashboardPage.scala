package ch.seidel.commons

import scalafx.Includes._
import ch.seidel.KuTuAppTree
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.{Priority, VBox}

/** Dashboard Page */
class DashboardPage(dashPart: String = "dashboard", tree: KuTuAppTree) extends DisplayablePage {

  def getPage = {
    val thumbs = dashPart match {
      case "dashboard" => tree.getDashThumbsCtrl
      case _           => tree.getDashThumb(dashPart)
    }

    new ScrollPane {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
      fitToHeight = true
      fitToWidth = true
      content = new VBox {
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        content = thumbs
      }
      styleClass += "category-page"
    }
  }
}
