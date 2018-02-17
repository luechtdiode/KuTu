package ch.seidel.kutu

import scala.util.Try
import scala.util.{ Success, Failure }
import scala.io.StdIn
import ch.seidel.kutu.http.KuTuAppHTTPServer
import ch.seidel.kutu.http.Hashing
import scala.concurrent.ExecutionContext
import ch.seidel.kutu.http.Core

object KuTuServer extends App with KuTuAppHTTPServer with Hashing {

  val binding = startServer(user => sha256(user))
  
  override def shutDown(caller: String) {
    import Core._
    implicit val executionContext: ExecutionContext = system.dispatcher
    println(s"$caller: Server stops ...")
    if (binding  != null) {
      binding.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { done =>
        done.failed.map { ex => log.error(ex, "Failed unbinding") }
        super.shutDown("KuTuServer")
      }    
    } else {
      super.shutDown("KuTuServer")
    }
  }

  println(s"Server online at http://$httpInterface:$httpPort/\ntype 'quit' to stop...")

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
