package ch.seidel.kutu.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{ Directives, Directive1, Route }
import authentikat.jwt._
import java.util.concurrent.TimeUnit
import akka.http.scaladsl.server.directives.Credentials
import ch.seidel.kutu.Config._

trait JwtSupport extends Directives {
  private lazy val userKey = "user"
  private lazy val expiredAtKey = "expiredAtKey"
  
  def authenticated(rejectRequest: Boolean = false): Directive1[String] = {
    optionalHeaderValueByName(jwtAuthorizationKey).flatMap(authenticateWith(_, rejectRequest))
  }
  
  def authenticateWith(jwt: Option[String], rejectRequest: Boolean): Directive1[String] = jwt match {
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

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get(expiredAtKey) match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }
}