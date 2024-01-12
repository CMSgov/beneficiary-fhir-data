package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key;
import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileCache.MD5Result.MISMATCH;

import com.google.common.io.ByteSource;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.DataSetManifestFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class NewDataSetQueue {
  public static final String MD5ChecksumMetaDataField = "md5chksum";

  private final S3Dao s3Dao;
  private final String s3Bucket;
  private final String incomingS3KeyPrefix;
  private final S3FilesDao s3Records;
  private final S3FileCache s3Files;

  public List<EligibleManifest> readEligibleManifests(
      Instant currentTime,
      Instant minTime,
      Instant maxTime,
      ThrowingFunction<Boolean, EligibleManifest, IOException> acceptanceCriteria,
      int maxToRead)
      throws IOException {
    final List<ParsedManifestId> possiblyEligibleManifestIds = scanS3ForManifests(minTime, maxTime);
    final List<EligibleManifest> eligibleManifests = new ArrayList<>();
    for (ParsedManifestId manifestId : possiblyEligibleManifestIds) {
      if (eligibleManifests.size() >= maxToRead) {
        break;
      }
      final var s3Key = manifestId.getS3Key();
      final var manifestFile = downloadAndCheckMD5(s3Key);
      final var dataSetManifest = parseManifestFile(s3Key, manifestFile.getBytes());
      final var manifestRecord =
          s3Records.insertOrReadManifestAndDataFiles(s3Key, dataSetManifest, currentTime);
      final var manifest =
          new EligibleManifest(
              s3Key, manifestId.getManifestId(), manifestFile, dataSetManifest, manifestRecord);
      if (acceptanceCriteria.apply(manifest)) {
        eligibleManifests.add(manifest);
      }
    }
    return eligibleManifests;
  }

  private S3FileCache.DownloadedFile downloadAndCheckMD5(String s3Key) throws IOException {
    final var manifestFile = s3Files.downloadFile(s3Key);
    if (s3Files.checkMD5(manifestFile, MD5ChecksumMetaDataField) == MISMATCH) {
      throw new IOException(
          String.format("MD5 checksum mismatch for file %s", manifestFile.getS3Key()));
    }
    return manifestFile;
  }

  private List<ParsedManifestId> scanS3ForManifests(Instant minTimestamp, Instant maxTimestamp) {
    final var ineligibleS3Keys = s3Records.readIneligibleManifestS3Keys(minTimestamp);
    return s3Dao
        .listObjects(s3Bucket, incomingS3KeyPrefix)
        .filter(s3Summary -> !ineligibleS3Keys.contains(s3Summary.getKey()))
        .flatMap(s3Summary -> parseManifestEntryFromS3Key(s3Summary).stream())
        .filter(parsedManifestId -> parsedManifestId.manifestId.isAfter(minTimestamp))
        .filter(parsedManifestId -> parsedManifestId.manifestId.isBefore(maxTimestamp))
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
  public static class EligibleManifest {
    private final String s3Key;
    private final DataSetManifest.DataSetManifestId manifestId;
    private final S3FileCache.DownloadedFile manifestFile;
    private final DataSetManifest manifest;
    private final S3ManifestFile record;
  }
}
