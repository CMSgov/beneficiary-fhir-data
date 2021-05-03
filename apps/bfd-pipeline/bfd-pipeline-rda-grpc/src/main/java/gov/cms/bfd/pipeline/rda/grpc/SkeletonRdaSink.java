package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton RDASink implementation that just updates a Meter to count the number of times it has
 * been called. This implementation satisfies the requirements of DCGEO-18.
 */
public class SkeletonRdaSink<T> implements RdaSink<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SkeletonRdaSource.class);
  private final Meter callsMeter;

  public SkeletonRdaSink(MetricRegistry appMetrics) {
    callsMeter = appMetrics.meter(MetricRegistry.name(getClass().getSimpleName(), "calls"));
  }

  @Override
  public int writeObject(T ignored) throws ProcessingException {
    callsMeter.mark();
    LOGGER.warn("simulating object storage");
    return 1;
  }

  @Override
  public void close() throws Exception {
    LOGGER.info("closed");
  }
}
