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
    // Manual HTTPS configuration
  
  val ks: KeyStore = KeyStore.getInstance("JKS")
  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(httpHostname + ".jks")
  lazy val hasHttpsConfig = certPw != null && keystore != null
  
  lazy val https: HttpsConnectionContext = {
    require(certPw != null, "Keystore Password required!")
    val password: Array[Char] = certPw.toCharArray // do not store passwords in code, read them from somewhere safe!
      
    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)
    
    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
    
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)
    
    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    
    ConnectionContext.https(sslContext)
  }
 
}