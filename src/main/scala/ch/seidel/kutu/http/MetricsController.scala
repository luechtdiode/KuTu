package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.pekko.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import fr.davit.pekko.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}
import io.prometheus.metrics.model.snapshots.PrometheusNaming
import org.apache.pekko.http.scaladsl.server.Directives.{path, _}
import org.apache.pekko.http.scaladsl.server.Route
import io.prometheus.metrics.model.{registry => prometheus}

trait MetricsController {

  import MetricsController._

  val metricsroute: Route = (get & path("metrics")) (metrics(registry))
}

object MetricsController {
  private val settings: PrometheusSettings = PrometheusSettings
    .default
    .withNamespace(PrometheusNaming.sanitizeMetricName(Config.metricsNamespaceName))
    .withIncludePathDimension(true)
    .withIncludeMethodDimension(true)
    .withIncludeStatusDimension(true)
    //.withDurationConfig(Buckets(1, 2, 3, 5, 8, 13, 21, 34))
    .withDurationConfig(PrometheusSettings.DurationBuckets)
    //.withReceivedBytesConfig(Quantiles(0.5, 0.75, 0.9, 0.95, 0.99))
    .withReceivedBytesConfig(PrometheusSettings.DefaultQuantiles)
    .withSentBytesConfig(PrometheusSettings.DefaultQuantiles)
    .withDefineError(_.status.isFailure)

  val registry: PrometheusRegistry = PrometheusRegistry(prometheus.PrometheusRegistry.defaultRegistry, settings)
}