package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents an asynchronous operation to move/rename a data set in S3. */
public final class DataSetMoveTask implements Callable<Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMoveTask.class);

  /** Handle by which to invoke s3 tasks. */
  private final S3TaskManager s3TaskManager;

  /** The extraction options which contain metadata such as the bucket the files are in. */
  private final ExtractionOptions options;

  /** The Manifest for the files being moved. */
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

  /** {@inheritDoc} */
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
      String sourceKey;
      String targetKey;

      // Move the item from where it started into the corresponding output, assume Incoming first
      sourceKey =
          String.format("%s/%s", manifest.getManifestKeyIncomingLocation(), s3KeySuffixToMove);
      targetKey = String.format("%s/%s", manifest.getManifestKeyDoneLocation(), s3KeySuffixToMove);

      LOGGER.info(
          "Copying {}/{} to {}/{}",
          options.getS3BucketName(),
          sourceKey,
          options.getS3BucketName(),
          targetKey);
      s3TaskManager
          .getS3Dao()
          .copyObject(options.getS3BucketName(), sourceKey, options.getS3BucketName(), targetKey);
    }
    LOGGER.debug("Data set copied in S3 (step 1 of move).");

    /*
     * After everything's been copied, loop over it all again and delete the source objects. (We
     * could do it all in the same loop, but this is a bit easier to clean up from if it goes
     * sideways.)
     */
    for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
      String sourceKey =
          String.format("%s/%s", manifest.getManifestKeyIncomingLocation(), s3KeySuffixToMove);

      s3TaskManager.getS3Dao().deleteObject(options.getS3BucketName(), sourceKey);
    }
    LOGGER.debug("Data set deleted in S3 (step 2 of move).");

    LOGGER.debug("Renamed data set '{}' in S3, now that processing is complete.", manifest);
    return null;
  }
}
