package ch.seidel.kutu

import ch.seidel.commons.IOUtils.withResources
import ch.seidel.jwt.JwtHeader
import ch.seidel.kutu.http.KuTuSSLContext
import com.github.markusbernhardt.proxy.ProxySearch
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import java.io.File
import java.net.{Proxy, ProxySelector, URI}
import java.nio.file.{Files, LinkOption, StandardOpenOption}
import java.security.{NoSuchAlgorithmException, SecureRandom}
import java.util.Collections.emptyList
import java.util.UUID
import java.util.prefs.Preferences
import javax.crypto.KeyGenerator
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.javaapi.CollectionConverters

object Config extends KuTuSSLContext {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("OS-Name: " + System.getProperty("os.name"))

  val configPath: String = System.getProperty("kutuAPPDIR", System.getProperty("user.dir"))
  logger.info(s"user.dir Path where custom configurations (kutuapp.conf) are taken from: ${new File(configPath).getAbsolutePath}")
  val userHomePath: String = System.getProperty("user.home") + "/kutuapp"
  logger.info(s"user.home Path: ${new File(userHomePath).getAbsolutePath}")

  System.setProperty("pekko.persistence.snapshot-store.local.dir", userHomePath + "/snapshot")
  System.setProperty("pekko.persistence.journal.leveldb.dir", userHomePath + "/journal")

  val userConfig: File = new File(configPath + "/kutuapp.conf")
  val config: com.typesafe.config.Config =
    ConfigFactory.systemEnvironment().withFallback(
      if (userConfig.exists())
        ConfigFactory.parseFile(new File(configPath + "/kutuapp.conf")).withFallback(ConfigFactory.load())
      else
        ConfigFactory.load()).resolve()

  val metricsNamespaceName: String = if (config.hasPath("NAMESPACE")) {
    config.getString("NAMESPACE")
  } else {
    "kutuapp"
  }

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

  val logoFileMaxSize = 1024 * 1024
  val mediafileMaxSize = 5 * 1024 * 1024

  val importDataFrom: Option[String] = if(config.hasPath("app.import.data.fromversion")) {
    Some(config.getString("app.import.data.fromversion"))
  } else {
    None
  }

  val bestenlisteSchwellwert: Double = if (config.hasPath("app.bestenlisteSchwellwert")) config.getDouble("app.bestenlisteSchwellwert") else 9.0

  val donationLink: String = if (config.hasPath("X_DONATION_LINK")) config.getString("X_DONATION_LINK") else ""
  val donationPrice: String = if (config.hasPath("X_DONATION_PRICE")) config.getString("X_DONATION_PRICE") else ""
  val donationDonationBegin: String = if (config.hasPath("X_DONATION_BEGIN")) config.getString("X_DONATION_BEGIN") else ""

  private val jwtConfig = config.getConfig("jwt")
  private val appRemoteConfig = config.getConfig("app.remote")

  def saveSecret(secret: String): Unit = {
    val path = new File(userHomePath + "/.jwt").toPath
    withResources(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
      _.write(secret.getBytes("utf-8"))
    }

    if (System.getProperty("os.name").toLowerCase.indexOf("win") > -1) {
      Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
    }
    logger.info("Secret new createt " + path)
  }

