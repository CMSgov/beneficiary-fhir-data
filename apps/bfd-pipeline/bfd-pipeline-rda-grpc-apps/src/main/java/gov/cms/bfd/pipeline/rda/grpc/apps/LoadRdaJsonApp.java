package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.S3JsonMessageSources;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(LoadRdaJsonApp.class);
  /** The protocol for S3, used for checking file location in logic. */
  private static final String AMAZON_S3_PROTOCOL = "s3://";

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

      MessageSource.Factory<FissClaimChange> fissFactory;
      MessageSource.Factory<McsClaimChange> mcsFactory;

      String fileLocation = config.fileLocation.orElse("");

      if (fileLocation.startsWith(AMAZON_S3_PROTOCOL)) {
        final AmazonS3 s3Client = SharedS3Utilities.createS3Client(config.s3Region);
        final String directory = fileLocation.replace(AMAZON_S3_PROTOCOL, "");
        final S3JsonMessageSources s3Sources =
            new S3JsonMessageSources(s3Client, config.s3Bucket, directory);
        checkConnectivity("FISS", s3Sources.fissClaimChangeFactory());
        checkConnectivity("MCS", s3Sources.mcsClaimChangeFactory());

        fissFactory = config.createFissS3Source(s3Sources);
        mcsFactory = config.createMcsS3Source(s3Sources);
      } else {
        fissFactory = config::createFissClaimsSource;
        mcsFactory = config::createMcsClaimsSource;
      }

      RdaServer.LocalConfig.builder()
          .fissSourceFactory(fissFactory)
          .mcsSourceFactory(mcsFactory)
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
   * @throws Exception If there was an issue connecting to the message source.
   */
  private static void checkConnectivity(String claimType, MessageSource.Factory<?> factory)
      throws Exception {
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
    /** The RDA Version for the data to load */
    private final RdaVersion rdaVersion;
    /** The location where the files are. This could be a local directory or S3 path */
    private final Optional<String> fileLocation;
    /** The name of the FISS file to read from at the source. */
    private final Optional<String> fissFile;
    /** The name of the MCS file to read from at the source. */
    private final Optional<String> mcsFile;
    /** The S3 region to use if the source is an S3 connection. */
    private final Regions s3Region;
    /** The S3 bucket to use if the source is an S3 connection. */
    private final String s3Bucket;

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
              .enumOption("job.sinkType", AbstractRdaLoadJob.SinkTypePreference::valueOf)
              .orElse(AbstractRdaLoadJob.SinkTypePreference.PRE_PROCESSOR);
      writeThreads = options.intValue("job.writeThreads", 1);
      batchSize = options.intValue("job.batchSize", 100);
      runSchemaMigration = options.booleanValue("job.migration", false);
      rdaVersion = RdaVersion.builder().versionString(options.stringValue("rda.version")).build();
      fileLocation = options.stringOption("file.location");
      fissFile = options.stringOption("file.fiss");
      mcsFile = options.stringOption("file.mcs");
      s3Region =
          options
              .enumOption("s3.region", Regions::fromName)
              .orElse(SharedS3Utilities.REGION_DEFAULT);
      s3Bucket = options.stringOption("s3.bucket").orElse("");
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
              .rdaVersion(rdaVersion)
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
     * Creates a source factory for making sources connecting to S3 for FISS claims.
     *
     * @param s3Sources The {@link S3JsonMessageSources} to use for creating S3 connection sources.
     * @return The created {@link MessageSource.Factory} for creating an S3 connection source.
     */
    private MessageSource.Factory<FissClaimChange> createFissS3Source(
        S3JsonMessageSources s3Sources) {
      return sequenceNumber -> {
        MessageSource<FissClaimChange> source;

        if (fissFile.isPresent()) {
          source = s3Sources.readFissClaimChanges(fissFile.get());
        } else {
          // If the file wasn't specified, FISS claims aren't desired
          source = new EmptyMessageSource<>();
        }

        return source;
      };
    }

    /**
     * Creates a source factory for making sources connecting to S3 for MCS claims.
     *
     * @param s3Sources The {@link S3JsonMessageSources} to use for creating S3 connection sources.
     * @return The created {@link MessageSource.Factory} for creating an S3 connection source.
     */
    private MessageSource.Factory<McsClaimChange> createMcsS3Source(
        S3JsonMessageSources s3Sources) {
      return sequenceNumber -> {
        MessageSource<McsClaimChange> source;

        if (mcsFile.isPresent()) {
          source = s3Sources.readMcsClaimChanges(mcsFile.get());
        } else {
          // If the file wasn't specified, MCS claims aren't desired
          source = new EmptyMessageSource<>();
        }

        return source;
      };
    }

    /**
     * Creates the FissClaims from the fissFile.
     *
     * @param sequenceNumber for each FissClaim
     * @return objects that produce FissClaim objects
     */
    private MessageSource<FissClaimChange> createFissClaimsSource(long sequenceNumber) {
      return createClaimsSourceForFile(fissFile, JsonMessageSource::parseFissClaimChange);
    }

    /**
     * Creates the McsClaims from the mcsFile.
     *
     * @param sequenceNumber for each McsClaim
     * @return objects that produce McsClaim objects
     */
    private MessageSource<McsClaimChange> createMcsClaimsSource(long sequenceNumber) {
      return createClaimsSourceForFile(mcsFile, JsonMessageSource::parseMcsClaimChange);
    }

    /**
     * Creates the claims for the RDA app.
     *
     * @param <T> generic message source to be used by both Fiss and Mcs claims
     * @param jsonFile the claim source json file
     * @param parser the claim parser
     * @return objects that produce claim objects
     */
    private <T> MessageSource<T> createClaimsSourceForFile(
        Optional<String> jsonFile, JsonMessageSource.Parser<T> parser) {
      MessageSource<T> source;

      if (jsonFile.isPresent()) {
        String fileName = jsonFile.get();
        String location = fileLocation.orElse("./");
        File file = Paths.get(location, fileName).toFile();

        source = new JsonMessageSource<>(file, parser);
      } else {
        // If no file specified, this type isn't needed.
        source = new EmptyMessageSource<>();
      }

      return source;
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
      List<PipelineJob<?>> answer = new ArrayList<>();
      fissFile.ifPresent(f -> answer.add(jobConfig.createFissClaimsLoadJob(appState)));
      mcsFile.ifPresent(f -> answer.add(jobConfig.createMcsClaimsLoadJob(appState)));
      return answer;
    }
  }
}
