package ch.seidel.commons

import scalafx.scene.control.TabPane
import scalafx.scene.layout.Priority
import scalafx.scene.control.Tab

class LazyTabPane(progSites: Seq[Tab]) extends TabPane {
  hgrow = Priority.ALWAYS
  vgrow = Priority.ALWAYS
  id = "source-tabs"
  tabs = progSites

  def init {
    progSites.foreach(_.asInstanceOf[TabWithService].populated)
  }
}