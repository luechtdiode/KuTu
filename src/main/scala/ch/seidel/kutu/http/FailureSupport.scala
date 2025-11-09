package ch.seidel.kutu.http

import org.apache.pekko.http.scaladsl.model._

import scala.util.Try


trait FailureSupport {
  def responseOrFail[T](in: (Try[HttpResponse], T)): (HttpResponse, T) = in match {
    case (responseTry, context) => (responseTry.get, context)
  }
}