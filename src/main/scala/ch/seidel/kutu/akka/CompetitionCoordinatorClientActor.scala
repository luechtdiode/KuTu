package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.{EnrichedJson, JsonSupport}
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Failure
import scala.util.control.NonFatal

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends PersistentActor with JsonSupport with KutuService with ActorLogging {

  import context._

  private val wettkampf = readWettkampf(wettkampfUUID)
  private val websocketProcessor = ResourceExchanger.processWSMessage(wettkampf, handleWebsocketMessages)
  private val wkPgmId = wettkampf.programmId
  private val isDNoteUsed = wkPgmId != 20 && wkPgmId != 1
  private val snapShotInterval = 100

  private var wsSend: Map[Option[String], List[ActorRef]] = Map.empty
  private var deviceWebsocketRefs: Map[String, ActorRef] = Map.empty
  private var pendingKeepAliveAck: Option[Int] = None

  private var state: CompetitionState = CompetitionState()

  private def deviceIdOf(actor: ActorRef) = deviceWebsocketRefs.filter(_._2 == actor).map(_._1)

  private def actorWithSameDeviceIdOfSender(originSender: ActorRef = sender): Iterable[ActorRef] =
    deviceWebsocketRefs.filter(p => originSender.path.name.endsWith(p._1)).map(_._2)

  // send keepalive messages to prevent closing the websocket connection
  private case object KeepAlive

  private val liveticker = context.system.scheduler.schedule(15.second, 15.second) {
    self ! KeepAlive
  }

  override def persistenceId = wettkampfUUID

  override val supervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in WettkampfCoordinator actor " + wettkampf, e)
      Restart
  }

  override def preStart(): Unit = {
    log.info("Starting CompetitionCoordinatorClientActor")
  }

  override def postStop: Unit = {
    liveticker.cancel()
    log.info("CompetitionCoordinatorClientActor stopped")
    wsSend.values.flatten.foreach(context.stop)
  }

  val receiveRecover: Receive = {
    case evt: KutuAppEvent => handleEvent(evt)
    case SnapshotOffer(_, snapshot: CompetitionState) => state = snapshot
    case _ =>
  }

  def handleEvent(evt: KutuAppEvent) {
    state = state.updated(evt, isDNoteUsed)
    if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
      saveSnapshot(state)
  }

  val receiveCommand: Receive = {

    case StartedDurchgaenge(_) => sender ! ResponseMessage(state.startedDurchgaenge)

    case StartDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val started = DurchgangStarted(wettkampfUUID, durchgang)
      persist(started) { evt =>
        handleEvent(evt)
        sender ! started

        val toPublish = TextMessage(started.asInstanceOf[KutuAppEvent].toJson.compactPrint)
        notifyWebSocketClients(senderWebSocket, toPublish, durchgang)
        val msg = TextMessage(
          NewLastResults(state.lastWertungen, state.lastBestenResults)
            .asInstanceOf[KutuAppEvent]
            .toJson
            .compactPrint)
        notifyWebSocketClients(senderWebSocket, msg, durchgang)
      }

    case FinishDurchgang(wettkampfUUID, durchgang) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      persist(eventDurchgangFinished) { evt =>
        handleEvent(evt)
        sender ! eventDurchgangFinished
        val toPublish = TextMessage(eventDurchgangFinished.asInstanceOf[KutuAppEvent].toJson.compactPrint)
        notifyWebSocketClients(senderWebSocket, toPublish, durchgang)
      }

    case UpdateAthletWertung(athlet, wertung, wettkampfUUID, durchgang, geraet, step, programm) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      if (state.finishedDurchgangSteps.exists(fds =>
        encodeURIComponent(fds.durchgang) == encodeURIComponent(durchgang)
          && fds.geraet == geraet && fds.step == step + 1)) {
        sender ! MessageAck("Diese Station ist bereits abgeschlossen und kann keine neuen Resultate mehr entgegennehmen.")
      } else if (!state.startedDurchgaenge.exists(d => encodeURIComponent(d) == encodeURIComponent(durchgang))) {
        sender ! MessageAck("Dieser Durchgang ist noch nicht für die Resultaterfassung freigegeben.")
      } else try {
        val verifiedWertung = updateWertungSimple(wertung, true)
        val awu: KutuAppEvent = AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet, programm)
        persist(awu){case _ =>}
//        persist(awu) { evt =>
          handleEvent(awu)
          sender ! awu

          val toPublish = TextMessage(awu.toJson.compactPrint)
          notifyWebSocketClients(senderWebSocket, toPublish, durchgang)
          notifyBestenResult()
//        }
      } catch {
        case e: Exception =>
          sender ! MessageAck(e.getMessage)
      }

    case awu: AthletWertungUpdated => websocketProcessor(Some(sender), awu)

    case fds: FinishDurchgangStation =>
      persist(DurchgangStationFinished(fds.wettkampfUUID, fds.durchgang, fds.geraet, fds.step)) { evt =>
        handleEvent(evt)
        sender ! MessageAck("OK")
      }

    case fds: FinishDurchgangStep =>
      persist(DurchgangStepFinished(fds.wettkampfUUID)) { evt =>
        handleEvent(evt)
        sender ! MessageAck("OK")
        notifyBestenResult()
      }

    case Subscribe(ref, deviceId, durchgang) =>
      val durchgangNormalized = durchgang.map(encodeURIComponent)
      val durchgangClients = wsSend.getOrElse(durchgangNormalized, List.empty) :+ ref
      context.watch(ref)
      wsSend = wsSend + (durchgangNormalized -> durchgangClients)
      deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
      ref ! TextMessage("Connection established.")
      squashDurchgangEvents(durchgangNormalized).foreach { d =>
        ref ! TextMessage(d.asInstanceOf[KutuAppEvent].toJson.compactPrint)
      }
      ref ! TextMessage(
        NewLastResults(state.lastWertungen, state.lastBestenResults)
          .asInstanceOf[KutuAppEvent]
          .toJson
          .compactPrint)

    // system actions
    case KeepAlive => wsSend.flatMap(_._2).foreach(ws => ws ! TextMessage("KeepAlive"))

    case MessageAck(txt) => if (txt.equals("keepAlive")) handleKeepAliveAck else println(txt)

    case StopDevice(deviceId) =>
      log.info(s"stopped device $deviceId")
      deviceWebsocketRefs.get(deviceId).foreach { stoppedWebsocket =>
        deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)
        wsSend = wsSend.map { x => (x._1, x._2.filter(_ != stoppedWebsocket)) }.filter(x => x._2.nonEmpty)
      }
      if (state.startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop

    case Terminated(stoppedWebsocket) =>
      context.unwatch(stoppedWebsocket)
      val deviceId = deviceWebsocketRefs.filter(x => x._2 == stoppedWebsocket).map(_._1).headOption
      log.info(s"terminated device $deviceId")
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)
      wsSend = wsSend.map { x => (x._1, x._2.filter(_ != stoppedWebsocket)) }.filter(x => x._2.nonEmpty)
      if (state.startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop

    case _ =>
  }

  private def squashDurchgangEvents(durchgangNormalized: Option[String]) = {
    durchgangNormalized match {
      case Some(dgn) => // take last of requested durchgang
        state.startStopEvents.seq.reverse.filter {
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
    }
  }

  def handleWebsocketMessages(originSender: Option[ActorRef], event: KutuAppEvent) {
    event match {
      case awuv: AthletWertungUpdated =>
        val senderWebSocket = actorWithSameDeviceIdOfSender(originSender.getOrElse(sender))
        persist(awuv){case _ =>}
//        persist(awuv) { evt =>
          handleEvent(awuv)
          val toPublish = TextMessage(event.toJson.compactPrint)
          notifyWebSocketClients(senderWebSocket, toPublish, awuv.durchgang)
          notifyBestenResult()
//        }
      case _ =>
    }
  }

  private def notifyWebSocketClients(
                                      senderWebSocket: Iterable[ActorRef],
                                      toPublish: TextMessage.Strict,
                                      durchgang: String) = Future {
    if (durchgang == "") {
      wsSend.foreach(entry => {
        val (dgoption, _) = entry
        wsSend.get(dgoption) match {
          case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
          case _ =>
        }
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

  def notifyBestenResult() {
    val msg = TextMessage(NewLastResults(state.lastWertungen, state.lastBestenResults)
      .asInstanceOf[KutuAppEvent]
      .toJson
      .compactPrint)
    notifyWebSocketClients(Iterable.empty, msg, "")
  }

  private def handleStop {
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

  private def handleKeepAliveAck {
    pendingKeepAliveAck = pendingKeepAliveAck.map(_ - 1) match {
      case Some(i) if (i > 0) => Some(i)
      case _ => None
    }
  }

}

class ClientActorSupervisor extends Actor with ActorLogging {

  var wettkampfCoordinators = Map[String, ActorRef]()

  override val supervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in WettkampfCoordinator actor", e)
      Stop
  }

  override def receive = {
    case CreateClient(deviceID, wettkampfUUID) =>
      val coordinator = wettkampfCoordinators.get(wettkampfUUID) match {
        case Some(coordinator) =>
          log.info(s"Connect new client to existing coordinator. Device: $deviceID, Wettkampf: $wettkampfUUID")
          coordinator
        case _ =>
          val coordinator = context.actorOf(
            CompetitionCoordinatorClientActor.props(wettkampfUUID), "client-" + wettkampfUUID)
          context.watch(coordinator)
          wettkampfCoordinators = wettkampfCoordinators + (wettkampfUUID -> coordinator)
          log.info(s"Connect new client to new coordinator. Device: $deviceID, Wettkampf: $wettkampfUUID")
          coordinator
      }
      sender ! coordinator

    case uw: KutuAppAction =>
      wettkampfCoordinators.get(uw.wettkampfUUID) match {
        case Some(coordinator) => coordinator.forward(uw)
        case _ =>
          println("Action for unknown competition: " + uw)
          sender ! MessageAck("Action for unknown competition: " + uw)
      }

    case Terminated(wettkampfActor) =>
      context.unwatch(wettkampfActor)
      wettkampfCoordinators = wettkampfCoordinators.filter(_._2 != wettkampfActor)

    case MessageAck(text) => println(text)
  }
}

object CompetitionCoordinatorClientActor extends JsonSupport with EnrichedJson {
  private val logger = LoggerFactory.getLogger(this.getClass)

  import ch.seidel.kutu.http.Core._

  val supervisor = system.actorOf(Props[ClientActorSupervisor])

  def publish(action: KutuAppAction) = {
    implicit val timeout = Timeout(15000 milli)
    (supervisor ? action).mapTo[KutuAppEvent]
  }

  def props(wettkampfUUID: String) = Props(classOf[CompetitionCoordinatorClientActor], wettkampfUUID)

  def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          logger.error(s"WS-Server stream failed with $cause")
        case s => // ignore regular completion
          logger.info(s"WS-Server stream closed")
      })


  def tryMapText(text: String): KutuAppEvent = try {
    text.asType[KutuAppEvent]
  } catch {
    case e: Exception =>
      logger.debug("unparsable json mapped to MessageAck: " + text)
      MessageAck(text)
  }

  def websocketFlow: Flow[Message, KutuAppEvent, Any] =
    Flow[Message]
      .mapAsync(1) {
        case TextMessage.Strict(text) => Future.successful(tryMapText(text))
        case TextMessage.Streamed(stream) => stream.runFold("")(_ + _).map(tryMapText(_))
        case b: BinaryMessage => throw new Exception("Binary message cannot be handled")
      }.via(reportErrorsFlow)

  def createActorSinkSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String]): Flow[Message, Message, Any] = {
    val clientActor = Await.result(ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)

    val sink = websocketFlow.to(Sink.actorRef(clientActor, StopDevice(deviceId)).named(deviceId))
    val source: Source[Nothing, ActorRef] = Source.actorRef(256, OverflowStrategy.dropNew)
      .mapMaterializedValue { wsSource =>
        clientActor ! Subscribe(wsSource, deviceId, durchgang)
        wsSource
      }.named(deviceId)

    Flow.fromSinkAndSource(sink, source)
  }

  def createActorSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String]): Flow[Message, Message, Any] = {
    val clientActor = Await.result(
      ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)

    val sink = websocketFlow.filter {
      case MessageAck(msg) if (msg.equals("keepAlive")) => true
      case _ => false
    }.to(Sink.actorRef(clientActor, StopDevice(deviceId)).named(deviceId))

    val source: Source[Nothing, ActorRef] = Source.actorRef(
      256,
      OverflowStrategy.dropNew).mapMaterializedValue { wsSource =>
      clientActor ! Subscribe(wsSource, deviceId, durchgang)
      wsSource
    }.named(deviceId)

    Flow.fromSinkAndSource(sink, source)
  }
}
