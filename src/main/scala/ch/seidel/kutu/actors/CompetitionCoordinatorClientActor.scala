package ch.seidel.kutu.actors

import ch.seidel.kutu.Config
import ch.seidel.kutu.actors.CompetitionCoordinatorClientActor.{PublishAction, competitionWebsocketConnectionsActive, competitionsActive}
import ch.seidel.kutu.calc.ScoreCalcTemplate
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.data.ResourceExchanger.listWettkampfDisziplineViews
import ch.seidel.kutu.domain.{given_Conversion_Date_LocalDate, *}
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.{EnrichedJson, JsonSupport, MetricsController}
import ch.seidel.kutu.renderer.{MailTemplates, RiegenBuilder}
import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.snapshots.{Labels, PrometheusNaming}
import org.apache.pekko.actor.SupervisorStrategy.{Restart, Stop}
import org.apache.pekko.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, Terminated}
import org.apache.pekko.event.{Logging, LoggingAdapter}
import org.apache.pekko.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import org.apache.pekko.pattern.ask
import org.apache.pekko.persistence.{PersistentActor, SnapshotOffer, SnapshotSelectionCriteria}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.stream.{CompletionStrategy, OverflowStrategy}
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory
import spray.json.*

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Failure
import scala.util.control.NonFatal

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends PersistentActor with JsonSupport with KutuService /*with ActorLogging*/ {
  def shortName: String = self.toString().split("/").last.split("#").head + "/" + clientId()

  lazy val l: LoggingAdapter = Logging(system, this)

  //  lazy val log = new BusLogging(system.eventStream, "RouterLogging", classOf[RouterLogging], system.asInstanceOf[ExtendedActorSystem].logFilter) with DiagnosticLoggingAdapter
  object log {
    def error(s: String): Unit = l.error(s"[$shortName] $s")

    def error(s: String, ex: Throwable): Unit = l.error(s"[$shortName] $s", ex)

    def warning(s: String): Unit = l.warning(s"[$shortName] $s")

    def info(s: String): Unit = l.info(s"[$shortName] $s")

    def debug(s: String): Unit = l.debug(s"[$shortName] $s")
  }

  import context.*

  private val wettkampf = readWettkampf(wettkampfUUID)
  private val websocketProcessor = ResourceExchanger.processWSMessage(wettkampf, handleWebsocketMessages)
  private val cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]] = scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]()
  private val wkDiszs = listWettkampfDisziplineViews(wettkampf).map(d => d.id -> d).toMap
  private val wkPgmId = wettkampf.programmId
  private val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
  private val snapShotInterval = 100
  private val donationDonationBegin: LocalDate = if (Config.donationDonationBegin.nonEmpty) LocalDate.parse(Config.donationDonationBegin) else LocalDate.of(2000, 1, 1)
  private val wettkampfdatum: LocalDate = wettkampf.datum
  private val donationActiv = donationDonationBegin.isBefore(wettkampfdatum) && Config.donationLink.nonEmpty && Config.donationPrice.nonEmpty

  private var wsSend: Map[Option[String], List[ActorRef]] = Map.empty
  private var deviceWebsocketRefs: Map[String, ActorRef] = Map.empty
  private var pendingKeepAliveAck: Option[Int] = None
  private var openDurchgangJournal: Map[Option[String], List[AthletWertungUpdatedSequenced]] = Map.empty
  private var state: CompetitionState = CompetitionState()
  private var lastMediaEvent: Option[MediaPlayerEvent] = None
  private var geraeteRigeListe: List[GeraeteRiege] = List.empty
  private var clientId: () => String = () => ""
  private var currentPlayer: Option[(Iterable[ActorRef], UseMyMediaPlayer)] = None
  private val wkUUID: UUID = UUID.fromString(wettkampfUUID)

  def rebuildWettkampfMap(): Unit = {
    openDurchgangJournal = Map.empty
    cache2.clear()
    geraeteRigeListe = RiegenBuilder.mapToGeraeteRiegen(
      getAllKandidatenWertungen(wkUUID)
        .toList)
  }

  /**
   * finds the durchgang for a specific wertung.
   * @param wertung must be from the local database with its local wertung.id
   * @return durchgang, where the wertung is assigned
   */
  def findDurchgangForWertung(wertung: Wertung): String = {
    geraeteRigeListe.flatMap(_.findDurchgangForWertung(wertung)).headOption.getOrElse("")
  }

  private def deviceIdOf(actor: ActorRef) = deviceWebsocketRefs.filter(_._2 == actor).keys

  private def actorWithSameDeviceIdOfSender(originSender: ActorRef = sender()): Iterable[ActorRef] =
    deviceWebsocketRefs.filter(p => originSender.path.name.endsWith(p._1)).values

  // send keepalive messages to prevent closing the websocket connection
  private case object KeepAlive

  private case object TryStop

  private val liveticker = context.system.scheduler.scheduleAtFixedRate(10.second, 10.second, self, KeepAlive)

  override def persistenceId = s"$wettkampfUUID/${Config.appFullVersion}"

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in WettkampfCoordinator actor " + wettkampf, e)
      Restart
  }

  override def preStart(): Unit = {
    log.debug(s"Starting for $persistenceId, $wettkampf")
    rebuildWettkampfMap()
  }

  override def postStop(): Unit = {
    liveticker.cancel()
    log.debug(s"stopped: $persistenceId, $wettkampf")
    wsSend.values.flatten.foreach(context.stop)
  }

  override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
    log.info(event.toString)
    super.onRecoveryFailure(cause, event)
  }

  val receiveRecover: Receive = {
    case evt: KutuAppEvent => handleEvent(evt, recoveryMode = true)
    case SnapshotOffer(_, snapshot: CompetitionState) => state = snapshot
    case _ =>
  }

  def handleEvent(evt: KutuAppEvent, recoveryMode: Boolean = false): Boolean = {
    val stateBefore = state
    state = state.updated(evt, isDNoteUsed)
    if (!recoveryMode && lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {
      val criteria = SnapshotSelectionCriteria.Latest
      deleteSnapshots(criteria)
      saveSnapshot(state)
      deleteMessages(criteria.maxSequenceNr)
    }
    !state.equals(stateBefore)
  }

  val receiveCommand: Receive = {

    case RefreshWettkampfMap(_) =>
      rebuildWettkampfMap()

    case GetGeraeteRiegeList(_) =>
      sender() ! GeraeteRiegeList(geraeteRigeListe, wettkampfUUID)

    case PublishAction(id: String, action: KutuAppAction) =>
      this.clientId = () => id
      try {
        receiveCommand(action)
      } finally {
        this.clientId = () => ""
      }

    case StartedDurchgaenge(_) => sender() ! ResponseMessage(state.startedDurchgaenge)

    case ResetStartDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val resetted = DurchgangResetted(wettkampfUUID, durchgang)
      if (handleEvent(resetted)) persist(resetted) { _ =>
        storeDurchgangResetted(resetted)
        notifyWebSocketClients(senderWebSocket, resetted, durchgang)
      }
      sender() ! resetted

    case StartDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val started = DurchgangStarted(wettkampfUUID, durchgang)
      if (handleEvent(started)) persist(started) { evt =>
        storeDurchgangStarted(started)
        notifyWebSocketClients(senderWebSocket, started, durchgang)
        val msg = NewLastResults(
          state.lastWertungenPerWKDisz(durchgang),
          state.lastWertungenPerDisz(durchgang),
          state.lastBestenResults)
        notifyWebSocketClients(senderWebSocket, msg, durchgang)
      }
      sender() ! started

    case FinishDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      if (handleEvent(eventDurchgangFinished)) persist(eventDurchgangFinished) { evt =>
        storeDurchgangFinished(eventDurchgangFinished)
        notifyWebSocketClients(senderWebSocket, eventDurchgangFinished, durchgang)
        notifyBestenResult(durchgang)
        openDurchgangJournal = openDurchgangJournal - Some(encodeURIComponent(durchgang))
      }
      sender() ! eventDurchgangFinished

    case UpdateAthletWertung(athlet, wertung, wettkampfUUID, durchgang, geraet, step, programm) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      if (state.finishedDurchgangSteps.exists(fds =>
        encodeURIComponent(fds.durchgang) == encodeURIComponent(durchgang)
          && fds.geraet == geraet && fds.step == step + 1)) {
        sender() ! MessageAck("Diese Station ist bereits abgeschlossen und kann keine neuen Resultate mehr entgegennehmen.")
      } else if (!state.startedDurchgaenge.exists(d => encodeURIComponent(d) == encodeURIComponent(durchgang))) {
        sender() ! MessageAck("Dieser Durchgang ist noch nicht für die Resultaterfassung freigegeben.")
      } else try {
        val disz = wkDiszs.get(wertung.wettkampfdisziplinId).map(_.disziplin.easyprint).getOrElse(s"Disz${wertung.wettkampfdisziplinId}")
        log.debug(s"received for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm Disz $disz: ${wertung}")
        val verifiedWertung = updateWertungSimple(wertung, cache2)
        val updated = AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet, programm)
        log.info(s"saved for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm Disz $disz: ${verifiedWertung.resultatWithVariables}")

        persist(updated) { _ => }
        handleEvent(updated)
        val handledEvent = updated.toAthletWertungUpdatedSequenced(state.lastSequenceId)
        log.debug("completed " + handledEvent)
        updategeraeteRigeListe(handledEvent)
        log.debug("updated riegenliste " + handledEvent)
        sender() ! handledEvent

        addToDurchgangJournal(handledEvent, durchgang)
        notifyWebSocketClients(senderWebSocket, handledEvent, durchgang)
        notifyBestenResult(durchgang)
        //        }
      } catch {
        case e: Exception =>
          log.error(s"failed to complete save new score for  ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm new Wertung: $wertung", e)
          sender() ! MessageAck(e.getMessage)
      }

    case awu: AthletWertungUpdated => websocketProcessor(Some(sender()), awu)
    case awu: AthletWertungUpdatedSequenced => websocketProcessor(Some(sender()), awu)
    case awu: AthletsAddedToWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: AthletMovedInWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: AthletRemovedFromWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: ScoresPublished => websocketProcessor(Some(sender()), awu)

    case playerEvent: UseMyMediaPlayer =>
      val ws: Option[ActorRef] = deviceWebsocketRefs.find(p => p._1.endsWith(playerEvent.context)).map(_._2)
      currentPlayer.foreach(p => {
        sendMediaEjectedEvent()
        notifyWebSocketClients(None, MediaPlayerDisconnected(p._2.context), "")
      })
      currentPlayer = Some((ws, playerEvent))
      notifyWebSocketClients(None, MediaPlayerIsReady(playerEvent.context), "")

    case playerEvent: ForgetMyMediaPlayer if currentPlayer.exists(p => p._2.context.equals(playerEvent.context)) =>
      sendMediaEjectedEvent()
      currentPlayer = None
      notifyWebSocketClients(None, MediaPlayerDisconnected(playerEvent.context), "")

    case m: MediaPlayerAction => websocketProcessor(Some(sender()), m.asInstanceOf[KutuAppEvent])
    case m: MediaPlayerEvent =>
      lastMediaEvent = Some(m)
      websocketProcessor(Some(sender()), m.asInstanceOf[KutuAppEvent])

    case fds: FinishDurchgangStation =>
      persist(DurchgangStationFinished(fds.wettkampfUUID, fds.durchgang, fds.geraet, fds.step)) { evt =>
        handleEvent(evt)
        sender() ! MessageAck("OK")
      }

    case fds: FinishDurchgangStep =>
      persist(DurchgangStepFinished(fds.wettkampfUUID)) { evt =>
        handleEvent(evt)
        sender() ! MessageAck("OK")
        notifyBestenResult("")
      }

    case GetResultsToReplicate(_, fromSequenceId) =>
      openDurchgangJournal.get(None).foreach { list =>
        sender() ! TextMessage(
          LastResults(list.filter(_.sequenceId >= fromSequenceId))
            .asInstanceOf[KutuAppEvent].toJson.compactPrint)
      }

    case Subscribe(ref, deviceId, durchgang, lastSequenceIdOption) =>
      val durchgangNormalized = durchgang.map(encodeURIComponent)
      val durchgangClients = wsSend.getOrElse(durchgangNormalized, List.empty) :+ ref
      context.watch(ref)
      wsSend = wsSend + (durchgangNormalized -> durchgangClients)
      deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
      competitionWebsocketConnectionsActive
        .labelValues(wettkampf.easyprint, durchgangNormalized.getOrElse("all"))
        .set(durchgangClients.size)

      ref ! TextMessage("Connection established." + s"$deviceId@".split("@")(1))

      Future {
        lastSequenceIdOption match {
          case Some(sid) =>
            openDurchgangJournal.get(durchgang) match {
              case Some(messages) =>
                val lastResults = LastResults(
                  messages.filter(_.sequenceId >= sid)
                )
                ref ! lastResults
              case None =>
            }
          case _ =>
        }
      }
      ref ! BulkEvent(wettkampfUUID, squashDurchgangEvents(durchgangNormalized).toList)
      //      squashDurchgangEvents(durchgangNormalized).foreach { d =>
      //        ref ! d
      //      }
      ref ! NewLastResults(
      state.lastWertungenPerWKDisz(durchgang.getOrElse("")),
      state.lastWertungenPerDisz(durchgang.getOrElse("")),
      state.lastBestenResults)
      lastMediaEvent.foreach(ref ! _)

    // system actions
    case KeepAlive =>
      wsSend.flatMap(_._2).foreach(ws => ws ! TextMessage("keepAlive"))
      checkDonation()

    case MessageAck(txt) => if (txt.equals("keepAlive")) handleKeepAliveAck() else println(txt)

    case Stop => handleStop()

    case StopDevice(deviceId) =>
      log.debug(s"stopped device $deviceId")
      deviceWebsocketRefs.get(deviceId).foreach { stoppedWebsocket =>
        cleanupWebsocketRefs(stoppedWebsocket)
      }
      context.system.scheduler.scheduleOnce(30.second, self, TryStop)

    case TryStop =>
      if (state.startedDurchgaenge.isEmpty &&
        wsSend.isEmpty &&
        (LocalDate.now().isBefore(wettkampfdatum) || LocalDate.now().minusDays(2).isAfter(wettkampfdatum))
      ) handleStop()

    case Terminated(stoppedWebsocket) =>
      context.unwatch(stoppedWebsocket)
      val deviceId = deviceWebsocketRefs.filter(x => x._2 == stoppedWebsocket).keys.headOption
      log.debug(s"terminated device $deviceId")
      cleanupWebsocketRefs(stoppedWebsocket)

      context.system.scheduler.scheduleOnce(30.second, self, TryStop)

    case Delete(wk) =>
      val criteria = SnapshotSelectionCriteria.Latest
      deleteSnapshots(criteria)
      deleteMessages(lastSequenceNr)
      log.info(s"Wettkampf will be deleted $wk")
      sender() ! MessageAck(s"OK, Wettkampf $wk deleted")
      wsSend.keys.foreach(dg => {
        competitionWebsocketConnectionsActive
          .labelValues(wettkampf.easyprint, dg.getOrElse("all"))
          .set(wsSend.get(dg).size)
      })
      wsSend.values.foreach(_.foreach(_.actorRef ! PoisonPill))
      deviceWebsocketRefs = Map.empty
      wsSend = Map.empty
      openDurchgangJournal = Map.empty
      pendingKeepAliveAck = None
      state = CompetitionState()
      handleStop()

    case _ =>
  }

  private def sendMediaEjectedEvent(): Unit = {
    lastMediaEvent.foreach {
      case AthletMediaIsFree(media, context) =>
        lastMediaEvent = Some(AthletMediaIsFree(media, context))
      case AthletMediaIsAtStart(media, context) =>
        lastMediaEvent = Some(AthletMediaIsFree(media, context))
      case AthletMediaIsRunning(media, context) =>
        lastMediaEvent = Some(AthletMediaIsFree(media, context))
      case AthletMediaIsPaused(media, context) =>
        lastMediaEvent = Some(AthletMediaIsFree(media, context))
      case _ =>
    }
    lastMediaEvent.foreach(mediaEvent => {
      websocketProcessor(Some(sender()), mediaEvent.asInstanceOf[KutuAppEvent])
    })
  }

  private def checkDonation(): Unit = {
    if (donationActiv && LocalDateTime.of(wettkampf.datum, LocalTime.of(7, 0, 0)).plusDays(2).isBefore(LocalDateTime.now()) && !state.completedflags.exists {
      case DonationMailSent(_, _, _, wkid) if wkid == this.wettkampfUUID => true
      case _ => false
    }) {
      val meta = getWettkampfMetaData(wkUUID)
      meta.finishDonationAsked match {
        case None =>
          val abschluss = saveWettkampfAbschluss(wkUUID)
          log.info(
            s"""
               |Abschlussverarbeitung mit Wettkampf ${wettkampf.easyprint}:
               |Statistik: $abschluss
               |""".stripMargin)
          val teilnehmer = getAllKandidatenWertungen(wkUUID)
          val results = teilnehmer.flatMap(k => k.wertungen.map(w => w.resultat.endnote)).sum
          if (abschluss.finishOnlineAthletesCnt > 10 || (results > 0 && abschluss.finishAthletesCnt > 10)) {
            val teilnehmerCnt = if (abschluss.finishAthletesCnt > 0) abschluss.finishAthletesCnt else abschluss.finishOnlineAthletesCnt
            val clubsCnt = if (abschluss.finishClubsCnt > 0) abschluss.finishClubsCnt else abschluss.finishOnlineClubsCnt
            val pricePerTn = BigDecimal(Config.donationPrice)
            val betrag = (pricePerTn * teilnehmerCnt).setScale(2)
            val donationLink = Config.donationLink
            val mail = MailTemplates.createDonateMail(wettkampf, donationLink, abschluss, pricePerTn, betrag, clubsCnt, teilnehmerCnt)
            KuTuMailerActor.send(mail)
            mail match {
              case mpm:MultipartMail =>
                log.info(s"Mail submitted to ${mpm.to}:\n${mpm.messageText}")
              case sm:SimpleMail =>
                log.info(s"Mail submitted to ${sm.to}:\n${sm.messageText}")
            }
            saveWettkampfDonationAsk(wkUUID, wettkampf.notificationEMail, betrag)
            handleEvent(DonationMailSent(teilnehmerCnt, pricePerTn, donationLink, wettkampfUUID))
            saveSnapshot(state)
          } else {
            log.info("Kein Mail versendet - zu wenig Teilnehmer mit Wertung - WK wurde ev. nicht durchgeführt.")
            saveWettkampfDonationAsk(wkUUID, "", BigDecimal(0))
            handleEvent(DonationMailSent(0, BigDecimal(0), "", wettkampfUUID))
            saveSnapshot(state)
          }
        case _ =>
      }
    }
  }

  private def cleanupWebsocketRefs(stoppedWebsocket: ActorRef): Unit = {
    deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)
    val durchgaenge = wsSend
      .filter { x => x._2.exists(_.equals(stoppedWebsocket)) }
      .keys

    wsSend = wsSend.map { x =>
      (x._1, x._2
        .filter(_ != stoppedWebsocket)
        .filter(socket => deviceWebsocketRefs.exists(_._2 == socket)))
    }.filter(x => x._2.nonEmpty)

    durchgaenge.foreach(dg => {
      competitionWebsocketConnectionsActive
        .labelValues(wettkampf.easyprint, dg.getOrElse("all"))
        .set(wsSend.get(dg).size)
    })
    currentPlayer = currentPlayer.flatMap {
      case (playerActorRefs, useMyMediaPlayerAction) =>
        if (wsSend.isEmpty || deviceWebsocketRefs.exists(p => p._1.equals(useMyMediaPlayerAction.context) || playerActorRefs.exists(a => a.equals(stoppedWebsocket)))) {
          notifyWebSocketClients(actorWithSameDeviceIdOfSender(), MediaPlayerDisconnected(useMyMediaPlayerAction.context), "")
          None
        } else {
          Some((playerActorRefs, useMyMediaPlayerAction))
        }
    }
  }

  private def squashDurchgangEvents(durchgangNormalized: Option[String]) = {
    (durchgangNormalized match {
      case Some(dgn) => // take last of requested durchgang
        state.startStopEvents.reverse.filter {
          case DurchgangStarted(_, d, _) => encodeURIComponent(d).equals(dgn)
          case DurchgangFinished(_, d, _) => encodeURIComponent(d).equals(dgn)
          case DurchgangResetted(_, d) => encodeURIComponent(d).equals(dgn)
          case _ => false
        }.take(1)
      case _ => //take first and last per durchgang
        state.startStopEvents.groupBy {
            case DurchgangStarted(w, d, t) => d
            case DurchgangFinished(w, d, t) => d
            case DurchgangResetted(w, d) => d
            case _ => "_"
          }
          .filter(_._1 != "_")
          .flatMap { d =>
            if (d._2.size > 1) {
              Seq(d._2.head, d._2.last)
            } else {
              d._2
            }
          }
    }).toList.sortBy {
      case DurchgangStarted(_, _, t) => t
      case DurchgangFinished(_, _, t) => t
      case DurchgangResetted(_, _) => System.currentTimeMillis()
      case _ => 0L
    }
  }

  def handleWebsocketMessages(originSender: Option[ActorRef], event: KutuAppEvent): Unit = {
    val senderWebSocket = actorWithSameDeviceIdOfSender(originSender.getOrElse(sender()))

    def forwardToListeners(handledEvent: AthletWertungUpdatedSequenced): Unit = {
      updategeraeteRigeListe(handledEvent)
      addToDurchgangJournal(handledEvent, handledEvent.durchgang)
      notifyWebSocketClients(senderWebSocket, handledEvent, handledEvent.durchgang)
      notifyBestenResult(handledEvent.durchgang)
    }

    event match {
      case awuv: AthletWertungUpdated =>
        val awuvWithDG = awuv.copy(durchgang = if (awuv.durchgang.isEmpty) findDurchgangForWertung(awuv.wertung) else awuv.durchgang)
        persist(awuvWithDG) { _ => }
        handleEvent(awuvWithDG)
        val handledEvent = awuvWithDG.toAthletWertungUpdatedSequenced(state.lastSequenceId)
        forwardToListeners(handledEvent)

      case awuv: AthletWertungUpdatedSequenced =>
        val awuvWithDG = awuv.copy(durchgang = if (awuv.durchgang.isEmpty) findDurchgangForWertung(awuv.wertung) else awuv.durchgang)
        persist(awuvWithDG) { _ => }
        handleEvent(awuvWithDG)
        val handledEvent = awuvWithDG.toAthletWertungUpdated().toAthletWertungUpdatedSequenced(state.lastSequenceId)
        forwardToListeners(handledEvent)

      case scoresPublished: ScoresPublished =>
        notifyWebSocketClients(senderWebSocket, scoresPublished, "")

      case add: AthletsAddedToWettkampf =>
        AthletIndexActor.publish(ResyncIndex)
        CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wettkampf.uuid.get), "WK-Admin")
        CompetitionRegistrationClientActor.publish(RegistrationResync(wettkampf.uuid.get), "WK-Admin")
        notifyWebSocketClients(senderWebSocket, add, "")

      case awu: AthletMovedInWettkampf =>
        CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wettkampf.uuid.get), "WK-Admin")
        CompetitionRegistrationClientActor.publish(RegistrationResync(wettkampf.uuid.get), "WK-Admin")
        notifyWebSocketClients(senderWebSocket, awu, "")

      case arw: AthletRemovedFromWettkampf =>
        CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wettkampf.uuid.get), "WK-Admin")
        CompetitionRegistrationClientActor.publish(RegistrationResync(wettkampf.uuid.get), "WK-Admin")
        notifyWebSocketClients(senderWebSocket, arw, "")

      case mediaPlayerAction: MediaPlayerAction =>
        notifyPlayerWebSocketClient(mediaPlayerAction)

      case mediaPlayerEvent: MediaPlayerEvent =>
        notifyWebSocketClients(senderWebSocket, mediaPlayerEvent, "")

      case _ =>
    }
    val s = sender()
    s ! MessageAck("OK")
    originSender.foreach(os => if (!os.equals(s)) {
      os ! MessageAck("OK")
    })
  }

  private def notifyPlayerWebSocketClient(toPublish: KutuAppEvent) = currentPlayer.foreach {
      case (playerActorRefs, useMyMediaPlayerAction) =>
        playerActorRefs.foreach(player => player ! toPublish)
      case null =>
    }

  private def notifyWebSocketClients(
                                      senderWebSocket: Iterable[ActorRef],
                                      toPublish: KutuAppEvent,
                                      durchgang: String, exclusive:Boolean = false) = Future {
    //println(s"notifyWebsocketClients dg: $durchgang, excl: $exclusive, $toPublish")
    if (durchgang == "") {
      if (exclusive) {
        wsSend.get(None) match {
          case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
          case _ =>
        }
      } else {
        wsSend.values.foreach(wsList => {
          wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
        })
      }
    } else {
      wsSend.get(Some(encodeURIComponent(durchgang))) match {
        case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
        case _ =>
      }
      if (!exclusive) {
        wsSend.get(None) match {
          case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
          case _ =>
        }
      }
    }
  }

  private def updategeraeteRigeListe(toPublish: AthletWertungUpdatedSequenced): Unit = {
    val wertung = toPublish.wertung
    geraeteRigeListe = geraeteRigeListe.map(_.updated(wertung))
  }

  private def addToDurchgangJournal(toPublish: AthletWertungUpdatedSequenced, durchgang: String): Unit = {
    if (durchgang == "") {
      openDurchgangJournal = openDurchgangJournal.flatMap {
        case (dgoption: Option[String], list: List[AthletWertungUpdatedSequenced]) =>
          Some(dgoption -> (list :+ toPublish))
        case null => None
      }
    } else {
      val key = Some(encodeURIComponent(durchgang))
      openDurchgangJournal.get(key) match {
        case Some(list) => openDurchgangJournal = openDurchgangJournal + (key -> (list :+ toPublish))
        case None => openDurchgangJournal = openDurchgangJournal + (key -> List(toPublish))
      }
      openDurchgangJournal.get(None) match {
        case Some(list) => openDurchgangJournal = openDurchgangJournal + (None -> (list :+ toPublish))
        case None => openDurchgangJournal = openDurchgangJournal + (None -> List(toPublish))
      }
    }
  }

  def notifyBestenResult(durchgang: String): Unit = {
    val msg = NewLastResults(
      state.lastWertungenPerWKDisz(durchgang),
      state.lastWertungenPerDisz(durchgang),
      state.lastBestenResults)
    notifyWebSocketClients(Iterable.empty, msg, durchgang, exclusive = true)
    if (durchgang.nonEmpty) {
      val msgAll = NewLastResults(
        state.lastWertungenPerWKDisz(""),
        state.lastWertungenPerDisz(""),
        state.lastBestenResults)
      notifyWebSocketClients(Iterable.empty, msgAll, "", exclusive = true)
    }
  }

  private def handleStop(): Unit = {
    stop(self)
  }

  //
  //  private def handleKeepAlive {
  //    wsSend.values.flatten.foreach(_ ! TextMessage("keepAlive"))
  //    if (wsSend.nonEmpty) pendingKeepAliveAck = pendingKeepAliveAck.map(_ + 1) match {
  //      case Some(i) if (i < 10) =>
  //        Some(i)
  //      case Some(i) if (i >= 10) =>
  //        handleStop
  //        None
  //      case _ =>
  //        Some(1)
  //    }
  //  }

  private def handleKeepAliveAck(): Unit = {
    pendingKeepAliveAck = pendingKeepAliveAck.map(_ - 1) match {
      case Some(i) if i > 0 => Some(i)
      case _ => None
    }
  }

}

