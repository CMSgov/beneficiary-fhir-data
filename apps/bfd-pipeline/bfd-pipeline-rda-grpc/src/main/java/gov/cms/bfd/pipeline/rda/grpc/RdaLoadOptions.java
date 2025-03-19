package gov.cms.bfd.pipeline.rda.grpc;

import com.google.common.base.Preconditions;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
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
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A single combined configuration object to hold the configuration settings for the various
 * components of the RDA load job.
 */
public class RdaLoadOptions {

  /** The job configuration. */
  private final AbstractRdaLoadJob.Config jobConfig;

  /** The RDA source configuration. */
  private final RdaSourceConfig rdaSourceConfig;

  /** The mock server configuration. */
  private final RdaServerJob.Config mockServerConfig;

  /** The number of transformation errors that can exist before a job will exit. */
  private final int errorLimit;

  /** The id hasher configuration. */
  private final IdHasher.Config idHasherConfig;

  /**
   * Instantiates a new rda load options.
   *
   * @param jobConfig the job config
   * @param rdaSourceConfig the rda source config
   * @param mockServerConfig the mock server config
   * @param errorLimit the number of transformation errors that can exist before a job will exit
   * @param idHasherConfig the id hasher config
   */
  public RdaLoadOptions(
      AbstractRdaLoadJob.Config jobConfig,
      RdaSourceConfig rdaSourceConfig,
      RdaServerJob.Config mockServerConfig,
      int errorLimit,
      IdHasher.Config idHasherConfig) {
    this.jobConfig = Preconditions.checkNotNull(jobConfig, "jobConfig is a required parameter");
    this.rdaSourceConfig =
        Preconditions.checkNotNull(rdaSourceConfig, "rdaSourceConfig is a required parameter");
    this.mockServerConfig =
        Preconditions.checkNotNull(mockServerConfig, "mockServerConfig is a required parameter");
    this.errorLimit = errorLimit;
    this.idHasherConfig =
        Preconditions.checkNotNull(idHasherConfig, "idHasherConfig is a required parameter");
  }

  /**
   * Gets the {@link #jobConfig}.
   *
   * @return settings for the overall job.
   */
  public AbstractRdaLoadJob.Config getJobConfig() {
    return jobConfig;
  }

