package ch.seidel.kutu

import ch.seidel.kutu.Config.setLocalHostServer
import javafx.application.Platform
import scalafx.beans.property.*

object LocalServerStates {

  private val _localServerProperty = new BooleanProperty()

  _localServerProperty.setValue(false)

  val localServerProperty = new ReadOnlyBooleanProperty(_localServerProperty)

  localServerProperty.onChange(println())

  def startLocalServer(listNetworkAddresses: () => IterableOnce[String]): Unit = {
    setLocalHostServer(value = true, None)
    Platform.runLater(
      () => {
        listNetworkAddresses().iterator.toList match {
          case xs::_ =>
            val firstColon = xs.indexOf("://") + 3
            val secondColon = xs.indexOf(":", firstColon)
            setLocalHostServer(value = true, Some(xs.substring(firstColon, secondColon)))
          case _ =>
            setLocalHostServer(value = true, None)
        }
        println("local server is listening on " + Config.remoteBaseUrl)
        _localServerProperty.setValue(true)
      }
    )
  }

  def stopLocalServer(): Unit = {
    setLocalHostServer(value = false, None)
    Platform.runLater(
      () => {
        ConnectionStates.disconnect()
        _localServerProperty.setValue(false)
      }
    )
  }
}