package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileManager.MD5Result.MISMATCH;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.ByteSource;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.DataSetManifestFactory;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao.DownloadedFile;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class DataSetQueue implements AutoCloseable {
  /**
   * Name of S3 meta data field used by CCW to comunicate an expected MD5 checksum value for every
   * file they upload to the S3 bucket for processing.
   */
  public static final String MD5_CHECKSUM_META_DATA_FIELD = "md5chksum";

  /** Name used for read list of eligible manifests timer. */
  static final String TIMER_READ_MANIFESTS =
      MetricRegistry.name(DataSetQueue.class, "readEligibleManifests");

  /** Name used for download one manifest timer. */
  static final String TIMER_DOWNLOAD_MANIFEST =
      MetricRegistry.name(DataSetQueue.class, "downloadManifest");

  /** Name used for download one manifest entry timer. */
  static final String TIMER_DOWNLOAD_ENTRY =
      MetricRegistry.name(DataSetQueue.class, "downloadEntry");

  /** Name used for updating one manifest in database timer. */
  static final String TIMER_MANIFEST_DB_UPDATE =
      MetricRegistry.name(DataSetQueue.class, "updateManifestInDb");

  /** Statuses that indicate that processing is allowed on a {@link S3ManifestFile}. */
  private static final Set<S3ManifestFile.ManifestStatus> STARTABLE_MANIFEST_STATUSES =
      Set.of(S3ManifestFile.ManifestStatus.DISCOVERED, S3ManifestFile.ManifestStatus.STARTED);

  /** Statuses that indicate that processing is allowed on a {@link S3DataFile}. */
  private static final Set<S3DataFile.FileStatus> STARTABLE_ENTRY_STATUSES =
      Set.of(S3DataFile.FileStatus.DISCOVERED, S3DataFile.FileStatus.STARTED);

  /** Used to access current time. */
  private final Clock clock;

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  /** Used to track S3 files in the database. */
  private final S3ManifestDbDao s3Records;

  /** Used to download files from S3 to a local cache for processing. */
  private final S3FileManager s3Files;

  /**
   * Used only for the soon to be obsolete S3 file move task.
   *
   * <p>TODO Remove this when file moves are no longer necessary.
   */
  private final S3TaskManager s3TaskManager;

  @Override
  public void close() throws Exception {
    final var closer = new MultiCloser();
    closer.close(s3Files::close);
    closer.close(s3TaskManager::close);
    closer.finish();
  }

  /**
   * Scan the S3 bucket for available manifests, filter any that database indicates have already
   * been processed or rejected, and return the resulting list. Any newly discovered maniests will
   * be added to the database for tracking.
   *
   * @param currentTime timestamp to use for current time
   * @param minimumAllowedManifestTimestamp oldest allowed timestamp for manifests to be returned
   * @param acceptanceCriteria function to allow caller to evaluate manifests
   * @param maxToReturn maximum number of eligible manifests to return
   * @return list of eligible manifests based on criteria
   * @throws IOException pass through in case of error
   */
  public List<Manifest> readEligibleManifests(
      Instant currentTime,
      Instant minimumAllowedManifestTimestamp,
      ThrowingFunction<Boolean, Manifest, IOException> acceptanceCriteria,
      int maxToReturn)
      throws IOException {
    try (var ignored1 = appMetrics.timer(TIMER_READ_MANIFESTS).time()) {
      final List<ParsedManifestId> possiblyEligibleManifestIds =
          scanS3ForManifests(minimumAllowedManifestTimestamp);
      final List<Manifest> manifests = new ArrayList<>();
      for (ParsedManifestId manifestId : possiblyEligibleManifestIds) {
        if (manifests.size() >= maxToReturn) {
          break;
        }
        final var s3Key = manifestId.getS3Key();
        final DownloadedFile manifestFile;
        final DataSetManifest dataSetManifest;
        try (var ignored2 = appMetrics.timer(TIMER_DOWNLOAD_MANIFEST).time()) {
          manifestFile = downloadAndCheckMD5(s3Key);
          dataSetManifest = parseManifestFile(s3Key, manifestFile.getBytes());
        }
        final S3ManifestFile manifestRecord;
        try (var ignored3 = appMetrics.timer(TIMER_MANIFEST_DB_UPDATE).time()) {
          manifestRecord =
              s3Records.insertOrReadManifestAndDataFiles(s3Key, dataSetManifest, currentTime);
        }
        final var manifest =
            new Manifest(manifestId.getManifestId(), manifestFile, dataSetManifest, manifestRecord);
        if (acceptanceCriteria.apply(manifest)) {
          manifests.add(manifest);
        }
      }
      return manifests;
    }
  }

  /**
   * Downloads the data file from S3 and confirms its MD5 checksum.
   *
   * @param record database record corresponding to the entry
   * @return object containing information about the downloaded file
   * @throws IOException pass through in case of error
   */
  public ManifestEntry downloadManifestEntry(S3DataFile record) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_ENTRY).time()) {
      final var s3Key = record.getS3Key();
      final var downloadedFile = downloadAndCheckMD5(s3Key);
      return new ManifestEntry(record, downloadedFile);
    }
  }

  /**
   * Checks the S3 bucket to see if all of the files referenced in manifest entries exist in the
   * bucket. Does not download any files.
   *
   * @param record database record corresponding to the manifest
   * @return true if bucket contains a file for all of the manifest's entries
   */
  public boolean allEntriesExistInS3(S3ManifestFile record) {
    final var manifestS3Prefix = S3FileManager.extractPrefixFromS3Key(record.getS3Key());
    final var namesAtPrefix = s3Files.fetchKeysWithPrefix(manifestS3Prefix);
    return record.getDataFiles().stream()
        .map(S3DataFile::getS3Key)
        .allMatch(namesAtPrefix::contains);
  }

  /**
   * Updates this manifest's record in the database to reflect that processing of the manifest has
   * been started.
   *
   * @param record database record corresponding to the manifest
   */
  public void markAsStarted(S3ManifestFile record) {
    if (!STARTABLE_MANIFEST_STATUSES.contains(record.getStatus())) {
      throw new BadCodeMonkeyException("Attempting to start processing a completed manifest.");
    }
    record.setStatus(S3ManifestFile.ManifestStatus.STARTED);
    record.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(record);
  }

  /**
   * Updates this manifest's record in the database to reflect that processing of the manifest has
   * been completed successfully.
   *
   * @param record database record corresponding to the manifest
   */
  public void markAsProcessed(S3ManifestFile record) {
    record.setStatus(S3ManifestFile.ManifestStatus.COMPLETED);
    record.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(record);
  }

  /**
   * Updates this manifest's record in the database to reflect that the manifest has been rejected
   * and will not be processed.
   *
   * @param record database record corresponding to the manifest
   */
  public void markAsRejected(S3ManifestFile record) {
    record.setStatus(S3ManifestFile.ManifestStatus.REJECTED);
    record.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(record);
  }

  /**
   * Gets the number of bytes of usable disk space from the file system containing our cache
   * directory.
   *
   * @return number of bytes
   * @throws IOException pass through from file system check
   */
  public long getAvailableDiskSpaceInBytes() throws IOException {
    return s3Files.getAvailableDiskSpaceInBytes();
  }

  /**
   * Starts a background thread that moves all of the files associated with the specified manifest
   * from the Incoming tree to the Done tree.
   *
   * <p>TODO Remove this method (and {@link #s3TaskManager} once S3 file movement is no longer
   * necessary
   *
   * @param manifest the manifest to move
   */
  public void moveManifestFilesInS3(DataSetManifest manifest) {
    s3TaskManager.moveManifestFilesInS3(manifest);
  }

  /**
   * Download a file from S3 and confirm that its MD5 checksum matches that in the S3 file's meta
   * data.
   *
   * @param s3Key identifies file to download in S3 bucket
   * @return object containing information about downloaded file
   * @throws IOException pass through in case of error
   */
  private DownloadedFile downloadAndCheckMD5(String s3Key) throws IOException {
    final var manifestFile = s3Files.downloadFile(s3Key);
    if (s3Files.checkMD5(manifestFile, MD5_CHECKSUM_META_DATA_FIELD) == MISMATCH) {
      throw new IOException(
          String.format("MD5 checksum mismatch for file %s", manifestFile.getS3Key()));
    }
    return manifestFile;
  }

  /**
   * Scans S3 bucket for all manifests that are eligible for processing and have a timestamp greater
   * than or equal to the provided one.
   *
   * @param minimumAllowedManifestTimestamp oldest allowed timestamp for manifests to be returned
   * @return list of manifest ids
   */
  private List<ParsedManifestId> scanS3ForManifests(Instant minimumAllowedManifestTimestamp) {
    final var ineligibleS3Keys =
        s3Records.readIneligibleManifestS3Keys(minimumAllowedManifestTimestamp);
    return Stream.concat(
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS),
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS))
        .filter(s3Summary -> !ineligibleS3Keys.contains(s3Summary.getKey()))
        .flatMap(s3Summary -> parseManifestIdFromS3Key(s3Summary).stream())
        .filter(
            parsedManifestId ->
                parsedManifestId.manifestId.isAfter(minimumAllowedManifestTimestamp))
        .sorted()
        .toList();
  }

  /**
   * Parses S3 key in the provided {@link gov.cms.bfd.pipeline.sharedutils.s3.S3Dao.S3ObjectSummary}
   * to extract a valid {@link ParsedManifestId}.
   *
   * @param s3Summary summary of object with key to parse
   * @return empty if parsing fails, otherwise the parsed id
   */
  private Optional<ParsedManifestId> parseManifestIdFromS3Key(S3Dao.S3ObjectSummary s3Summary) {
    DataSetManifest.DataSetManifestId manifestId =
        DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key(s3Summary.getKey());
    if (manifestId == null) {
      return Optional.empty();
    } else {
      return Optional.of(new ParsedManifestId(s3Summary, manifestId));
    }
  }

  private DataSetManifest parseManifestFile(String manifestS3Key, ByteSource fileBytes) {
    try (InputStream dataManifestStream = fileBytes.openBufferedStream()) {
      DataSetManifest manifest =
          DataSetManifestFactory.newInstance().parseManifest(dataManifestStream);

      // Setup the manifest incoming/outgoing location
      if (manifestS3Key.contains(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS)) {
        manifest.setManifestKeyIncomingLocation(
            CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS);
        manifest.setManifestKeyDoneLocation(CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS);
      } else {
        manifest.setManifestKeyIncomingLocation(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS);
        manifest.setManifestKeyDoneLocation(CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS);
      }

      return manifest;
    } catch (Exception e) {
      throw new RuntimeException("Error reading manifest: " + manifestS3Key, e);
    }
  }

  @Data
  public static class ParsedManifestId implements Comparable<ParsedManifestId> {
    private final S3Dao.S3ObjectSummary s3Summary;
    private final DataSetManifest.DataSetManifestId manifestId;

    public String getS3Key() {
      return s3Summary.getKey();
    }

    @Override
    public int compareTo(@Nonnull ParsedManifestId o) {
      return manifestId.compareTo(o.manifestId);
    }
  }

  @Data
  public static class Manifest {
    private final DataSetManifest.DataSetManifestId manifestId;
    private final DownloadedFile fileData;
    private final DataSetManifest manifest;
    private final S3ManifestFile record;
  }

  @Data
  public class ManifestEntry {
    private final S3DataFile record;
    private final DownloadedFile fileData;

    public boolean isIncomplete() {
      return STARTABLE_ENTRY_STATUSES.contains(record.getStatus());
    }

    public void markAsStarted() {
      if (!isIncomplete()) {
        throw new BadCodeMonkeyException("Attempting to start processing a completed data file.");
      }
      record.setStatus(S3DataFile.FileStatus.STARTED);
      record.setStatusTimestamp(clock.instant());
      s3Records.updateS3ManifestAndDataFiles(record.getParentManifest());
    }

    public void markAsCompleted() {
      if (!isIncomplete()) {
        throw new BadCodeMonkeyException("Attempting to mark a completed data file as completed.");
      }
      record.setStatus(S3DataFile.FileStatus.COMPLETED);
      record.setStatusTimestamp(clock.instant());
      s3Records.updateS3ManifestAndDataFiles(record.getParentManifest());
    }
  }
}
