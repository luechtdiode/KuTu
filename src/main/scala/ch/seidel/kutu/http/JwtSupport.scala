package ch.seidel.kutu.http

import ch.seidel.jwt
import ch.seidel.jwt.{JsonWebToken, JwtClaimsSet, JwtClaimsSetMap}
import ch.seidel.kutu.Config.*
import ch.seidel.kutu.domain.Wettkampf
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.server.{Directive0, Directive1, Directives}
import org.slf4j.LoggerFactory

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import java.util.{Base64, Date}

trait JwtSupport extends Directives {
  private lazy val userKey = "user"
  private lazy val expiredAtKey = "expiredAtKey"
  private val logger = LoggerFactory.getLogger(this.getClass)

  def authenticated(rejectRequest: Boolean = false): Directive1[String] = {
    optionalHeaderValueByName(jwtAuthorizationKey).flatMap(authenticateWith(_, rejectRequest))
  }

  def authenticatedId: Directive1[Option[String]] = {
    optionalHeaderValueByName(jwtAuthorizationKey)
      .flatMap(jwtOption => provide(jwtOption
        .filter(token => JsonWebToken.validate(token, jwtSecretKey))
        .filter(token => !isTokenExpired(token))
        .flatMap(token => getUserID(getClaims(token))))
      )
  }

  def authenticateWith(jwtOption: Option[String], rejectRequest: Boolean): Directive1[String] = jwtOption match {
    case Some(jwt) if JsonWebToken.validate(jwt, jwtSecretKey) =>
      if isTokenExpired(jwt) then {
        complete(StatusCodes.Unauthorized -> "Token expired.")
      } else {
        getUserID(getClaims(jwt)) match {
          case Some(id) => provide(id)
          case _=> if rejectRequest then reject else complete(StatusCodes.Unauthorized)
        }
      }

    case _ =>
      if rejectRequest then reject else complete(StatusCodes.Unauthorized)
  }

  def respondWithJwtHeader(userId: String): Directive0 = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    respondWithHeader(RawHeader(jwtAuthorizationKey, jwt.JsonWebToken(jwtHeader, claims, jwtSecretKey)))
  }

  def respondWithJwtHeader(wettkampf: Wettkampf): Directive0 = {
    val claims = setClaims(wettkampf.uuid.get, wettkampf.datum)
    val jwt = ch.seidel.jwt.JsonWebToken(jwtHeader, claims, jwtSecretKey)
    respondWithHeader(RawHeader(jwtAuthorizationKey, jwt))
  }

  def createOneTimeResetRegistrationLoginToken(competitionUUID: String, registrationId: Long): String = {
    val token = jwt.JsonWebToken(jwtHeader, setClaims(registrationId.toString, 1), jwtSecretKey)
    new String(Base64.getUrlEncoder.encodeToString(s"registration&c=$competitionUUID&rid=$registrationId&rs=$token".getBytes))
  }

  def setClaims(userid: String, wettkampfDate: Date): JwtClaimsSetMap = {
    val wkStart = Instant.ofEpochMilli(wettkampfDate.getTime).truncatedTo(TimeUnit.DAYS.toChronoUnit)
    val wkEnd = wkStart.plus(Duration.ofDays(1))
    val originalTimeout = Duration.between(Instant.now(), wkEnd)
    val permissionTimeout = if originalTimeout.toDays > 3 || originalTimeout.toDays < jwtTokenExpiryPeriodInDays then {
      Instant.now().plus(Duration.ofDays(jwtTokenExpiryPeriodInDays))
    } else {
      wkEnd
    }

    JwtClaimsSet(Map(
      userKey -> userid,
      expiredAtKey -> permissionTimeout.toEpochMilli
    ))
  }

  def setClaims(userid: String, expiryPeriodInDays: Long): JwtClaimsSetMap = JwtClaimsSet(
    Map(
      userKey -> userid,
      expiredAtKey -> (System.currentTimeMillis() + TimeUnit.DAYS
        .toMillis(expiryPeriodInDays))
    )
  )

  private def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }

  private def getUserID(claims: Option[Map[String, String]]): Option[String] = claims.map(_.get(userKey)).get

  def getExpiration(jwt: String): Option[Date] = getClaims(jwt) match {
    case Some(claims) => claims.get(expiredAtKey) match {
      case Some(value) => Some(new Date(value.toLong))
      case _ => None
    }
    case _ => None
  }

  private def formatDateTime(d: Date) = f"$d%td.$d%tm.$d%tY - $d%tH:$d%tM"

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get(expiredAtKey) match {
        case Some(value) =>
          val ret = value.toLong < System.currentTimeMillis()
          if ret then {
            logger.warn(s"token (${getUserID(Some(claims))}) expired! expiredAt: ${formatDateTime(new Date(value.toLong))}")
          }
          ret
        case None =>
          logger.warn(s"invalid token (${getUserID(Some(claims))}): No claims expiredAtKey found")
          false
      }
    case None =>
      logger.warn("No claims")
      false
  }
}