case object StopAll

class ClientActorSupervisor extends Actor with ActorLogging {

  var wettkampfCoordinators: Map[String, ActorRef] = Map[String, ActorRef]()

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in WettkampfCoordinator actor.", e)
      Stop
  }

  override def preStart(): Unit = {
    log.info("Start ClientActorSupervisor")
    super.preStart()
  }

  override def postStop(): Unit = {
    log.info("Stop ClientActorSupervisor")
    super.postStop()
  }

  override def receive: Receive = {
    case CreateClient(deviceID, wettkampfUUID) =>
      val coordinator = wettkampfCoordinators.get(wettkampfUUID) match {
        case Some(coordinator) =>
          log.debug(s"Connect new client to existing coordinator. Wettkampf: $wettkampfUUID, Device: $deviceID")
          coordinator
        case _ =>
          log.info(s"Connect new client to new coordinator. Wettkampf: $wettkampfUUID, Device: $deviceID")
          val coordinator = context.actorOf(
            CompetitionCoordinatorClientActor.props(wettkampfUUID), "client-" + wettkampfUUID)
          context.watch(coordinator)
          wettkampfCoordinators = wettkampfCoordinators + (wettkampfUUID -> coordinator)
          coordinator
      }
      sender() ! coordinator

    case StopAll =>
      wettkampfCoordinators.foreach(coordinator => coordinator._2 ! Stop)
      sender() ! "OK"

    case uw: PublishAction =>
      wettkampfCoordinators.get(uw.action.wettkampfUUID) match {
        case Some(coordinator) => coordinator.forward(uw)
        case _ =>
          log.info(s"Connect new client to new coordinator. Wettkampf: ${uw.action.wettkampfUUID}, Context: ${uw.id} via Rest-API (Sessionless)")
          val coordinator = context.actorOf(
            CompetitionCoordinatorClientActor.props(uw.action.wettkampfUUID), "client-" + uw.action.wettkampfUUID)
          context.watch(coordinator)
          wettkampfCoordinators = wettkampfCoordinators + (uw.action.wettkampfUUID -> coordinator)
          coordinator.forward(uw)
          competitionsActive.set(wettkampfCoordinators.size)
        //log.warning("Action for unknown competition: " + uw)
        //sender() ! MessageAck("Action for unknown competition: " + uw)
      }

    case Terminated(wettkampfActor) =>
      context.unwatch(wettkampfActor)
      wettkampfCoordinators = wettkampfCoordinators.filter(_._2 != wettkampfActor)
      competitionsActive.set(wettkampfCoordinators.size)

    case MessageAck(text) => println(text)

    case s => println(s)
  }
}

