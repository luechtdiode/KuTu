package ch.seidel.kutu.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import ch.seidel.kutu.Config.{jwtHeader, jwtSecretKey}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.AuthSupport.OPTION_LOGINRESET
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

trait LoginRoutes extends SprayJsonSupport with EnrichedJson with JwtSupport with AuthSupport with RouterLogging with KutuService {
  import AbuseHandler._

  def login(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = pathPrefixLabeled("login", "login") {
    pathEndOrSingleSlash {
      authenticateBasicPF(realm = "secure site", userPassAuthenticator(userLookup, userIdLookup)) { userId =>
        extractUri { uri =>
          respondWithJwtHeader(userId) {
            if (userId.endsWith(OPTION_LOGINRESET)) {
              log.error(handleAbuse(userId, uri))
              complete(StatusCodes.Unauthorized)
            } else {
              complete(StatusCodes.OK)
            }
          }
        }
      } ~ authenticated() { userId =>
        respondWithJwtHeader(userId) {
          complete(StatusCodes.OK)
        }
      }
    }
  }~
  pathPrefixLabeled("loginrenew", "loginrenew") {
    pathEndOrSingleSlash {
      authenticated() { userId =>
        val wettkampf = readWettkampf(userId)
        respondWithJwtHeader(wettkampf) {
          complete(StatusCodes.OK)
        }
      }
    }
  }
}
