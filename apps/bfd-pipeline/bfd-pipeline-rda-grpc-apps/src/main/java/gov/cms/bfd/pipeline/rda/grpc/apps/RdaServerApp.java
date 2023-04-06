package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.amazonaws.regions.Regions;
import com.google.common.io.Files;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomClaimGeneratorConfig;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * A stand-alone mock RDA API (version 0.2 MVP) server implementation. The server is intended for
 * testing purposes only and will not be used in production. Data served is specified on the command
 * line and comes from either random data or from a NDJSON file. The server always starts on port
 * 5003.
 */
@Slf4j
public class RdaServerApp {
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
    log.info("Starting server on port {}.", config.port);
    final var serverConfig =
        RdaServer.LocalConfig.builder()
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

    /** Configuration used to create the {@link RdaServer}. */
    private final RdaMessageSourceFactory.Config serviceConfig;

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
      final var randomClaimConfig =
          RandomClaimGeneratorConfig.builder()
              .seed(config.longOption("random.seed").orElse(defaultRandomSeed))
              .optionalOverride(config.booleanValue("random.verbose", false))
              .randomErrorRate(config.intOption("random.errorRate").orElse(0))
              .maxUniqueMbis(config.intOption("random.max.mbi").orElse(0))
              .maxUniqueClaimIds(config.intOption("random.max.claimId").orElse(0))
              .useTimestampForErrorSeed(true)
              .build();
      final var messageSourceFactoryConfig =
          RdaMessageSourceFactory.Config.builder()
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
      serviceConfig = messageSourceFactoryConfig;
    }
  }
}
