package gov.cms.bfd.pipeline.app;

import static gov.cms.bfd.pipeline.app.AppConfiguration.MICROMETER_CW_ALLOWED_METRIC_NAMES;

import ch.qos.logback.classic.LoggerContext;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
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
import com.zaxxer.hikari.pool.HikariProxyConnection;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
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
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.DatabaseMetaData;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.postgresql.core.BaseConnection;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main application/driver/entry point for the ETL system, which will pull any data stored in
 * the specified S3 bucket, parse it, and push it to the specified database server. See {@link
 * #main(String[])}.
 */
public final class PipelineApplication {
  static final Logger LOGGER = LoggerFactory.getLogger(PipelineApplication.class);

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
   * This method is the one that will get called when users launch the application from the command
   * line.
   *
   * @param args (should be empty, as this application accepts configuration via environment
   *     variables)
   * @throws Exception any unhandled checked {@link Exception}s that are encountered will cause the
   *     application to halt
   */
  public static void main(String[] args) throws Exception {
    LOGGER.info("Application starting up!");
    configureUnexpectedExceptionHandlers();

    AppConfiguration appConfig = null;
    try {
      appConfig = AppConfiguration.readConfigFromEnvironmentVariables();
      LOGGER.info("Application configured: '{}'", appConfig);
    } catch (AppConfigurationException e) {
      System.err.println(e.getMessage());
      LOGGER.warn("Invalid app configuration.", e);
      System.exit(EXIT_CODE_BAD_CONFIG);
    }

    final MetricOptions metricOptions = appConfig.getMetricOptions();
    final var micrometerClock = io.micrometer.core.instrument.Clock.SYSTEM;
    final var appMeters = new CompositeMeterRegistry();

    final var commonTags =
        List.of(
            Tag.of("host", metricOptions.getHostname().orElse("unknown")),
            Tag.of("appName", metricOptions.getNewRelicAppName().orElse("unknown")));

    final var cloudwatchRegistryConfig = AppConfiguration.getCloudWatchRegistryConfig();
    if (cloudwatchRegistryConfig.enabled()) {
      LOGGER.info("Adding CloudWatchMeterRegistry...");
      final var cloudWatchRegistry =
          new CloudWatchMeterRegistry(
              cloudwatchRegistryConfig, micrometerClock, new AmazonCloudWatchAsyncClient());
      cloudWatchRegistry
          .config()
          .meterFilter(
              MeterFilter.denyUnless(
                  id -> MICROMETER_CW_ALLOWED_METRIC_NAMES.contains(id.getName())));
      appMeters.add(cloudWatchRegistry);
      LOGGER.info("Added CloudWatchMeterRegistry.");
    }
    if (AppConfiguration.isJmxMetricsEnabled()) {
      appMeters.add(new JmxMeterRegistry(JmxConfig.DEFAULT, micrometerClock));
      LOGGER.info("Added JmxMeterRegistry.");
    }

    MetricRegistry appMetrics = new MetricRegistry();
    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();

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
      LOGGER.info("Added NewRelicReporter.");
    }

    appMeters.add(getDropWizardMeterRegistry(appMetrics, commonTags, micrometerClock));
    LOGGER.info("Added DropwizardMeterRegistry.");

    new JvmMemoryMetrics().bindTo(appMeters);

    appMetricsReporter.start(1, TimeUnit.HOURS);

    // Create a pooled data source for use by any registered jobs.
    final HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(appConfig.getDatabaseOptions(), appMetrics);

    HikariProxyConnection dbConn = null;
    try {
      dbConn = (HikariProxyConnection) pooledDataSource.getConnection();
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
        LOGGER.info(
            "pgjdbc, logServerDetail: {}",
            ((PgConnection) pSqlConnection).getLogServerErrorDetail());
      }
    } finally {
      if (dbConn != null) {
        try {
          dbConn.close();
        } catch (Exception ex) {
        }
      }
    }

    /*
     * Create all jobs and run their smoke tests.
     */
    final var jobs = createAllJobs(appConfig, appMeters, appMetrics, pooledDataSource);
    if (anySmokeTestFailed(jobs)) {
      LOGGER.info("Pipeline terminating due to smoke test failure.");
      pooledDataSource.close();
      System.exit(EXIT_CODE_SMOKE_TEST_FAILURE);
    }

    /*
     * Create the PipelineManager and register all jobs.
     */
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore(appMetrics);
    PipelineManager pipelineManager = new PipelineManager(appMetrics, jobRecordStore);
    registerShutdownHook(appMetrics, pipelineManager);
    jobs.forEach(pipelineManager::registerJob);
    LOGGER.info("Job processing started.");

    /*
     * At this point, we're done here with the main thread. From now on, the PipelineManager's
     * executor service should be the only non-daemon thread running (and whatever it kicks off).
     * Once/if that thread stops, the application will run all registered shutdown hooks and Wait
     * for the PipelineManager to stop running jobs, and then check to see if we should exit
     * normally with 0 or abnormally with a non-0 because a job failed.
     */
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
  private static DropwizardMeterRegistry getDropWizardMeterRegistry(
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
  private static HierarchicalNameMapper getHierarchicalNameMapper(List<Tag> commonTags) {
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
  private static boolean anySmokeTestFailed(List<PipelineJob<?>> jobs) {
    boolean anyTestFailed = false;
    for (PipelineJob<?> job : jobs) {
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
   * Create all pipeline jobs and return them in a list.
   *
   * @param appConfig our {@link AppConfiguration} for configuring jobs
   * @param appMeters the app meters
   * @param appMetrics our {@link MetricRegistry} for metrics reporting
   * @param pooledDataSource our {@link javax.sql.DataSource}
   * @return list of {@link PipelineJob}s to be registered
   */
  private static List<PipelineJob<?>> createAllJobs(
      AppConfiguration appConfig,
      MeterRegistry appMeters,
      MetricRegistry appMetrics,
      HikariDataSource pooledDataSource) {
    final var jobs = new ArrayList<PipelineJob<?>>();

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
              Clock.systemUTC());

      jobs.add(createCcwRifLoadJob(appConfig.getCcwRifLoadOptions().get(), appState));
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
              Clock.systemUTC());

      final RdaLoadOptions rdaLoadOptions = appConfig.getRdaLoadOptions().get();

      final Optional<RdaServerJob> mockServerJob = rdaLoadOptions.createRdaServerJob();
      if (mockServerJob.isPresent()) {
        jobs.add(mockServerJob.get());
        LOGGER.warn("Registered RdaServerJob.");
      } else {
        LOGGER.info("Skipping RdaServerJob registration - not enabled in app configuration.");
      }

      jobs.add(rdaLoadOptions.createFissClaimsLoadJob(rdaAppState));
      LOGGER.info("Registered RdaFissClaimLoadJob.");

      //      jobs.add(rdaLoadOptions.createMcsClaimsLoadJob(rdaAppState));
      //      LOGGER.info("Registered RdaMcsClaimLoadJob.");
    } else {
      LOGGER.info("RDA API jobs are not enabled in app configuration.");
    }
    return jobs;
  }

  /**
   * Creates the CCW RIF loader job and returns it.
   *
   * @param loadOptions the {@link CcwRifLoadOptions} to use
   * @param appState the {@link PipelineApplicationState} to use
   * @return a {@link CcwRifLoadJob} instance for the application to use
   */
  private static PipelineJob<?> createCcwRifLoadJob(
      CcwRifLoadOptions loadOptions, PipelineApplicationState appState) {
    /*
     * Create the services that will be used to handle each stage in the extract, transform, and
     * load process.
     */
    S3TaskManager s3TaskManager =
        new S3TaskManager(appState.getMetrics(), loadOptions.getExtractionOptions());
    RifFilesProcessor rifProcessor = new RifFilesProcessor();
    RifLoader rifLoader = new RifLoader(loadOptions.getLoadOptions(), appState);

    /*
     * Create the DataSetMonitorListener that will glue those stages together and run them all for
     * each data set that is found.
     */
    DataSetMonitorListener dataSetMonitorListener =
        new DefaultDataSetMonitorListener(
            appState.getMetrics(),
            PipelineApplication::handleUncaughtException,
            rifProcessor,
            rifLoader);
    CcwRifLoadJob ccwRifLoadJob =
        new CcwRifLoadJob(
            appState,
            loadOptions.getExtractionOptions(),
            s3TaskManager,
            dataSetMonitorListener,
            loadOptions.getLoadOptions().isIdempotencyRequired());

    return ccwRifLoadJob;
  }

  /**
   * Registers a JVM shutdown hook that ensures that the application exits gracefully: any
   * in-progress data set batches should always be allowed to complete.
   *
   * <p>The way the JVM handles all of this can be a bit surprising. Some observational notes:
   *
   * <ul>
   *   <li>If a user sends a <code>SIGINT</code> signal to the application (e.g. by pressing <code>
   *       ctrl+c</code>), the JVM will do the following: 1) it will run all registered shutdown
   *       hooks and wait for them to complete, and then 2) all threads will be stopped. No
   *       exceptions will be thrown on those threads that they could catch to prevent this; they
   *       just die.
   *   <li>If a user sends a more aggressive <code>SIGKILL</code> signal to the application (e.g. by
   *       using their task manager), the JVM will just immediately stop all threads.
   *   <li>If an application has a poorly designed shutdown hook that never completes, the
   *       application will never stop any of its threads or exit (in response to a <code>SIGINT
   *       </code>).
   *   <li>I haven't verified this in a while, but the <code>-Xrs</code> JVM option (which we're not
   *       using) should cause the application to completely ignore <code>SIGINT</code> signals.
   *   <li>If all of an application's non-daemon threads complete, the application will then run all
   *       registered shutdown hooks and exit.
   *   <li>You can't call {@link System#exit(int)} (to set the exit code) inside a shutdown hook. If
   *       you do, the application will hang forever.
   * </ul>
   *
   * @param metrics the {@link MetricRegistry} to log out before the application exits
   * @param pipelineManager the {@link PipelineManager} to be gracefully shut down before the
   *     application exits
   */
  private static void registerShutdownHook(
      MetricRegistry metrics, PipelineManager pipelineManager) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    LOGGER.info("Application is shutting down...");

                    /*
                     * Just a reminder: this might take a while! It's going to wait
                     * for any data sets that are being actively processed to finish
                     * processing.
                     */
                    pipelineManager.stop();

                    // Ensure that the final metrics get logged.
                    Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build().report();

                    LOGGER.info("Application has finished shutting down.");

                    /*
                     * We have to do this ourselves (rather than use Logback's DelayingShutdownHook)
                     * to ensure that the logger isn't closed before the above logging.
                     */
                    LoggerContext logbackContext =
                        (LoggerContext) LoggerFactory.getILoggerFactory();
                    logbackContext.stop();
                  }
                }));
  }

  /**
   * Registers {@link UncaughtExceptionHandler}s for the main thread, and a default one for all
   * other threads. These are just here to make sure that things don't die silently, but instead at
   * least log any errors that have occurred.
   */
  private static void configureUnexpectedExceptionHandlers() {
    Thread.currentThread()
        .setUncaughtExceptionHandler(
            new UncaughtExceptionHandler() {
              @Override
              public void uncaughtException(Thread t, Throwable e) {
                handleUncaughtException(e);
              }
            });
    Thread.setDefaultUncaughtExceptionHandler(
        new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable e) {
            handleUncaughtException(e);
          }
        });
  }

  /**
   * Call this method to deal with any uncaught exceptions. It'll log the error and then shut things
   * down gracefully.
   *
   * @param throwable the error that occurred
   */
  static void handleUncaughtException(Throwable throwable) {
    /*
     * If an error is caught, log it and then shut everything down.
     */

    LOGGER.error("Data set failed with an unhandled error. Application will exit.", throwable);

    /*
     * This will trigger the shutdown monitors, block until they complete, and then terminate this
     * thread (and all others). Accordingly, we can be doubly sure that the data set processing will
     * be halted: 1) this thread is the CcwRifLoadJob's and that thread will block then die,
     * and 2) the shutdown monitor will call PipelineManager.stop(). Pack it up: we're going home,
     * folks.
     */
    System.exit(EXIT_CODE_JOB_FAILED);
  }
}
