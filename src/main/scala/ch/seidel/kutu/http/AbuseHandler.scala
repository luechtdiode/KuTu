package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.snapshots.PrometheusNaming
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

object AbuseHandler {

  val abusedGauge: Gauge = Gauge
    .builder()
    .name(PrometheusNaming.sanitizeMetricName(Config.metricsNamespaceName + "_abused_clients"))
    .help("Abused client counter")
    .register(MetricsController.registry.underlying)

  val abusedWatchListGauge: Gauge = Gauge
    .builder()
    .name(PrometheusNaming.sanitizeMetricName(Config.metricsNamespaceName + "_abused_watchlist_clients"))
    .help("Abused watchlist client counter")
    .register(MetricsController.registry.underlying)

  case class AbusedClient(ip: String, cid: String, path: String, abused: Boolean) {
    val mapKey: String = s"${ip}@${cid}//${path}"
  }

  case object AbusedClient {

    @tailrec
    private def skipElements(path: Path, count: Int): Path =
      if count < 1 || path.isEmpty || path.tail.isEmpty then path else skipElements(path.tail, count -1)

    private def stripPath(path: Path) = s"${skipElements(path, 2).head}/${skipElements(path, 3).head}/../${path.reverse.head}"

    def apply(clientId: String, uri: Uri): AbusedClient = {
        val client = clientId
        val path = stripPath(uri.path)
        val parts = client.split("@").toList
        val ip = parts.head
        val cid = parts.tail.mkString("@")
        AbusedClient(ip, cid, path, abused = false)
      }
  }
  case class AbuseCounter(client: AbusedClient, count: Int, lastSeen: Long) {
    def isTimedOut: Boolean = System.currentTimeMillis() - lastSeen > 24 * 3_600_000 // 24h
    def isAbused: Boolean = count > 3 && !isTimedOut
    def getClient: AbusedClient = client.copy(abused = isAbused)
  }

  private val abuseMap = new AtomicReference(Map[String, AbuseCounter]())

  syncMetricsCounters()

  def handleExceptionAbuse(e: Exception, clientId: String, uri: Uri): String = {
    val abusedClient = AbusedClient(clientId, uri)
    abuseMap.get().get(abusedClient.mapKey) match {
      case Some(AbuseCounter(_, counter, _)) =>
        addToAbuseMap(abusedClient, counter)
        s"Request from $clientId with $counter fails to $uri could not be handled normally: ${e.toString}"
      case None =>
        addToAbuseMap(abusedClient)
        s"Request from $clientId to $uri could not be handled normally: ${e.toString}"
    }
  }

  def handleAbuse(clientId: String, uri: Uri): String = {
    val abusedClient = AbusedClient(clientId, uri)
    abuseMap.get.get(abusedClient.mapKey) match {
      case Some(AbuseCounter(_, counter, _)) =>
        addToAbuseMap(abusedClient, counter)
        s"Request from $clientId with $counter fails to $uri could not be found"
      case None =>
        addToAbuseMap(abusedClient)
        s"Request from $clientId with first fail to $uri could not be found"
    }
  }

  def findAbusedClient(clientId: String, uri: Uri): Option[AbuseCounter] = {
    findAbusedClient(AbusedClient(clientId, uri))
  }

  private def findAbusedClient(abusedClient: AbusedClient): Option[AbuseCounter] = {
    val maybeCounter = abuseMap.get.get(abusedClient.mapKey)
    maybeCounter match {
      case None =>
        None
      case acOption@Some(abuseCounter) if (abuseCounter.isAbused) =>
        acOption
      case Some(abuseCounter) if (abuseCounter.isTimedOut) =>
        removeInAbuseMap(abuseCounter.client)
      case _ =>
        None
    }
  }

  private def addToAbuseMap(abusedClient: AbusedClient, counter: Int = 0): Unit = {
    if counter == 0 then {
      findAbusedClient(abusedClient) match {
        case Some(ac) =>
          abuseMap.getAndUpdate{_ + (abusedClient.mapKey -> AbuseCounter(ac.getClient, ac.count + 1, System.currentTimeMillis()))}
        case None =>
          abuseMap.getAndUpdate{_ + (abusedClient.mapKey -> AbuseCounter(abusedClient, 1, System.currentTimeMillis()))}
      }
    } else {
      abuseMap.getAndUpdate{_ + (abusedClient.mapKey -> AbuseCounter(abusedClient, counter + 1, System.currentTimeMillis()))}
    }
    syncMetricsCounters()
  }

  private def removeInAbuseMap(abusedClient: AbusedClient): Option[AbuseCounter] = {
    abuseMap.getAndUpdate{_ - abusedClient.mapKey}
    syncMetricsCounters()
    None
  }

  def clearAbusedClients(): Unit = {
    abuseMap.set(Map.empty)
    syncMetricsCounters()
  }

  private def clearTimedOutClients(): Unit = abuseMap.getAndUpdate(_.filter(!_._2.isTimedOut))

  private def syncMetricsCounters(): Unit = {
    clearTimedOutClients()
    abusedGauge.set(getAbusedClientsCount)
    abusedWatchListGauge.set(getAbusedWatchlistClientsCount)
  }

  def getAbusedClients: Iterable[AbusedClient] = {
    abuseMap.get().values
      .filter(!_.isTimedOut)
      .map(_.getClient)
  }

  def getAbusedClientsCount: Int = {
    getAbusedClients.count{counter =>
      counter.abused
    }
  }

  def getAbusedWatchlistClientsCount: Int = {
    getAbusedClients.count{counter =>
      !counter.abused
    }
  }
}
