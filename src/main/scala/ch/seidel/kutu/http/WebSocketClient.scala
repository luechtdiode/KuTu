package ch.seidel.kutu.http


import ch.seidel.kutu.Config.*
import ch.seidel.kutu.actors.*
import ch.seidel.kutu.domain.*
import javafx.beans.property.SimpleObjectProperty
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import org.apache.pekko.stream.{OverflowStrategy, SubscriptionWithCancelException}
import org.slf4j.LoggerFactory
import scalafx.application.Platform
import spray.json.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object WebSocketClient extends SprayJsonSupport with JsonSupport with AuthSupport {
  private val logger = LoggerFactory.getLogger(this.getClass)
  import Core.materializer

  private var connectedOutgoingQueue: Option[SourceQueueWithComplete[Message]] = None
  private var connectedIncomingPromise: Option[Promise[Option[Message]]] = None
  private var mediaPlayerActions: Option[MediaPlayerAction=>Unit] = None
  private var mediaPlayerEvents: Option[MediaPlayerEvent=>Unit] = None
  val modelWettkampfWertungChanged: SimpleObjectProperty[KutuAppEvent] = new SimpleObjectProperty[KutuAppEvent]()
  var lastSequenceId = Long.MinValue
  var lastWettkampf: Option[Wettkampf] = None

  def connect[T](wettkampf: Wettkampf, messageProcessor: (Option[T], KutuAppEvent)=>Unit, handleError: Throwable=>Unit = println) = {

    def processorWithoutSender: KutuAppEvent=>Unit = {
      case LastResults(results) =>
        val relevantResults = results.filter(_.sequenceId > lastSequenceId)
        val sequenceId = relevantResults.foldLeft(lastSequenceId){(accumulator, b) => Math.max(accumulator, b.sequenceId)}
        if sequenceId > lastSequenceId then {
          messageProcessor(None, LastResults(relevantResults))
          lastSequenceId = sequenceId
        }

      case event@AthletWertungUpdatedSequenced(_, _, _, _, _, _, sequenceId) =>
        if sequenceId > lastSequenceId then {
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

    lastWettkampf match {
      case Some(wk) if (wk != wettkampf) =>
        lastSequenceId = Long.MinValue
      case None =>
        lastSequenceId = Long.MinValue
      case _ =>
    }
    lastWettkampf = Some(wettkampf)

    val promise = websocketClientRequest(
      if wettkampf.hasSecred(homedir, remoteHostOrigin) then {
        WebSocketRequest(
          s"$remoteWebSocketUrl/api/competition/ws?clientid=${encodeURIParam(System.getProperty("user.name") + ":" + deviceId)}&lastSequenceId=$lastSequenceId",
          extraHeaders = immutable.Seq(RawHeader(jwtAuthorizationKey, wettkampf.readSecret(homedir, remoteHostOrigin).get)))
      } else if wettkampf.hasRemote(homedir, remoteHostOrigin) then {
        WebSocketRequest(
          s"$remoteWebSocketUrl/api/durchgang/${wettkampf.uuid.get}/all/ws?clientid=${encodeURIParam(System.getProperty("user.name") + ":" + deviceId)}&lastSequenceId=$lastSequenceId")
      } else {
        throw new IllegalStateException("Competition has no remote-origin")
      },
      flow)
    connectedIncomingPromise = Some(promise)

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

  def registerMediaPlayerActionHandler(eventhandler: MediaPlayerAction=>Unit): Unit = {
    mediaPlayerActions = Some(eventhandler)
  }

  def registerMediaPlayerEventHandler(eventhandler: MediaPlayerEvent=>Unit): Unit = {
    mediaPlayerEvents = Some(eventhandler)
  }

  def publishMediaActionLocal(event: MediaPlayerAction): Unit = {
    mediaPlayerActions.foreach(_.apply(event))
  }

  def publishMediaEventLocal(event: MediaPlayerEvent): Unit = {
    mediaPlayerEvents.foreach(_.apply(event))
  }

  def publish(event: KutuAppEvent): Unit = {
    val message = tryMapEvent(event)
    Platform.runLater {
      event match {
        case _: MediaPlayerAction =>
        case _: MediaPlayerEvent =>
        case _: UseMyMediaPlayer =>
        case _: ForgetMyMediaPlayer =>
        case _ => modelWettkampfWertungChanged.set(event)
      }
    }
    connectedOutgoingQueue.foreach(_.offer(message))
  }
  
  def reportErrorsFlow[T](handleError: Throwable=>Unit): Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) => cause match {
          case SubscriptionWithCancelException.StageWasCompleted =>
            logger.info(s"WS-Client stream closed")
          case _ =>
            logger.error(s"WS-Client stream failed with $cause")
            handleError(cause)
        }
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