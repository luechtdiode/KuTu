package ch.seidel.kutu.http


import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.Files
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Promise
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import java.util.concurrent.TimeUnit
import java.util.UUID

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.EnhancedFromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.actor.ActorRef

import akka.stream.scaladsl.StreamConverters
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.OverflowStrategy

import spray.json._
import authentikat.jwt.JsonWebToken

import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.Wettkampf
import ch.seidel.kutu.domain.WettkampfService
import ch.seidel.kutu.akka._
import ch.seidel.kutu.Config._

object WebSocketClient extends SprayJsonSupport with JsonSupport {
  import DefaultJsonProtocol._
  import Core.system
  import Core.materializer

 // FIXME implement real ws consumer instead of println
  def connect(wettkampf: Wettkampf, messageProcessor: Message=>Unit = println) = {
    import Core.system
    import Core.materializer
    import scala.collection.immutable
    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(
        Sink.foreach[Message](messageProcessor),
        Source.maybe[Message])(Keep.right)
    
    val (upgradeResponse, promise) = Http().singleWebSocketRequest(
        WebSocketRequest(
            // FIXME take url from config, choose dynamic from ws/wss
            "ws://localhost:5757/api/competition/ws", 
            extraHeaders = immutable.Seq(RawHeader(jwtAuthorizationKey, wettkampf.readSecret(homedir).get))),
        flow)
    
    // FIXME close WS at some later time we want to disconnect
    promise//.success(None)    
  }
  
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

//  def createActorSinkSource(deviceId: String, wettkampfUUID: String, durchgang: Option[String]): Flow[Message, Message, Any] = {
//    val clientActor = Await.result(ask(supervisor, CreateClient(deviceId, wettkampfUUID))(5000 milli).mapTo[ActorRef], 5000 milli)     
//    
//    val source: Source[Nothing, ActorRef] = Source.actorRef(256, OverflowStrategy.dropNew).mapMaterializedValue { wsSend =>
//      clientActor ! Subscribe(wsSend, deviceId, durchgang)
//      wsSend
//    }
//
//    val sink = websocketFlow.to(Sink.actorRef(clientActor, StopDevice(deviceId)))
//
//    Flow.fromSinkAndSource(sink, source)
//  }  
}