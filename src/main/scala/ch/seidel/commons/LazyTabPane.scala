package ch.seidel.commons

import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.Priority

import scala.collection.JavaConverters

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
    val lazytabs: Seq[Tab] = refreshTabsFn(this)
    tabs.setAll(JavaConverters.asJavaCollection(lazytabs.map(t => {
      val fxtab: javafx.scene.control.Tab = t
      fxtab
    })))

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