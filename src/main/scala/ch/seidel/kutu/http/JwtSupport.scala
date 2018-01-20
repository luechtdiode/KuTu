package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{ Directives, Directive1, Route }
import authentikat.jwt._
import java.util.concurrent.TimeUnit

trait JwtSupport extends Directives with Config {
  private lazy val userKey = "user"
  private lazy val expiredAtKey = "expiredAtKey"
  private lazy val authorizationKey = "x-access-token"

  def authenticated: Directive1[Long] =
    optionalHeaderValueByName(authorizationKey).flatMap {
      case Some(jwt) if JsonWebToken.validate(jwt, jwtSecretKey) =>
        if (isTokenExpired(jwt)) {
          complete(StatusCodes.Unauthorized -> "Token expired.")
        } else {
          provide(getUserID(getClaims(jwt)))
        }

      case _ => complete(StatusCodes.Unauthorized)
    }

  def setClaims(userid: Long, expiryPeriodInDays: Long) = JwtClaimsSet(
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

  def getUserID(claims: Option[Map[String, String]]) = claims.map(_.get(userKey).map(_.toLong).getOrElse(0L)).getOrElse(0L)

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get(expiredAtKey) match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }
}