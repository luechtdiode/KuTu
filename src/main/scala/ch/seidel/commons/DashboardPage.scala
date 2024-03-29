package ch.seidel.commons

import ch.seidel.kutu.KuTuAppTree
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
      vgrow = Priority.Always
      hgrow = Priority.Always
      fitToHeight = true
      fitToWidth = true
      vbarPolicy = ScrollPane.ScrollBarPolicy.Always
      content = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = thumbs
        styleClass += "category-page"
      }
    }
  }
}
