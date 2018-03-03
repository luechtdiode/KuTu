package ch.seidel.kutu

import scalafx.beans.property._
import javafx.application.Platform

object ConnectionStates {
  
  private val _connectedWithProperty = new StringProperty("")
  private val _connectedProperty = new BooleanProperty()
  
  _connectedProperty.setValue(false)
  _connectedWithProperty.setValue("")
  
  val connectedWithProperty = new ReadOnlyStringProperty(_connectedWithProperty)
  val connectedProperty = new ReadOnlyBooleanProperty(_connectedProperty)
  
  def connectedWith(wettkampfUUID: String) {
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
        _connectedWithProperty.setValue("")
        _connectedProperty.setValue(false)
      }
    )
  }
}