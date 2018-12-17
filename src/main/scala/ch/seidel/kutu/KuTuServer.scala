package ch.seidel.kutu

import ch.seidel.kutu.http.{AuthSupport, Core, Hashing, KuTuAppHTTPServer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object KuTuServer extends App with KuTuAppHTTPServer with AuthSupport with Hashing {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  val binding = startServer(user => sha256(user))

  import Core._
  implicit val executionContext: ExecutionContext = system.dispatcher

  override def shutDown(caller: String) {
    if (binding  != null) {
      logger.info(s"$caller: Server stops ...")
      binding.flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete { done =>
        done.failed.map { ex => log.error(ex, "Failed unbinding") }
      }
    }
    super.shutDown("KuTuServer")
  }


  Future {
    logger.info(s"Server started\ntype 'quit' to stop...")
    while (
      StdIn.readLine() match {
        case s: String if (s.endsWith("quit")) =>
          shutDown("KuTuServer")
          false
        case s: String => 
          println(s"command submited: '$s'")
          true
        //      case s =>
        //        println(s"cached unknown comand: '$s'")
        case _ =>
          // on linux, readLine doesn't block
          Thread.sleep(5000)
          true
      }
    ) {}
  }
}
