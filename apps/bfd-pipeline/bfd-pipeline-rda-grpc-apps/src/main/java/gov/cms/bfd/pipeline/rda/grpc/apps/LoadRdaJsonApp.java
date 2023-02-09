package gov.cms.bfd.pipeline.rda.grpc.apps;

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
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Program to load RDA API NDJSON data files into a database. The NDJSON files must contain one
 * ClaimChange record per line and the underlying claim within the JSON must match the type of claim
 * expected by the program (i.e. FISS or MCS each have their own property to specify a data file).
 * Configuration is through a combination of a properties file and system properties. Any settings
 * in a system property override those in the configuration file.
 *
 * <p>The following settings are supported provided: hash.pepper, hash.iterations, database.url,
 * database.user, database.password, job.batchSize, job.migration, file.fiss, file.mcs.
 * job.migration (defaults to false) is a boolean value indicating whether to run flyway migrations
 * (true runs the migrations, false does not). The file.fiss and file.mcs each default to loading no
 * data so either or both can be provided as needed.
 */
public class LoadRdaJsonApp {
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
      RdaServer.LocalConfig.builder()
          .fissSourceFactory(config::createFissClaimsSource)
          .mcsSourceFactory(config::createMcsClaimsSource)
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
   * Private singleton class to load the config for values for hashing, database options, batch
   * sizes, fissFile, and mcsFile.
   */
  private static class Config {
    /** Stores the hash pepper. */
    private final String hashPepper;
    /** Stores the hash iterations. */
    private final int hashIterations;
    /** Stores the database url. */
    private final String dbUrl;
    /** Stores the database user. */
    private final String dbUser;
    /** Stores the database password. */
    private final String dbPassword;
    /** Stores the number of write threads. */
    private final int writeThreads;
    /** Stores the batch size. */
    private final int batchSize;
    /** Stores whether to run the schema migration. */
    private final boolean runSchemaMigration;
    /** Stores the file for fiss claims. */
    private final Optional<File> fissFile;
    /** Stores the file for mcs claims. */
    private final Optional<File> mcsFile;

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

      writeThreads = options.intValue("job.writeThreads", 1);
      batchSize = options.intValue("job.batchSize", 100);
      runSchemaMigration = options.booleanValue("job.migration", false);
      fissFile = options.readableFileOption("file.fiss");
      mcsFile = options.readableFileOption("file.mcs");
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
        Optional<File> jsonFile, JsonMessageSource.Parser<T> parser) {
      if (jsonFile.isPresent()) {
        return new JsonMessageSource<>(jsonFile.get(), parser);
      } else {
        return new EmptyMessageSource<>();
      }
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
