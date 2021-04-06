package gov.cms.bfd.pipeline.dc.geo;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkeletonRDASource implements RDASource<Object> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SkeletonRDASource.class);

  private final Meter callsMeter;

  SkeletonRDASource(MetricRegistry appMetrics) {
    callsMeter = appMetrics.meter(getClass().getSimpleName() + ".calls");
  }

  @Override
  public int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RDASink<Object> sink)
      throws ProcessingException {
    LOGGER.warn("simulating object processing");
    callsMeter.mark();
    return 0;
  }

  @Override
  public void close() throws Exception {
    LOGGER.info("closed");
  }
}
