package gov.cms.bfd.pipeline.ccw.rif;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetQueue;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3RifFile;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link PipelineJob} checks for and, if found, processes data that has been pushed from CMS'
 * Chronic Conditions Data (CCW) into an AWS S3 bucket. The data in S3 will be structured as
 * follows:
 *
 * <ul>
 *   <li>Amazon S3 Bucket: <code>&lt;s3-bucket-name&gt;</code>
 *       <ul>
 *         <li><code>1997-07-16T19:20:30Z</code>
 *             <ul>
 *               <li><code>Incoming</code>
 *                   <ul>
 *                     <li><code>23_manifest.xml</code>
 *                     <li><code>beneficiaries_42.rif</code>
 *                     <li><code>bcarrier_58.rif</code>
 *                     <li><code>pde_93.rif</code>
 *                   </ul>
 *               <li><code>Done</code>
 *                   <ul>
 *                     <li><code>64_manifest.xml</code>
 *                     <li><code>beneficiaries_45.rif</code>
 *                   </ul>
 *             </ul>
 *       </ul>
 * </ul>
 *
 * <p>In that structure, there will be one top-level directory in the bucket for each data set that
 * has yet to be completely processed by the ETL pipeline. Its name will be an <a
 * href="https://www.w3.org/TR/NOTE-datetime">ISO 8601 date and time</a> expressed in UTC, to a
 * precision of at least seconds. This will represent (roughly) the time that the data set was
 * created. Within each of those directories will be manifest files and the RIF files that they
 * reference.
 */
