package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileManager.MD5Result.MISMATCH;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.DataSetManifestFactory;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao.DownloadedFile;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a queue of data sets (manifest file plus entry files) by combining the current
 * contents of an S3 bucket and records in the database reflecting the status of those files.
 */
@AllArgsConstructor
public class DataSetQueue implements AutoCloseable {
  /** test. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetQueue.class);

  /**
   * Name of S3 meta data field used by CCW to communicate an expected MD5 checksum value for every
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
   * <p>TODO Remove this when file moves are no longer necessary. Expected to be changed as part of
   * BFD-3129.
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
   * been processed or rejected, and return the resulting list. Any newly discovered manifests will
   * be added to the database for tracking. Manifests with future timestamps or timestamps before
   * the minimum allowed value are ignored.
   *
   * @param currentTimestamp timestamp to use for current time
   * @param minimumAllowedManifestTimestamp oldest allowed timestamp for manifests to be returned
   * @param acceptanceCriteria function to allow caller to evaluate manifests
   * @param maxToReturn maximum number of eligible manifests to return
   * @return list of eligible manifests based on criteria
   * @throws IOException pass through in case of error
   */
  public List<Manifest> readEligibleManifests(
      Instant currentTimestamp,
      Instant minimumAllowedManifestTimestamp,
      Predicate<DataSetManifest> acceptanceCriteria,
      int maxToReturn)
      throws IOException {
    try (var ignored1 = appMetrics.timer(TIMER_READ_MANIFESTS).time()) {
      // scan the S3 bucket and get a sorted list of manifests that might be eligible
      final List<ParsedManifestId> possiblyEligibleManifestIds =
          scanS3ForManifests(minimumAllowedManifestTimestamp, currentTimestamp);

      final List<Manifest> manifests = new ArrayList<>();
      for (ParsedManifestId manifestId : possiblyEligibleManifestIds) {
        // enforce the size limit
        if (manifests.size() >= maxToReturn) {
          break;
        }

        // download the manifest XML file from S3 and parse it
        final var s3Key = manifestId.manifestS3Key();
        final DownloadedFile manifestFile;
        final DataSetManifest dataSetManifest;
        try (var ignored2 = appMetrics.timer(TIMER_DOWNLOAD_MANIFEST).time()) {
          manifestFile = downloadFileAndCheckMD5(s3Key);
          dataSetManifest = parseManifestFile(s3Key, manifestFile.getBytes());
        }

        // ensure manifest is represented in our database and get its record
        final S3ManifestFile manifestRecord;
        try (var ignored3 = appMetrics.timer(TIMER_MANIFEST_DB_UPDATE).time()) {
          manifestRecord =
              s3Records.insertOrReadManifestAndDataFiles(s3Key, dataSetManifest, currentTimestamp);
        }

        // add the manifest to our result list only if caller approves it
        if (acceptanceCriteria.test(dataSetManifest)) {
          manifests.add(new Manifest(dataSetManifest, manifestRecord));
        }
      }
      return manifests;
    }
  }

  /**
   * test.
   *
   * @param minimumAllowedManifestTimestamp test.
   * @return test
   */
  public List<FinalManifestList> readFinalManifestLists(Instant minimumAllowedManifestTimestamp) {
    final String manifestListName = "manifestlist.done";
    return s3Files
        // Purposefully excluding synthetic prefix here because synthetic loads don't have a final
        // manifest
        .scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS)
        .filter(
            s ->
                s.getKey().toLowerCase().endsWith(manifestListName)
                    && s.getLastModified().isAfter(minimumAllowedManifestTimestamp))
        .map(
            s -> {
              String key = s.getKey();

              try {
                DownloadedFile downloadedFile = downloadFileAndCheckMD5(key);
                return new FinalManifestList(downloadedFile.getBytes().read(), key);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  /**
   * test.
   *
   * @param manifestKeys test
   * @return test
   */
  public boolean hasIncompleteManifests(Set<String> manifestKeys) {
    return s3Records.hasIncompleteManifests(manifestKeys);
  }

  /**
   * test.
   *
   * @param manifestListTimestamps test
   * @param cutoff test
   * @return test
   */
  public boolean hasMissingManifests(Set<Instant> manifestListTimestamps, Instant cutoff) {
    return s3Records.hasMissingManifestLists(manifestListTimestamps, cutoff);
  }

  /**
   * Downloads the data file from S3 and confirms its MD5 checksum.
   *
   * @param entryRecord database record corresponding to the entry
   * @return object containing information about the downloaded file
   * @throws IOException pass through in case of error
   */
  public ManifestEntry downloadManifestEntry(S3DataFile entryRecord) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_ENTRY).time()) {
      final var s3Key = entryRecord.getS3Key();
      final var downloadedFile = downloadFileAndCheckMD5(s3Key);
      return new ManifestEntry(entryRecord, downloadedFile);
    }
  }

