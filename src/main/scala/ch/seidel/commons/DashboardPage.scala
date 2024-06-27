package ch.seidel.commons

import ch.seidel.kutu.KuTuAppTree
import scalafx.scene.control.{ScrollPane, TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, Priority, VBox}

/** Dashboard Page */
class DashboardPage(dashPart: String = "dashboard", tree: KuTuAppTree) extends DisplayablePage {

  def getPage = {
    def thumbs(filter: String = "") = dashPart match {
      case "dashboard" => tree.getDashThumbsCtrl(filter)
      case _           => tree.getDashThumb(dashPart, filter)
    }
    new BorderPane {
      private val box: VBox = new VBox {
        styleClass += "category-page"
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = thumbs()
      }
      private val scrollPane: ScrollPane = new ScrollPane {
        vgrow = Priority.Always
        hgrow = Priority.Always
        fitToHeight = true
        fitToWidth = true
        vbarPolicy = ScrollPane.ScrollBarPolicy.Always
        content = box
      }
      val filter = new TextField() {
        promptText = "Such-Text"
        styleClass += "search-text"

        text.addListener { (o: javafx.beans.value.ObservableValue[_], oldVal: String, newVal: String) =>
          box.children = thumbs(newVal)
        }
      }
      var searchIcon: Image = null
      try {
        searchIcon = new Image(getClass().getResourceAsStream("/images/search-inv.png"))
      }catch{case e: Exception => e.printStackTrace()}
      val iv = new ImageView(searchIcon) {
        styleClass += "search-icon"
      }
      val searchbar = new BorderPane{
        styleClass += "category-page"
        left = iv
        center = filter
      }
      top = searchbar
      center = scrollPane
    }
  }
}