public final class CcwRifLoadJob implements PipelineJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcwRifLoadJob.class);

  /**
   * Maximum age of a manifest for it to be considered for processing. Intended to keep database
   * queries reasonable.
   */
  public static final Duration MAX_MANIFEST_AGE = Duration.ofDays(60);

  /**
   * Minimum amount of free disk space (in bytes) to allow pre-fetch of second data set while
   * current one is being processed.
   */
  public static final long MIN_BYTES_FOR_SECOND_DATA_SET_DOWNLOAD = 50 * FileUtils.ONE_GB;

  /** The directory name that pending/incoming RIF data sets will be pulled from in S3. */
  public static final String S3_PREFIX_PENDING_DATA_SETS = "Incoming";

  /**
   * The directory name that pending/incoming synthetic RIF data sets can be pulled from in S3. In
   * essence this is just a second Incoming folder we can use for organization; it is not
   * functionally different from {@link #S3_PREFIX_PENDING_DATA_SETS}.
   */
  public static final String S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS = "Synthetic/Incoming";

  /** The directory name that completed/done RIF data sets will be moved to in S3. */
  public static final String S3_PREFIX_COMPLETED_DATA_SETS = "Done";

  /**
   * The directory name that completed/done RIF data sets loaded from {@link
   * #S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS} will be moved to in S3.
   */
  public static final String S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS = "Synthetic/Done";

  /**
   * The directory name that failed RIF data sets loaded from {@link
   * #S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS} will be moved to in S3.
   */
  public static final String S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS = "Synthetic/Failed";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifLoadJob} goes and
   * looks, but doesn't find any data sets waiting to be processed.
   */
  public static final String LOG_MESSAGE_NO_DATA_SETS = "No data sets to process found.";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifLoadJob} starts
   * processing a data set.
   */
  public static final String LOG_MESSAGE_DATA_SET_READY = "Data set ready. Processing it...";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifLoadJob} completes
   * the processing of a data set.
   */
  public static final String LOG_MESSAGE_DATA_SET_COMPLETE = "Data set processing complete.";

  /** Set of status values that indicate a data file requires processing. */
  public static final Set<S3DataFile.FileStatus> REQUIRED_PROCESSING_STATUS_VALUES =
      Set.of(S3DataFile.FileStatus.DISCOVERED, S3DataFile.FileStatus.STARTED);

  /**
   * A regex for {@link DataSetManifest} keys in S3. Provides capturing groups for the {@link
   * DataSetManifestId} fields.
   */
  public static final Pattern REGEX_PENDING_MANIFEST =
      Pattern.compile(
          "^("
              + S3_PREFIX_PENDING_DATA_SETS
              + "|"
              + S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
              + ")/(.*)/([0-9]+)_manifest\\.xml$");

  /**
   * A regex that can be used for checking for a manifest in the {@link
   * #S3_PREFIX_COMPLETED_DATA_SETS} location.
   */
  public static final Pattern REGEX_COMPLETED_MANIFEST =
      Pattern.compile(
          "^("
              + S3_PREFIX_COMPLETED_DATA_SETS
              + "|"
              + S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
              + ")/(.*)/([0-9]+)_manifest\\.xml$");

  /** The application metrics. */
  private final MetricRegistry appMetrics;

  /** The extraction options. */
  private final ExtractionOptions options;

  /** The data set listener for finding new files to load. */
  private final DataSetMonitorListener listener;

  /** The application state. */
  private final PipelineApplicationState appState;

  /** If the application is in idempotent mode. */
  private final boolean isIdempotentMode;

  /** Time between runs of the {@link CcwRifLoadJob}. Empty means to run exactly once. */
  private final Optional<Duration> runInterval;

  /** Used to send status updates to external processes. */
  private final CcwRifLoadJobStatusReporter statusReporter;

  /** The queue of S3 data to be processed. */
  private final DataSetQueue dataSetQueue;

  /** Maintains the background thread used to download files asynchronously. */
  private final ExecutorService downloadService;

  /**
   * Constructs a new instance. The {@link S3TaskManager} will be automatically shut down when this
   * job's {@link #close} method is called.
   *
   * @param appState the {@link PipelineApplicationState} for the overall application
   * @param options the {@link ExtractionOptions} to use
   * @param dataSetQueue the {@link DataSetQueue} to use
   * @param listener the {@link DataSetMonitorListener} to send events to
   * @param isIdempotentMode the {@link boolean} TRUE if running in idempotent mode
   * @param runInterval used to construct the job schedule
   * @param statusReporter used to update external processes with our latest status
   */
  public CcwRifLoadJob(
      PipelineApplicationState appState,
      ExtractionOptions options,
      DataSetQueue dataSetQueue,
      DataSetMonitorListener listener,
      boolean isIdempotentMode,
      Optional<Duration> runInterval,
      CcwRifLoadJobStatusReporter statusReporter) {
    this.appState = appState;
    this.appMetrics = appState.getMetrics();
    this.options = options;
    this.dataSetQueue = dataSetQueue;
    this.listener = listener;
    this.isIdempotentMode = isIdempotentMode;
    this.runInterval = runInterval;
    this.statusReporter = statusReporter;
    downloadService = Executors.newSingleThreadScheduledExecutor();
  }

  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return runInterval.map(
        interval -> new PipelineJobSchedule(interval.toMillis(), ChronoUnit.MILLIS));
  }

  @Override
  public boolean isInterruptible() {
    /*
     * TODO While the RIF pipeline itself is interruptable now, the S3 transfers are not.
     *  For now we will leave interrupts disabled and revisit the need for moving files
     *  between S3 buckets in a later PR. Expected to be changed as part of BFD-3129.
     */
    return false;
  }

  @Override
  public PipelineJobOutcome call() throws Exception {
    LOGGER.debug("Scanning for data sets to process...");

    final Instant startTime = appState.getClock().instant();

    // Get list of eligible manifests from database.
    statusReporter.reportCheckingBucketForManifest();
    final var eligibleManifests = readEligibleManifests(startTime);

    // If no manifest was found, we're done (until next time).
    if (eligibleManifests.isEmpty()) {
      LOGGER.debug(LOG_MESSAGE_NO_DATA_SETS);
      listener.noDataAvailable();
      statusReporter.reportNothingToDo();
      return PipelineJobOutcome.NOTHING_TO_DO;
    }

    // We've found the oldest manifest.
    S3ManifestFile manifestRecord = eligibleManifests.getFirst().manifestRecord();
    DataSetManifest manifestToProcess = eligibleManifests.getFirst().manifest();
    LOGGER.info(
        "Found data set to process: '{}'." + " There were '{}' total pending data sets.",
        manifestToProcess.toString(),
        eligibleManifests.size());

    /*
     * We've got a data set to process. However, it might still be uploading
     * to S3, so we need to wait for that to complete before we start
     * processing it.
     */
    boolean alreadyLoggedWaitingEvent = false;
    statusReporter.reportAwaitingManifestData(manifestRecord.getS3Key());
    while (!dataSetQueue.allEntriesExistInS3(manifestRecord)) {
      /*
       * We're very patient here, so we keep looping, but it's prudent to
       * pause between each iteration. TODO should eventually time out,
       * once we know how long transfers might take
       */
      if (!alreadyLoggedWaitingEvent) {
        LOGGER.info("Data set not ready. Waiting for it to finish uploading...");
        alreadyLoggedWaitingEvent = true;
      }
      Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    }

    /*
     * Huzzah! We've got a data set to process and we've verified it's all there
     * waiting for us in S3. Now convert it into a RifFilesEvent (containing a List
     * of asynchronously-downloading S3RifFiles.
     */
    LOGGER.info(LOG_MESSAGE_DATA_SET_READY);
    LOGGER.info("Data set syntheticData indicator is: {}", manifestToProcess.isSyntheticData());

    /*
     * The {@link DataSetManifest} can have an optional element {@link
     * PreValidationProperties}
     * which contains elements that can be used to preform a pre-validation
     * verification prior
     * to beginning the actual processing (loading) of data.
     *
     * For example, checking if a range of bene_id(s) will cause a database key
     * constraint
     * violation during an INSERT operation. However, if running in idempotent
     * mode, it will be acceptable to run 'as is' since this is probably a re-run
     * of a previous load and does not represent a pure INSERT(ing) of data.
     */
    boolean preValidationOK = true;
    if (manifestToProcess.getPreValidationProperties().isPresent()) {
      preValidationOK = (isIdempotentMode || checkPreValidationProperties(manifestToProcess));
    }

    /*
     * If pre-validation succeeded, then normal processing continues; however, if it
     * has failed
     * (currently only Synthea has pre-validation), then we'll skip over the normal
     * processing
     * and go directly to where the manifest and associated RIF files are (re-)moved
     * from the
     * incoming bucket folder.
     */
    if (preValidationOK) {
      List<S3RifFile> rifFiles =
          manifestToProcess.getEntries().stream()
              .flatMap(
                  manifestEntry ->
                      convertManifestEntryToS3RifFile(manifestRecord, manifestEntry).stream())
              .toList();

      RifFilesEvent rifFilesEvent =
          new RifFilesEvent(
              manifestToProcess.getTimestamp(),
              manifestToProcess.isSyntheticData(),
              new ArrayList<>(rifFiles));

      /*
       * To save time for the next data set, peek ahead at it. If it's available and
       * it looks like there's enough disk space, start downloading it early in the
       * background.
       */
      if (eligibleManifests.size() > 1) {
        DataSetQueue.Manifest secondManifestToProcess = eligibleManifests.get(1);
        final long usableFreeTempSpace = dataSetQueue.getAvailableDiskSpaceInBytes();
        if (usableFreeTempSpace >= MIN_BYTES_FOR_SECOND_DATA_SET_DOWNLOAD) {
          secondManifestToProcess.manifestRecord().getDataFiles().stream()
              .filter(this::isProcessingRequired)
              .forEach(
                  s3DataFile -> {
                    downloadService.submit(() -> dataSetQueue.downloadManifestEntry(s3DataFile));
                  });
        }
      }

      /*
       * Now we hand that off to the DataSetMonitorListener, to do the *real*
       * work of actually processing that data set. It's important that we
       * block until it's completed, in order to ensure that we don't end up
       * processing multiple data sets in parallel (which would lead to data
       * consistency problems).
       */
      statusReporter.reportProcessingManifestData(manifestToProcess.getIncomingS3Key());
      dataSetQueue.markAsStarted(manifestRecord);
      listener.dataAvailable(rifFilesEvent);
      statusReporter.reportCompletedManifest(manifestToProcess.getIncomingS3Key());
      dataSetQueue.markAsProcessed(manifestRecord);
      LOGGER.info(LOG_MESSAGE_DATA_SET_COMPLETE);

      /*
       * Now that the data set has been processed, we need to ensure that we
       * don't end up processing it again. We ensure this two ways: 1) we keep
       * a list of the data sets most recently processed, and 2) we rename the
       * S3 objects that comprise that data set. (#1 is required as S3
       * deletes/moves are only *eventually* consistent, so #2 may not take
       * effect right away.)
       */
      rifFiles.forEach(S3RifFile::cleanupTempFile);
    } else {
      // TODO BEGIN remove once S3 file moves are no longer necessary.
      // Expected to be changed as part of BFD-3129.
      /*
       * If here, Synthea pre-validation has failed; we want to move the S3 incoming
       * files to a failed folder; so instead of moving files to a done folder we'll just
       * replace the manifest's notion of its Done folder to a Failed folder.
       */
      manifestToProcess.setManifestKeyDoneLocation(S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS);
      // TODO END remove once S3 file moves are no longer necessary.

      /*
       * If here, Synthea pre-validation has failed; we want to mark the data set as rejected in the database.
       */
      dataSetQueue.markAsRejected(manifestRecord);
    }

    // TODO BEGIN remove once S3 file moves are no longer necessary.
    // Expected to be changed as part of BFD-3129.
    dataSetQueue.moveManifestFilesInS3(manifestToProcess);
    // TODO END remove once S3 file moves are no longer necessary.

    return PipelineJobOutcome.WORK_DONE;
  }

  /**
   * Shuts down our {@link S3TaskManager} and clears our S3 files cache. If any download or move
   * tasks are still running this method will wait for them to complete before returning.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void close() throws Exception {
    final var closer = new MultiCloser();
    closer.close(
        () -> {
          downloadService.shutdown();
          downloadService.awaitTermination(1, TimeUnit.HOURS);
        });
    closer.close(dataSetQueue::close);
    closer.finish();
  }

  /**
   * Perform pre-validation for a data load if the {@link
   * DataSetManifest#getPreValidationProperties()} has content. At this time, only Synthea-based
   * {@link DataSetManifest} have content that can be used for pre-validation.
   *
   * @param manifest the {@link DataSetManifest} that lists pre-validation properties to verify
   * @return <code>true</code> if all of the pre-validation parameters listed in the manifest do not
   *     introduce potential data loading issues, <code>false</code> if not
   */
  private boolean checkPreValidationProperties(DataSetManifest manifest) throws Exception {
    LOGGER.info(
        "PreValidationProperties found in manifest, ID: {}; verifying efficacy...",
        manifest.getId());

    // for now only Synthea manifests will have PreValidationProperties
    if (!manifest.isSyntheticData()) {
      return true;
    }

    /* we are processing Synthea data...setup a pre-validation interface. */
    CcwRifLoadPreValidateInterface preValInterface = new CcwRifLoadPreValidateSynthea();
    // initialize the interface with the appState
    preValInterface.init(appState);

    // perform whatever vaidation is appropriate
    LOGGER.info(
        "Synthea pre-validation being performed by: {}...", preValInterface.getClass().getName());
    return preValInterface.isValid(manifest);
  }

  /**
   * Finds and returns the {@link S3DataFile} from {@link S3ManifestFile#dataFiles} that corresponds
   * to the provided {@link DataSetManifestEntry}. This should never fail.
   *
   * @param manifest the manifest record
   * @param entry the entry to search for
   * @return the entry that was found
   * @throws BadCodeMonkeyException if the manifest record is out of sync with the manifest it
   *     represents
   */
  private S3DataFile selectS3DataRecordForEntry(
      S3ManifestFile manifest, DataSetManifestEntry entry) {
    if (manifest.getDataFiles().size() != entry.getParentManifest().getEntries().size()) {
      throw new BadCodeMonkeyException(
          String.format(
              "mismatch in number of entries: database=%d xml=%d",
              entry.getParentManifest().getEntries().size(), manifest.getDataFiles().size()));
    }
    for (S3DataFile dataFile : manifest.getDataFiles()) {
      if (dataFile.getFileName().equals(entry.getName())) {
        return dataFile;
      }
    }
    throw new BadCodeMonkeyException(
        String.format("no data file found for entry name: name=%s", entry.getName()));
  }

  /**
   * Used as a lambda when calling {@link DataSetQueue#readEligibleManifests} to ensure that only
   * manifests that pass our filter criteria and are not intended to be loaded in the future will be
   * processed.
   *
   * @param dataSetManifest the manifest to test
   * @return true if manifest passes our filter criteria and is not a future manifest
   */
  private boolean isEligibleManifest(DataSetManifest dataSetManifest) {
    return options.getDataSetFilter().test(dataSetManifest)
        && !dataSetManifest.getId().isFutureManifest();
  }

  /**
   * Reads all manifests that are currently eligible for processing.
   *
   * @param now current time used in search
   * @return list of eligible manifests
   * @throws IOException pass through if lookup fails
   */
  private List<DataSetQueue.Manifest> readEligibleManifests(Instant now) throws IOException {
    final Instant minEligibleTime = now.minus(MAX_MANIFEST_AGE);
    return dataSetQueue.readEligibleManifests(now, minEligibleTime, this::isEligibleManifest, 500);
  }

  /**
   * Adds a task to the download queue to download the data file from S3 and returns a {@link
   * S3RifFile} containing a {@link Future} to access the result.
   *
   * @param manifestRecord database record for the manifest
   * @param manifestEntry manifest entry for the data file
   * @return empty if file does not require processing, the {@link S3RifFile} if it does
   */
  private Optional<S3RifFile> convertManifestEntryToS3RifFile(
      S3ManifestFile manifestRecord, DataSetManifestEntry manifestEntry) {
    final var dataFileRecord = selectS3DataRecordForEntry(manifestRecord, manifestEntry);
    if (!isProcessingRequired(dataFileRecord)) {
      return Optional.empty();
    }

    final Future<DataSetQueue.ManifestEntry> downloadResult =
        downloadService.submit(() -> dataSetQueue.downloadManifestEntry(dataFileRecord));
    return Optional.of(new S3RifFile(appMetrics, manifestEntry, downloadResult));
  }

  /**
   * Returns true if the status of this data file indicates that it has not yet been fully
   * processed.
   *
   * @param dataFileRecord data file to check
   * @return true if the file requires processing
   */
  private boolean isProcessingRequired(S3DataFile dataFileRecord) {
    if (REQUIRED_PROCESSING_STATUS_VALUES.contains(dataFileRecord.getStatus())) {
      return true;
    }
    LOGGER.info(
        "Skipping already processed data file: index={} type={} name={}",
        dataFileRecord.getIndex(),
        dataFileRecord.getFileType(),
        dataFileRecord.getFileName());
    return false;
  }
}
