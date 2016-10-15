package gov.hhs.cms.bluebutton.datapipeline.fhir;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Organization;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import ca.uhn.fhir.rest.client.IGenericClient;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirTestUtilities;

/**
 * <p>
 * Integration tests for {@link SharedDataManager}.
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
public class SharedDataManagerIT {
	/**
	 * Verifies that {@link SharedDataManager#upsertSharedData()} works as
	 * expected.
	 */
	@Test
	public void upsertSharedData() {
		// Create the FhirLoader to use.
		FhirLoader fhirLoader = new FhirLoader(new MetricRegistry(), FhirTestUtilities.getLoadOptions());

		// Run the shared data upsert.
		new SharedDataManager(fhirLoader).upsertSharedData();

		// Verify the results.
		IGenericClient fhirClient = FhirLoader.createFhirClient(FhirTestUtilities.getLoadOptions());
		Bundle results = fhirClient.search().forResource(Organization.class)
				.where(Organization.NAME.matches().value(SharedDataManager.COVERAGE_ISSUER)).returnBundle(Bundle.class)
				.execute();
		Assert.assertEquals(1, results.getEntry().size());
	}

	/**
	 * Ensures that {@link FhirTestUtilities#cleanFhirServer()} is called after
	 * each test case.
	 */
	@After
	public void cleanFhirServerAfterEachTestCase() {
		FhirTestUtilities.cleanFhirServer();
	}
}
