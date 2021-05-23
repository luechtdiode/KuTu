package ch.seidel.kutu.http

import java.util.Date
import java.util.concurrent.TimeUnit
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive1, Directives}
import ch.seidel.jwt
import ch.seidel.jwt.{JsonWebToken, JwtClaimsSet}
import ch.seidel.kutu.Config._

trait JwtSupport extends Directives {
  private lazy val userKey = "user"
  private lazy val expiredAtKey = "expiredAtKey"
  
  def authenticated(rejectRequest: Boolean = false): Directive1[String] = {
    optionalHeaderValueByName(jwtAuthorizationKey).flatMap(authenticateWith(_, rejectRequest))
  }
  
  def authenticateWith(jwtOption: Option[String], rejectRequest: Boolean): Directive1[String] = jwtOption match {
    case Some(jwt) if JsonWebToken.validate(jwt, jwtSecretKey) =>
      if (isTokenExpired(jwt)) {
        complete(StatusCodes.Unauthorized -> "Token expired.")
      } else {
        getUserID(getClaims(jwt)) match {
          case Some(id) => provide(id)
          case _=> if (rejectRequest) reject else complete(StatusCodes.Unauthorized)
        }
      }

    case _ =>
      if (rejectRequest) reject else complete(StatusCodes.Unauthorized)
  }

  def respondWithJwtHeader(userId: String): akka.http.scaladsl.server.Directive0 = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    respondWithHeader(RawHeader(jwtAuthorizationKey, jwt.JsonWebToken(jwtHeader, claims, jwtSecretKey)))
  }

  def setClaims(userid: String, expiryPeriodInDays: Long) = JwtClaimsSet(
    Map(
      userKey -> userid,
      expiredAtKey -> (System.currentTimeMillis() + TimeUnit.DAYS
        .toMillis(expiryPeriodInDays))
    )
  )

  def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }

  def getUserID(claims: Option[Map[String, String]]): Option[String] = claims.map(_.get(userKey)).get

  def getExpiration(jwt: String): Option[Date] = getClaims(jwt) match {
    case Some(claims) => claims.get(expiredAtKey) match {
      case Some(value) => Some(new Date(value.toLong))
      case _ => None
    }
    case _ => None
  }
  
  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get(expiredAtKey) match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }
}