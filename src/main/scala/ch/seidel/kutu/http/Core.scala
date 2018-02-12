package ch.seidel.kutu.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model._

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.InetAddress
import akka.http.scaladsl.model.headers.RawHeader
import authentikat.jwt.JsonWebToken

object Core extends KuTuSSLContext {
  val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Construct the ActorSystem we will use in our application
   */
  override implicit val system = ActorSystem("KuTuApp")
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  
//  val eventRegistryActor: ActorRef = system.actorOf(EventRegistryActor.props, "eventRegistryActor")
//  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props(eventRegistryActor), "userRegistryActor")
}

trait KuTuAppHTTPServer extends Config with ApiService {
  
  import Core._
  import collection.JavaConverters._
  private implicit val executionContext: ExecutionContext = system.dispatcher
    
  def startServer(userLookup: (String) => String) = {
    //Http().setDefaultServerHttpContext(https)
    val binding = Http().bindAndHandle(allroutes(userLookup), httpInterface, httpPort/*, connectionContext = https*/)
    logger.info("Http-Server started")
    binding
  }
  
  var clientheader = Some(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, setClaims("kutuapp-systemuser", jwtTokenExpiryPeriodInDays), jwtSecretKey)))
  

  def httpClientRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import HttpMethods._
    clientheader match {
      case Some(ch) => Http().singleRequest(HttpRequest(PUT, uri=uri, entity = entity, headers = List(ch)))
      case _ => Http().singleRequest(HttpRequest(PUT, uri=uri, entity = entity))
    }
  }
  
  def httpClientGetRequest(uri: String): Future[HttpResponse] = {
    import HttpMethods._
    clientheader match {
      case Some(ch) => Http().singleRequest(HttpRequest(GET, uri=uri, headers = List(ch)))
      case _ => Http().singleRequest(HttpRequest(GET, uri=uri))
    }
  }
  
  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(shutDown())
  
  def shutDown() {
    system.terminate()
    println("System terminated")
  }
}
