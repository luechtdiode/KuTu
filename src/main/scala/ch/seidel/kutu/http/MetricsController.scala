package ch.seidel.kutu.http

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import ch.seidel.kutu.Config
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import fr.davit.akka.http.metrics.prometheus.{Buckets, PrometheusRegistry, PrometheusSettings, Quantiles}
import io.prometheus.client.{Collector, CollectorRegistry}


trait MetricsController {

  import MetricsController._

  val metricsroute: Route = (get & path("metrics")) (metrics(registry))
}

object MetricsController {
  private val settings: PrometheusSettings = PrometheusSettings
    .default
    .withNamespace(Collector.sanitizeMetricName(Config.metricsNamespaceName))
    .withIncludePathDimension(true)
    .withIncludeMethodDimension(true)
    .withIncludeStatusDimension(true)
    //.withDurationConfig(Buckets(1, 2, 3, 5, 8, 13, 21, 34))
    .withDurationConfig(PrometheusSettings.DurationBuckets)
    //.withReceivedBytesConfig(Quantiles(0.5, 0.75, 0.9, 0.95, 0.99))
    .withReceivedBytesConfig(PrometheusSettings.DefaultQuantiles)
    .withSentBytesConfig(PrometheusSettings.DefaultQuantiles)
    .withDefineError(_.status.isFailure)

  private val collector: CollectorRegistry = CollectorRegistry.defaultRegistry

  val registry: PrometheusRegistry = PrometheusRegistry(collector, settings)
}