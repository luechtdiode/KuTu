package ch.seidel.kutu.http

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}

import org.apache.pekko.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import ch.seidel.kutu.Config._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}


object KuTuSSLContext {
  // Manual HTTPS configuration

  val ks: KeyStore = KeyStore.getInstance("JKS")
  val jksfile = new File(httpHostname + ".jks")
  lazy val keystore: InputStream = new FileInputStream(jksfile);
  lazy val hasHttpsConfig = certPw != null && jksfile.exists()

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

    ConnectionContext.httpsServer(sslContext)
  }
}
trait KuTuSSLContext {

    // Manual HTTPS configuration
  
  lazy val hasHttpsConfig = KuTuSSLContext.hasHttpsConfig
  
  lazy val https = KuTuSSLContext.https
 
}