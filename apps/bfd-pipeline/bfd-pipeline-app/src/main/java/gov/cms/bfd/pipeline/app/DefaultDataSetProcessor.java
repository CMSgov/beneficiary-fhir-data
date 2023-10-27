package gov.cms.bfd.pipeline.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.DataSetProcessor;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link DataSetProcessor} implementation "glues together" the {@link CcwRifLoadJob} with the
 * {@link RifFilesProcessor} and the {@link RifLoader}: pulling all of the data sets out of S3,
 * parsing them, and then loading them into the BFD database.
 */
public final class DefaultDataSetProcessor implements DataSetProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSetProcessor.class);

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
  DefaultDataSetProcessor(
      MetricRegistry appMetrics, RifFilesProcessor rifProcessor, RifLoader rifLoader) {
    this.appMetrics = appMetrics;
    this.rifProcessor = rifProcessor;
    this.rifLoader = rifLoader;
  }

  @Override
  public void processDataSet(RifFilesEvent rifFilesEvent) throws Exception {
    Timer.Context timerDataSet =
        appMetrics
            .timer(
                MetricRegistry.name(
                    PipelineApplication.class.getSimpleName(), "dataSet", "processed"))
            .time();

    Exception failure = null;
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      Slf4jReporter dataSetFileMetricsReporter =
          Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build();
      dataSetFileMetricsReporter.start(2, TimeUnit.MINUTES);

      try {
        final RifFileRecords rifFileRecords = rifProcessor.produceRecords(rifFileEvent);
        final long processedCount = rifLoader.processBlocking(rifFileRecords);
        LOGGER.info(
            "Successfully processed {} records in file {}",
            processedCount,
            rifFileEvent.getFile().getDisplayName());
      } catch (Exception e) {
        LOGGER.error("Exception while processing file {}", rifFileEvent.getFile().getDisplayName());
        failure = e;
      }

      dataSetFileMetricsReporter.stop();
      dataSetFileMetricsReporter.report();

      if (failure != null) {
        LOGGER.info("Stopping due to error.");
        break;
      }
    }
    timerDataSet.stop();
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public void noDataToProcess() {
    // Nothing to do here.
  }
}
