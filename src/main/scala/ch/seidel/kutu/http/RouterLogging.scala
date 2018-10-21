package ch.seidel.kutu.http

import ch.seidel.kutu.http.Core._

trait RouterLogging {
  lazy val log = akka.event.Logging(system, classOf[RouterLogging])
}