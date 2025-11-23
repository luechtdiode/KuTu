package ch.seidel.commons

import ch.seidel.kutu.KuTuAppTree
import ch.seidel.kutu.KuTuAppTree.showThumbnails
import ch.seidel.kutu.view.WettkampfTableView
import scalafx.scene.Node
import scalafx.scene.control.{ScrollPane, TableView, TextField, Toggle, ToggleButton, ToggleGroup}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}

/** Dashboard Page */
class DashboardPage(dashPart: String = "dashboard", tree: KuTuAppTree) extends DisplayablePage {

  def getPage: BorderPane = {
    def thumbs(filter: String = "") = dashPart match {
      case "dashboard" => tree.getDashThumbsCtrl(filter)
      case _           => tree.getDashThumb(dashPart, filter)
    }
    new BorderPane {
      private val box: VBox = new VBox {
        styleClass += "category-page"
        vgrow = Priority.Always
        hgrow = Priority.Always
      }
      def makeScrollPane(node: Node): ScrollPane = new ScrollPane {
        vgrow = Priority.Always
        hgrow = Priority.Always
        fitToHeight = true
        fitToWidth = true
        vbarPolicy = ScrollPane.ScrollBarPolicy.Always
        content = node
      }
      val filter = new TextField() {
        promptText = "Such-Text"
        styleClass += "search-text"

        text.addListener { (o: javafx.beans.value.ObservableValue[?], oldVal: String, newVal: String) =>
          refreshView()
        }
      }

      showThumbnails.onChange { (_, o, n) => refreshView() }
      val iconToggleButton: ToggleButton = new ToggleButton {
        text = "Icons"
        styleClass += "toggle-button3"
        selected <==> showThumbnails
      }
      val listToggleButton: ToggleButton = new ToggleButton {
        text = "Liste"
        styleClass += "toggle-button3"
        selected =!= showThumbnails
      }
      val toggleGrp = new ToggleGroup {
        toggles += iconToggleButton
        toggles += listToggleButton
      }
      def refreshView(): Unit = {
        val nodes = thumbs(filter.text.value)
        if showThumbnails.value then {
          iconToggleButton.selected = true
          listToggleButton.selected = false
          box.children = nodes
          val scrollPane = makeScrollPane(box)
          center = scrollPane
        } else {
          iconToggleButton.selected = false
          listToggleButton.selected = true
          val tv = nodes match {
            case _::tv::Nil => tv match {
              case tableView: TableView[?] =>
                Some(tableView)
              case _ => None
            }
            case _ => None
          }
          tv match {
            case Some(tv) =>
              tv.prefHeight = 2000d
              center = tv
            case _ =>
              box.children = nodes
              center = box
          }
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
        right = new HBox{
          children += iconToggleButton
          children += listToggleButton
        }
      }
      top = searchbar
      refreshView()
    }
  }
}
