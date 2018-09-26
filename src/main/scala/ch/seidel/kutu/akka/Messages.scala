package ch.seidel.kutu.akka

import akka.actor.ActorRef
import ch.seidel.kutu.domain._
import java.time.Instant

case class Subscribe(clientSource: ActorRef, deviceId: String, durchgang: Option[String])
case class StopDevice(deviceId: String)
case class CreateClient(deviceID: String, wettkampfUUID: String)

case class WertungContainer(id: Long, vorname: String, name: String, geschlecht: String, verein: String, wertung: Wertung, geraet: Long, programm: String, isDNoteUsed: Boolean)

sealed trait KutuAppProtokoll

sealed trait KutuAppAction extends KutuAppProtokoll {
  val wettkampfUUID: String
}
case class StartDurchgang(override val wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class UpdateAthletWertung(athlet: AthletView, wertung: Wertung, override val wettkampfUUID: String, durchgang: String, geraet: Long, step: Int, programm: String) extends KutuAppAction
case class FinishDurchgangStation(override val wettkampfUUID: String, durchgang: String, geraet: Long, step: Int) extends KutuAppAction
case class FinishDurchgang(override val wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class FinishDurchgangStep(override val wettkampfUUID: String) extends KutuAppAction

sealed trait KutuAppEvent extends KutuAppProtokoll
case class DurchgangStarted(wettkampfUUID: String, durchgang: String, time: Long = System.currentTimeMillis()) extends KutuAppEvent
case class StationWertungenCompleted(wertungen: List[UpdateAthletWertung]) extends KutuAppEvent
case class DurchgangFinished(wettkampfUUID: String, durchgang: String, time: Long = System.currentTimeMillis()) extends KutuAppEvent
case class AthletWertungUpdated(athlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String) extends KutuAppEvent
case class NewLastResults(results: Map[String, WertungContainer], lastTopResults: Map[String, WertungContainer]) extends KutuAppEvent

case class MessageAck(msg: String) extends KutuAppEvent
