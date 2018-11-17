package ch.seidel.kutu.http

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import ch.seidel.kutu.Config._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

trait KuTuSSLContext {

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