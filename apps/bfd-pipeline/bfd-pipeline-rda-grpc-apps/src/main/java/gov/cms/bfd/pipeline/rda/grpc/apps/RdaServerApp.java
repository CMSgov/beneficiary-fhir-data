package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomClaimGeneratorConfig;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.S3JsonMessageSources;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.Server;
import java.io.File;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stand-alone mock RDA API (version 0.2 MVP) server implementation. The server is intended for
 * testing purposes only and will not be used in production. Data served is specified on the command
 * line and comes from either random data or from a NDJSON file. The server always starts on port
 * 5003.
 */
public class RdaServerApp {
  /** Used for logging. */
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaServerApp.class);

  /**
   * Adjustment applied to {@link RandomClaimGeneratorConfig#seed} for FISS claims so that FISS and
   * MCS jobs use a different seed.
   */
  private static final int FISS_RANDOM_SEED_DELTA = 12345;

  /**
   * Adjustment applied to {@link RandomClaimGeneratorConfig#seed} for MCS claims so that FISS and
   * MCS jobs use a different seed.
   */
  private static final int MCS_RANDOM_SEED_DELTA = 67890;

  /**
   * Starts a RDA API server listening on localhost at a specific port. Configuration is controlled
   * by command line arguments. Each argument specifies one setting. Valid arguments are:
   *
   * <ul>
   *   <li>maxToSend:number sets the maximum number of objects to send in random streams
   *   <li>port:number sets the port for the server to listen on (default is 5003)
   *   <li>seed:number creates a random source using the number as the PRNG seed value
   *   <li>random creates a random source using current time as the PRNG seed
   *   <li>fissFile:filename creates a source that returns FissClaims contained in an NDJSON file
   *   <li>mcsFile:filename creates a source that returns McsClaims contained in an NDJSON file
   * </ul>
   *
   * @param args the input arguments
   * @throws Exception any exception thrown during runtime
   */
  public static void main(String[] args) throws Exception {
    final Config config = new Config(args);
    LOGGER.info("Starting server on port {}.", config.getPort());
    Server server =
        RdaServer.startLocal(
            RdaServer.LocalConfig.builder()
                .port(config.getPort())
                .fissSourceFactory(config::createFissClaims)
                .mcsSourceFactory(config::createMcsClaims)
                .build());
    server.awaitTermination();
    LOGGER.info("server stopping.");
  }

  /** Configuration details for the RDI server. */
  private static class Config {
    /** The port to use for the RDI Server. */
    private final int port;
    /** The {@link RandomClaimGeneratorConfig} to use for random claim generation. */
    private final RandomClaimGeneratorConfig randomClaimConfig;
    /** The max number to send for the RDI Server. */
    private final int maxToSend;

    /** The fiss claim file for the RDI Server. */
    @Nullable private final File fissClaimFile;
    /** The mcs claim file for the RDI Server. */
    @Nullable private final File mcsClaimFile;
    /** The s3 file client for the RDI Server. */
    @Nullable private final S3JsonMessageSources s3Sources;

    /**
     * Configures the S3bucket for connectivity for Fiss and Mcs claims.
     *
     * @param args that are sent in
     * @throws Exception if there is a connectivity issue to S3
     */
    private Config(String[] args) throws Exception {
      final ConfigLoader config =
          ConfigLoader.builder().addKeyValueCommandLineArguments(args).build();
      final var defaultRandomSeed = System.currentTimeMillis();
      port = config.intValue("port", 5003);
      randomClaimConfig =
          RandomClaimGeneratorConfig.builder()
              .seed(config.longOption("random.seed").orElse(defaultRandomSeed))
              .optionalOverride(config.booleanValue("random.verbose", false))
              .randomErrorRate(config.intOption("random.errorRate").orElse(0))
              .maxUniqueMbis(config.intOption("random.max.mbi").orElse(0))
              .maxUniqueClaimIds(config.intOption("random.max.claimId").orElse(0))
              .build();
      maxToSend = config.intValue("maxToSend", 5_000);
      fissClaimFile = config.readableFileOption("fissFile").orElse(null);
      mcsClaimFile = config.readableFileOption("mcsFile").orElse(null);
      final Optional<String> s3Bucket = config.stringOption("s3Bucket");
      if (s3Bucket.isPresent()) {
        final Regions s3Region =
            config
                .enumOption("s3Region", Regions::fromName)
                .orElse(SharedS3Utilities.REGION_DEFAULT);
        final AmazonS3 s3Client = SharedS3Utilities.createS3Client(s3Region);
        final String s3Directory = config.stringOption("s3Directory").orElse("");
        s3Sources = new S3JsonMessageSources(s3Client, s3Bucket.get(), s3Directory);
        checkS3Connectivity("FISS", s3Sources.fissClaimChangeFactory());
        checkS3Connectivity("MCS", s3Sources.mcsClaimChangeFactory());
      } else {
        s3Sources = null;
      }
    }

    /**
     * Checks the s3 connectivity for the specified claim factory.
     *
     * @param claimType specifies whether to use the fiss or mcs claims
     * @param factory message factory to use
     * @throws Exception if the specied claim factory can't be opened
     */
    private void checkS3Connectivity(String claimType, MessageSource.Factory<?> factory)
        throws Exception {
      try (MessageSource<?> source = factory.apply(0)) {
        LOGGER.info("checking for {} claims: {}", claimType, source.hasNext());
      }
    }

    /**
     * Gets the {@link #port}.
     *
     * @return port number
     */
    private int getPort() {
      return port;
    }

    /**
     * Creates the fiss claims to process.
     *
     * @param sequenceNumber the starting number
     * @return the Fiss claims
     * @throws Exception if source cannot be closed
     */
    private MessageSource<FissClaimChange> createFissClaims(long sequenceNumber) throws Exception {
      if (fissClaimFile != null) {
        LOGGER.info(
            "serving FissClaims using JsonClaimSource with data from file {}",
            fissClaimFile.getAbsolutePath());
        return new JsonMessageSource<>(fissClaimFile, JsonMessageSource::parseFissClaimChange)
            .filter(change -> change.getSeq() >= sequenceNumber);
      } else if (s3Sources != null) {
        LOGGER.info(
            "serving FissClaims using JsonClaimSource with data from S3 bucket {}",
            s3Sources.getBucketName());
        return s3Sources.fissClaimChangeFactory().apply(sequenceNumber);
      } else {
        final var adjustedConfig = adjustErrorSeed(FISS_RANDOM_SEED_DELTA);
        LOGGER.info(
            "serving no more than {} FissClaims using RandomFissClaimSource with seed {}",
            maxToSend,
            adjustedConfig.getSeed());
        return new RandomFissClaimSource(adjustedConfig, maxToSend)
            .toClaimChanges()
            .skip(sequenceNumber);
      }
    }

    /**
     * Create Mcs Claims depending on the source of the claims. The source can be from a file, S3
     * bucket, or a random claim source.
     *
     * @param sequenceNumber to start at
     * @return the Mcs Claims
     * @throws Exception if the sources cannot be closed
     */
    private MessageSource<McsClaimChange> createMcsClaims(long sequenceNumber) throws Exception {
      if (mcsClaimFile != null) {
        LOGGER.info(
            "serving McsClaims using JsonClaimSource with data from file {}",
            mcsClaimFile.getAbsolutePath());
        return new JsonMessageSource<>(mcsClaimFile, JsonMessageSource::parseMcsClaimChange)
            .filter(change -> change.getSeq() >= sequenceNumber);
      } else if (s3Sources != null) {
        LOGGER.info(
            "serving McsClaims using JsonClaimSource with data from S3 bucket {}",
            s3Sources.getBucketName());
        return s3Sources.mcsClaimChangeFactory().apply(sequenceNumber);
      } else {
        final var adjustedConfig = adjustErrorSeed(MCS_RANDOM_SEED_DELTA);
        LOGGER.info(
            "serving no more than {} McsClaims using RandomMcsClaimSource with seed {}",
            maxToSend,
            adjustedConfig.getSeed());
        return new RandomMcsClaimSource(adjustedConfig, maxToSend)
            .toClaimChanges()
            .skip(sequenceNumber);
      }
    }

    /**
     * Add a small number to the configured random error seed so that different claim types will not
     * generate errors on the exact same sequence number.
     *
     * @param delta amount to add to configured seed
     * @return config with the modified seed value
     */
    private RandomClaimGeneratorConfig adjustErrorSeed(int delta) {
      final var oldSeed = randomClaimConfig.getSeed();
      final var adjustedSeed = oldSeed + delta;
      return randomClaimConfig.toBuilder().seed(adjustedSeed).build();
    }
  }
}
