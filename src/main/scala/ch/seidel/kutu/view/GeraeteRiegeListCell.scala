package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import javafx.scene.{control => jfxsc}
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}

class GeraeteRiegeListCell extends ListCell[GeraeteRiege] {
  var okIcon: Image = null
  try {
    okIcon = new Image(getClass().getResourceAsStream("/images/GreenOk.png"))
  }catch{case e: Exception => e.printStackTrace()}
  var nokIcon: Image = null
  try {
    nokIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
  }catch{
    case e: Exception => e.printStackTrace()
  }

  override val delegate: jfxsc.ListCell[GeraeteRiege] = new jfxsc.ListCell[GeraeteRiege] {
    override protected def updateItem(item: GeraeteRiege, empty: Boolean): Unit = {
      super.updateItem(item, empty)
      if item != null then {
        val imageView = new ImageView {
          image = okIcon
        }
        item.durchgang match {
          case Some(d) =>
            setText(s"${item.sequenceId} ${item.durchgang.get}: ${item.disziplin.map(d => d.name).getOrElse("")}  (${item.halt + 1}. Gerät)")
            if !item.erfasst then {
              styleClass.add("incomplete")
              imageView.image = nokIcon
            }
            else if styleClass.indexOf("incomplete") > -1 then {
              styleClass.remove(styleClass.indexOf("incomplete"))
            }
          case None =>
            setText(s"Alle")
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
        graphic = null
      }
    }
  }
}
