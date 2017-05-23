package gov.hhs.cms.bluebutton.datapipeline.fhir;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.rest.client.IGenericClient;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirBundleResult;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;
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
 * found in the project's <code>target/bluebutton-server/wildfly-*</code>
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
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), StaticRifResourceGroup.SAMPLE_A.toRifFiles());

		// Setup the metrics that we'll log to.
		MetricRegistry fhirMetrics = new MetricRegistry();

		// Create the processors that will handle each stage of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		FhirLoader loader = new FhirLoader(fhirMetrics, FhirTestUtilities.getLoadOptions());

		// Create/update the shared data that FhirLoader will require.
		new SharedDataManager(loader).upsertSharedData();

		/*
		 * Run the extraction an extra time upfront to grab some data for the
		 * assertions below.
		 */
		List<Stream<RifRecordEvent<?>>> rifRecordEventsCopy = processor.process(rifFilesEvent);
		List<RifRecordEvent<?>> rifRecordEventsCopyFlat = rifRecordEventsCopy.stream()
				.flatMap(s -> s.collect(Collectors.toList()).stream()).collect(Collectors.toList());
		RifRecordEvent<BeneficiaryRow> beneRecordEvent = (RifRecordEvent<BeneficiaryRow>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof BeneficiaryRow).findAny().get();
		RifRecordEvent<CarrierClaimGroup> carrierRecordEvent = (RifRecordEvent<CarrierClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof CarrierClaimGroup).findAny().get();
		RifRecordEvent<InpatientClaimGroup> inpatientRecordEvent = (RifRecordEvent<InpatientClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof InpatientClaimGroup).findAny().get();
		RifRecordEvent<OutpatientClaimGroup> outpatientRecordEvent = (RifRecordEvent<OutpatientClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof OutpatientClaimGroup).findAny().get();
		RifRecordEvent<PartDEventRow> pdeRecordEvent = (RifRecordEvent<PartDEventRow>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof PartDEventRow).findAny().get();
		RifRecordEvent<HHAClaimGroup> hhaRecordEvent = (RifRecordEvent<HHAClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof HHAClaimGroup).findAny().get();
		RifRecordEvent<HospiceClaimGroup> hospiceRecordEvent = (RifRecordEvent<HospiceClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof HospiceClaimGroup).findAny().get();
		RifRecordEvent<SNFClaimGroup> snfRecordEvent = (RifRecordEvent<SNFClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof SNFClaimGroup).findAny().get();
		RifRecordEvent<DMEClaimGroup> dmeRecordEvent = (RifRecordEvent<DMEClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof DMEClaimGroup).findAny().get();

		loadData(rifFilesEvent, loader, processor);

		/*
		 * Run some spot-checks against the server, to verify that things look
		 * as expected.
		 */
		IGenericClient client = FhirTestUtilities.createFhirClient();
		Assert.assertEquals(1,
				client.search().forResource(Patient.class)
						.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_PDE_ID,
				pdeRecordEvent.getRecord().partDEventId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				carrierRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				inpatientRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				outpatientRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				hospiceRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				hhaRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				snfRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				dmeRecordEvent.getRecord().claimId);
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
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), StaticRifResourceGroup.SAMPLE_B.toRifFiles());

		// Setup the metrics that we'll log to.
		MetricRegistry fhirMetrics = new MetricRegistry();
		Slf4jReporter fhirMetricsReporter = Slf4jReporter.forRegistry(fhirMetrics).outputTo(LOGGER).build();

		// Create the processors that will handle each stage of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		DataTransformer transformer = new DataTransformer();
		FhirLoader loader = new FhirLoader(fhirMetrics, FhirTestUtilities.getLoadOptions());

		// Create/update the shared data that FhirLoader will require.
		new SharedDataManager(loader).upsertSharedData();

		// Link up the pipeline and run it.
		Timer.Context timerDataSet = fhirMetrics.timer(MetricRegistry.name(FhirLoaderIT.class, "dataSet", "processed"))
				.time();
		List<FhirBundleResult> resultsList = new ArrayList<>();
		for (Stream<RifRecordEvent<?>> rifRecordEvents : processor.process(rifFilesEvent)) {
			Stream<TransformedBundle> fhirInputBundles = transformer.transform(rifRecordEvents);
			loader.process(fhirInputBundles, error -> {
				throw new RuntimeException(error);
			}, result -> {
				resultsList.add(result);
			});
		}
		timerDataSet.stop();

		LOGGER.info("FHIR resources loaded.");
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

	@SuppressWarnings("unchecked")
	@Test
	public void idempotentLoadRifSampleA() {
		// Generate the sample RIF data to feed through the pipeline.
		RifFilesEvent rifFilesEvent = new RifFilesEvent(Instant.now(), StaticRifResourceGroup.SAMPLE_A.toRifFiles());

		// Setup the metrics that we'll log to.
		MetricRegistry fhirMetrics = new MetricRegistry();

		// Create the processors that will handle each stage of the pipeline.
		RifFilesProcessor processor = new RifFilesProcessor();
		FhirLoader loader = new FhirLoader(fhirMetrics, FhirTestUtilities.getLoadOptions());

		// Create/update the shared data that FhirLoader will require.
		new SharedDataManager(loader).upsertSharedData();

		/*
		 * Run the extraction an extra time upfront to grab some data for the
		 * assertions below.
		 */
		List<Stream<RifRecordEvent<?>>> rifRecordEventsCopy = processor.process(rifFilesEvent);
		List<RifRecordEvent<?>> rifRecordEventsCopyFlat = rifRecordEventsCopy.stream()
				.flatMap(s -> s.collect(Collectors.toList()).stream()).collect(Collectors.toList());
		RifRecordEvent<BeneficiaryRow> beneRecordEvent = (RifRecordEvent<BeneficiaryRow>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof BeneficiaryRow).findAny().get();
		RifRecordEvent<CarrierClaimGroup> carrierRecordEvent = (RifRecordEvent<CarrierClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof CarrierClaimGroup).findAny().get();
		RifRecordEvent<InpatientClaimGroup> inpatientRecordEvent = (RifRecordEvent<InpatientClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof InpatientClaimGroup).findAny().get();
		RifRecordEvent<OutpatientClaimGroup> outpatientRecordEvent = (RifRecordEvent<OutpatientClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof OutpatientClaimGroup).findAny().get();
		RifRecordEvent<PartDEventRow> pdeRecordEvent = (RifRecordEvent<PartDEventRow>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof PartDEventRow).findAny().get();
		RifRecordEvent<HHAClaimGroup> hhaRecordEvent = (RifRecordEvent<HHAClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof HHAClaimGroup).findAny().get();
		RifRecordEvent<HospiceClaimGroup> hospiceRecordEvent = (RifRecordEvent<HospiceClaimGroup>) rifRecordEventsCopyFlat
				.stream().filter(e -> e.getRecord() instanceof HospiceClaimGroup).findAny().get();
		RifRecordEvent<SNFClaimGroup> snfRecordEvent = (RifRecordEvent<SNFClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof SNFClaimGroup).findAny().get();
		RifRecordEvent<DMEClaimGroup> dmeRecordEvent = (RifRecordEvent<DMEClaimGroup>) rifRecordEventsCopyFlat.stream()
				.filter(e -> e.getRecord() instanceof DMEClaimGroup).findAny().get();

		// load all claim types to FHIR database
		loadData(rifFilesEvent, loader, processor);

		IGenericClient client = FhirTestUtilities.createFhirClient();
		Assert.assertEquals(1,
				client.search().forResource(Patient.class)
						.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());

		// check version number on Patient resource
		Bundle patientBundle = client.search().forResource(Patient.class)
				.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
				.returnBundle(Bundle.class).execute();
		BundleEntryComponent beneEntry = patientBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Patient).findAny().get();
		Patient beneA = (Patient) beneEntry.getResource();
		LOGGER.info("Patient1 version #- " + beneA.getMeta().getVersionId());

		// load same claim types to FHIR database again
		loadData(rifFilesEvent, loader, processor);

		// Ensure only one Patient resource with the given beneficiaryId is
		// present in database
		Assert.assertEquals(1,
				client.search().forResource(Patient.class)
						.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());

		patientBundle = client.search().forResource(Patient.class)
				.where(Patient.RES_ID.matches().value("bene-" + beneRecordEvent.getRecord().beneficiaryId))
				.returnBundle(Bundle.class).execute();
		beneEntry = patientBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Patient).findAny().get();

		Patient beneB = (Patient) beneEntry.getResource();
		LOGGER.info("Patient2 version #- " + beneB.getMeta().getVersionId());

		Assert.assertNotEquals(beneA.getMeta().getVersionId(), beneB.getMeta().getVersionId());

		// Ensure only one Coverage resource for each coverage type (i.e. Part
		// A, B, D) with the
		// given beneficiaryId is present in database
		Assert.assertEquals(1,
				client.search().forResource(Coverage.class)
						.where(Coverage.RES_ID.matches().value("partA-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());
		Assert.assertEquals(1,
				client.search().forResource(Coverage.class)
						.where(Coverage.RES_ID.matches().value("partB-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());
		Assert.assertEquals(1,
				client.search().forResource(Coverage.class)
						.where(Coverage.RES_ID.matches().value("partD-" + beneRecordEvent.getRecord().beneficiaryId))
						.returnBundle(Bundle.class).execute().getTotal());

		// Ensure only one ExplanationOfBenefit resource with the given
		// beneficiaryId and claimId is present in database for each claim type
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_PDE_ID,
				pdeRecordEvent.getRecord().partDEventId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				carrierRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				inpatientRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				outpatientRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				hospiceRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				hhaRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				snfRecordEvent.getRecord().claimId);
		assertEOBEquals(client, beneRecordEvent.getRecord().beneficiaryId, DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID,
				dmeRecordEvent.getRecord().claimId);
	}

	/**
	 * Load data files to FHIR database *
	 * 
	 * @param rifFilesEvent
	 *            {@link RifFilesEvent)}
	 * @param loader
	 *            {@link FhirLoader)}
	 * 
	 * @param processor
	 *            * {@link RifFilesProcessor)}
	 * 
	 */
	public void loadData(RifFilesEvent rifFilesEvent, FhirLoader loader, RifFilesProcessor processor) {
		DataTransformer transformer = new DataTransformer();
		List<FhirBundleResult> resultsList = new ArrayList<>();
		for (Stream<RifRecordEvent<?>> rifRecordEvents : processor.process(rifFilesEvent)) {
			Stream<TransformedBundle> fhirInputBundles = transformer.transform(rifRecordEvents);
			loader.process(fhirInputBundles, error -> {
				throw new RuntimeException(error);
			}, result -> {
				resultsList.add(result);
			});
		}

		LOGGER.info("FHIR resources loaded.");
		// Verify the results.
		Assert.assertNotNull(resultsList);
		Assert.assertEquals(rifFilesEvent.getFiles().size(), resultsList.size());
		assertResultIsLegit(resultsList);

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

	/**
	 * Verifies that only one EOB resource exists for each claim type
	 * 
	 * @param client
	 *            {@link IGenericClient}
	 * @param beneficiaryId
	 * 
	 * @param claimType
	 * 
	 * @param claimId
	 *
	 */
	private static void assertEOBEquals(IGenericClient client, String beneficiaryId, String claimType, String claimId) {
		Assert.assertEquals(1,
				client.search().forResource(ExplanationOfBenefit.class)
						.where(ExplanationOfBenefit.PATIENT.hasId("Patient/bene-" + beneficiaryId))
						.and(ExplanationOfBenefit.IDENTIFIER.exactly()
								.systemAndCode(claimType, claimId))
						.returnBundle(Bundle.class).execute().getTotal());
	}
}
