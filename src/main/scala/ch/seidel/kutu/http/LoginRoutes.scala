package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.AuthSupport.OPTION_LOGINRESET

trait LoginRoutes extends SprayJsonSupport with EnrichedJson with JwtSupport with AuthSupport with RouterLogging with KutuService {

  def login(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = pathPrefix("login") {
    pathEndOrSingleSlash {
      authenticateBasicPF(realm = "secure site", userPassAuthenticator(userLookup, userIdLookup)) { userId =>
        respondWithJwtHeader(userId) {
          if(userId.endsWith(OPTION_LOGINRESET)) {
            complete(StatusCodes.Unauthorized)
          } else {
            complete(StatusCodes.OK)
          }
        }
      } ~ authenticated() { userId =>
        respondWithJwtHeader(userId) {
          complete(StatusCodes.OK)
        }
      }
    }
  }~
  pathPrefix("loginrenew") {
    pathEndOrSingleSlash {
      authenticated() { userId =>
        respondWithJwtHeader(userId) {
          complete(StatusCodes.OK)
        }
      }
    }
  }
}
