package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DropWizard gauges are created using an implementation of an interface that contains a single
 * method to read the current value of the gauge. The {@link com.codahale.metrics.MetricRegistry}
 * remembers the actual gauge but not the getter method or the object backing it. This class
 * provides a way to retain the underlying value store (an {@link
 * java.util.concurrent.atomic.AtomicLong}) and provide access to its setter method on demand using
 * a unique name.
 */
public class NumericGauges {
  private final Map<String, AtomicLong> gaugeValues = Collections.synchronizedMap(new HashMap<>());

  /**
   * Looks up the {@link AtomicLong} for the given metric name. Creates an returns a new one if none
   * currently exists. Repeated calls with the same name will return the same object.
   *
   * @param gaugeName unique name used to identify the gauge metric in {@link
   *     com.codahale.metrics.MetricRegistry}
   * @return the {@link AtomicLong} holding the gauge value
   */
  public AtomicLong getValueForName(String gaugeName) {
    return gaugeValues.computeIfAbsent(gaugeName, key -> new AtomicLong());
  }

  /**
   * Obtains the {@link Gauge} object for the given name from the {@link MetricRegistry}. Creates a
   * new one if none is currently registered using an {@link AtomicLong} registered with this
   * object. Multiple calls with a given registry and name will return the same gauge and calling
   * {@link NumericGauges#getValueForName} with the same name will always return the underlying
   * value for the gauge.
   *
   * @param appMetrics registry to store the gauge itself
   * @param gaugeName name used to identify the gauge in the registry and the value store in this
   * @return a gauge registered with the registry
   */
  public Gauge<?> getGaugeForName(MetricRegistry appMetrics, String gaugeName) {
    return appMetrics.gauge(gaugeName, () -> getValueForName(gaugeName)::get);
  }
}
