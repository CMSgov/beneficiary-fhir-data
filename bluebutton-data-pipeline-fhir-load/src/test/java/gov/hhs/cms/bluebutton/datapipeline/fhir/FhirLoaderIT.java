package gov.hhs.cms.bluebutton.datapipeline.fhir;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import ca.uhn.fhir.rest.client.IGenericClient;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirBundleResult;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifGenerator;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResource;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResourceGroup;

/**
 * <p>
 * Integration tests for {@link FhirLoader}.
 * </p>
 * <p>
 * These tests require a local FHIR server to be running. This is handled
 * automatically by the POM when run as part of a Maven build. To run these
 * tests in Eclipse, you can launch the server manually, as follows:
 * </p>
 * <ol>
 * <li>Right-click the <code>bluebutton-data-pipeline-fhir-load</code> project,
 * and select <strong>Run As > Maven build...</strong>.</li>
 * <li>Set <strong>goal</strong> to
 * <code>dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start</code>
 * .</li>
 * <li>Click <strong>Run</strong>.</li>
 * </ol>
 * <p>
 * When done with the server, you can stop it by running the
 * <code>org.codehaus.mojo:exec-maven-plugin:exec@server-stop</code> goal in a
 * similar fashion. Once it's been run the first time, the server can be
 * re-launched from Eclipse's <strong>Run</strong> toolbar dropdown button, just
 * like any other Java application, unit test, etc. Logs from the server can be
 * found in the project's <code>target/bbonfhir-server/wildfly-*</code>
 * directory.
 * </p>
 */
