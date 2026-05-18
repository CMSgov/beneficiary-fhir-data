package gov.cms.bfd.server.ng.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Utility class used for metric timing recordings with dynamic tags. */
@Component
@AllArgsConstructor
public class MetricTimer {

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

  private final MeterRegistry meterRegistry;

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
