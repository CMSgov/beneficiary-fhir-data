package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Integration tests for {@link PatientResourceProvider}.
 */
public final class PatientResourceProviderIT {
	/**
	 * Verifies that
	 * {@link PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void readExistingPatient() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Patient patient = fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

		Assert.assertNotNull(patient);
		BeneficiaryTransformerTest.assertMatches(beneficiary, patient);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} works
	 * as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void readExistingPatientIncludeIdentifiersFalse() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();
		ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
		extraParamsInterceptor.setIncludeIdentifiers("false");
		fhirClient.registerInterceptor(extraParamsInterceptor);

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Patient patient = fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

		Assert.assertNotNull(patient);
		BeneficiaryTransformerTest.assertMatches(beneficiary, patient);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} works
	 * as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void readExistingPatientIncludeIdentifiersTrue() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();
		ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
		extraParamsInterceptor.setIncludeIdentifiers("true");
		fhirClient.registerInterceptor(extraParamsInterceptor);

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Patient patient = fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

		Assert.assertNotNull(patient);
		BeneficiaryTransformerTest.assertMatches(beneficiary, patient);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
	 * works as expected for a {@link Patient} that does not exist in the DB.
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void readMissingPatient() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return nothing.
		fhirClient.read().resource(Patient.class).withId("1234").execute();
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void searchForExistingPatientByLogicalId() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);

		/*
		 * Verify that no paging links exist within the bundle.
		 */
		Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
		Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
		Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
		Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

		Assert.assertEquals(1, searchResults.getTotal());
		Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
		BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB, including
	 * identifiers to return the unhashed HICN and MBI.
	 */
	@Test
	public void searchForExistingPatientByLogicalIdIncludeIdentifiers() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();
		ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
		extraParamsInterceptor.setIncludeIdentifiers("true");
		fhirClient.registerInterceptor(extraParamsInterceptor);

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);
		Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

		/*
		 * Ensure the unhashed values for HICN and MBI are present.
		 */
		Boolean hicnUnhashedPresent = false;
		Boolean mbiUnhashedPresent = false;
		Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
		while (identifiers.hasNext()) {
			Identifier identifier = identifiers.next();
			if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
				hicnUnhashedPresent = true;
			if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
				mbiUnhashedPresent = true;
		}

		Assert.assertTrue(hicnUnhashedPresent);
		Assert.assertTrue(mbiUnhashedPresent);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB, with
	 * paging.
	 */
	@Test
	public void searchForPatientByLogicalIdWithPaging() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId())).count(1)
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);
		Assert.assertEquals(1, searchResults.getTotal());

		/*
		 * Verify that only the first and last paging links exist, since there should
		 * only be one page.
		 */
		Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
		Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
		Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
		Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does not exist in the DB.
	 */
	@Test
	public void searchForMissingPatientByLogicalId() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return 0 matches.
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, "foo")).returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);
		Assert.assertEquals(0, searchResults.getTotal());
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB.
	 */
	@Test
	public void searchForExistingPatientByHicnHash() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly().systemAndIdentifier(
						TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);

		/*
		 * Verify that no paging links exist within the bundle.
		 */
		Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
		Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
		Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
		Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

		Assert.assertEquals(1, searchResults.getTotal());
		Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
		BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB, including
	 * identifiers to return the unhashed HICN and MBI.
	 */
	@Test
	public void searchForExistingPatientByHicnHashIncludeIdentifiers() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();
		ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
		extraParamsInterceptor.setIncludeIdentifiers("true");
		fhirClient.registerInterceptor(extraParamsInterceptor);

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
				.returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);
		Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

		/*
		 * Ensure the unhashed values for HICN and MBI are present.
		 */
		Boolean hicnUnhashedPresent = false;
		Boolean mbiUnhashedPresent = false;
		Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
		while (identifiers.hasNext()) {
			Identifier identifier = identifiers.next();
			if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
				hicnUnhashedPresent = true;
			if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
				mbiUnhashedPresent = true;
		}

		Assert.assertTrue(hicnUnhashedPresent);
		Assert.assertTrue(mbiUnhashedPresent);
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does exist in the DB, with
	 * paging.
	 */
	@Test
	public void searchForExistingPatientByHicnHashWithPaging() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
				.count(1).returnBundle(Bundle.class).execute();

		Assert.assertNotNull(searchResults);
		Assert.assertEquals(1, searchResults.getTotal());
		Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
		BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);

		/*
		 * Verify that only the first and last paging links exist, since there should
		 * only be one page.
		 */
		Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
		Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
		Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
		Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for HICNs that should be present as a
	 * {@link BeneficiaryHistory} record.
	 */
	@Test
	public void searchForExistingPatientByHistoricalHicnHash() {
		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		loadedRecords.stream().filter(r -> r instanceof BeneficiaryHistory).map(r -> (BeneficiaryHistory) r)
				.forEach(h -> {
					Bundle searchResults = fhirClient.search().forResource(Patient.class)
							.where(Patient.IDENTIFIER.exactly()
									.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, h.getHicn()))
							.returnBundle(Bundle.class).execute();

					Assert.assertNotNull(searchResults);
					Assert.assertEquals(1, searchResults.getTotal());
					Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
					Assert.assertEquals(h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
				});
	}

	/**
	 * Verifies that
	 * {@link PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
	 * works as expected for a {@link Patient} that does not exist in the DB.
	 */
	@Test
	public void searchForMissingPatientByHicnHash() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();

		// No data is loaded, so this should return 0 matches.
		Bundle searchResults = fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "1234"))
				.returnBundle(Bundle.class).execute();

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
}
