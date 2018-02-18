package ch.seidel.kutu.http

import akka.util.ByteString
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpHeader$ParsingResult._

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.InetAddress
import authentikat.jwt.JsonWebToken
import scala.concurrent.Await

object Core extends KuTuSSLContext {
//  val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system: ActorSystem = ActorSystem("KuTuApp") // , Config.config
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  
//  val eventRegistryActor: ActorRef = system.actorOf(EventRegistryActor.props, "eventRegistryActor")
//  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props(eventRegistryActor), "userRegistryActor")
  private var terminated = false;
  var serverBinding: Option[Future[Http.ServerBinding]] = None

  def terminate() {
    if(!terminated) {        
      terminated = true
      system.terminate()
    }
  }
}

trait KuTuAppHTTPServer extends Config with ApiService with JsonSupport {
  private val logger = LoggerFactory.getLogger(this.getClass)
  import Core._
  
  def startServer(userLookup: (String) => String) = {
    serverBinding match {
      case None =>
      /**
       * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
       */
      sys.addShutdownHook(shutDown(getClass.getName))
      
      startDB
      
      import collection.JavaConverters._
      val binding = if (hasHttpsConfig) {
        Http().setDefaultServerHttpContext(https)
        val b = Http().bindAndHandle(allroutes(userLookup), Core.httpInterface, Core.httpPort, connectionContext = https)
        logger.info(s"Server online at https://${Core.httpInterface}:${Core.httpPort}/")
        b
      } else {
        val b = Http().bindAndHandle(allroutes(userLookup), Core.httpInterface, Core.httpPort)
        logger.info(s"Server online at http://${Core.httpInterface}:${Core.httpPort}/")
        b
      }
      serverBinding = Some(binding)
      binding
      case Some(binding) => binding
    }
  }
  
  def shutDown(caller: String) {
    serverBinding match {
      case Some(_) =>
        Core.terminate()
        serverBinding = None
        println(caller + " System terminated")
      case _ =>
    }
  }
}
