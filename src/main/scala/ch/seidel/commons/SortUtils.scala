package ch.seidel.commons

import ch.seidel.kutu.KuTuAppThumbNail
import scalafx.scene.control._

/**
 * utility to sort the items
 */
object SortUtils {

  def treeItemSort: (TreeItem[String], TreeItem[String]) => Boolean = (ti: TreeItem[String], t2: TreeItem[String]) =>
    compare(ti.value(), t2.value())

  def thumbNailsSort: (KuTuAppThumbNail, KuTuAppThumbNail) => Boolean = (t1: KuTuAppThumbNail, t2: KuTuAppThumbNail) =>
    compare(t1.button.text(), t2.button.text())

  def sortKeys: (String, String) => Boolean = (x: String, y: String) => compare(x, y)

  private def compare: (String, String) => Boolean = (x: String, y: String) =>
    x.compareToIgnoreCase(y) < 0
}
