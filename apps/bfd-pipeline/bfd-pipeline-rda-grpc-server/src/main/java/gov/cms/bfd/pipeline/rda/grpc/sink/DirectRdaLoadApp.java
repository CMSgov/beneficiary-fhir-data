package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.shared.ConfigLoader;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
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
    final DatabaseOptions databaseConfig = readDatabaseOptions(options);
    HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(databaseConfig, metrics);
    DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
    try (PipelineApplicationState appState =
        new PipelineApplicationState(
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

  private static DatabaseOptions readDatabaseOptions(ConfigLoader options) {
    return new DatabaseOptions(
        options.stringValue("database.url", null),
        options.stringValue("database.user", null),
        options.stringValue("database.password", null),
        10);
  }

  private static RdaLoadOptions readRdaLoadOptionsFromProperties(ConfigLoader options) {
    final IdHasher.Config idHasherConfig =
        new IdHasher.Config(
            options.intValue("hash.iterations", 100),
            options.stringValue("hash.pepper", "notarealpepper"));
    final AbstractRdaLoadJob.Config jobConfig =
        new AbstractRdaLoadJob.Config(
            Duration.ofDays(1), options.intValue("job.batchSize", 1),
            options.longOption("job.startingFissSeqNum"),
                options.longOption("job.startingMcsSeqNum"));
    final GrpcRdaSource.Config grpcConfig =
        new GrpcRdaSource.Config(
            GrpcRdaSource.Config.ServerType.Remote,
            options.stringValue("api.host", "localhost"),
            options.intValue("api.port", 5003),
            "",
            Duration.ofSeconds(options.intValue("job.idleSeconds", Integer.MAX_VALUE)));
    return new RdaLoadOptions(jobConfig, grpcConfig, idHasherConfig);
  }
}
