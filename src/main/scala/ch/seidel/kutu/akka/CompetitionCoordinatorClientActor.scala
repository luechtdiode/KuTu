package ch.seidel.kutu.akka

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.control.NonFatal

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy.Stop
import akka.actor.Terminated
import akka.pattern.ask

import spray.json._

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import ch.seidel.kutu.domain.Wertung
import ch.seidel.kutu.http.EnrichedJson
import ch.seidel.kutu.http.JsonSupport
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import ch.seidel.kutu.domain._
import akka.stream.scaladsl.Keep
import ch.seidel.kutu.data.ResourceExchanger
import akka.actor.ActorLogging
import org.slf4j.LoggerFactory

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends Actor with JsonSupport with KutuService with ActorLogging {
  import context._

  var wsSend: Map[Option[String],List[ActorRef]] = Map.empty
  var deviceWebsocketRefs: Map[String,ActorRef] = Map.empty
  var pendingKeepAliveAck: Option[Int] = None

  var startedDurchgaenge: Set[String] = Set.empty
  var finishedDurchgangSteps: Set[FinishDurchgangStation] = Set.empty
  var finishedDurchgaenge: Set[String] = Set.empty
  var startStopEvents: List[KutuAppEvent] = List.empty
  // Durchgang -> WettkampfdisciplinId -> Wertung
  var lastWertungen: Map[String, WertungContainer] = Map.empty
  var bestenResults = Map[String,WertungContainer]()
  var lastBestenResults = Map[String,WertungContainer]()
  
  private def deviceIdOf(actor: ActorRef) = deviceWebsocketRefs.filter(_._2 == actor).map(_._1)
  private def actorWithSameDeviceIdOfSender(originSender: ActorRef = sender) = deviceWebsocketRefs.filter(p => originSender.path.name.endsWith(p._1)).map(_._2)
  
  // send keepalive messages to prevent closing the websocket connection
  private case object KeepAlive
  val liveticker = context.system.scheduler.schedule(15.second, 15.second) {
    self ! KeepAlive
  }

  val websocketProcessor = ResourceExchanger.processWSMessage(handleWebsocketMessages)
  
  override def preStart(): Unit = {
    log.info("Starting CompetitionCoordinatorClientActor")
  }

  override def postStop: Unit = {
    liveticker.cancel()
    log.info("CompetitionCoordinatorClientActor stopped")
    wsSend.values.flatten.foreach(context.stop)
  }

  override def receive = {

    case StartDurchgang(wettkampfUUID, durchgang) =>
      val eventDurchgangStarted = DurchgangStarted(wettkampfUUID, durchgang)
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      startStopEvents = startStopEvents :+ eventDurchgangStarted
      startedDurchgaenge += eventDurchgangStarted.durchgang
      finishedDurchgaenge -= eventDurchgangFinished.durchgang
      finishedDurchgangSteps = finishedDurchgangSteps.filter(fds => encodeURIComponent(fds.durchgang) != encodeURIComponent(durchgang))
      sender ! eventDurchgangStarted

      val dgsText = TextMessage(eventDurchgangStarted.asInstanceOf[KutuAppEvent].toJson.compactPrint)
      wsSend.get(Some(encodeURIComponent(durchgang))) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      wsSend.get(None) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      
    case FinishDurchgang(wettkampfUUID, durchgang) =>
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      startStopEvents = startStopEvents :+ eventDurchgangFinished
      startedDurchgaenge -= durchgang
      finishedDurchgaenge += eventDurchgangFinished.durchgang
      sender ! eventDurchgangFinished
      val dgsText = TextMessage(eventDurchgangFinished.asInstanceOf[KutuAppEvent].toJson.compactPrint)
      wsSend.get(Some(encodeURIComponent(durchgang))) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      wsSend.get(None) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      
    case uw @ UpdateAthletWertung(athlet, wertung, wettkampfUUID, durchgang, geraet, step, programm) =>
      val senderWebSocket = actorWithSameDeviceIdOfSender()
      if (finishedDurchgangSteps.exists(fds => encodeURIComponent(fds.durchgang) == encodeURIComponent(durchgang) && fds.geraet == geraet && fds.step == step+1)) {
        sender ! MessageAck("Diese Station ist bereits abgeschlossen und kann keine neuen Resultate mehr entgegennehmen.")
      } else if (!startedDurchgaenge.exists(d => encodeURIComponent(d) == encodeURIComponent(durchgang))) {
        sender ! MessageAck("Dieser Durchgang ist noch nicht für die Resultaterfassung freigegeben.")
      } else try {
        val verifiedWertung = updateWertungSimple(wertung, true)
        
        val awu: KutuAppEvent = AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet, programm)
        sender ! awu     

        updateLastWertungen(
            WertungContainer(
                athlet.id, athlet.vorname, athlet.name, athlet.geschlecht, athlet.verein.map(_.name).getOrElse(""), 
                verifiedWertung, 
                geraet, programm))

        val toPublish = TextMessage(awu.toJson.compactPrint)
        wsSend.get(Some(encodeURIComponent(durchgang))) match {
          case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
          case _ =>
        }
        wsSend.get(None) match {
          case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
          case _ =>
        }
      } catch {
        case e: Exception =>
          sender ! MessageAck(e.getMessage)
      }
        
    case awu: AthletWertungUpdated => websocketProcessor(Some(sender), awu)
      
    case fds: FinishDurchgangStation =>
      finishedDurchgangSteps += fds
      sender ! MessageAck("OK")
      
    case fds: FinishDurchgangStep =>
      resetBestenResult()
      sender ! MessageAck("OK")
      
    case uw: KutuAppAction =>
      wsSend.flatMap(_._2).foreach(ws => ws ! uw)
      sender ! MessageAck("OK")
      
    case Subscribe(ref, deviceId, durchgang) =>
      val durchgangNormalized = durchgang.map(encodeURIComponent)
      val durchgangClients = wsSend.getOrElse(durchgangNormalized, List.empty) :+ ref
      context.watch(ref)
      wsSend = wsSend + (durchgangNormalized -> durchgangClients)
      deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
      ref ! TextMessage("Connection established.")      
      (durchgang match {
        case Some(dgn) => // take last of requested durchgang
          startStopEvents.seq.reverse.filter {
            case DurchgangStarted(w, d, t) => encodeURIComponent(d) == dgn
            case DurchgangFinished(w, d, t) => encodeURIComponent(d) == dgn
            case _ => false
          }.take(1)
        case _ => //take first and last per durchgang
          startStopEvents.groupBy { 
            case DurchgangStarted(w, d, t) => d
            case DurchgangFinished(w, d, t) => d
            case _ => "_"
          }
          .filter(_._1 != "_")
          .flatMap(d => Seq(d._2.head, d._2.last))
        }
      )
      .foreach(d => ref ! TextMessage(d.asInstanceOf[KutuAppEvent].toJson.compactPrint))
      ref ! TextMessage(NewLastResults(lastWertungen, lastBestenResults).asInstanceOf[KutuAppEvent].toJson.compactPrint)

    // system actions
    case KeepAlive => wsSend.flatMap(_._2).foreach(ws => ws ! TextMessage("KeepAlive"))
    
    case MessageAck(txt) => if (txt.equals("keepAlive")) handleKeepAliveAck else println(txt)

    case StopDevice(deviceId) =>
      log.info(s"stopped device $deviceId")
      val stoppedWebsocket = deviceWebsocketRefs(deviceId)
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop
      
    case Terminated(stoppedWebsocket) =>
      context.unwatch(stoppedWebsocket)
      val deviceId = deviceWebsocketRefs.filter(x => x._2 == stoppedWebsocket).map(_._1).headOption
      log.info(s"terminated device $deviceId")
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop
      
    case _: Unit => handleStop
    case _ =>
  }

  def handleWebsocketMessages(originSender: Option[ActorRef], event: KutuAppEvent) {
    event match {
      case awuv: AthletWertungUpdated =>
        val senderWebSocket = actorWithSameDeviceIdOfSender(originSender.getOrElse(sender))

        val toPublish = TextMessage(event.toJson.compactPrint)
        if (awuv.durchgang == "") {
          wsSend.foreach(entry => {
            val (dgoption, actors) = entry
            wsSend.get(dgoption) match {
              case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
              case _ =>
            }
          })
        } else {
          wsSend.get(Some(encodeURIComponent(awuv.durchgang))) match {
            case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
            case _ =>
          }
          wsSend.get(None) match {
            case Some(wsList) => wsList.filter(ws => !senderWebSocket.exists(_ == ws)).foreach(ws => ws ! toPublish)
            case _ =>
          }
        }
        updateLastWertungen(
            WertungContainer(
                awuv.athlet.id, awuv.athlet.vorname, awuv.athlet.name, awuv.athlet.geschlecht, awuv.athlet.verein.map(_.name).getOrElse(""), 
                awuv.wertung, 
                awuv.geraet, awuv.programm))
      case _ =>
    }
  }
  
  def updateLastWertungen(wertungContainer: WertungContainer) {
    lastWertungen = lastWertungen.updated(wertungContainer.wertung.wettkampfdisziplinId, wertungContainer)
    if(wertungContainer.wertung.endnote >= 8.7) {
      putBestenResult(wertungContainer)
    }
    
    val wsSends = wsSend.values
    val msg = TextMessage(NewLastResults(lastWertungen, lastBestenResults).asInstanceOf[KutuAppEvent].toJson.compactPrint)
//    println(msg)
    Future {
      for(wsList <- wsSends) {
        wsList.foreach(ws => ws ! msg)
      }
    }
  }
  
  def putBestenResult(wertungContainer: WertungContainer) {
    bestenResults = bestenResults.updated(wertungContainer.id + ":" + wertungContainer.wertung.wettkampfdisziplinId, wertungContainer)
  }
  
  def resetBestenResult() {
    lastBestenResults = bestenResults
    bestenResults = Map[String,WertungContainer]()
    val wsSends = wsSend.values
    val msg = TextMessage(NewLastResults(lastWertungen, lastBestenResults).asInstanceOf[KutuAppEvent].toJson.compactPrint)
//    println(msg)
    Future {
      for(wsList <- wsSends) {
        wsList.foreach(ws => ws ! msg)
      }
    }
  }
  
  private def handleStop {
    stop(self)
  }

  private def handleKeepAlive {
    wsSend.values.flatten.foreach(_ ! TextMessage("keepAlive"))
    if (wsSend.nonEmpty) pendingKeepAliveAck = pendingKeepAliveAck.map(_ + 1) match {
      case Some(i) if (i < 10) =>
        Some(i)
      case Some(i) if (i >= 10) =>
        handleStop
        None
      case _ =>
        Some(1)
    }
  }

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
          val coordinator = context.actorOf(CompetitionCoordinatorClientActor.props(wettkampfUUID), "client-" + wettkampfUUID)
          context.watch(coordinator)
          wettkampfCoordinators = wettkampfCoordinators + (wettkampfUUID -> coordinator)
          log.info(s"Connect new client to new coordinator. Device: $deviceID, Wettkampf: $wettkampfUUID")
          coordinator
      }
      sender ! coordinator
    
    case uw: KutuAppAction =>
      wettkampfCoordinators.get(uw.wettkampfUUID) match {
        case Some(coordinator) => coordinator.forward(uw)
        case _=> sender ! MessageAck("OK")
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
    implicit val timeout = Timeout(5000 milli)
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
    val source: Source[Nothing, ActorRef] = Source.actorRef(256, OverflowStrategy.dropNew).mapMaterializedValue { wsSource =>
      clientActor ! Subscribe(wsSource, deviceId, durchgang)
      wsSource
    }.named(deviceId)

    Flow.fromSinkAndSource(sink, source)
  }
      
  def createActorSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String]): Flow[Message, Message, Any] = {
    val clientActor = Await.result(ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)     
    
    val sink = websocketFlow.filter{
      case MessageAck(msg) if (msg.equals("keepAlive")) => true
      case _ => false
    }.to(Sink.actorRef(clientActor, StopDevice(deviceId)).named(deviceId))
    
    val source: Source[Nothing, ActorRef] = Source.actorRef(256, OverflowStrategy.dropNew).mapMaterializedValue { wsSource =>
      clientActor ! Subscribe(wsSource, deviceId, durchgang)
      wsSource
    }.named(deviceId)

    Flow.fromSinkAndSource(sink, source)
  }
}
