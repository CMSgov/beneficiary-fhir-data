package gov.hhs.cms.bluebutton.fhirstress.utils;

import org.hl7.fhir.dstu3.model.Patient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple FhirClient.
 */
public class FhirClientIT extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public FhirClientIT(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(FhirClientIT.class);
	}

	/**
	 * Test FHIR server connectivity
	 */
	public void testFhirClient() {
		IGenericClient client = FhirClient.create(
				"https://internal-tsbb10lb01-758855236.us-east-1.elb.amazonaws.com/v1/fhir", "./dev/ssl-stores");
		Patient patient = client.read().resource(Patient.class).withId("567834").execute();
		assertEquals("Doe", patient.getName().get(0).getFamily());
	}
}
