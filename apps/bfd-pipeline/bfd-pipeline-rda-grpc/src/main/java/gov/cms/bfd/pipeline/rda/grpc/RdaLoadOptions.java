package gov.cms.bfd.pipeline.rda.grpc;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import java.io.Serializable;
import java.util.Objects;

/**
 * A single combined configuration object to hold the configuration settings for the various
 * components of the RDA load job.
 */
public class RdaLoadOptions implements Serializable {
  private static final long serialVersionUID = 7635897362336183L;

  private final RdaLoadJob.Config jobConfig;
  private final GrpcRdaSource.Config grpcConfig;

  public RdaLoadOptions(RdaLoadJob.Config jobConfig, GrpcRdaSource.Config grpcConfig) {
    this.jobConfig = Preconditions.checkNotNull(jobConfig, "jobConfig is a required parameter");
    this.grpcConfig = Preconditions.checkNotNull(grpcConfig, "grpcConfig is a required parameter");
  }

  /** @return settings for the overall job. */
  public RdaLoadJob.Config getJobConfig() {
    return jobConfig;
  }

  /** @return settings for the gRPC service caller. */
  public GrpcRdaSource.Config getGrpcConfig() {
    return grpcConfig;
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appMetrics MetricRegistry used to track operational metrics
   * @return a DcGeoRDALoadJob instance suitable for use by PipelineManager.
   */
  public PipelineJob<NullPipelineJobArguments> createFissClaimsLoadJob(MetricRegistry appMetrics) {
    return new RdaLoadJob<>(
        jobConfig,
        () -> new GrpcRdaSource<>(grpcConfig, new FissClaimStreamCaller(), appMetrics),
        () -> new SkeletonRdaSink<>(appMetrics),
        appMetrics);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RdaLoadOptions)) {
      return false;
    }
    RdaLoadOptions that = (RdaLoadOptions) o;
    return Objects.equals(jobConfig, that.jobConfig) && Objects.equals(grpcConfig, that.grpcConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobConfig, grpcConfig);
  }
}
