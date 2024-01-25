package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.ChecksumException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Represents an asynchronous operation to download the contents of a specific {@link
 * DataSetManifestEntry} from S3.
 */
@AllArgsConstructor
public final class ManifestEntryDownloadTask implements Callable<ManifestEntryDownloadResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

  /** The task manager for S3. */
  private final S3TaskManager s3TaskManager;

  /** The metrics registry. */
  private final MetricRegistry appMetrics;

  /** The extraction options. */
  private final ExtractionOptions options;

  /** The manifest data. */
  private final DataSetManifestEntry manifestEntry;

  /** Performs the download and returns the result. {@inheritDoc} */
  @Override
  public ManifestEntryDownloadResult call() throws Exception {
    try {
      String s3Key =
          String.format(
              "%s/%s/%s",
              manifestEntry.getParentManifest().getManifestKeyIncomingLocation(),
              manifestEntry.getParentManifest().getTimestampText(),
              manifestEntry.getName());
      Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");

      Timer.Context downloadTimer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "downloadSystemTime"))
              .time();
      LOGGER.debug("Downloading '{}' to '{}'...", manifestEntry, localTempFile.toAbsolutePath());
      S3Dao.S3ObjectDetails downloadResult =
          s3TaskManager.getS3Dao().downloadObject(options.getS3BucketName(), s3Key, localTempFile);
      LOGGER.debug("Downloaded '{}' to '{}'.", manifestEntry, localTempFile.toAbsolutePath());
      downloadTimer.close();

      // generate MD5ChkSum value on file just downloaded
      Timer.Context md5ChkSumTimer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "md5ChkSumSystemTime"))
              .time();
      InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
      String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
      md5ChkSumTimer.close();
      String downloadedFileMD5ChkSum = downloadResult.getMetaData().get("md5chksum");
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
    } catch (SdkClientException | CompletionException e) {
      throw new AwsFailureException(e);
    } catch (CancellationException e) {
      // Shouldn't happen, as our apps don't use thread interrupts.
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Calculates and returns a Base64 encoded MD5chksum value for the file just downloaded from S3.
   *
   * @param downloadedS3File the {@link InputStream} of the file just downloaded from S3
   * @return Base64 encoded md5 value
   * @throws IOException if there is an issue reading or closing the downloaded file
   * @throws NoSuchAlgorithmException if there is an issue getting the specified message digest
   *     algorithm
   */
  public static String computeMD5ChkSum(InputStream downloadedS3File)
      throws IOException, NoSuchAlgorithmException {
    // ensure the stream is always closed
    try (downloadedS3File) {
      // Create byte array to read data in chunks
      byte[] byteArray = new byte[1024];
      int bytesCount = 0;
      MessageDigest md5Digest = MessageDigest.getInstance("MD5");

      // Read file data and update in message digest
      while ((bytesCount = downloadedS3File.read(byteArray)) != -1) {
        md5Digest.update(byteArray, 0, bytesCount);
      }

      // Get the hash's bytes
      byte[] bytes = md5Digest.digest();

      // return complete hash
      return Base64.getEncoder().encodeToString(bytes);
    }
  }

  /** Represents the results of a {@link ManifestEntryDownloadTask}. */
  @Getter
  @AllArgsConstructor
  public static final class ManifestEntryDownloadResult {
    /** The {@link DataSetManifestEntry} whose file was downloaded. */
    private final DataSetManifestEntry manifestEntry;

    /**
     * The {@link Path} to the local copy of the {@link DataSetManifestEntry}'s contents, which
     * should be deleted once it is no longer needed, to prevent disk space usage leaks.
     */
    private final Path localDownload;
  }
}
