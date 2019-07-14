package ch.seidel.commons

import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.Priority

class LazyTabPane(refreshTabsFn: (LazyTabPane) => Seq[Tab], releaseTabs: () => Unit) extends TabPane {
  hgrow = Priority.Always
  vgrow = Priority.Always
  id = "source-tabs"

  def init() {
//    release()
    val selected = selectionModel.value.getSelectedItem

    def indexOfTab(title: String): Int = {
      for(idx <- (tabs.size()-1 to 0 by -1)) {
        if(title.equals(tabs.get(idx).textProperty().getValue)) {
          return idx
        }
      }
      -1
    }
    val lazytabs = refreshTabsFn(this)
    for(idx <- (0 until lazytabs.size)) {
      val existingIndex = indexOfTab(lazytabs(idx).text.value)
      if(existingIndex > -1) {
//        println("removing " + tabs.get(existingIndex).textProperty().getValue)
        tabs.remove(existingIndex)
//        println("inserting " + lazytabs(idx).text.value)
        tabs.add(idx, lazytabs(idx))
      }
      else {
//        println("inserting " + lazytabs(idx).text.value)
        tabs.add(idx, lazytabs(idx))
      }
    }
    for(idx <- (tabs.size()-1 to 0 by -1)) {
       if(!lazytabs.contains(tabs.get(idx))) {
         println("removing " + tabs.get(idx).textProperty().getValue)
         tabs.remove(idx)
       }
    }
    lazytabs.foreach(_.asInstanceOf[TabWithService].populated)
    if(selected != null && indexOfTab(selected.textProperty().getValue) > -1) {
      selectionModel.value.select(indexOfTab(selected.textProperty().getValue))
    }
  }

  def refreshTabs() {
    init()
  }
  def release() {
    releaseTabs()
  }
}