package ch.seidel.kutu.http

import akka.actor.ExtendedActorSystem
import akka.event.{BusLogging, DiagnosticLoggingAdapter}
import ch.seidel.kutu.http.Core._

trait RouterLogging {
//  lazy val log = akka.event.Logging(system, classOf[RouterLogging])
  lazy val log = new BusLogging(system.eventStream, "RouterLogging", classOf[RouterLogging], system.asInstanceOf[ExtendedActorSystem].logFilter) with DiagnosticLoggingAdapter
}