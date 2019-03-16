package ch.seidel.kutu.http


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import ch.seidel.kutu.Config.{homedir, jwtAuthorizationKey, _}
import ch.seidel.kutu.akka._
import ch.seidel.kutu.domain.Wettkampf
import javafx.beans.property.SimpleObjectProperty
import org.slf4j.LoggerFactory
import scalafx.application.Platform
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import ch.seidel.kutu.domain._

object WebSocketClient extends SprayJsonSupport with JsonSupport with AuthSupport {
  private val logger = LoggerFactory.getLogger(this.getClass)
  import Core.materializer

  private var connectedOutgoingQueue: Option[SourceQueueWithComplete[Message]] = None
  private var connectedIncomingPromise: Option[Promise[Option[Message]]] = None
  val modelWettkampfWertungChanged: SimpleObjectProperty[KutuAppEvent] = new SimpleObjectProperty[KutuAppEvent]()
  var lastSequenceId = Long.MinValue
  var lastWettkampf: Option[Wettkampf] = None

  def connect[T](wettkampf: Wettkampf, messageProcessor: (Option[T], KutuAppEvent)=>Unit, handleError: Throwable=>Unit = println) = {

    def processorWithoutSender: KutuAppEvent=>Unit = {
      case LastResults(results) =>
        results.foreach(processorWithoutSender)

      case event@AthletWertungUpdatedSequenced(_, _, _, _, _, _, sequenceId) =>
        if (sequenceId > lastSequenceId) {
          lastSequenceId = sequenceId
          messageProcessor(None, event)
        }

      case event => messageProcessor(None, event)
    }

    import scala.collection.immutable
    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(
        websocketIncomingFlow(handleError).to(Sink.foreach[KutuAppEvent](processorWithoutSender)),
        websocketOutgoingSource.concatMat(Source.maybe[Message])(Keep.right))(Keep.right)

    val promise = websocketClientRequest(
      if (wettkampf.hasSecred(homedir, remoteHostOrigin)) {
        WebSocketRequest(
          s"$remoteWebSocketUrl/api/competition/ws?clientid=${encodeURIParam(System.getProperty("user.name") + ":" + deviceId)}&lastSequenceId=$lastSequenceId",
          extraHeaders = immutable.Seq(RawHeader(jwtAuthorizationKey, wettkampf.readSecret(homedir, remoteHostOrigin).get)))
      } else if (wettkampf.hasRemote(homedir, remoteHostOrigin)) {
        WebSocketRequest(
          s"$remoteWebSocketUrl/api/durchgang/${wettkampf.uuid.get}/all/ws?clientid=${encodeURIParam(System.getProperty("user.name") + ":" + deviceId)}&lastSequenceId=$lastSequenceId")
      } else {
        throw new IllegalStateException("Competition has no remote-origin")
      },
      flow)
    connectedIncomingPromise = Some(promise)

    lastWettkampf match {
      case Some(wk) if (wk != wettkampf) =>
        lastSequenceId = Long.MinValue
      case _ =>
    }
    lastWettkampf = Some(wettkampf)

    promise.future.onComplete{
      case Success(_) => disconnect
      case Failure(error) => 
        logger.error(s"completed with error: $error")
        disconnect
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
    Platform.runLater {
      modelWettkampfWertungChanged.set(event)
      connectedOutgoingQueue.foreach(_.offer(tryMapEvent(event)))
    }
  }
  
  def reportErrorsFlow[T](handleError: Throwable=>Unit): Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          logger.error(s"WS-Client stream failed with $cause")
          handleError(cause)
        case _ => // ignore regular completion
          logger.info(s"WS-Client stream closed")
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
  
  def websocketIncomingFlow(handleError: Throwable=>Unit): Flow[Message, KutuAppEvent, Any] =
    Flow[Message]
      .mapAsync(1) {
        case TextMessage.Strict(text) => Future.successful(tryMapText(text))
        case TextMessage.Streamed(stream) => stream.runFold("")(_ + _).map(tryMapText(_))
        case b: BinaryMessage => throw new Exception("Binary message cannot be handled")
      }.via(reportErrorsFlow(handleError))
      
  val websocketOutgoingSource = Source.queue[Message](100, OverflowStrategy.dropHead)
    .mapMaterializedValue(queue => connectedOutgoingQueue = Some(queue))
}