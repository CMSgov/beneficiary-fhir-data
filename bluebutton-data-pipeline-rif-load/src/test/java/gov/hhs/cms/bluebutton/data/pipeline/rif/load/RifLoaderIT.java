package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

import gov.hhs.cms.bluebutton.data.model.rif.RifFileRecords;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.data.pipeline.rif.load.RifRecordLoadResult.LoadAction;
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
	 * {@link StaticRifResourceGroup#SAMPLE_B} data.
	 */
	@Test
	public void loadSampleB() {
		loadSample(StaticRifResourceGroup.SAMPLE_B);
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
				.filter(r -> r.getRifFileType() != RifFileType.PDE).collect(Collectors.toList());
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(),
				sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

		// Setup the metrics that we'll log to.
		MetricRegistry metrics = new MetricRegistry();

		// Create the processors that will handle each stage of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		RifLoader loader = new RifLoader(RifLoaderTestUtils.getLoadOptions(), metrics);

		// Link up the pipeline and run it.
		LOGGER.info("Loading RIF records...");
		AtomicInteger failureCount = new AtomicInteger(0);
		Queue<RifRecordLoadResult> successfulRecords = new ConcurrentLinkedQueue<>();
		for (RifFileRecords rifFileRecords : processor.process(rifFilesEvent)) {
			loader.process(rifFileRecords, error -> {
				failureCount.incrementAndGet();
				LOGGER.warn("Record(s) failed to load.", error);
			}, result -> {
				successfulRecords.add(result);
			});
		}
		LOGGER.info("Loaded RIF records: '{}'.", successfulRecords.size());
		Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
		metricsReporter.report();

		// Verify that the expected number of records were run successfully.
		Assert.assertEquals(0, failureCount.get());
		// FIXME remove successfulRecords filter once CBBD-266 is resolved
		Assert.assertEquals(sampleResources.stream().mapToInt(r -> r.getRecordCount()).sum(),
				successfulRecords.stream().filter(r -> r.getLoadAction() == LoadAction.INSERTED).count());

		/*
		 * Run the extraction an extra time and verify that each record can now
		 * be found in the database.
		 */
		EntityManagerFactory entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory();
		for (RifFileRecords rifFileRecordsCopy : processor.process(rifFilesEvent)) {
			rifFileRecordsCopy.getRecords().forEach(r -> {
				assertIsInDatabase(entityManagerFactory, r.getRecord());
			});
		}
	}

	/**
	 * Ensures that {@link RifLoaderTestUtils#cleanDatabaseServer()} is called
	 * after each test case.
	 */
	@After
	public void cleanDatabaseServerAfterEachTestCase() {
		RifLoaderTestUtils.cleanDatabaseServer();
	}

	/**
	 * Verifies that the specified RIF record is actually in the database.
	 * 
	 * @param entityManagerFactory
	 *            the {@link EntityManagerFactory} to use
	 * @param record
	 *            the RIF record to verify
	 */
	private static void assertIsInDatabase(EntityManagerFactory entityManagerFactory, Object record) {
		// TODO Auto-generated method stub
	}
}
