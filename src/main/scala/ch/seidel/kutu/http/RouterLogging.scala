package ch.seidel.kutu.http

import akka.event.LoggingAdapter
import akka.actor.ActorSystem
import ch.seidel.kutu.http.Core._

trait RouterLogging {
  lazy val log = akka.event.Logging(system, classOf[RouterLogging])
}