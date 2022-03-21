package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PipelineJob requires that the class of the job be used to define the PipelineJobType. This class
 * is a simple wrapper to ensure that {@link PartAdjFissClaim} processing has a unique {@link
 * gov.cms.bfd.pipeline.sharedutils.PipelineJobType} value based on its class.
 */
public class RdaFissClaimLoadJob
    extends AbstractRdaLoadJob<FissClaimChange, RdaChange<PartAdjFissClaim>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaFissClaimLoadJob.class);

  public RdaFissClaimLoadJob(
      Config config,
      Callable<RdaSource<FissClaimChange, RdaChange<PartAdjFissClaim>>> sourceFactory,
      Callable<RdaSink<FissClaimChange, RdaChange<PartAdjFissClaim>>> sinkFactory,
      MetricRegistry appMetrics) {
    super(config, sourceFactory, sinkFactory, appMetrics, LOGGER);
  }
}
