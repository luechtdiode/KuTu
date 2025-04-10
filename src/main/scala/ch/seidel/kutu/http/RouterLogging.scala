package ch.seidel.kutu.http

import ch.seidel.kutu.http.Core._
import org.apache.pekko.event.Logging

trait RouterLogging {
  lazy val log = Logging(system, classOf[RouterLogging])
//  def shortName = {
//
//  }
//  lazy val log = new BusLogging(system.eventStream, "RouterLogging", classOf[RouterLogging], system.asInstanceOf[ExtendedActorSystem].logFilter) with DiagnosticLoggingAdapter
//  object log {
//    def error(s: String): Unit = l.error(s"[$shortName] $s")
//    def error(ex: Throwable, s: String): Unit = l.error(ex, s"[$shortName] $s")
//    def warning(s: String): Unit = l.warning(s"[$shortName] $s")
//    def info(s: String): Unit = l.info(s"[$shortName] $s")
//    def debug(s: String): Unit = l.debug(s"[$shortName] $s")
//  }
}