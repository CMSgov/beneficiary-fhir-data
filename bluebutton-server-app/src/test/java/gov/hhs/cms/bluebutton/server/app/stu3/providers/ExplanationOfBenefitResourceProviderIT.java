package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Integration tests for {@link ExplanationOfBenefitResourceProvider}.
 */
public final class ExplanationOfBenefitResourceProviderIT {
	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link CarrierClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 */
	@Test
	public void readEobForExistingCarrierClaim() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		CarrierClaim claim = loadedRecords.stream().filter(r -> r instanceof CarrierClaim).map(r -> (CarrierClaim) r)
				.findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()));

		Assert.assertNotNull(eob);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link CarrierClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingCarrierClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.CARRIER, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
	 * works as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void searchForEobsByExistingPatient() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);

		/*
		 * Verify that each of the expected claims (one for every claim type) is
		 * present and looks correct.
		 */

		CarrierClaim carrierClaim = loadedRecords.stream().filter(r -> r instanceof CarrierClaim)
				.map(r -> (CarrierClaim) r).findFirst().get();
		ExplanationOfBenefit carrierClaimFromSearchResult = (ExplanationOfBenefit) searchResults.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit)
				.map(e -> (ExplanationOfBenefit) e.getResource()).findFirst().get();
		assertMatches(carrierClaim, carrierClaimFromSearchResult);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
	 * works as expected for a {@link Patient} that does not exist in the DB.
	 */
	@Test
	public void searchForEobsByMissingPatient() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return 0 matches.
		Bundle searchResults = fhirClient.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(new IdDt("Patient", "1234"))).returnBundle(Bundle.class)
				.execute();

		Assert.assertNotNull(searchResults);
		Assert.assertEquals(0, searchResults.getTotal());
	}

	/**
	 * Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called
	 * after each test case.
	 */
	@After
	public void cleanDatabaseServerAfterEachTestCase() {
		ServerTestUtils.cleanDatabaseServer();
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link CarrierClaim}.
	 * 
	 * @param claim
	 *            the {@link CarrierClaim} that the {@link ExplanationOfBenefit}
	 *            was generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link CarrierClaim}
	 */
	private static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob) {
		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()),
				eob.getIdElement().getIdPart());
	}
}
