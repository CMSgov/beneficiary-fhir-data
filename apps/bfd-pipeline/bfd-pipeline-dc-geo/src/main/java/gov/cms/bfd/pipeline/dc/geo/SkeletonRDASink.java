package gov.cms.bfd.pipeline.dc.geo;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkeletonRDASink implements RDASink<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SkeletonRDASource.class);
  private final Meter callsMeter;

  public SkeletonRDASink(MetricRegistry appMetrics) {
    callsMeter = appMetrics.meter(getClass().getSimpleName() + ".calls");
  }

  @Override
  public int writeObject(PreAdjudicatedClaim object) throws ProcessingException {
    callsMeter.mark();
    LOGGER.warn("simulating object storage");
    return 1;
  }

  @Override
  public void close() throws Exception {
    LOGGER.info("closed");
  }
}
