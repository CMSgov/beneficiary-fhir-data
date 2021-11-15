package gov.cms.bfd.pipeline.rda.grpc;

import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.sink.FissClaimRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.McsClaimRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * A single combined configuration object to hold the configuration settings for the various
 * components of the RDA load job.
 */
public class RdaLoadOptions implements Serializable {
  private static final long serialVersionUID = 7635897362336183L;

  private final AbstractRdaLoadJob.Config jobConfig;
  private final GrpcRdaSource.Config grpcConfig;
  private final RdaServerJob.Config mockServerConfig;
  private final IdHasher.Config idHasherConfig;

  public RdaLoadOptions(
      AbstractRdaLoadJob.Config jobConfig,
      GrpcRdaSource.Config grpcConfig,
      RdaServerJob.Config mockServerConfig,
      IdHasher.Config idHasherConfig) {
    this.jobConfig = Preconditions.checkNotNull(jobConfig, "jobConfig is a required parameter");
    this.grpcConfig = Preconditions.checkNotNull(grpcConfig, "grpcConfig is a required parameter");
    this.mockServerConfig =
        Preconditions.checkNotNull(mockServerConfig, "mockServerConfig is a required parameter");
    this.idHasherConfig =
        Preconditions.checkNotNull(idHasherConfig, "idHasherConfig is a required parameter");
  }

  /** @return settings for the overall job. */
  public AbstractRdaLoadJob.Config getJobConfig() {
    return jobConfig;
  }

  /** @return settings for the gRPC service caller. */
  public GrpcRdaSource.Config getGrpcConfig() {
    return grpcConfig;
  }

  public Optional<RdaServerJob> createRdaServerJob() {
    if (grpcConfig.getServerType() == GrpcRdaSource.Config.ServerType.InProcess) {
      return Optional.of(new RdaServerJob(mockServerConfig));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param databaseOptions the shared application {@link DatabaseOptions}
   * @param appState the shared {@link PipelineApplicationState}
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaFissClaimLoadJob createFissClaimsLoadJob(PipelineApplicationState appState) {
    return new RdaFissClaimLoadJob(
        jobConfig,
        () ->
            new GrpcRdaSource<>(
                grpcConfig,
                new FissClaimStreamCaller(
                    new FissClaimTransformer(appState.getClock(), new IdHasher(idHasherConfig))),
                appState.getMetrics(),
                "fiss",
                jobConfig.getStartingFissSeqNum()),
        () -> new FissClaimRdaSink(appState),
        appState.getMetrics());
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param databaseOptions connection options for SQL database
   * @param appMetrics MetricRegistry used to track operational metrics
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaMcsClaimLoadJob createMcsClaimsLoadJob(PipelineApplicationState appState) {
    return new RdaMcsClaimLoadJob(
        jobConfig,
        () ->
            new GrpcRdaSource<>(
                grpcConfig,
                new McsClaimStreamCaller(
                    new McsClaimTransformer(appState.getClock(), new IdHasher(idHasherConfig))),
                appState.getMetrics(),
                "mcs",
                jobConfig.getStartingMcsSeqNum()),
        () -> new McsClaimRdaSink(appState),
        appState.getMetrics());
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
