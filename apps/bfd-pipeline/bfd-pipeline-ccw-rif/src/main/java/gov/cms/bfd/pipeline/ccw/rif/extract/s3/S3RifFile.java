package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link RifFile} implementation can be used for files that are backed by S3 Objects. Note
 * that this lazy-loads the files, to ensure that connections are not opened until needed.
 */
public final class S3RifFile implements RifFile {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3RifFile.class);

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  /** The manifest data. */
  private final DataSetManifestEntry manifestEntry;

  /** The manifest download result. */
  private final Future<DataSetQueue.ManifestEntry> manifestEntryDownload;

  /**
   * Constructs a new {@link S3RifFile} instance.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param manifestEntry the specific {@link DataSetManifestEntry} represented by this {@link
   *     S3RifFile}
   * @param manifestEntryDownload a {@link Future} for the {@link ManifestEntryDownloadResult} with
   *     a local download of the RIF file's contents
   */
  public S3RifFile(
      MetricRegistry appMetrics,
      DataSetManifestEntry manifestEntry,
      Future<DataSetQueue.ManifestEntry> manifestEntryDownload) {
    Objects.requireNonNull(appMetrics);
    Objects.requireNonNull(manifestEntry);
    Objects.requireNonNull(manifestEntryDownload);

    this.appMetrics = appMetrics;
    this.manifestEntry = manifestEntry;
    this.manifestEntryDownload = manifestEntryDownload;
  }

  /** {@inheritDoc} */
  @Override
  public RifFileType getFileType() {
    return manifestEntry.getType();
  }

  /** {@inheritDoc} */
  @Override
  public String getDisplayName() {
    return String.format(
        "%s.%d:%s",
        manifestEntry.getParentManifest().getTimestampText(),
        manifestEntry.getParentManifest().getSequenceId(),
        manifestEntry.getName());
  }

  /** {@inheritDoc} */
  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  /** {@inheritDoc} */
  @Override
  public InputStream open() {
    DataSetQueue.ManifestEntry fileDownloadResult = waitForDownload();

    // Open a stream for the file.
    InputStream fileDownloadStream;
    try {
      fileDownloadStream = fileDownloadResult.getFileData().getBytes().openBufferedStream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return fileDownloadStream;
  }

  /**
   * Downloads the manifest entry and waits for its completion before returning.
   *
   * @return the completed {@link ManifestEntryDownloadResult} for {@link #manifestEntryDownload}
   */
  private DataSetQueue.ManifestEntry waitForDownload() {
    Timer.Context downloadWaitTimer = null;
    if (!manifestEntryDownload.isDone()) {
      downloadWaitTimer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "waitingForDownloads"))
              .time();
      LOGGER.info("Waiting for RIF file download: '{}'...", getDisplayName());
    }

    // Get the file download result, blocking and waiting if necessary.
    DataSetQueue.ManifestEntry fileDownloadResult;
    try {
      fileDownloadResult = manifestEntryDownload.get(2, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      // We're not expecting interrupts here, so go boom.
      throw new BadCodeMonkeyException(e);
    } catch (ExecutionException e) {
      throw new AwsFailureException(e);
    } catch (TimeoutException e) {
      /*
       * We expect downloads to complete within the (generous) timeout. If
       * they don't, it's more likely than not that something's wrong, so
       * the service should go boom.
       */
      throw new AwsFailureException(e);
    }

    if (downloadWaitTimer != null) {
      LOGGER.info("RIF file downloaded: '{}'.", getDisplayName());
      downloadWaitTimer.close();
    }

    return fileDownloadResult;
  }

  /**
   * Removes the local temporary file that was used to cache this {@link S3RifFile}'s corresponding
   * S3 object data locally.
   */
  public void cleanupTempFile() {
    LOGGER.debug("Cleaning up '{}'...", this);

    /*
     * We need to either cancel the download or wait for it to complete and then clean up the file.
     * However, canceling isn't a thread-safe operation (which is bonkers, but true), so we'll just
     * wait for completion.
     */
    try {
      DataSetQueue.ManifestEntry fileDownloadResult = waitForDownload();
      fileDownloadResult.getFileData().delete();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (CancellationException e) {
      LOGGER.debug("Download was cancelled and can't be cleaned up.");
    }
    LOGGER.debug("Cleaned up '{}'.", this);
  }

  @Override
  public boolean requiresProcessing() {
    return waitForDownload().isIncomplete();
  }

  @Override
  public void markAsStarted() {
    waitForDownload().markAsStarted();
  }

  @Override
  public void markAsProcessed() {
    waitForDownload().markAsCompleted();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    String localDownloadPath;
    try {
      localDownloadPath =
          manifestEntryDownload.isDone()
              ? manifestEntryDownload.get().getFileData().getAbsolutePath()
              : "(not downloaded)";
    } catch (InterruptedException e) {
      // We're not expecting interrupts here, so go boom.
      throw new BadCodeMonkeyException(e);
    } catch (ExecutionException e) {
      localDownloadPath = "(download failed)";
    } catch (CancellationException e) {
      localDownloadPath = "(download cancelled)";
    }

    StringBuilder builder = new StringBuilder();
    builder.append("S3RifFile [manifestEntry=");
    builder.append(manifestEntry);
    builder.append(", manifestEntryDownload.localPath=");
    builder.append(localDownloadPath);
    builder.append("]");
    return builder.toString();
  }
}
