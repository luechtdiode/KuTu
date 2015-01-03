package ch.seidel.commons

import scalafx.scene.Node
import scalafx.scene.layout.{Priority, VBox}
import ch.seidel.KuTuAppTree
import ch.seidel.domain.WettkampfView

/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {

  def choosePage(context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _ =>
        /*if (value.startsWith("dashBoard - ")) {

        } else*/ context match {
          case Some(w: WettkampfView) => chooseWettkampfPage(w, tree)
          case _                      => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
        }
    }
  }
  def chooseWettkampfPage(wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    displayPage(WettkampfPage.buildTab(wettkampf, tree.getService))
  }

  private def displayPage(nodeToAdd: DisplayablePage): Node = {
    new VBox {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
      content = nodeToAdd.getPage
    }
  }
}
