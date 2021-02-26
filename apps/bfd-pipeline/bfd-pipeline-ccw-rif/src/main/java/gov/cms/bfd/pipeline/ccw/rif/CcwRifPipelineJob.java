package gov.cms.bfd.pipeline.ccw.rif;

import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetQueue;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3RifFile;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.DataSetMoveTask;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
public final class CcwRifPipelineJob implements PipelineJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcwRifPipelineJob.class);

  private static final int GIGA = 1000 * 1000 * 1000;

  /** The directory name that pending/incoming RIF data sets will be pulled from in S3. */
  public static final String S3_PREFIX_PENDING_DATA_SETS = "Incoming";

  /** The directory name that completed/done RIF data sets will be moved to in S3. */
  public static final String S3_PREFIX_COMPLETED_DATA_SETS = "Done";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifPipelineJob} goes and
   * looks, but doesn't find any data sets waiting to be processed.
   */
  public static final String LOG_MESSAGE_NO_DATA_SETS = "No data sets to process found.";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifPipelineJob} starts
   * processing a data set.
   */
  public static final String LOG_MESSAGE_DATA_SET_READY = "Data set ready. Processing it...";

  /**
   * The {@link Logger} message that will be recorded if/when the {@link CcwRifPipelineJob}
   * completes the processing of a data set.
   */
  public static final String LOG_MESSAGE_DATA_SET_COMPLETE = "Data set processing complete.";

  /**
   * A regex for {@link DataSetManifest} keys in S3. Provides capturing groups for the {@link
   * DataSetManifestId} fields.
   */
  public static final Pattern REGEX_PENDING_MANIFEST =
      Pattern.compile("^" + S3_PREFIX_PENDING_DATA_SETS + "\\/(.*)\\/([0-9]+)_manifest\\.xml$");

  public static final Pattern REGEX_COMPLETED_MANIFEST =
      Pattern.compile("^" + S3_PREFIX_COMPLETED_DATA_SETS + "\\/(.*)\\/([0-9]+)_manifest\\.xml$");

  private final MetricRegistry appMetrics;
  private final ExtractionOptions options;
  private final DataSetMonitorListener listener;
  private final S3TaskManager s3TaskManager;

  private final DataSetQueue dataSetQueue;

  /**
   * Constructs a new {@link CcwRifPipelineJob} instance.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param options the {@link ExtractionOptions} to use
   * @param s3TaskManager the {@link S3TaskManager} to use
   * @param listener the {@link DataSetMonitorListener} to send events to
   */
  public CcwRifPipelineJob(
      MetricRegistry appMetrics,
      ExtractionOptions options,
      S3TaskManager s3TaskManager,
      DataSetMonitorListener listener) {
    this.appMetrics = appMetrics;
    this.options = options;
    this.listener = listener;
    this.s3TaskManager = s3TaskManager;

    this.dataSetQueue = new DataSetQueue(appMetrics, options, s3TaskManager);
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#call() */
  @Override
  public PipelineJobOutcome call() throws Exception {
    LOGGER.debug("Scanning for data sets to process...");

    // Update the queue from S3.
    dataSetQueue.updatePendingDataSets();

    // If no manifest was found, we're done (until next time).
    if (dataSetQueue.isEmpty()) {
      LOGGER.debug(LOG_MESSAGE_NO_DATA_SETS);
      listener.noDataAvailable();
      return PipelineJobOutcome.NOTHING_TO_DO;
    }

    // We've found the oldest manifest.
    DataSetManifest manifestToProcess = dataSetQueue.getNextDataSetToProcess().get();
    LOGGER.info(
        "Found data set to process: '{}'."
            + " There were '{}' total pending data sets and '{}' completed ones.",
        manifestToProcess.toString(),
        dataSetQueue.getPendingManifestsCount(),
        dataSetQueue.getCompletedManifestsCount().get());

    /*
     * We've got a data set to process. However, it might still be uploading
     * to S3, so we need to wait for that to complete before we start
     * processing it.
     */
    boolean alreadyLoggedWaitingEvent = false;
    while (!dataSetIsAvailable(manifestToProcess)) {
      /*
       * We're very patient here, so we keep looping, but it's prudent to
       * pause between each iteration. TODO should eventually time out,
       * once we know how long transfers might take
       */
      try {
        if (!alreadyLoggedWaitingEvent) {
          LOGGER.info("Data set not ready. Waiting for it to finish uploading...");
          alreadyLoggedWaitingEvent = true;
        }
        Thread.sleep(1000 * 1);
      } catch (InterruptedException e) {
        /*
         * Many Java applications use InterruptedExceptions to signal
         * that a thread should stop what it's doing ASAP. This app
         * doesn't, so this is unexpected, and accordingly, we don't
         * know what to do. Safest bet is to blow up.
         */
        throw new RuntimeException(e);
      }
    }

    /*
     * Huzzah! We've got a data set to process and we've verified it's all there
     * waiting for us in S3. Now convert it into a RifFilesEvent (containing a List
     * of asynchronously-downloading S3RifFiles.
     */
    LOGGER.info(LOG_MESSAGE_DATA_SET_READY);
    List<S3RifFile> rifFiles =
        manifestToProcess.getEntries().stream()
            .map(
                manifestEntry ->
                    new S3RifFile(
                        appMetrics, manifestEntry, s3TaskManager.downloadAsync(manifestEntry)))
            .collect(Collectors.toList());
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(manifestToProcess.getTimestamp(), new ArrayList<>(rifFiles));

    /*
     * To save time for the next data set, peek ahead at it. If it's available and
     * it looks like there's enough disk space, start downloading it early in the
     * background.
     */
    Optional<DataSetManifest> secondManifestToProcess = dataSetQueue.getSecondDataSetToProcess();
    if (secondManifestToProcess.isPresent() && dataSetIsAvailable(secondManifestToProcess.get())) {
      Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));
      long usableFreeTempSpace;
      try {
        usableFreeTempSpace = Files.getFileStore(tmpdir).getUsableSpace();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      if (usableFreeTempSpace >= (50 * GIGA)) {
        secondManifestToProcess.get().getEntries().stream()
            .forEach(manifestEntry -> s3TaskManager.downloadAsync(manifestEntry));
      }
    }

    /*
     * Now we hand that off to the DataSetMonitorListener, to do the *real*
     * work of actually processing that data set. It's important that we
     * block until it's completed, in order to ensure that we don't end up
     * processing multiple data sets in parallel (which would lead to data
     * consistency problems).
     */
    listener.dataAvailable(rifFilesEvent);
    LOGGER.info(LOG_MESSAGE_DATA_SET_COMPLETE);

    /*
     * Now that the data set has been processed, we need to ensure that we
     * don't end up processing it again. We ensure this two ways: 1) we keep
     * a list of the data sets most recently processed, and 2) we rename the
     * S3 objects that comprise that data set. (#1 is required as S3
     * deletes/moves are only *eventually* consistent, so #2 may not take
     * effect right away.)
     */
    rifFiles.stream().forEach(f -> f.cleanupTempFile());
    dataSetQueue.markProcessed(manifestToProcess);
    s3TaskManager.submit(new DataSetMoveTask(s3TaskManager, options, manifestToProcess));

    return PipelineJobOutcome.WORK_DONE;
  }

  /**
   * @param manifest the {@link DataSetManifest} that lists the objects to verify the presence of
   * @return <code>true</code> if all of the objects listed in the specified manifest can be found
   *     in S3, <code>false</code> if not
   */
  private boolean dataSetIsAvailable(DataSetManifest manifest) {
    /*
     * There are two ways to do this: 1) list all the objects in the data
     * set and verify the ones we're looking for are there after, or 2) try
     * to grab the metadata for each object. Option #2 *should* be simpler,
     * but isn't, because each missing object will result in an exception.
     * Exceptions-as-control-flow is a poor design choice, so we'll go with
     * option #1.
     */

    String dataSetKeyPrefix =
        String.format("%s/%s/", S3_PREFIX_PENDING_DATA_SETS, manifest.getTimestampText());

    ListObjectsV2Request s3BucketListRequest = new ListObjectsV2Request();
    s3BucketListRequest.setBucketName(options.getS3BucketName());
    s3BucketListRequest.setPrefix(dataSetKeyPrefix);
    if (options.getS3ListMaxKeys().isPresent())
      s3BucketListRequest.setMaxKeys(options.getS3ListMaxKeys().get());

    Set<String> dataSetObjectNames = new HashSet<>();
    ListObjectsV2Result s3ObjectListing;
    do {
      s3ObjectListing = s3TaskManager.getS3Client().listObjectsV2(s3BucketListRequest);

      /*
       * Pull the object names from the keys that were returned, by
       * stripping the timestamp prefix and slash from each of them.
       */
      Set<String> namesForObjectsInPage =
          s3ObjectListing.getObjectSummaries().stream()
              .map(s -> s.getKey())
              .peek(s -> LOGGER.debug("Found file: '{}', part of data set: '{}'.", s, manifest))
              .map(k -> k.substring(dataSetKeyPrefix.length()))
              .collect(Collectors.toSet());
      dataSetObjectNames.addAll(namesForObjectsInPage);

      // On to the next page! (If any.)
      s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
    } while (s3ObjectListing.isTruncated());

    for (DataSetManifestEntry manifestEntry : manifest.getEntries()) {
      if (!dataSetObjectNames.contains(manifestEntry.getName())) {
        LOGGER.debug(
            "Waiting for file '{}', part of data set: '{}'.", manifestEntry.getName(), manifest);
        return false;
      }
    }

    return true;
  }
}
