package gov.cms.bfd.migrator.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.newrelic.NewRelicReporter;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.SenderConfiguration;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main application/entry point for the Database Migration system. See {@link
 * MigratorApp#main(String[])}.
 */
public final class MigratorApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  /**
   * This {@link System#exit(int)} value should be used when the provided configuration values are
   * incomplete and/or invalid.
   */
  static final int EXIT_CODE_BAD_CONFIG = 1;

  /** This {@link System#exit(int)} value should be used when the migrations fail for any reason. */
  static final int EXIT_CODE_FAILED_MIGRATION = 2;

  /**
   * This {@link System#exit(int)} value should be used when hibernate validations fail for any
   * reason.
   */
  static final int EXIT_CODE_FAILED_HIBERNATE_VALIDATION = 3;

  /** This {@link System#exit(int)} value should be used when the application exits successfully. */
  static final int EXIT_CODE_SUCCESS = 0;

  /**
   * The list of packages to scan when doing hibernate validation; these packages should contain the
   * database models that hibernate should attempt to validate. These are allowed to be from
   * external packages. This may be better served coming from an application configuration.
   */
  static final List<String> hibernateValidationModelPackages =
      List.of("gov.cms.bfd.model.rda", "gov.cms.bfd.model.rif");

  /** This {@link ProcessHandle#pid} value is the migrator app's current process id (pid). */
  private static final String PID = Long.toString(ProcessHandle.current().pid());

  /** The file name of the {@link #PID} file to externally signal the migrator app's status. */
  private static final String PID_FILENAME = String.format("%s.pid", PID);

  /**
   * This method is called to create a file that encodes the migrator app's process id as the file's
   * name and stores the migrator app's status upon exit. This file should remain empty until the
   * application has exited.
   */
  private static void createPidFile() {
    try {
      File pidFile = new File(PID_FILENAME);
      if (pidFile.createNewFile()) {
        LOGGER.info(String.format("Created new pid file %s", PID_FILENAME));
      } else {
        LOGGER.info(String.format("Found existing %s file", PID_FILENAME));
      }
    } catch (IOException e) {
      LOGGER.error(
          String.format("IOException Error when attempting to write the file %s", PID_FILENAME));
    }
  }

  /**
   * Provides an external signal of the migrator app's exit state in the form of the exit code
   * written to the {@link #PID_FILE}.
   *
   * @param exitCode the code corresponding to the application's exit state
   */
  private static void writeExitCode(int exitCode) {
    String message = Integer.toString(exitCode);
    try {
      FileWriter writer = new FileWriter(PID_FILENAME, false);
      writer.write(message);
      writer.close();
    } catch (IOException e) {
      LOGGER.error(
          String.format("IOException Error when attempting to write to file %s", PID_FILENAME));
    }
  }

  /**
   * This terminates app and records exit code value via {@link System#exit(int)} and {@link
   * #writeExitCode(int)}.
   *
   * @param exitCode the code corresponding to the application's exit state
   */
  private static void exitApp(int exitCode) {
    writeExitCode(exitCode);
    System.exit(exitCode);
  }

  /**
   * This method is called when the application is launched from the command line.
   *
   * @param args generally, should be empty. Application <strong>will</strong> accept configuration
   *     via environment variables.
   */
  public static void main(String[] args) {
    createPidFile();
    LOGGER.info("Successfully started");

    AppConfiguration appConfig = null;
    try {
      appConfig = AppConfiguration.readConfigFromEnvironmentVariables();
      LOGGER.info("Application configured: '{}'", appConfig);
    } catch (AppConfigurationException e) {
      LOGGER.error("Invalid app configuration, shutting down.", e);
      exitApp(EXIT_CODE_BAD_CONFIG);
    }

    MetricRegistry appMetrics = setupMetrics(appConfig);

    // Create a data source for use by createOrUpdateSchema
    HikariDataSource pooledDataSource =
        createPooledDataSource(appConfig.getDatabaseOptions(), appMetrics);

    // run migration
    boolean migrationSuccess =
        DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource, appConfig);

    if (!migrationSuccess) {
      LOGGER.error("Migration failed, shutting down");
      exitApp(EXIT_CODE_FAILED_MIGRATION);
    }

    // Hibernate suggests not reusing data sources for validations
    pooledDataSource = createPooledDataSource(appConfig.getDatabaseOptions(), appMetrics);

    // Run hibernate validation after the migrations have succeeded
    HibernateValidator validator =
        new HibernateValidator(pooledDataSource, hibernateValidationModelPackages);
    boolean validationSuccess = validator.runHibernateValidation();

    if (!validationSuccess) {
      LOGGER.error("Validation failed, shutting down");
      exitApp(EXIT_CODE_FAILED_HIBERNATE_VALIDATION);
    }

    LOGGER.info("Migration and validation passed, shutting down");
    exitApp(EXIT_CODE_SUCCESS);
  }

  /**
   * Sets the metrics.
   *
   * <p>TODO: BFD-1558 Move to shared location for pipeline + this app
   *
   * @param appConfig the app config
   * @return the metrics
   */
  private static MetricRegistry setupMetrics(AppConfiguration appConfig) {
    MetricRegistry appMetrics = new MetricRegistry();
    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();

    MetricOptions metricOptions = appConfig.getMetricOptions();
    if (metricOptions.getNewRelicMetricKey().isPresent()) {
      SenderConfiguration configuration =
          SenderConfiguration.builder(
                  metricOptions.getNewRelicMetricHost().orElse(null),
                  metricOptions.getNewRelicMetricPath().orElse(null))
              .httpPoster(new OkHttpPoster())
              .apiKey(metricOptions.getNewRelicMetricKey().orElse(null))
              .build();

      MetricBatchSender metricBatchSender = MetricBatchSender.create(configuration);

      Attributes commonAttributes =
          new Attributes()
              .put("host", metricOptions.getHostname().orElse("unknown"))
              .put("appName", metricOptions.getNewRelicAppName().orElse(null));

      NewRelicReporter newRelicReporter =
          NewRelicReporter.build(appMetrics, metricBatchSender)
              .commonAttributes(commonAttributes)
              .build();

      newRelicReporter.start(metricOptions.getNewRelicMetricPeriod().orElse(15), TimeUnit.SECONDS);
    }

    appMetricsReporter.start(1, TimeUnit.HOURS);
    return appMetrics;
  }

  /**
   * Creates a connection to the configured database.
   *
   * <p>TODO: BFD-1558 Move to shared location for pipeline + this app
   *
   * @param dbOptions the {@link DatabaseOptions} to use for the application's DB
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  private static HikariDataSource createPooledDataSource(
      DatabaseOptions dbOptions, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = new HikariDataSource();

    pooledDataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    pooledDataSource.setUsername(dbOptions.getDatabaseUsername());
    pooledDataSource.setPassword(dbOptions.getDatabasePassword());
    pooledDataSource.setMaximumPoolSize(dbOptions.getMaxPoolSize());
    pooledDataSource.setRegisterMbeans(true);
    pooledDataSource.setMetricRegistry(metrics);

    return pooledDataSource;
  }
}
