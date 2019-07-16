package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory_;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileRecords;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;

/**
 * Integration tests for {@link RifLoader}.
 */
public final class RifLoaderIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIT.class);

	/**
	 * Runs {@link RifLoader} against the
	 * {@link StaticRifResourceGroup#SAMPLE_A} data.
	 */
	@Test
	public void loadSampleA() {
		loadSample(StaticRifResourceGroup.SAMPLE_A);
	}

	/**
	 * Runs {@link RifLoader} against the
	 * {@link StaticRifResourceGroup#SAMPLE_U} data.
	 */
	@Test
	public void loadSampleU() {
		loadSample(StaticRifResourceGroup.SAMPLE_A);
		loadSample(StaticRifResourceGroup.SAMPLE_U);

		/*
		 * Verify that the updates worked as expected by manually checking some fields.
		 */
		LoadAppOptions options = RifLoaderTestUtils.getLoadOptions();
		EntityManagerFactory entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory(options);
		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory.createEntityManager();

			CriteriaQuery<BeneficiaryHistory> beneficiaryHistoryCriteria = entityManager.getCriteriaBuilder()
					.createQuery(BeneficiaryHistory.class);
			List<BeneficiaryHistory> beneficiaryHistoryEntries = entityManager.createQuery(
					beneficiaryHistoryCriteria.select(beneficiaryHistoryCriteria.from(BeneficiaryHistory.class)))
					.getResultList();
			Assert.assertEquals(4, beneficiaryHistoryEntries.size());

			Beneficiary beneficiaryFromDb = entityManager.find(Beneficiary.class, "567834");
			// Last Name inserted with value of "Doe"
			Assert.assertEquals("Johnson", beneficiaryFromDb.getNameSurname());
			// Following fields were NOT changed in update record
			Assert.assertEquals("John", beneficiaryFromDb.getNameGiven());
			Assert.assertEquals(new Character('A'), beneficiaryFromDb.getNameMiddleInitial().get());

			CarrierClaim carrierRecordFromDb = entityManager.find(CarrierClaim.class, "9991831999");
			Assert.assertEquals('N', carrierRecordFromDb.getFinalAction());
			// DateThrough inserted with value 10-27-1999
			Assert.assertEquals(LocalDate.of(2000, Month.OCTOBER, 27), carrierRecordFromDb.getDateThrough());
			Assert.assertEquals(1, carrierRecordFromDb.getLines().size());

			CarrierClaimLine carrierLineRecordFromDb = carrierRecordFromDb.getLines().get(0);
			// CliaLabNumber inserted with value BB889999AA
			Assert.assertEquals("GG443333HH", carrierLineRecordFromDb.getCliaLabNumber().get());
		} finally {
			if (entityManager != null)
				entityManager.close();
		}
	}

	/**
	 * Runs {@link RifLoader} against the
	 * {@link StaticRifResourceGroup#SAMPLE_B} data.
	 */
	@Ignore
	@Test
	public void loadSampleB() {
		loadSample(StaticRifResourceGroup.SAMPLE_B);
	}

	/**
	 * Runs {@link RifLoader} against the
	 * {@link StaticRifResourceGroup#SYNTHETIC_DATA} data.
	 */
	@Ignore
	@Test
	public void loadSyntheticData() {
		Assume.assumeTrue(String.format("Not enough memory for this test (%s bytes max). Run with '-Xmx5g' or more.",
				Runtime.getRuntime().maxMemory()), Runtime.getRuntime().maxMemory() >= 4500000000L);
		loadSample(StaticRifResourceGroup.SYNTHETIC_DATA);
	}

	/**
	 * Runs {@link RifLoader} against the {@link StaticRifResourceGroup#SAMPLE_MCT}
	 * data.
	 */
	@Test
	public void loadSampleMctData() {
		loadSample(StaticRifResourceGroup.SAMPLE_MCT);
		loadSample(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_1);
		loadSample(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_2);
	}

	/**
	 * Runs {@link RifLoader} against the specified
	 * {@link StaticRifResourceGroup}.
	 * 
	 * @param sampleGroup
	 *            the {@link StaticRifResourceGroup} to load
	 */
	private void loadSample(StaticRifResourceGroup sampleGroup) {
		// Generate the sample RIF data to feed through the pipeline.
		List<StaticRifResource> sampleResources = Arrays.stream(sampleGroup.getResources())
				.collect(Collectors.toList());

		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(),
				sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

		// Create the processors that will handle each stage of the pipeline.
		MetricRegistry appMetrics = new MetricRegistry();
		RifFilesProcessor processor = new RifFilesProcessor();
		LoadAppOptions options = RifLoaderTestUtils.getLoadOptions();
		RifLoader loader = new RifLoader(appMetrics, options);

		// Link up the pipeline and run it.
		LOGGER.info("Loading RIF records...");
		AtomicInteger failureCount = new AtomicInteger(0);
		AtomicInteger loadCount = new AtomicInteger(0);
		for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
			RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
			loader.process(rifFileRecords, error -> {
				failureCount.incrementAndGet();
				LOGGER.warn("Record(s) failed to load.", error);
			}, result -> {
				loadCount.incrementAndGet();
			});
			Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build().report();
		}
		LOGGER.info("Loaded RIF records: '{}'.", loadCount.get());
		Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build().report();

		// Verify that the expected number of records were run successfully.
		Assert.assertEquals(0, failureCount.get());
		Assert.assertEquals("Unexpected number of loaded records.",
				sampleResources.stream().mapToInt(r -> r.getRecordCount()).sum(), loadCount.get());

		/*
		 * Run the extraction an extra time and verify that each record can now
		 * be found in the database.
		 */
		EntityManagerFactory entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory(options);
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
			RifFileRecords rifFileRecordsCopy = processor.produceRecords(rifFilesEventSingle.getFileEvents().get(0));
			assertAreInDatabase(options, entityManagerFactory, rifFileRecordsCopy.getRecords().map(r -> r.getRecord()));
		}
		LOGGER.info("All records found in DB.");
	}

	/**
	 * Ensures that {@link RifLoaderTestUtils#cleanDatabaseServer()} is called
	 * after each test case.
	 */
	@After
	public void cleanDatabaseServerAfterEachTestCase() {
		RifLoaderTestUtils.cleanDatabaseServer(RifLoaderTestUtils.getLoadOptions());
	}

	/**
	 * Verifies that the specified RIF records are actually in the database.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param entityManagerFactory
	 *            the {@link EntityManagerFactory} to use
	 * @param records
	 *            the RIF records to verify
	 */
	private static void assertAreInDatabase(LoadAppOptions options, EntityManagerFactory entityManagerFactory,
			Stream<Object> records) {
		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory.createEntityManager();

			for (Object record : records.collect(Collectors.toList())) {
				/*
				 * We need to handle BeneficiaryHistory separately, as it has a generated ID.
				 */
				if (record instanceof BeneficiaryHistory) {
					BeneficiaryHistory beneficiaryHistoryToFind = (BeneficiaryHistory) record;
					beneficiaryHistoryToFind.setHicn(RifLoader.computeHicnHash(options,
							RifLoader.createSecretKeyFactory(), beneficiaryHistoryToFind.getHicn()));

					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<BeneficiaryHistory> query = criteriaBuilder.createQuery(BeneficiaryHistory.class);
					Root<BeneficiaryHistory> from = query.from(BeneficiaryHistory.class);
					query.select(from).where(
							criteriaBuilder.equal(from.get(BeneficiaryHistory_.beneficiaryId),
									beneficiaryHistoryToFind.getBeneficiaryId()),
							criteriaBuilder.equal(from.get(BeneficiaryHistory_.birthDate),
									beneficiaryHistoryToFind.getBirthDate()),
							criteriaBuilder.equal(from.get(BeneficiaryHistory_.sex), beneficiaryHistoryToFind.getSex()),
							criteriaBuilder.equal(from.get(BeneficiaryHistory_.hicn),
									beneficiaryHistoryToFind.getHicn()));
					List<BeneficiaryHistory> beneficiaryHistoryFound = entityManager.createQuery(query).getResultList();
					Assert.assertNotNull(beneficiaryHistoryFound);
					Assert.assertFalse(beneficiaryHistoryFound.isEmpty());
				} else {
					Object recordId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(record);
					Object recordFromDb = entityManager.find(record.getClass(), recordId);
					Assert.assertNotNull(recordFromDb);
				}
			}
		} finally {
			if (entityManager != null)
				entityManager.close();
		}
	}
}
