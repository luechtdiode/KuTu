package ch.seidel.kutu

import scalafx.beans.property._
import javafx.application.Platform
import scala.concurrent.Promise

object ConnectionStates {
  
  private val _connectedWithProperty = new StringProperty("")
  private val _connectedProperty = new BooleanProperty()
  private var _wsPromise: Option[Promise[Option[_]]] = None
  
  _connectedProperty.setValue(false)
  _connectedWithProperty.setValue("")
  
  val connectedWithProperty = new ReadOnlyStringProperty(_connectedWithProperty)
  val connectedProperty = new ReadOnlyBooleanProperty(_connectedProperty)
  
  def connectedWith[T <: Option[_]](wettkampfUUID: String, wsPromise: Promise[T]) {
    Platform.runLater(
      () => {
        _wsPromise.foreach(p => try {p.success(None)} catch {case e: Exception => })
        _wsPromise = Some(wsPromise).asInstanceOf[Option[Promise[Option[_]]]]
        _connectedWithProperty.setValue(wettkampfUUID)
        _connectedProperty.setValue(wettkampfUUID.nonEmpty)
      }
    )
  }
  
  def disconnected() {
    Platform.runLater(
      () => {
        _wsPromise.foreach(p => try {p.success(None)} catch {case e: Exception => })
        _wsPromise = None
        _connectedWithProperty.setValue("")
        _connectedProperty.setValue(false)
      }
    )
  }
}