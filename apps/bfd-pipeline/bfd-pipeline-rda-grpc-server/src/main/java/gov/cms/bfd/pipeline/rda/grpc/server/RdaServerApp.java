package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaServerApp.class);

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

  private static class Config {
    private final int port;
    private final long seed;
    private final int maxToSend;
    @Nullable private final File fissClaimFile;
    @Nullable private final File mcsClaimFile;
    @Nullable private final S3JsonMessageSources s3Sources;

    private Config(String[] args) throws Exception {
      final ConfigLoader config =
          ConfigLoader.builder().addKeyValueCommandLineArguments(args).build();
      port = config.intValue("port", 5003);
      seed = config.longOption("seed").orElseGet(System::currentTimeMillis);
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

    private void checkS3Connectivity(String claimType, MessageSource.Factory<?> factory)
        throws Exception {
      try (MessageSource<?> source = factory.apply(0)) {
        LOGGER.info("checking for {} claims: {}", claimType, source.hasNext());
      }
    }

    private int getPort() {
      return port;
    }

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
