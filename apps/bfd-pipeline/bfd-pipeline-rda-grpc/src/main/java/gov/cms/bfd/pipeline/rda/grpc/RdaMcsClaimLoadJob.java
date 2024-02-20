package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PipelineJob requires that the class of the job be used to define the PipelineJobType. This class
 * is a simple wrapper to ensure that {@link RdaMcsClaim} processing has a unique PipelineJobType
 * value based on its class.
 */
public class RdaMcsClaimLoadJob extends AbstractRdaLoadJob<McsClaimChange, RdaChange<RdaMcsClaim>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaMcsClaimLoadJob.class);

  /**
   * Instantiates a new RDA MCS claim load job.
   *
   * @param config the configuration for this job
   * @param preJobTaskFactory the pre job task factory
   * @param sourceFactory the source factory
   * @param sinkFactory the sink factory
   * @param cleanupJob the cleanup job
   * @param appMetrics the app metrics
   */
  public RdaMcsClaimLoadJob(
      Config config,
      Callable<RdaSource<McsClaimChange, RdaChange<RdaMcsClaim>>> preJobTaskFactory,
      Callable<RdaSource<McsClaimChange, RdaChange<RdaMcsClaim>>> sourceFactory,
      ThrowingFunction<
              RdaSink<McsClaimChange, RdaChange<RdaMcsClaim>>, SinkTypePreference, Exception>
          sinkFactory,
      CleanupJob cleanupJob,
      MeterRegistry appMetrics) {
    super(config, preJobTaskFactory, sourceFactory, sinkFactory, cleanupJob, appMetrics, LOGGER);
  }
}
