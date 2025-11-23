package ch.seidel.kutu.actors

import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.{Disziplin, encodeURIComponent}

case class CompetitionState (
           //                  durchgangGeraetMap: Map[String, List[Disziplin]] = Map.empty,
                             startedDurchgaenge: Set[String] = Set.empty,
                             finishedDurchgangSteps: Set[DurchgangStationFinished] = Set.empty,
                             finishedDurchgaenge: Set[String] = Set.empty,
                             startStopEvents: List[KutuAppEvent] = List.empty,
                             lastWertungen: Map[String, WertungContainer] = Map.empty,
                             bestenResults: Map[String, WertungContainer] = Map.empty,
                             lastBestenResults: Map[String, WertungContainer] = Map.empty,
                             lastSequenceId: Long = Long.MinValue,
                             completedflags: List[KutuAppEvent] = List.empty
                           ) {

  def updated(event: KutuAppEvent, isDNoteUsed: Boolean): CompetitionState = event match {
    case eventDurchgangStarted: DurchgangStarted =>
      if startedDurchgaenge.contains(eventDurchgangStarted.durchgang) then {
        this
      } else {
        CompetitionState(
          startedDurchgaenge + eventDurchgangStarted.durchgang,
          finishedDurchgangSteps
            .filter(fds => encodeURIComponent(fds.durchgang) != encodeURIComponent(eventDurchgangStarted.durchgang)),
          finishedDurchgaenge - eventDurchgangStarted.durchgang,
          startStopEvents.filter {
            case ds: DurchgangResetted => !ds.durchgang.equals(eventDurchgangStarted.durchgang)
            case _ => true
          } :+ eventDurchgangStarted,
          lastWertungen, bestenResults, lastBestenResults, lastSequenceId,
          completedflags
        )
    }

    case eventDurchgangFinished: DurchgangFinished =>
      if finishedDurchgaenge.contains(eventDurchgangFinished.durchgang) then {
        this
      } else {
        CompetitionState(
          startedDurchgaenge - eventDurchgangFinished.durchgang,
          finishedDurchgangSteps,
          finishedDurchgaenge + eventDurchgangFinished.durchgang,
          startStopEvents.filter {
            case ds: DurchgangResetted => !ds.durchgang.equals(eventDurchgangFinished.durchgang)
            case _ => true
          } :+ eventDurchgangFinished,
          Map.empty, Map.empty, Map.empty, lastSequenceId,
          completedflags
        )
      }

    case eventDurchgangResetted: DurchgangResetted =>
      CompetitionState(
        startedDurchgaenge - eventDurchgangResetted.durchgang,
        finishedDurchgangSteps.filter(_.durchgang.equals(eventDurchgangResetted.durchgang)),
        finishedDurchgaenge - eventDurchgangResetted.durchgang,
        startStopEvents.filter {
          case ds: DurchgangStarted => !ds.durchgang.equals(eventDurchgangResetted.durchgang)
          case ds: DurchgangFinished => !ds.durchgang.equals(eventDurchgangResetted.durchgang)
          case ds: DurchgangResetted => !ds.durchgang.equals(eventDurchgangResetted.durchgang)
          case _ => true
        } :+ eventDurchgangResetted,
        Map.empty, Map.empty, Map.empty, lastSequenceId,
        completedflags
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
        lastWertungen, bestenResults, lastBestenResults, lastSequenceId,
        completedflags
      )

    case dms: DonationMailSent =>
      copy(completedflags = completedflags :+ dms)

    case _: DurchgangStepFinished =>
      if lastWertungen.nonEmpty then {
        CompetitionState(
          startedDurchgaenge,
          finishedDurchgangSteps,
          finishedDurchgaenge,
          startStopEvents,
          Map.empty, Map.empty, bestenResults, lastSequenceId,
          completedflags
        )
      } else {
        this
      }

    case _ => this
  }

  def putBestenResult(wertungContainer: WertungContainer) =
    if wertungContainer.wertung.endnote.sum >= Config.bestenlisteSchwellwert then {
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
      awuv.geraet, awuv.programm, awuv.durchgang, isDNoteUsed, isStroked = false)
  }

  def lastWertungenPerWKDisz(durchgang: String): Map[String, WertungContainer] = {
    val dgs = encodeURIComponent(durchgang)
    //println(s"lastWertungenPerWKDisz($dgs)")
    lastWertungen
      .filter(!_._1.startsWith("G"))
      .map(w => (w._1.substring(1), w._2))
      .filter { w =>
        val dg = encodeURIComponent(w._2.durchgang)
        dg.isEmpty || dgs.isEmpty || dg.equals(dgs)
      }
  }
  def lastWertungenPerDisz(durchgang: String): Map[String, WertungContainer] = {
    val dgs = encodeURIComponent(durchgang)
    //println(s"lastWertungenPerDisz($dgs)")
    lastWertungen
      .filter(!_._1.startsWith("D"))
      .map(w => (w._1.substring(1), w._2))
      .filter{w =>
        val dg = encodeURIComponent(w._2.durchgang)
        dg.isEmpty || dgs.isEmpty || dg.equals(dgs)
      }
  }
  private def newCompetitionStateWith(wertungContainer: WertungContainer) =
    CompetitionState(
      startedDurchgaenge,
      finishedDurchgangSteps,
      finishedDurchgaenge,
      startStopEvents,
      lastWertungen
        .updated(s"D${wertungContainer.wertung.wettkampfdisziplinId}", wertungContainer)
        .updated(s"G${wertungContainer.geraet}", wertungContainer),
      putBestenResult(wertungContainer), lastBestenResults, lastSequenceId + 1,
      completedflags
    )

}
