package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryRow;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
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
		// Generate the sample RIF data to feed through the pipeline.
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), StaticRifResourceGroup.SAMPLE_A.toRifFiles());

		// Setup the metrics that we'll log to.
		MetricRegistry fhirMetrics = new MetricRegistry();

		// Create the processors that will handle each stage of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		RifLoader loader = new RifLoader(RifLoaderTestUtils.getLoadOptions(), fhirMetrics);

		// Link up the pipeline and run it.
		LOGGER.info("Loading RIF records...");
		List<RifRecordLoadResult> resultsList = new ArrayList<>();
		for (Stream<RifRecordEvent<?>> rifRecordEvents : processor.process(rifFilesEvent)) {
			loader.process(rifRecordEvents, error -> {
				throw new RuntimeException(error);
			}, result -> {
				resultsList.add(result);
			});
		}
		LOGGER.info("Loaded RIF records.");

		// Verify that the expected number of records were run successfully.
		Assert.assertEquals(
				Arrays.stream(StaticRifResourceGroup.SAMPLE_A.getResources()).mapToInt(r -> r.getRecordCount()).sum(),
				resultsList.size());

		/*
		 * Run the extraction an extra time and verify that each record can now
		 * be found in the database.
		 */
		EntityManagerFactory entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory();
		for (Stream<RifRecordEvent<?>> rifRecordEventsCopy : processor.process(rifFilesEvent)) {
			rifRecordEventsCopy.forEach(r -> {
				if (r.getFile().getFileType() == RifFileType.BENEFICIARY)
					assertIsInDatabase(entityManagerFactory, (BeneficiaryRow) r.getRecord());
				// TODO other record types
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
	private static void assertIsInDatabase(EntityManagerFactory entityManagerFactory, BeneficiaryRow record) {
		// TODO Auto-generated method stub
	}
}
