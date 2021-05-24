package ch.seidel.kutu.akka

import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.encodeURIComponent

case class CompetitionState (
                             startedDurchgaenge: Set[String] = Set.empty,
                             finishedDurchgangSteps: Set[DurchgangStationFinished] = Set.empty,
                             finishedDurchgaenge: Set[String] = Set.empty,
                             startStopEvents: List[KutuAppEvent] = List.empty,
                             lastWertungen: Map[String, WertungContainer] = Map.empty,
                             bestenResults: Map[String, WertungContainer] = Map.empty,
                             lastBestenResults: Map[String, WertungContainer] = Map.empty,
                             lastSequenceId: Long = Long.MinValue
                           ) {

  def updated(event: KutuAppEvent, isDNoteUsed: Boolean): CompetitionState = event match {
    case eventDurchgangStarted: DurchgangStarted =>
      CompetitionState(
        startedDurchgaenge + eventDurchgangStarted.durchgang,
        finishedDurchgangSteps
          .filter(fds => encodeURIComponent(fds.durchgang) != encodeURIComponent(eventDurchgangStarted.durchgang)),
        finishedDurchgaenge - eventDurchgangStarted.durchgang,
        startStopEvents :+ eventDurchgangStarted,
        lastWertungen, bestenResults, lastBestenResults, lastSequenceId
      )

    case eventDurchgangFinished: DurchgangFinished =>
      CompetitionState(
        startedDurchgaenge - eventDurchgangFinished.durchgang,
        finishedDurchgangSteps,
        finishedDurchgaenge + eventDurchgangFinished.durchgang,
        startStopEvents :+ eventDurchgangFinished,
        Map.empty, bestenResults, lastBestenResults, lastSequenceId
      )

    case au: AthletWertungUpdatedSequenced =>
      newCompetitionStateWith(mapToWertungContainer(au.toAthletWertungUpdated(), isDNoteUsed))

    case au: AthletWertungUpdated =>
      newCompetitionStateWith(mapToWertungContainer(au, isDNoteUsed))

    case fds: DurchgangStationFinished =>
      CompetitionState(
        startedDurchgaenge,
        finishedDurchgangSteps + fds,
        finishedDurchgaenge,
        startStopEvents,
        lastWertungen, bestenResults, lastBestenResults, lastSequenceId
      )

    case _: DurchgangStepFinished =>
      if (lastWertungen.nonEmpty) {
        CompetitionState(
          startedDurchgaenge,
          finishedDurchgangSteps,
          finishedDurchgaenge,
          startStopEvents,
          Map.empty, Map.empty, bestenResults, lastSequenceId
        )
      } else {
        this
      }

    case _ => this
  }

  def putBestenResult(wertungContainer: WertungContainer) =
    if (wertungContainer.wertung.endnote.sum >= Config.bestenlisteSchwellwert) {
      val key = s"${wertungContainer.id}:${wertungContainer.wertung.wettkampfdisziplinId.toString}"
      bestenResults.updated(key, wertungContainer)
    } else {
      bestenResults
    }

  private def mapToWertungContainer(awuv: AthletWertungUpdated, isDNoteUsed: Boolean) = {
    val athlet = awuv.athlet
    WertungContainer(
      athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, athlet.verein.map(_.name).getOrElse(""),
      awuv.wertung,
      awuv.geraet, awuv.programm, isDNoteUsed)
  }

  private def newCompetitionStateWith(wertungContainer: WertungContainer) =
    CompetitionState(
      startedDurchgaenge,
      finishedDurchgangSteps,
      finishedDurchgaenge,
      startStopEvents,
      lastWertungen.updated(wertungContainer.wertung.wettkampfdisziplinId.toString(), wertungContainer),
      putBestenResult(wertungContainer), lastBestenResults, lastSequenceId + 1
    )

}
