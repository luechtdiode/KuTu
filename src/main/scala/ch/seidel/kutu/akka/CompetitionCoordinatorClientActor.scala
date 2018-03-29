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

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends Actor with JsonSupport with KutuService with ActorLogging {
  import context._

  var wsSend: Map[Option[String],List[ActorRef]] = Map.empty
  var deviceWebsocketRefs: Map[String,ActorRef] = Map.empty
  var pendingKeepAliveAck: Option[Int] = None

  var startedDurchgaenge: Set[DurchgangStarted] = Set.empty
  var finishedDurchgangSteps: Set[FinishDurchgangStation] = Set.empty
  var finishedDurchgaenge: Set[DurchgangFinished] = Set.empty
  
  private def deviceIdOf(actor: ActorRef) = deviceWebsocketRefs.filter(_._2 == actor).map(_._1)
  private def actorWithSameDeviceIdOfSender = deviceWebsocketRefs.filter(p => sender.path.name.endsWith(p._1)).map(_._2)
  
  // send keepalive messages to prevent closing the websocket connection
  private case object KeepAlive
  val liveticker = context.system.scheduler.schedule(15.second, 15.second) {
    self ! KeepAlive
  }

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
      startedDurchgaenge += eventDurchgangStarted
      finishedDurchgaenge -= eventDurchgangFinished
      finishedDurchgangSteps = finishedDurchgangSteps.filter(fds => encodeURIComponent(fds.durchgang) != encodeURIComponent(durchgang))
      val dgsText = TextMessage(eventDurchgangStarted.asInstanceOf[KutuAppEvent].toJson.compactPrint)
      wsSend.get(Some(durchgang)) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      sender ! eventDurchgangStarted
      
    case FinishDurchgang(wettkampfUUID, durchgang) =>
      val eventDurchgangFinished = DurchgangFinished(wettkampfUUID, durchgang)
      startedDurchgaenge -= DurchgangStarted(wettkampfUUID, durchgang)
      finishedDurchgaenge += eventDurchgangFinished
      val dgsText = TextMessage(eventDurchgangFinished.asInstanceOf[KutuAppEvent].toJson.compactPrint)
      wsSend.get(Some(durchgang)) match {
        case Some(wsList) => wsList.foreach(ws => ws ! dgsText)
        case _ =>
      }
      sender ! eventDurchgangFinished
      
    case uw @ UpdateAthletWertung(athlet, wertung, wettkampfUUID, durchgang, geraet, step) =>
      if (finishedDurchgangSteps.exists(fds => encodeURIComponent(fds.durchgang) == encodeURIComponent(durchgang) && fds.geraet == geraet && fds.step == step+1)) {
        sender ! MessageAck("Diese Station ist bereits abgeschlossen und kann keine neuen Resultate mehr entgegennehmen.")
      } else if (!startedDurchgaenge.exists(d => encodeURIComponent(d.durchgang) == encodeURIComponent(durchgang))) {
        sender ! MessageAck("Dieser Durchgang ist noch nicht für die Resultaterfassung freigegeben.")
      } else try {
        val verifiedWertung = updateWertungSimple(wertung, true)
       
        // calculate progress for durchgang and for durchgang-geraet
        // if complete, close durchgang and commit to wettkampf-origin
        val awu: KutuAppEvent = AthletWertungUpdated(athlet, verifiedWertung, wettkampfUUID, durchgang, geraet)
        val toPublish = TextMessage(awu.toJson.compactPrint)
        wsSend.flatMap(_._2).foreach(ws => ws ! toPublish)
        sender ! awu
      } catch {
        case e: Exception =>
          sender ! MessageAck(e.getMessage)
      }
      
    case awu: AthletWertungUpdated =>
      val senderWebSocket = actorWithSameDeviceIdOfSender
      ResourceExchanger.processWSMessage(event => event match {
        case awuv: KutuAppEvent =>
          val toPublish = TextMessage(awuv.toJson.compactPrint)
          wsSend.flatMap(_._2).filter(ws => !senderWebSocket.exists(_ == ws)).foreach{ws => 
            println("publishing from " + senderWebSocket + " to " + ws.path)
            ws ! toPublish
          }
        case _ =>
      })(awu)
      
    case fds: FinishDurchgangStation =>
      finishedDurchgangSteps += fds
      sender ! MessageAck("OK")
      
    case uw: KutuAppAction =>
      wsSend.flatMap(_._2).foreach(ws => ws ! uw)
      sender ! MessageAck("OK")
      
    case Subscribe(ref, deviceId, durchgang) =>
      val durchgangClients = wsSend.getOrElse(durchgang, List.empty) :+ ref
      context.watch(ref)
      wsSend = wsSend + (durchgang -> durchgangClients)
      deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
      ref ! TextMessage("Connection established.")      
      startedDurchgaenge.filter(d => durchgang.exists(_ == d.durchgang)).foreach(d => ref ! TextMessage(d.asInstanceOf[KutuAppEvent].toJson.compactPrint))

    // system actions
    case KeepAlive => wsSend.flatMap(_._2).foreach(ws => ws ! TextMessage("KeepAlive"))
    
    case MessageAck(txt) => if (txt.equals("keepAlive")) handleKeepAliveAck else println(txt)

    case StopDevice(deviceId) =>
      val stoppedWebsocket = deviceWebsocketRefs(deviceId)
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop
      
    case Terminated(stoppedWebsocket) =>
      context.unwatch(stoppedWebsocket)
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (startedDurchgaenge.isEmpty && wsSend.isEmpty) handleStop
      
    case _: Unit => handleStop
    case _ =>
  }

  private def handleStop {
    log.info("Closing client actor")
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
        case Some(coordinator) => coordinator
        case _ =>
          val coordinator = context.actorOf(CompetitionCoordinatorClientActor.props(wettkampfUUID), "client-" + wettkampfUUID)
          context.watch(coordinator)
          wettkampfCoordinators = wettkampfCoordinators + (wettkampfUUID -> coordinator)
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
          println(s"WS-Server stream failed with $cause")
        case s => // ignore regular completion
          println(s.toString)
          println(s"WS-Server stream closed")
      })

  
  def tryMapText(text: String): KutuAppEvent = try {
    text.asType[KutuAppEvent]
  } catch {
    case e: Exception => 
      e.printStackTrace
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
