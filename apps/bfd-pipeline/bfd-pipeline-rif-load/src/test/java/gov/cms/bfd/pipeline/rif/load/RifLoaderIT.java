package gov.cms.bfd.pipeline.rif.load;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_A);
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_U} data.
   */
  @Test
  public void loadSampleU() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_A);
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_U);

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
      }
      Assert.assertEquals(4, beneficiaryHistoryEntries.size());

      Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
      // Last Name inserted with value of "Doe"
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

      CarrierClaim carrierRecordFromDb = entityManager.find(CarrierClaim.class, "9991831999");
      Assert.assertEquals('N', carrierRecordFromDb.getFinalAction());
      // DateThrough inserted with value 10-27-1999
      Assert.assertEquals(
          LocalDate.of(2000, Month.OCTOBER, 27), carrierRecordFromDb.getDateThrough());
      Assert.assertEquals(1, carrierRecordFromDb.getLines().size());

      CarrierClaimLine carrierLineRecordFromDb = carrierRecordFromDb.getLines().get(0);
      // CliaLabNumber inserted with value BB889999AA
      Assert.assertEquals("GG443333HH", carrierLineRecordFromDb.getCliaLabNumber().get());
    } finally {
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_B} data.
   */
  @Ignore
  @Test
  public void loadSampleB() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_B);
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
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, StaticRifResourceGroup.SYNTHETIC_DATA);
  }

  /**
   * Runs {@link gov.cms.bfd.pipeline.rif.load.RifLoader} against the {@link
   * StaticRifResourceGroup#SAMPLE_MCT} data.
   */
  @Test
  public void loadSampleMctData() {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_MCT);
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_MCT_UPDATE_1);
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_MCT_UPDATE_2);
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_MCT_UPDATE_3);
  }

  /** Tests the RifLoaderIdleTasks class with a Sample. Note: only works with Postgres. */
  @Ignore
  @Test
  public void runIdleTasks() {
    final DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_A);
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
    loadSample(dataSource, StaticRifResourceGroup.SAMPLE_A);
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
  private void loadSample(DataSource dataSource, StaticRifResourceGroup sampleGroup) {
    // Generate the sample RIF data to feed through the pipeline.
    List<StaticRifResource> sampleResources =
        Arrays.stream(sampleGroup.getResources()).collect(Collectors.toList());

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
            fixupsEnabled));
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
