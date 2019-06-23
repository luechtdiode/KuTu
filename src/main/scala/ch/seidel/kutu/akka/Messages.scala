package ch.seidel.kutu.akka

import akka.actor.ActorRef
import ch.seidel.kutu.data.GroupBy
import ch.seidel.kutu.domain._

case class Subscribe(clientSource: ActorRef, deviceId: String, durchgang: Option[String], lastSequenceId: Option[Long])
case class StopDevice(deviceId: String)
case class CreateClient(deviceID: String, wettkampfUUID: String)

case class WertungContainer(id: Long, vorname: String, name: String, geschlecht: String, verein: String, wertung: Wertung, geraet: Long, programm: String, isDNoteUsed: Boolean)

sealed trait KutuAppProtokoll

sealed trait KutuAppAction extends KutuAppProtokoll {
  val wettkampfUUID: String
}
case class GetResultsToReplicate(override val wettkampfUUID: String, fromSequenceId: Long) extends KutuAppAction
case class StartedDurchgaenge(override val wettkampfUUID: String) extends KutuAppAction
case class StartDurchgang(override val wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class UpdateAthletWertung(athlet: AthletView, wertung: Wertung, override val wettkampfUUID: String, durchgang: String, geraet: Long, step: Int, programm: String) extends KutuAppAction
case class FinishDurchgangStation(override val wettkampfUUID: String, durchgang: String, geraet: Long, step: Int) extends KutuAppAction
case class FinishDurchgang(override val wettkampfUUID: String, durchgang: String) extends KutuAppAction
case class FinishDurchgangStep(override val wettkampfUUID: String) extends KutuAppAction
case class Delete(override val wettkampfUUID: String) extends KutuAppAction
case class PublishScores(override val wettkampfUUID: String, title: String, query: String, published: Boolean) extends KutuAppAction

sealed trait KutuAppEvent extends KutuAppProtokoll
case class DurchgangStarted(wettkampfUUID: String, durchgang: String, time: Long = System.currentTimeMillis()) extends KutuAppEvent
case class StationWertungenCompleted(wertungen: List[UpdateAthletWertung]) extends KutuAppEvent
case class DurchgangStationFinished(wettkampfUUID: String, durchgang: String, geraet: Long, step: Int) extends KutuAppEvent
case class DurchgangStepFinished(wettkampfUUID: String, time: Long = System.currentTimeMillis()) extends KutuAppEvent
case class DurchgangFinished(wettkampfUUID: String, durchgang: String, time: Long = System.currentTimeMillis()) extends KutuAppEvent
case class AthletWertungUpdated(athlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String) extends KutuAppEvent {
  def toAthletWertungUpdatedSequenced(sequenceId: Long) = AthletWertungUpdatedSequenced(athlet, wertung, wettkampfUUID, durchgang, geraet, programm, sequenceId)
}
case class AthletWertungUpdatedSequenced(athlet: AthletView, wertung: Wertung, wettkampfUUID: String, durchgang: String, geraet: Long, programm: String, val sequenceId: Long) extends KutuAppEvent {
  def toAthletWertungUpdated() = AthletWertungUpdated(athlet, wertung, wettkampfUUID, durchgang, geraet, programm)
}
case class AthletRemovedFromWettkampf(athlet: AthletView, wettkampfUUID: String) extends KutuAppEvent
case class AthletMovedInWettkampf(athlet: AthletView, wettkampfUUID: String, pgmId: Long) extends KutuAppEvent
case class DurchgangChanged(durchgang: String, wettkampfUUID: String, athlet: AthletView) extends KutuAppEvent
case class ScoresPublished(scoreId: String, title: String, query: String, published: Boolean, wettkampfUUID: String) extends KutuAppEvent

case class NewLastResults(results: Map[String, WertungContainer], lastTopResults: Map[String, WertungContainer]) extends KutuAppEvent
case class LastResults(results: List[AthletWertungUpdatedSequenced]) extends KutuAppEvent
case class MessageAck(msg: String) extends KutuAppEvent
case class ResponseMessage(data: Object) extends KutuAppEvent
