package ch.seidel.kutu

import ch.seidel.kutu.http.WebSocketClient
import javafx.application.Platform
import scalafx.beans.property._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

object ConnectionStates {
  
  private val _connectedWithProperty = new StringProperty("")
  private val _connectedProperty = new BooleanProperty()

  _connectedProperty.setValue(false)
  _connectedWithProperty.setValue("")
  
  val connectedWithProperty = new ReadOnlyStringProperty(_connectedWithProperty)
  val connectedProperty = new ReadOnlyBooleanProperty(_connectedProperty)

  def connectedWith[T <: Option[_]](wettkampfUUID: String, wspromise: Promise[_]) {
    wspromise.future.onComplete { case _ => disconnected }
    Platform.runLater(
      () => {
        _connectedWithProperty.setValue(wettkampfUUID)
        _connectedProperty.setValue(wettkampfUUID.nonEmpty)
      }
    )
  }
  
  def disconnected() {
    Platform.runLater(
      () => {
        disconnect
      }
    )
  }

  def disconnect = {
    WebSocketClient.disconnect
    _connectedWithProperty.setValue("")
    _connectedProperty.setValue(false)
  }
}