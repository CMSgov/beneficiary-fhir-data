package gov.cms.bfd.pipeline.rda.grpc.apps;

import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import com.amazonaws.regions.Regions;
import com.google.common.io.Files;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomClaimGeneratorConfig;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.Server;
import java.io.File;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import lombok.extern.slf4j.Slf4j;

/**
 * A stand-alone mock RDA API (version 0.2 MVP) server implementation. The
 * server is intended for
 * testing purposes only and will not be used in production. Data served is
 * specified on the command
 * line and comes from either random data or from a NDJSON file. The server
 * always starts on port
 * 5003.
 */
@Slf4j
public class RdaServerApp {
  /**
   * Starts a RDA API server listening on localhost at a specific port.
   * Configuration is controlled
   * by command line arguments. Each argument specifies one setting. Valid
   * arguments are:
   *
   * <ul>
   * <li>maxToSend:number sets the maximum number of objects to send in random
   * streams
   * <li>port:number sets the port for the server to listen on (default is 5003)
   * <li>seed:number creates a random source using the number as the PRNG seed
   * value
   * <li>random creates a random source using current time as the PRNG seed
   * <li>fissFile:filename creates a source that returns FissClaims contained in
   * an NDJSON file
   * <li>mcsFile:filename creates a source that returns McsClaims contained in an
   * NDJSON file
   * </ul>
   *
   * @param args the input arguments
   * @throws Exception any exception thrown during runtime
   */
  public static void main(String[] args) throws Exception {
    final Config config = new Config(args);
    log.info("Starting server on port {}.", config.port);
    final var serverConfig = RdaServer.LocalConfig.builder()
        .port(config.port)
        .serviceConfig(config.serviceConfig)
        .build();
    try (RdaServer.ServerState state = RdaServer.startLocal(serverConfig)) {
      state.getServer().awaitTermination();
      log.info("server stopping.");
    }
  }

  /** Configuration details for the RDI server. */
  private static class Config {
    /** The port to use for the RDI Server. */
    private final int port;

    /**
     * Configures the S3bucket for connectivity for Fiss and Mcs claims.
     *
     * @param args that are sent in
     * @throws Exception if there is a connectivity issue to S3
     */
    private Config(String[] args) throws Exception {
      final ConfigLoader config = ConfigLoader.builder().addKeyValueCommandLineArguments(args).build();
      final var defaultRandomSeed = System.currentTimeMillis();
      final var randomClaimConfig = RandomClaimGeneratorConfig.builder()
          .seed(config.longOption("random.seed").orElse(defaultRandomSeed))
          .optionalOverride(config.booleanValue("random.verbose", false))
          .randomErrorRate(config.intOption("random.errorRate").orElse(0))
          .maxUniqueMbis(config.intOption("random.max.mbi").orElse(0))
          .maxUniqueClaimIds(config.intOption("random.max.claimId").orElse(0))
          .useTimestampForErrorSeed(true)
          .build();
      final var messageSourceFactoryConfig = RdaMessageSourceFactory.Config.builder()
          .randomMaxClaims(config.intValue("maxToSend", 5_000))
          .randomClaimConfig(randomClaimConfig)
          .fissClaimJson(
              config.readableFileOption("file.fiss").map(Files::asByteSource).orElse(null))
          .mcsClaimJson(
              config.readableFileOption("file.mcs").map(Files::asByteSource).orElse(null))
          .s3Bucket(config.stringOption("s3.bucket").orElse(null))
          .s3Region(config.enumOption("s3.region", Regions::fromName).orElse(null))
          .s3Directory(config.stringOption("s3.directory").orElse(""))
          .s3CacheDirectory(config.stringOption("s3.cacheDirectory").orElse(""))
          .build();
      port = config.intValue("port", 5003);
      seed = config.longOption("seed").orElseGet(System::currentTimeMillis);
      maxToSend = config.intValue("maxToSend", 5_000);
      fissClaimFile = config.readableFileOption("fissFile").orElse(null);
      mcsClaimFile = config.readableFileOption("mcsFile").orElse(null);
      final Optional<String> s3Bucket = config.stringOption("s3Bucket");
      if (s3Bucket.isPresent()) {
        final Region s3Region = Optional.of(Region.of(config.stringOption("s3Region").orElse("")))
            .orElse(SharedS3Utilities.REGION_DEFAULT);
        final S3Client s3Client = SharedS3Utilities.createS3Client(s3Region);
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
     * @param factory   message factory to use
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
        LOGGER.info(
            "serving no more than {} FissClaims using RandomFissClaimSource with seed {}",
            maxToSend,
            seed);
        return new RandomFissClaimSource(seed, maxToSend).toClaimChanges().skip(sequenceNumber);
      }
    }

    /**
     * Create Mcs Claims depending on the source of the claims. The source can be
     * from a file, S3
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
        LOGGER.info(
            "serving no more than {} McsClaims using RandomMcsClaimSource with seed {}",
            maxToSend,
            seed);
        return new RandomMcsClaimSource(seed, maxToSend).toClaimChanges().skip(sequenceNumber);
      }
    }
  }
}
