package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key;
import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileManager.Md5Result.MISMATCH;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.ByteSource;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.DataSetManifestFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao.DownloadedFile;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class DataSetQueue implements AutoCloseable {
  public static final String MD5ChecksumMetaDataField = "md5chksum";
  static final String TIMER_READ_MANIFESTS =
      MetricRegistry.name(DataSetQueue.class, "readEligibleManifests");
  static final String TIMER_DOWNLOAD_MANIFEST =
      MetricRegistry.name(DataSetQueue.class, "downloadManifest");
  static final String TIMER_DOWNLOAD_ENTRY =
      MetricRegistry.name(DataSetQueue.class, "downloadEntry");
  static final String TIMER_MANIFEST_DB_UPDATE =
      MetricRegistry.name(DataSetQueue.class, "updateManifestInDb");

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  private final S3ManifestDbDao s3Records;
  private final S3FileManager s3Files;

  @Override
  public void close() throws Exception {
    s3Files.close();
  }

  public List<Manifest> readEligibleManifests(
      Instant currentTime,
      Instant minTime,
      ThrowingFunction<Boolean, Manifest, IOException> acceptanceCriteria,
      int maxToRead)
      throws IOException {
    try (var ignored1 = appMetrics.timer(TIMER_READ_MANIFESTS).time()) {
      final List<ParsedManifestId> possiblyEligibleManifestIds = scanS3ForManifests(minTime);
      final List<Manifest> manifests = new ArrayList<>();
      for (ParsedManifestId manifestId : possiblyEligibleManifestIds) {
        if (manifests.size() >= maxToRead) {
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

  public ManifestEntry downloadManifestEntry(S3DataFile record) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_ENTRY).time()) {
      final var s3Key = record.getS3Key();
      final var downloadedFile = downloadAndCheckMD5(s3Key);
      return new ManifestEntry(record, downloadedFile);
    }
  }

  public boolean allEntriesExistInS3(S3ManifestFile record) {
    final var manifestS3Prefix = S3FileManager.extractPrefixFromS3Key(record.getS3Key());
    final var namesAtPrefix = s3Files.fetchFileNamesWithPrefix(manifestS3Prefix);
    return record.getDataFiles().stream()
        .map(S3DataFile::getS3Key)
        .allMatch(namesAtPrefix::contains);
  }

  public void markAsStarted(S3ManifestFile manifestFile) {
    if (manifestFile.getStatus() == S3ManifestFile.ManifestStatus.DISCOVERED) {
      manifestFile.setStatus(S3ManifestFile.ManifestStatus.STARTED);
      s3Records.updateS3ManifestAndDataFiles(manifestFile);
    }
  }

  public void markAsProcessed(S3ManifestFile manifestFile) {
    manifestFile.setStatus(S3ManifestFile.ManifestStatus.COMPLETED);
    s3Records.updateS3ManifestAndDataFiles(manifestFile);
  }

  public void markAsRejected(S3ManifestFile manifestFile) {
    manifestFile.setStatus(S3ManifestFile.ManifestStatus.REJECTED);
    s3Records.updateS3ManifestAndDataFiles(manifestFile);
  }

  private DownloadedFile downloadAndCheckMD5(String s3Key) throws IOException {
    final var manifestFile = s3Files.downloadFile(s3Key);
    if (s3Files.checkMD5(manifestFile, MD5ChecksumMetaDataField) == MISMATCH) {
      throw new IOException(
          String.format("MD5 checksum mismatch for file %s", manifestFile.getS3Key()));
    }
    return manifestFile;
  }

  private List<ParsedManifestId> scanS3ForManifests(Instant minTimestamp) {
    final var ineligibleS3Keys = s3Records.readIneligibleManifestS3Keys(minTimestamp);
    return Stream.concat(
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS),
            s3Files.scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS))
        .filter(s3Summary -> !ineligibleS3Keys.contains(s3Summary.getKey()))
        .flatMap(s3Summary -> parseManifestEntryFromS3Key(s3Summary).stream())
        .filter(parsedManifestId -> parsedManifestId.manifestId.isAfter(minTimestamp))
        .sorted()
        .toList();
  }

  private Optional<ParsedManifestId> parseManifestEntryFromS3Key(S3Dao.S3ObjectSummary s3Summary) {
    DataSetManifest.DataSetManifestId manifestId = parseManifestIdFromS3Key(s3Summary.getKey());
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

    public void deleteFile() throws IOException {
      fileData.delete();
    }

    public boolean isIncomplete() {
      return record.getStatus() != S3DataFile.FileStatus.COMPLETED;
    }

    public void markAsStarted() {
      if (record.getStatus() == S3DataFile.FileStatus.DISCOVERED) {
        record.setStatus(S3DataFile.FileStatus.STARTED);
        s3Records.updateS3ManifestAndDataFiles(record.getParentManifest());
      }
    }

    public void markAsCompleted() {
      record.setStatus(S3DataFile.FileStatus.COMPLETED);
      s3Records.updateS3ManifestAndDataFiles(record.getParentManifest());
    }
  }
}