object CompetitionCoordinatorClientActor extends JsonSupport with EnrichedJson {
  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy val competitionsActive: Gauge = Gauge.builder()
    .name(PrometheusNaming.sanitizeMetricName(Config.metricsNamespaceName + "_competitions_active"))
    .help("Active competitions")
    .register(MetricsController.registry.underlying)

  lazy val competitionWebsocketConnectionsActive: Gauge = Gauge
    .builder()
    .name(PrometheusNaming.sanitizeMetricName(Config.metricsNamespaceName + "_competition_websockets_active"))
    .labelNames("comp", "dg")
    .help("Active websocket connections for per competition and durchgang")
    .register(MetricsController.registry.underlying)

  case class PublishAction(id: String, action: KutuAppAction)

  import ch.seidel.kutu.http.Core.*

  val supervisor: ActorRef = system.actorOf(Props[ClientActorSupervisor](), name = "Supervisor")

  def publish(action: KutuAppAction, context: String): Future[KutuAppEvent] = {
    implicit val timeout: Timeout = Timeout(15000, TimeUnit.MILLISECONDS)
    (supervisor ? PublishAction(context, action)).mapTo[KutuAppEvent]
  }

  def stopAll(): Future[String] = {
    implicit val timeout: Timeout = Timeout(15000, TimeUnit.MILLISECONDS)
    (supervisor ? StopAll).mapTo[String]
  }

