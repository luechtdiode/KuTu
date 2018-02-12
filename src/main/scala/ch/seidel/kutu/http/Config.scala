package ch.seidel.kutu.http

import com.typesafe.config.ConfigFactory
import authentikat.jwt.JwtHeader
import java.util.UUID
import scala.util.Random

object Config {
  Random.setSeed(Random.nextLong() / System.currentTimeMillis())
  val jwtSecretKey = UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString
}

trait Config {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")
  
  val httpInterface = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")
  val httpHostname = httpConfig.getString("hostname")
  val certPw = httpConfig.getString("certPw")
  
  val jwtAuthorizationKey = "x-access-token"
  val jwtTokenExpiryPeriodInDays = jwtConfig.getInt("tokenExpiryPeriodInDays")
  val jwtSecretKey = Config.jwtSecretKey
  val jwtHeader = JwtHeader(jwtConfig.getString("algorithm"), jwtConfig.getString("contenttype"))
  
  val remoteHost = appRemoteConfig.getString("hostname")
  val remotePort = appRemoteConfig.getInt("port")
}
