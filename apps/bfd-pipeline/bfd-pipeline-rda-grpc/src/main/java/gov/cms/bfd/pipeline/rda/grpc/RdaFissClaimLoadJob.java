package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PipelineJob requires that the class of the job be used to define the PipelineJobType. This class
 * is a simple wrapper to ensure that PreAdjFissClaim processing has a unique PipelineJobType value
 * based on its class.
 */
public class RdaFissClaimLoadJob extends AbstractRdaLoadJob<RdaChange<PreAdjFissClaim>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaFissClaimLoadJob.class);

  public RdaFissClaimLoadJob(
      Config config,
      Callable<RdaSource<RdaChange<PreAdjFissClaim>>> sourceFactory,
      Callable<RdaSink<RdaChange<PreAdjFissClaim>>> sinkFactory,
      MetricRegistry appMetrics) {
    super(config, sourceFactory, sinkFactory, appMetrics, LOGGER);
  }
}
