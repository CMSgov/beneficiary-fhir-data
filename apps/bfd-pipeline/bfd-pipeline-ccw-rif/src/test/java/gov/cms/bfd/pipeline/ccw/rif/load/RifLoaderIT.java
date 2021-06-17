package gov.cms.bfd.pipeline.ccw.rif.load;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link RifLoader}. */
public final class RifLoaderIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIT.class);

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_A} data. */
  @Test
  public void loadSampleA() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
  }

  @Ignore
  @Test
  public void loadSampleAWithoutClean() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabase();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
  }

  @Test
  public void singleFileLoad() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          // Verify that LoadedFile entity
          loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
          final List<LoadedFile> loadedFiles = RifLoaderTestUtils.findLoadedFiles(entityManager);
          Assert.assertTrue(
              "Expected to have many loaded files in SAMPLE A", loadedFiles.size() > 1);
          final LoadedFile loadedFile = loadedFiles.get(0);
          Assert.assertNotNull(loadedFile.getCreated());

          // Verify that beneficiaries table was loaded
          final List<LoadedBatch> batches =
              loadBatches(entityManager, loadedFile.getLoadedFileId());
          final LoadedBatch allBatches = batches.stream().reduce(null, LoadedBatch::combine);
          Assert.assertTrue("Expected to have at least one beneficiary loaded", batches.size() > 0);
          Assert.assertEquals(
              "Expected to match the sample-a beneficiary",
              "567834",
              allBatches.getBeneficiariesAsList().get(0));
        });
  }

  @Test
  public void multipleFileLoads() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          // Verify that a loaded files exsits
          loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
          final List<LoadedFile> beforeLoadedFiles =
              RifLoaderTestUtils.findLoadedFiles(entityManager);
          Assert.assertTrue("Expected to have at least one file", beforeLoadedFiles.size() > 0);
          LoadedFile beforeLoadedFile = beforeLoadedFiles.get(0);
          LoadedFile beforeOldestFile = beforeLoadedFiles.get(beforeLoadedFiles.size() - 1);

          RifLoaderTestUtils.pauseMillis(10);
          loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

          // Verify that the loaded list was updated properly
          final List<LoadedFile> afterLoadedFiles =
              RifLoaderTestUtils.findLoadedFiles(entityManager);
          Assert.assertTrue(
              "Expected to have more loaded files",
              beforeLoadedFiles.size() < afterLoadedFiles.size());
          final LoadedFile afterLoadedFile = afterLoadedFiles.get(0);
          final LoadedFile afterOldestFile = afterLoadedFiles.get(afterLoadedFiles.size() - 1);
          Assert.assertEquals(
              "Expected same oldest file",
              beforeOldestFile.getLoadedFileId(),
              afterOldestFile.getLoadedFileId());
          Assert.assertTrue(
              "Expected range to expand",
              beforeLoadedFile.getCreated().before(afterLoadedFile.getCreated()));
        });
  }

  @Test
  public void trimLoadedFiles() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          // Setup a loaded file with an old date
          loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
          final List<LoadedFile> loadedFiles = RifLoaderTestUtils.findLoadedFiles(entityManager);
          final EntityTransaction txn = entityManager.getTransaction();
          txn.begin();
          LoadedFile oldFile = loadedFiles.get(loadedFiles.size() - 1);
          oldFile.setCreated(Date.from(Instant.now().minus(101, ChronoUnit.DAYS)));
          txn.commit();

          // Look at the files now
          final List<LoadedFile> beforeFiles = RifLoaderTestUtils.findLoadedFiles(entityManager);
          final Date oldDate = Date.from(Instant.now().minus(99, ChronoUnit.DAYS));
          Assert.assertTrue(
              "Expect to have old files",
              beforeFiles.stream().anyMatch(file -> file.getCreated().before(oldDate)));

          // Load another set that will cause the old file to be trimmed
          loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

          // Verify that old file was trimmed
          final List<LoadedFile> afterFiles = RifLoaderTestUtils.findLoadedFiles(entityManager);
          Assert.assertFalse(
              "Expect to not have old files",
              afterFiles.stream().anyMatch(file -> file.getCreated().before(oldDate)));
        });
  }

  @Ignore
  @Test
  public void buildSyntheticLoadedFiles() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          loadSample(
              dataSource, Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources()));
          // Verify that a loaded files exsits
          final List<LoadedFile> loadedFiles = RifLoaderTestUtils.findLoadedFiles(entityManager);
          Assert.assertTrue("Expected to have at least one file", loadedFiles.size() > 0);
          final LoadedFile file = loadedFiles.get(0);
          final List<LoadedBatch> batches = loadBatches(entityManager, file.getLoadedFileId());
          Assert.assertTrue(batches.size() > 0);
        });
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_U} data. */
  @Test
  public void loadSampleU() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
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
        Assert.assertEquals("567834", beneHistory.getBeneficiaryId());
        // A recent lastUpdated timestamp
        Assert.assertTrue("Expected a lastUpdated field", beneHistory.getLastUpdated().isPresent());
        beneHistory
            .getLastUpdated()
            .ifPresent(
                lastUpdated -> {
                  Assert.assertTrue(
                      "Expected a recent lastUpdated timestamp",
                      lastUpdated.after(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES))));
                });
      }
      Assert.assertEquals(4, beneficiaryHistoryEntries.size());

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Last Name inserted with value of "Johnson"
      Assert.assertEquals("Johnson", beneficiaryFromDb.getNameSurname());
      // Following fields were NOT changed in update record
      Assert.assertEquals("John", beneficiaryFromDb.getNameGiven());
      Assert.assertEquals(new Character('A'), beneficiaryFromDb.getNameMiddleInitial().get());
      Assert.assertEquals(
          "Beneficiary has MBI", Optional.of("SSSS"), beneficiaryFromDb.getMedicareBeneficiaryId());
      Assert.assertEquals(
          "Beneficiary has mbiHash",
          Optional.of("401441595efcc68bc5b26f4e88bd9fa550004e068d69ff75761ab946ec553a02"),
          beneficiaryFromDb.getMbiHash());
      // A recent lastUpdated timestamp
      Assert.assertTrue(
          "Expected a lastUpdated field", beneficiaryFromDb.getLastUpdated().isPresent());
      beneficiaryFromDb
          .getLastUpdated()
          .ifPresent(
              lastUpdated -> {
                Assert.assertTrue(
                    "Expected a recent lastUpdated timestamp",
                    lastUpdated.after(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
              });

      CarrierClaim carrierRecordFromDb = entityManager.find(CarrierClaim.class, "9991831999");
      Assert.assertEquals('N', carrierRecordFromDb.getFinalAction());
      // DateThrough inserted with value 10-27-1999
      Assert.assertEquals(
          LocalDate.of(2000, Month.OCTOBER, 27), carrierRecordFromDb.getDateThrough());
      Assert.assertEquals(1, carrierRecordFromDb.getLines().size());
      // A recent lastUpdated timestamp
      Assert.assertTrue(
          "Expected a lastUpdated field", carrierRecordFromDb.getLastUpdated().isPresent());
      carrierRecordFromDb
          .getLastUpdated()
          .ifPresent(
              lastUpdated -> {
                Assert.assertTrue(
                    "Expected a recent lastUpdated timestamp",
                    lastUpdated.after(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
              });

      CarrierClaimLine carrierLineRecordFromDb = carrierRecordFromDb.getLines().get(0);
      // CliaLabNumber inserted with value BB889999AA
      Assert.assertEquals("GG443333HH", carrierLineRecordFromDb.getCliaLabNumber().get());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_U} data. */
  @Test
  public void loadSampleUUnchanged() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // this should insert a new beneficiary history record
    /*
     * FIXME Why is this called "_UNCHANGED" if it will result in a new bene history record? Is the
     * name off, or are we still creating some unnecessary history records?
     */
    loadSample(dataSource, Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    long start = System.currentTimeMillis();
    // this should bypass inserting a new beneficiary history record because it already exists
    loadSample(dataSource, Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
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
        Assert.assertEquals("567834", beneHistory.getBeneficiaryId());
        // A recent lastUpdated timestamp
        Assert.assertTrue("Expected a lastUpdated field", beneHistory.getLastUpdated().isPresent());
        long end = System.currentTimeMillis();
        // finding the time difference and converting it into seconds
        long secs = (end - start) / 1000L;
        beneHistory
            .getLastUpdated()
            .ifPresent(
                lastUpdated -> {
                  Assert.assertFalse(
                      "Expected not a recent lastUpdated timestamp",
                      lastUpdated.after(Date.from(Instant.now().minusSeconds(secs))));
                });
      }
      // Make sure the size is the same and no records have been inserted if the same fields in the
      // beneficiary history table are the same.
      Assert.assertEquals(4, beneficiaryHistoryEntries.size());

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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    // Loads sample A Data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks all 12 months are in beneficiary monthlys for that beneficiary
      Assert.assertEquals(12, beneficiaryFromDb.getBeneficiaryMonthlys().size());
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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    // Loads first year of data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Loads second year of data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks to make sure we have 2 years or 24 months of data
      Assert.assertEquals(24, beneficiaryFromDb.getBeneficiaryMonthlys().size());
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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    // Loads first year of data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Loads second year of data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    // Loads  second year of data with only 8 months
    loadSample(
        dataSource,
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS.getResources()));

    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Checks to make sure we only have 20 months of data
      Assert.assertEquals(20, beneficiaryFromDb.getBeneficiaryMonthlys().size());
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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    // Load first year of data
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // Load 8 months of data in year two
    loadSample(
        dataSource,
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS.getResources()));

    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      Assert.assertEquals(20, beneficiaryFromDb.getBeneficiaryMonthlys().size());

      BeneficiaryMonthly augustMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(19);
      Assert.assertEquals("2019-08-01", augustMonthly.getYearMonth().toString());
      Assert.assertEquals("C", augustMonthly.getEntitlementBuyInInd().get().toString());
      Assert.assertEquals("AA", augustMonthly.getFipsStateCntyCode().get());
      Assert.assertFalse(augustMonthly.getHmoIndicatorInd().isPresent());
      Assert.assertEquals("AA", augustMonthly.getMedicaidDualEligibilityCode().get());
      Assert.assertEquals("AA", augustMonthly.getMedicareStatusCode().get());
      Assert.assertEquals("C", augustMonthly.getPartCContractNumberId().get());
      Assert.assertEquals("C", augustMonthly.getPartCPbpNumberId().get());
      Assert.assertEquals("C", augustMonthly.getPartCPlanTypeCode().get());
      Assert.assertEquals("C", augustMonthly.getPartDContractNumberId().get());
      Assert.assertEquals("AA", augustMonthly.getPartDLowIncomeCostShareGroupCode().get());
      Assert.assertFalse(augustMonthly.getPartDPbpNumberId().isPresent());
      Assert.assertEquals("C", augustMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      Assert.assertFalse(augustMonthly.getPartDSegmentNumberId().isPresent());

    } finally {
      if (entityManager != null) entityManager.close();
    }
    // Load 9 months of data in year two with some data updated in july
    loadSample(
        dataSource,
        Arrays.asList(StaticRifResourceGroup.SAMPLE_U_BENES_CHANGED_WITH_9_MONTHS.getResources()));

    entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory(dataSource);
    entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      Assert.assertEquals(21, beneficiaryFromDb.getBeneficiaryMonthlys().size());
      BeneficiaryMonthly augustMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(19);
      Assert.assertEquals("2019-08-01", augustMonthly.getYearMonth().toString());
      Assert.assertEquals("C", augustMonthly.getEntitlementBuyInInd().get().toString());
      Assert.assertEquals("AA", augustMonthly.getFipsStateCntyCode().get());
      // Updated in file
      Assert.assertEquals("C", augustMonthly.getHmoIndicatorInd().get().toString());
      Assert.assertEquals("AA", augustMonthly.getMedicaidDualEligibilityCode().get());
      Assert.assertEquals("AA", augustMonthly.getMedicareStatusCode().get());
      Assert.assertEquals("C", augustMonthly.getPartCContractNumberId().get());
      Assert.assertEquals("C", augustMonthly.getPartCPbpNumberId().get());
      Assert.assertEquals("C", augustMonthly.getPartCPlanTypeCode().get());
      Assert.assertEquals("C", augustMonthly.getPartDContractNumberId().get());
      Assert.assertEquals("AA", augustMonthly.getPartDLowIncomeCostShareGroupCode().get());
      Assert.assertFalse(augustMonthly.getPartDPbpNumberId().isPresent());
      Assert.assertEquals("C", augustMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      Assert.assertFalse(augustMonthly.getPartDSegmentNumberId().isPresent());

      BeneficiaryMonthly septMonthly = beneficiaryFromDb.getBeneficiaryMonthlys().get(20);
      Assert.assertEquals("2019-09-01", septMonthly.getYearMonth().toString());
      Assert.assertFalse(septMonthly.getEntitlementBuyInInd().isPresent());
      Assert.assertFalse(septMonthly.getFipsStateCntyCode().isPresent());
      Assert.assertFalse(septMonthly.getHmoIndicatorInd().isPresent());
      Assert.assertEquals("AA", septMonthly.getMedicaidDualEligibilityCode().get());
      Assert.assertFalse(septMonthly.getMedicareStatusCode().isPresent());
      Assert.assertFalse(septMonthly.getPartCContractNumberId().isPresent());
      Assert.assertFalse(septMonthly.getPartCPbpNumberId().isPresent());
      Assert.assertFalse(septMonthly.getPartCPlanTypeCode().isPresent());
      Assert.assertFalse(septMonthly.getPartDContractNumberId().isPresent());
      Assert.assertFalse(septMonthly.getPartDLowIncomeCostShareGroupCode().isPresent());
      Assert.assertFalse(septMonthly.getPartDPbpNumberId().isPresent());
      Assert.assertEquals("C", septMonthly.getPartDRetireeDrugSubsidyInd().get().toString());
      Assert.assertFalse(septMonthly.getPartDSegmentNumberId().isPresent());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_B} data. */
  @Ignore
  @Test
  public void loadSampleB() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_B.getResources()));
  }

  /**
   * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SYNTHETIC_DATA} data.
   *
   * <p>This test only works with a PostgreSQL database instance. It 10s or minutes to run.
   */
  @Ignore
  @Test
  public void loadSyntheticData() {
    /*Assume.assumeTrue(
    String.format(
        "Not enough memory for this test (%s bytes max). Run with '-Xmx5g' or more.",
        Runtime.getRuntime().maxMemory()),
    Runtime.getRuntime().maxMemory() >= 4500000000L); */
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources()));
  }

  /** Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_MCT} data. */
  @Test
  public void loadSampleMctData() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_1.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_2.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_3.getResources()));
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param dataSource a {@link DataSource} for the test DB to use
   * @param sampleGroup the {@link StaticRifResourceGroup} to load
   */
  private void loadSample(DataSource dataSource, List<StaticRifResource> sampleResources) {
    LOGGER.info("Loading RIF file from {}...", sampleResources.get(0).getResourceUrl().toString());

    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

    // Create the processors that will handle each stage of the pipeline.
    MetricRegistry appMetrics = new MetricRegistry();
    RifFilesProcessor processor = new RifFilesProcessor();
    LoadAppOptions options = RifLoaderTestUtils.getLoadOptions();
    RifLoader loader = new RifLoader(appMetrics, options, dataSource);

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
    LOGGER.info("Loaded RIF records: '{}'.", loadCount.get());
    Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build().report();

    // Verify that the expected number of records were run successfully.
    Assert.assertEquals(0, failureCount.get());
    Assert.assertEquals(
        "Unexpected number of loaded records.",
        sampleResources.stream().mapToInt(r -> r.getRecordCount()).sum(),
        loadCount.get());

    /*
     * Run the extraction an extra time and verify that each record can now
     * be found in the database.
     */
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(dataSource);
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
    loader.close();
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
                      beneficiaryHistoryToFind.getBeneficiaryId()),
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
          Assert.assertNotNull(beneficiaryHistoryFound);
          Assert.assertFalse(beneficiaryHistoryFound.isEmpty());
        } else {
          Object recordId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(record);
          Object recordFromDb = entityManager.find(record.getClass(), recordId);
          Assert.assertNotNull(recordFromDb);
        }
      }
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  public static void assertBeneficiaryMonthly(Beneficiary beneficiaryFromDb) {
    List<BeneficiaryMonthly> beneficiaryMonthly = beneficiaryFromDb.getBeneficiaryMonthlys();

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        1,
        beneficiaryMonthly.get(0),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        2,
        beneficiaryMonthly.get(1),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        3,
        beneficiaryMonthly.get(2),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        4,
        beneficiaryMonthly.get(3),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        5,
        beneficiaryMonthly.get(4),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        6,
        beneficiaryMonthly.get(5),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        7,
        beneficiaryMonthly.get(6),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        8,
        beneficiaryMonthly.get(7),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        9,
        beneficiaryMonthly.get(8),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        10,
        beneficiaryMonthly.get(9),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        11,
        beneficiaryMonthly.get(10),
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
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().intValue(),
        12,
        beneficiaryMonthly.get(11),
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

    Assert.assertEquals(LocalDate.of(referenceYear, month, 1), enrollment.getYearMonth());
    Assert.assertEquals(
        entitlementBuyInInd.orElse(null), enrollment.getEntitlementBuyInInd().orElse(null));
    Assert.assertEquals(
        fipsStateCntyCode.orElse(null), enrollment.getFipsStateCntyCode().orElse(null));
    Assert.assertEquals(hmoIndicatorInd.orElse(null), enrollment.getHmoIndicatorInd().orElse(null));
    Assert.assertEquals(
        medicaidDualEligibilityCode.orElse(null),
        enrollment.getMedicaidDualEligibilityCode().orElse(null));
    Assert.assertEquals(
        medicareStatusCode.orElse(null), enrollment.getMedicareStatusCode().orElse(null));
    Assert.assertEquals(
        partCContractNumberId.orElse(null), enrollment.getPartCContractNumberId().orElse(null));
    Assert.assertEquals(
        partCPbpNumberId.orElse(null), enrollment.getPartCPbpNumberId().orElse(null));
    Assert.assertEquals(
        partCPlanTypeCode.orElse(null), enrollment.getPartCPlanTypeCode().orElse(null));
    Assert.assertEquals(
        partDContractNumberId.orElse(null), enrollment.getPartDContractNumberId().orElse(null));
    Assert.assertEquals(
        partDLowIncomeCostShareGroupCode.orElse(null),
        enrollment.getPartDLowIncomeCostShareGroupCode().orElse(null));
    Assert.assertEquals(
        partDPbpNumberId.orElse(null), enrollment.getPartDPbpNumberId().orElse(null));
    Assert.assertEquals(
        partDRetireeDrugSubsidyInd.orElse(null),
        enrollment.getPartDRetireeDrugSubsidyInd().orElse(null));
    Assert.assertEquals(
        partDSegmentNumberId.orElse(null), enrollment.getPartDSegmentNumberId().orElse(null));
  }
}
