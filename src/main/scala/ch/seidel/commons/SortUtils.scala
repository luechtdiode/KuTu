package ch.seidel.commons

import ch.seidel.kutu.KuTuAppThumbNail
import scalafx.scene.control._

/**
 * utility to sort the items
 */
object SortUtils {

  def treeItemSort = (ti: TreeItem[String], t2: TreeItem[String]) =>
    compare(ti.value(), t2.value())

  def thumbNailsSort = (t1: KuTuAppThumbNail, t2: KuTuAppThumbNail) =>
    compare(t1.button.text(), t2.button.text())

  def sortKeys = (x: String, y: String) => compare(x, y)

  private def compare = (x: String, y: String) =>
    x.compareToIgnoreCase(y) < 0
}
