package ch.seidel.kutu.akka

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.pattern.ask

import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
import akka.stream.{ Graph, OverflowStrategy, SinkShape }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.Timeout

import spray.json._

import ch.seidel.kutu.http.JsonSupport
import ch.seidel.kutu.http.EnrichedJson
import scala.concurrent.Await
import akka.actor.OneForOneStrategy
import scala.util.control.NonFatal
import akka.actor.Props
import akka.actor.Terminated

class CompetitionCoordinatorClientActor(wettkampfUUID: String) extends Actor with JsonSupport {
  import akka.pattern.pipe
  import context._
  var wsSend: Map[Option[String],List[ActorRef]] = Map.empty
  var deviceWebsocketRefs: Map[String,ActorRef] = Map.empty
  var pendingKeepAliveAck: Option[Int] = None

  // send keepalive messages to prevent closing the websocket connection
  private case object KeepAlive
  val liveticker = context.system.scheduler.schedule(15.second, 15.second) {
    self ! KeepAlive
  }

  override def preStart(): Unit = {
    println("Starting CompetitionCoordinatorClientActor")
  }

  override def postStop: Unit = {
    liveticker.cancel()
    println("CompetitionCoordinatorClientActor stopped")
    wsSend.values.flatten.foreach(context.stop)
  }

  /**
   * initial stage (unauthenticated + unsubscribed)
   */
  override def receive = {

    case StartDurchgang(wettkampfUUID, durchgang) =>
      val eventDurchgangStarted = DurchgangStarted(wettkampfUUID, durchgang)
      wsSend(Some(durchgang)).foreach(ws => ws ! eventDurchgangStarted)
      
    case UpdateWertung(wertung) =>
      
      
    case Subscribe(ref, deviceId, durchgang) =>
      val durchgangClients = wsSend.getOrElse(durchgang, List.empty) :+ ref
      context.watch(ref)
      wsSend = wsSend + (durchgang -> durchgangClients)
      deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
      ref ! TextMessage("Connection established.")

    // system actions
    case KeepAlive => wsSend.flatMap(_._2).foreach(ws => ws ! TextMessage("KeepAlive"))
    
    case MessageAck(txt) if (txt.equals("keepAlive")) => handleKeepAliveAck

    case StopDevice(deviceId) =>
      val stoppedWebsocket = deviceWebsocketRefs(deviceId)
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (wsSend.isEmpty) handleStop
      
    case Terminated(stoppedWebsocket) =>
      context.unwatch(stoppedWebsocket)
      deviceWebsocketRefs = deviceWebsocketRefs.filter(x => x._2 != stoppedWebsocket)      
      wsSend = wsSend.map{x => (x._1, x._2.filter(_ != stoppedWebsocket))}.filter(x => x._2.nonEmpty)
      if (wsSend.isEmpty) handleStop
      
    case _: Unit => handleStop
    case _ =>
  }

  private def handleStop {
    println("Closing client actor")
    stop(self)
  }

  private def handleKeepAlive {
    wsSend.values.flatten.foreach(_ ! TextMessage("keepAlive"))
    pendingKeepAliveAck = pendingKeepAliveAck.map(_ + 1) match {
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

class ClientActorSupervisor extends Actor {

  var wettkampfCoordinators = Map[String, ActorRef]()
  
  override val supervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      println("Error in client actor", e)
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
    
    case Terminated(wettkampfActor) =>
      context.unwatch(wettkampfActor)
      wettkampfCoordinators = wettkampfCoordinators.filter(_._2 != wettkampfActor)
  }
}

object CompetitionCoordinatorClientActor extends JsonSupport with EnrichedJson {

  import ch.seidel.kutu.http.Core._
  val supervisor = system.actorOf(Props[ClientActorSupervisor])
  
  def props(wettkampfUUID: String) = Props(classOf[CompetitionCoordinatorClientActor], wettkampfUUID)
  
  def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          println(s"WS stream failed with $cause")
        case _ => // ignore regular completion
          println(s"WS stream closed")
      })

  def tryMapText(text: String): KutuAppProtokoll = try {
    text.asType[KutuAppProtokoll]
  } catch {
    case e: Exception => MessageAck(text)
  }

  def websocketFlow: Flow[Message, KutuAppProtokoll, Any] =
    Flow[Message]
      .mapAsync(1) {
        case TextMessage.Strict(text) => Future.successful(tryMapText(text))
        case TextMessage.Streamed(stream) => stream.runFold("")(_ + _).map(tryMapText(_))
        case b: BinaryMessage => throw new Exception("Binary message cannot be handled")
      }.via(reportErrorsFlow)

  def createActorSinkSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String]): Flow[Message, Message, Any] = {
    val clientActor = Await.result(ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)     
    
    val source: Source[Nothing, ActorRef] = Source.actorRef(256, OverflowStrategy.dropNew).mapMaterializedValue { wsSend =>
      clientActor ! Subscribe(wsSend, deviceId, durchgang)
      wsSend
    }

    val sink = websocketFlow.to(Sink.actorRef(clientActor, StopDevice(deviceId)))

    Flow.fromSinkAndSource(sink, source)
  }
}
