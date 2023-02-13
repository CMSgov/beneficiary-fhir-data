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
import java.io.DataOutput;
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

  private static final Logger logger = LoggerFactory.getLogger(LoadRdaJsonApp.class);
  private static final String AMAZON_S3_PROTOCOL = "s3://";

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
      logger.info("starting RDA API local server");

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
                  logger.info("running database migration");
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
                    logger.info("starting job {}", job.getClass().getSimpleName());
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
   * Checks that we can make a viable connection to the source
   *
   * @param claimType The type of claims the tested source serves
   * @param factory A {@link MessageSource.Factory} for creating message sources.
   * @throws Exception If there was an issue connecting to the message source.
   */
  private static void checkConnectivity(String claimType, MessageSource.Factory<?> factory)
      throws Exception {
    try (MessageSource<?> source = factory.apply(0)) {
      logger.info("checking for {} claims: {}", claimType, source.hasNext());
    }
  }

  /** Helper class for reading and storing the needed application configurations */
  private static class Config {
    /** The pepper used for hashing MBIs */
    private final String hashPepper;
    /** The number of hashing iterations to use when hashing MBIs */
    private final int hashIterations;
    /** The URL for the database */
    private final String dbUrl;
    /** The username for the database */
    private final String dbUser;
    /** The password for the database */
    private final String dbPassword;
    /** The number of write threads to use when igesting data */
    private final int writeThreads;
    /** The number of records to write to the database in each batch */
    private final int batchSize;
    /** Dictates if the migration logic should be run if needed */
    private final boolean runSchemaMigration;
    /** The location where the files are. This could be a local directory or S3 path */
    private final Optional<String> fileLocation;
    /** The name of the FISS file to read from at the source */
    private final Optional<String> fissFile;
    /** The name of the MCS file to read from at the source */
    private final Optional<String> mcsFile;
    /** The S3 region to use if the source is an S3 connection */
    private final Regions s3Region;
    /** The S3 bucket to use if the source is an S3 connection */
    private final String s3Bucket;

    private Config(ConfigLoader options) {
      hashPepper = options.stringValue("hash.pepper", "notarealpepper");
      hashIterations = options.intValue("hash.iterations", 2);
      dbUrl = options.stringValue("database.url", "jdbc:hsqldb:mem:LoadRdaJsonApp");
      dbUser = options.stringValue("database.user", "");
      dbPassword = options.stringValue("database.password", "");

      writeThreads = options.intValue("job.writeThreads", 1);
      batchSize = options.intValue("job.batchSize", 100);
      runSchemaMigration = options.booleanValue("job.migration", false);
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
     * Creates the {@link DatabaseOptions} for creating a DB connection
     *
     * @return The {@link DataOutput} for creating a database connection
     */
    private DatabaseOptions createDatabaseOptions() {
      return new DatabaseOptions(dbUrl, dbUser, dbPassword, 10);
    }

    /**
     * Creates the {@link RdaLoadOptions} that configure our RDA connection and ingestion specs
     *
     * @param port The port to connect on for the connection.
     * @return An {@link RdaLoadOptions} with the needed configurations and options.
     */
    private RdaLoadOptions createRdaLoadOptions(int port) {
      final IdHasher.Config idHasherConfig = new IdHasher.Config(hashIterations, hashPepper);
      final AbstractRdaLoadJob.Config jobConfig =
          AbstractRdaLoadJob.Config.builder()
              .runInterval(Duration.ofDays(1))
              .writeThreads(writeThreads)
              .batchSize(batchSize)
              .build();
      final RdaSourceConfig grpcConfig =
          RdaSourceConfig.builder()
              .serverType(RdaSourceConfig.ServerType.Remote)
              .host("localhost")
              .port(port)
              .maxIdle(Duration.ofDays(1))
              .build();
      return new RdaLoadOptions(jobConfig, grpcConfig, new RdaServerJob.Config(), idHasherConfig);
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
     * Creates a local file connection source for reading FISS claims.
     *
     * @param sequenceNumber The sequence number to start at when pulling claims (ignored for this
     *     implementation)
     * @return The {@link MessageSource} for reading local files.
     */
    private MessageSource<FissClaimChange> createFissClaimsSource(long sequenceNumber) {
      return createClaimsSourceForFile(fissFile, JsonMessageSource::parseFissClaimChange);
    }

    /**
     * Creates a local file connection source for reading MCS claims.
     *
     * @param sequenceNumber The sequence number to start at when pulling claims (ignored for this
     *     implementation)
     * @return The {@link MessageSource} for reading local files.
     */
    private MessageSource<McsClaimChange> createMcsClaimsSource(long sequenceNumber) {
      return createClaimsSourceForFile(mcsFile, JsonMessageSource::parseMcsClaimChange);
    }

    /**
     * Creates a local file connection source for reading claims of an inferred type.
     *
     * @param jsonFile The file to read from as the source.
     * @param parser The parser to use for parsing the file.
     * @return The created {@link MessageSource} for reading from the local file.
     * @param <T> The claim type that this source is associated with.
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
     * Creates the jobs needed for the pipeline.
     *
     * @param jobConfig The {@link RdaLoadOptions} to use when creating the jobs.
     * @param appState The {@link PipelineApplicationState} to use when configuring the jobs.
     * @return A list of configured {@link PipelineJob}s to run.
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
