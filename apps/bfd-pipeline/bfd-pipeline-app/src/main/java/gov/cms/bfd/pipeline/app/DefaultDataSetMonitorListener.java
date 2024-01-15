package gov.cms.bfd.pipeline.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link DataSetMonitorListener} implementation "glues together" the {@link CcwRifLoadJob}
 * with the {@link RifFilesProcessor} and the {@link RifLoader}: pulling all of the data sets out of
 * S3, parsing them, and then loading them into the BFD database.
 */
public final class DefaultDataSetMonitorListener implements DataSetMonitorListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSetMonitorListener.class);
  public static final String TIMER_PROCESSING =
      MetricRegistry.name(PipelineApplication.class.getSimpleName(), "dataSet", "processed");

  /** Metrics for this class. */
  private final MetricRegistry appMetrics;

  /** Handles processing of new RIF files. */
  private final RifFilesProcessor rifProcessor;

  /** Loads RIF files into the database. */
  private final RifLoader rifLoader;

  /**
   * Initializes the instance.
   *
   * @param appMetrics the {@link MetricRegistry} for the application
   * @param rifProcessor the {@link RifFilesProcessor} for the application
   * @param rifLoader the {@link RifLoader} for the application
   */
  DefaultDataSetMonitorListener(
      MetricRegistry appMetrics, RifFilesProcessor rifProcessor, RifLoader rifLoader) {
    this.appMetrics = appMetrics;
    this.rifProcessor = rifProcessor;
    this.rifLoader = rifLoader;
  }

  @Override
  public void dataAvailable(RifFilesEvent rifFilesEvent) throws Exception {
    Timer.Context timerDataSet = appMetrics.timer(TIMER_PROCESSING).time();

    Exception failure = null;
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      final RifFile rifFile = rifFileEvent.getFile();
      if (!rifFile.requiresProcessing()) {
        LOGGER.info("Skipped previously processed file {}", rifFile.getDisplayName());
        continue;
      }

      Slf4jReporter dataSetFileMetricsReporter =
          Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build();
      dataSetFileMetricsReporter.start(2, TimeUnit.MINUTES);

      try {
        LOGGER.info("Processing file {}", rifFile.getDisplayName());
        rifFile.markAsStarted();

        final RifFileRecords rifFileRecords = rifProcessor.produceRecords(rifFileEvent);
        final long processedCount = rifLoader.processBlocking(rifFileRecords);
        rifFile.markAsProcessed();
        LOGGER.info(
            "Successfully processed {} records in file {}",
            processedCount,
            rifFile.getDisplayName());
      } catch (Exception e) {
        LOGGER.error("Exception while processing file {}", rifFile.getDisplayName());
        failure = e;
      }

      dataSetFileMetricsReporter.stop();
      dataSetFileMetricsReporter.report();

      if (failure != null) {
        if (failure instanceof InterruptedException) {
          LOGGER.info("Stopping due to interrupt.");
        } else {
          LOGGER.info("Stopping due to error.");
        }
        break;
      }
    }
    timerDataSet.stop();
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public void noDataAvailable() {
    // Nothing to do here.
  }
}
