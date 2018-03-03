package ch.seidel.kutu.akka

import ch.seidel.kutu.domain.WertungView
import akka.actor.ActorRef
import ch.seidel.kutu.view.DurchgangView

sealed trait KutuAppProtokoll

sealed trait KutuAppAction extends KutuAppProtokoll
case class Subscribe(client: ActorRef, deviceId: String, durchgang: Option[String])
case class StopDevice(deviceId: String) extends KutuAppAction
case class CreateClient(deviceID: String, wettkampfUUID: String) extends KutuAppAction
case class StartDurchgang(wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class UpdateWertung(wertung: WertungView) extends KutuAppAction

sealed trait KutuAppEvent extends KutuAppProtokoll
case class DurchgangStarted(wettkampfUUID: String, durchgang: String)
case class StationWertungenCompleted(wertungen: List[WertungView]) extends KutuAppProtokoll
case class DurchgangFinished(wettkampfUUID: String, durchgang: String)

case class MessageAck(msg: String) extends KutuAppProtokoll
