package gov.cms.bfd.migrator.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.exceptions.FatalAppException;
import gov.cms.bfd.sharedutils.sqs.SqsDao;
import gov.cms.bfd.sharedutils.sqs.SqsEventPublisher;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

  /**
   * This {@link System#exit(int)} value should be used when any {@link RuntimeException} is passed
   * through to the {@link #main} method.
   */
  static final int EXIT_CODE_FAILED_UNHANDLED_EXCEPTION = 4;

  /** This {@link System#exit(int)} value should be used when the application exits successfully. */
  static final int EXIT_CODE_SUCCESS = 0;

  /**
   * The list of packages to scan when doing hibernate validation; these packages should contain the
   * database models that hibernate should attempt to validate. These are allowed to be from
   * external packages. This may be better served coming from an application configuration.
   */
  static final List<String> hibernateValidationModelPackages =
      List.of("gov.cms.bfd.model.rda", "gov.cms.bfd.model.rif");

  /**
   * This method is called when the application is launched from the command line. Creates a new
   * instance and calls its {@link MigratorApp#performMigrations} method. Terminates with an
   * appropriate error code.
   *
   * @param args generally, should be empty. Application <strong>will</strong> accept configuration
   *     via layered configuration sources.
   */
  public static void main(String[] args) {
    int exitCode = new MigratorApp().performMigrationsAndHandleExceptions();
    System.exit(exitCode);
  }

  /**
   * Wrapper around {@link #performMigrations()} that catches any thrown exceptions and returns an
   * appropriate exit code for use by the {@link #main(String[])} method.
   *
   * @return exit code for {@link System#exit}
   */
  @VisibleForTesting
  int performMigrationsAndHandleExceptions() {
    try {
      performMigrations();
      return EXIT_CODE_SUCCESS;
    } catch (FatalAppException ex) {
      if (ex.getCause() != null) {
        LOGGER.error(
            "app terminating due to exception: message={}",
            ex.getCause().getMessage(),
            ex.getCause());
      } else {
        LOGGER.error("{}, shutting down", ex.getMessage());
      }
      return ex.getExitCode();
    } catch (RuntimeException ex) {
      LOGGER.error("app terminating due to unhandled exception: message={}", ex.getMessage(), ex);
      return EXIT_CODE_FAILED_UNHANDLED_EXCEPTION;
    }
  }

  /**
   * Main application logic. Performs the schema creation, migration and validation.
   *
   * @throws FatalAppException to report an error that should terminate the application
   */
  private void performMigrations() throws FatalAppException {
    LOGGER.info("Successfully started");
    final AppConfiguration appConfig = loadAppConfiguration();

    final MigratorProgressTracker progressTracker = createProgressTracker(appConfig);
    progressTracker.appStarted();

    final HikariDataSourceFactory dataSourceFactory = appConfig.createHikariDataSourceFactory();
    final MetricRegistry appMetrics = setupMetrics(appConfig);
    createOrUpdateSchema(
        dataSourceFactory,
        appConfig.getFlywayScriptLocationOverride(),
        progressTracker,
        appMetrics);
    validateSchema(dataSourceFactory, progressTracker, appMetrics);

    LOGGER.info("Migration and validation passed, shutting down");
    progressTracker.appFinished();
  }

  /**
   * Uses {@link HibernateValidator} to validate the database matches our entities.
   *
   * @param dataSourceFactory used to connect to database
   * @param progressTracker used to record app progress
   * @param appMetrics used to track app metrics
   * @throws FatalAppException to report an error that should terminate the application
   */
  private void validateSchema(
      HikariDataSourceFactory dataSourceFactory,
      MigratorProgressTracker progressTracker,
      MetricRegistry appMetrics)
      throws FatalAppException {
    // Hibernate suggests not reusing data sources for validations
    try (HikariDataSource pooledDataSource =
        createPooledDataSource(dataSourceFactory, appMetrics)) {
      boolean validationSuccess;
      // Run hibernate validation after the migrations have succeeded
      HibernateValidator validator =
          new HibernateValidator(pooledDataSource, hibernateValidationModelPackages);
      validationSuccess = validator.runHibernateValidation();

      if (!validationSuccess) {
        progressTracker.appFailed();
        throw new FatalAppException("Validation failed", EXIT_CODE_FAILED_HIBERNATE_VALIDATION);
      }
    }
  }

  /**
   * Uses {@link DatabaseSchemaManager} to create and apply migrations to the database as necessary.
   *
   * @param dataSourceFactory used to connect to database
   * @param flywayScriptLocationOverride the flyway script location override, can be null if no
   *     override
   * @param progressTracker used to record app progress
   * @param appMetrics used to track app metrics
   * @throws FatalAppException to report an error that should terminate the application
   */
  private void createOrUpdateSchema(
      HikariDataSourceFactory dataSourceFactory,
      String flywayScriptLocationOverride,
      MigratorProgressTracker progressTracker,
      MetricRegistry appMetrics)
      throws FatalAppException {
    // Hibernate suggests not reusing data sources for validations
    try (HikariDataSource pooledDataSource =
        createPooledDataSource(dataSourceFactory, appMetrics)) {
      progressTracker.appConnected();

      // run migration
      boolean migrationSuccess =
          DatabaseSchemaManager.createOrUpdateSchema(
              pooledDataSource, flywayScriptLocationOverride, progressTracker::migrating);

      if (!migrationSuccess) {
        progressTracker.appFailed();
        throw new FatalAppException("Migration failed", EXIT_CODE_FAILED_MIGRATION);
      }
    }
  }

  /**
   * Creates a {@link ConfigLoader} that can be used to populate an {@link AppConfiguration}.
   * Intended as an insertion point for a mock during testing.
   *
   * @return the config loader
   */
  @VisibleForTesting
  ConfigLoader createConfigLoader() {
    return LayeredConfiguration.createConfigLoader(Map.of(), ConfigLoaderSource.fromEnv());
  }

  /**
   * Loads and returns the app configuration.
   *
   * @return the configuration
   * @throws FatalAppException to report an error that should terminate the application
   */
  private AppConfiguration loadAppConfiguration() throws FatalAppException {
    try {
      ConfigLoader configLoader = createConfigLoader();
      AppConfiguration appConfig = AppConfiguration.loadConfig(configLoader);
      LOGGER.info("Application configured: '{}'", appConfig);
      return appConfig;
    } catch (ConfigException | AppConfigurationException e) {
      throw new FatalAppException("Invalid app configuration", e, EXIT_CODE_BAD_CONFIG);
    }
  }

  /**
   * Creates a {@link MigratorProgressTracker} that either sends progress updates to SQS or simply
   * logs them depending on the configuration settings.
   *
   * @param appConfig contains the SQS configuration settings
   * @return the tracker
   */
  private MigratorProgressTracker createProgressTracker(AppConfiguration appConfig) {
    Consumer<MigratorProgress> progressConsumer;
    final var sqsClient = appConfig.getSqsClient();
    if (sqsClient == null) {
      progressConsumer = progress -> LOGGER.info("progress: {}", progress);
    } else {
      final var sqsDao = new SqsDao(sqsClient);
      final var queueUrl = sqsDao.lookupQueueUrl(appConfig.getSqsQueueName());
      final var eventPublisher = new SqsEventPublisher(sqsDao, queueUrl);
      final var sqsProgressReporter = new MigratorProgressReporter(eventPublisher);
      progressConsumer = sqsProgressReporter::reportMigratorProgress;
    }
    return new MigratorProgressTracker(progressConsumer);
  }

  /**
   * Sets the metrics.
   *
   * <p>TODO: BFD-1558 Move to shared location for pipeline + this app
   *
   * @param appConfig the app config
   * @return the metrics
   */
  private MetricRegistry setupMetrics(AppConfiguration appConfig) {
    MetricRegistry appMetrics = new MetricRegistry();
    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();

    appMetricsReporter.start(1, TimeUnit.HOURS);
    return appMetrics;
  }

  /**
   * Creates a connection to the configured database.
   *
   * <p>TODO: BFD-1558 Move to shared location for pipeline + this app
   *
   * @param dataSourceFactory the {@link HikariDataSourceFactory} to use to create a {@link
   *     HikariDataSource}
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  private HikariDataSource createPooledDataSource(
      HikariDataSourceFactory dataSourceFactory, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = dataSourceFactory.createDataSource(metrics);
    // we know that flyway requires at least two connections so override the value if it's 1
    pooledDataSource.setMaximumPoolSize(Math.max(2, pooledDataSource.getMaximumPoolSize()));
    return pooledDataSource;
  }
}
