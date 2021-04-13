package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton RDASource implementation that just updates a Meter to count the number of times it has
 * been called. This implementation satisfies the requirements of DCGEO-18.
 */
public class SkeletonRDASource implements RDASource<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SkeletonRDASource.class);

  private final Meter callsMeter;

  public SkeletonRDASource(MetricRegistry appMetrics) {
    callsMeter = appMetrics.meter(MetricRegistry.name(getClass().getSimpleName(), "calls"));
  }

  @Override
  public int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RDASink<PreAdjudicatedClaim> sink)
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
