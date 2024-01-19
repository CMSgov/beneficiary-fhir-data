package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.AbstractLocalStackS3Test;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for {@link S3ManifestDbDao}. */
public class S3ManifestDbDaoIT extends AbstractLocalStackS3Test {
  /** Fixed time for manifests. */
  private static final Instant MANIFEST_TIMESTAMP = Instant.parse("2024-01-18T15:04:25Z");

  /** Fixed time for discovery of manifests. */
  private static final Instant DISCOVERY_TIMESTAMP = Instant.parse("2024-01-18T15:06:18Z");

  /** Object under test. */
  private S3ManifestDbDao dbDao;

  /** Set up a clean database and create object being tested. */
  @BeforeEach
  void setUp() {
    var entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    var transactionManager = new TransactionManager(entityManagerFactory);
    dbDao = new S3ManifestDbDao(transactionManager);
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /** Verifies that the entity is created when a record does not already exist in the database. */
  @Test
  void testCreateNewRecord() {
    final var sampleXmlManifest = createSampleXmlManifest();
    final var sampleDbManifest = createSampleDbManifest();

    // ensure record does not exist yet
    final var s3Key = sampleXmlManifest.getIncomingS3Key();
    assertThat(dbDao.readS3ManifestAndDataFiles(s3Key)).isNull();

    // ensure record is created and returned and matches the xml data set
    final var insertedManifest =
        dbDao.insertOrReadManifestAndDataFiles(s3Key, sampleXmlManifest, DISCOVERY_TIMESTAMP);
    assertEntitiesEqual(sampleDbManifest, insertedManifest, true);

    // Read it back and
    final var readManifest = dbDao.readS3ManifestAndDataFiles(s3Key);
    assertEntitiesEqual(sampleDbManifest, readManifest, false);
  }

  /**
   * Verifies that inconsistencies in the database version of a manifest are detected and reported
   * via an exception.
   */
  @Test
  void testInconsistencyDetectionWhenEntryCountDoesNotMatch() {
    final var sampleXmlManifest = createSampleXmlManifest();
    final var sampleDbManifest = createSampleDbManifest();

    // Write a version of the entity with fewer entries.
    sampleDbManifest.getDataFiles().remove(1);
    dbDao.updateS3ManifestAndDataFiles(sampleDbManifest);

    // Now try to read it back and verify the exception is thrown properly.
    assertThatThrownBy(
            () ->
                dbDao.insertOrReadManifestAndDataFiles(
                    sampleXmlManifest.getIncomingS3Key(), sampleXmlManifest, DISCOVERY_TIMESTAMP))
        .isInstanceOf(S3ManifestDbDao.DataSetOutOfSyncException.class)
        .hasMessage("number of entries do not match: db=1 xml=2");
  }

  /**
   * Verifies that inconsistencies in the database version of a manifest are detected and reported
   * via an exception.
   */
  @Test
  void testInconsistencyDetectionWhenEntryOrderDoesNotMatch() {
    final var sampleXmlManifest = createSampleXmlManifest();
    final var sampleDbManifest = createSampleDbManifest();

    // Write a version of the entity.
    dbDao.updateS3ManifestAndDataFiles(sampleDbManifest);

    // Change the expected order of entries in the XML manifest so it won't match the database.
    Collections.reverse(sampleXmlManifest.getEntries());

    // Now try to read it back and verify the exception is thrown properly.
    assertThatThrownBy(
            () ->
                dbDao.insertOrReadManifestAndDataFiles(
                    sampleXmlManifest.getIncomingS3Key(), sampleXmlManifest, DISCOVERY_TIMESTAMP))
        .isInstanceOf(S3ManifestDbDao.DataSetOutOfSyncException.class)
        .hasMessage("entry name mismatch: index=0 db=beneficiary.csv xml=pde.csv");
  }

  /**
   * Verifies that inconsistencies in the database version of a manifest are detected and reported
   * via an exception.
   */
  @Test
  void testInconsistencyDetectionWhenEntryTypeDoesNotMatch() {
    final var sampleXmlManifest = createSampleXmlManifest();
    final var sampleDbManifest = createSampleDbManifest();

    // Write a version of the entity with first entry having a different type.
    sampleDbManifest.getDataFiles().get(1).setFileType(RifFileType.SNF.name());
    dbDao.updateS3ManifestAndDataFiles(sampleDbManifest);

    // Now try to read it back and verify the exception is thrown properly.
    assertThatThrownBy(
            () ->
                dbDao.insertOrReadManifestAndDataFiles(
                    sampleXmlManifest.getIncomingS3Key(), sampleXmlManifest, DISCOVERY_TIMESTAMP))
        .isInstanceOf(S3ManifestDbDao.DataSetOutOfSyncException.class)
        .hasMessage("entry type mismatch: index=1 name=pde.csv db=SNF xml=PDE");
  }

  /**
   * Verifies that manifests discovered after the min time stamp having ineligible status are
   * included in the set and others are not.
   */
  @Test
  void testReadIneligibleManifestS3Keys() {
    final var manifestBeforeMinTime = createSampleDbManifest();
    final var manifestRejected = createSampleDbManifest();
    final var manifestOk = createSampleDbManifest();
    final var manifestComplete = createSampleDbManifest();
    final var allManifests =
        List.of(manifestBeforeMinTime, manifestRejected, manifestOk, manifestComplete);

    manifestBeforeMinTime.setDiscoveryTimestamp(DISCOVERY_TIMESTAMP.minus(Duration.ofHours(1)));
    manifestRejected.setStatus(S3ManifestFile.ManifestStatus.REJECTED);
    manifestComplete.setStatus(S3ManifestFile.ManifestStatus.COMPLETED);

    int valueToMakeS3KeysUnique = 1;
    for (S3ManifestFile manifest : allManifests) {
      manifest.setS3Key(manifest.getS3Key() + valueToMakeS3KeysUnique);
      for (S3DataFile dataFile : manifest.getDataFiles()) {
        dataFile.setS3Key(dataFile.getS3Key() + valueToMakeS3KeysUnique);
      }
      dbDao.updateS3ManifestAndDataFiles(manifest);
      valueToMakeS3KeysUnique += 1;
    }

    assertEquals(
        Set.of(manifestRejected.getS3Key(), manifestComplete.getS3Key()),
        dbDao.readIneligibleManifestS3Keys(DISCOVERY_TIMESTAMP.minus(Duration.ofMinutes(1))));
  }

  /**
   * Create a {@link DataSetManifest} with known values for use in tests.
   *
   * @return the data set
   */
  private DataSetManifest createSampleXmlManifest() {
    final var entries = new ArrayList<DataSetManifest.DataSetManifestEntry>();
    entries.add(
        new DataSetManifest.DataSetManifestEntry("beneficiary.csv", RifFileType.BENEFICIARY));
    entries.add(new DataSetManifest.DataSetManifestEntry("pde.csv", RifFileType.PDE));
    final var manifest = new DataSetManifest(MANIFEST_TIMESTAMP, 0, false, entries);
    manifest.setManifestKeyIncomingLocation(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS);
    manifest.setManifestKeyDoneLocation(CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS);
    return manifest;
  }

  /**
   * Create a {@link S3ManifestFile} with known values for use in tests. Contains a properly mapped
   * version of the {@link DataSetManifest} returned by {@link #createSampleXmlManifest()}.
   *
   * @return the entity
   */
  private S3ManifestFile createSampleDbManifest() {
    final var baseS3Key =
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS + "/" + MANIFEST_TIMESTAMP + "/";

    var manifest = new S3ManifestFile();
    manifest.setManifestId(1);
    manifest.setS3Key(baseS3Key + "0_manifest.xml");
    manifest.setStatus(S3ManifestFile.ManifestStatus.DISCOVERED);
    manifest.setStatusTimestamp(DISCOVERY_TIMESTAMP);
    manifest.setManifestTimestamp(MANIFEST_TIMESTAMP);
    manifest.setDiscoveryTimestamp(DISCOVERY_TIMESTAMP);

    var entry = new S3DataFile();
    entry.setParentManifest(manifest);
    entry.setIndex((short) 0);
    entry.setS3Key(baseS3Key + "beneficiary.csv");
    entry.setFileName("beneficiary.csv");
    entry.setFileType(RifFileType.BENEFICIARY.name());
    entry.setStatus(S3DataFile.FileStatus.DISCOVERED);
    entry.setStatusTimestamp(DISCOVERY_TIMESTAMP);
    entry.setDiscoveryTimestamp(DISCOVERY_TIMESTAMP);
    manifest.getDataFiles().add(entry);

    entry = new S3DataFile();
    entry.setParentManifest(manifest);
    entry.setIndex((short) 1);
    entry.setS3Key(baseS3Key + "pde.csv");
    entry.setFileName("pde.csv");
    entry.setFileType(RifFileType.PDE.name());
    entry.setStatus(S3DataFile.FileStatus.DISCOVERED);
    entry.setStatusTimestamp(DISCOVERY_TIMESTAMP);
    entry.setDiscoveryTimestamp(DISCOVERY_TIMESTAMP);
    manifest.getDataFiles().add(entry);

    return manifest;
  }

  /**
   * Compare two objects using reflection. Manifestds can be copied prior to comparison for cases
   * where a new record has been inserted.
   *
   * @param expected expected value
   * @param actual actual value
   * @param copyManifestId set to true to copy actual manifestId prior to comparison
   */
  private void assertEntitiesEqual(
      S3ManifestFile expected, @Nullable S3ManifestFile actual, boolean copyManifestId) {
    assertThat(actual).isNotNull();

    // Copies manifestId and recordId values because in the database they are assigned by a sequence
    // and can change as tests are run.
    if (copyManifestId) {
      // manifestId is easy because there is only the one field
      expected.setManifestId(actual.getManifestId());
    }

    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
