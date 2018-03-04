package ch.seidel.kutu

import com.typesafe.config.ConfigFactory
import authentikat.jwt.JwtHeader
import java.util.UUID
import scala.util.Random
import java.io.File
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.LinkOption

object Config {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  val configPath = System.getProperty("user.dir")
  logger.info("Path where custom configurations (kutuapp.conf) are taken from:", configPath)
  val userHomePath = System.getProperty("user.home")
  logger.info("Path where db is taken from:", userHomePath)
  val userConfig = new File(configPath + "/kutuapp.conf")
  val config = if (userConfig.exists()) ConfigFactory.parseFile(new File(configPath + "/kutuapp.conf")).withFallback(ConfigFactory.load()) else ConfigFactory.load()

  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")

  def saveSecret(secret: String) {
    val path = new File(userHomePath + "/kutuapp/.jwt").toPath
    Files.newOutputStream(path, StandardOpenOption.CREATE_NEW).write(secret.getBytes("utf-8"))
    if (System.getProperty("os.name").toLowerCase.indexOf("win") > -1) {
      Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
    }
    logger.info("Secret new createt " + path)
  }
  
  def readSecret: Option[String] = {
    val path = new File(userHomePath + "/kutuapp/.jwt").toPath
    if (path.toFile.exists) {
      logger.info("Secret found " + path)
      Some(new String(Files.readAllBytes(path), "utf-8"))
    }
    else {
      logger.info("No secret found")
      None
    }
  }
  
  lazy val jwtSecretKey = {
    readSecret match {
      case Some(secret) => secret
      case None =>
        Random.setSeed(Random.nextLong() / System.currentTimeMillis())  
        val secret = UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString + UUID.randomUUID().toString
        saveSecret(secret)
        secret
    }
  }
  val jwtAuthorizationKey = "x-access-token"
  
  lazy val homedir = if(new File("./data").exists()) {
    "./data"
  }
  else if(new File(System.getProperty("user.home") + "/kutuapp/data").exists()) {
    System.getProperty("user.home") + "/kutuapp/data"
  }
  else {
    val f = new File(System.getProperty("user.home") + "/kutuapp/data")
    f.mkdirs();
    System.getProperty("user.home") + "/kutuapp/data"
  }
  
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
