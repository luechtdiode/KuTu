package ch.seidel.kutu

import java.io.File
import java.net.{Proxy, ProxySelector, URI}
import java.nio.file.{Files, LinkOption, StandardOpenOption}
import java.security.{NoSuchAlgorithmException, SecureRandom}

import authentikat.jwt.JwtHeader
import ch.seidel.kutu.http.KuTuSSLContext
import com.github.markusbernhardt.proxy.ProxySearch
import com.typesafe.config.ConfigFactory
import javax.crypto.KeyGenerator
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

object Config extends KuTuSSLContext {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("OS-Name: " + System.getProperty("os.name"))

  val configPath: String = System.getProperty("user.dir")
  logger.info(s"user.dir Path where custom configurations (kutuapp.conf) are taken from: ${new File(configPath).getAbsolutePath}")
  val userHomePath: String = System.getProperty("user.home") + "/kutuapp"
  logger.info(s"user.home Path: ${new File(userHomePath).getAbsolutePath}")

  System.setProperty("akka.persistence.snapshot-store.local.dir", userHomePath + "/snapshot")
  System.setProperty("akka.persistence.journal.leveldb.dir", userHomePath + "/journal")

  val userConfig: File = new File(configPath + "/kutuapp.conf")
  val config: com.typesafe.config.Config = if (userConfig.exists()) ConfigFactory.parseFile(new File(configPath + "/kutuapp.conf")).withFallback(ConfigFactory.load()) else ConfigFactory.load()

  val appVersion: String = if (config.hasPath("app.majorversion")
    && !config.getString("app.majorversion").startsWith("${"))
    config.getString("app.majorversion")
  else "dev.dev.test"
  val appFullVersion: String = if (config.hasPath("app.fullversion")
    && !config.getString("app.fullversion").startsWith("${"))
    config.getString("app.fullversion")
  else "dev.dev.test"
  val builddate: String = if (config.hasPath("app.builddate")
    && !config.getString("app.builddate").startsWith("${"))
    config.getString("app.builddate")
  else "today"

  logger.info(s"App-Version: $appVersion")

  val bestenlisteSchwellwert = if (config.hasPath("app.bestenlisteSchwellwert")) config.getDouble("app.bestenlisteSchwellwert") else 9.0
  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")

  def saveSecret(secret: String): Unit = {
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

  lazy val jwtSecretKey: String = {
    readSecret match {
      case Some(secret) => secret
      case None =>
        val kgen = try {
          KeyGenerator.getInstance("AES")
        } catch {
          case e: NoSuchAlgorithmException =>
            throw new RuntimeException("AES key generator should always be available in a Java runtime", e)
        }
        val rng = try {
          SecureRandom.getInstanceStrong()
        } catch {
          case e: NoSuchAlgorithmException =>
            throw new RuntimeException("No strong secure random available to generate strong AES key", e)
        }
        // already throws IllegalParameterException for wrong key sizes
        kgen.init(256, rng)

        val key = kgen.generateKey().getEncoded

        val toHex: Array[Byte] => String = (data) => {
          val sb = new StringBuilder(data.length * 2)
          for (b <- data) {
            sb.append(f"$b%02X")
          }
          sb.toString()
        }

        val secret = toHex(key)
        saveSecret(secret)
        secret
    }
  }
  val jwtAuthorizationKey: String = "x-access-token"

  lazy val homedir: String = if (new File("./data").exists()) {
    "./data"
  }
  else if (new File(userHomePath + "/data").exists()) {
    userHomePath + "/data"
  }
  else {
    val f = new File(userHomePath + "/data")
    f.mkdirs();
    userHomePath + "/data"
  }

  // Use the static factory method getDefaultProxySearch to create a proxy search instance
  // configured with the default proxy search strategies for the current environment.
  val proxySearch: ProxySearch = ProxySearch.getDefaultProxySearch()
  val proxySelector: ProxySelector = proxySearch.getProxySelector()
  ProxySelector.setDefault(proxySelector)

  //  private val proxy = Proxy.NO_PROXY

  // Get list of proxies from default ProxySelector available for given URL
  private val proxies: List[Proxy] = if (ProxySelector.getDefault() != null) {
    JavaConverters.asScalaIterator(ProxySelector.getDefault().select(new URI(Config.remoteBaseUrl)).iterator()).toList
  } else {
    List()
  }

  // Find first proxy for HTTP/S. Any DIRECT proxy in the list returned is only second choice
  val autoconfigProxy: (Option[String], Some[String]) = proxies.filter(p => p.`type` match {
    case Proxy.Type.HTTP => true
    case _ => false
  })
    .map { p => p.address.toString.split(":") }
    .map(a => (a(0), a(a.length - 1))).headOption match {
    case Some((proxyConfigIp, proxyConfigPort)) =>
      println(proxyConfigIp, proxyConfigPort)
      (Some(proxyConfigIp), Some(proxyConfigPort))
    case _ => (None, Some("3128"))
  }

  lazy val httpInterface = if (config.hasPath("http.interface")) config.getString("http.interface") else "0.0.0.0"
  lazy val httpPort = if (config.hasPath("http.port")) config.getInt("http.port") else 5757
  lazy val httpHostname = if (config.hasPath("http.hostname")) config.getString("http.hostname") else "localhost"
  lazy val certPw = if (config.hasPath("http.certPw")) config.getString("http.certPw") else null

  lazy val jwtTokenExpiryPeriodInDays = jwtConfig.getInt("tokenExpiryPeriodInDays")
  lazy val jwtHeader = JwtHeader(jwtConfig.getString("algorithm"), jwtConfig.getString("contenttype"))

  lazy val remoteHost = if (appRemoteConfig.hasPath("hostname")) appRemoteConfig.getString("hostname") else "kutuapp"
  def remoteSchema = if(_isLocalHostServer) {
    if(hasHttpsConfig) "https" else "http"
  } else if (appRemoteConfig.hasPath("schema")) {
    appRemoteConfig.getString("schema")
  } else "https"

  lazy val proxyHost = if (appRemoteConfig.hasPath("proxyHost")) Some(appRemoteConfig.getString("proxyHost")) else autoconfigProxy._1
  lazy val proxyPort = if (appRemoteConfig.hasPath("proxyPort")) Some(appRemoteConfig.getString("proxyPort")) else autoconfigProxy._2
  def remoteHostOrigin = if(_isLocalHostServer) "localhost" else remoteHost.split(":")(0)

  private var _isLocalHostServer = false
  def setLocalHostServer(value: Boolean): Unit = {
    _isLocalHostServer = value
  }
  def isLocalHostServer() = _isLocalHostServer
  def remoteBaseUrl = if(_isLocalHostServer) if(hasHttpsConfig)s"https://$httpHostname:$httpPort" else s"http://$httpHostname:$httpPort" else s"$remoteSchema://$remoteHost"

  def remoteOperatingBaseUrl = remoteBaseUrl //s"http://$remoteHost:$remotePort/operating"
  def remoteAdminBaseUrl = remoteBaseUrl//s"$remoteBaseUrl/wkadmin"
  def remoteWebSocketUrl = remoteBaseUrl.replace("http", "ws")
}
