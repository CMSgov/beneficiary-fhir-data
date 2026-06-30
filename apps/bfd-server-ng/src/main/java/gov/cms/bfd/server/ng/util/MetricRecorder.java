package gov.cms.bfd.server.ng.util;

import io.micrometer.core.instrument.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Utility class used for metric timing recordings with dynamic tags. */
@Component
@AllArgsConstructor
public class MetricRecorder {

  /** Metric tag name for the claim type being queried. */
  public static final String CLAIM_TYPE = "claim_type";

  /** Metric tag name for the samhsa filter mode used. */
  public static final String SAMHSA_FILTER_MODE = "samhsa_filter_mode";

  /** Metric tag name indicating if an operation was successful. */
  public static final String PATIENT_MATCH_OUTCOME = "outcome";

  /** Metric tag name indicating whether a coverage query included Part C. */
  public static final String HAS_PART_C = "hasPartC";

  /** Metric tag name indicating whether a coverage query included Part D. */
  public static final String HAS_PART_D = "hasPartD";

  /** Metric tag name indicating whether a coverage query included LIS. */
  public static final String HAS_LIS = "hasLis";

  /** Metric tag name for the request endpoint. */
  public static final String ENDPOINT = "endpoint";

  /** Metric tag name for the certificate alias which is used to indicate a client. */
  public static final String CLIENT = "certificate_alias";

  /** Metric tag name for the response status code. */
  public static final String RESPONSE_STATUS = "response_status";

  /** Metric name for the request latency per partner. */
  public static final String REQUEST_LATENCY_BY_PARTNER_METRIC = "http-requests.latency.by-partner";

  /** Metric name for the overall aggregated latency. */
  public static final String OVERALL_REQUEST_LATENCY_METRIC = "http-requests.latency.all";

  /** Metric name for the request count per partner. */
  public static final String REQUEST_COUNT_PER_PARTNER_METRIC = "http-requests.count.by-partner";

  /** Metric name for the overall request counts per endpoint. */
  public static final String OVERALL_REQUEST_COUNT_PER_ENDPOINT_METRIC = "http-requests.count.all";

  /** Metric name for the overall request count per endpoint per partner. */
  public static final String REQUEST_COUNT_PER_ENDPOINT_PER_PARTNER_METRIC =
      "http-requests.count.by-partner-by-endpoint";

  /** Metric name for the amount of 4xx responses. */
  public static final String RESPONSES_4XX_METRIC = "http-requests.count.4xx-responses";

  /** Metric name for the amount of 5xx responses. */
  public static final String RESPONSES_5XX_METRIC = "http-requests.count.5xx-responses";

  /** Percentiles that can be published for a timer metric. */
  private static final double[] PERCENTILES_TO_REPORT = {0.99};

  private final MeterRegistry meterRegistry;

  /**
   * Creates and increments a counter metric by one.
   *
   * @param metricName the metric name
   * @param tags the tags to dimension the metric by
   */
  public void incrementCounter(String metricName, String... tags) {
    meterRegistry.counter(metricName, tags).increment();
  }

  /**
   * Records a duration in a timer metric.
   *
   * @param metricName the metric name
   * @param duration the recorded duration of an operation
   * @param unit the timed unit
   * @param tags the tags to dimension the metric by
   */
  public void recordDuration(String metricName, long duration, TimeUnit unit, String... tags) {
    Timer.builder(metricName)
        .tags(tags)
        .publishPercentiles(PERCENTILES_TO_REPORT)
        .register(meterRegistry)
        .record(duration, unit);
  }

  /**
   * Records a distribution value from an operation.
   *
   * @param metricName the metric name
   * @param measurement the measurement to record
   * @param tags the tags to dimension the metric by
   */
  public void recordDistribution(String metricName, int measurement, String... tags) {
    DistributionSummary.builder(metricName).tags(tags).register(meterRegistry).record(measurement);
  }

  /**
   * Executes the provided asynchronous operation while recording a timer metric. Tags are extracted
   * from the operation using the tagsSupplier.
   *
   * @param metricName the metric name
   * @param tagsSupplier supplied tags to attach when the operation completes
   * @param supplier the operation to execute and measure
   * @param <T> the type of the operation result
   * @return the result
   */
  public <T> CompletableFuture<T> recordMetricAsync(
      String metricName,
      Supplier<Iterable<Tag>> tagsSupplier,
      Supplier<CompletableFuture<T>> supplier) {

    var timer = Timer.start(meterRegistry);

    try {
      return supplier.get().whenComplete((_, _) -> stop(metricName, timer, tagsSupplier.get()));
    } catch (RuntimeException e) {
      var tags = Tags.of("exception_occurred", "true");
      stop(metricName, timer, tags);
      throw e;
    }
  }

  /**
   * Executes the provided operation while recording a timer metric. Tags are extracted from the
   * operation result using the tagsFunction.
   *
   * @param metricName the metric name
   * @param supplier the operation to execute and measure
   * @param tagsFunction the function that extracts the tags from the result
   * @param <T> the type of the operation result
   * @return the result
   */
  public <T> T recordMetric(
      String metricName, Supplier<T> supplier, Function<T, Iterable<Tag>> tagsFunction) {

    var timer = Timer.start(meterRegistry);

    try {
      var result = supplier.get();
      stop(metricName, timer, tagsFunction.apply(result));
      return result;
    } catch (RuntimeException e) {
      var tags = Tags.of("exception_occurred", "true");
      stop(metricName, timer, tags);
      throw e;
    }
  }

  private void stop(String metricName, Timer.Sample timer, Iterable<Tag> tags) {
    timer.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
  }
}
