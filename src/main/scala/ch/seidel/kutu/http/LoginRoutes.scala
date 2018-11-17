package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import authentikat.jwt.JsonWebToken
import ch.seidel.kutu.Config._
import ch.seidel.kutu.domain._

trait LoginRoutes extends SprayJsonSupport with EnrichedJson with JwtSupport with AuthSupport with RouterLogging with KutuService {

  def login(userLookup: (String) => String) = pathPrefix("login") {
    pathEndOrSingleSlash {
      authenticateBasicPF(realm = "secure site", userPassAuthenticator(userLookup)) { userId =>
        val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
        respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
          complete(StatusCodes.OK)
        }
      }
    }
  }~
  pathPrefix("loginrenew") {
    pathEndOrSingleSlash {
      authenticated() { userId =>
        val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
        respondWithHeader(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))) {
          complete(StatusCodes.OK)
        }
      }
    }
  }
  
}
