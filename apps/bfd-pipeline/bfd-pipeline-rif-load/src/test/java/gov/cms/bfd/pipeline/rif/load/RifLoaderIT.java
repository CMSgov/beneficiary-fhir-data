package gov.cms.bfd.pipeline.rif.load;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.Enrollment;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import gov.cms.bfd.pipeline.rif.extract.RifFilesProcessor;
import java.time.Duration;
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

/** Integration tests for {@link gov.cms.bfd.pipeline.rif.load.RifLoader}. */
public final class RifLoaderIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIT.class);

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_A} data.
   */
  @Test
  public void loadSampleA() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
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

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_U} data.
   */
  @Test
  public void loadSampleU() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    LoadAppOptions options = RifLoaderTestUtils.getLoadOptions(dataSource);
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(options);
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

      assertEnrollments(beneficiaryFromDb, 12);

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

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_U} data.
   */
  @Test
  public void loadSampleUUnchanged() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    // this should insert a new beneficiary history record
    loadSample(dataSource, Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    long start = System.currentTimeMillis();
    // this should bypass inserting a new beneficiary history record because it already exists
    loadSample(dataSource, Arrays.asList(StaticRifResource.SAMPLE_U_BENES_UNCHANGED));

    /*
     * Verify that the updates worked as expected by manually checking some fields.
     */
    LoadAppOptions options = RifLoaderTestUtils.getLoadOptions(dataSource);
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(options);
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

  /* @Test
  public void loadEnrollmentUpdates() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    loadSample(
        dataSource,
        Arrays.asList(
            StaticRifResourceGroup.SAMPLE_U_BENES_REFERENCE_YEAR_CHANGE.getResources()));

    LoadAppOptions options = RifLoaderTestUtils.getLoadOptions(dataSource);
    EntityManagerFactory entityManagerFactory =
        RifLoaderTestUtils.createEntityManagerFactory(options);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      assertEnrollments(beneficiaryFromDb, 12);


    } finally {
      if (entityManager != null) entityManager.close();
    }
  } */
  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_B} data.
   */
  @Ignore
  @Test
  public void loadSampleB() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_B.getResources()));
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SYNTHETIC_DATA} data.
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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SYNTHETIC_DATA.getResources()));
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_MCT} data.
   */
  @Test
  public void loadSampleMctData() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_1.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_2.getResources()));
    loadSample(
        dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_3.getResources()));
  }

  /** Tests the RifLoaderIdleTasks class with a Sample. Note: only works with Postgres. */
  @Ignore
  @Test
  public void runIdleTasks() {
    final DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    RifLoader loader = createLoader(dataSource, true);

    // The sample are loaded with mbiHash set, clear them for this test
    clearMbiHash(loader);
    final String selectBeneficiary = "select b from Beneficiary b where b.mbiHash is null";
    EntityManager em = RifLoader.createEntityManagerFactory(dataSource).createEntityManager();
    Assert.assertFalse(
        "Should not be empty now",
        em.createQuery(selectBeneficiary, Beneficiary.class).getResultList().isEmpty());
    final String selectHistory = "select b from BeneficiaryHistory b where b.mbiHash is null";
    Assert.assertFalse(
        "Should not be empty now",
        em.createQuery(selectHistory, BeneficiaryHistory.class).getResultList().isEmpty());

    // Run the initial task
    Assert.assertEquals(
        "Should be running the initial task",
        RifLoaderIdleTasks.Task.INITIAL,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Run the post startup task
    Assert.assertEquals(
        "Should be running the post-startup task",
        RifLoaderIdleTasks.Task.POST_STARTUP,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Run the post startup beneficiary task
    Assert.assertEquals(
        "Should be running the post-startup task",
        RifLoaderIdleTasks.Task.POST_STARTUP_FIXUP_BENEFICIARIES,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Run the post startup beneficiary history task
    Assert.assertEquals(
        "Should be running the post-startup task",
        RifLoaderIdleTasks.Task.POST_STARTUP_FIXUP_BENEFICIARY_HISTORY,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Should mbiHash should be set now
    Assert.assertEquals(
        "Should be running the normal task",
        RifLoaderIdleTasks.Task.NORMAL,
        loader.getIdleTasks().getCurrentTask());
    Assert.assertTrue(
        "Expect all mbiHash have been filled",
        em.createQuery(selectBeneficiary, Beneficiary.class).getResultList().isEmpty());
    Assert.assertTrue(
        "Should all mbiHash should have been filled",
        em.createQuery(selectHistory, BeneficiaryHistory.class).getResultList().isEmpty());

    loader.close();
  }

  /** Tests the RifLoaderIdleTasks with no fixups needed. */
  @Test
  public void runIdleTasksWithNoFixups() {
    final DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    final RifLoader loader = createLoader(dataSource, false);

    // Should need no work
    final String selectBeneficiary = "select b from Beneficiary b where b.mbiHash is null";
    EntityManager em = RifLoader.createEntityManagerFactory(dataSource).createEntityManager();
    Assert.assertTrue(
        "Beneficiaries should be fixed up",
        em.createQuery(selectBeneficiary, Beneficiary.class).getResultList().isEmpty());
    final String selectHistory = "select b from BeneficiaryHistory b where b.mbiHash is null";
    Assert.assertTrue(
        "Histories should be fixed up",
        em.createQuery(selectHistory, BeneficiaryHistory.class).getResultList().isEmpty());

    // Run the initial task
    Assert.assertEquals(
        "Should be running the initial task",
        RifLoaderIdleTasks.Task.INITIAL,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Run the post startup task
    Assert.assertEquals(
        "Should be running the post-startup task",
        RifLoaderIdleTasks.Task.POST_STARTUP,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Should be normal now
    Assert.assertEquals(
        "Should be running the normal task",
        RifLoaderIdleTasks.Task.NORMAL,
        loader.getIdleTasks().getCurrentTask());

    loader.close();
  }

  /**
   * Tests the RifLoaderIdleTasks class with existing data in the database. Useful for profiling
   * against the beneficiary data set.
   */
  @Ignore
  @Test
  public void runExistingIdleTasks() {
    final DataSource dataSource = DatabaseTestHelper.getTestDatabase();
    final RifLoader loader = createLoader(dataSource, true);

    // The sample are loaded with mbiHash set, clear them for this test
    clearMbiHash(loader);

    // Run the initial task
    Assert.assertEquals(
        "Should be running the initial task",
        RifLoaderIdleTasks.Task.INITIAL,
        loader.getIdleTasks().getCurrentTask());
    loader.doIdleTask();

    // Run the post startup task
    Instant startTime = Instant.now();
    while (loader.getIdleTasks().getCurrentTask() != RifLoaderIdleTasks.Task.NORMAL) {
      loader.doIdleTask();
    }
    Duration time = Duration.between(startTime, Instant.now());
    LOGGER.info("Post migration took: {} seconds", time.getSeconds());

    // Should mbiHash should be set now
    Assert.assertEquals(
        "Should be running the normal task",
        RifLoaderIdleTasks.Task.NORMAL,
        loader.getIdleTasks().getCurrentTask());
    loader.close();
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the specified {@link
   * StaticRifResourceGroup}.
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
    LoadAppOptions options = RifLoaderTestUtils.getLoadOptions(dataSource);
    RifLoader loader = new RifLoader(appMetrics, options);

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
        RifLoaderTestUtils.createEntityManagerFactory(options);
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
   * @param options the {@link gov.cms.bfd.pipeline.rif.load.LoadAppOptions} to use
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
                  options, RifLoader.createSecretKeyFactory(), beneficiaryHistoryToFind.getHicn()));
          beneficiaryHistoryToFind.setMbiHash(
              beneficiaryHistoryToFind.getMedicareBeneficiaryId().isPresent()
                  ? Optional.of(
                      RifLoader.computeMbiHash(
                          options,
                          RifLoader.createSecretKeyFactory(),
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

  /**
   * Create a RIF loader
   *
   * @param dataSource to use
   * @param fixupsEnabled option
   */
  private static RifLoader createLoader(DataSource dataSource, boolean fixupsEnabled) {
    MetricRegistry appMetrics = new MetricRegistry();
    LoadAppOptions defaultOptions = RifLoaderTestUtils.getLoadOptions(dataSource);
    return new RifLoader(
        appMetrics,
        new LoadAppOptions(
            defaultOptions.getHicnHashIterations(),
            defaultOptions.getHicnHashPepper(),
            defaultOptions.getDatabaseDataSource(),
            defaultOptions.getLoaderThreads(),
            defaultOptions.isIdempotencyRequired(),
            fixupsEnabled,
            defaultOptions.getFixupThreads()));
  }

  public static void assertEnrollments(Beneficiary beneficiaryFromDb, int enrollmentSize) {
    List<Enrollment> enrollments = beneficiaryFromDb.getEnrollments();

    Assert.assertEquals(enrollmentSize, enrollments.size());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "01",
        enrollments.get(0),
        beneficiaryFromDb.getEntitlementBuyInJanInd().get(),
        beneficiaryFromDb.getFipsStateCntyJanCode().get(),
        beneficiaryFromDb.getHmoIndicatorJanInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityJanCode().get(),
        beneficiaryFromDb.getMedicareStatusJanCode().get(),
        beneficiaryFromDb.getPartCContractNumberJanId().get(),
        beneficiaryFromDb.getPartCPbpNumberJanId().get(),
        beneficiaryFromDb.getPartCPlanTypeJanCode().get(),
        beneficiaryFromDb.getPartDContractNumberJanId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJanCode().get(),
        beneficiaryFromDb.getPartDPbpNumberJanId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJanInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberJanId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "02",
        enrollments.get(1),
        beneficiaryFromDb.getEntitlementBuyInFebInd().get(),
        beneficiaryFromDb.getFipsStateCntyFebCode().get(),
        beneficiaryFromDb.getHmoIndicatorFebInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityFebCode().get(),
        beneficiaryFromDb.getMedicareStatusFebCode().get(),
        beneficiaryFromDb.getPartCContractNumberFebId().get(),
        beneficiaryFromDb.getPartCPbpNumberFebId().get(),
        beneficiaryFromDb.getPartCPlanTypeFebCode().get(),
        beneficiaryFromDb.getPartDContractNumberFebId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupFebCode().get(),
        beneficiaryFromDb.getPartDPbpNumberFebId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyFebInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberFebId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "03",
        enrollments.get(2),
        beneficiaryFromDb.getEntitlementBuyInMarInd().get(),
        beneficiaryFromDb.getFipsStateCntyMarCode().get(),
        beneficiaryFromDb.getHmoIndicatorMarInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityMarCode().get(),
        beneficiaryFromDb.getMedicareStatusMarCode().get(),
        beneficiaryFromDb.getPartCContractNumberMarId().get(),
        beneficiaryFromDb.getPartCPbpNumberMarId().get(),
        beneficiaryFromDb.getPartCPlanTypeMarCode().get(),
        beneficiaryFromDb.getPartDContractNumberMarId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupMarCode().get(),
        beneficiaryFromDb.getPartDPbpNumberMarId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyMarInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberMarId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "04",
        enrollments.get(3),
        beneficiaryFromDb.getEntitlementBuyInAprInd().get(),
        beneficiaryFromDb.getFipsStateCntyAprCode().get(),
        beneficiaryFromDb.getHmoIndicatorAprInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityAprCode().get(),
        beneficiaryFromDb.getMedicareStatusAprCode().get(),
        beneficiaryFromDb.getPartCContractNumberAprId().get(),
        beneficiaryFromDb.getPartCPbpNumberAprId().get(),
        beneficiaryFromDb.getPartCPlanTypeAprCode().get(),
        beneficiaryFromDb.getPartDContractNumberAprId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupAprCode().get(),
        beneficiaryFromDb.getPartDPbpNumberAprId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyAprInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberAprId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "05",
        enrollments.get(4),
        beneficiaryFromDb.getEntitlementBuyInMayInd().get(),
        beneficiaryFromDb.getFipsStateCntyMayCode().get(),
        beneficiaryFromDb.getHmoIndicatorMayInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityMayCode().get(),
        beneficiaryFromDb.getMedicareStatusMayCode().get(),
        beneficiaryFromDb.getPartCContractNumberMayId().get(),
        beneficiaryFromDb.getPartCPbpNumberMayId().get(),
        beneficiaryFromDb.getPartCPlanTypeMayCode().get(),
        beneficiaryFromDb.getPartDContractNumberMayId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupMayCode().get(),
        beneficiaryFromDb.getPartDPbpNumberMayId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyMayInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberMayId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "06",
        enrollments.get(5),
        beneficiaryFromDb.getEntitlementBuyInJunInd().get(),
        beneficiaryFromDb.getFipsStateCntyJunCode().get(),
        beneficiaryFromDb.getHmoIndicatorJunInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityJunCode().get(),
        beneficiaryFromDb.getMedicareStatusJunCode().get(),
        beneficiaryFromDb.getPartCContractNumberJunId().get(),
        beneficiaryFromDb.getPartCPbpNumberJunId().get(),
        beneficiaryFromDb.getPartCPlanTypeJunCode().get(),
        beneficiaryFromDb.getPartDContractNumberJunId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJunCode().get(),
        beneficiaryFromDb.getPartDPbpNumberJunId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJunInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberJunId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "07",
        enrollments.get(6),
        beneficiaryFromDb.getEntitlementBuyInJulInd().get(),
        beneficiaryFromDb.getFipsStateCntyJulCode().get(),
        beneficiaryFromDb.getHmoIndicatorJulInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityJulCode().get(),
        beneficiaryFromDb.getMedicareStatusJulCode().get(),
        beneficiaryFromDb.getPartCContractNumberJulId().get(),
        beneficiaryFromDb.getPartCPbpNumberJulId().get(),
        beneficiaryFromDb.getPartCPlanTypeJulCode().get(),
        beneficiaryFromDb.getPartDContractNumberJulId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupJulCode().get(),
        beneficiaryFromDb.getPartDPbpNumberJulId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyJulInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberJulId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "08",
        enrollments.get(7),
        beneficiaryFromDb.getEntitlementBuyInAugInd().get(),
        beneficiaryFromDb.getFipsStateCntyAugCode().get(),
        beneficiaryFromDb.getHmoIndicatorAugInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityAugCode().get(),
        beneficiaryFromDb.getMedicareStatusAugCode().get(),
        beneficiaryFromDb.getPartCContractNumberAugId().get(),
        beneficiaryFromDb.getPartCPbpNumberAugId().get(),
        beneficiaryFromDb.getPartCPlanTypeAugCode().get(),
        beneficiaryFromDb.getPartDContractNumberAugId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupAugCode().get(),
        beneficiaryFromDb.getPartDPbpNumberAugId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyAugInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberAugId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "09",
        enrollments.get(8),
        beneficiaryFromDb.getEntitlementBuyInSeptInd().get(),
        beneficiaryFromDb.getFipsStateCntySeptCode().get(),
        beneficiaryFromDb.getHmoIndicatorSeptInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilitySeptCode().get(),
        beneficiaryFromDb.getMedicareStatusSeptCode().get(),
        beneficiaryFromDb.getPartCContractNumberSeptId().get(),
        beneficiaryFromDb.getPartCPbpNumberSeptId().get(),
        beneficiaryFromDb.getPartCPlanTypeSeptCode().get(),
        beneficiaryFromDb.getPartDContractNumberSeptId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupSeptCode().get(),
        beneficiaryFromDb.getPartDPbpNumberSeptId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidySeptInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberSeptId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "10",
        enrollments.get(9),
        beneficiaryFromDb.getEntitlementBuyInOctInd().get(),
        beneficiaryFromDb.getFipsStateCntyOctCode().get(),
        beneficiaryFromDb.getHmoIndicatorOctInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityOctCode().get(),
        beneficiaryFromDb.getMedicareStatusOctCode().get(),
        beneficiaryFromDb.getPartCContractNumberOctId().get(),
        beneficiaryFromDb.getPartCPbpNumberOctId().get(),
        beneficiaryFromDb.getPartCPlanTypeOctCode().get(),
        beneficiaryFromDb.getPartDContractNumberOctId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupOctCode().get(),
        beneficiaryFromDb.getPartDPbpNumberOctId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyOctInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberOctId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "11",
        enrollments.get(10),
        beneficiaryFromDb.getEntitlementBuyInNovInd().get(),
        beneficiaryFromDb.getFipsStateCntyNovCode().get(),
        beneficiaryFromDb.getHmoIndicatorNovInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityNovCode().get(),
        beneficiaryFromDb.getMedicareStatusNovCode().get(),
        beneficiaryFromDb.getPartCContractNumberNovId().get(),
        beneficiaryFromDb.getPartCPbpNumberNovId().get(),
        beneficiaryFromDb.getPartCPlanTypeNovCode().get(),
        beneficiaryFromDb.getPartDContractNumberNovId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupNovCode().get(),
        beneficiaryFromDb.getPartDPbpNumberNovId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyNovInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberNovId().get());

    checkEnrollments(
        beneficiaryFromDb.getBeneEnrollmentReferenceYear().get().toString(),
        "12",
        enrollments.get(11),
        beneficiaryFromDb.getEntitlementBuyInDecInd().get(),
        beneficiaryFromDb.getFipsStateCntyDecCode().get(),
        beneficiaryFromDb.getHmoIndicatorDecInd().get(),
        beneficiaryFromDb.getMedicaidDualEligibilityDecCode().get(),
        beneficiaryFromDb.getMedicareStatusDecCode().get(),
        beneficiaryFromDb.getPartCContractNumberDecId().get(),
        beneficiaryFromDb.getPartCPbpNumberDecId().get(),
        beneficiaryFromDb.getPartCPlanTypeDecCode().get(),
        beneficiaryFromDb.getPartDContractNumberDecId().get(),
        beneficiaryFromDb.getPartDLowIncomeCostShareGroupDecCode().get(),
        beneficiaryFromDb.getPartDPbpNumberDecId().get(),
        beneficiaryFromDb.getPartDRetireeDrugSubsidyDecInd().get(),
        beneficiaryFromDb.getPartDSegmentNumberDecId().get());
  }

  public static void checkEnrollments(
      String referenceYear,
      String month,
      Enrollment enrollment,
      Character entitlementBuyInInd,
      String fipsStateCntyCode,
      Character hmoIndicatorInd,
      String medicaidDualEligibilityCode,
      String medicareStatusCode,
      String partCContractNumberId,
      String partCPbpNumberId,
      String partCPlanTypeCode,
      String partDContractNumberId,
      String partDLowIncomeCostShareGroupCode,
      String partDPbpNumberId,
      Character partDRetireeDrugSubsidyInd,
      String partDSegmentNumberId) {

    Assert.assertEquals(String.format("%s-%s", referenceYear, month), enrollment.getYearMonth());
    Assert.assertEquals(entitlementBuyInInd, enrollment.getEntitlementBuyInInd().get());
    Assert.assertEquals(fipsStateCntyCode, enrollment.getFipsStateCntyCode().get());
    Assert.assertEquals(hmoIndicatorInd, enrollment.getHmoIndicatorInd().get());
    Assert.assertEquals(
        medicaidDualEligibilityCode, enrollment.getMedicaidDualEligibilityCode().get());
    Assert.assertEquals(medicareStatusCode, enrollment.getMedicareStatusCode().get());
    Assert.assertEquals(partCContractNumberId, enrollment.getPartCContractNumberId().get());
    Assert.assertEquals(partCPbpNumberId, enrollment.getPartCPbpNumberId().get());
    Assert.assertEquals(partCPlanTypeCode, enrollment.getPartCPlanTypeCode().get());
    Assert.assertEquals(partDContractNumberId, enrollment.getPartDContractNumberId().get());
    Assert.assertEquals(
        partDLowIncomeCostShareGroupCode, enrollment.getPartDLowIncomeCostShareGroupCode().get());
    Assert.assertEquals(partDPbpNumberId, enrollment.getPartDPbpNumberId().get());
    Assert.assertEquals(
        partDRetireeDrugSubsidyInd, enrollment.getPartDRetireeDrugSubsidyInd().get());
    Assert.assertEquals(partDSegmentNumberId, enrollment.getPartDSegmentNumberId().get());
  }
  /**
   * Clear the MBI hash fields in the db
   *
   * @param loader the loader and the db connection within
   */
  private static void clearMbiHash(final RifLoader loader) {
    loader
        .getIdleTasks()
        .doBatches(
            (session) -> {
              session.createQuery("update Beneficiary set mbiHash = null").executeUpdate();
              session.createQuery("update BeneficiaryHistory set mbiHash = null").executeUpdate();
              return true;
            });
  }
}
