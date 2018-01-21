package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{ Directives, Directive1, Route }
import authentikat.jwt._
import java.util.concurrent.TimeUnit
import akka.http.scaladsl.server.directives.Credentials

trait BasicAuthSupport extends Directives with Config with Hashing {

  def userPassAuthenticator(userLookup: (String) => String): AuthenticatorPF[String] = {
    case p @ Credentials.Provided(id) if p.verify(userLookup(id)/*, sha256*/) => id
  }
  
}