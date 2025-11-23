package ch.seidel.kutu.http

import ch.seidel.kutu.Config.{jwtHeader, jwtSecretKey}
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.AuthSupport.OPTION_LOGINRESET
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes

trait LoginRoutes extends SprayJsonSupport with EnrichedJson with JwtSupport with AuthSupport with RouterLogging with KutuService {
  import AbuseHandler.*

  def login(userLookup: (String) => String, userIdLookup: (String) => Option[Long]) = pathPrefixLabeled("login", "login") {
    pathEndOrSingleSlash {
      authenticateBasicPF(realm = "secure site", userPassAuthenticator(userLookup, userIdLookup)) { userId =>
        extractUri { uri =>
          respondWithJwtHeader(userId) {
            if userId.endsWith(OPTION_LOGINRESET) then {
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
        if wettkampfExists(userId) then {
          val wettkampf = readWettkampf(userId)
          respondWithJwtHeader(wettkampf) {
            complete(StatusCodes.OK)
          }
        }
        else {
          complete(StatusCodes.NotFound)
        }
      }
    }
  }
}
