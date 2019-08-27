package gov.hhs.cms.bluebutton.tests;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Patient;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.EncodingEnum;
import gov.hhs.cms.bluebutton.tests.FhirClientHelper.ApiUser;

/**
 * Integration tests for the BlueButton frontend's open/unsecured API, which was
 * provided for the April, 2016 Codeathon.
 */
public final class ApiUnsecuredIT {
	/**
	 * Verifies that a {@link Patient} can be retrieved.
	 */
	@Test
	public void retrievePatient() {
		ApiUser apiUser = FhirClientHelper.getApiUsers().get(0);
		IGenericClient fhirClient = FhirClientHelper.createFhirClient(FhirClientHelper.FHIR_URL_API_OPEN);
		fhirClient.setEncoding(EncodingEnum.JSON);

		Patient patient = fhirClient.read(Patient.class, apiUser.getPatientId());
		Assert.assertNotNull(patient);
		Assert.assertEquals(apiUser.getPatientId(), patient.getId());
	}

	/**
	 * Verifies that a search for all {@link Patient}s works as expected.
	 */
	@Test
	public void searchPatients() {
		IGenericClient fhirClient = FhirClientHelper.createFhirClient(FhirClientHelper.FHIR_URL_API_OPEN);
		fhirClient.setEncoding(EncodingEnum.JSON);

		Bundle searchResult = fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute();
		Assert.assertNotNull(searchResult);
		Assert.assertEquals(1, searchResult.getEntry().size());
	}
}
