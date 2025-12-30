package gov.cms.bfd.pipeline.app;

import static gov.cms.bfd.pipeline.app.AppConfiguration.MICROMETER_CW_ALLOWED_METRIC_NAMES;

import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusReporter;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetQueue;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileManager;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3ManifestDbDao;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineOutcome;
import gov.cms.bfd.pipeline.sharedutils.ec2.AwsEc2Client;
import gov.cms.bfd.pipeline.sharedutils.npi_fda.NpiFdaLoadJob;
import gov.cms.bfd.pipeline.sharedutils.npi_fda.NpiFdaLoadJobConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.BackfillConfigOptions;
import gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.SamhsaBackfillJob;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.events.DoNothingEventPublisher;
import gov.cms.bfd.sharedutils.events.EventPublisher;
import gov.cms.bfd.sharedutils.exceptions.FatalAppException;
import gov.cms.bfd.sharedutils.sqs.SqsDao;
import gov.cms.bfd.sharedutils.sqs.SqsEventPublisher;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * The main application/driver/entry point for the ETL system, which will pull any data stored in
 * the specified S3 bucket, parse it, and push it to the specified database server. See {@link
 * #main(String[])}.
 */
@RequiredArgsConstructor
public final class PipelineApplication {
  static final Logger LOGGER = LoggerFactory.getLogger(PipelineApplication.class);

  /** EC2 client. */
  private final AwsEc2Client ec2Client;

  /** This {@link System#exit(int)} value should be used when the application exits successfully. */
  static final int EXIT_CODE_SUCCESS = 0;

  /**
   * This {@link System#exit(int)} value should be used when the provided configuration values are
   * incomplete and/or invalid.
   */
  static final int EXIT_CODE_BAD_CONFIG = 1;

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception.
   */
  static final int EXIT_CODE_JOB_FAILED = 2;

  /**
   * This {@link System#exit(int)} value should be used when the smoke test for one or more jobs
   * fails.
   */
  static final int EXIT_CODE_SMOKE_TEST_FAILURE = 3;

  /**
   * This {@link System#exit(int)} value should be used when any {@link RuntimeException} is passed
   * through to the {@link #main} method.
   */
  static final int EXIT_CODE_FAILED_UNHANDLED_EXCEPTION = 4;

  /**
   * This method is the one that will get called when users launch the application from the command
   * line.
   *
   * @param args (should be empty, as this application accepts configuration via environment
   *     variables)
   */
  public static void main(String[] args) {
    int exitCode = new PipelineApplication(new AwsEc2Client()).runPipelineAndHandleExceptions();
    System.exit(exitCode);
  }

  /**
   * Wrapper around {@link #runPipeline} that catches any thrown exceptions and returns an
   * appropriate exit code for use by the {@link #main} method.
   *
   * @return exit code for {@link System#exit}
   */
  @VisibleForTesting
  int runPipelineAndHandleExceptions() {
    try {
      PipelineOutcome outcome = runPipeline();
      if (outcome == PipelineOutcome.TERMINATE_INSTANCE) {
        // When we trigger a scale-in, the instance gets terminated very quickly,
        // so we should call this at the last minute after all log messages have been flushed.
        // We can't schedule a scale-in in the future without causing a race condition that may
        // prevent the next scale-out from happening.
        ec2Client.scaleInNow();
      }
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
    } catch (Exception ex) {
      LOGGER.error("app terminating due to unhandled exception: message={}", ex.getMessage(), ex);
      return EXIT_CODE_FAILED_UNHANDLED_EXCEPTION;
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
    return AppConfiguration.createConfigLoader(ConfigLoaderSource.fromEnv());
  }

  /**
   * Runs the actual pipeline logic and throws an exception if any problems are encountered.
   *
   * @throws FatalAppException if app shutdown required
   * @throws IOException for I/O errors
   * @throws SQLException for database errors
   * @return outcome
   */
  private PipelineOutcome runPipeline() throws FatalAppException, SQLException, IOException {

    LOGGER.info("Application starting up!");
    logTempDirectory();

    final AppConfiguration appConfig;
    final ConfigLoader configLoader;
    try {
      // add any additional sources of configuration variables then load the app config
      configLoader = createConfigLoader();
      appConfig = AppConfiguration.loadConfig(configLoader);
      LOGGER.info("Application configured: '{}'", appConfig);
    } catch (ConfigException | AppConfigurationException e) {
      System.err.println(e.getMessage());
      LOGGER.warn("Invalid app configuration.", e);
      throw new FatalAppException("Invalid app configuration", e, EXIT_CODE_BAD_CONFIG);
    }
    final var appMeters = new CompositeMeterRegistry();
    final var appMetrics = new MetricRegistry();
    configureMetrics(configLoader, appMeters, appMetrics);

    // Create a pooled data source for use by any registered jobs.
    final HikariDataSourceFactory dataSourceFactory = appConfig.createHikariDataSourceFactory();
    try (HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(dataSourceFactory, appMetrics)) {
      logDatabaseDetails(pooledDataSource);
      return createJobsAndRunPipeline(appConfig, appMeters, appMetrics, pooledDataSource);
    }
  }

  /**
   * Configures all of our metrics. This can include CloudWatch, JMX, and Slf4j.
   *
   * @param configLoader used to obtain additional settings
   * @param appMeters micrometer registry
   * @param appMetrics drop wizard registry
   */
  private void configureMetrics(
      ConfigLoader configLoader, CompositeMeterRegistry appMeters, MetricRegistry appMetrics) {
    final var micrometerClock = io.micrometer.core.instrument.Clock.SYSTEM;

    final var cloudwatchRegistryConfig =
        AppConfiguration.loadCloudWatchRegistryConfig(configLoader);
    if (cloudwatchRegistryConfig.enabled()) {
      LOGGER.info("Adding CloudWatchMeterRegistry...");
      final var cloudWatchRegistry =
          new CloudWatchMeterRegistry(
              cloudwatchRegistryConfig, micrometerClock, CloudWatchAsyncClient.builder().build());
      cloudWatchRegistry
          .config()
          .meterFilter(
              MeterFilter.denyUnless(
                  id -> MICROMETER_CW_ALLOWED_METRIC_NAMES.contains(id.getName())));
      appMeters.add(cloudWatchRegistry);
      LOGGER.info("Added CloudWatchMeterRegistry.");
    }
    if (AppConfiguration.isJmxMetricsEnabled(configLoader)) {
      appMeters.add(new JmxMeterRegistry(JmxConfig.DEFAULT, micrometerClock));
      LOGGER.info("Added JmxMeterRegistry.");
    }

    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();

    appMeters.add(getDropWizardMeterRegistry(appMetrics, List.of(), micrometerClock));
    LOGGER.info("Added DropwizardMeterRegistry.");

    new JvmMemoryMetrics().bindTo(appMeters);

    appMetricsReporter.start(1, TimeUnit.HOURS);
  }

  /**
   * Extracts database details from the pool and writes them to the log file.
   *
   * @param pooledDataSource our connection pool
   * @throws SQLException if database details cannot be obtained
   */
  private void logDatabaseDetails(HikariDataSource pooledDataSource) throws SQLException {
    try (HikariProxyConnection dbConn = (HikariProxyConnection) pooledDataSource.getConnection()) {
      DatabaseMetaData dbmeta = dbConn.getMetaData();
      String dbName = dbmeta.getDatabaseProductName();
      LOGGER.info(
          "Database: {}, Driver version: major: {}, minor: {}; JDBC version, major: {}, minor: {}",
          dbName,
          dbmeta.getDriverMajorVersion(),
          dbmeta.getDriverMinorVersion(),
          dbmeta.getJDBCMajorVersion(),
          dbmeta.getJDBCMinorVersion());

      if (dbName.equals("PostgreSQL")) {
        BaseConnection pSqlConnection = dbConn.unwrap(BaseConnection.class);
        LOGGER.info("pgjdbc, logServerDetail: {}", pSqlConnection.getLogServerErrorDetail());
      }
    }
  }

  /**
   * Creates all of the configured jobs and runs them using a {@link PipelineManager}.
   *
   * @param appConfig our {@link AppConfiguration} for configuring jobs
   * @param appMeters the app meters
   * @param appMetrics the {@link MetricRegistry} to receive metrics
   * @param pooledDataSource our connection pool
   * @return pipeline outcome
   * @throws FatalAppException if app shutdown required
   */
  private PipelineOutcome createJobsAndRunPipeline(
      AppConfiguration appConfig,
      CompositeMeterRegistry appMeters,
      MetricRegistry appMetrics,
      HikariDataSource pooledDataSource)
      throws FatalAppException, IOException {
    final var clock = Clock.systemUTC();

    /*
     * Create all jobs and run their smoke tests.
     */
    final var jobs = createAllJobs(appConfig, appMeters, appMetrics, pooledDataSource, clock);
    if (anySmokeTestFailed(jobs)) {
      LOGGER.info("Pipeline terminating due to smoke test failure.");
      throw new FatalAppException("Pipeline smoke test failure", EXIT_CODE_SMOKE_TEST_FAILURE);
    }

    final var pipelineManager = new PipelineManager(Thread::sleep, clock, jobs);
    registerShutdownHook(appMetrics, pipelineManager);

    pipelineManager.start();
    LOGGER.info("Job processing started.");

    PipelineOutcome pipelineOutcome = pipelineManager.awaitCompletion();

    // Ensures that any CloudWatch metrics are published prior to the stop of the Pipeline
    appMeters.close();

    if (pipelineManager.getError() != null) {
      throw new FatalAppException(
          "Pipeline job threw exception", pipelineManager.getError(), EXIT_CODE_JOB_FAILED);
    }

    return pipelineOutcome;
  }

  /**
   * Creates a {@link DropwizardMeterRegistry} to transfer micrometer metrics into a {@link
   * MetricRegistry}.
   *
   * @param appMetrics the {@link MetricRegistry} to receive metrics
   * @param commonTags the {@link Tag}s to assign to all metrics
   * @param micrometerClock the {@link io.micrometer.core.instrument.Clock} used to compute time
   * @return the {@link DropwizardMeterRegistry}
   */
  private DropwizardMeterRegistry getDropWizardMeterRegistry(
      MetricRegistry appMetrics,
      List<Tag> commonTags,
      io.micrometer.core.instrument.Clock micrometerClock) {
    DropwizardConfig dropwizardConfig =
        new DropwizardConfig() {
          @Nonnull
          @Override
          public String prefix() {
            return "dropwizard";
          }

          @Override
          public String get(@Nonnull String key) {
            return null;
          }
        };
    return new DropwizardMeterRegistry(
        dropwizardConfig, appMetrics, getHierarchicalNameMapper(commonTags), micrometerClock) {
      @Nonnull
      @Override
      protected Double nullGaugeValue() {
        return 0.0;
      }
    };
  }

  /**
   * Creates a {@link HierarchicalNameMapper} suitable for copying metrics from Micrometer into
   * DropWizard. Ignores the common tags since those would just add noise to the metric names.
   *
   * @param commonTags tags to ignore when generating unique names
   * @return a {@link HierarchicalNameMapper}
   */
  @Nonnull
  private HierarchicalNameMapper getHierarchicalNameMapper(List<Tag> commonTags) {
    final var commonTagNames = commonTags.stream().map(Tag::getKey).collect(Collectors.toSet());
    final var convention = NamingConvention.identity;
    return (id, ignored) -> {
      var tags =
          id.getTags().stream()
              .filter(t -> !commonTagNames.contains(t.getKey()))
              .collect(Collectors.toList());
      return id.getConventionName(convention)
          + tags.stream()
              .map(t -> "." + t.getKey() + "." + t.getValue())
              .map(nameSegment -> nameSegment.replace(" ", "_"))
              .collect(Collectors.joining(""));
    };
  }

  /**
   * Run the smoke test for all jobs and log any failures. Return true if any of them failed. Even
   * if one test fails all of the others are also run so that error reporting can be as complete as
   * possible.
   *
   * @param jobs all {@link PipelineJob} to test
   * @return true if any test failed
   */
  private boolean anySmokeTestFailed(List<PipelineJob> jobs) {
    boolean anyTestFailed = false;
    for (PipelineJob job : jobs) {
      try {
        LOGGER.info("smoke test running: job={}", job.getType());
        if (job.isSmokeTestSuccessful()) {
          LOGGER.info("smoke test successful: job={}", job.getType());
        } else {
          anyTestFailed = true;
          LOGGER.error("smoke test reported failure: job={}", job.getType());
        }
      } catch (Exception ex) {
        LOGGER.error(
            "smoke test threw exception: job={} error={}", job.getType(), ex.getMessage(), ex);
        anyTestFailed = true;
      }
    }
    return anyTestFailed;
  }

  /**
   * Creates the pipeline job for the SAMHSA backfill.
   *
   * @param batchSize The query batch size.
   * @param logInterval The log interval.
   * @param appMeters The meter registry.
   * @param appMetrics The metrics registry.
   * @param pooledDataSource The Hikari data source.
   * @param clock Clock.
   * @return The Pipeline job.
   */
  PipelineJob createBackfillJob(
      int batchSize,
      Long logInterval,
      MeterRegistry appMeters,
      MetricRegistry appMetrics,
      HikariDataSource pooledDataSource,
      Clock clock) {
    PipelineApplicationState ccwAppState =
        new PipelineApplicationState(
            appMeters,
            appMetrics,
            pooledDataSource,
            PipelineApplicationState.PERSISTENCE_UNIT_NAME,
            clock);
    PipelineApplicationState rdaState =
        new PipelineApplicationState(
            appMeters,
            appMetrics,
            pooledDataSource,
            PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME,
            clock);
    return new SamhsaBackfillJob(ccwAppState, rdaState, batchSize, logInterval);
  }

  /**
   * Create all pipeline jobs and return them in a list.
   *
   * @param appConfig our {@link AppConfiguration} for configuring jobs
   * @param appMeters the app meters
   * @param appMetrics our {@link MetricRegistry} for metrics reporting
   * @param pooledDataSource our {@link javax.sql.DataSource}
   * @param clock used to get current time
   * @return list of {@link PipelineJob}s to be registered
   */
  @VisibleForTesting
  List<PipelineJob> createAllJobs(
      AppConfiguration appConfig,
      MeterRegistry appMeters,
      MetricRegistry appMetrics,
      HikariDataSource pooledDataSource,
      Clock clock)
      throws IOException {
    final var jobs = new ArrayList<PipelineJob>();

    /*
     * Create and register the other jobs.
     */
    if (appConfig.getCcwRifLoadOptions().isPresent()) {
      // Create an application state that reuses the existing pooled data source with the ccw/rif
      // persistence unit.
      final PipelineApplicationState appState =
          new PipelineApplicationState(
              appMeters,
              appMetrics,
              pooledDataSource,
              PipelineApplicationState.PERSISTENCE_UNIT_NAME,
              clock);

      final var loadOptions = appConfig.getCcwRifLoadOptions().get();
      final var awsClientConfig = appConfig.getAwsClientConfig();
      final var job = createCcwRifLoadJob(loadOptions, appState, awsClientConfig, clock);
      jobs.add(job);
      LOGGER.info("Registered CcwRifLoadJob.");
    } else {
      LOGGER.warn("CcwRifLoadJob is disabled in app configuration.");
    }

    if (appConfig.getRdaLoadOptions().isPresent()) {
      LOGGER.info("RDA API jobs are enabled in app configuration.");
      // Create an application state that reuses the existing pooled data source with the rda
      // persistence unit.
      final PipelineApplicationState rdaAppState =
          new PipelineApplicationState(
              appMeters,
              appMetrics,
              pooledDataSource,
              PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME,
              clock);

      final RdaLoadOptions rdaLoadOptions = appConfig.getRdaLoadOptions().get();

      final Optional<RdaServerJob> mockServerJob = rdaLoadOptions.createRdaServerJob();
      if (mockServerJob.isPresent()) {
        jobs.add(mockServerJob.get());
        LOGGER.warn("Registered RdaServerJob.");
      } else {
        LOGGER.info("Skipping RdaServerJob registration - not enabled in app configuration.");
      }

      final var mbiCache = rdaLoadOptions.createComputedMbiCache(rdaAppState);
      jobs.add(rdaLoadOptions.createFissClaimsLoadJob(rdaAppState, mbiCache));
      LOGGER.info("Registered RdaFissClaimLoadJob.");

      jobs.add(rdaLoadOptions.createMcsClaimsLoadJob(rdaAppState, mbiCache));
      LOGGER.info("Registered RdaMcsClaimLoadJob.");
    } else {
      LOGGER.info("RDA API jobs are not enabled in app configuration.");
    }
    final Optional<BackfillConfigOptions> backfillConfigOptions =
        appConfig.getBackfillConfigOptions();
    if (backfillConfigOptions.isPresent()) {
      final var backfillJob =
          createBackfillJob(
              backfillConfigOptions.get().getBatchSize(),
              backfillConfigOptions.get().getLogInterval(),
              appMeters,
              appMetrics,
              pooledDataSource,
              clock);
      jobs.add(backfillJob);
      LOGGER.warn("Registered SAMHSA backfill job.");
    } else {
      LOGGER.info("SAMHSA backfill job is disabled.");
    }
    final Optional<NpiFdaLoadJobConfig> npiFdaConfig = appConfig.getNpiFdaLoadConfigOptions();
    if (npiFdaConfig.isPresent()) {
      final var npiFdaJob =
          createNpiFdaJob(
              appMeters,
              appMetrics,
              pooledDataSource,
              clock,
              npiFdaConfig.get().getBatchSize(),
              npiFdaConfig.get().getRunInterval());
      if (npiFdaJob != null) {
        jobs.add(npiFdaJob);
      } else {
        LOGGER.error("There was a problem creating NpiFdaJob.");
      }
      LOGGER.warn("Registered NpiFda job.");
    } else {
      LOGGER.info("NpiFdaLoadJob is disabled.");
    }

    return jobs;
  }

  private NpiFdaLoadJob createNpiFdaJob(
      MeterRegistry appMeters,
      MetricRegistry appMetrics,
      HikariDataSource pooledDataSource,
      Clock clock,
      int batchSize,
      int runInterval) {
    PipelineApplicationState npiAppState =
        new PipelineApplicationState(
            appMeters,
            appMetrics,
            pooledDataSource,
            PipelineApplicationState.PERSISTENCE_UNIT_NAME,
            clock);
    PipelineApplicationState fdaAppState =
        new PipelineApplicationState(
            appMeters,
            appMetrics,
            pooledDataSource,
            PipelineApplicationState.PERSISTENCE_UNIT_NAME,
            clock);
    try {
      return new NpiFdaLoadJob(npiAppState, fdaAppState, batchSize, runInterval);
    } catch (Exception e) {
      LOGGER.error("An exception was thrown while creating NpiFdaLoadJob: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Creates the CCW RIF loader job and returns it.
   *
   * @param loadOptions the {@link CcwRifLoadOptions} to use
   * @param appState the {@link PipelineApplicationState} to use
   * @param awsClientConfig AWS client configuration
   * @param clock used to get current time
   * @return a {@link CcwRifLoadJob} instance for the application to use
   */
  private PipelineJob createCcwRifLoadJob(
      CcwRifLoadOptions loadOptions,
      PipelineApplicationState appState,
      AwsClientConfig awsClientConfig,
      Clock clock)
      throws IOException {
    RifFilesProcessor rifProcessor = new RifFilesProcessor();
    RifLoader rifLoader = new RifLoader(loadOptions.getLoadOptions(), appState);

    /*
     * Create the DataSetMonitorListener that will glue those stages together and run them all for
     * each data set that is found.
     */
    DataSetMonitorListener dataSetMonitorListener =
        new DefaultDataSetMonitorListener(
            appState.getMetrics(), appState.getMeters(), rifProcessor, rifLoader);
    var s3Factory = new AwsS3ClientFactory(loadOptions.getExtractionOptions().getS3ClientConfig());
    // Tell SQ it's ok not to use try-finally here since this will be closed by the CcwRifLoadJob.
    @SuppressWarnings("java:S2095")
    var dataSetQueue =
        new DataSetQueue(
            clock,
            appState.getMetrics(),
            new S3ManifestDbDao(appState.getEntityManagerFactory()),
            new S3FileManager(
                appState.getMetrics(),
                s3Factory.createS3Dao(),
                loadOptions.getExtractionOptions().getS3BucketName()));
    var statusReporter = createCcwRifLoadJobStatusReporter(loadOptions, awsClientConfig, clock);
    CcwRifLoadJob ccwRifLoadJob =
        new CcwRifLoadJob(
            appState,
            loadOptions.getExtractionOptions(),
            dataSetQueue,
            dataSetMonitorListener,
            loadOptions.getLoadOptions().isIdempotencyRequired(),
            loadOptions.getRunInterval(),
            statusReporter);

    return ccwRifLoadJob;
  }

  /**
   * Creates a {@link CcwRifLoadJobStatusReporter} that either sends progress updates to SQS or
   * simply ignores them depending on the configuration settings.
   *
   * @param loadOptions contains the SQS configuration settings
   * @param awsClientConfig AWS client configuration
   * @param clock used to get current time
   * @return the reporter
   */
  private CcwRifLoadJobStatusReporter createCcwRifLoadJobStatusReporter(
      CcwRifLoadOptions loadOptions, AwsClientConfig awsClientConfig, Clock clock) {
    EventPublisher eventPublisher;
    final var sqsQueueUrl = loadOptions.getSqsQueueUrl().orElse(null);
    if (sqsQueueUrl == null) {
      eventPublisher = new DoNothingEventPublisher();
      LOGGER.info("CCW SQS progress reporting is disabled");
    } else {
      final var sqsClient = AppConfiguration.createSqsClient(awsClientConfig);
      final var sqsDao = new SqsDao(sqsClient);
      eventPublisher = new SqsEventPublisher(sqsDao, sqsQueueUrl);
      LOGGER.info("CCW SQS progress reporting is enabled: queue={}", sqsQueueUrl);
    }
    return new CcwRifLoadJobStatusReporter(eventPublisher, clock);
  }

  /**
   * Registers a JVM shutdown hook that ensures that the application exits gracefully: any
   * in-progress data set batches should always be allowed to complete.
   *
   * <p>The way the JVM handles all of this can be a bit surprising. Some observational notes:
   *
   * <ul>
   *   <li>If a user sends a {@code SIGINT} signal to the application (e.g. by pressing {@code
   *       ctrl+c}), the JVM will do the following: 1) it will run all registered shutdown hooks and
   *       wait for them to complete, and then 2) all threads will be stopped. No exceptions will be
   *       thrown on those threads that they could catch to prevent this; they just die.
   *   <li>If a user sends a more aggressive {@code SIGKILL} signal to the application (e.g. by
   *       using their task manager), the JVM will just immediately stop all threads.
   *   <li>If an application has a poorly designed shutdown hook that never completes, the
   *       application will never stop any of its threads or exit (in response to a {@code SIGINT
   *       }).
   *   <li>I haven't verified this in a while, but the {@code -Xrs} JVM option (which we're not
   *       using) should cause the application to completely ignore {@code SIGINT} signals.
   *   <li>If all of an application's non-daemon threads complete, the application will then run all
   *       registered shutdown hooks and exit.
   *   <li>You can't call {@link System#exit(int)} (to set the exit code) inside a shutdown hook. If
   *       you do, the application will hang forever.
   * </ul>
   *
   * <p>Visible so that it can be disabled during testing using a Mockito spy.
   *
   * @param metrics the {@link MetricRegistry} to log out before the application exits
   * @param pipelineManager the {@link PipelineManager} to be gracefully shut down before the
   *     application exits
   */
  @VisibleForTesting
  void registerShutdownHook(MetricRegistry metrics, PipelineManager pipelineManager) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  /*
                   * Just a reminder: this might take a while! It's going to wait
                   * for any data sets that are being actively processed to finish
                   * processing.
                   */
                  LOGGER.info("Application is shutting down...");
                  pipelineManager.stop();
                  pipelineManager.awaitCompletion();
                  LOGGER.info("Job processing stopped.");

                  if (pipelineManager.getError() != null) {
                    LOGGER.error(
                        "Application encountered exception: message={}",
                        pipelineManager.getError().getMessage());
                  }

                  // Ensure that the final metrics get logged.
                  Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build().report();

                  LOGGER.info("Application has finished shutting down.");

                  /*
                   * We have to do this ourselves (rather than use Logback's DelayingShutdownHook)
                   * to ensure that the logger isn't closed before the above logging.
                   */
                  LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                  logbackContext.stop();
                }));
  }

  /**
   * Log the temporary directory path.
   *
   * @throws IOException pass through
   */
  @SuppressWarnings("java:S5443")
  private void logTempDirectory() throws IOException {
    Path tempFile = Files.createTempFile("a", "x");
    LOGGER.info("Temp dir is {}", tempFile.getParent().toFile().getAbsoluteFile());
    Files.deleteIfExists(tempFile);
  }
}
