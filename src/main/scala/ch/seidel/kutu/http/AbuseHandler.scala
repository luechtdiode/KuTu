package ch.seidel.kutu.http

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import ch.seidel.kutu.Config
import io.prometheus.client
import io.prometheus.client.Collector

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

object AbuseHandler {

  val abusedGauge: client.Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(Collector.sanitizeMetricName(Config.metricsNamespaceName))
    .name("abused_clients")
    .help("Abused client counter")
    .create().register()

  case class AbuseCounter(count: Int, lastSeen: Long)

  private val abuseMap = new AtomicReference(Map[String, AbuseCounter]())

  @tailrec
  def skipElements(path: Path, count: Int): Path =
    if (count < 1 || path.isEmpty || path.tail.isEmpty) path else skipElements(path.tail, count -1)

  def stripPath(path: Path) = s"${skipElements(path, 2).head}/${skipElements(path, 3).head}/../${path.reverse.head}"

  def keyOfClientId(clientId: String, uri: Uri): String = s"${clientId}${stripPath(uri.path)}"

  def handleExceptionAbuse(e: Exception, clientId: String, uri: Uri): String = {
    abuseMap.get().get(keyOfClientId(clientId, uri)) match {
      case Some(AbuseCounter(counter, _)) =>
        toAbuseMap(clientId, uri, counter)
        s"Request from $clientId with $counter fails to $uri could not be handled normally: ${e.toString}"
      case None =>
        toAbuseMap(clientId, uri)
        s"Request from $clientId to $uri could not be handled normally: ${e.toString}"
    }
  }

  def handleAbuse(clientId: String, uri: Uri): String = {
    abuseMap.get.get(keyOfClientId(clientId, uri)) match {
      case Some(AbuseCounter(counter, _)) =>
        toAbuseMap(clientId, uri, counter)
        s"Request from $clientId with $counter fails to $uri could not be found"
      case None =>
        toAbuseMap(clientId, uri)
        s"Request from $clientId with first fail to $uri could not be found"
    }
  }

  def findAbusedClient(clientId: String, uri: Uri): Option[AbuseCounter] = {
    val key = keyOfClientId(clientId, uri)
    val maybeCounter = abuseMap.get.get(key)
    maybeCounter match {
      case None => None
      case acoption@Some(AbuseCounter(counter, lasttime)) =>
        val timeout = System.currentTimeMillis() - lasttime > 24 * 3_600_000 // 24h
        if (counter > 3 && !timeout) {
          acoption
        } else if (timeout) {
          removeInAbuseMap(clientId, uri)
          None
        } else {
          None
        }
    }
  }

  def toAbuseMap(clientId: String, uri: Uri, counter: Int = 0): Unit = {
    val key = keyOfClientId(clientId, uri)
    if (counter == 0) {
      findAbusedClient(clientId, uri) match {
        case Some(ac) =>
          abuseMap.getAndUpdate{_ + (key -> AbuseCounter(ac.count + 1, System.currentTimeMillis()))}
        case None =>
          abuseMap.getAndUpdate{_ + (key -> AbuseCounter(1, System.currentTimeMillis()))}
      }
    } else {
      abuseMap.getAndUpdate{_ + (key -> AbuseCounter(counter + 1, System.currentTimeMillis()))}
    }
    abusedGauge.set(abuseMap.get().size)
  }

  def removeInAbuseMap(clientId: String, uri: Uri): Unit = {
    abuseMap.getAndUpdate{_ - keyOfClientId(clientId, uri)}
    abusedGauge.set(abuseMap.get().size)
  }

  def clearAbusedClients(): Unit = {
    abuseMap.set(Map.empty)
    abusedGauge.set(abuseMap.get().size)
  }

  def getAbusedClients(): Iterable[String] = {
    abuseMap.get().keys
  }
}
