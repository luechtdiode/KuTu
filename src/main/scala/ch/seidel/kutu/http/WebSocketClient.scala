package ch.seidel.kutu.http


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure

import spray.json._

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import ch.seidel.kutu.Config.homedir
import ch.seidel.kutu.Config.jwtAuthorizationKey
import ch.seidel.kutu.akka.KutuAppEvent
import ch.seidel.kutu.akka.MessageAck
import ch.seidel.kutu.domain.Wettkampf
import ch.seidel.kutu.Config._
import akka.stream.OverflowStrategy
import ch.seidel.kutu.akka.KutuAppEvent
import akka.stream.scaladsl.SourceQueueWithComplete

object WebSocketClient extends SprayJsonSupport with JsonSupport {
  import Core.materializer
  import Core.system

  private var connectedOutgoingQueue: Option[SourceQueueWithComplete[Message]] = None
  private var connectedIncomingPromise: Option[Promise[Option[Message]]] = None
  
  def connect(wettkampf: Wettkampf, messageProcessor: KutuAppEvent=>Unit = println) = {
    import scala.collection.immutable
    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(
        websocketIncomingFlow.to(Sink.foreach[KutuAppEvent](messageProcessor)),
        websocketOutgoingSource.concatMat(Source.maybe[Message])(Keep.right))(Keep.right)

    val (upgradeResponse, promise) = Http().singleWebSocketRequest(
        WebSocketRequest(
            s"$remoteWebSocketUrl/api/competition/ws", 
            extraHeaders = immutable.Seq(RawHeader(jwtAuthorizationKey, wettkampf.readSecret(homedir, remoteHostOrigin).get))),
        flow)
    connectedIncomingPromise = Some(promise)
    promise.future.onComplete{
      case _ => disconnect
    }
    promise // return promise to close the ws-connection at some point later    
  }
  
  def disconnect = {
    connectedOutgoingQueue.foreach(_.complete())
    connectedOutgoingQueue = None
    connectedIncomingPromise.foreach(p => try {p.success(None)} catch {case e: Exception => })
    connectedIncomingPromise = None
  }
  
  def isConnected = connectedIncomingPromise.nonEmpty
  
  def publish(event: KutuAppEvent) {
    connectedOutgoingQueue.foreach(_.offer(tryMapEvent(event)))
  }
  
  def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          println(s"WS-Client stream failed with $cause")
        case _ => // ignore regular completion
          println(s"WS-Client stream closed")
      })

  def tryMapText(text: String): KutuAppEvent = try {
    text.asType[KutuAppEvent]
  } catch {
    case e: Exception => MessageAck(text)
  }
  def tryMapEvent(event: KutuAppEvent): TextMessage = try {
    TextMessage(event.toJson.compactPrint) 
  } catch {
    case e: Exception => TextMessage(event.toString)
  }
  
  def websocketIncomingFlow: Flow[Message, KutuAppEvent, Any] =
    Flow[Message]
      .mapAsync(1) {
        case TextMessage.Strict(text) => Future.successful(tryMapText(text))
        case TextMessage.Streamed(stream) => stream.runFold("")(_ + _).map(tryMapText(_))
        case b: BinaryMessage => throw new Exception("Binary message cannot be handled")
      }.via(reportErrorsFlow)
      
  val websocketOutgoingSource = Source.queue[Message](10, OverflowStrategy.dropHead)
    .mapMaterializedValue(queue => connectedOutgoingQueue = Some(queue))
}