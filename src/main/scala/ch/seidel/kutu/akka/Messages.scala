package ch.seidel.kutu.akka

import akka.actor.ActorRef
import ch.seidel.kutu.domain._

case class Subscribe(clientSource: ActorRef, deviceId: String, durchgang: Option[String])
case class StopDevice(deviceId: String)
case class CreateClient(deviceID: String, wettkampfUUID: String)

case class WertungContainer(id: Long, vorname: String, name: String, geschlecht: String, verein: String, wertung: Wertung, geraet: Long)

sealed trait KutuAppProtokoll

sealed trait KutuAppAction extends KutuAppProtokoll {
  val wettkampfUUID: String
}
case class StartDurchgang(override val wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class UpdateAthletWertung(ahtlet: AthletView, wertung: Wertung, override val wettkampfUUID: String, durchgang: String, geraet: Long) extends KutuAppAction

sealed trait KutuAppEvent extends KutuAppProtokoll
case class DurchgangStarted(wettkampfUUID: String, durchgang: String) extends KutuAppEvent
case class StationWertungenCompleted(wertungen: List[UpdateAthletWertung]) extends KutuAppEvent
case class DurchgangFinished(wettkampfUUID: String, durchgang: String) extends KutuAppEvent
case class AthletWertungUpdated(ahtlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long) extends KutuAppEvent

case class MessageAck(msg: String) extends KutuAppEvent
