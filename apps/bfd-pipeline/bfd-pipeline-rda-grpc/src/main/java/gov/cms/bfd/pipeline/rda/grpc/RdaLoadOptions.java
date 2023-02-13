package gov.cms.bfd.pipeline.rda.grpc;

import com.google.common.base.Preconditions;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ConcurrentRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.FissClaimRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.McsClaimRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.DLQGrpcRdaSource;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.StandardGrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A single combined configuration object to hold the configuration settings for the various
 * components of the RDA load job.
 */
public class RdaLoadOptions implements Serializable {
  private static final long serialVersionUID = 7635897362336183L;

  private final AbstractRdaLoadJob.Config jobConfig;
  private final RdaSourceConfig rdaSourceConfig;
  private final RdaServerJob.Config mockServerConfig;
  private final IdHasher.Config idHasherConfig;

  public RdaLoadOptions(
      AbstractRdaLoadJob.Config jobConfig,
      RdaSourceConfig rdaSourceConfig,
      RdaServerJob.Config mockServerConfig,
      IdHasher.Config idHasherConfig) {
    this.jobConfig = Preconditions.checkNotNull(jobConfig, "jobConfig is a required parameter");
    this.rdaSourceConfig =
        Preconditions.checkNotNull(rdaSourceConfig, "rdaSourceConfig is a required parameter");
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
  public RdaSourceConfig getRdaSourceConfig() {
    return rdaSourceConfig;
  }

  public Optional<RdaServerJob> createRdaServerJob() {
    if (rdaSourceConfig.getServerType() == RdaSourceConfig.ServerType.InProcess) {
      return Optional.of(new RdaServerJob(mockServerConfig));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaFissClaimLoadJob createFissClaimsLoadJob(PipelineApplicationState appState) {
    Callable<RdaSource<FissClaimChange, RdaChange<RdaFissClaim>>> preJobTaskFactory;

    if (jobConfig.shouldProcessDLQ()) {
      preJobTaskFactory =
          () ->
              new DLQGrpcRdaSource<>(
                  appState.getEntityManagerFactory().createEntityManager(),
                  (seqNumber, fissClaimChange) -> seqNumber == fissClaimChange.getSeq(),
                  rdaSourceConfig,
                  new FissClaimStreamCaller(),
                  appState.getMeters(),
                  "fiss");
    } else {
      preJobTaskFactory = EmptyRdaSource::new;
    }

    return new RdaFissClaimLoadJob(
        jobConfig,
        preJobTaskFactory,
        () ->
            new StandardGrpcRdaSource<>(
                rdaSourceConfig,
                new FissClaimStreamCaller(),
                appState.getMeters(),
                "fiss",
                jobConfig.getStartingFissSeqNum()),
        createFissSinkFactory(appState),
        appState.getMeters());
  }

  /**
   * Helper method to define a FISS sink factory
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @return A FISS sink factory that creates {@link RdaSink} objects.
   */
  private ThrowingFunction<
          RdaSink<FissClaimChange, RdaChange<RdaFissClaim>>,
          AbstractRdaLoadJob.SinkTypePreference,
          Exception>
      createFissSinkFactory(PipelineApplicationState appState) {
    return (AbstractRdaLoadJob.SinkTypePreference sinkTypePreference) -> {
      RdaSink<FissClaimChange, RdaChange<RdaFissClaim>> sink;
      FissClaimTransformer transformer =
          new FissClaimTransformer(
              appState.getClock(), MbiCache.computedCache(idHasherConfig, appState.getMetrics()));

      if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.SYNCHRONOUS) {
        sink = new FissClaimRdaSink(appState, transformer, true);
      } else if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR) {
        sink = new FissClaimRdaSink(appState, transformer, false);
      } else {
        sink =
            ConcurrentRdaSink.createSink(
                jobConfig.getWriteThreads(),
                jobConfig.getBatchSize(),
                autoUpdateSequenceNumbers ->
                    new FissClaimRdaSink(appState, transformer, autoUpdateSequenceNumbers));
      }

      return sink;
    };
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appState the app state
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaMcsClaimLoadJob createMcsClaimsLoadJob(PipelineApplicationState appState) {
    Callable<RdaSource<McsClaimChange, RdaChange<RdaMcsClaim>>> preJobTaskFactory;

    if (jobConfig.shouldProcessDLQ()) {
      preJobTaskFactory =
          () ->
              new DLQGrpcRdaSource<>(
                  appState.getEntityManagerFactory().createEntityManager(),
                  (seqNumber, mcsClaimChange) -> seqNumber == mcsClaimChange.getSeq(),
                  rdaSourceConfig,
                  new McsClaimStreamCaller(),
                  appState.getMeters(),
                  "mcs");
    } else {
      preJobTaskFactory = EmptyRdaSource::new;
    }

    return new RdaMcsClaimLoadJob(
        jobConfig,
        preJobTaskFactory,
        () ->
            new StandardGrpcRdaSource<>(
                rdaSourceConfig,
                new McsClaimStreamCaller(),
                appState.getMeters(),
                "mcs",
                jobConfig.getStartingMcsSeqNum()),
        createMcsSinkFactory(appState),
        appState.getMeters());
  }

  /**
   * Helper method to define an MCS sink factory
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @return An MCS sink factory that creates {@link RdaSink} objects.
   */
  private ThrowingFunction<
          RdaSink<McsClaimChange, RdaChange<RdaMcsClaim>>,
          AbstractRdaLoadJob.SinkTypePreference,
          Exception>
      createMcsSinkFactory(PipelineApplicationState appState) {
    return (AbstractRdaLoadJob.SinkTypePreference sinkTypePreference) -> {
      RdaSink<McsClaimChange, RdaChange<RdaMcsClaim>> sink;
      McsClaimTransformer transformer =
          new McsClaimTransformer(
              appState.getClock(), MbiCache.computedCache(idHasherConfig, appState.getMetrics()));

      if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.SYNCHRONOUS) {
        sink = new McsClaimRdaSink(appState, transformer, true);
      } else if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR) {
        sink = new McsClaimRdaSink(appState, transformer, false);
      } else {
        sink =
            ConcurrentRdaSink.createSink(
                jobConfig.getWriteThreads(),
                jobConfig.getBatchSize(),
                autoUpdateSequenceNumbers ->
                    new McsClaimRdaSink(appState, transformer, autoUpdateSequenceNumbers));
      }

      return sink;
    };
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
    return Objects.equals(jobConfig, that.jobConfig)
        && Objects.equals(rdaSourceConfig, that.rdaSourceConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobConfig, rdaSourceConfig);
  }

  /**
   * Empty source for stubbing
   *
   * @param <TMessage> The message type for received source messages
   * @param <TClaim> The object type for transformed claims
   */
  private static class EmptyRdaSource<TMessage, TClaim> implements RdaSource<TMessage, TClaim> {

    @Override
    public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TMessage, TClaim> sink)
        throws ProcessingException {
      return 0;
    }

    @Override
    public void close() throws Exception {}
  }
}
