package gov.cms.bfd.pipeline.app;

import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.rif.extract.s3.DataSetMonitor;
import gov.cms.bfd.pipeline.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.rif.load.RifLoader;
import gov.cms.bfd.pipeline.rif.load.RifRecordLoadResult;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main application/driver/entry point for the ETL system, which will pull any data stored in
 * the specified S3 bucket, parse it, and push it to the specified database server. See {@link
 * #main(String[])}.
 */
public final class S3ToDatabaseLoadApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3ToDatabaseLoadApp.class);

  /** How often the {@link DataSetMonitor} will wait between scans for new data sets. */
  private static final Duration S3_SCAN_INTERVAL = Duration.ofSeconds(1L);

  /**
   * This {@link System#exit(int)} value should be used when the provided configuration values are
   * incomplete and/or invalid.
   */
  static final int EXIT_CODE_BAD_CONFIG = 1;

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception.
   */
  static final int EXIT_CODE_MONITOR_ERROR = DataSetMonitor.EXIT_CODE_MONITOR_ERROR;

  /**
   * This method is the one that will get called when users launch the application from the command
   * line.
   *
   * @param args (should be empty, as this application accepts configuration via environment
   *     variables)
   */
  public static void main(String[] args) {
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

    MetricRegistry appMetrics = new MetricRegistry();
    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();
    appMetricsReporter.start(1, TimeUnit.HOURS);

    /*
     * Create the services that will be used to handle each stage in the
     * extract, transform, and load process.
     */
    RifFilesProcessor rifProcessor = new RifFilesProcessor();
    RifLoader rifLoader = new RifLoader(appMetrics, appConfig.getLoadOptions());

    /*
     * Create the DataSetMonitorListener that will glue those stages
     * together and run them all for each data set that is found.
     */
    DataSetMonitorListener dataSetMonitorListener =
        new DataSetMonitorListener() {
          @Override
          public void dataAvailable(RifFilesEvent rifFilesEvent) {
            Timer.Context timerDataSet =
                appMetrics
                    .timer(
                        MetricRegistry.name(
                            S3ToDatabaseLoadApp.class.getSimpleName(), "dataSet", "processed"))
                    .time();

            Consumer<Throwable> errorHandler =
                error -> {
                  /*
                   * This will be called on the same thread used to run each
                   * RifLoader task (probably a background one). This is not
                   * the right place to do any error _recovery_ (that'd have
                   * to be inside RifLoader itself), but it is likely the
                   * right place to decide when/if a failure is "bad enough"
                   * that the rest of processing should be stopped. Right now
                   * we stop that way for _any_ failure, but we probably want
                   * to be more discriminating than that.
                   */
                  errorOccurred(error);
                };

            Consumer<RifRecordLoadResult> resultHandler =
                result -> {
                  /*
                   * Don't really *need* to do anything here. The RifLoader
                   * already records metrics for each data set.
                   */
                };

            /*
             * Each ETL stage produces a stream that will be handed off to
             * and processed by the next stage.
             */
            for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
              Slf4jReporter dataSetFileMetricsReporter =
                  Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics())
                      .outputTo(LOGGER)
                      .build();
              dataSetFileMetricsReporter.start(2, TimeUnit.MINUTES);

              RifFileRecords rifFileRecords = rifProcessor.produceRecords(rifFileEvent);
              rifLoader.process(rifFileRecords, errorHandler, resultHandler);

              dataSetFileMetricsReporter.stop();
              dataSetFileMetricsReporter.report();
            }
            timerDataSet.stop();
          }

          /**
           * @see
           *     gov.cms.bfd.pipeline.rif.extract.s3.DataSetMonitorListener#errorOccurred(java.lang.Throwable)
           */
          @Override
          public void errorOccurred(Throwable error) {
            handleUncaughtException(error);
          }

          /** Called when no RIF files are available to process. */
          @Override
          public void noDataAvailable() {
            rifLoader.doIdleTask();
            DataSetMonitorListener.super.noDataAvailable();
          }
        };

    /*
     * Create and start the DataSetMonitor that will find data sets as
     * they're pushed into S3. As each data set is found, it will be handed
     * off to the DataSetMonitorListener to be run through the ETL pipeline.
     */
    DataSetMonitor s3Monitor =
        new DataSetMonitor(
            appMetrics,
            appConfig.getExtractionOptions(),
            (int) S3_SCAN_INTERVAL.toMillis(),
            dataSetMonitorListener);
    registerShutdownHook(appMetrics, s3Monitor);
    s3Monitor.start();
    LOGGER.info("Monitoring S3 for new data sets to process...");

    /*
     * At this point, we're done here with the main thread. From now on, the
     * DataSetMonitor's executor service should be the only non-daemon
     * thread running (and whatever it kicks off). Once/if that thread
     * stops, the application will run all registered shutdown hooks and
     * exit.
     */
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
   * @param s3Monitor the {@link DataSetMonitor} to be gracefully shut down before the application
   *     exits
   */
  private static void registerShutdownHook(MetricRegistry metrics, DataSetMonitor s3Monitor) {
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
                    s3Monitor.stop();

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
  private static void handleUncaughtException(Throwable throwable) {
    /*
     * If an error is caught, log it and then shut everything down.
     */

    LOGGER.error("Data set failed with an unhandled error. Application will exit.", throwable);

    /*
     * This will trigger the shutdown monitors, block until they complete, and then terminate this
     * thread (and all others). Accordingly, we can be doubly sure that the data set processing will
     * be halted: 1) this thread is the DataSetMonitorWorker's and that thread will block then die,
     * and 2) the shutdown monitor will call DataSetMonitor.stop(). Pack it up: we're going home,
     * folks.
     */
    System.exit(EXIT_CODE_MONITOR_ERROR);
  }
}
