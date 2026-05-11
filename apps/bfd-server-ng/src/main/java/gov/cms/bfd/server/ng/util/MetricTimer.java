package gov.cms.bfd.server.ng.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Utility class used for metric timing recordings with dynamic tags. */
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
  private final Timer.Sample timer;

  /**
   * Creates a metrics timer and starts timing from this point forward until stop.
   *
   * @param meterRegistry the registry used to publish the recorded timer metric.
   */
  public MetricTimer(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.timer = Timer.start(meterRegistry);
  }

  /**
   * Stops the timer and records the metric with the provided name and tags.
   *
   * @param metricName metric name to record
   * @param tags alternating tag keys and values
   */
  public void stop(String metricName, String... tags) {
    timer.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
  }
}
