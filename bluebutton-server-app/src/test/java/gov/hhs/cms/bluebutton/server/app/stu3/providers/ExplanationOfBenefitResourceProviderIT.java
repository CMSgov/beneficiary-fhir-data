package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
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
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingCarrierClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		CarrierClaim claim = loadedRecords.stream().filter(r -> r instanceof CarrierClaim).map(r -> (CarrierClaim) r)
				.findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		CarrierClaimTransformerTest.assertMatches(claim, eob);
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
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link DMEClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingDMEClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		DMEClaim claim = loadedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> (DMEClaim) r).findFirst()
				.get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.DME, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		DMEClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link DMEClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingDMEClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.DME, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link HHAClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingHHAClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		HHAClaim claim = loadedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> (HHAClaim) r).findFirst()
				.get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.HHA, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		HHAClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link HHAClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingHHAClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.HHA, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link HospiceClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingHospiceClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		HospiceClaim claim = loadedRecords.stream().filter(r -> r instanceof HospiceClaim).map(r -> (HospiceClaim) r)
				.findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.HOSPICE, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		HospiceClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link HospiceClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingHospiceClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.HOSPICE, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link InpatientClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingInpatientClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		InpatientClaim claim = loadedRecords.stream().filter(r -> r instanceof InpatientClaim)
				.map(r -> (InpatientClaim) r).findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.INPATIENT, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		InpatientClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link InpatientClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingInpatientClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.INPATIENT, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link OutpatientClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingOutpatientClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		OutpatientClaim claim = loadedRecords.stream().filter(r -> r instanceof OutpatientClaim)
				.map(r -> (OutpatientClaim) r).findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.OUTPATIENT, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		OutpatientClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link OutpatientClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingOutpatientClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.OUTPATIENT, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link PartDEvent}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingPartDEvent() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		PartDEvent claim = loadedRecords.stream().filter(r -> r instanceof PartDEvent).map(r -> (PartDEvent) r)
				.findFirst().get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.PDE, claim.getEventId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		PartDEventTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link PartDEvent}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingPartDEvent() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.PDE, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link SNFClaim}-derived
	 * {@link ExplanationOfBenefit} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void readEobForExistingSNFClaim() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		SNFClaim claim = loadedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> (SNFClaim) r).findFirst()
				.get();
		ExplanationOfBenefit eob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.SNF, claim.getClaimId()));

		Assert.assertNotNull(eob);
		Assert.assertNotNull(eob.getMeta().getLastUpdated());
		SNFClaimTransformerTest.assertMatches(claim, eob);
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for an {@link SNFClaim}-derived
	 * {@link ExplanationOfBenefit} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readEobForMissingSNFClaim() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.SNF, "1234"));
	}

	/**
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
	 * works as expected for a {@link Patient} that does exist in the DB.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void searchForEobsByExistingPatient() throws FHIRException {
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
		Assert.assertEquals(1, filterToClaimType(searchResults, ClaimType.CARRIER).size());
		CarrierClaimTransformerTest.assertMatches(carrierClaim,
				filterToClaimType(searchResults, ClaimType.CARRIER).get(0));

		DMEClaim dmeClaim = loadedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> (DMEClaim) r).findFirst()
				.get();
		DMEClaimTransformerTest.assertMatches(dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0));

		HHAClaim hhaClaim = loadedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> (HHAClaim) r).findFirst()
				.get();
		HHAClaimTransformerTest.assertMatches(hhaClaim, filterToClaimType(searchResults, ClaimType.HHA).get(0));

		HospiceClaim hospiceClaim = loadedRecords.stream().filter(r -> r instanceof HospiceClaim)
				.map(r -> (HospiceClaim) r).findFirst().get();
		HospiceClaimTransformerTest.assertMatches(hospiceClaim,
				filterToClaimType(searchResults, ClaimType.HOSPICE).get(0));

		InpatientClaim inpatientClaim = loadedRecords.stream().filter(r -> r instanceof InpatientClaim)
				.map(r -> (InpatientClaim) r).findFirst().get();
		InpatientClaimTransformerTest.assertMatches(inpatientClaim,
				filterToClaimType(searchResults, ClaimType.INPATIENT).get(0));

		OutpatientClaim outpatientClaim = loadedRecords.stream().filter(r -> r instanceof OutpatientClaim)
				.map(r -> (OutpatientClaim) r).findFirst().get();
		OutpatientClaimTransformerTest.assertMatches(outpatientClaim,
				filterToClaimType(searchResults, ClaimType.OUTPATIENT).get(0));

		PartDEvent partDEvent = loadedRecords.stream().filter(r -> r instanceof PartDEvent).map(r -> (PartDEvent) r)
				.findFirst().get();
		PartDEventTransformerTest.assertMatches(partDEvent, filterToClaimType(searchResults, ClaimType.PDE).get(0));

		SNFClaim snfClaim = loadedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> (SNFClaim) r).findFirst()
				.get();
		SNFClaimTransformerTest.assertMatches(snfClaim, filterToClaimType(searchResults, ClaimType.SNF).get(0));
	}

	/**
	 * <p>
	 * Verifies that
	 * {@link ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
	 * doesn't return duplicate results.
	 * </p>
	 * <p>
	 * This is a regression test case for TODO.
	 * </p>
	 *
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	// @Test
	public void searchForEobsHasNoDupes() throws FHIRException {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_B.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r).forEach(beneficiary -> {
			Bundle searchResults = fhirClient.search().forResource(ExplanationOfBenefit.class)
					.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
					.returnBundle(Bundle.class).execute();
			Assert.assertNotNull(searchResults);

			/*
			 * Verify that the returned Bundle doesn't have any resources with duplicate
			 * IDs.
			 */
			Set<String> claimIds = new HashSet<>();
			for (BundleEntryComponent searchResultEntry : searchResults.getEntry()) {
				String resourceId = searchResultEntry.getResource().getId();
				if (claimIds.contains(resourceId))
					Assert.assertFalse(claimIds.contains(resourceId));
				claimIds.add(resourceId);
			}
			if (searchResults.getTotal() > 0)
				Assert.assertFalse(claimIds.isEmpty());
		});

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
	 * @param bundle
	 *            the {@link Bundle} to filter
	 * @param claimType
	 *            the {@link ClaimType} to use as a filter
	 * @return a filtered {@link List} of the {@link ExplanationOfBenefit}s from the
	 *         specified {@link Bundle} that match the specified {@link ClaimType}
	 */
	private static List<ExplanationOfBenefit> filterToClaimType(Bundle bundle, ClaimType claimType) {
		List<ExplanationOfBenefit> carrierClaimResults = bundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit)
				.map(e -> (ExplanationOfBenefit) e.getResource()).filter(e -> {
					return TransformerTestUtils.isCodeInConcept(e.getType(),
							TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, claimType.name());
				}).collect(Collectors.toList());
		return carrierClaimResults;
	}
}
