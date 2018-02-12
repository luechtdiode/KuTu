package ch.seidel.kutu

import scala.util.Try
import scala.util.{ Success, Failure }
import scala.io.StdIn
import ch.seidel.kutu.http.KuTuAppHTTPServer
import ch.seidel.kutu.http.Hashing
import scala.concurrent.ExecutionContext
import ch.seidel.kutu.http.Core

object KuTuServer extends App with KuTuAppHTTPServer with Hashing {
  import Core._
  private implicit val executionContext: ExecutionContext = system.dispatcher

  println(sha256("geräteturnen"))
  println("917DA0F80E3C0821063D9A0C04ED9E7F5138C7D8C535C8434C0091503FEDC25E")
  val binding = startServer(user => user)
  
  override def shutDown() {
    println(s"Server stops ...")
    if (binding  != null) {
      binding.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { done =>
        done.failed.map { ex => log.error(ex, "Failed unbinding") }
        super.shutDown()
      }    
    } else {
      super.shutDown()
    }
  }

  println(s"Server online at http://$httpInterface:$httpPort/\ntype 'quit' to stop...")

  while (
    StdIn.readLine() match {
      case s: String if (s.endsWith("quit")) =>
        shutDown()
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
