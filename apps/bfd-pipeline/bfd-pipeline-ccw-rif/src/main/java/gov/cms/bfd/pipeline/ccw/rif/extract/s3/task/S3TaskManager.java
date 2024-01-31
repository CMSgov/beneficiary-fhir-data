package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.TaskExecutor;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles the execution and management of S3-related tasks. */
public final class S3TaskManager implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3TaskManager.class);

  /** The extraction options. */
  private final ExtractionOptions options;

  /** Used for accessing objects in S3. */
  @Getter private final S3Dao s3Dao;

  /** The executor for file moves. */
  private final TaskExecutor moveTasksExecutor;

  /**
   * Constructs a new {@link S3TaskManager}.
   *
   * @param options the {@link ExtractionOptions} to use
   * @param s3Factory used to create instance of {@link S3Dao}
   */
  public S3TaskManager(ExtractionOptions options, S3ClientFactory s3Factory) {
    this.options = options;

    this.s3Dao = s3Factory.createS3Dao();
    this.moveTasksExecutor = new TaskExecutor("Move Completed RIF Executor", 2);
  }

  /**
   * Shuts down this {@link S3TaskManager} safely, which may require waiting for some
   * already-submitted tasks to complete.
   */
  public void close() {
    LOGGER.info("Shutting down S3 resources...");

    /*
     * Prevent any new move tasks from being submitted, while allowing those
     * that are queued and/or actively running to complete. This is
     * necessary to ensure that data sets present in the database aren't
     * left marked as pending in S3.
     */
    this.moveTasksExecutor.shutdown();

    try {
      if (!this.moveTasksExecutor.isTerminated()) {
        LOGGER.info("Waiting for all S3 rename/move operations to complete...");
        this.moveTasksExecutor.awaitTermination(30, TimeUnit.MINUTES);
        LOGGER.info("All S3 rename/move operations are complete.");
      }

      s3Dao.close();
    } catch (InterruptedException e) {
      // We're not expecting interrupts here, so go boom.
      throw new BadCodeMonkeyException(e);
    }

    LOGGER.info("S3 resources completely shut down.");
  }

  /**
   * Submits a task to the {@link #moveTasksExecutor} to move the manifest's files.
   *
   * @param manifest the {@link DataSetManifest} to be moved
   */
  public void moveManifestFilesInS3(DataSetManifest manifest) {
    moveTasksExecutor.submit(new DataSetMoveTask(this, options, manifest));
  }
}
