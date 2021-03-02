package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.waiters.WaiterParameters;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorWorker;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents an asynchronous operation to move/rename a data set in S3. */
public final class DataSetMoveTask implements Callable<Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMoveTask.class);

  private final S3TaskManager s3TaskManager;
  private final ExtractionOptions options;
  private final DataSetManifest manifest;

  /**
   * Constructs a new {@link DataSetMoveTask}.
   *
   * @param s3TaskManager the {@link S3TaskManager} to use
   * @param options the {@link ExtractionOptions} to use
   * @param manifest the {@link DataSetManifest} to be moved/renamed (along with the files it
   *     references)
   */
  public DataSetMoveTask(
      S3TaskManager s3TaskManager, ExtractionOptions options, DataSetManifest manifest) {
    this.s3TaskManager = s3TaskManager;
    this.options = options;
    this.manifest = manifest;
  }

  /** @see java.util.concurrent.Callable#call() */
  @Override
  public Void call() throws Exception {
    LOGGER.debug("Renaming data set '{}' in S3, now that processing is complete...", manifest);

    /*
     * S3 doesn't support batch/transactional operations, or an atomic move
     * operation. Instead, we have to first copy all of the objects to their
     * new location, and then remove the old objects. If something blows up
     * in the middle of this method, orphaned S3 objects WILL be created.
     * That's an unlikely enough occurrence, though, that we're not going to
     * engineer around it right now.
     */

    // First, get a list of all the object keys to work on.
    List<String> s3KeySuffixesToMove =
        manifest.getEntries().stream()
            .map(e -> String.format("%s/%s", manifest.getTimestampText(), e.getName()))
            .collect(Collectors.toList());
    s3KeySuffixesToMove.add(
        String.format("%s/%d_manifest.xml", manifest.getTimestampText(), manifest.getSequenceId()));

    /*
     * Then, loop through each of those objects and copy them (S3 has no
     * bulk copy operation).
     */
    for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
      String sourceKey =
          String.format(
              "%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS, s3KeySuffixToMove);
      String targetKey =
          String.format(
              "%s/%s", DataSetMonitorWorker.S3_PREFIX_COMPLETED_DATA_SETS, s3KeySuffixToMove);

      /*
       * Before copying, grab the metadata of the source object to ensure
       * that we maintain its encryption settings (by default, the copy
       * will maintain all metadata EXCEPT: server-side-encryption,
       * storage-class, and website-redirect-location).
       */
      ObjectMetadata objectMetadata =
          s3TaskManager.getS3Client().getObjectMetadata(options.getS3BucketName(), sourceKey);
      CopyObjectRequest copyRequest =
          new CopyObjectRequest(
              options.getS3BucketName(), sourceKey, options.getS3BucketName(), targetKey);
      if (objectMetadata.getSSEAwsKmsKeyId() != null) {
        copyRequest.setSSEAwsKeyManagementParams(
            new SSEAwsKeyManagementParams(objectMetadata.getSSEAwsKmsKeyId()));
      }

      Copy copyOperation = s3TaskManager.getS3TransferManager().copy(copyRequest);
      try {
        copyOperation.waitForCopyResult();
        s3TaskManager
            .getS3Client()
            .waiters()
            .objectExists()
            .run(
                new WaiterParameters<GetObjectMetadataRequest>(
                    new GetObjectMetadataRequest(options.getS3BucketName(), targetKey)));
      } catch (InterruptedException e) {
        throw new BadCodeMonkeyException(e);
      }
    }
    LOGGER.debug("Data set copied in S3 (step 1 of move).");

    /*
     * After everything's been copied, loop over it all again and delete it the source objects. (We
     * could do it all in the same loop, but this is a bit easier to clean up from if it goes
     * sideways.)
     */
    for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
      String sourceKey =
          String.format(
              "%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS, s3KeySuffixToMove);
      DeleteObjectRequest deleteObjectRequest =
          new DeleteObjectRequest(options.getS3BucketName(), sourceKey);
      s3TaskManager.getS3Client().deleteObject(deleteObjectRequest);
      s3TaskManager
          .getS3Client()
          .waiters()
          .objectNotExists()
          .run(
              new WaiterParameters<GetObjectMetadataRequest>(
                  new GetObjectMetadataRequest(options.getS3BucketName(), sourceKey)));
    }
    LOGGER.debug("Data set deleted in S3 (step 2 of move).");

    LOGGER.debug("Renamed data set '{}' in S3, now that processing is complete.", manifest);
    return null;
  }
}
