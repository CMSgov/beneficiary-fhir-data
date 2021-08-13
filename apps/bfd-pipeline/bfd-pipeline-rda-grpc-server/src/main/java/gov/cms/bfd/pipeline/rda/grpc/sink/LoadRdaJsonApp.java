package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.mpsm.rda.v1.ClaimChange;
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
 * in a system property override those in the configuration file. The following settings are
 * supported provided: hash.pepper, hash.iterations, database.url, database.user, database.password,
 * job.batchSize, job.migration, file.fiss, file.mcs. job.migration (defaults to false) is a boolean
 * value indicating whether to run flyway migrations (true runs the migrations, false does not). The
 * file.fiss and file.mcs each default to loading no data so either or both can be provided as
 * needed.
 */
public class LoadRdaJsonApp {
  private static final Logger logger = LoggerFactory.getLogger(LoadRdaJsonApp.class);

  public static void main(String[] argv) throws Exception {
    final ConfigLoader options =
        (argv.length == 1)
            ? ConfigLoader.fromPropertiesFile(new File(argv[0]))
            : ConfigLoader.fromSystemProperties();
    final Config config = new Config(options);

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
      RdaServer.runWithLocalServer(
          config::createFissClaimsSource,
          config::createMcsClaimsSource,
          port -> {
            final RdaLoadOptions jobConfig = config.createRdaLoadOptions(port);
            final DatabaseOptions databaseConfig = config.createDatabaseOptions();
            final HikariDataSource pooledDataSource =
                PipelineApplicationState.createPooledDataSource(databaseConfig, metrics);
            if (config.runSchemaMigration) {
              logger.info("running database migration");
              DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
            }
            final PipelineApplicationState appState =
                new PipelineApplicationState(
                    metrics,
                    pooledDataSource,
                    PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME,
                    Clock.systemUTC());
            final List<PipelineJob<?>> jobs = config.createPipelineJobs(jobConfig, appState);
            for (PipelineJob<?> job : jobs) {
              logger.info("starting job {}", job.getClass().getSimpleName());
              job.call();
            }
          });
    } finally {
      reporter.report();
    }
  }

  private static class Config {
    private final String hashPepper;
    private final int hashIterations;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int batchSize;
    private final boolean runSchemaMigration;
    private final Optional<File> fissFile;
    private final Optional<File> mcsFile;

    private Config(ConfigLoader options) {
      hashPepper = options.stringValue("hash.pepper", "notarealpepper");
      hashIterations = options.intValue("hash.iterations", 2);
      dbUrl = options.stringValue("database.url", "jdbc:hsqldb:mem:LoadRdaJsonApp");
      dbUser = options.stringValue("database.user", "");
      dbPassword = options.stringValue("database.password", "");

      batchSize = options.intValue("job.batchSize", 10);
      runSchemaMigration = options.booleanValue("job.migration", false);
      fissFile = options.readableFileOption("file.fiss");
      mcsFile = options.readableFileOption("file.mcs");
    }

    private DatabaseOptions createDatabaseOptions() {
      return new DatabaseOptions(dbUrl, dbUser, dbPassword, 10);
    }

    private RdaLoadOptions createRdaLoadOptions(int port) {
      final IdHasher.Config idHasherConfig = new IdHasher.Config(hashIterations, hashPepper);
      final AbstractRdaLoadJob.Config jobConfig =
          new AbstractRdaLoadJob.Config(Duration.ofDays(1), batchSize);
      final GrpcRdaSource.Config grpcConfig =
          new GrpcRdaSource.Config("localhost", port, Duration.ofDays(1));
      return new RdaLoadOptions(jobConfig, grpcConfig, idHasherConfig);
    }

    private MessageSource<ClaimChange> createFissClaimsSource() {
      return createClaimsSourceForFile(fissFile);
    }

    private MessageSource<ClaimChange> createMcsClaimsSource() {
      return createClaimsSourceForFile(mcsFile);
    }

    private MessageSource<ClaimChange> createClaimsSourceForFile(Optional<File> jsonFile) {
      if (jsonFile.isPresent()) {
        return new JsonMessageSource<>(jsonFile.get(), JsonMessageSource::parseClaimChange);
      } else {
        return new EmptyMessageSource<>();
      }
    }

    private List<PipelineJob<?>> createPipelineJobs(
        RdaLoadOptions jobConfig, PipelineApplicationState appState) {
      List<PipelineJob<?>> answer = new ArrayList<>();
      fissFile.ifPresent(f -> answer.add(jobConfig.createFissClaimsLoadJob(appState)));
      mcsFile.ifPresent(f -> answer.add(jobConfig.createMcsClaimsLoadJob(appState)));
      return answer;
    }
  }
}