  def readSecret: Option[String] = {
    if (config.hasPath("X_KUTU_SECRET") && config.getString("X_KUTU_SECRET").nonEmpty) {
      Some(config.getString("X_KUTU_SECRET"))
    } else {
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
          SecureRandom.getInstanceStrong
        } catch {
          case e: NoSuchAlgorithmException =>
            throw new RuntimeException("No strong secure random available to generate strong AES key", e)
        }
        // already throws IllegalParameterException for wrong key sizes
        kgen.init(256, rng)

        val key = kgen.generateKey().getEncoded

        val toHex: Array[Byte] => String = data => {
          val sb = new mutable.StringBuilder(data.length * 2)
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
    f.mkdirs()
    userHomePath + "/data"
  }


  def saveDeviceId(deviceId: String): Unit = {
    val path = new File(userHomePath + "/.deviceId").toPath
    val fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
    try {
      fos.write(deviceId.getBytes("utf-8"))
    } finally {
      fos.close()
    }
    if (System.getProperty("os.name").toLowerCase.indexOf("win") > -1) {
      Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
    }
    logger.info("DeviceId new created " + path)
  }

  def readDeviceId: Option[String] = {
    val path = new File(userHomePath + "/.deviceId").toPath
    if (path.toFile.exists) {
      logger.info("DeviceId found " + path)
      Some(new String(Files.readAllBytes(path), "utf-8"))
    }
    else {
      logger.info("No DeviceId found")
      None
    }
  }
  lazy val deviceId: String = {
    readDeviceId match {
      case Some(id) => id
      case None =>
        val newDeviceId = UUID.randomUUID().toString
        saveDeviceId(newDeviceId)
        newDeviceId
    }
  }

  // Use the static factory method getDefaultProxySearch to create a proxy search instance
  // configured with the default proxy search strategies for the current environment.
  val proxySearch: ProxySearch = ProxySearch.getDefaultProxySearch
  val proxySelector: ProxySelector = proxySearch.getProxySelector
  ProxySelector.setDefault(proxySelector)

  //  private val proxy = Proxy.NO_PROXY

  // Get list of proxies from default ProxySelector available for given URL
  private val proxies: List[Proxy] = if (ProxySelector.getDefault != null) {
    CollectionConverters.asScala(ProxySelector.getDefault.select(new URI(Config.remoteBaseUrl)).iterator()).toList
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

  lazy val httpInterface: String = if (config.hasPath("http.interface")) config.getString("http.interface") else "0.0.0.0"
  lazy val httpPort: Int = if (config.hasPath("http.port")) config.getInt("http.port") else 5757
  lazy val httpHostname: String = if (config.hasPath("http.hostname")) config.getString("http.hostname") else "localhost"
  lazy val certPw: String = if (config.hasPath("http.certPw")) config.getString("http.certPw") else null

  lazy val jwtTokenExpiryPeriodInDays: Int = jwtConfig.getInt("tokenExpiryPeriodInDays")
  lazy val jwtHeader: JwtHeader = JwtHeader(jwtConfig.getString("algorithm"), jwtConfig.getString("contenttype"))

  val defaultRemoteHost: String = if (appRemoteConfig.hasPath("hostname"))
    appRemoteConfig.getString("hostname")
  else "kutuapp"
  private var _remoteHost: String = defaultRemoteHost
  def getRemoteHosts: List[String] = ((
    if (appRemoteConfig.hasPath("hostnames"))
      appRemoteConfig.getStringList("hostnames")
    else
      emptyList[String]()
    )
    .asScala.toList :+  defaultRemoteHost).toSet.toList.sorted
  def setRemoteHost(host: String): Unit = {
    _remoteHost = host
  }
  def remoteHost = _remoteHost

  def remoteSchema: String = if(_isLocalHostServer) {
    if(hasHttpsConfig) "https" else "http"
  } else if (appRemoteConfig.hasPath("schema")) {
    appRemoteConfig.getString("schema")
  } else "https"

  lazy val proxyHost: Option[String] = if (appRemoteConfig.hasPath("proxyHost")) Some(appRemoteConfig.getString("proxyHost")) else autoconfigProxy._1
  lazy val proxyPort: Option[String] = if (appRemoteConfig.hasPath("proxyPort")) Some(appRemoteConfig.getString("proxyPort")) else autoconfigProxy._2
  def remoteHostOrigin: String = if(_isLocalHostServer) "localhost" else remoteHost.split(":")(0)

  private var _isLocalHostServer = false
  private var _localHostRemoteIP: Option[String] = None
  def setLocalHostServer(value: Boolean, localHostRemoteIP: Option[String]): Unit = {
    _isLocalHostServer = value
    _localHostRemoteIP = localHostRemoteIP
  }
  def isLocalHostServer: Boolean = _isLocalHostServer
  lazy val remoteHostPort: String = if (appRemoteConfig.hasPath("port")) appRemoteConfig.getString("port") else "443"
  def remoteBaseUrl: String = if(_isLocalHostServer)
    if(hasHttpsConfig)s"https://${_localHostRemoteIP.getOrElse(httpHostname)}:$httpPort"
    else s"http://${_localHostRemoteIP.getOrElse(httpHostname)}:$httpPort"
  else s"$remoteSchema://$remoteHost:$remoteHostPort"

  def remoteOperatingBaseUrl: String = remoteBaseUrl //s"http://$remoteHost:$remotePort/operating"
  def remoteAdminBaseUrl: String = remoteBaseUrl//s"$remoteBaseUrl/wkadmin"
  def remoteWebSocketUrl: String = remoteBaseUrl.replace("http", "ws")
}
