package ch.seidel.commons

import scalafx.scene.control.TabPane
import scalafx.scene.layout.Priority
import scalafx.scene.control.Tab

class LazyTabPane(refreshTabs: (LazyTabPane) => Seq[Tab]) extends TabPane {
  hgrow = Priority.Always
  vgrow = Priority.Always
  id = "source-tabs"

  def init {
    val lazytabs = refreshTabs(this)
    for(idx <- (tabs.size()-1 to 0 by -1)) {
       if(!lazytabs.contains(tabs.get(idx))) {
//         println("removing " + tabs.get(idx).textProperty().getValue)
         tabs.remove(idx)
       }
    }
    for(idx <- (0 until lazytabs.size)) {
      if(!tabs.contains(lazytabs(idx))) {
//        println("inserting " + lazytabs(idx).text.value)
        tabs.add(idx, lazytabs(idx))
      }
    }
    lazytabs.foreach(_.asInstanceOf[TabWithService].populated)
  }

  def refreshTabs() {
    init
  }
}