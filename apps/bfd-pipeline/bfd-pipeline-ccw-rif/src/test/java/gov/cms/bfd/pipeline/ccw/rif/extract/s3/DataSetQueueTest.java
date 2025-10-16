package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetQueue.MD5_CHECKSUM_META_DATA_FIELD;
import static gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileManager.MD5Result.MISMATCH;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.ByteSource;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link DataSetQueue}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DataSetQueueTest {
  /** Base time used for simulated clock. */
  private static final Instant BASE_TIME_FOR_CLOCK = Instant.parse("2024-01-23T14:48:49+00:00");

  /** Used to check timestamp parsed from the {@link #SAMPLE_MANIFEST}. */
  private static final String MANIFEST_FILE_TIMESTAMP = "2024-01-19T16:16:38Z";

  /** Used to validate parsing of manifests. */
  private static final String SAMPLE_MANIFEST =
"""
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<bbr:dataSetManifest xmlns:bbr="http://cms.hhs.gov/bluebutton/api/schema/ccw-rif/v10" timestamp="2024-01-19T16:16:38Z" sequenceId="0">
  <bbr:entry name="beneficiary.csv" type="BENEFICIARY"/>
</bbr:dataSetManifest>
""";

  /** Used as an incrementing epoc millis by the mock clock. */
  private AtomicLong clockMillis;

  /** Used to access current time. */
  @Mock private Clock clock;

  /** Used to track S3 files in the database. */
  @Mock private S3ManifestDbDao s3Records;

  /** Used to download files from S3 to a local cache for processing. */
  @Mock private S3FileManager s3Files;

  /** Used to confirm timers are created as expected. */
  @Spy private MetricRegistry appMetrics;

  /** Object being tested. Created as a spy in {@link #setUp}. */
  private DataSetQueue dataSetQueue;

  /** Sets up object being tested and a simulated clock with known time. */
  @BeforeEach
  void setUp() {
    // have the clock use a 1 second incrementing time for the clock
    clockMillis = new AtomicLong(BASE_TIME_FOR_CLOCK.toEpochMilli());
    doAnswer(i -> Instant.ofEpochMilli(clockMillis.getAndAdd(1_000L))).when(clock).instant();

    // object being tested just uses our mocks
    dataSetQueue = spy(new DataSetQueue(clock, appMetrics, s3Records, s3Files));
  }

  /**
   * Verify that {@link DataSetQueue#readEligibleManifests} returns the expected manifests and
   * respects the time range and maximum number of manifests limit.
   *
   * @throws Exception pass through from method signatures
   */
  @Test
  void testReadEligibleManifests() throws Exception {
    final var currentTimestamp = BASE_TIME_FOR_CLOCK;
    final var minimumAllowedManifestTimestamp = currentTimestamp.minusSeconds(500);
    // Create our sample data and tell the spy how to return each set of data when methods are
    // called.
    // Accumulate the keys and ids in lists because we need them later.
    final List<String> s3Keys = new ArrayList<>();
    final List<DataSetQueue.ParsedManifestId> manifestIds = new ArrayList<>();

    // We are creating more than the actual call will only process the first 2
    // just so we can confirm they were skipped.
    for (int sequenceId = 0; sequenceId < 5; ++sequenceId) {
      final var s3Key =
          createManifestS3Key(
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              currentTimestamp.getEpochSecond(),
              sequenceId);
      s3Keys.add(s3Key);

      // we'll need these ids as return value from scanS3ForManifests().
      final var manifestId = mock(DataSetQueue.ParsedManifestId.class);
      doReturn(s3Key).when(manifestId).manifestS3Key();
      manifestIds.add(manifestId);

      // Every manifest is downloaded so simulate that action.
      final var downloadedFile = mock(S3DirectoryDao.DownloadedFile.class);
      doReturn(s3Key).when(downloadedFile).getS3Key();
      doReturn(downloadedFile).when(dataSetQueue).downloadFileAndCheckMD5(s3Key);

      // Every manifest is parsed.
      final var dataSetManifest = mock(DataSetManifest.class);
      doReturn(s3Key).when(dataSetManifest).getIncomingS3Key();
      doReturn(dataSetManifest).when(dataSetQueue).parseManifestFile(eq(s3Key), any());

      // Every manifest is written to the database so simulate that happening too.
      final var manifestRecord = new S3ManifestFile();
      manifestRecord.setS3Key(s3Key);
      doReturn(manifestRecord)
          .when(s3Records)
          .insertOrReadManifestAndDataFiles(s3Key, dataSetManifest, currentTimestamp);
    }

    // Ensure all of our ids are returned when S3 is scanned for manifests.
    doReturn(manifestIds).when(dataSetQueue).scanS3ForManifests(any(), any());

    // Now make the call for a maximum of 2 records so we can confirm the size cap is enforced.
    // Also using a predicate that skips the second s3 key just to confirm that logic works as well.
    var result =
        dataSetQueue.readEligibleManifests(
            currentTimestamp,
            minimumAllowedManifestTimestamp,
            dataSetManifest -> !dataSetManifest.getIncomingS3Key().equals(s3Keys.get(1)),
            2);

    // Just extract the s3 keys and ensure we received all of the expected ones and no more.
    var resultKeys =
        result.stream().map(manifestRecord -> manifestRecord.manifestRecord().getS3Key()).toList();
    assertEquals(List.of(s3Keys.get(0), s3Keys.get(2)), resultKeys);

    // verify our timers were updated
    verify(appMetrics).timer(DataSetQueue.TIMER_READ_MANIFESTS);
    verify(appMetrics, times(3)).timer(DataSetQueue.TIMER_DOWNLOAD_MANIFEST);
    verify(appMetrics, times(3)).timer(DataSetQueue.TIMER_MANIFEST_DB_UPDATE);
  }

  /**
   * Verify that {@link DataSetQueue#downloadManifestEntry} downloads the file and creates a timer
   * to track the time.
   */
  @Test
  void testDownloadManifestEntry() throws IOException {
    String s3Key =
        createManifestS3Key(
            CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, BASE_TIME_FOR_CLOCK.getEpochSecond(), 1);
    var downloadedFile = mock(S3DirectoryDao.DownloadedFile.class);
    doReturn("/a/b/c.dat").when(downloadedFile).getAbsolutePath();
    doReturn(downloadedFile).when(dataSetQueue).downloadFileAndCheckMD5(s3Key);

    var entryRecord = S3DataFile.builder().s3Key(s3Key).build();
    var manifestEntry = dataSetQueue.downloadManifestEntry(entryRecord);
    assertSame("/a/b/c.dat", manifestEntry.getCachedFilePath());

    verify(appMetrics).timer(DataSetQueue.TIMER_DOWNLOAD_ENTRY);
  }

  /**
   * Verify that if any files are not in S3 {@link DataSetQueue#allEntriesExistInS3} returns false.
   */
  @Test
  void testNotAllEntriesExistInS3() {
    final String s3Key =
        createManifestS3Key(
            CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, BASE_TIME_FOR_CLOCK.getEpochSecond(), 1);
    final String keyPrefix = S3FileManager.extractPrefixFromS3Key(s3Key);
    final String fileAKey = keyPrefix + "a";
    doReturn(Set.of(fileAKey)).when(s3Files).fetchKeysWithPrefix(keyPrefix);

    var manifestRecord = new S3ManifestFile();
    manifestRecord.setS3Key(s3Key);
    manifestRecord.getDataFiles().add(S3DataFile.builder().s3Key(keyPrefix + "a").build());
    manifestRecord.getDataFiles().add(S3DataFile.builder().s3Key(keyPrefix + "b").build());
    assertFalse(dataSetQueue.allEntriesExistInS3(manifestRecord));
  }

  /**
   * Verify that if any files are not in S3 {@link DataSetQueue#allEntriesExistInS3} returns false.
   */
  @Test
  void testAllEntriesExistInS3() {
    String s3Key =
        createManifestS3Key(
            CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, BASE_TIME_FOR_CLOCK.getEpochSecond(), 1);
    String keyPrefix = S3FileManager.extractPrefixFromS3Key(s3Key);
    var fileAKey = keyPrefix + "a";
    var fileBKey = keyPrefix + "b";
    doReturn(Set.of(fileAKey, fileBKey)).when(s3Files).fetchKeysWithPrefix(keyPrefix);

    var manifestRecord = new S3ManifestFile();
    manifestRecord.setS3Key(s3Key);
    manifestRecord.getDataFiles().add(S3DataFile.builder().s3Key(fileAKey).build());
    manifestRecord.getDataFiles().add(S3DataFile.builder().s3Key(fileBKey).build());
    Assertions.assertTrue(dataSetQueue.allEntriesExistInS3(manifestRecord));
  }

  /** Verify that manifests can be started and are written with proper status and timestamp. */
  @Test
  void testMarkManifestAsStarted() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.DISCOVERED);

    dataSetQueue.markAsStarted(manifestRecord);
    assertEquals(S3ManifestFile.ManifestStatus.STARTED, manifestRecord.getStatus());
    assertEquals(BASE_TIME_FOR_CLOCK, manifestRecord.getStatusTimestamp());
    verify(s3Records).updateS3ManifestAndDataFiles(manifestRecord);
  }

  /** Verify that completed manifests cannot be started. */
  @Test
  void testMarkManifestAsStartedFailsForCompletedRecord() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.COMPLETED);

    assertThatThrownBy(() -> dataSetQueue.markAsStarted(manifestRecord))
        .isInstanceOf(BadCodeMonkeyException.class);
  }

  /** Verify that manifests can be rejected and are written with proper status and timestamp. */
  @Test
  void testMarkManifestAsProcessed() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.STARTED);

    dataSetQueue.markAsProcessed(manifestRecord);
    assertEquals(S3ManifestFile.ManifestStatus.COMPLETED, manifestRecord.getStatus());
    assertEquals(BASE_TIME_FOR_CLOCK, manifestRecord.getStatusTimestamp());
    verify(s3Records).updateS3ManifestAndDataFiles(manifestRecord);
  }

  /** Verify that manifests can be rejected and are written with proper status and timestamp. */
  @Test
  void testMarkManifestAsRejected() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    manifestRecord.setStatus(S3ManifestFile.ManifestStatus.DISCOVERED);

    dataSetQueue.markAsRejected(manifestRecord);
    assertEquals(S3ManifestFile.ManifestStatus.REJECTED, manifestRecord.getStatus());
    assertEquals(BASE_TIME_FOR_CLOCK, manifestRecord.getStatusTimestamp());
    verify(s3Records).updateS3ManifestAndDataFiles(manifestRecord);
  }

  /** Verify that data files can be started and are written with proper status and timestamp. */
  @Test
  void testMarkDataFileAsStarted() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    S3DataFile dataFileRecord = new S3DataFile();
    dataFileRecord.setParentManifest(manifestRecord);
    dataFileRecord.setStatus(S3DataFile.FileStatus.DISCOVERED);

    dataSetQueue.markAsStarted(dataFileRecord);
    assertEquals(S3DataFile.FileStatus.STARTED, dataFileRecord.getStatus());
    assertEquals(BASE_TIME_FOR_CLOCK, dataFileRecord.getStatusTimestamp());
    verify(s3Records).updateS3ManifestAndDataFiles(manifestRecord);
  }

  /** Verify that completed data files cannot be started. */
  @Test
  void testMarkDataFileAsStartedFailsForCompletedRecord() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    S3DataFile dataFileRecord = new S3DataFile();
    dataFileRecord.setParentManifest(manifestRecord);
    dataFileRecord.setStatus(S3DataFile.FileStatus.COMPLETED);

    assertThatThrownBy(() -> dataSetQueue.markAsStarted(dataFileRecord))
        .isInstanceOf(BadCodeMonkeyException.class);
  }

  /** Verify that data files can be completed and are written with proper status and timestamp. */
  @Test
  void testMarkDataFileAsCompleted() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    S3DataFile dataFileRecord = new S3DataFile();
    dataFileRecord.setParentManifest(manifestRecord);
    dataFileRecord.setStatus(S3DataFile.FileStatus.DISCOVERED);

    dataSetQueue.markAsCompleted(dataFileRecord);
    assertEquals(S3DataFile.FileStatus.COMPLETED, dataFileRecord.getStatus());
    assertEquals(BASE_TIME_FOR_CLOCK, dataFileRecord.getStatusTimestamp());
    verify(s3Records).updateS3ManifestAndDataFiles(manifestRecord);
  }

  /** Verify that completed data files cannot be completed again. */
  @Test
  void testMarkDataFileAsCompletedFailsForCompletedRecord() {
    S3ManifestFile manifestRecord = new S3ManifestFile();
    S3DataFile dataFileRecord = new S3DataFile();
    dataFileRecord.setParentManifest(manifestRecord);
    dataFileRecord.setStatus(S3DataFile.FileStatus.COMPLETED);

    assertThatThrownBy(() -> dataSetQueue.markAsCompleted(dataFileRecord))
        .isInstanceOf(BadCodeMonkeyException.class);
  }

  /**
   * Verifies that if a file is downloaded and checksum does not match that an {@link IOException}
   * is thrown.
   *
   * @throws Exception pass through from message signatures
   */
  @Test
  void testInvalidChecksumThrowsIOException() throws Exception {
    final String s3Key = "some/s3/key";
    final S3DirectoryDao.DownloadedFile downloadResult = mock(S3DirectoryDao.DownloadedFile.class);
    doReturn(downloadResult).when(s3Files).downloadFile(s3Key);
    doReturn(s3Key).when(downloadResult).getS3Key();
    doReturn(MISMATCH).when(s3Files).checkMD5(downloadResult, MD5_CHECKSUM_META_DATA_FIELD);
    assertThatThrownBy(() -> dataSetQueue.downloadFileAndCheckMD5(s3Key))
        .isInstanceOf(IOException.class)
        .hasMessage("MD5 checksum mismatch for file " + s3Key);
  }

  /** Verifies that the filters in {@link DataSetQueue#scanS3ForManifests} filter keys properly. */
  @Test
  void testScanS3ForManifests() {
    long nextSecond = BASE_TIME_FOR_CLOCK.getEpochSecond();
    // Two keys that will be skipped because they come before min timestamp
    final String oldNormalS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, nextSecond++, 0);
    final String oldSyntheticS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS, nextSecond++, 0);
    final long minimumAllowedEpochSecond = nextSecond++;

    // Two keys that will be skipped because we will have s3Files return them as ineligible
    final String ineligibleNormalS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, nextSecond++, 1);
    final String ineligibleSyntheticS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS, nextSecond++, 1);

    // Two keys that will be returned as eligible because nothing else eliminates them
    final String eligibleNormalS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, nextSecond++, 1);
    final String eligibleSyntheticS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS, nextSecond++, 1);
    final long maximumAllowedEpochSecond = nextSecond++;

    // Two keys that will be skipped because they come after max timestamp
    final String futureNormalS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, nextSecond++, 0);
    final String futureSyntheticS3Key =
        createManifestS3Key(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS, nextSecond++, 0);

    // mock database indicating two otherwise eligible keys are ineligible
    final Instant minimumAllowedManifestTimestamp =
        Instant.ofEpochSecond(minimumAllowedEpochSecond);
    final Instant maximumAllowedManifestTimestamp =
        Instant.ofEpochSecond(maximumAllowedEpochSecond);
    doReturn(Set.of(ineligibleSyntheticS3Key, ineligibleNormalS3Key))
        .when(s3Records)
        .readIneligibleManifestS3Keys(minimumAllowedManifestTimestamp);

    // mock s3 folder scans returning our simulated keys
    doReturn(
            Stream.of(oldNormalS3Key, ineligibleNormalS3Key, eligibleNormalS3Key, futureNormalS3Key)
                .map(this::createSummaryForS3Key))
        .when(s3Files)
        .scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS);
    doReturn(
            Stream.of(
                    oldSyntheticS3Key,
                    ineligibleSyntheticS3Key,
                    eligibleSyntheticS3Key,
                    futureSyntheticS3Key)
                .map(this::createSummaryForS3Key))
        .when(s3Files)
        .scanS3ForFiles(CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS);

    // now ensure the method returned only the keys that it should have
    final var manifestIds =
        dataSetQueue.scanS3ForManifests(
            minimumAllowedManifestTimestamp, maximumAllowedManifestTimestamp);
    assertEquals(
        List.of(
            new DataSetQueue.ParsedManifestId(
                eligibleNormalS3Key,
                DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key(eligibleNormalS3Key)),
            new DataSetQueue.ParsedManifestId(
                eligibleSyntheticS3Key,
                DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key(
                    eligibleSyntheticS3Key))),
        manifestIds);
  }

  /** Parses a manifest id from an S3 key and verifies correct components. */
  @Test
  void testParseManifestIdFromS3Key() {
    // verify an invalid key doesn't blow up
    assertEquals(Optional.empty(), dataSetQueue.parseManifestIdFromS3Key("not a valid key"));

    // verify valid key parses correctly
    final String s3Key =
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS
            + "/"
            + MANIFEST_FILE_TIMESTAMP
            + "/3_manifest.xml";
    final var parsedId = dataSetQueue.parseManifestIdFromS3Key(s3Key).orElse(null);
    assertNotNull(parsedId);
    assertEquals(s3Key, parsedId.manifestS3Key());
    assertEquals(
        DataSetManifest.DataSetManifestId.parseManifestIdFromS3Key(s3Key), parsedId.manifestId());
  }

  /** Parses a manifest from a normal directory and ensures key locations are set properly. */
  @Test
  void testManifestFileParsingNormalIncomingFolder() {
    final ByteSource manifestBytes =
        ByteSource.wrap(SAMPLE_MANIFEST.getBytes(StandardCharsets.UTF_8));
    String s3Key =
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS
            + "/"
            + MANIFEST_FILE_TIMESTAMP
            + "/0_manifest.xml";
    DataSetManifest manifest = dataSetQueue.parseManifestFile(s3Key, manifestBytes);
    assertEquals(s3Key, manifest.getIncomingS3Key());
    assertEquals(
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, manifest.getManifestKeyIncomingLocation());
  }

  /** Parses a manifest from a synthetic directory and ensures key locations are set properly. */
  @Test
  void testManifestFileParsingSyntheticIncomingFolder() {
    final ByteSource manifestBytes =
        ByteSource.wrap(SAMPLE_MANIFEST.getBytes(StandardCharsets.UTF_8));
    String s3Key =
        CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
            + "/"
            + MANIFEST_FILE_TIMESTAMP
            + "/0_manifest.xml";
    DataSetManifest manifest = dataSetQueue.parseManifestFile(s3Key, manifestBytes);
    assertEquals(s3Key, manifest.getIncomingS3Key());
    assertEquals(
        CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
        manifest.getManifestKeyIncomingLocation());
  }

  /**
   * Create an S3 key for a manifest with the specified components.
   *
   * @param prefix incoming directory path
   * @param epochSecond epoch second for computing timestamp in path
   * @param sequenceId manifest sequence number
   * @return the S3 key
   */
  private String createManifestS3Key(String prefix, long epochSecond, int sequenceId) {
    return prefix + "/" + Instant.ofEpochSecond(epochSecond) + "/" + sequenceId + "_manifest.xml";
  }

  /**
   * Creates a minimal object summary from an s3 key. We only care about the key for our tests so
   * the other values are hard coded.
   *
   * @param s3Key the S3 key
   * @return the object summary
   */
  private S3Dao.S3ObjectSummary createSummaryForS3Key(String s3Key) {
    return new S3Dao.S3ObjectSummary(s3Key, "some-etag", 100L, Instant.now());
  }
}
