package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifPipelineJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.ChecksumException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an asynchronous operation to download the contents of a specific {@link
 * DataSetManifestEntry} from S3.
 */
public final class ManifestEntryDownloadTask implements Callable<ManifestEntryDownloadResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

  private final S3TaskManager s3TaskManager;
  private final MetricRegistry appMetrics;
  private final ExtractionOptions options;
  private final DataSetManifestEntry manifestEntry;

  /**
   * Constructs a new {@link ManifestEntryDownloadTask}.
   *
   * @param s3TaskManager the {@link S3TaskManager} to use
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param options the {@link ExtractionOptions} to use
   * @param manifestEntry the {@link DataSetManifestEntry} to download the file for
   */
  public ManifestEntryDownloadTask(
      S3TaskManager s3TaskManager,
      MetricRegistry appMetrics,
      ExtractionOptions options,
      DataSetManifestEntry manifestEntry) {
    this.s3TaskManager = s3TaskManager;
    this.appMetrics = appMetrics;
    this.options = options;
    this.manifestEntry = manifestEntry;
  }

  /** @see java.util.concurrent.Callable#call() */
  @Override
  public ManifestEntryDownloadResult call() throws Exception {
    try {
      GetObjectRequest objectRequest =
          new GetObjectRequest(
              options.getS3BucketName(),
              String.format(
                  "%s/%s/%s",
                  CcwRifPipelineJob.S3_PREFIX_PENDING_DATA_SETS,
                  manifestEntry.getParentManifest().getTimestampText(),
                  manifestEntry.getName()));
      Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");

      Timer.Context downloadTimer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "downloadSystemTime"))
              .time();
      LOGGER.debug(
          "Downloading '{}' to '{}'...", manifestEntry, localTempFile.toAbsolutePath().toString());
      Download downloadHandle =
          s3TaskManager.getS3TransferManager().download(objectRequest, localTempFile.toFile());
      downloadHandle.waitForCompletion();
      LOGGER.debug(
          "Downloaded '{}' to '{}'.", manifestEntry, localTempFile.toAbsolutePath().toString());
      downloadTimer.close();

      // generate MD5ChkSum value on file just downloaded
      Timer.Context md5ChkSumTimer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "md5ChkSumSystemTime"))
              .time();
      InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
      String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
      md5ChkSumTimer.close();

      String downloadedFileMD5ChkSum =
          downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum");
      // TODO Remove null check below once Jira CBBD-368 is completed
      if ((downloadedFileMD5ChkSum != null)
          && (!generatedMD5ChkSum.equals(downloadedFileMD5ChkSum)))
        throw new ChecksumException(
            "Checksum doesn't match on downloaded file "
                + localTempFile
                + " manifest entry is "
                + manifestEntry.toString());

      return new ManifestEntryDownloadResult(manifestEntry, localTempFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (AmazonClientException e) {
      throw new AwsFailureException(e);
    } catch (InterruptedException e) {
      // Shouldn't happen, as our apps don't use thread interrupts.
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Calculates and returns a Base64 encoded MD5chksum value for the file just downloaded from S3
   *
   * @param downloadedS3File the {@link InputStream} of the file just downloaded from S3
   * @return Base64 encoded md5 value
   */
  public static String computeMD5ChkSum(InputStream downloadedS3File)
      throws IOException, NoSuchAlgorithmException {
    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;
    MessageDigest md5Digest = MessageDigest.getInstance("MD5");

    // Read file data and update in message digest
    while ((bytesCount = downloadedS3File.read(byteArray)) != -1) {
      md5Digest.update(byteArray, 0, bytesCount);
    }

    // close the stream
    downloadedS3File.close();

    // Get the hash's bytes
    byte[] bytes = md5Digest.digest();

    // return complete hash
    return Base64.getEncoder().encodeToString(bytes);
  }

  /** Represents the results of a {@link ManifestEntryDownloadTask}. */
  public static final class ManifestEntryDownloadResult {
    private final DataSetManifestEntry manifestEntry;
    private final Path localDownload;

    /**
     * Constructs a new {@link ManifestEntryDownloadResult} instance.
     *
     * @param manifestEntry the value to use for {@link #getManifestEntry()}
     * @param localDownload the value to use for {@link #getLocalDownload()}
     */
    public ManifestEntryDownloadResult(DataSetManifestEntry manifestEntry, Path localDownload) {
      this.manifestEntry = manifestEntry;
      this.localDownload = localDownload;
    }

    /** @return the {@link DataSetManifestEntry} whose file was downloaded */
    public DataSetManifestEntry getManifestEntry() {
      return manifestEntry;
    }

    /**
     * @return the {@link Path} to the local copy of the {@link DataSetManifestEntry}'s contents,
     *     which should be deleted once it is no longer needed, to prevent disk space usage leaks
     */
    public Path getLocalDownload() {
      return localDownload;
    }
  }
}
