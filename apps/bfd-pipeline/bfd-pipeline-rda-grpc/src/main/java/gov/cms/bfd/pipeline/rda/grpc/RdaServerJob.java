package gov.cms.bfd.pipeline.rda.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomClaimGeneratorConfig;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PipelineJob implementation that runs a mock RDA API server using the gRPC in-process mode. Since
 * gRPC uses its own thread pool the job thread simply sleeps and waits for an interrupt to be
 * received. When the interrupt is received the server is stopped.
 */
public class RdaServerJob implements PipelineJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaServerJob.class);

  /** The server configuration. */
  private final Config config;

  /** Keeps track of how many servers are running, primarily for integration testing. */
  private final AtomicInteger running;

  /**
   * Instantiates a new rda server job.
   *
   * @param config the job configuration
   */
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
      final var serverConfig =
          RdaServer.InProcessConfig.builder()
              .serviceConfig(config.messageSourceFactoryConfig)
              .serverName(config.serverName)
              .build();
      RdaServer.runWithInProcessServerNoParam(
          serverConfig,
          () -> {
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
            }
          });
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

  /** Configuration for the server job. */
  @EqualsAndHashCode
  public static class Config {

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

    /** Message source config used when creating the {@link RdaServer}. */
    private final RdaMessageSourceFactory.Config messageSourceFactoryConfig;

    /** Indicates the source the server will return data from. */
    public enum ServerMode {
      /** Indicates the server will generate its own random data. */
      Random,
      /** Indicates the server will get data from S3. */
      S3
    }

    /** Instantiates a new config. */
    public Config() {
      this(ServerMode.Random, DEFAULT_SERVER_NAME, null, null, null, null, null, null, null);
    }

    /**
     * Instantiates a new config. S3 bucket is required when {@link #serverMode} is {@link
     * ServerMode#S3}.
     *
     * @param serverMode the server mode
     * @param serverName optional server name
     * @param runInterval optional run interval
     * @param randomSeed optional random seed
     * @param randomMaxClaims optional random max claims
     * @param s3ClientConfig optional configuration used to create S3Clients
     * @param s3Bucket optional s3 bucket
     * @param s3Directory optional s3 directory
     * @param s3CacheDirectory optional s3 cache directory
     */
    @Builder
    private Config(
        ServerMode serverMode,
        @Nullable String serverName,
        @Nullable Duration runInterval,
        @Nullable Long randomSeed,
        @Nullable Integer randomMaxClaims,
        @Nullable S3ClientConfig s3ClientConfig,
        @Nullable String s3Bucket,
        @Nullable String s3Directory,
        @Nullable String s3CacheDirectory) {
      Preconditions.checkNotNull(serverMode, "serverMode is required");
      if (serverMode == ServerMode.S3) {
        Preconditions.checkArgument(
            !Strings.isNullOrEmpty(s3Bucket), "S3 bucket is required in S3 mode");
      }
      this.serverMode = serverMode;
      this.serverName = serverName;
      this.runInterval = runInterval == null ? DEFAULT_RUN_INTERVAL : runInterval;
      messageSourceFactoryConfig =
          RdaMessageSourceFactory.Config.builder()
              .randomClaimConfig(
                  RandomClaimGeneratorConfig.builder()
                      .seed(randomSeed == null ? DEFAULT_SEED : randomSeed)
                      .maxToSend(randomMaxClaims == null ? DEFAULT_MAX_CLAIMS : randomMaxClaims)
                      .build())
              .s3ClientConfig(s3ClientConfig)
              .s3Bucket(s3Bucket)
              .s3Directory(s3Directory)
              .s3CacheDirectory(s3CacheDirectory)
              .build();
    }
  }
}
