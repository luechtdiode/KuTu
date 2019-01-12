package ch.seidel.kutu

import ch.seidel.kutu.Config.setLocalHostServer
import javafx.application.Platform
import scalafx.beans.property._

object LocalServerStates {

  private val _localServerProperty = new BooleanProperty()

  _localServerProperty.setValue(false)

  val localServerProperty = new ReadOnlyBooleanProperty(_localServerProperty)

  localServerProperty.onChange(println)

  def startLocalServer: Unit = {
    setLocalHostServer(true)
    Platform.runLater(
      () => {
        _localServerProperty.setValue(true)
      }
    )
  }

  def stopLocalServer: Unit = {
    setLocalHostServer(false)
    Platform.runLater(
      () => {
        ConnectionStates.disconnect
        _localServerProperty.setValue(false)
      }
    )
  }
}