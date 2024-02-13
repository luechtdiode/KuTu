package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, Terminated}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer, SnapshotSelectionCriteria}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.akka.CompetitionCoordinatorClientActor.{PublishAction, competitionWebsocketConnectionsActive, competitionsActive}
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.{EnrichedJson, JsonSupport}
import ch.seidel.kutu.renderer.{MailTemplates, RiegenBuilder}
import io.prometheus.client
import io.prometheus.client.{Collector, CollectorRegistry}
import org.slf4j.LoggerFactory
import spray.json._

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Failure
import scala.util.control.NonFatal

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends PersistentActor with JsonSupport with KutuService /*with ActorLogging*/ {
  def shortName: String = self.toString().split("/").last.split("#").head + "/" + clientId()

  lazy val l: LoggingAdapter = akka.event.Logging(system, this)

  //  lazy val log = new BusLogging(system.eventStream, "RouterLogging", classOf[RouterLogging], system.asInstanceOf[ExtendedActorSystem].logFilter) with DiagnosticLoggingAdapter
  object log {
    def error(s: String): Unit = l.error(s"[$shortName] $s")

    def error(s: String, ex: Throwable): Unit = l.error(s"[$shortName] $s", ex)

    def warning(s: String): Unit = l.warning(s"[$shortName] $s")

    def info(s: String): Unit = l.info(s"[$shortName] $s")

    def debug(s: String): Unit = l.debug(s"[$shortName] $s")
  }

  import context._

  private val wettkampf = readWettkampf(wettkampfUUID)
  private val websocketProcessor = ResourceExchanger.processWSMessage(wettkampf, handleWebsocketMessages)
  private val wkPgmId = wettkampf.programmId
  private val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
  private val snapShotInterval = 100
  private val donationDonationBegin = if (Config.donationDonationBegin.nonEmpty) LocalDate.parse(Config.donationDonationBegin) else LocalDate.of(2000,1,1)
  private val donationActiv = donationDonationBegin.before(wettkampf.datum) && Config.donationLink.nonEmpty && Config.donationPrice.nonEmpty

  private var wsSend: Map[Option[String], List[ActorRef]] = Map.empty
  private var deviceWebsocketRefs: Map[String, ActorRef] = Map.empty
  private var pendingKeepAliveAck: Option[Int] = None
  private var openDurchgangJournal: Map[Option[String], List[AthletWertungUpdatedSequenced]] = Map.empty
  private var state: CompetitionState = CompetitionState()
  private var geraeteRigeListe: List[GeraeteRiege] = List.empty
  private var clientId: () => String = () => ""

  def rebuildWettkampfMap(): Unit = {
    openDurchgangJournal = Map.empty
    geraeteRigeListe = RiegenBuilder.mapToGeraeteRiegen(
      getAllKandidatenWertungen(UUID.fromString(wettkampfUUID))
        .toList)
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
      saveSnapshot(state)
      deleteSnapshots(criteria)
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

    case StartDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val started = DurchgangStarted(wettkampfUUID, durchgang)
      if (handleEvent(started)) persist(started) { evt =>
        storeDurchgangStarted(started)
        notifyWebSocketClients(senderWebSocket, started, durchgang)
        val msg = NewLastResults(state.lastWertungen, state.lastBestenResults)
        notifyWebSocketClients(senderWebSocket, msg, durchgang)
      }
      sender() ! started

    case FinishDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      if (handleEvent(eventDurchgangFinished)) persist(eventDurchgangFinished) { evt =>
        storeDurchgangFinished(eventDurchgangFinished)
        notifyWebSocketClients(senderWebSocket, eventDurchgangFinished, durchgang)
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
        log.debug(s"received for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm new Wertung: D:${wertung.noteD}, E:${wertung.noteE}")
        val verifiedWertung = updateWertungSimple(wertung)
        val updated = AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet, programm)
        log.info(s"saved for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm new Wertung: D:${verifiedWertung.noteD}, E:${verifiedWertung.noteE}")

        persist(updated) { _ => }
        handleEvent(updated)
        val handledEvent = updated.toAthletWertungUpdatedSequenced(state.lastSequenceId)
        log.debug("completed " + handledEvent)
        updategeraeteRigeListe(handledEvent)
        log.debug("updated riegenliste " + handledEvent)
        sender() ! handledEvent

        addToDurchgangJournal(handledEvent, durchgang)
        notifyWebSocketClients(senderWebSocket, handledEvent, durchgang)
        notifyBestenResult()
        //        }
      } catch {
        case e: Exception =>
          log.error(s"failed to complete save new score for  ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm new Wertung: D:${wertung.noteD}, E:${wertung.noteE}", e)
          sender() ! MessageAck(e.getMessage)
      }

    case awu: AthletWertungUpdated => websocketProcessor(Some(sender()), awu)
    case awu: AthletWertungUpdatedSequenced => websocketProcessor(Some(sender()), awu)
    case awu: AthletsAddedToWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: AthletMovedInWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: AthletRemovedFromWettkampf => websocketProcessor(Some(sender()), awu)
    case awu: ScoresPublished => websocketProcessor(Some(sender()), awu)

    case fds: FinishDurchgangStation =>
      persist(DurchgangStationFinished(fds.wettkampfUUID, fds.durchgang, fds.geraet, fds.step)) { evt =>
        handleEvent(evt)
        sender() ! MessageAck("OK")
      }

    case fds: FinishDurchgangStep =>
      persist(DurchgangStepFinished(fds.wettkampfUUID)) { evt =>
        handleEvent(evt)
        sender() ! MessageAck("OK")
        notifyBestenResult()
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
        .labels(wettkampf.easyprint, durchgangNormalized.getOrElse("all"))
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
      ref ! NewLastResults(state.lastWertungen, state.lastBestenResults)

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
        (LocalDate.now().before(wettkampf.datum) || LocalDate.now().minusDays(2).after(wettkampf.datum))
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
          .labels(wettkampf.easyprint, dg.getOrElse("all"))
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

  private def checkDonation(): Unit = {
    if (donationActiv && LocalDateTime.of(wettkampf.datum, LocalTime.of(7, 0, 0)).plusDays(2).isBefore(LocalDateTime.now()) && !state.completedflags.exists {
      case DonationMailSent(_, _, _, wkid) if wkid == this.wettkampfUUID => true
      case _ => false
    }) {
      val teilnehmer = getAllKandidatenWertungen(UUID.fromString(wettkampfUUID))
      val results = teilnehmer.flatMap(k => k.wertungen.map(w => w.resultat.endnote)).sum
      if (results > 0 && teilnehmer.size > 10) {
        val price = BigDecimal(Config.donationPrice)
        val donationLink = Config.donationLink
        KuTuMailerActor.send(
          MailTemplates.createDonateMail(wettkampf, donationLink, teilnehmer.size, price)
        )
      }
      handleEvent(DonationMailSent(0, BigDecimal(0), "", wettkampfUUID))
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
        .labels(wettkampf.easyprint, dg.getOrElse("all"))
        .set(wsSend.get(dg).size)
    })
  }

  private def squashDurchgangEvents(durchgangNormalized: Option[String]) = {
    (durchgangNormalized match {
      case Some(dgn) => // take last of requested durchgang
        state.startStopEvents.reverse.filter {
          case DurchgangStarted(_, d, _) => encodeURIComponent(d).equals(dgn)
          case DurchgangFinished(_, d, _) => encodeURIComponent(d).equals(dgn)
          case _ => false
        }.take(1)
      case _ => //take first and last per durchgang
        state.startStopEvents.groupBy {
          case DurchgangStarted(w, d, t) => d
          case DurchgangFinished(w, d, t) => d
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
      case _ => 0L
    }
  }

  def handleWebsocketMessages(originSender: Option[ActorRef], event: KutuAppEvent): Unit = {
    val senderWebSocket = actorWithSameDeviceIdOfSender(originSender.getOrElse(sender()))

    def forwardToListeners(handledEvent: AthletWertungUpdatedSequenced): Unit = {
      updategeraeteRigeListe(handledEvent)
      addToDurchgangJournal(handledEvent, handledEvent.durchgang)
      notifyWebSocketClients(senderWebSocket, handledEvent, handledEvent.durchgang)
      notifyBestenResult()
    }

    event match {
      case awuv: AthletWertungUpdated =>
        persist(awuv) { _ => }
        handleEvent(awuv)
        val handledEvent = awuv.toAthletWertungUpdatedSequenced(state.lastSequenceId)
        forwardToListeners(handledEvent)

      case awuv: AthletWertungUpdatedSequenced =>
        persist(awuv) { _ => }
        handleEvent(awuv)
        val handledEvent = awuv.toAthletWertungUpdated().toAthletWertungUpdatedSequenced(state.lastSequenceId)
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

      case _ =>
    }
    val s = sender()
    s ! MessageAck("OK")
    originSender.foreach(os => if (!os.equals(s)) {os ! MessageAck("OK")})
  }

  private def notifyWebSocketClients(
                                      senderWebSocket: Iterable[ActorRef],
                                      toPublish: KutuAppEvent,
                                      durchgang: String) = Future {
    if (durchgang == "") {
      wsSend.values.foreach(wsList => {
        wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
      })
    } else {
      wsSend.get(Some(encodeURIComponent(durchgang))) match {
        case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
        case _ =>
      }
      wsSend.get(None) match {
        case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
        case _ =>
      }
    }
  }

  private def updategeraeteRigeListe(toPublish: AthletWertungUpdatedSequenced): Unit = {
    val wertung = toPublish.wertung
    geraeteRigeListe = geraeteRigeListe.map(_.updated(wertung))
  }

  private def addToDurchgangJournal(toPublish: AthletWertungUpdatedSequenced, durchgang: String): Unit = {
    if (durchgang == "") {
      openDurchgangJournal = openDurchgangJournal.map {
        case (dgoption: Option[String], list: List[AthletWertungUpdatedSequenced]) =>
          dgoption -> (list :+ toPublish)
        case x => x
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

  def notifyBestenResult(): Unit = {
    val msg = NewLastResults(state.lastWertungen, state.lastBestenResults)
    notifyWebSocketClients(Iterable.empty, msg, "")
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
  lazy val competitionsActive: client.Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(Collector.sanitizeMetricName(Config.metricsNamespaceName))
    .name("competitions_active")
    .help("Active competitions")
    .create().register()
  lazy val competitionWebsocketConnectionsActive: client.Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(Collector.sanitizeMetricName(Config.metricsNamespaceName))
    .name(Collector.sanitizeMetricName("competition_websockets_active"))
    .labelNames("comp", "dg")
    .help("Active websocket connections for per competition and durchgang")
    .create()
  competitionWebsocketConnectionsActive.register(CollectorRegistry.defaultRegistry)

  case class PublishAction(id: String, action: KutuAppAction)

  import ch.seidel.kutu.http.Core._

  val supervisor: ActorRef = system.actorOf(Props[ClientActorSupervisor](), name = "Supervisor")

  def publish(action: KutuAppAction, context: String): Future[KutuAppEvent] = {
    implicit val timeout: Timeout = Timeout(15000 milli)
    (supervisor ? PublishAction(context, action)).mapTo[KutuAppEvent]
  }

  def stopAll(): Future[String] = {
    implicit val timeout: Timeout = Timeout(15000 milli)
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
      logger.debug("unparsable json mapped to MessageAck: " + text, e)
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

  val completionMatcher: PartialFunction[scala.Any, akka.stream.CompletionStrategy] = {
    case StopDevice(_) => CompletionStrategy.immediately
  }
  val failureMatcher: PartialFunction[scala.Any, scala.Throwable] = {
    case Err(x) => x
  }

  // authenticated bidirectional streaming
  def createActorSinkSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String], lastSequenceId: Option[Long]): Flow[Message, Message, Any] = {
    val clientActor = Await.result(
      ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)

    val sink = fromWebsocketToActorFlow.to(Sink.actorRef(clientActor, StopDevice(deviceId), _ => StopDevice(deviceId)).named(deviceId))
    val source = fromCoordinatorActorToWebsocketFlow(lastSequenceId,
      Source.actorRef(completionMatcher, failureMatcher, 256, OverflowStrategy.dropHead)
        .mapMaterializedValue { wsSource: ActorRef =>
          clientActor ! Subscribe(wsSource, deviceId, durchgang, lastSequenceId)
          wsSource
        }.named(deviceId))

    Flow.fromSinkAndSource(sink, source).log(name = deviceId)
  }

  // unauthenticted oneway/readonly streaming
  def createActorSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String], lastSequenceId: Option[Long] = None): Flow[Message, Message, Any] = {
    val clientActor = Await.result(
      ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)

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

    Flow.fromSinkAndSource(sink, source).log(name = deviceId)

  }
}