  def props(wettkampfUUID: String): Props = Props(classOf[CompetitionCoordinatorClientActor], wettkampfUUID)

  def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          logger.error(s"WS-Server stream failed with $cause")
        case _ => // ignore regular completion
          logger.debug(s"WS-Server stream closed")
      })


  def tryMapText(text: String): KutuAppEvent = try {
    if ("keepAlive".equalsIgnoreCase(text)) {
      MessageAck(text)
    } else {
      text.asType[KutuAppEvent]
    }
  } catch {
    case e: Exception =>
      //logger.debug("unparsable json mapped to MessageAck: " + text, e)
      println(("unparsable json mapped to MessageAck: " + text, e))
      e.printStackTrace()
      MessageAck(text)
  }

  def fromWebsocketToActorFlow: Flow[Message, KutuAppEvent, Any] =
    Flow[Message]
      .mapAsync(1) {
        case TextMessage.Strict(text) => Future.successful(tryMapText(text))
        case TextMessage.Streamed(stream) => stream.runFold("")(_ + _).map(tryMapText)
        case _: BinaryMessage => throw new Exception("Binary message cannot be handled")
      }.via(reportErrorsFlow)

  def fromCoordinatorActorToWebsocketFlow(lastSequenceId: Option[Long], source: Source[Any, ActorRef]): Source[TextMessage, ActorRef] =
    source.filter({
      case LastResults(_) if lastSequenceId.isEmpty =>
        false
      case _ =>
        true
    }).map({
      case awus: AthletWertungUpdatedSequenced if lastSequenceId.isEmpty =>
        TextMessage(awus.toAthletWertungUpdated().asInstanceOf[KutuAppEvent].toJson.compactPrint)
      case msg: KutuAppEvent =>
        TextMessage(msg.toJson.compactPrint)
      case msg: TextMessage => msg
    })

  val completionMatcher: PartialFunction[scala.Any, CompletionStrategy] = {
    case StopDevice(_) => CompletionStrategy.immediately
  }
  val failureMatcher: PartialFunction[scala.Any, scala.Throwable] = {
    case Err(x) => x
  }

  // authenticated bidirectional streaming
  def createActorSinkSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String], lastSequenceId: Option[Long]): Flow[Message, Message, Any] = {
    implicit val timeout: Timeout = Timeout(5000, TimeUnit.MILLISECONDS)
    val clientActor = Await.result(
      ask(supervisor, CreateClient(deviceId, wettkampfUUID)).mapTo[ActorRef]
      , timeout.duration
    )

    val sink = fromWebsocketToActorFlow.to(Sink.actorRef(clientActor, StopDevice(deviceId), _ => StopDevice(deviceId)).named(deviceId))
    val source = fromCoordinatorActorToWebsocketFlow(lastSequenceId,
      Source.actorRef(completionMatcher, failureMatcher, 256, OverflowStrategy.dropHead)
        .mapMaterializedValue { (wsSource: ActorRef) =>
          clientActor ! Subscribe(wsSource, deviceId, durchgang, lastSequenceId)
          wsSource
        }.named(deviceId))

    Flow.fromSinkAndSourceCoupled(sink, source).log(name = deviceId)
  }

  // unauthenticted oneway/readonly streaming
  def createActorSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String], lastSequenceId: Option[Long] = None): Flow[Message, Message, Any] = {
    implicit val timeout: Timeout = Timeout(5000, TimeUnit.MILLISECONDS)
    val clientActor = Await.result(
      ask(supervisor, CreateClient(deviceId, wettkampfUUID)).mapTo[ActorRef]
      , timeout.duration
    )

    val sink = fromWebsocketToActorFlow.filter {
      case MessageAck(msg) if msg.equalsIgnoreCase("keepAlive") => true
      case _ => false
    }.to(Sink.actorRef(clientActor, StopDevice(deviceId), _ => StopDevice(deviceId)).named(deviceId))

    val source = fromCoordinatorActorToWebsocketFlow(lastSequenceId,
      Source.actorRef(completionMatcher, failureMatcher,
        256,
        OverflowStrategy.dropHead).mapMaterializedValue { wsSource =>
        clientActor ! Subscribe(wsSource, deviceId, durchgang, lastSequenceId)
        wsSource
      }.named(deviceId))

    Flow.fromSinkAndSourceCoupled(sink, source).log(name = deviceId)

  }
}
