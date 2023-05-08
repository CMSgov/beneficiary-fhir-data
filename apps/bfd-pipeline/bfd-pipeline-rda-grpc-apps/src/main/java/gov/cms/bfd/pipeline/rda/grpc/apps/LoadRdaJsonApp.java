package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.io.Files;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Program to load RDA API NDJSON data files into a database from either a local file or from an S3
 * bucket, depending on the configuration for the file.location. The NDJSON files must contain one
 * ClaimChange record per line and the underlying claim within the JSON must match the type of claim
 * expected by the program (i.e. FISS or MCS each have their own property to specify a data file).
 * Configuration is through a combination of a properties file and system properties. Any settings
 * in a system property override those in the configuration file.
 *
 * <p>The following settings are supported provided: hash.pepper, hash.iterations, database.url,
 * database.user, database.password, job.batchSize, job.migration, file.location, file.fiss,
 * file.mcs, s3.region, and s3.bucket. job.migration (defaults to false) is a boolean value
 * indicating whether to run flyway migrations (true runs the migrations, false does not). The
 * file.fiss and file.mcs each default to loading no data so either or both can be provided as
 * needed.
 */
public class LoadRdaJsonApp {
  /** Used for logging. */
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadRdaJsonApp.class);

  /**
   * Main method to load the System Properties for the Config, start the log4jReporter, start the
   * metrics, and start the RDA pipeline.
   *
   * @param args to be passed in by the command line
   * @throws Exception if the RDA builder doesnt close or open
   */
  public static void main(String[] args) throws Exception {
    final ConfigLoader.Builder options = ConfigLoader.builder();
    if (args.length == 1) {
      options.addPropertiesFile(new File(args[0]));
    } else if (System.getProperty("config.properties", "").length() > 0) {
      options.addPropertiesFile(new File(System.getProperty("config.properties")));
    }
    options.addSystemProperties();
    final Config config = new Config(options.build());

    if (!config.isAtLeastOneSourceDefined()) {
      System.err.println("One of file.fiss, file.mcs, or s3.bucket must be defined.");
      System.exit(1);
    }

    final MetricRegistry metrics = new MetricRegistry();
    final Slf4jReporter reporter =
        Slf4jReporter.forRegistry(metrics)
            .outputTo(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    reporter.start(5, TimeUnit.SECONDS);
    try {
      LOGGER.info("starting RDA API local server");

      var serviceConfig = config.createMessageSourceFactoryConfig();

      try (var messageSourceFactory = serviceConfig.createMessageSourceFactory()) {
        checkConnectivity("FISS", messageSourceFactory::createFissMessageSource);
        checkConnectivity("MCS", messageSourceFactory::createMcsMessageSource);
      }

      RdaServer.LocalConfig.builder()
          .serviceConfig(serviceConfig)
          .build()
          .runWithPortParam(
              port -> {
                final RdaLoadOptions jobConfig = config.createRdaLoadOptions(port);
                final DatabaseOptions databaseConfig = config.createDatabaseOptions();
                final HikariDataSource pooledDataSource =
                    PipelineApplicationState.createPooledDataSource(databaseConfig, metrics);
                if (config.runSchemaMigration) {
                  LOGGER.info("running database migration");
                  DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
                }
                try (PipelineApplicationState appState =
                    new PipelineApplicationState(
                        new SimpleMeterRegistry(),
                        metrics,
                        pooledDataSource,
                        PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME,
                        Clock.systemUTC())) {
                  final List<PipelineJob<?>> jobs = config.createPipelineJobs(jobConfig, appState);
                  for (PipelineJob<?> job : jobs) {
                    LOGGER.info("starting job {}", job.getClass().getSimpleName());
                    job.call();
                  }
                }
              });
    } finally {
      reporter.report();
      reporter.close();
    }
  }

  /**
   * Checks that we can make a viable connection to the source.
   *
   * @param claimType The type of claims the tested source serves
   * @param factory A {@link MessageSource.Factory} for creating message sources.
   * @param <TMessage> the type of objects returned by the message sources
   * @throws Exception If there was an issue connecting to the message source.
   */
  private static <TMessage> void checkConnectivity(
      String claimType, MessageSource.Factory<TMessage> factory) throws Exception {
    try (MessageSource<?> source = factory.apply(0)) {
      LOGGER.info("checking for {} claims: {}", claimType, source.hasNext());
    }
  }

  /**
   * Private singleton class to load the config for values for hashing, database options, batch
   * sizes, fissFile, and mcsFile.
   */
  private static class Config {
    /** The hash pepper. */
    private final String hashPepper;
    /** The hash iterations. */
    private final int hashIterations;
    /** The database url. */
    private final String dbUrl;
    /** The database user. */
    private final String dbUser;
    /** The database password. */
    private final String dbPassword;
    /**
     * Indicates the type of {@link AbstractRdaLoadJob.SinkTypePreference} to use when building
     * sinks.
     */
    private final AbstractRdaLoadJob.SinkTypePreference sinkTypePreference;
    /** The number of write threads. */
    private final int writeThreads;
    /** The batch size. */
    private final int batchSize;
    /** Whether to run the schema migration. */
    private final boolean runSchemaMigration;
    /** The RDA Version for the data to load. */
    private final String rdaVersion;
    /** The name of the FISS file to read from at the source. */
    private final Optional<File> fissFile;
    /** The name of the MCS file to read from at the source. */
    private final Optional<File> mcsFile;
    /** The S3 region to use if the source is an S3 connection. */
    private final Optional<Regions> s3Region;
    /** The S3 bucket to use if the source is an S3 connection. */
    private final Optional<String> s3Bucket;
    /** Optional directory name within our S3 bucket. */
    private final Optional<String> s3Directory;

    /**
     * Constructor to load the Configuration options for the private fields above.
     *
     * @param options to load for the RDA pipeline
     */
    private Config(ConfigLoader options) {
      hashPepper = options.stringValue("hash.pepper", "notarealpepper");
      hashIterations = options.intValue("hash.iterations", 2);
      dbUrl = options.stringValue("database.url", "jdbc:hsqldb:mem:LoadRdaJsonApp");
      dbUser = options.stringValue("database.user", "");
      dbPassword = options.stringValue("database.password", "");

      sinkTypePreference =
          options
              .enumOption("job.sinkType", AbstractRdaLoadJob.SinkTypePreference.class)
              .orElse(AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR);
      writeThreads = options.intValue("job.writeThreads", 1);
      batchSize = options.intValue("job.batchSize", 100);
      runSchemaMigration = options.booleanValue("job.migration", false);
      rdaVersion = options.stringOption("rda.version").orElse(RdaService.RDA_PROTO_VERSION);
      fissFile = options.readableFileOption("file.fiss");
      mcsFile = options.readableFileOption("file.mcs");
      s3Region = options.parsedOption("s3.region", Regions.class, Regions::fromName);
      s3Bucket = options.stringOption("s3.bucket");
      s3Directory = options.stringOption("s3.directory");
    }

    /**
     * We do not want to load random data so at least one source of data must have been defined for
     * the application to be able to proceed.
     *
     * @return true if at least one source of data is defined
     */
    private boolean isAtLeastOneSourceDefined() {
      return fissFile.isPresent() || mcsFile.isPresent() || s3Bucket.isPresent();
    }

    /**
     * Creates the {@link DatabaseOptions} from this configuration.
     *
     * @return the database options to be used
     */
    private DatabaseOptions createDatabaseOptions() {
      return new DatabaseOptions(dbUrl, dbUser, dbPassword, 10);
    }

    /**
     * Creates and returns the options to load for the RDA app.
     *
     * @param port the port to be used for the RDA pipeline
     * @return the options used for the RDA pipeline
     */
    private RdaLoadOptions createRdaLoadOptions(int port) {
      final IdHasher.Config idHasherConfig = new IdHasher.Config(hashIterations, hashPepper);
      final AbstractRdaLoadJob.Config jobConfig =
          AbstractRdaLoadJob.Config.builder()
              .runInterval(Duration.ofDays(1))
              .writeThreads(writeThreads)
              .batchSize(batchSize)
              .sinkTypePreference(sinkTypePreference)
              .rdaVersion(RdaVersion.builder().versionString(rdaVersion).build())
              .build();
      final RdaSourceConfig grpcConfig =
          RdaSourceConfig.builder()
              .serverType(RdaSourceConfig.ServerType.Remote)
              .host("localhost")
              .port(port)
              .maxIdle(Duration.ofDays(1))
              .build();
      return new RdaLoadOptions(
          jobConfig, grpcConfig, new RdaServerJob.Config(), 0, idHasherConfig);
    }

    /**
     * Creates a {@link RdaMessageSourceFactory.Config} based on our configuration.
     *
     * @return the config
     */
    private RdaMessageSourceFactory.Config createMessageSourceFactoryConfig() {
      return RdaMessageSourceFactory.Config.builder()
          .version(RdaService.Version.builder().version(RdaService.RDA_PROTO_VERSION).build())
          .fissClaimJson(fissFile.map(Files::asByteSource).orElse(null))
          .mcsClaimJson(mcsFile.map(Files::asByteSource).orElse(null))
          .s3Bucket(s3Bucket.orElse(null))
          .s3Region(s3Region.orElse(null))
          .s3Directory(s3Directory.orElse(null))
          .build();
    }

    /**
     * This function creates the pipeline jobs for Fiss and Mcs claims from the app state.
     *
     * @param jobConfig the RDA options to load
     * @param appState the pipeline application state
     * @return the pipeline jobs to execute
     */
    private List<PipelineJob<?>> createPipelineJobs(
        RdaLoadOptions jobConfig, PipelineApplicationState appState) {
      final var mbiCache = jobConfig.createComputedMbiCache(appState);
      return List.of(
          jobConfig.createFissClaimsLoadJob(appState, mbiCache),
          jobConfig.createMcsClaimsLoadJob(appState, mbiCache));
    }
  }
}
