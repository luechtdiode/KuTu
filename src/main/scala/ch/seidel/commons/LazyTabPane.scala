package ch.seidel.commons

import javafx.scene.control
import org.slf4j.{Logger, LoggerFactory}
import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.Priority

import scala.jdk.CollectionConverters.IterableHasAsJava

class LazyTabPane(refreshTabsFn: (LazyTabPane) => Seq[Tab], releaseTabs: () => Unit) extends TabPane {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  hgrow = Priority.Always
  vgrow = Priority.Always
  id = "source-tabs"

  def init(): Unit = {
//    release()
    val selected = selectionModel.value.getSelectedItem

    def indexOfTab(title: String): Int = {
      for idx <- (tabs.size()-1 to 0 by -1) do {
        if title.equals(tabs.get(idx).textProperty().getValue) then {
          return idx
        }
      }
      -1
    }
    val lazytabs: Seq[Tab] = refreshTabsFn(this)
    val lazyJFXTabs = lazytabs.map(t => {
      val fxtab: control.Tab = t
      fxtab
    })
    tabs.setAll(lazyJFXTabs.asJavaCollection)
    tabs.sort((tab1, tab2) => lazyJFXTabs.indexOf(tab2) - lazyJFXTabs.indexOf(tab1) > 0)
    lazytabs.foreach(_.asInstanceOf[TabWithService].populated)
    if selected != null && indexOfTab(selected.textProperty().getValue) > -1 then {
      selectionModel.value.select(indexOfTab(selected.textProperty().getValue))
    }
  }

  def refreshTabs(): Unit = {
    init()
  }
  def release(): Unit = {
    releaseTabs()
  }
}