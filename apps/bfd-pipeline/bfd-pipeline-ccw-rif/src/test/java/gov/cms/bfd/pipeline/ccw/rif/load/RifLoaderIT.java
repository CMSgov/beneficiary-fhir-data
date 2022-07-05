package gov.cms.bfd.pipeline.ccw.rif.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.codahale.metrics.Slf4jReporter;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryColumn;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.SkippedRifRecord;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.ccw.rif.extract.LocalRifFile;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link RifLoader}. */
public final class RifLoaderIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIT.class);
  private static final boolean USE_INSERT_IDEMPOTENT_STRATEGY = true;
  private static final boolean USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY = false;

  /** Ensures that each test case here starts with a clean/empty database, with the right schema. */
  @BeforeEach
  public void prepareTestDatabase(TestInfo testInfo) {
    LOGGER.info("{}: starting.", testInfo.getDisplayName());
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  @AfterEach
  public void finished(TestInfo testInfo) {
    LOGGER.info("{}: finished.", testInfo.getDisplayName());
  };

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data. */
  @Test
  public void loadSampleA() {
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    verifyRecordPrimaryKeysPresent(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Ensure no records were skipped
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the modified {@link StaticRifResourceGroup#SAMPLE_A} data for an
   * <code>UPDATE</code> on a {@link Beneficiary} record that has a single file with multiple
   * reference years for the same bene id. The beneMonthly data should be populated for the bene in
   * the beneMonthly table for each ref year, and HICN and MBIs are also populated.
   */
  @Test
  public void multipleReferenceYearsInSingleFileForSameBeneExpectBeneMonthlyPopulated() {
    // load the file so it exists
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_A_WITHOUT_REFERENCE_YEAR.getResources()));

    // Set the batch to 10 so all records are in the same batch
    LoadAppOptions options = CcwRifLoadTestUtils.getLoadOptionsWithBatchSize(10);

    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_ENTRIES_SAME_BENE.getResources()),
        options);

    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
    // Validate bene monthly
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks all 12 months are in beneficiary monthlys for that beneficiary, for each 6 records
      // loaded (6x12)
      assertEquals(72, beneficiaryFromDb.getBeneficiaryMonthlys().size());
      // Checks every month in the beneficiary monthly table for each of the years loaded
      assertBeneficiaryMonthly(beneficiaryFromDb, 1990);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2000);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2001);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2003);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2019);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2022);

      // Validate HICN and MBI
      String expectedMbiHash =
          RifLoader.computeMbiHash(new IdHasher(options.getIdHasherConfig()), "3456789");
      String expectedHicn =
          RifLoader.computeHicnHash(new IdHasher(options.getIdHasherConfig()), "543217066U");
      assertEquals(
          expectedMbiHash,
          beneficiaryFromDb.getMbiHash().orElse(null),
          "Beneficiary has incorrect mbiHash");
      assertEquals(expectedHicn, beneficiaryFromDb.getHicn(), "Beneficiary has incorrect HICN");
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Runs {@link RifLoader} against the modified {@link StaticRifResourceGroup#SAMPLE_A} data for an
   * <code>UPDATE</code> on a {@link Beneficiary} record that has a single file with multiple
   * reference years for the same bene id, and does not fit in a single batch. The beneMonthly data
   * should be populated for the bene in the beneMonthly table for each ref year, and HICN and MBIs
   * are also populated.
   */
  @Test
  public void
      multipleReferenceYearsInSingleFileForSameBeneInMultipleBatchesExpectBeneMonthlyPopulated() {
    // load the file initially so it exists
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_A_WITHOUT_REFERENCE_YEAR.getResources()));

    // Set the batch to 2 so this is split into multiple batches
    LoadAppOptions options = CcwRifLoadTestUtils.getLoadOptionsWithBatchSize(2);
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_ENTRIES_SAME_BENE.getResources()),
        options);

    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
    // Validate bene monthly
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks all 12 months are in beneficiary monthlys for that beneficiary, for each 6 records
      // loaded (6x12)
      assertEquals(72, beneficiaryFromDb.getBeneficiaryMonthlys().size());
      // Checks every month in the beneficiary monthly table for each of the years loaded
      assertBeneficiaryMonthly(beneficiaryFromDb, 1990);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2000);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2001);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2003);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2019);
      assertBeneficiaryMonthly(beneficiaryFromDb, 2022);

      // Validate HICN and MBI
      String expectedMbiHash =
          RifLoader.computeMbiHash(new IdHasher(options.getIdHasherConfig()), "3456789");
      String expectedHicn =
          RifLoader.computeHicnHash(new IdHasher(options.getIdHasherConfig()), "543217066U");
      assertEquals(
          expectedMbiHash,
          beneficiaryFromDb.getMbiHash().orElse(null),
          "Beneficiary has incorrect mbiHash");
      assertEquals(expectedHicn, beneficiaryFromDb.getHicn(), "Beneficiary has incorrect HICN");
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data for an <code>
   * UPDATE</code> {@link Beneficiary} record that there hasn't been a previous <code>INSERT</code>
   * on, to verify that this fails as expected.
   */
  @Test
  public void failOnUpdateBeneficiaryBeforeInsert() {
    // Tweak the SAMPLE_A beneficiary to be an UPDATE.
    Stream<RifFile> samplesStream =
        filterSamples(
            r -> r.getFileType() == RifFileType.BENEFICIARY,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    Function<RifRecordEvent<?>, List<List<String>>> recordEditor =
        rifRecordEvent -> {
          CSVRecord beneCsvRow = rifRecordEvent.getRawCsvRecords().get(0);
          List<String> beneCsvValues =
              StreamSupport.stream(beneCsvRow.spliterator(), false).collect(Collectors.toList());
          beneCsvValues.set(0, "UPDATE");
          return List.of(beneCsvValues);
        };
    Function<RifFile, RifFile> fileEditor = sample -> editSampleRecords(sample, recordEditor);
    Stream<RifFile> editedSample = editSamples(samplesStream, fileEditor);

    // Load the edited sample to verify that it fails, as expected.
    AssertionFailedError thrown =
        assertThrows(
            AssertionFailedError.class,
            () -> {
              loadSample(
                  "SAMPLE_A, bene only, UPDATE",
                  CcwRifLoadTestUtils.getLoadOptions(),
                  editedSample);
            });

    assertTrue(thrown.getMessage().contains("Load errors encountered"));
  }

  @Test
  public void singleFileLoad() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              // Verify that LoadedFile entity
              loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
              final List<LoadedFile> loadedFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              assertTrue(loadedFiles.size() > 1, "Expected to have many loaded files in SAMPLE A");
              final LoadedFile loadedFile = loadedFiles.get(0);
              assertNotNull(loadedFile.getCreated());

              // Verify that beneficiaries table was loaded
              final List<LoadedBatch> batches =
                  loadBatches(entityManager, loadedFile.getLoadedFileId());
              final LoadedBatch allBatches = batches.stream().reduce(null, LoadedBatch::combine);
              assertTrue(batches.size() > 0, "Expected to have at least one beneficiary loaded");
              assertEquals(
                  "567834",
                  allBatches.getBeneficiariesAsList().get(0),
                  "Expected to match the sample-a beneficiary");
            });
  }

  @Test
  @Disabled
  public void multipleFileLoads() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              // Verify that a loaded files exsits
              loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
              final List<LoadedFile> beforeLoadedFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              assertTrue(beforeLoadedFiles.size() > 0, "Expected to have at least one file");
              LoadedFile beforeLoadedFile = beforeLoadedFiles.get(0);
              LoadedFile beforeOldestFile = beforeLoadedFiles.get(beforeLoadedFiles.size() - 1);

              PipelineTestUtils.get().pauseMillis(10);
              loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

              // Verify that the loaded list was updated properly
              final List<LoadedFile> afterLoadedFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              assertTrue(
                  beforeLoadedFiles.size() < afterLoadedFiles.size(),
                  "Expected to have more loaded files");
              final LoadedFile afterLoadedFile = afterLoadedFiles.get(0);
              final LoadedFile afterOldestFile = afterLoadedFiles.get(afterLoadedFiles.size() - 1);
              assertEquals(
                  beforeOldestFile.getLoadedFileId(),
                  afterOldestFile.getLoadedFileId(),
                  "Expected same oldest file");
              assertTrue(
                  beforeLoadedFile.getCreated().isBefore(afterLoadedFile.getCreated()),
                  "Expected range to expand");
            });
  }

  @Test
  public void trimLoadedFiles() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              // Setup a loaded file with an old date
              loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
              final List<LoadedFile> loadedFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              final EntityTransaction txn = entityManager.getTransaction();
              txn.begin();
              LoadedFile oldFile = loadedFiles.get(loadedFiles.size() - 1);
              oldFile.setCreated(Instant.now().minus(101, ChronoUnit.DAYS));
              txn.commit();

              // Look at the files now
              final List<LoadedFile> beforeFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              final Instant oldDate = Instant.now().minus(99, ChronoUnit.DAYS);
              assertTrue(
                  beforeFiles.stream().anyMatch(file -> file.getCreated().isBefore(oldDate)),
                  "Expect to have old files");

              // Load another set that will cause the old file to be trimmed
              loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

              // Verify that old file was trimmed
              final List<LoadedFile> afterFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              assertFalse(
                  afterFiles.stream().anyMatch(file -> file.getCreated().isBefore(oldDate)),
                  "Expect to not have old files");
            });
  }

  @Disabled
  @Test
  public void buildSyntheticLoadedFiles() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              loadSample(Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources()));
              // Verify that a loaded files exsits
              final List<LoadedFile> loadedFiles =
                  PipelineTestUtils.get().findLoadedFiles(entityManager);
              assertTrue(loadedFiles.size() > 0, "Expected to have at least one file");
              final LoadedFile file = loadedFiles.get(0);
              final List<LoadedBatch> batches = loadBatches(entityManager, file.getLoadedFileId());
              assertTrue(batches.size() > 0);
            });
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_U} data. */
  @Test
  @Disabled
  public void loadSampleU() {
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    verifyRecordPrimaryKeysPresent(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    // Ensure no records were skipped
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      CriteriaQuery<BeneficiaryHistory> beneficiaryHistoryCriteria =
          entityManager.getCriteriaBuilder().createQuery(BeneficiaryHistory.class);
      List<BeneficiaryHistory> beneficiaryHistoryEntries =
          entityManager
              .createQuery(
                  beneficiaryHistoryCriteria.select(
                      beneficiaryHistoryCriteria.from(BeneficiaryHistory.class)))
              .getResultList();
      for (BeneficiaryHistory beneHistory : beneficiaryHistoryEntries) {
        assertEquals("567834", beneHistory.getBeneficiaryId());
        // A recent lastUpdated timestamp
        assertTrue(beneHistory.getLastUpdated().isPresent(), "Expected a lastUpdated field");
        beneHistory
            .getLastUpdated()
            .ifPresent(
                lastUpdated -> {
                  assertTrue(
                      lastUpdated.isAfter(Instant.now().minus(10, ChronoUnit.MINUTES)),
                      "Expected a recent lastUpdated timestamp");
                });
      }
      assertEquals(4, beneficiaryHistoryEntries.size());

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Last Name inserted with value of "Johnson"
      assertEquals("Johnson", beneficiaryFromDb.getNameSurname());
      // Following fields were NOT changed in update record
      assertEquals("John", beneficiaryFromDb.getNameGiven());
      assertEquals(new Character('A'), beneficiaryFromDb.getNameMiddleInitial().get());
      assertEquals(
          Optional.of("SSSS"), beneficiaryFromDb.getMedicareBeneficiaryId(), "Beneficiary has MBI");
      assertEquals(
          Optional.of("401441595efcc68bc5b26f4e88bd9fa550004e068d69ff75761ab946ec553a02"),
          beneficiaryFromDb.getMbiHash(),
          "Beneficiary has mbiHash");
      // A recent lastUpdated timestamp
      assertTrue(beneficiaryFromDb.getLastUpdated().isPresent(), "Expected a lastUpdated field");
      beneficiaryFromDb
          .getLastUpdated()
          .ifPresent(
              lastUpdated -> {
                assertTrue(
                    lastUpdated.isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)),
                    "Expected a recent lastUpdated timestamp");
              });

      CarrierClaim carrierRecordFromDb = entityManager.find(CarrierClaim.class, "9991831999");
      assertEquals('N', carrierRecordFromDb.getFinalAction());
      // DateThrough inserted with value 10-27-1999
      assertEquals(LocalDate.of(2000, Month.OCTOBER, 27), carrierRecordFromDb.getDateThrough());
      assertEquals(1, carrierRecordFromDb.getLines().size());
      // A recent lastUpdated timestamp
      assertTrue(carrierRecordFromDb.getLastUpdated().isPresent(), "Expected a lastUpdated field");
      carrierRecordFromDb
          .getLastUpdated()
          .ifPresent(
              lastUpdated -> {
                assertTrue(
                    lastUpdated.isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)),
                    "Expected a recent lastUpdated timestamp");
              });

      CarrierClaimLine carrierLineRecordFromDb = carrierRecordFromDb.getLines().get(0);
      // CliaLabNumber inserted with value BB889999AA
      assertEquals("GG443333HH", carrierLineRecordFromDb.getCliaLabNumber().get());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_U} data. */
  @Test
  public void loadSampleUUnchanged() {
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // this should insert a new beneficiary history record
    /*
     * FIXME Why is this called "_UNCHANGED" if it will result in a new bene history record? Is the
     * name off, or are we still creating some unnecessary history records?
     */
    loadSample(Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));
    verifyRecordPrimaryKeysPresent(Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    long start = System.currentTimeMillis();
    // this should bypass inserting a new beneficiary history record because it already exists
    loadSample(Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      CriteriaQuery<BeneficiaryHistory> beneficiaryHistoryCriteria =
          entityManager.getCriteriaBuilder().createQuery(BeneficiaryHistory.class);
      List<BeneficiaryHistory> beneficiaryHistoryEntries =
          entityManager
              .createQuery(
                  beneficiaryHistoryCriteria.select(
                      beneficiaryHistoryCriteria.from(BeneficiaryHistory.class)))
              .getResultList();
      for (BeneficiaryHistory beneHistory : beneficiaryHistoryEntries) {
        assertEquals(567834L, beneHistory.getBeneficiaryId());
        // A recent lastUpdated timestamp
        assertTrue(beneHistory.getLastUpdated().isPresent(), "Expected a lastUpdated field");
        long end = System.currentTimeMillis();
        // finding the time difference and converting it into seconds
        long secs = (end - start) / 1000L;
        beneHistory
            .getLastUpdated()
            .ifPresent(
                lastUpdated -> {
                  assertFalse(
                      lastUpdated.isAfter(Instant.now().minusSeconds(secs)),
                      "Expected not a recent lastUpdated timestamp");
                });
      }
      // Make sure the size is the same and no records have been inserted if the same fields in the
      // beneficiary history table are the same.
      assertEquals(4, beneficiaryHistoryEntries.size());

    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /*
   * This test checks that all enrollment data for the year has been loaded into the beneficiary
   * monthly table and checks each month to make sure the correct values are there.
   */
  @Test
  public void loadInitialEnrollmentShouldCount12() {
    // Loads sample A Data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks all 12 months are in beneficiary monthlys for that beneficiary
      assertEquals(12, beneficiaryFromDb.getBeneficiaryMonthlys().size());
      // Checks every month in the beneficiary monthly table
      assertBeneficiaryMonthly(beneficiaryFromDb);

    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /*
   * This test checks that all enrollment data for 2 years has been loaded into the beneficiary
   * monthly table.
   */
  @Test
  public void loadInitialEnrollmentShouldCount24() {
    // Loads first year of data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Loads second year of data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks to make sure we have 2 years or 24 months of data
      assertEquals(24, beneficiaryFromDb.getBeneficiaryMonthlys().size());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /*
   * This test checks that all enrollment data for 2 years has been loaded into the beneficiary
   * monthly table and than does an update of 8 years without the 4 other months for the year
   */
  @Test
  public void loadInitialEnrollmentShouldCount20SinceThereIsAUpdateOf8Months() {
    // Loads first year of data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Loads second year of data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    // Loads  second year of data with only 8 months
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS.getResources()));

    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks to make sure we only have 20 months of data
      assertEquals(20, beneficiaryFromDb.getBeneficiaryMonthlys().size());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /*
   * This test checks that all enrollment data for month july in its 2 year is updated when there is data
   * for august that comes in.
   */
  @Test
  public void loadInitialEnrollmentShouldCount21SinceThereIsAUpdateOf8MonthsAndAUpdateOf9Months() {
    // Load first year of data
    loadSample(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Load 8 months of data in year two
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS.getResources()));

    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      assertEquals(20, beneficiaryFromDb.getBeneficiaryMonthlys().size());

      BeneficiaryMonthly augustMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(19);
      assertEquals("2019-08-01", augustMonthly.getYearMonth().toString());
      assertEquals("C", augustMonthly.getEntitlementBuyInInd().get().toString());
      assertEquals("AA", augustMonthly.getFipsStateCntyCode().get());
      assertFalse(augustMonthly.getHmoIndicatorInd().isPresent());
      assertEquals("AA", augustMonthly.getMedicaidDualEligibilityCode().get());
      assertEquals("AA", augustMonthly.getMedicareStatusCode().get());
      assertEquals("C", augustMonthly.getPartCContractNumberId().get());
      assertEquals("C", augustMonthly.getPartCPbpNumberId().get());
      assertEquals("C", augustMonthly.getPartCPlanTypeCode().get());
      assertEquals("C", augustMonthly.getPartDContractNumberId().get());
      assertEquals("AA", augustMonthly.getPartDLowIncomeCostShareGroupCode().get());
      assertFalse(augustMonthly.getPartDPbpNumberId().isPresent());
      assertEquals("C", augustMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      assertFalse(augustMonthly.getPartDSegmentNumberId().isPresent());

    } finally {
      if (entityManager != null) entityManager.close();
    }
    // Load 9 months of data in year two with some data updated in july
    loadSample(
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_9_MONTHS.getResources()));

    entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      assertEquals(21, beneficiaryFromDb.getBeneficiaryMonthlys().size());
      BeneficiaryMonthly augustMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(19);
      assertEquals("2019-08-01", augustMonthly.getYearMonth().toString());
      assertEquals("C", augustMonthly.getEntitlementBuyInInd().get().toString());
      assertEquals("AA", augustMonthly.getFipsStateCntyCode().get());
      // Updated in file
      assertEquals("C", augustMonthly.getHmoIndicatorInd().get().toString());
      assertEquals("AA", augustMonthly.getMedicaidDualEligibilityCode().get());
      assertEquals("AA", augustMonthly.getMedicareStatusCode().get());
      assertEquals("C", augustMonthly.getPartCContractNumberId().get());
      assertEquals("C", augustMonthly.getPartCPbpNumberId().get());
      assertEquals("C", augustMonthly.getPartCPlanTypeCode().get());
      assertEquals("C", augustMonthly.getPartDContractNumberId().get());
      assertEquals("AA", augustMonthly.getPartDLowIncomeCostShareGroupCode().get());
      assertFalse(augustMonthly.getPartDPbpNumberId().isPresent());
      assertEquals("C", augustMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      assertFalse(augustMonthly.getPartDSegmentNumberId().isPresent());

      BeneficiaryMonthly septMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(20);
      assertEquals("2019-09-01", septMonthly.getYearMonth().toString());
      assertFalse(septMonthly.getEntitlementBuyInInd().isPresent());
      assertFalse(septMonthly.getFipsStateCntyCode().isPresent());
      assertFalse(septMonthly.getHmoIndicatorInd().isPresent());
      assertEquals("AA", septMonthly.getMedicaidDualEligibilityCode().get());
      assertFalse(septMonthly.getMedicareStatusCode().isPresent());
      assertFalse(septMonthly.getPartCContractNumberId().isPresent());
      assertFalse(septMonthly.getPartCPbpNumberId().isPresent());
      assertFalse(septMonthly.getPartCPlanTypeCode().isPresent());
      assertFalse(septMonthly.getPartDContractNumberId().isPresent());
      assertFalse(septMonthly.getPartDLowIncomeCostShareGroupCode().isPresent());
      assertFalse(septMonthly.getPartDPbpNumberId().isPresent());
      assertEquals("C", septMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      assertFalse(septMonthly.getPartDSegmentNumberId().isPresent());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SYNTHETIC_DATA} data.
   *
   * <p>This test only works with a PostgreSQL database instance. It 10s or minutes to run.
   */
  @Disabled
  @Test
  public void loadSyntheticData() {
    /*Assume.assumeTrue(
    String.format(
        "Not enough memory for this test (%s bytes max). Run with '-Xmx5g' or more.",
        Runtime.getRuntime().maxMemory()),
    Runtime.getRuntime().maxMemory() >= 4500000000L); */
    List<StaticRifResource> samples =
        Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources());
    loadSample(Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources()));
    verifyRecordPrimaryKeysPresent(samples);
  }

  @Test
  public void loadSyntheaData() {
    List<StaticRifResource> samples =
        Arrays.asList(StaticRifResourceGroup.SYNTHEA_DATA.getResources());
    loadSample(samples);
    verifyRecordPrimaryKeysPresent(samples);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when INSERT and
   * 2022 enrollment date and filter on expect the data is loaded to the regular database tables.
   */
  @Test
  public void loadBeneficiaryWhenInsertAnd2022EnrollmentDateAndFilterOnExpectRecordLoaded() {
    loadSampleABeneWithEnrollmentRefYear(
        "2022",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY));
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when INSERT and
   * non-2022 enrollment date and filter on expect the record is successfully loaded. A log message
   * will be printed in this case.
   */
  @Test
  public void loadBeneficiaryWhenInsertAndNon2022EnrollmentDateAndFilterOnExpectRecordLoaded() {

    loadSampleABeneWithEnrollmentRefYear(
        "2021",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY));
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when the
   * LoadStrategy.INSERT_IDEMPOTENT is used and 2022 enrollment date and filter on expect the data
   * is loaded to the regular database tables.
   */
  @Test
  public void
      loadBeneficiaryWhenInsertAnd2022EnrollmentDateAndFilterOnAndIdempotentInsertStrategyExpectRecordLoaded() {
    loadSampleABeneWithEnrollmentRefYear(
        "2022",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_IDEMPOTENT_STRATEGY));
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when the
   * LoadStrategy.INSERT_IDEMPOTENT is used with a non-2022 enrollment date and filter on expect the
   * data is loaded to the regular database tables. A log message will be printed.
   */
  @Test
  public void
      loadBeneficiaryWhenInsertAndNon2022EnrollmentDateAndFilterOnAndIdempotentInsertStrategyExpectRecordLoaded() {

    loadSampleABeneWithEnrollmentRefYear(
        "2021",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_IDEMPOTENT_STRATEGY));

    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when the
   * LoadStrategy.INSERT_IDEMPOTENT is used with a {@code null} enrollment date and filter on expect
   * the data is loaded to the regular database tables. A log message will be printed.
   */
  @Test
  public void
      loadBeneficiaryWhenInsertAndNullEnrollmentDateAndFilterOnAndIdempotentInsertStrategyExpectRecordLoaded() {

    loadSampleABeneWithEnrollmentRefYear(
        null,
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_IDEMPOTENT_STRATEGY),
        false);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when UPDATE and
   * 2022 enrollment date and filter on expect the data is loaded to the regular database tables.
   */
  @Test
  public void loadBeneficiaryWhenUpdateAnd2022EnrollmentDateAndFilterOnExpectRecordLoaded() {

    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    loadSampleABeneWithEnrollmentRefYear(
        "2022",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
        true);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Verifies that {@link RifLoader} skips {@link Beneficiary} records, as expected, for <code>
   * UPDATE</code>s of a non-2022 {@link Beneficiary}, when {@link
   * LoadAppOptions#isFilteringNonNullAndNon2022Benes()} is enabled.
   */
  @Test
  public void loadBeneficiaryWhenUpdateAndNon2022EnrollmentDateAndFilterOnExpectRecordSkipped() {

    /* First, load a bene that SHOULD be filtered out (when filtering is turned on) normally. */
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    /* Re-load that bene again as an UPDATE with filtering turned on, and verify that it was skipped. */
    loadDefaultSampleABeneData(
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
        true);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 1);
  }

  /**
   * Verifies that {@link RifLoader} loads {@link Beneficiary} records, as expected, for <code>
   * UPDATE</code>s of a {@code null} {@link Beneficiary} enrollment year, when {@link
   * LoadAppOptions#isFilteringNonNullAndNon2022Benes()} is enabled.
   */
  @Test
  public void loadBeneficiaryWhenUpdateAndNullEnrollmentDateAndFilterOnExpectRecordLoaded() {

    /* First, load a bene normally. */
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    /* Re-load that bene again as an UPDATE with filtering turned on, with a null ref year, and verify that it was loaded. */
    loadSampleABeneWithEnrollmentRefYear(
        null,
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
        true);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Verifies that {@link RifLoader} loads {@link Beneficiary} records, as expected, for <code>
   * UPDATE</code>s of a 2022 {@link Beneficiary}, when {@link
   * LoadAppOptions#isFilteringNonNullAndNon2022Benes()} is disabled.
   *
   * <p>If the filter is off, we take no special action to filter records.
   */
  @Test
  public void loadBeneficiaryWhenUpdateAnd2022EnrollmentDateAndFilterOffExpectRecordLoaded() {
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    loadSampleABeneWithEnrollmentRefYear("2022", CcwRifLoadTestUtils.getLoadOptions(), true);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when INSERT,
   * filter is on, and the LoadStrategy.INSERT_IDEMPOTENT is used with a non-Beneficiary type,
   * expect the data is loaded normally.
   */
  @Test
  public void
      loadNonBeneficiaryWhenInsertAndFilterOnAndIdempotentInsertStrategyExpectRecordLoaded() {

    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    Stream<RifFile> stream =
        filterSamples(
            r -> r.getFileType() == RifFileType.INPATIENT,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    loadSample(
        "non-Bene sample",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_IDEMPOTENT_STRATEGY),
        stream);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when INSERT,
   * filter is on, and a non-Beneficiary type, expect the data is loaded normally.
   */
  @Test
  public void loadNonBeneficiaryWhenInsertAndFilterOnExpectRecordLoaded() {

    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    Stream<RifFile> stream =
        filterSamples(
            r -> r.getFileType() == RifFileType.INPATIENT,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    loadSample(
        "non-Bene sample",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
        stream);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when INSERT,
   * filter is off, and the LoadStrategy.INSERT_IDEMPOTENT is used with a non-Beneficiary type,
   * expect the data is loaded normally.
   */
  @Test
  public void
      loadNonBeneficiaryWhenInsertAndFilterOffAndIdempotentInsertStrategyExpectRecordLoaded() {

    boolean FILTER_MODE_DISABLED = false;
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    Stream<RifFile> stream =
        filterSamples(
            r -> r.getFileType() == RifFileType.INPATIENT,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    loadSample(
        "non-Bene sample",
        CcwRifLoadTestUtils.getLoadOptions(USE_INSERT_IDEMPOTENT_STRATEGY, FILTER_MODE_DISABLED),
        stream);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data when UPDATE,
   * filter setting is on, and a non-Beneficiary type expect the data is loaded normally.
   */
  @Test
  public void loadNonBeneficiaryWhenUpdateAndFilterOnExpectRecordLoaded() {
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());
    loadSample(
        "non-Bene sample",
        CcwRifLoadTestUtils.getLoadOptions(),
        getStreamForFileType(RifFileType.INPATIENT));
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

    // Load again to test UPDATE
    Stream<RifFile> updateStream =
        editStreamToBeUpdate(getStreamForFileType(RifFileType.INPATIENT));
    loadSample(
        "non-Bene sample update",
        CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
            USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
        updateStream);
    validateBeneficiaryAndSkippedCountsInDatabase(1, 0);
  }

  /**
   * Runs {@link RifLoader} against the modified {@link StaticRifResourceGroup#SAMPLE_A} data for an
   * <code>UPDATE</code> on a {@link Beneficiary} record that has a single file with multiple update
   * records. The Beneficiary object will have been initialized by the pipeline causing the
   * bene_id_numeric (native long) to be initialized to 0 (zero). However, the beneficiaries table
   * has a pre-insert / pre-update trigger that populates the bene_id_numeric at the time the data
   * is persisted to the table. This check verifies that both the insert and the update trigger has
   * fired and successully populated the bene_id_numeric column.
   *
   * <p>NOTE: This test will be removed when the new schema migration has been completed; at that
   * time the bene_id_numeric will no longer be needed and the trigger will be removed.
   */
  @Test
  public void verifyBeneficiaryEntityHasPopulatedBeneIdNumeric() {
    // persist a record into Beneficiary table
    loadDefaultSampleABeneData(CcwRifLoadTestUtils.getLoadOptions());

    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;

    try {
      entityManager = entityManagerFactory.createEntityManager();
      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");

      // verify that the pre-insert trigger populated the bene_id_numeric
      assertEquals(
          567834L,
          beneficiaryFromDb.getBeneficiaryIdNumeric(),
          "Beneficiary has incorrect bene_id_numeric after INSERT");

      /* Re-load that bene again as an UPDATE with filtering turned on, with a null ref year, and verify that it was loaded. */
      loadSampleABeneWithEnrollmentRefYear(
          null,
          CcwRifLoadTestUtils.getLoadOptionsWithFilteringofNon2022BenesEnabled(
              USE_INSERT_UPDATE_NON_IDEMPOTENT_STRATEGY),
          true);
      validateBeneficiaryAndSkippedCountsInDatabase(1, 0);

      // verify that the pre-update trigger populated the bene_id_numeric
      Beneficiary beneficiary2FromDb = entityManager.find(Beneficiary.class, "567834");
      assertEquals(
          beneficiaryFromDb.getBeneficiaryIdNumeric(),
          beneficiary2FromDb.getBeneficiaryIdNumeric(),
          "Beneficiary has incorrect bene_id_numeric after UPDATE");

      // brute force trying to break the bene_id_numeric by explicitly setting to zero; this
      // actually mimics a RIF load UPDATE as the RIF columns-to-entity setters will not know about
      // bene_id_numeric.
      beneficiary2FromDb.setBeneficiaryIdNumeric(0L);
      entityManager.persist(beneficiary2FromDb);
      // secret sauce for verifying that our Hibernate object actually was
      // refreshed with what is in the database.
      entityManager.refresh(beneficiary2FromDb);

      // verify that the update trigger still did its thing
      assertEquals(
          567834L,
          beneficiary2FromDb.getBeneficiaryIdNumeric(),
          "Beneficiary has incorrect bene_id_numeric afer explicit UPDATE");

    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  /**
   * Gets the stream for the specified file type from the SAMPLE_A data.
   *
   * @param fileType the file type to get from the SAMPLE_A data
   * @return the stream for file type
   */
  private Stream<RifFile> getStreamForFileType(RifFileType fileType) {
    return filterSamples(
        r -> r.getFileType() == fileType, StaticRifResourceGroup.SAMPLE_A.getResources());
  }

  /**
   * Loads the default SAMPLE_A bene data, useful for testing updates. Assumes this load is an
   * INSERT.
   *
   * @param loadAppOptions the load app options for controlling filtering setting
   */
  private void loadDefaultSampleABeneData(LoadAppOptions loadAppOptions) {
    loadDefaultSampleABeneData(loadAppOptions, false);
  }

  /**
   * Loads the default SAMPLE_A bene data, useful for testing updates.
   *
   * @param loadAppOptions the load app options for controlling filtering setting
   * @param isUpdate if the load should be an update
   */
  private void loadDefaultSampleABeneData(LoadAppOptions loadAppOptions, boolean isUpdate) {
    Stream<RifFile> sampleABene;
    if (isUpdate) {
      sampleABene = getSampleABeneAsUpdate();
    } else {
      sampleABene =
          filterSamples(
              r -> r.getFileType() == RifFileType.BENEFICIARY,
              StaticRifResourceGroup.SAMPLE_A.getResources());
    }
    loadSample("SAMPLE_A, bene only, default ref year", loadAppOptions, sampleABene);
  }

  /**
   * Gathers the SAMPLE_A data, modifies the enrollment reference year to the input date, and
   * returns the stream of data for further use. Assumes this load is an INSERT.
   *
   * @param refYear the ref year
   * @param loadAppOptions the load app options, for controlling filtering setting
   */
  private void loadSampleABeneWithEnrollmentRefYear(String refYear, LoadAppOptions loadAppOptions) {
    loadSampleABeneWithEnrollmentRefYear(refYear, loadAppOptions, false);
  }

  /**
   * Gathers the SAMPLE_A data, modifies the enrollment reference year to the input date, and
   * returns the stream of data for further use.
   *
   * @param refYear the ref year
   * @param loadAppOptions the load app options, for controlling filtering setting
   * @param isUpdate if this load should be an update
   */
  private void loadSampleABeneWithEnrollmentRefYear(
      String refYear, LoadAppOptions loadAppOptions, boolean isUpdate) {
    Stream<RifFile> samplesStream =
        filterSamples(
            r -> r.getFileType() == RifFileType.BENEFICIARY,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    Function<RifRecordEvent<?>, List<List<String>>> recordEditor =
        rifRecordEvent -> {
          CSVRecord beneCsvRow = rifRecordEvent.getRawCsvRecords().get(0);
          List<String> beneCsvValues =
              StreamSupport.stream(beneCsvRow.spliterator(), false).collect(Collectors.toList());
          beneCsvValues.set(BeneficiaryColumn.RFRNC_YR.ordinal() + 1, refYear);
          if (isUpdate) {
            beneCsvValues.set(0, "UPDATE");
          }
          return List.of(beneCsvValues);
        };
    Function<RifFile, RifFile> fileEditor = sample -> editSampleRecords(sample, recordEditor);
    Stream<RifFile> updatedSampleAStream = editSamples(samplesStream, fileEditor);

    loadSample("SAMPLE_A, updates to 2022 ref year", loadAppOptions, updatedSampleAStream);
  }

  /**
   * Gathers the SAMPLE_A data, modifies the enrollment reference year to the input date, and
   * returns the stream of data for further use.
   *
   * @return the rif file stream with the modified data
   */
  private Stream<RifFile> getSampleABeneAsUpdate() {
    Stream<RifFile> samplesStream =
        filterSamples(
            r -> r.getFileType() == RifFileType.BENEFICIARY,
            StaticRifResourceGroup.SAMPLE_A.getResources());
    return editStreamToBeUpdate(samplesStream);
  }

  /**
   * Edit the given stream to be an UPDATE by editing the csv data.
   *
   * @param samplesStream the samples stream
   * @return the edited stream
   */
  private Stream<RifFile> editStreamToBeUpdate(Stream<RifFile> samplesStream) {
    Function<RifRecordEvent<?>, List<List<String>>> recordEditor =
        rifRecordEvent -> {
          CSVRecord beneCsvRow = rifRecordEvent.getRawCsvRecords().get(0);
          List<String> beneCsvValues =
              StreamSupport.stream(beneCsvRow.spliterator(), false).collect(Collectors.toList());
          beneCsvValues.set(0, "UPDATE");
          return List.of(beneCsvValues);
        };
    Function<RifFile, RifFile> fileEditor = sample -> editSampleRecords(sample, recordEditor);
    return editSamples(samplesStream, fileEditor);
  }

  /**
   * Validates the database load went as expected for the normal data table and the skipped table.
   *
   * @param expectedBeneCount the expected records in normal table
   * @param expectedSkippedCount the expected records in skipped table
   */
  private void validateBeneficiaryAndSkippedCountsInDatabase(
      int expectedBeneCount, int expectedSkippedCount) {
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

      // Count and verify the number of bene records in the DB.
      CriteriaQuery<Long> beneCountQuery = criteriaBuilder.createQuery(Long.class);
      beneCountQuery.select(criteriaBuilder.count(beneCountQuery.from(Beneficiary.class)));
      Long beneCount = entityManager.createQuery(beneCountQuery).getSingleResult();
      assertEquals(expectedBeneCount, beneCount, "Unexpected number of beneficiary records.");

      // Count and verify the number of bene records in the DB.
      CriteriaQuery<Long> skippedCountQuery = criteriaBuilder.createQuery(Long.class);
      skippedCountQuery.select(
          criteriaBuilder.count(skippedCountQuery.from(SkippedRifRecord.class)));
      Long skippedCount = entityManager.createQuery(skippedCountQuery).getSingleResult();
      assertEquals(expectedSkippedCount, skippedCount, "Unexpected number of skipped records.");
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  /**
   * @param filter a {@link Predicate} that should return <code>true</code> for only those {@link
   *     StaticRifResource}s that should be included in the result
   * @param samples the {@link StaticRifResource}s to be filtered
   * @return a {@link Stream} of the {@link RifFile}s from the specified {@link StaticRifResource}s
   *     that matched the specified filter
   */
  private Stream<RifFile> filterSamples(Predicate<RifFile> filter, StaticRifResource... samples) {
    return Arrays.stream(samples).map(sample -> sample.toRifFile()).filter(filter);
  }

  /**
   * @param samples the {@link Stream} of {@link RifFile}s to return an edited copy of (note that
   *     this input {@link Stream} will be consumed)
   * @param editor a {@link Function} that, given an input {@link RifFile}, produces an
   *     edited/output {@link RifFile}
   * @return a new {@link Stream} of {@link RifFile}s, as edited by the specified {@link Function}
   */
  private Stream<RifFile> editSamples(Stream<RifFile> samples, Function<RifFile, RifFile> editor) {
    Stream<RifFile> editedRifFiles = samples.map(sampleFile -> editor.apply(sampleFile));
    return editedRifFiles;
  }

  /**
   * @param inputFile the {@link RifFile} to return an edited copy of
   * @param editor a {@link Function} that, given an input {@link RifRecordEvent}, produces an
   *     edited/output copy of it, represented as a nested {@link List} of {@link List}s of {@link
   *     String}s (where each {@link String} is a CSV cell, each {@link List} of {@link String}s is
   *     a CSV row), and the outer {@link List} represents the CSV rows that comprised the {@link
   *     RifRecordEvent})
   * @return a new {@link RifFile}, with its records edited by the specified {@link Function}
   */
  private RifFile editSampleRecords(
      RifFile inputFile, Function<RifRecordEvent<?>, List<List<String>>> editor) {
    try {
      Path editedTempFile = Files.createTempFile("edited-sample-rif", ".rif");
      RifFilesProcessor rifProcessor = new RifFilesProcessor();
      RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), inputFile);
      RifFileEvent rifFileEvent = rifFilesEvent.getFileEvents().get(0);
      RifFileRecords records = rifProcessor.produceRecords(rifFileEvent);

      /*
       * Each List<String> represents a single CSV row's cell values. Each List<List<String>>
       * represents a single RIF "record group", because claims are often composed of multiple CSV
       * rows. Thus, each List<List<List<String>>> is a collection of multiple RIF record groups,
       * e.g. multiple claims or beneficiaries.
       */
      List<List<List<String>>> editedRifRecords =
          records.getRecords().map(editor).collect(Collectors.toList());

      // Build a CSVFormat with the specific header needed for the RIF file type.
      String[] csvHeader =
          Stream.concat(
                  Stream.of("DML_IND"),
                  Arrays.stream(rifFileEvent.getFile().getFileType().getColumns())
                      .map(e -> e.name()))
              .toArray(String[]::new);
      CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader(csvHeader);

      /*
       * Write the RIF records back out to a new RIF temp file. Worth noting that, because we aren't
       * RAII'ing this, we have no way to reliably clean up the temp files that this creates. They
       * _shouldn't_ be large enough for that to be a major problem, but it's still not great.
       */
      try (FileWriter fileWriter = new FileWriter(editedTempFile.toFile());
          CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat); ) {
        LOGGER.debug("Writing out temp RIF/CSV file: '{}'", editedTempFile);

        // Then, print out each row of RIF/CSV.
        for (List<List<String>> rifRecordGroup : editedRifRecords) {
          for (List<String> csvRow : rifRecordGroup) {
            LOGGER.debug("Printing RIF/CSV row: '{}'", csvRow);
            csvPrinter.printRecord(csvRow);
          }
        }
      }

      LocalRifFile editedRifFile = new LocalRifFile(editedTempFile, inputFile.getFileType());
      return editedRifFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Runs {@link RifLoader} against the specified {@link RifFile}s.
   *
   * @param sampleName a human-friendly name that will be logged to identify the data load being
   *     kicked off here
   * @param options the {@link LoadAppOptions} to use
   * @param filesToLoad the {@link RifFile}s to load
   */
  private void loadSample(String sampleName, LoadAppOptions options, Stream<RifFile> filesToLoad) {
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(Instant.now(), filesToLoad.collect(Collectors.toList()));
    loadSample(sampleName, options, rifFilesEvent);
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param sampleResources the {@link StaticRifResourceGroup} to load
   * @param loadAppOptions the load app options
   */
  private void loadSample(List<StaticRifResource> sampleResources, LoadAppOptions loadAppOptions) {
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));
    int loadCount =
        loadSample(
            sampleResources.get(0).getResourceUrl().toString(), loadAppOptions, rifFilesEvent);

    // Verify that the expected number of records were run successfully.
    assertEquals(
        sampleResources.stream().mapToInt(r -> r.getRecordCount()).sum(),
        loadCount,
        "Unexpected number of loaded records.");
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param sampleResources the {@link StaticRifResourceGroup} to load
   */
  private void loadSample(List<StaticRifResource> sampleResources) {
    loadSample(sampleResources, CcwRifLoadTestUtils.getLoadOptions());
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param sampleName a human-friendly name that will be logged to identify the data load being
   *     kicked off here
   * @param options the {@link LoadAppOptions} to use
   * @param rifFilesEvent the {@link RifFilesEvent} to load
   * @return the number of RIF records that were loaded (as reported by the {@link RifLoader})
   */
  private int loadSample(String sampleName, LoadAppOptions options, RifFilesEvent rifFilesEvent) {
    LOGGER.info("Loading RIF files: '{}'...", sampleName);

    // Create the processors that will handle each stage of the pipeline.
    RifFilesProcessor processor = new RifFilesProcessor();
    RifLoader loader =
        new RifLoader(options, PipelineTestUtils.get().getPipelineApplicationState());

    // Link up the pipeline and run it.
    LOGGER.info("Loading RIF records...");
    AtomicInteger failureCount = new AtomicInteger(0);
    AtomicInteger loadCount = new AtomicInteger(0);
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
      loader.process(
          rifFileRecords,
          error -> {
            failureCount.incrementAndGet();
            LOGGER.warn("Record(s) failed to load.", error);
          },
          result -> {
            loadCount.incrementAndGet();
          });
      Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build().report();
    }
    LOGGER.info("Loaded RIF files: '{}', record count: '{}'.", sampleName, loadCount.get());
    Slf4jReporter.forRegistry(PipelineTestUtils.get().getPipelineApplicationState().getMetrics())
        .outputTo(LOGGER)
        .build()
        .report();

    // Verify that the expected number of records were run successfully.
    assertEquals(0, failureCount.get(), "Load errors encountered.");

    return loadCount.get();
  }

  /**
   * Runs the {@link RifFilesProcessor} to extract RIF records from the specified {@link
   * StaticRifResource}s, and then calls {@link #assertAreInDatabase(LoadAppOptions,
   * EntityManagerFactory, Stream)} on each record to verify that it's present in the database.
   * Basically: this is a decent smoke test to verify that {@link RifLoader} did what it should have
   * -- not thorough, but something.
   *
   * @param sampleResources the {@link StaticRifResource}s to go check for in the DB
   */
  private void verifyRecordPrimaryKeysPresent(List<StaticRifResource> sampleResources) {
    EntityManagerFactory entityManagerFactory =
        PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
    RifFilesProcessor processor = new RifFilesProcessor();
    LoadAppOptions options = CcwRifLoadTestUtils.getLoadOptions();

    /*
     * Run the extraction an extra time and verify that each record can now
     * be found in the database.
     */
    for (StaticRifResource rifResource : sampleResources) {
      /*
       * This is too slow to run against larger data sets: for instance,
       * it took 45 minutes to run against the synthetic data. So, we skip
       * it for some things.
       */
      if (rifResource.getRecordCount() > 10000) {
        LOGGER.info("Skipping DB records check for: {}", rifResource);
        continue;
      }

      LOGGER.info("Checking DB for records for: {}", rifResource);
      RifFilesEvent rifFilesEventSingle = new RifFilesEvent(Instant.now(), rifResource.toRifFile());
      RifFileRecords rifFileRecordsCopy =
          processor.produceRecords(rifFilesEventSingle.getFileEvents().get(0));
      assertAreInDatabase(
          options, entityManagerFactory, rifFileRecordsCopy.getRecords().map(r -> r.getRecord()));
    }
    LOGGER.info("All records found in DB.");
  }

  /**
   * Load the batches associated with a particular file
   *
   * @param entityManager to use
   * @param loadedFileId to use
   * @return array of ids
   */
  private List<LoadedBatch> loadBatches(EntityManager entityManager, long loadedFileId) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<LoadedBatch> fetch = cb.createQuery(LoadedBatch.class);
    Root<LoadedBatch> b = fetch.from(LoadedBatch.class);
    fetch.where(cb.equal(b.get("loadedFileId"), loadedFileId));
    return entityManager.createQuery(fetch).getResultList();
  }

  /**
   * Verifies that the specified RIF records are actually in the database.
   *
   * @param options the {@link LoadAppOptions} to use
   * @param entityManagerFactory the {@link EntityManagerFactory} to use
   * @param records the RIF records to verify
   */
  private static void assertAreInDatabase(
      LoadAppOptions options, EntityManagerFactory entityManagerFactory, Stream<Object> records) {
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      for (Object record : records.collect(Collectors.toList())) {
        /*
         * We need to handle BeneficiaryHistory separately, as it has a generated ID.
         */
        if (record instanceof BeneficiaryHistory) {
          BeneficiaryHistory beneficiaryHistoryToFind = (BeneficiaryHistory) record;
          beneficiaryHistoryToFind.setHicn(
              RifLoader.computeHicnHash(
                  new IdHasher(options.getIdHasherConfig()), beneficiaryHistoryToFind.getHicn()));
          beneficiaryHistoryToFind.setMbiHash(
              beneficiaryHistoryToFind.getMedicareBeneficiaryId().isPresent()
                  ? Optional.of(
                      RifLoader.computeMbiHash(
                          new IdHasher(options.getIdHasherConfig()),
                          beneficiaryHistoryToFind.getMedicareBeneficiaryId().get()))
                  : Optional.empty());

          CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
          CriteriaQuery<BeneficiaryHistory> query =
              criteriaBuilder.createQuery(BeneficiaryHistory.class);
          Root<BeneficiaryHistory> from = query.from(BeneficiaryHistory.class);
          query
              .select(from)
              .where(
                  criteriaBuilder.equal(
                      from.get(BeneficiaryHistory_.beneficiaryId),
                      String.valueOf(beneficiaryHistoryToFind.getBeneficiaryId())),
                  criteriaBuilder.equal(
                      from.get(BeneficiaryHistory_.birthDate),
                      beneficiaryHistoryToFind.getBirthDate()),
                  criteriaBuilder.equal(
                      from.get(BeneficiaryHistory_.sex), beneficiaryHistoryToFind.getSex()),
                  criteriaBuilder.equal(
                      from.get(BeneficiaryHistory_.hicn), beneficiaryHistoryToFind.getHicn()),
                  criteriaBuilder.equal(
                      from.get(BeneficiaryHistory_.mbiHash),
                      beneficiaryHistoryToFind.getMbiHash().orElse(null)));

          List<BeneficiaryHistory> beneficiaryHistoryFound =
              entityManager.createQuery(query).getResultList();
          assertNotNull(beneficiaryHistoryFound);
          assertFalse(beneficiaryHistoryFound.isEmpty());
        } else {
          Object recordId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(record);
          Object recordFromDb = entityManager.find(record.getClass(), recordId);
          assertNotNull(recordFromDb);
        }
      }
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Assert the beneficiary monthly table has entries for every month for the provided beneficiary
   * for that beneficiaries' last recorded enrollment year.
   *
   * @param beneficiaryFromDb the beneficiary from db
   */
  public static void assertBeneficiaryMonthly(Beneficiary beneficiaryFromDb) {
    if (beneficiaryFromDb.getBeneEnrollmentReferenceYear().isEmpty()) {
      fail("Bene in db had no enrollment reference year to check for in bene monthly table.");
    }

    assertBeneficiaryMonthly(
        beneficiaryFromDb, beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue());
  }

  /**
   * Assert the beneficiary monthly table has entries for every month for the provided beneficiary,
   * for the provided enrollment year.
   *
   * @param beneficiaryFromDb the beneficiary from db
   * @param yearToCheckFor the year to check for
   */
  public static void assertBeneficiaryMonthly(Beneficiary beneficiaryFromDb, int yearToCheckFor) {
    List<BeneficiaryMonthly> beneficiaryMonthlyList = beneficiaryFromDb.getBeneficiaryMonthlys();
    List<BeneficiaryMonthly> beneficiaryMonthliesForYear =
        beneficiaryMonthlyList.stream()
            .filter(bene -> bene.getYearMonth().getYear() == yearToCheckFor)
            .collect(Collectors.toList());

    if (beneficiaryMonthliesForYear.isEmpty()) {
      fail("Expected year " + yearToCheckFor + " not found in bene monthly table");
    }

    checkEnrollments(
        yearToCheckFor,
        1,
        beneficiaryMonthliesForYear.get(0),
        beneficiaryFromDb.getEntitlementBuyInJanInd(),
        beneficiaryFromDb.getFipsStateCntyJanCode(),
        beneficiaryFromDb.getHmoIndicatorJanInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityJanCode(),
        beneficiaryFromDb.getMedicareStatusJanCode(),
        beneficiaryFromDb.getPartCContractNumberJanId(),
        beneficiaryFromDb.getPartCPbpNumberJanId(),
        beneficiaryFromDb.getPartCPlanTypeJanCode(),
        beneficiaryFromDb.getPartDContractNumberJanId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJanCode(),
        beneficiaryFromDb.getPartDPbpNumberJanId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJanInd(),
        beneficiaryFromDb.getPartDSegmentNumberJanId());

    checkEnrollments(
        yearToCheckFor,
        2,
        beneficiaryMonthliesForYear.get(1),
        beneficiaryFromDb.getEntitlementBuyInFebInd(),
        beneficiaryFromDb.getFipsStateCntyFebCode(),
        beneficiaryFromDb.getHmoIndicatorFebInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityFebCode(),
        beneficiaryFromDb.getMedicareStatusFebCode(),
        beneficiaryFromDb.getPartCContractNumberFebId(),
        beneficiaryFromDb.getPartCPbpNumberFebId(),
        beneficiaryFromDb.getPartCPlanTypeFebCode(),
        beneficiaryFromDb.getPartDContractNumberFebId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupFebCode(),
        beneficiaryFromDb.getPartDPbpNumberFebId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyFebInd(),
        beneficiaryFromDb.getPartDSegmentNumberFebId());

    checkEnrollments(
        yearToCheckFor,
        3,
        beneficiaryMonthliesForYear.get(2),
        beneficiaryFromDb.getEntitlementBuyInMarInd(),
        beneficiaryFromDb.getFipsStateCntyMarCode(),
        beneficiaryFromDb.getHmoIndicatorMarInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityMarCode(),
        beneficiaryFromDb.getMedicareStatusMarCode(),
        beneficiaryFromDb.getPartCContractNumberMarId(),
        beneficiaryFromDb.getPartCPbpNumberMarId(),
        beneficiaryFromDb.getPartCPlanTypeMarCode(),
        beneficiaryFromDb.getPartDContractNumberMarId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupMarCode(),
        beneficiaryFromDb.getPartDPbpNumberMarId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyMarInd(),
        beneficiaryFromDb.getPartDSegmentNumberMarId());

    checkEnrollments(
        yearToCheckFor,
        4,
        beneficiaryMonthliesForYear.get(3),
        beneficiaryFromDb.getEntitlementBuyInAprInd(),
        beneficiaryFromDb.getFipsStateCntyAprCode(),
        beneficiaryFromDb.getHmoIndicatorAprInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityAprCode(),
        beneficiaryFromDb.getMedicareStatusAprCode(),
        beneficiaryFromDb.getPartCContractNumberAprId(),
        beneficiaryFromDb.getPartCPbpNumberAprId(),
        beneficiaryFromDb.getPartCPlanTypeAprCode(),
        beneficiaryFromDb.getPartDContractNumberAprId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupAprCode(),
        beneficiaryFromDb.getPartDPbpNumberAprId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyAprInd(),
        beneficiaryFromDb.getPartDSegmentNumberAprId());

    checkEnrollments(
        yearToCheckFor,
        5,
        beneficiaryMonthliesForYear.get(4),
        beneficiaryFromDb.getEntitlementBuyInMayInd(),
        beneficiaryFromDb.getFipsStateCntyMayCode(),
        beneficiaryFromDb.getHmoIndicatorMayInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityMayCode(),
        beneficiaryFromDb.getMedicareStatusMayCode(),
        beneficiaryFromDb.getPartCContractNumberMayId(),
        beneficiaryFromDb.getPartCPbpNumberMayId(),
        beneficiaryFromDb.getPartCPlanTypeMayCode(),
        beneficiaryFromDb.getPartDContractNumberMayId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupMayCode(),
        beneficiaryFromDb.getPartDPbpNumberMayId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyMayInd(),
        beneficiaryFromDb.getPartDSegmentNumberMayId());

    checkEnrollments(
        yearToCheckFor,
        6,
        beneficiaryMonthliesForYear.get(5),
        beneficiaryFromDb.getEntitlementBuyInJunInd(),
        beneficiaryFromDb.getFipsStateCntyJunCode(),
        beneficiaryFromDb.getHmoIndicatorJunInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityJunCode(),
        beneficiaryFromDb.getMedicareStatusJunCode(),
        beneficiaryFromDb.getPartCContractNumberJunId(),
        beneficiaryFromDb.getPartCPbpNumberJunId(),
        beneficiaryFromDb.getPartCPlanTypeJunCode(),
        beneficiaryFromDb.getPartDContractNumberJunId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJunCode(),
        beneficiaryFromDb.getPartDPbpNumberJunId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJunInd(),
        beneficiaryFromDb.getPartDSegmentNumberJunId());

    checkEnrollments(
        yearToCheckFor,
        7,
        beneficiaryMonthliesForYear.get(6),
        beneficiaryFromDb.getEntitlementBuyInJulInd(),
        beneficiaryFromDb.getFipsStateCntyJulCode(),
        beneficiaryFromDb.getHmoIndicatorJulInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityJulCode(),
        beneficiaryFromDb.getMedicareStatusJulCode(),
        beneficiaryFromDb.getPartCContractNumberJulId(),
        beneficiaryFromDb.getPartCPbpNumberJulId(),
        beneficiaryFromDb.getPartCPlanTypeJulCode(),
        beneficiaryFromDb.getPartDContractNumberJulId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJulCode(),
        beneficiaryFromDb.getPartDPbpNumberJulId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJulInd(),
        beneficiaryFromDb.getPartDSegmentNumberJulId());

    checkEnrollments(
        yearToCheckFor,
        8,
        beneficiaryMonthliesForYear.get(7),
        beneficiaryFromDb.getEntitlementBuyInAugInd(),
        beneficiaryFromDb.getFipsStateCntyAugCode(),
        beneficiaryFromDb.getHmoIndicatorAugInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityAugCode(),
        beneficiaryFromDb.getMedicareStatusAugCode(),
        beneficiaryFromDb.getPartCContractNumberAugId(),
        beneficiaryFromDb.getPartCPbpNumberAugId(),
        beneficiaryFromDb.getPartCPlanTypeAugCode(),
        beneficiaryFromDb.getPartDContractNumberAugId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupAugCode(),
        beneficiaryFromDb.getPartDPbpNumberAugId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyAugInd(),
        beneficiaryFromDb.getPartDSegmentNumberAugId());

    checkEnrollments(
        yearToCheckFor,
        9,
        beneficiaryMonthliesForYear.get(8),
        beneficiaryFromDb.getEntitlementBuyInSeptInd(),
        beneficiaryFromDb.getFipsStateCntySeptCode(),
        beneficiaryFromDb.getHmoIndicatorSeptInd(),
        beneficiaryFromDb.getMedicaidDualEligibilitySeptCode(),
        beneficiaryFromDb.getMedicareStatusSeptCode(),
        beneficiaryFromDb.getPartCContractNumberSeptId(),
        beneficiaryFromDb.getPartCPbpNumberSeptId(),
        beneficiaryFromDb.getPartCPlanTypeSeptCode(),
        beneficiaryFromDb.getPartDContractNumberSeptId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupSeptCode(),
        beneficiaryFromDb.getPartDPbpNumberSeptId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidySeptInd(),
        beneficiaryFromDb.getPartDSegmentNumberSeptId());

    checkEnrollments(
        yearToCheckFor,
        10,
        beneficiaryMonthliesForYear.get(9),
        beneficiaryFromDb.getEntitlementBuyInOctInd(),
        beneficiaryFromDb.getFipsStateCntyOctCode(),
        beneficiaryFromDb.getHmoIndicatorOctInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityOctCode(),
        beneficiaryFromDb.getMedicareStatusOctCode(),
        beneficiaryFromDb.getPartCContractNumberOctId(),
        beneficiaryFromDb.getPartCPbpNumberOctId(),
        beneficiaryFromDb.getPartCPlanTypeOctCode(),
        beneficiaryFromDb.getPartDContractNumberOctId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupOctCode(),
        beneficiaryFromDb.getPartDPbpNumberOctId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyOctInd(),
        beneficiaryFromDb.getPartDSegmentNumberOctId());

    checkEnrollments(
        yearToCheckFor,
        11,
        beneficiaryMonthliesForYear.get(10),
        beneficiaryFromDb.getEntitlementBuyInNovInd(),
        beneficiaryFromDb.getFipsStateCntyNovCode(),
        beneficiaryFromDb.getHmoIndicatorNovInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityNovCode(),
        beneficiaryFromDb.getMedicareStatusNovCode(),
        beneficiaryFromDb.getPartCContractNumberNovId(),
        beneficiaryFromDb.getPartCPbpNumberNovId(),
        beneficiaryFromDb.getPartCPlanTypeNovCode(),
        beneficiaryFromDb.getPartDContractNumberNovId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupNovCode(),
        beneficiaryFromDb.getPartDPbpNumberNovId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyNovInd(),
        beneficiaryFromDb.getPartDSegmentNumberNovId());

    checkEnrollments(
        yearToCheckFor,
        12,
        beneficiaryMonthliesForYear.get(11),
        beneficiaryFromDb.getEntitlementBuyInDecInd(),
        beneficiaryFromDb.getFipsStateCntyDecCode(),
        beneficiaryFromDb.getHmoIndicatorDecInd(),
        beneficiaryFromDb.getMedicaidDualEligibilityDecCode(),
        beneficiaryFromDb.getMedicareStatusDecCode(),
        beneficiaryFromDb.getPartCContractNumberDecId(),
        beneficiaryFromDb.getPartCPbpNumberDecId(),
        beneficiaryFromDb.getPartCPlanTypeDecCode(),
        beneficiaryFromDb.getPartDContractNumberDecId(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupDecCode(),
        beneficiaryFromDb.getPartDPbpNumberDecId(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyDecInd(),
        beneficiaryFromDb.getPartDSegmentNumberDecId());
  }

  public static void checkEnrollments(
      int referenceYear,
      int month,
      BeneficiaryMonthly enrollment,
      Optional<Character> entitlementBuyInInd,
      Optional<String> fipsStateCntyCode,
      Optional<Character> hmoIndicatorInd,
      Optional<String> medicaidDualEligibilityCode,
      Optional<String> medicareStatusCode,
      Optional<String> partCContractNumberId,
      Optional<String> partCPbpNumberId,
      Optional<String> partCPlanTypeCode,
      Optional<String> partDContractNumberId,
      Optional<String> partDLowIncomeCostShareGroupCode,
      Optional<String> partDPbpNumberId,
      Optional<Character> partDRetireeDrugSubsidyInd,
      Optional<String> partDSegmentNumberId) {

    assertEquals(LocalDate.of(referenceYear, month, 1), enrollment.getYearMonth());
    assertEquals(
        entitlementBuyInInd.orElse(null), enrollment.getEntitlementBuyInInd().orElse(null));
    assertEquals(fipsStateCntyCode.orElse(null), enrollment.getFipsStateCntyCode().orElse(null));
    assertEquals(hmoIndicatorInd.orElse(null), enrollment.getHmoIndicatorInd().orElse(null));
    assertEquals(
        medicaidDualEligibilityCode.orElse(null),
        enrollment.getMedicaidDualEligibilityCode().orElse(null));
    assertEquals(medicareStatusCode.orElse(null), enrollment.getMedicareStatusCode().orElse(null));
    assertEquals(
        partCContractNumberId.orElse(null), enrollment.getPartCContractNumberId().orElse(null));
    assertEquals(partCPbpNumberId.orElse(null), enrollment.getPartCPbpNumberId().orElse(null));
    assertEquals(partCPlanTypeCode.orElse(null), enrollment.getPartCPlanTypeCode().orElse(null));
    assertEquals(
        partDContractNumberId.orElse(null), enrollment.getPartDContractNumberId().orElse(null));
    assertEquals(
        partDLowIncomeCostShareGroupCode.orElse(null),
        enrollment.getPartDLowIncomeCostShareGroupCode().orElse(null));
    assertEquals(partDPbpNumberId.orElse(null), enrollment.getPartDPbpNumberId().orElse(null));
    assertEquals(
        partDRetireeDrugSubsidyInd.orElse(null),
        enrollment.getPartDRetireeDrugSubsidyInd().orElse(null));
    assertEquals(
        partDSegmentNumberId.orElse(null), enrollment.getPartDSegmentNumberId().orElse(null));
  }
}
