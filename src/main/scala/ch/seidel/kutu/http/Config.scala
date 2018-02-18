package ch.seidel.kutu.http

import com.typesafe.config.ConfigFactory
import authentikat.jwt.JwtHeader
import java.util.UUID
import scala.util.Random
import java.io.File
import org.slf4j.LoggerFactory

object Config {
  private val logger = LoggerFactory.getLogger(this.getClass)
  Random.setSeed(Random.nextLong() / System.currentTimeMillis())
  val jwtSecretKey = UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString
  val jwtAuthorizationKey = "x-access-token"
  
  val configPath = System.getProperty("user.dir")
  logger.info("Path where custom configurations (kutuapp.conf) are taken from:", configPath)
  val userHomePath = System.getProperty("user.home")
  logger.info("Path where db is taken from:", userHomePath)
  val userConfig = new File(configPath + "/kutuapp.conf")
  val config = if (userConfig.exists()) ConfigFactory.parseFile(new File(configPath + "/kutuapp.conf")).withFallback(ConfigFactory.load()) else ConfigFactory.load()

  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")
}

trait Config {
  import Config._
  
  lazy val httpInterface = if (config.hasPath("http.interface"))    config.getString("http.interface")    else "0.0.0.0"
  lazy val httpPort =      if (config.hasPath("http.port"))         config.getInt("http.port")            else 5757
  lazy val httpHostname =  if (config.hasPath("http.hostname"))     config.getString("http.hostname")     else "kutuapp"
  lazy val certPw =        if (config.hasPath("http.certPw"))       config.getString("http.certPw")       else null
  
  lazy val jwtTokenExpiryPeriodInDays = jwtConfig.getInt("tokenExpiryPeriodInDays")
  lazy val jwtHeader = JwtHeader(jwtConfig.getString("algorithm"), jwtConfig.getString("contenttype"))
  
  lazy val remoteHost =    if (appRemoteConfig.hasPath("hostname")) appRemoteConfig.getString("hostname") else "kutuapp"
  lazy val remoteSchema =  if (appRemoteConfig.hasPath("schema"))   appRemoteConfig.getString("schema")   else "https"
    
  lazy val remoteBaseUrl = s"$remoteSchema://$remoteHost"
  lazy val remoteOperatingBaseUrl = remoteBaseUrl //s"http://$remoteHost:$remotePort/operating"
  lazy val remoteAdminBaseUrl = remoteBaseUrl//s"$remoteBaseUrl/wkadmin"
}
