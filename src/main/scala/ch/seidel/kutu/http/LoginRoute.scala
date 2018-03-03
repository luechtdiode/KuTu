package ch.seidel.kutu.http

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.{ ActorRef, ActorSystem, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._

import spray.json._

import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.RiegenBuilder
import authentikat.jwt.JsonWebToken
import akka.http.scaladsl.model.headers.RawHeader

trait LoginRoutes extends SprayJsonSupport with EnrichedJson with JwtSupport with BasicAuthSupport with RouterLogging with KutuService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import slick.jdbc.SQLiteProfile
  import slick.jdbc.SQLiteProfile.api._
  
  import DefaultJsonProtocol._

  def login(userLookup: (String) => String) = pathPrefix("login") {
    pathEndOrSingleSlash {
      authenticateBasicPF(realm = "secure site", userPassAuthenticator(userLookup)) { userId =>
        import Config._
        val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
        respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
          complete(StatusCodes.OK)
        }
      }
    }
  }~
  pathPrefix("loginrenew") {
    pathEndOrSingleSlash {
      authenticated { userId =>
        import Config._
        val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
        respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
          complete(StatusCodes.OK)
        }
      }
    }
  }
  
}