  /**
   * Checks the S3 bucket to see if all of the files corresponding to the manifest's entries exist
   * in the bucket. Does not download any files.
   *
   * @param manifestRecord database record corresponding to the manifest
   * @return true if bucket contains a file for all of the manifest's entries
   */
  public boolean allEntriesExistInS3(S3ManifestFile manifestRecord) {
    final var manifestS3Prefix = S3FileManager.extractPrefixFromS3Key(manifestRecord.getS3Key());
    final var namesAtPrefix = s3Files.fetchKeysWithPrefix(manifestS3Prefix);
    return manifestRecord.getDataFiles().stream()
        .map(S3DataFile::getS3Key)
        .allMatch(namesAtPrefix::contains);
  }

  /**
   * Updates this manifest's record in the database to reflect that processing of the manifest has
   * been started.
   *
   * @param manifestRecord database record corresponding to the manifest
   * @throws BadCodeMonkeyException if the entry has already been completely processed
   */
  public void markAsStarted(S3ManifestFile manifestRecord) {
    if (!STARTABLE_MANIFEST_STATUSES.contains(manifestRecord.getStatus())) {
      throw new BadCodeMonkeyException("Attempting to start processing a completed manifest.");
    }
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.STARTED);
    manifestRecord.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(manifestRecord);
  }

  /**
   * Updates this manifest's record in the database to reflect that processing of the manifest has
   * been completed successfully.
   *
   * @param manifestRecord database record corresponding to the manifest
   */
  public void markAsProcessed(S3ManifestFile manifestRecord) {
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.COMPLETED);
    manifestRecord.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(manifestRecord);
  }

  /**
   * Updates this manifest's record in the database to reflect that the manifest has been rejected
   * and will not be processed.
   *
   * @param manifestRecord database record corresponding to the manifest
   */
  public void markAsRejected(S3ManifestFile manifestRecord) {
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.REJECTED);
    manifestRecord.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(manifestRecord);
  }

  /**
   * Updates the entry's record in the database to mark it as started.
   *
   * @param dataFileRecord database record corresponding to the data file
   * @throws BadCodeMonkeyException if the entry has already been completely processed
   */
  public void markAsStarted(S3DataFile dataFileRecord) {
    if (!STARTABLE_ENTRY_STATUSES.contains(dataFileRecord.getStatus())) {
      throw new BadCodeMonkeyException("Attempting to start processing a completed data file.");
    }
    dataFileRecord.setStatus(S3DataFile.FileStatus.STARTED);
    dataFileRecord.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(dataFileRecord.getParentManifest());
  }

  /**
   * Updates the entry's record in the database to mark it as completed.
   *
   * @param dataFileRecord database record corresponding to the data file
   * @throws BadCodeMonkeyException if the entry has already been completely processed
   */
  public void markAsCompleted(S3DataFile dataFileRecord) {
    if (!STARTABLE_ENTRY_STATUSES.contains(dataFileRecord.getStatus())) {
      throw new BadCodeMonkeyException("Attempting to mark a completed data file as completed.");
    }
    dataFileRecord.setStatus(S3DataFile.FileStatus.COMPLETED);
    dataFileRecord.setStatusTimestamp(clock.instant());
    s3Records.updateS3ManifestAndDataFiles(dataFileRecord.getParentManifest());
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
   * necessary. Expected to be changed as part of BFD-3129.
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
  @VisibleForTesting
  DownloadedFile downloadFileAndCheckMD5(String s3Key) throws IOException {
    final var manifestFile = s3Files.downloadFile(s3Key);
    if (s3Files.checkMD5(manifestFile, MD5_CHECKSUM_META_DATA_FIELD) == MISMATCH) {
      throw new IOException(
          String.format("MD5 checksum mismatch for file %s", manifestFile.getS3Key()));
    }
    return manifestFile;
  }

  /**
   * Scans S3 bucket for all manifests that are eligible for processing and have a timestamp greater
   * than or equal to the provided minimum and less than or equal to the provided maximum.
   *
   * @param minimumAllowedManifestTimestamp oldest allowed timestamp for manifests to be returned
   * @param maximumAllowedManifestTimestamp current time used to filter out manifests meant for
   *     processing in the future
   * @return list of manifest ids
   */
  @VisibleForTesting
  List<ParsedManifestId> scanS3ForManifests(
      Instant minimumAllowedManifestTimestamp, Instant maximumAllowedManifestTimestamp) {
    final var ineligibleS3Keys =
        s3Records.readIneligibleManifestS3Keys(minimumAllowedManifestTimestamp);
    return Stream.concat(
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS),
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS))
        .map(S3Dao.S3ObjectSummary::getKey)
        .filter(s3Key -> !ineligibleS3Keys.contains(s3Key))
        .flatMap(s3Key -> parseManifestIdFromS3Key(s3Key).stream())
        .filter(
            parsedManifestId ->
                parsedManifestId.manifestId.isBefore(maximumAllowedManifestTimestamp))
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
   * @param manifestS3Key key to the manifest file in S3 bucket
   * @return empty if parsing fails, otherwise the parsed id
   */
  @VisibleForTesting
  Optional<ParsedManifestId> parseManifestIdFromS3Key(String manifestS3Key) {
    DataSetManifest.DataSetManifestId manifestId =
        DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key(manifestS3Key);
    if (manifestId == null) {
      return Optional.empty();
    } else {
      return Optional.of(new ParsedManifestId(manifestS3Key, manifestId));
    }
  }

  /**
   * Parse the manifest XML file represented by the {@link ByteSource} into a {#link
   * DataSetManifest} object.
   *
   * @param manifestS3Key key to the manifest file in S3 bucket
   * @param fileBytes the contents of the XML file downloaded from S3
   * @return the parsed manifest
   */
  @VisibleForTesting
  DataSetManifest parseManifestFile(String manifestS3Key, ByteSource fileBytes) {
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

  /**
   * Parsed manifest id along with its S3 key. Natural order is by ascending {@link #manifestId}.
   *
   * @param manifestS3Key key to the manifest file in S3 bucket
   * @param manifestId timestamp and sequence number that identify a manifest file in S3
   */
  public record ParsedManifestId(String manifestS3Key, DataSetManifest.DataSetManifestId manifestId)
      implements Comparable<ParsedManifestId> {
    /** Provides a natural ordering for objects by ascending {@link #manifestId}. {@inheritDoc} */
    @Override
    public int compareTo(@Nonnull ParsedManifestId o) {
      return manifestId.compareTo(o.manifestId);
    }
  }

  /**
   * Data regarding a manifest file that has been downloaded from S3 and tracked in the database.
   *
   * @param manifest the manifest itself parsed from XML
   * @param manifestRecord database record that tracks status of the manifest
   */
  public record Manifest(DataSetManifest manifest, S3ManifestFile manifestRecord) {}

  /**
   * Representation of a manifest entry (data file) that has been downloaded from S3 and tracked in
   * the database. Provides helper methods to interact with the file and its contents without
   * exposing them to other classes.
   */
  @AllArgsConstructor
  public class ManifestEntry {
    /** The database record for the data file itself. */
    private final S3DataFile dataFileRecord;

    /** The cached file. */
    private final DownloadedFile fileData;

    /**
     * Extracts the manifest id and index in the form of a {@link RifFile.RecordId}.
     *
     * @return the record id
     */
    public RifFile.RecordId getRifFileRecordId() {
      return new RifFile.RecordId(
          dataFileRecord.getParentManifest().getManifestId(), dataFileRecord.getIndex());
    }

    /**
     * Returns a {@link ByteSource} that can be used to read the cached data.
     *
     * @return the byte source
     */
    public ByteSource getBytes() {
      return fileData.getBytes();
    }

    /**
     * Deletes the data file from the cache.
     *
     * @throws IOException pass through if delete fails
     */
    public void delete() throws IOException {
      fileData.delete();
    }

    /**
     * Returns the absolute path of the cached file for use in logging.
     *
     * @return the path
     */
    public String getCachedFilePath() {
      return fileData.getAbsolutePath();
    }

    /**
     * Returns true if the entry is still eligible for processing.
     *
     * @return true if the entry can be processed, false otherwise
     */
    public boolean isIncomplete() {
      return STARTABLE_ENTRY_STATUSES.contains(dataFileRecord.getStatus());
    }

    /**
     * Updates the entry's record in the database to mark it as started.
     *
     * @throws BadCodeMonkeyException if the entry has already been completely processed
     */
    public void markAsStarted() {
      DataSetQueue.this.markAsStarted(dataFileRecord);
    }

    /**
     * Updates the entry's record in the database to mark it as completed.
     *
     * @throws BadCodeMonkeyException if the entry has already been completely processed
     */
    public void markAsCompleted() {
      DataSetQueue.this.markAsCompleted(dataFileRecord);
    }

    /**
     * Used to implement {@link RifFile#getLastRecordNumber}.
     *
     * @return the last record number value or zero if we have no value
     */
    public long getLastRecordNumber() {
      return dataFileRecord.getLastRecordNumber();
    }

    /**
     * Used to implement {@link RifFile#updateLastRecordNumber}. Sets the new record number value
     * and updates the record in the database.
     *
     * @param recordNumber the new value
     */
    public void updateLastRecordNumber(long recordNumber) {
      dataFileRecord.setLastRecordNumber(recordNumber);
      s3Records.updateS3DataFile(dataFileRecord);
    }
  }
}
