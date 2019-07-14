package ch.seidel.kutu

import ch.seidel.kutu.Config.setLocalHostServer
import javafx.application.Platform
import scalafx.beans.property._

object LocalServerStates {

  private val _localServerProperty = new BooleanProperty()

  _localServerProperty.setValue(false)

  val localServerProperty = new ReadOnlyBooleanProperty(_localServerProperty)

  localServerProperty.onChange(println)

  def startLocalServer(listNetworkAdresses: () => TraversableOnce[String]): Unit = {
    setLocalHostServer(true, None)
    Platform.runLater(
      () => {
        listNetworkAdresses().toList match {
          case xs::_ =>
            val firstColon = xs.indexOf("://") + 3
            val secondColon = xs.indexOf(":", firstColon)
            setLocalHostServer(true, Some(xs.substring(firstColon, secondColon)))
          case _ =>
            setLocalHostServer(true, None)
        }
        println("local server is listening on " + Config.remoteBaseUrl)
        _localServerProperty.setValue(true)
      }
    )
  }

  def stopLocalServer: Unit = {
    setLocalHostServer(false, None)
    Platform.runLater(
      () => {
        ConnectionStates.disconnect
        _localServerProperty.setValue(false)
      }
    )
  }
}