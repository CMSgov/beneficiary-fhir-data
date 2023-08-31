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
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Getter;
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
   * instance and calls its {@link MigratorApp#run} method. Terminates with an appropriate error
   * code.
   *
   * @param args generally, should be empty. Application <strong>will</strong> accept configuration
   *     via layered configuration sources.
   */
  public static void main(String[] args) {
    try {
      new MigratorApp().run();
      System.exit(EXIT_CODE_SUCCESS);
    } catch (FatalErrorException ex) {
      if (ex.getCause() != null) {
        LOGGER.error(
            "app terminating due to exception: message={}",
            ex.getCause().getMessage(),
            ex.getCause());
      } else {
        LOGGER.error("{}, shutting down", ex.getMessage());
      }
      System.exit(ex.getExitCode());
    } catch (RuntimeException ex) {
      LOGGER.error("app terminating due to unhandled exception: message={}", ex.getMessage(), ex);
      System.exit(EXIT_CODE_FAILED_UNHANDLED_EXCEPTION);
    }
  }

  /**
   * Main application logic. Performs the schema creation, migration and validation.
   *
   * @throws FatalErrorException to report an error that should terminate the application
   */
  private void run() throws FatalErrorException {
    LOGGER.info("Successfully started");
    final AppConfiguration appConfig = loadAppConfiguration();

    final MigratorProgressTracker progressTracker = createProgressTracker(appConfig);
    progressTracker.appStarted();

    final DataSourceFactory dataSourceFactory = appConfig.createDataSourceFactory();
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
   * @throws FatalErrorException to report an error that should terminate the application
   */
  private void validateSchema(
      DataSourceFactory dataSourceFactory,
      MigratorProgressTracker progressTracker,
      MetricRegistry appMetrics)
      throws FatalErrorException {
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
        throw new FatalErrorException("Validation failed", EXIT_CODE_FAILED_HIBERNATE_VALIDATION);
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
   * @throws FatalErrorException to report an error that should terminate the application
   */
  private void createOrUpdateSchema(
      DataSourceFactory dataSourceFactory,
      String flywayScriptLocationOverride,
      MigratorProgressTracker progressTracker,
      MetricRegistry appMetrics)
      throws FatalErrorException {
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
        throw new FatalErrorException("Migration failed", EXIT_CODE_FAILED_MIGRATION);
      }
    }
  }

  /**
   * Loads and returns the app configuration.
   *
   * @return the configuration
   * @throws FatalErrorException to report an error that should terminate the application
   */
  private AppConfiguration loadAppConfiguration() throws FatalErrorException {

    try {
      AppConfiguration appConfig = AppConfiguration.loadConfig(System::getenv);
      LOGGER.info("Application configured: '{}'", appConfig);
      return appConfig;
    } catch (ConfigException | AppConfigurationException e) {
      throw new FatalErrorException("Invalid app configuration", e, EXIT_CODE_BAD_CONFIG);
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
      final var queueName = appConfig.getSqsQueueName();
      final var queueUrl = sqsDao.lookupQueueUrl(queueName);
      // Presently there is no need for a dynamically generated message group id when sending and
      // receiving messages,
      // however any static value is required at a bare-minimum for FIFO sqs queues
      final var messageGroupId = queueName;
      final var sqsProgressReporter = new SqsProgressReporter(sqsDao, queueUrl, messageGroupId);
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
   * @param dataSourceFactory the {@link DataSourceFactory} to use to create a {@link
   *     HikariDataSource}
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  private HikariDataSource createPooledDataSource(
      DataSourceFactory dataSourceFactory, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = dataSourceFactory.createDataSource();
    // we know that flyway requires at least two connections so override the value if it's 1
    pooledDataSource.setMaximumPoolSize(Math.max(2, pooledDataSource.getMaximumPoolSize()));
    pooledDataSource.setMetricRegistry(metrics);
    return pooledDataSource;
  }

  /** Thrown to inform {@link #main} that app should be shut down with a specific exit code. */
  private static class FatalErrorException extends Exception {
    /** Code to pass to {@link System#exit}. */
    @Getter private final int exitCode;

    /**
     * Initializes an instance.
     *
     * @param message Error message to log on exit.
     * @param exitCode Exit code for the application.
     */
    public FatalErrorException(String message, int exitCode) {
      super(message);
      this.exitCode = exitCode;
    }

    /**
     * Initializes an instance.
     *
     * @param message Error message to log on exit.
     * @param cause Exception that triggered the shutdown.
     * @param exitCode Exit code for the application.
     */
    public FatalErrorException(String message, Throwable cause, int exitCode) {
      super(message, cause);
      this.exitCode = exitCode;
    }
  }
}
