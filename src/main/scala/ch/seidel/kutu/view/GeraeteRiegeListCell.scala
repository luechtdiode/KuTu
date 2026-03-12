package ch.seidel.kutu.view

import ch.seidel.kutu.domain.*
import javafx.scene.control as jfxsc
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control.*
import scalafx.scene.image.{Image, ImageView}

class GeraeteRiegeListCell extends ListCell[GeraeteRiege] {
  var okIcon: Image = null
  try {
    okIcon = new Image(getClass.getResourceAsStream("/images/GreenOk.png"))
  }catch{case e: Exception => e.printStackTrace()}
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass.getResourceAsStream("/images/RedException.png"))
  }catch{
    case e: Exception => e.printStackTrace()
  }

  item.onChange { (_, _, newItem) =>
    updateCellContent(newItem)
  }

  private def updateCellContent(item: GeraeteRiege): Unit = {
    if item != null then {
      val imageView = new ImageView {
        image = okIcon
      }
      item.durchgang match {
        case Some(d) =>
          text = s"${item.sequenceId} ${item.durchgang.get}: ${item.disziplin.map(d => d.name).getOrElse("")}  (${item.halt + 1}. Gerät)"
          if !item.erfasst then {
            styleClass.add("incomplete")
            imageView.image = nokIcon
          }
          else if styleClass.indexOf("incomplete") > -1 then {
            styleClass.remove(styleClass.indexOf("incomplete"))
          }
        case None =>
          text = "Alle"
          if !item.erfasst then {
            styleClass.add("incomplete")
            imageView.image = nokIcon
          }
          else if styleClass.indexOf("incomplete") > -1 then {
            styleClass.remove(styleClass.indexOf("incomplete"))
          }
      }
      graphic = imageView
    }
    else {
      text = null
      graphic = null
    }
  }
}
