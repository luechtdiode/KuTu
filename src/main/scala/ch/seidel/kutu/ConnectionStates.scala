package ch.seidel.kutu

import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.beans.property._
import javafx.application.Platform
import scala.concurrent.Promise
import ch.seidel.kutu.http.WebSocketClient

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
        WebSocketClient.disconnect
        _connectedWithProperty.setValue("")
        _connectedProperty.setValue(false)
      }
    )
  }
}