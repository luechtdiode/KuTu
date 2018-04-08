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
import java.util.Properties
import com.github.markusbernhardt.proxy.ProxySearch
import java.net.ProxySelector
import java.net.URI
import java.net.Proxy
import java.util.Collections
import scala.collection.JavaConverters

object Config {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("OS-Name: " + System.getProperty("os.name"))
  
  val configPath = System.getProperty("user.dir")
  logger.info(s"user.dir Path where custom configurations (kutuapp.conf) are taken from: ${new File(configPath).getAbsolutePath}")
  val userHomePath = System.getProperty("user.home") + "/kutuapp"
  logger.info(s"user.home Path: ${new File(userHomePath).getAbsolutePath}")
  val userConfig = new File(configPath + "/kutuapp.conf")
  val config = if (userConfig.exists()) ConfigFactory.parseFile(new File(configPath + "/kutuapp.conf")).withFallback(ConfigFactory.load()) else ConfigFactory.load()

  val appVersion = if (config.hasPath("app.majorversion")) config.getString("app.majorversion") else "dev.dev.test"
  val builddate = if (config.hasPath("app.builddate")) config.getString("app.builddate") else "today"
    
  logger.info(s"App-Version: $appVersion")
    
  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")

  def saveSecret(secret: String) {
    val path = new File(userHomePath + "/.jwt").toPath
    val fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
    try {
      fos.write(secret.getBytes("utf-8"))
    } finally {
      fos.close
    }
    if (System.getProperty("os.name").toLowerCase.indexOf("win") > -1) {
      Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
    }
    logger.info("Secret new createt " + path)
  }
  
  def readSecret: Option[String] = {
    val path = new File(userHomePath + "/.jwt").toPath
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
  else if(new File(userHomePath + "/data").exists()) {
    userHomePath + "/data"
  }
  else {
    val f = new File(userHomePath + "/data")
    f.mkdirs();
    userHomePath + "/data"
  }
  
  // Use the static factory method getDefaultProxySearch to create a proxy search instance 
  // configured with the default proxy search strategies for the current environment.
  val proxySearch = ProxySearch.getDefaultProxySearch()
  val proxySelector = proxySearch.getProxySelector()
  ProxySelector.setDefault(proxySelector)

//  private val proxy = Proxy.NO_PROXY
  
  // Get list of proxies from default ProxySelector available for given URL
  private val proxies: List[Proxy] = if (ProxySelector.getDefault() != null) {
      JavaConverters.asScalaIterator(ProxySelector.getDefault().select(new URI(Config.remoteBaseUrl)).iterator()).toList
  } else { List() }
  
  // Find first proxy for HTTP/S. Any DIRECT proxy in the list returned is only second choice
  val autoconfigProxy = proxies.filter(p => p.`type` match{
    case Proxy.Type.HTTP => true 
    case _ => false
  })
  .map{p => p.address.toString.split(":")}
  .map(a => (a(0), a(a.length-1))).headOption match {
    case Some((proxyConfigIp, proxyConfigPort)) =>
      println(proxyConfigIp, proxyConfigPort)
      (Some(proxyConfigIp), Some(proxyConfigPort))
    case _=> (None, Some("3128"))
  }

  lazy val httpInterface = if (config.hasPath("http.interface"))    config.getString("http.interface")    else "0.0.0.0"
  lazy val httpPort =      if (config.hasPath("http.port"))         config.getInt("http.port")            else 5757
  lazy val httpHostname =  if (config.hasPath("http.hostname"))     config.getString("http.hostname")     else "kutuapp.sharevic.net"
  lazy val certPw =        if (config.hasPath("http.certPw"))       config.getString("http.certPw")       else null
  
  lazy val jwtTokenExpiryPeriodInDays = jwtConfig.getInt("tokenExpiryPeriodInDays")
  lazy val jwtHeader = JwtHeader(jwtConfig.getString("algorithm"), jwtConfig.getString("contenttype"))
  
  lazy val remoteHost =    if (appRemoteConfig.hasPath("hostname")) appRemoteConfig.getString("hostname") else "kutuapp"
  lazy val remoteSchema =  if (appRemoteConfig.hasPath("schema"))   appRemoteConfig.getString("schema")   else "https"
  
  lazy val proxyHost =     if (appRemoteConfig.hasPath("proxyHost")) Some(appRemoteConfig.getString("proxyHost")) else autoconfigProxy._1
  lazy val proxyPort =     if (appRemoteConfig.hasPath("proxyPort")) Some(appRemoteConfig.getString("proxyPort")) else autoconfigProxy._2
  lazy val remoteHostOrigin = remoteHost.split(":")(0)
  
  lazy val remoteBaseUrl = s"$remoteSchema://$remoteHost"
  lazy val remoteOperatingBaseUrl = remoteBaseUrl //s"http://$remoteHost:$remotePort/operating"
  lazy val remoteAdminBaseUrl = remoteBaseUrl//s"$remoteBaseUrl/wkadmin"
  lazy val remoteWebSocketUrl = remoteBaseUrl.replace("http", "ws")
}
