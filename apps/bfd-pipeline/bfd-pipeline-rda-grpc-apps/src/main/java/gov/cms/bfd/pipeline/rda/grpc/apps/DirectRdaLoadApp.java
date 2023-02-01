package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple application that invokes an RDA API server and writes FISS claims to a database using
 * RdaLoadJob. This program was written for testing purposes and is not planned to be executed in a
 * production environment.
 *
 * <p>Two command line options are required when running the program:
 *
 * <ol>
 *   <li>file: path to a properties file containing configuration settings
 *   <li>claimType: either fiss or mcs to specify type of claims to download
 * </ol>
 */
public class DirectRdaLoadApp {
  /**
   * Sets up the database options to use, the pipeline to use, and creates the pipeline job for RDA.
   *
   * @param args that are passed in
   * @throws Exception if the pipeline encounters a problem loading or reading
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.printf("usage: %s configfile claimType%n", DirectRdaLoadApp.class.getSimpleName());
      System.exit(1);
    }
    final ConfigLoader options =
        ConfigLoader.builder().addPropertiesFile(new File(args[0])).addSystemProperties().build();
    final String claimType = Strings.nullToEmpty(args[1]);

    final MetricRegistry metrics = new MetricRegistry();
    final Slf4jReporter reporter =
        Slf4jReporter.forRegistry(metrics)
            .outputTo(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    reporter.start(5, TimeUnit.SECONDS);

    final RdaLoadOptions jobConfig = readRdaLoadOptionsFromProperties(options);
    final DatabaseOptions databaseConfig =
        readDatabaseOptions(options, jobConfig.getJobConfig().getWriteThreads());
    HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(databaseConfig, metrics);
    System.out.printf("thread count is %d%n", jobConfig.getJobConfig().getWriteThreads());
    System.out.printf("database pool size %d%n", pooledDataSource.getMaximumPoolSize());
    try (PipelineApplicationState appState =
        new PipelineApplicationState(
            new SimpleMeterRegistry(),
            metrics,
            pooledDataSource,
            PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME,
            Clock.systemUTC())) {
      final Optional<PipelineJob<?>> job = createPipelineJob(jobConfig, appState, claimType);
      if (!job.isPresent()) {
        System.err.printf("error: invalid claim type: '%s' expected 'fiss' or 'mcs'%n", claimType);
        System.exit(1);
      }
      try {
        job.get().call();
      } finally {
        reporter.report();
      }
    }
  }

  /**
   * Create a job for the pipeline with the correct claim type of fiss or mcs.
   *
   * @param jobConfig the config to use
   * @param appState sets the state for the pipeline
   * @param claimType whether to use fiss or mcs claims
   * @return the pipeline job for mcs or fiss
   */
  private static Optional<PipelineJob<?>> createPipelineJob(
      RdaLoadOptions jobConfig, PipelineApplicationState appState, String claimType) {
    switch (claimType.toLowerCase()) {
      case "fiss":
        return Optional.of(jobConfig.createFissClaimsLoadJob(appState));
      case "mcs":
        return Optional.of(jobConfig.createMcsClaimsLoadJob(appState));
      default:
        return Optional.empty();
    }
  }

  /**
   * This sets up the database options of db url, user, password, and max connections.
   *
   * @param options the database options to set
   * @param threadCount the number of threads to use
   * @return the database options
   */
  private static DatabaseOptions readDatabaseOptions(ConfigLoader options, int threadCount) {
    return new DatabaseOptions(
        options.stringValue("database.url", null),
        options.stringValue("database.user", null),
        options.stringValue("database.password", null),
        options.intValue("database.maxConnections", Math.max(10, 5 * threadCount)));
  }

  /**
   * Reads and sets the rda options to load from a config file.
   *
   * @param options the config options to use
   * @return the rda confic load options
   */
  private static RdaLoadOptions readRdaLoadOptionsFromProperties(ConfigLoader options) {
    final IdHasher.Config idHasherConfig =
        new IdHasher.Config(
            options.intValue("hash.iterations", 100),
            options.stringValue("hash.pepper", "notarealpepper"));
    final AbstractRdaLoadJob.Config.ConfigBuilder jobConfig =
        AbstractRdaLoadJob.Config.builder()
            .runInterval(Duration.ofDays(1))
            .batchSize(options.intValue("job.batchSize", 1))
            .writeThreads(options.intValue("job.writeThreads", 1));
    options.longOption("job.startingFissSeqNum").ifPresent(jobConfig::startingFissSeqNum);
    options.longOption("job.startingMcsSeqNum").ifPresent(jobConfig::startingMcsSeqNum);
    final RdaSourceConfig grpcConfig =
        RdaSourceConfig.builder()
            .serverType(RdaSourceConfig.ServerType.Remote)
            .host(options.stringValue("api.host", "localhost"))
            .port(options.intValue("api.port", 5003))
            .maxIdle(Duration.ofSeconds(options.intValue("job.idleSeconds", Integer.MAX_VALUE)))
            .build();
    return new RdaLoadOptions(
        jobConfig.build(), grpcConfig, new RdaServerJob.Config(), idHasherConfig);
  }
}
