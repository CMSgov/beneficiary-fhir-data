package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer gauges are created using an implementation of an interface that contains a single
 * method to read the current value for the gauge. This class provides a way to retain the
 * underlying value store (an {@link java.util.concurrent.atomic.AtomicLong}) and provide access to
 * its setter method on demand using a unique name.
 */
public class NumericGauges {
  /** The values for the gauges. */
  private final Map<String, AtomicLong> gaugeValues = Collections.synchronizedMap(new HashMap<>());

  /**
   * Looks up the {@link AtomicLong} for the given metric name. Creates and returns a new one if
   * none currently exists. Repeated calls with the same name will return the same object.
   *
   * @param gaugeName unique name used to identify the gauge metric in {@link MeterRegistry}
   * @return the {@link AtomicLong} holding the gauge value
   */
  public AtomicLong getValueForName(String gaugeName) {
    return gaugeValues.computeIfAbsent(gaugeName, key -> new AtomicLong());
  }

  /**
   * Obtains the {@link Gauge} object for the given name from the {@link MeterRegistry}. Creates a
   * new one if none is currently registered using an {@link AtomicLong} registered with this
   * object. Multiple calls with a given registry and name will return the same gauge and calling
   * {@link NumericGauges#getValueForName} with the same name will always return the underlying
   * object for the gauge.
   *
   * @param appMetrics registry to store the gauge itself
   * @param gaugeName name used to identify the gauge in the registry and the value store in this
   * @return a gauge registered with the registry
   */
  public AtomicLong getGaugeForName(MeterRegistry appMetrics, String gaugeName) {
    return appMetrics.gauge(gaugeName, getValueForName(gaugeName));
  }
}