  /**
   * Creates an RDA server job.
   *
   * @return the job, or an empty {@link Optional} if the server type is not configured as a mock
   *     server
   */
  public Optional<RdaServerJob> createRdaServerJob() {
    if (rdaSourceConfig.getServerType() == RdaSourceConfig.ServerType.InProcess) {
      return Optional.of(new RdaServerJob(mockServerConfig));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Creates a new {@link MbiCache} instance that computes hashes on demand. Scales the cache size
   * by multiplying the configured size times the number of writer threads.
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @return a new {@link MbiCache} instance
   */
  public MbiCache createComputedMbiCache(PipelineApplicationState appState) {
    var scaledCacheSize = jobConfig.getWriteThreads() * idHasherConfig.getCacheSize();
    var scaledHasherConfig = idHasherConfig.toBuilder().cacheSize(scaledCacheSize).build();
    return MbiCache.computedCache(scaledHasherConfig, appState.getMetrics());
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @param mbiCache the shared {@link MbiCache}
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaFissClaimLoadJob createFissClaimsLoadJob(
      PipelineApplicationState appState, MbiCache mbiCache) {
    Callable<RdaSource<FissClaimChange, RdaChange<RdaFissClaim>>> preJobTaskFactory;
    CleanupJob cleanupJob;

    if (jobConfig.shouldProcessDLQ()) {
      preJobTaskFactory =
          () ->
              new DLQGrpcRdaSource<>(
                  new TransactionManager(appState.getEntityManagerFactory()),
                  (seqNumber, fissClaimChange) -> seqNumber == fissClaimChange.getSeq(),
                  rdaSourceConfig,
                  new FissClaimStreamCaller(),
                  appState.getMeters(),
                  "fiss",
                  jobConfig.getRdaVersion());
    } else {
      preJobTaskFactory = EmptyRdaSource::new;
    }

    // removes old RDA FISS claims data if enabled.
    cleanupJob =
        new RdaFissClaimCleanupJob(
            new TransactionManager(appState.getEntityManagerFactory()),
            jobConfig.getCleanupRunSize(),
            jobConfig.getCleanupTransactionSize(),
            jobConfig.shouldRunCleanup());

    return new RdaFissClaimLoadJob(
        jobConfig,
        preJobTaskFactory,
        () ->
            new StandardGrpcRdaSource<>(
                rdaSourceConfig,
                new FissClaimStreamCaller(),
                appState.getMeters(),
                "fiss",
                jobConfig.getStartingFissSeqNum(),
                jobConfig.getRdaVersion()),
        createFissSinkFactory(appState, mbiCache),
        cleanupJob,
        appState.getMeters());
  }

  /**
   * Helper method to define a FISS sink factory.
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @param mbiCache the shared {@link MbiCache}
   * @return A FISS sink factory that creates {@link RdaSink} objects.
   */
  private ThrowingFunction<
          RdaSink<FissClaimChange, RdaChange<RdaFissClaim>>,
          AbstractRdaLoadJob.SinkTypePreference,
          Exception>
      createFissSinkFactory(PipelineApplicationState appState, MbiCache mbiCache) {
    return (AbstractRdaLoadJob.SinkTypePreference sinkTypePreference) -> {
      RdaSink<FissClaimChange, RdaChange<RdaFissClaim>> sink;
      FissClaimTransformer transformer = new FissClaimTransformer(appState.getClock(), mbiCache);

      if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.SYNCHRONOUS) {
        sink = new FissClaimRdaSink(appState, transformer, true, errorLimit);
      } else if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR) {
        sink = new FissClaimRdaSink(appState, transformer, false, errorLimit);
      } else {
        sink =
            ConcurrentRdaSink.createSink(
                jobConfig.getWriteThreads(),
                jobConfig.getBatchSize(),
                autoUpdateSequenceNumbers ->
                    new FissClaimRdaSink(
                        appState, transformer, autoUpdateSequenceNumbers, errorLimit));
      }

      return sink;
    };
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appState the app state
   * @param mbiCache the shared {@link MbiCache}
   * @return a PipelineJob instance suitable for use by PipelineManager.
   */
  public RdaMcsClaimLoadJob createMcsClaimsLoadJob(
      PipelineApplicationState appState, MbiCache mbiCache) {
    Callable<RdaSource<McsClaimChange, RdaChange<RdaMcsClaim>>> preJobTaskFactory;
    CleanupJob cleanupJob;

    if (jobConfig.shouldProcessDLQ()) {
      preJobTaskFactory =
          () ->
              new DLQGrpcRdaSource<>(
                  new TransactionManager(appState.getEntityManagerFactory()),
                  (seqNumber, mcsClaimChange) -> seqNumber == mcsClaimChange.getSeq(),
                  rdaSourceConfig,
                  new McsClaimStreamCaller(),
                  appState.getMeters(),
                  "mcs",
                  jobConfig.getRdaVersion());
    } else {
      preJobTaskFactory = EmptyRdaSource::new;
    }

    // removes old RDA MCS claims data if enabled.
    cleanupJob =
        new RdaMcsClaimCleanupJob(
            new TransactionManager(appState.getEntityManagerFactory()),
            jobConfig.getCleanupRunSize(),
            jobConfig.getCleanupTransactionSize(),
            jobConfig.shouldRunCleanup());

    return new RdaMcsClaimLoadJob(
        jobConfig,
        preJobTaskFactory,
        () ->
            new StandardGrpcRdaSource<>(
                rdaSourceConfig,
                new McsClaimStreamCaller(),
                appState.getMeters(),
                "mcs",
                jobConfig.getStartingMcsSeqNum(),
                jobConfig.getRdaVersion()),
        createMcsSinkFactory(appState, mbiCache),
        cleanupJob,
        appState.getMeters());
  }

  /**
   * Helper method to define an MCS sink factory.
   *
   * @param appState the shared {@link PipelineApplicationState}
   * @param mbiCache the shared {@link MbiCache}
   * @return An MCS sink factory that creates {@link RdaSink} objects.
   */
  private ThrowingFunction<
          RdaSink<McsClaimChange, RdaChange<RdaMcsClaim>>,
          AbstractRdaLoadJob.SinkTypePreference,
          Exception>
      createMcsSinkFactory(PipelineApplicationState appState, MbiCache mbiCache) {
    return (AbstractRdaLoadJob.SinkTypePreference sinkTypePreference) -> {
      RdaSink<McsClaimChange, RdaChange<RdaMcsClaim>> sink;
      McsClaimTransformer transformer = new McsClaimTransformer(appState.getClock(), mbiCache);

      if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.SYNCHRONOUS) {
        sink = new McsClaimRdaSink(appState, transformer, true, errorLimit);
      } else if (sinkTypePreference == AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR) {
        sink = new McsClaimRdaSink(appState, transformer, false, errorLimit);
      } else {
        sink =
            ConcurrentRdaSink.createSink(
                jobConfig.getWriteThreads(),
                jobConfig.getBatchSize(),
                autoUpdateSequenceNumbers ->
                    new McsClaimRdaSink(
                        appState, transformer, autoUpdateSequenceNumbers, errorLimit));
      }

      return sink;
    };
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(jobConfig, rdaSourceConfig);
  }

  /**
   * Empty source for stubbing.
   *
   * @param <TMessage> The message type for received source messages
   * @param <TClaim> The object type for transformed claims
   */
  private static class EmptyRdaSource<TMessage, TClaim> implements RdaSource<TMessage, TClaim> {

    @Override
    public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TMessage, TClaim> sink) {
      return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws Exception {}
  }
}
