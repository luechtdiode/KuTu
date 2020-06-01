package ch.seidel.kutu.view

import java.util.UUID
import java.util.concurrent.{ScheduledFuture, TimeUnit}

import ch.seidel.commons._
import ch.seidel.kutu.Config._
import ch.seidel.kutu.KuTuApp.enc
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer._
import ch.seidel.kutu.{Config, KuTuApp, KuTuServer}
import javafx.scene.{control => jfxsc}
import org.slf4j.LoggerFactory
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.beans.property.{DoubleProperty, ReadOnlyStringWrapper, _}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.geometry._
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{Clipboard, KeyEvent}
import scalafx.scene.layout._
import scalafx.util.converter.{DefaultStringConverter, DoubleStringConverter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success}

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
    override protected def updateItem(item: GeraeteRiege, empty: Boolean) {
      super.updateItem(item, empty)
      if (item != null) {
        val imageView = new ImageView {
          image = okIcon
        }
        item.durchgang match {
          case Some(d) =>
            setText(s"${item.sequenceId} ${item.durchgang.get}: ${item.disziplin.map(d => d.name).getOrElse("")}  (${item.halt + 1}. Gerät)")
            if(!item.erfasst) {
              styleClass.add("incomplete")
              imageView.image = nokIcon
            }
            else if (styleClass.indexOf("incomplete") > -1) {
              styleClass.remove(styleClass.indexOf("incomplete"))
            }
          case None =>
            setText(s"Alle")
            if(!item.erfasst) {
              styleClass.add("incomplete")
              imageView.image = nokIcon
            }
            else if (styleClass.indexOf("incomplete") > -1) {
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
