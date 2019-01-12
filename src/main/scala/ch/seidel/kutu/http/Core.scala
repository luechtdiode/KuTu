package ch.seidel.kutu.http

import java.net.{DatagramSocket, InetAddress, NetworkInterface}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config._
import ch.seidel.kutu.domain.DBService
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

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

trait KuTuAppHTTPServer extends ApiService with JsonSupport {
  private val logger = LoggerFactory.getLogger(this.getClass)
  import Core._

  
  def startServer(userLookup: (String) => String) = {
    serverBinding match {
      case None =>
      /**
       * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
       */
      sys.addShutdownHook(shutDown(getClass.getName))
      
      DBService.startDB()
      val binding = if (hasHttpsConfig) {
        Http().setDefaultServerHttpContext(https)
        val b = Http().bindAndHandle(allroutes(userLookup), httpInterface, httpPort, connectionContext = https)
        logger.info(s"Server online at https://${httpInterface}:${httpPort}/")
        b
      } else {
        val b = Http().bindAndHandle(allroutes(userLookup), httpInterface, httpPort)
        logger.info(s"Server online at http://${httpInterface}:${httpPort}/")
        b
      }
      serverBinding = Some(binding)
      binding
      case Some(binding) => binding
    }
  }

  def stopServer(caller: String): Unit = {
    implicit val executionContext: ExecutionContext = system.dispatcher

    serverBinding match {
      case Some(binding) =>
        binding.flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete { done =>
          done.failed.map { ex => log.error(ex, "Failed unbinding") }
        }

        serverBinding = None
        println(caller + " Server stopped")
      case _ =>
    }
  }

  def shutDown(caller: String) {
    Core.terminate()
    stopServer(caller)
    println(caller + " System terminated")
  }

  def listNetworkAdresses = {
    import collection.JavaConverters._
    val dgs = new DatagramSocket()

    def mapToInteraceInfo(n: InetAddress) = {
      val url = if (n.getHostAddress.contains(":")) {
        s"${Config.remoteSchema}://[${n.getHostAddress}]:${Config.httpPort}"
      } else {
        s"${Config.remoteSchema}://${n.getHostAddress}:${Config.httpPort}"
      }
      val status = if (n.isReachable(2)) {
        val test = try {
          val value = s"$url/"
          Await.result(httpGetClientRequest(value), Duration.Inf).status.intValue()
        } catch {
          case _: Exception => 500
        }
        (url, test)
      } else {
        (url, 500)
      }
      (n, status._1, status._2)
    }

    try {
      dgs.connect(InetAddress.getByAddress(Array[Byte](1, 1, 1, 1)), 0)
      val networkInterface = NetworkInterface
        .getByInetAddress(dgs.getLocalAddress)
      val internetAccessAdresses = if (networkInterface == null) List.empty else networkInterface
        .getInetAddresses.asIterator().asScala.map(mapToInteraceInfo)
        .filter{
          case (_, _, 200) => true
          case _ => false
        }
        .map(_._2)
//        .toList.mkString("\n"))

      if (internetAccessAdresses.nonEmpty) {
        internetAccessAdresses
      } else {
        NetworkInterface
          .getNetworkInterfaces.asIterator().asScala
          .flatMap{_.getInetAddresses.asIterator().asScala.map(mapToInteraceInfo)}
          .filter{
            case (_, _, 200) => true
            case _ => false
          }
          .map(_._2)

      }
    } finally {
      dgs.close
    }

  }
}
