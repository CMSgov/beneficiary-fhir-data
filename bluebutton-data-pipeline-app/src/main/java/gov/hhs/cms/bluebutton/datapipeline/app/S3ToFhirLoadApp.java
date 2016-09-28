package gov.hhs.cms.bluebutton.datapipeline.app;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirBundleResult;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitor;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorListener;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;

/**
 * The main application/driver/entry point for the ETL system, which will pull
 * any data stored in the specified S3 bucket, transform it, and push it to the
 * specified FHIR server. See {@link #main(String[])}.
 */
public final class S3ToFhirLoadApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3ToFhirLoadApp.class);

	/**
	 * How often the {@link DataSetMonitor} will wait between scans for new data
	 * sets.
	 */
	private static final Duration S3_SCAN_INTERVAL = Duration.ofSeconds(1L);

	/**
	 * This {@link System#exit(int)} value should be used when the provided
	 * configuration values are incomplete and/or invalid.
	 */
	static final int EXIT_CODE_BAD_CONFIG = 1;

	/**
	 * This {@link System#exit(int)} value should be used when the application
	 * exits due to an unhandled exception.
	 */
	static final int EXIT_CODE_MONITOR_ERROR = DataSetMonitor.EXIT_CODE_MONITOR_ERROR;

	/**
	 * This method is the one that will get called when users launch the
	 * application from the command line.
	 * 
	 * @param args
	 *            (should be empty, as this application accepts configuration
	 *            via environment variables)
	 */
	public static void main(String[] args) {
		configureUnexpectedExceptionHandlers();

		AppConfiguration appConfig = null;
		try {
			appConfig = AppConfiguration.readConfigFromEnvironmentVariables();
		} catch (AppConfigurationException e) {
			System.err.println(e.getMessage());
			LOGGER.warn("Invalid app configuration.", e);
			System.exit(EXIT_CODE_BAD_CONFIG);
		}

		MetricRegistry metrics = new MetricRegistry();
		metrics.registerAll(new MemoryUsageGaugeSet());
		metrics.registerAll(new GarbageCollectorMetricSet());
		Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
		metricsReporter.start(300, TimeUnit.SECONDS);

		/*
		 * Create the services that will be used to handle each stage in the
		 * extract, transform, and load process.
		 */
		RifFilesProcessor rifProcessor = new RifFilesProcessor();
		DataTransformer rifToFhirTransformer = new DataTransformer();
		FhirLoader fhirLoader = new FhirLoader(metrics, appConfig.getLoadOptions());

		/*
		 * Create the DataSetMonitorListener that will glue those stages
		 * together and run them all for each data set that is found.
		 */
		DataSetMonitorListener dataSetMonitorListener = new DataSetMonitorListener() {
			@Override
			public void dataAvailable(RifFilesEvent rifFilesEvent) {
				/*
				 * Each ETL stage produces a stream that will be handed off to
				 * and processed by the next stage.
				 */
				Stream<RifRecordEvent<?>> rifRecordsStream = rifProcessor.process(rifFilesEvent);
				Stream<TransformedBundle> transformedFhirStream = rifToFhirTransformer.transform(rifRecordsStream);
				Stream<FhirBundleResult> fhirLoadResultsStream = fhirLoader.process(transformedFhirStream);

				/*
				 * Unless those streams are terminated by wiring them up to a
				 * terminal operation (i.e. something that consumes them),
				 * they'll just sit there, doing nothing. That's really the main
				 * point of this `forEach(...)` block here.
				 */
				fhirLoadResultsStream.forEach(fhirResult -> {
					/*
					 * Don't really *need* to do anything here. The FhirLoader
					 * already records metrics for each data set.
					 */
					LOGGER.debug("FHIR bundle load returned %d response entries.",
							fhirResult.getOutputBundle().getTotal());

					/*
					 * TODO This is too late to do any error *recovery*, but the
					 * result objects should contain some information that we
					 * can check to see if a load operation
					 * really-actually-couldn't-recover-failed. If one of those
					 * is detected, we should stop processing and bail out of
					 * the application. Or perhaps we should rely on the
					 * FhirLoader to blow up if that happens? Whatever. One of
					 * those.
					 */
				});
			}

			/**
			 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorListener#errorOccurred(java.lang.Throwable)
			 */
			@Override
			public void errorOccurred(Throwable error) {
				/*
				 * If an error is caught, log it and then shut everything down.
				 */

				LOGGER.error("Data set failed with an unhandled error. Application will exit.", error);

				/*
				 * This will trigger the shutdown monitors, block until they
				 * complete, and then terminate this thread (and all others).
				 * Accordingly, we can be doubly sure that the data set
				 * processing will be halted: 1) this thread is the
				 * DataSetMonitorWorker's and that thread will block then die,
				 * and 2) the shutdown monitor will call DataSetMonitor.stop().
				 * Pack it up: we're going home, folks.
				 */
				System.exit(EXIT_CODE_MONITOR_ERROR);
			}
		};

		/*
		 * Create and start the DataSetMonitor that will find data sets as
		 * they're pushed into S3. As each data set is found, it will be handed
		 * off to the DataSetMonitorListener to be run through the ETL pipeline.
		 */
		DataSetMonitor s3Monitor = new DataSetMonitor(appConfig.getS3BucketName(), (int) S3_SCAN_INTERVAL.toMillis(),
				dataSetMonitorListener);
		registerShutdownHook(metrics, s3Monitor);
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
	 * <p>
	 * Registers a JVM shutdown hook that ensures that the application exits
	 * gracefully: any in-progress data set batches should always be allowed to
	 * complete.
	 * </p>
	 * <p>
	 * The way the JVM handles all of this can be a bit surprising. Some
	 * observational notes:
	 * </p>
	 * <ul>
	 * <li>If a user sends a <code>SIGINT</code> signal to the application (e.g.
	 * by pressing <code>ctrl+c</code>), the JVM will do the following: 1) it
	 * will run all registered shutdown hooks and wait for them to complete, and
	 * then 2) all threads will be stopped. No exceptions will be thrown on
	 * those threads that they could catch to prevent this; they just die.</li>
	 * <li>If a user sends a more aggressive <code>SIGKILL</code> signal to the
	 * application (e.g. by using their task manager), the JVM will just
	 * immediately stop all threads.</li>
	 * <li>If an application has a poorly designed shutdown hook that never
	 * completes, the application will never stop any of its threads or exit (in
	 * response to a <code>SIGINT</code>).</li>
	 * <li>I haven't verified this in a while, but the <code>-Xrs</code> JVM
	 * option (which we're not using) should cause the application to completely
	 * ignore <code>SIGINT</code> signals.</li>
	 * <li>If all of an application's non-daemon threads complete, the
	 * application will then run all registered shutdown hooks and exit.</li>
	 * <li>You can't call {@link System#exit(int)} (to set the exit code) inside
	 * a shutdown hook. If you do, the application will hang forever.</li>
	 * </ul>
	 * 
	 * @param metrics
	 *            the {@link MetricRegistry} to log out before the application
	 *            exits
	 * @param s3Monitor
	 *            the {@link DataSetMonitor} to be gracefully shut down before
	 *            the application exits
	 */
	private static void registerShutdownHook(MetricRegistry metrics, DataSetMonitor s3Monitor) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
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
			}
		}));
	}

	/**
	 * Registers {@link UncaughtExceptionHandler}s for the main thread, and a
	 * default one for all other threads. These are just here to make sure that
	 * things don't die silently, but instead at least log any errors that have
	 * occurred.
	 */
	private static void configureUnexpectedExceptionHandlers() {
		Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				LOGGER.error("Uncaught exception on main thread. Main thread stopping.", e);
			}
		});
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				/*
				 * Just a note on something that I found a bit surprising: this
				 * won't be triggered for errors that occur in the
				 * DataSetMonitorWorker, as the ScheduledExecutorService
				 * swallows those exceptions.
				 */

				LOGGER.error("Uncaught exception on non-main thread.", e);
			}
		});
	}
}
