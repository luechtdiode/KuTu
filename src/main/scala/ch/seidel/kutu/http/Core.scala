package ch.seidel.kutu.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.server.HttpApp

import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Core extends KuTuSSLContext {
  val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Construct the ActorSystem we will use in our application
   */
  override implicit val system = ActorSystem("KuTuApp")
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
//  private implicit val executionContext: ExecutionContext = system.dispatcher
  
//  val eventRegistryActor: ActorRef = system.actorOf(EventRegistryActor.props, "eventRegistryActor")
//  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props(eventRegistryActor), "userRegistryActor")
}

trait KuTuAppHTTPServer extends Config with ApiService {
  
  import Core._
  
  def startServer(userLookup: (String) => String) {
    Http().setDefaultServerHttpContext(https)
    Http().bindAndHandle(allroutes(userLookup), httpInterface, httpPort, connectionContext = https)
    logger.info("Https-Server started")
  }
  
  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(shutDown())
  
  def shutDown() {
    system.terminate()
  }
}
