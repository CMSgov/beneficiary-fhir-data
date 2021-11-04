package gov.cms.bfd.pipeline.rda.grpc;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.server.S3JsonMessageSources;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.Server;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PipelineJob implementation that runs a mock RDA API server using the gRPC in-process mode. Since
 * gRPC uses its own thread pool the job thread simply sleeps and waits for an interrupt to be
 * received. When the interrupt is received the server is stopped.
 */
public class RdaServerJob implements PipelineJob<NullPipelineJobArguments> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaServerJob.class);
  private static final Duration SERVER_SHUTDOWN_TIMEOUT = Duration.ofMinutes(5);

  private final Config config;
  private final AtomicInteger running;

  public RdaServerJob(Config config) {
    this.config = config;
    running = new AtomicInteger();
  }

  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return Optional.of(new PipelineJobSchedule(config.runInterval.toMillis(), ChronoUnit.MILLIS));
  }

  @Override
  public boolean isInterruptible() {
    return true;
  }

  /**
   * Starts the mock grpc server, waits for an InterruptedException, then stops the server. The job
   * can potentially run for the entire lifetime of the pipeline app. Any exception thrown by the
   * server causes the job to clean up and terminate and logs the exception but does not pass it
   * through so that the job can be restarted.
   */
  @Override
  public PipelineJobOutcome call() throws Exception {
    try {
      LOGGER.info("starting server with name {} and mode {}", config.serverName, config.serverMode);
      final Server server =
          RdaServer.startInProcess(
              RdaServer.InProcessConfig.builder()
                  .serverName(config.serverName)
                  .version(config.createVersion())
                  .fissSourceFactory(config::createFissClaims)
                  .mcsSourceFactory(config::createMcsClaims)
                  .build());
      try {
        running.incrementAndGet();
        try {
          LOGGER.info("server started - sleeping...");
          Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
          LOGGER.info("sleep interrupted");
        }
      } finally {
        running.decrementAndGet();
        LOGGER.info("telling server to shut down");
        server.shutdown();
        LOGGER.info("waiting up to {} for server to finish shutting down", SERVER_SHUTDOWN_TIMEOUT);
        server.awaitTermination(SERVER_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }
      LOGGER.info("server shutdown complete");
    } catch (Exception ex) {
      LOGGER.error("server terminated by an exception: message={}", ex.getMessage(), ex);
    }
    return PipelineJobOutcome.WORK_DONE;
  }

  /**
   * Method to indicate if the server is currently running. Intended for coordinating integration
   * tests.
   *
   * @return true if the server has been started, false otherwise
   */
  @VisibleForTesting
  public boolean isServerRunning() {
    return running.get() > 0;
  }

  @Getter
  @EqualsAndHashCode
  public static class Config implements Serializable {
    private static final long serialVersionUID = 9653997632697744L;

    /** Default name used by the in-process gRPC server. */
    public static final String DEFAULT_SERVER_NAME = "mock-rda-server";

    /** Default run interval for the job that manages the in-process gRPC server. */
    public static final Duration DEFAULT_RUN_INTERVAL = Duration.ofMinutes(5);

    /** Default PRNG seed used to generate random claims when running in Random mode. */
    public static final long DEFAULT_SEED = 1;

    /** Default number of random claims to return to clients when running in Random mode. */
    public static final int DEFAULT_MAX_CLAIMS = 1_000;

    /**
     * runInterval specifies how often the job should be scheduled. It is used to create a return
     * value for the PipelineJob.getSchedule() method.
     */
    private final Duration runInterval;

    /**
     * Server can either return data from an S3 bucket or generate random data. This field
     * determines which mode the server will use.
     */
    private final ServerMode serverMode;
    /**
     * Name given to the in-process gRPC server. Clients use this name to open a ManagedChannel to
     * the server.
     */
    private final String serverName;

    /** The starting PRNG seed when operating in {@code Random} mode. */
    private final long randomSeed;
    /** The maximum number of claims to be returned when operating in {@code Random} mode. */
    private final int randomMaxClaims;

    /** The S3 connection details when operating in {@code S3} mode. */
    @EqualsAndHashCode.Exclude private final S3JsonMessageSources s3Sources;

    public enum ServerMode {
      Random,
      S3
    }

    public Config() {
      this(ServerMode.Random, DEFAULT_SERVER_NAME, null, null, null, null, null, null);
    }

    @Builder
    private Config(
        ServerMode serverMode,
        String serverName,
        Duration runInterval,
        Long randomSeed,
        Integer randomMaxClaims,
        Regions s3Region,
        String s3Bucket,
        String s3Directory) {
      Preconditions.checkNotNull(serverMode, "serverMode is required");
      if (serverMode == ServerMode.S3) {
        Preconditions.checkArgument(
            !Strings.isNullOrEmpty(serverName), "serverName is required in S3 mode");
        Preconditions.checkArgument(
            !Strings.isNullOrEmpty(s3Bucket), "S3 bucket is required in S3 mode");
      }
      this.serverMode = serverMode;
      this.serverName = serverName;
      this.runInterval = runInterval == null ? DEFAULT_RUN_INTERVAL : runInterval;
      this.randomSeed = randomSeed == null ? DEFAULT_SEED : randomSeed;
      this.randomMaxClaims = randomMaxClaims == null ? DEFAULT_MAX_CLAIMS : randomMaxClaims;
      if (Strings.isNullOrEmpty(s3Bucket)) {
        s3Sources = null;
      } else {
        final Regions region = s3Region == null ? SharedS3Utilities.REGION_DEFAULT : s3Region;
        final AmazonS3 s3Client = SharedS3Utilities.createS3Client(region);
        final String directory = s3Directory == null ? "" : s3Directory;
        s3Sources = new S3JsonMessageSources(s3Client, s3Bucket, directory);
      }
    }

    private RdaService.Version createVersion() {
      RdaService.Version.VersionBuilder versionBuilder = RdaService.Version.builder();
      if (serverMode == ServerMode.S3) {
        versionBuilder.version(String.format("S3:%d", System.currentTimeMillis()));
      } else {
        versionBuilder.version(String.format("Random:%d", randomSeed));
      }
      return versionBuilder.build();
    }

    private MessageSource<FissClaimChange> createFissClaims(long sequenceNumber) throws Exception {
      if (serverMode == ServerMode.S3) {
        LOGGER.info(
            "serving FissClaims using JsonClaimSource with data from S3 bucket {}",
            s3Sources.getBucketName());
        return s3Sources.fissClaimChangeFactory().apply(sequenceNumber);
      } else {
        LOGGER.info(
            "serving no more than {} FissClaims using RandomFissClaimSource with seed {}",
            randomMaxClaims,
            randomSeed);
        return new RandomFissClaimSource(randomSeed, randomMaxClaims)
            .toClaimChanges()
            .skip(sequenceNumber);
      }
    }

    private MessageSource<McsClaimChange> createMcsClaims(long sequenceNumber) throws Exception {
      if (serverMode == ServerMode.S3) {
        LOGGER.info(
            "serving McsClaims using JsonClaimSource with data from S3 bucket {}",
            s3Sources.getBucketName());
        return s3Sources.mcsClaimChangeFactory().apply(sequenceNumber);
      } else {
        LOGGER.info(
            "serving no more than {} McsClaims using RandomMcsClaimSource with seed {}",
            randomMaxClaims,
            randomSeed);
        return new RandomMcsClaimSource(randomSeed, randomMaxClaims)
            .toClaimChanges()
            .skip(sequenceNumber);
      }
    }
  }
}
