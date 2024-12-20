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
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3RifFile;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link DataSetMonitorListener} implementation "glues together" the {@link CcwRifLoadJob}
 * with the {@link RifFilesProcessor} and the {@link RifLoader}: pulling all of the data sets out of
 * S3, parsing them, and then loading them into the BFD database.
 */
public final class DefaultDataSetMonitorListener implements DataSetMonitorListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSetMonitorListener.class);

  /** Name of timer used to track processing time. */
  public static final String TIMER_PROCESSING =
      MetricRegistry.name(PipelineApplication.class.getSimpleName(), "dataSet", "processed");

  /** Metrics for this class. */
  private final MetricRegistry appMetrics;

  /** Micrometer metrics for this class. */
  private final Metrics metrics;

  /** Handles processing of new RIF files. */
  private final RifFilesProcessor rifProcessor;

  /** Loads RIF files into the database. */
  private final RifLoader rifLoader;

  /**
   * Initializes the instance.
   *
   * @param appMetrics the {@link MetricRegistry} for the application
   * @param micrometerMetrics the {@link MeterRegistry} for the application
   * @param rifProcessor the {@link RifFilesProcessor} for the application
   * @param rifLoader the {@link RifLoader} for the application
   */
  DefaultDataSetMonitorListener(
      MetricRegistry appMetrics,
      MeterRegistry micrometerMetrics,
      RifFilesProcessor rifProcessor,
      RifLoader rifLoader) {
    this.appMetrics = appMetrics;
    this.rifProcessor = rifProcessor;
    this.rifLoader = rifLoader;
    this.metrics = new Metrics(micrometerMetrics);
  }

  @Override
  public void dataAvailable(RifFilesEvent rifFilesEvent) throws Exception {
    Timer.Context timerDataSet = appMetrics.timer(TIMER_PROCESSING).time();

    Exception failure = null;
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      final RifFile rifFile = rifFileEvent.getFile();
      if (!rifFile.requiresProcessing()) {
        LOGGER.info("Skipping previously processed file {}", rifFile.getDisplayName());
        continue;
      }

      Slf4jReporter dataSetFileMetricsReporter =
          Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build();
      dataSetFileMetricsReporter.start(2, TimeUnit.MINUTES);

      final LongTaskTimer.Sample activeTimer = metrics.createActiveTimerForRif(rifFile).start();
      final io.micrometer.core.instrument.Timer.Sample totalTimer =
          io.micrometer.core.instrument.Timer.start();

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

      activeTimer.stop();
      totalTimer.stop(metrics.createTotalTimerForRif(rifFile));

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

  /** Metrics for the {@link DefaultDataSetMonitorListener}'s operations. */
  @RequiredArgsConstructor
  public static final class Metrics {
    /**
     * Name of the per-{@link RifFile} data processing {@link LongTaskTimer}s that actively, at each
     * Micrometer reporting interval, records and reports the duration of processing of a given
     * {@link RifFile}.
     *
     * @implNote We use the class name of {@link CcwRifLoadJob} as the metric prefix instead of
     *     {@link DefaultDataSetMonitorListener} as there are other CCW RIF-related metrics
     *     generated from the {@link CcwRifLoadJob}. Additionally, {@link
     *     DefaultDataSetMonitorListener} is indirectly invoked by {@link CcwRifLoadJob}
     */
    public static final String RIF_FILE_PROCESSING_ACTIVE_TIMER_NAME =
        String.format("%s.rif_file_processing.active", CcwRifLoadJob.class.getSimpleName());

    /**
     * Name of the per-{@link RifFile} data processing {@link Timer}s that report the final duration
     * of processing once the {@link RifFile} is processed.
     *
     * @implNote We use the class name of {@link CcwRifLoadJob} as the metric prefix instead of
     *     {@link DefaultDataSetMonitorListener} as there are other CCW RIF-related metrics
     *     generated from the {@link CcwRifLoadJob}. Additionally, {@link
     *     DefaultDataSetMonitorListener} is indirectly invoked by {@link CcwRifLoadJob}
     */
    public static final String RIF_FILE_PROCESSING_TOTAL_TIMER_NAME =
        String.format("%s.rif_file_processing.total", CcwRifLoadJob.class.getSimpleName());

    /**
     * Tag indicating which data set (identified by its timestamp in S3) a given metric measured.
     */
    private static final String TAG_DATA_SET_TIMESTAMP = "data_set_timestamp";

    /** Tag indicating which RIF file a given metric measured. */
    private static final String TAG_RIF_FILE = "rif_file";

    /**
     * Tag indicating whether the data load associated with the measured metric was synthetic or
     * not.
     */
    private static final String TAG_IS_SYNTHETIC = "is_synthetic";

    /** Tag indicating which {@link DataSetManifest} was associated with the measured metric. */
    private static final String TAG_MANIFEST = "manifest";

    /** Micrometer {@link MeterRegistry} for the Pipeline application. */
    private final MeterRegistry micrometerMetrics;

    /**
     * Creates a {@link LongTaskTimer} for a given {@link RifFile} so that the time it takes to
     * process the RIF can be measured while processing is ongoing. Should be called prior to
     * processing a {@link RifFile}.
     *
     * @param rifFile the {@link RifFile} to time
     * @return the {@link LongTaskTimer} that will be used to measure the ongoing load time of the
     *     {@link RifFile}
     */
    LongTaskTimer createActiveTimerForRif(RifFile rifFile) {
      return LongTaskTimer.builder(RIF_FILE_PROCESSING_ACTIVE_TIMER_NAME)
          .tags(getTags(rifFile))
          .register(micrometerMetrics);
    }

    /**
     * Creates a {@link io.micrometer.core.instrument.Timer} for a given {@link RifFile} so that the
     * total time it takes to process the RIF can be recorded once a {@link RifFile} is done
     * processing. Should be used with {@link
     * io.micrometer.core.instrument.Timer.Sample#stop(io.micrometer.core.instrument.Timer)} after
     * processing a {@link RifFile} to record the total duration.
     *
     * @param rifFile the {@link RifFile} to time
     * @return the {@link LongTaskTimer} that will be used to measure the total time taken to load
     *     the {@link RifFile}
     */
    io.micrometer.core.instrument.Timer createTotalTimerForRif(RifFile rifFile) {
      return io.micrometer.core.instrument.Timer.builder(RIF_FILE_PROCESSING_TOTAL_TIMER_NAME)
          .tags(getTags(rifFile))
          .register(micrometerMetrics);
    }

    /**
     * Returns a {@link List} of default {@link Tag}s that is used to disambiguate a given metric
     * based on its corresponding {@link DataSetManifest}.
     *
     * @param rifFile {@link RifFile} from which several properties will be used to set relevant
     *     {@link Tag}s
     * @return a {@link List} of {@link Tag}s including relevant information from {@code rifFile}
     */
    private List<Tag> getTags(RifFile rifFile) {
      final var rifFileTag = Tag.of(TAG_RIF_FILE, rifFile.getFileType().name().toLowerCase());
      if (rifFile instanceof S3RifFile s3RifFile) {
        final var manifest = s3RifFile.getManifestEntry().getParentManifest();
        final var manifestFullpath = manifest.getIncomingS3Key();
        final var manifestFilename =
            manifestFullpath.substring(manifestFullpath.lastIndexOf("/") + 1);
        return List.of(
            Tag.of(TAG_DATA_SET_TIMESTAMP, manifest.getTimestampText()),
            Tag.of(TAG_IS_SYNTHETIC, Boolean.toString(manifest.isSyntheticData())),
            rifFileTag,
            Tag.of(TAG_MANIFEST, manifestFilename));
      }

      return List.of(rifFileTag);
    }
  }
}
