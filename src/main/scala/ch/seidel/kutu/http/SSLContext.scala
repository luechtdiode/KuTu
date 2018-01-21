package ch.seidel.kutu.http

import java.io.InputStream
import java.security.{ SecureRandom, KeyStore }
import javax.net.ssl.{ SSLContext, TrustManagerFactory, KeyManagerFactory }

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ Route, Directives }
import akka.http.scaladsl.{ ConnectionContext, HttpsConnectionContext, Http }
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import scala.concurrent.ExecutionContextExecutor

trait KuTuSSLContext extends Config {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  private implicit lazy val executionContext = system.dispatcher
    // Manual HTTPS configuration
  
  private val password: Array[Char] = certPw.toCharArray // do not store passwords in code, read them from somewhere safe!
  
  private val ks: KeyStore = KeyStore.getInstance("JKS")
  private val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(httpHostname + ".jks")
  
  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)
  
  private val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)
  
  private val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)
  
  private val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  
  val https: HttpsConnectionContext = ConnectionContext.https(sslContext)
 
}