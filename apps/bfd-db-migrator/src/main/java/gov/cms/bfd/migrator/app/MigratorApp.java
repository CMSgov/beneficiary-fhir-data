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

  static final int EXIT_CODE_SUCCESS = 0;

  /**
   * This method is called when the application is launched from the command line.
   *
   * @param args generally, should be empty. Application <strong>will</strong> accept configuration
   *     via environment variables, however for local development the application will also accept
   *     the database url, user, and password as args 0, 1, and 2 respectively.
   */
  public static void main(String[] args) {
    LOGGER.info("Successfully started");

    AppConfiguration appConfig = null;
    try {
      // Take some args for running locally (IDE run configs dont honor env vars)
      String dbUrl = args.length > 1 ? args[0] : null;
      String dbUser = args.length > 2 ? args[1] : null;
      String dbPass = args.length > 3 ? args[2] : null;
      appConfig = AppConfiguration.readConfigFromEnvironmentVariables(dbUrl, dbUser, dbPass);
      LOGGER.info("Application configured: '{}'", appConfig);
    } catch (AppConfigurationException e) {
      LOGGER.error("Invalid app configuration, shutting down.", e);
      System.exit(EXIT_CODE_BAD_CONFIG);
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
      System.exit(EXIT_CODE_FAILED_MIGRATION);
    }

    // Hibernate suggests not reusing data sources for validations
    pooledDataSource = createPooledDataSource(appConfig.getDatabaseOptions(), appMetrics);

    // Run hibernate validation after the migrations have succeeded
    boolean validationSuccess = HibernateValidator.runHibernateValidation(pooledDataSource);

    if (!validationSuccess) {
      LOGGER.error("Validation failed, shutting down");
      System.exit(EXIT_CODE_FAILED_HIBERNATE_VALIDATION);
    }

    LOGGER.info("Migration and validation passed, shutting down");
    System.exit(EXIT_CODE_SUCCESS);
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
