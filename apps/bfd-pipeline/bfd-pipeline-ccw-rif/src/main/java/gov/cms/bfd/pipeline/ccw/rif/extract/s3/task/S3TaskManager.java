package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.OldDataSetQueue;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.TaskExecutor;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles the execution and management of S3-related tasks. */
public final class S3TaskManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3TaskManager.class);

  /** The metrics registry. */
  private final MetricRegistry appMetrics;

  /** The extraction options. */
  private final ExtractionOptions options;

  /** Used for accessing objects in S3. */
  @Getter private final S3Dao s3Dao;

  /** The executor for file downloads. */
  private final TaskExecutor downloadTasksExecutor;

  /** The executor for file moves. */
  private final TaskExecutor moveTasksExecutor;

  /**
   * Tracks the asynchronous downloads of {@link DataSetManifestEntry}s, which will produce {@link
   * ManifestEntryDownloadResult}.
   */
  private final Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> downloadTasks;

  /**
   * Constructs a new {@link S3TaskManager}.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param options the {@link ExtractionOptions} to use
   * @param s3Factory used to create instance of {@link S3Dao}
   */
  public S3TaskManager(
      MetricRegistry appMetrics, ExtractionOptions options, S3ClientFactory s3Factory) {
    this.appMetrics = appMetrics;
    this.options = options;

    this.s3Dao = s3Factory.createS3Dao();
    this.downloadTasksExecutor = new TaskExecutor("Download RIF Executor", 1);
    this.moveTasksExecutor = new TaskExecutor("Move Completed RIF Executor", 2);
    this.downloadTasks = new HashMap<>();
  }

  /**
   * Submits an asynchronously task to download the specified {@link DataSetManifestEntry}'s RIF
   * file and returns the result as a {@link Future} {@link ManifestEntryDownloadResult}.
   *
   * <p>Note that callers of this method need to be careful not to start downloading too much at
   * once, as each download can consume a large amount of CPU resources while in progress and a
   * large amount of disk space as it completes.
   *
   * @param manifestEntry the {@link DataSetManifestEntry} to download asynchronously
   * @return a {@link Future} {@link ManifestEntryDownloadResult} that can be used to retrieve the
   *     results of the asynchronous download
   */
  public Future<ManifestEntryDownloadResult> downloadAsync(DataSetManifestEntry manifestEntry) {
    // Has this download already been submitted? If so, just return it.
    if (this.downloadTasks.containsKey(manifestEntry)) return this.downloadTasks.get(manifestEntry);

    /*
     * Submit a new task for the job and cache the Future in case someone tries to
     * submit the job again later.
     */
    ManifestEntryDownloadTask downloadTask =
        new ManifestEntryDownloadTask(this, appMetrics, options, manifestEntry);
    Future<ManifestEntryDownloadResult> downloadFuture =
        this.downloadTasksExecutor.submit(downloadTask);
    LOGGER.debug("Submitted future: {}", TaskExecutor.getTaskId(downloadFuture));
    this.downloadTasks.put(manifestEntry, downloadFuture);

    return downloadFuture;
  }

  /**
   * The {@link OldDataSetQueue} needs to call this method as it discovers that {@link
   * DataSetManifest}s are no longer present in the "Incoming" queue in S3 -- most likely because
   * they've been successfully processed and moved elsewhere. This gives {@link S3TaskManager} a
   * chance to remove any dangling references to the {@link DataSetManifest}, preventing memory
   * leaks.
   *
   * @param manifestId the {@link DataSetManifest#getId()} of the {@link DataSetManifest} that is no
   *     longer present in the "Incoming" S3 queue
   */
  public void cleanupOldDataSet(DataSetManifestId manifestId) {
    downloadTasks
        .entrySet()
        .removeIf(e -> e.getKey().getParentManifest().getId().equals(manifestId));
  }

  /**
   * Shuts down this {@link S3TaskManager} safely, which may require waiting for some
   * already-submitted tasks to complete.
   */
  public void shutdownSafely() {
    LOGGER.info("Shutting down S3 resources...");

    /*
     * Prevent any new move tasks from being submitted, while allowing those
     * that are queued and/or actively running to complete. This is
     * necessary to ensure that data sets present in the database aren't
     * left marked as pending in S3.
     */
    this.moveTasksExecutor.shutdown();

    /*
     * Prevent any new download tasks from being submitted.
     */
    this.downloadTasksExecutor.shutdown();

    try {
      if (!this.moveTasksExecutor.isTerminated()) {
        LOGGER.info("Waiting for all S3 rename/move operations to complete...");
        this.moveTasksExecutor.awaitTermination(30, TimeUnit.MINUTES);
        LOGGER.info("All S3 rename/move operations are complete.");
      }

      if (!this.downloadTasksExecutor.isTerminated()) {
        LOGGER.info("Waiting for in-progress downloads to complete...");
        this.downloadTasksExecutor.awaitTermination(30, TimeUnit.MINUTES);
        LOGGER.info("All in-progress downloads are complete.");
      }

      s3Dao.close();
    } catch (InterruptedException e) {
      // We're not expecting interrupts here, so go boom.
      throw new BadCodeMonkeyException(e);
    }

    LOGGER.info("S3 resources completely shut down.");
  }

  /**
   * Submits a task to the {@link #moveTasksExecutor}.
   *
   * @param task the {@link DataSetMoveTask} to be asynchronously run
   */
  public void submit(DataSetMoveTask task) {
    moveTasksExecutor.submit(task);
  }
}