public final class FhirLoaderIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirLoaderIT.class);

	/**
	 * Verifies that the entire data pipeline works correctly: all the way from
	 * generating sample data through extracting, transform, and finally loading
	 * that data into a live FHIR server. Runs against
	 * {@link StaticRifResourceGroup#SAMPLE_A}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void loadRifDataSampleA() {
		// Generate the sample RIF data to feed through the pipeline.
		StaticRifResource[] rifResources = StaticRifResourceGroup.SAMPLE_A.getResources();
		StaticRifGenerator rifGenerator = new StaticRifGenerator(rifResources);
		Stream<RifFile> rifFilesStream = rifGenerator.generate();
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), rifFilesStream.collect(Collectors.toSet()));

		// Initialize the Extract phase of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifRecordEvents = processor.process(rifFilesEvent);

		// Grab a copy of that stream (for testing, below).
		List<RifRecordEvent<?>> rifRecordEventsCopy = rifRecordEvents.collect(Collectors.toList());
		RifRecordEvent<BeneficiaryRow> beneRecordEvent = (RifRecordEvent<BeneficiaryRow>) rifRecordEventsCopy.stream()
				.filter(e -> e.getRecord() instanceof BeneficiaryRow).findAny().get();
		RifRecordEvent<CarrierClaimGroup> carrierRecordEvent = (RifRecordEvent<CarrierClaimGroup>) rifRecordEventsCopy
				.stream().filter(e -> e.getRecord() instanceof CarrierClaimGroup).findAny().get();
		RifRecordEvent<PartDEventRow> pdeRecordEvent = (RifRecordEvent<PartDEventRow>) rifRecordEventsCopy.stream()
				.filter(e -> e.getRecord() instanceof PartDEventRow).findAny().get();
		rifRecordEvents = rifRecordEventsCopy.stream();

		// Initialize the Transform phase of the pipeline.
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> fhirInputBundles = transformer.transform(rifRecordEvents);

		// Setup the metrics for FhirLoader to use.
		MetricRegistry fhirMetrics = new MetricRegistry();
		fhirMetrics.registerAll(new MemoryUsageGaugeSet());
		fhirMetrics.registerAll(new GarbageCollectorMetricSet());
		Slf4jReporter fhirMetricsReporter = Slf4jReporter.forRegistry(fhirMetrics).outputTo(LOGGER).build();
		fhirMetricsReporter.start(300, TimeUnit.SECONDS);

		// Initialize the Load phase of the pipeline.
		FhirLoader loader = new FhirLoader(fhirMetrics, FhirTestUtilities.getLoadOptions());
		Stream<FhirBundleResult> resultsStream = loader.process(fhirInputBundles);

		/*
		 * Collect all of the results, which will actually start data processing
		 * through the pipeline (streams are lazy).
		 */
		List<FhirBundleResult> resultsList = resultsStream.collect(Collectors.toList());
		LOGGER.info("FHIR resources loaded.");
		fhirMetricsReporter.stop();
		fhirMetricsReporter.report();

		// Verify the results.
		Assert.assertNotNull(resultsList);
		Assert.assertEquals(rifResources.length, resultsList.size());
		assertResultIsLegit(resultsList);

		/*
		 * Run some spot-checks against the server, to verify that things look
		 * as expected.
		 */
		IGenericClient client = FhirTestUtilities.createFhirClient();
		Assert.assertEquals(1,
				client.search().forResource(Patient.class)
						.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());
		Assert.assertEquals(1, client.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId("Patient/bene-" + beneRecordEvent.getRecord().beneficiaryId))
				.and(ExplanationOfBenefit.IDENTIFIER.exactly().systemAndCode(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
						carrierRecordEvent.getRecord().claimId))
				.returnBundle(Bundle.class).execute().getTotal());
		Assert.assertEquals(1, client.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId("Patient/bene-" + beneRecordEvent.getRecord().beneficiaryId))
				.and(ExplanationOfBenefit.IDENTIFIER.exactly().systemAndCode(DataTransformer.CODING_SYSTEM_CCW_PDE_ID,
						pdeRecordEvent.getRecord().partDEventId))
				.returnBundle(Bundle.class).execute().getTotal());
	}

	/**
	 * Verifies that the entire data pipeline works correctly: all the way from
	 * generating sample data through extracting, transform, and finally loading
	 * that data into a live FHIR server. Runs against
	 * {@link StaticRifResourceGroup#SAMPLE_B}.
	 */
	@Test
	public void loadRifDataSampleB() {
		// Generate the sample RIF data to feed through the pipeline.
		StaticRifResource[] rifResources = StaticRifResourceGroup.SAMPLE_B.getResources();
		StaticRifGenerator rifGenerator = new StaticRifGenerator(rifResources);
		Stream<RifFile> rifFilesStream = rifGenerator.generate();
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), rifFilesStream.collect(Collectors.toSet()));

		// Initialize the Extract phase of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifRecordEvents = processor.process(rifFilesEvent);

		// Initialize the Transform phase of the pipeline.
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> fhirInputBundles = transformer.transform(rifRecordEvents);

		// Setup the metrics for FhirLoader to use.
		MetricRegistry fhirMetrics = new MetricRegistry();
		fhirMetrics.registerAll(new MemoryUsageGaugeSet());
		fhirMetrics.registerAll(new GarbageCollectorMetricSet());
		Slf4jReporter fhirMetricsReporter = Slf4jReporter.forRegistry(fhirMetrics).outputTo(LOGGER).build();
		fhirMetricsReporter.start(300, TimeUnit.SECONDS);

		// Initialize the Load phase of the pipeline.
		FhirLoader loader = new FhirLoader(fhirMetrics, FhirTestUtilities.getLoadOptions());
		Stream<FhirBundleResult> resultsStream = loader.process(fhirInputBundles);

		/*
		 * Collect all of the results, which will actually start data processing
		 * through the pipeline (streams are lazy).
		 */
		List<FhirBundleResult> resultsList = resultsStream.collect(Collectors.toList());
		LOGGER.info("FHIR resources loaded.");
		fhirMetricsReporter.stop();
		fhirMetricsReporter.report();

		// Verify the results.
		Assert.assertNotNull(resultsList);
		assertResultIsLegit(resultsList);

		/*
		 * Run some spot-checks against the server, to verify that things look
		 * as expected.
		 */
		IGenericClient client = FhirTestUtilities.createFhirClient();
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRecordCount(),
				client.search().forResource(Patient.class).returnBundle(Bundle.class).execute().getTotal());

		/*
		 * Note that we're not really testing much for correctness here. That's
		 * mostly covered in DataTransformerTest. Instead, the main goal here is
		 * to ensure that all of the FHIR resources we're pushing are accepted
		 * by the server as valid. Also: this particular test case benchmarks
		 * performance a bit, given the number of resources it's pushing.
		 */
	}

	/**
	 * Ensures that {@link FhirTestUtilities#cleanFhirServer()} is called after
	 * each test case.
	 */
	@After
	public void cleanFhirServerAfterEachTestCase() {
		FhirTestUtilities.cleanFhirServer();
	}

	/**
	 * Verifies that the specified {@link FhirBundleResult} has legit output,
	 * given its input. Basically, just make sure that it contains the expected
	 * number of responses and that those responses all have <code>2XX</code>
	 * HTTP status codes.
	 * 
	 * @param resultsList
	 *            the {@link FhirBundleResult}s to verify
	 */
	private static void assertResultIsLegit(List<FhirBundleResult> resultsList) {
		/*
		 * Verify each response (one per bundle that was sent, so one per
		 * bene/claim/event).
		 */
		for (FhirBundleResult bundleResult : resultsList) {
			Assert.assertEquals(bundleResult.getInputBundle().getResult().getEntry().size(),
					bundleResult.getOutputBundle().getEntry().size());

			// Verify each entry (one per resource that was in the bundle).
			for (BundleEntryComponent resultEntry : bundleResult.getOutputBundle().getEntry()) {
				Assert.assertNotNull(resultEntry.getResponse());
				Assert.assertTrue("Invalid status: " + resultEntry.getResponse().getStatus(),
						resultEntry.getResponse().getStatus().matches("^2\\d\\d .*$"));
			}
		}
	}
}
