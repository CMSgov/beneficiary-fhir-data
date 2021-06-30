package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.BeneficiaryMonthly_;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/** Integration tests for {@link gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider}. */
public final class PatientResourceProviderIT {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void readExistingPatient() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patient, getRHwithIncldAddrFldHdr("false"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "true".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersTrue() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "true",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "hicn,mbi".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersHicnMbi() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "hicn".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersHicn() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "mbi".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersMbi() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "false".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersFalse() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "false",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersBlank() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * "invalid-identifier-value" and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid1() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "invalid-identifier-value",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * ["mbi,invalid-identifier-value"] and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid2() {
    assertExistingPatientIncludeIdentifiersExpected(
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_HICN,
        PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "mbi,invalid-identifier-value",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Asserts that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * contains expected/present identifiers for a {@link Patient}.
   *
   * @param includeIdentifiersValue header value
   * @param expectingHicn true if expecting a HICN
   * @param expectingMbi true if expecting a MBI
   * @param includeAddressValues header value
   */
  public void assertExistingPatientIncludeIdentifiersExpected(
      boolean expectingHicn, boolean expectingMbi, RequestHeaders requestHeader) {

    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient(requestHeader);

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(beneficiary, patient, requestHeader);

    /*
     * Ensure the unhashed values for HICN and MBI are present.
     */
    Boolean hicnUnhashedPresent = false;
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patient.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
        hicnUnhashedPresent = true;
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    if (expectingHicn) Assert.assertTrue(hicnUnhashedPresent);
    else Assert.assertFalse(hicnUnhashedPresent);

    if (expectingMbi) Assert.assertTrue(mbiUnhashedPresent);
    else Assert.assertFalse(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} that does exist in the DB but has no {@link
   * BeneficiaryHistory} or {@link MedicareBeneficiaryIdHistory} records when include identifiers
   * value = ["true"].
   */
  @Test
  public void readExistingPatientWithNoHistoryIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "true",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true");
    IGenericClient fhirClient = createFhirClient(requestHeader);

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(beneficiary, patient, requestHeader);

    /*
     * Ensure the unhashed values for HICN and MBI are present.
     */
    Boolean hicnUnhashedPresent = false;
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patient.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
        hicnUnhashedPresent = true;
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertTrue(hicnUnhashedPresent);
    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readMissingPatient() {
    IGenericClient fhirClient = createFhirClient();
    // No data is loaded, so this should return nothing.
    fhirClient.read().resource(Patient.class).withId("1234").execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByLogicalId() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
            .returnBundle(Bundle.class)
            .execute();

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
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByLogicalIdIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("true", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
            .returnBundle(Bundle.class)
            .execute();

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
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertTrue(hicnUnhashedPresent);
    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByLogicalIdIncludeIdentifiersFalse() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("false", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    /*
     * Ensure the unhashed values for HICN and MBI are *not* present.
     */
    Boolean hicnUnhashedPresent = false;
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
        hicnUnhashedPresent = true;
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertFalse(hicnUnhashedPresent);
    Assert.assertFalse(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with paging.
   */
  @Test
  public void searchForPatientByLogicalIdWithPaging() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
            .count(1)
            .returnBundle(Bundle.class)
            .execute();

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForMissingPatientByLogicalId() {
    IGenericClient fhirClient = createFhirClient();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(Patient.RES_ID.exactly().systemAndIdentifier(null, "foo"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByHicnHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
            .returnBundle(Bundle.class)
            .execute();

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
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByHicnHashIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("true", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
            .returnBundle(Bundle.class)
            .execute();

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
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertTrue(hicnUnhashedPresent);
    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that the correct bene id is returned when a hicn points to more than one bene id in
   * either the Beneficiaries and/or BeneficiariesHistory table.
   */
  @Test
  public void searchForExistingPatientByHicnHashWithBeneDups() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records for
    // testing
    loadedRecords.addAll(
        ServerTestUtils.loadData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    IGenericClient fhirClient = createFhirClient();

    Stream<Beneficiary> beneficiariesStream =
        loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r);
    List<Beneficiary> beneficiariesList = beneficiariesStream.collect(Collectors.toList());

    Stream<BeneficiaryHistory> beneficiariesHistoryStream =
        loadedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r);
    List<BeneficiaryHistory> beneficiariesHistoryList =
        beneficiariesHistoryStream.collect(Collectors.toList());

    boolean useHicnFromBeneficiaryTable;
    boolean expectsSingleBeneMatch;

    /*
     * The following scenario tests when the same hicn is in the
     * Beneficiaries table but points to different bene ids.
     *
     */
    useHicnFromBeneficiaryTable = true;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "567834",
        "543217066U",
        useHicnFromBeneficiaryTable,
        "hicn",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when only one hicn is in the Beneficiaries table
     */
    useHicnFromBeneficiaryTable = true;
    expectsSingleBeneMatch = true;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "123456NULLREFYR",
        "543217066N",
        useHicnFromBeneficiaryTable,
        "hicn",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the same hicn is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The bene id
     * is different between the tables
     */
    useHicnFromBeneficiaryTable = true;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "BENE1234",
        "SAMEHICN",
        useHicnFromBeneficiaryTable,
        "hicn",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the requested hicn is only in the
     * BeneficiariesHistory table. Use the bene id from the
     * BeneficiariesHistory table to then read the Beneficiaries table.
     *
     */
    useHicnFromBeneficiaryTable = false;
    expectsSingleBeneMatch = true;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "55555",
        "HISTHICN",
        useHicnFromBeneficiaryTable,
        "hicn",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the requested hicn is only in the
     * BeneficiariesHistory table but this hicn points to more than one bene
     * id in history.
     */
    useHicnFromBeneficiaryTable = false;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "66666",
        "DUPHISTHIC",
        useHicnFromBeneficiaryTable,
        "hicn",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when a hicn is not found in the
     * Beneficiaries and BeneficiariesHistory table.
     *
     */
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "notfoundhicn"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByHicnHashIncludeIdentifiersFalse() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("false", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    /*
     * Ensure the unhashed values for HICN and MBI are *not* present.
     */
    Boolean hicnUnhashedPresent = false;
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
        hicnUnhashedPresent = true;
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertFalse(hicnUnhashedPresent);
    Assert.assertFalse(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with paging.
   */
  @Test
  public void searchForExistingPatientByHicnHashWithPaging() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
            .count(1)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getTotal());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for HICNs that should be present as a {@link BeneficiaryHistory} record.
   */
  @Test
  public void searchForExistingPatientByHistoricalHicnHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    loadedRecords.stream()
        .filter(r -> r instanceof BeneficiaryHistory)
        .map(r -> (BeneficiaryHistory) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, h.getHicn()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for HICNs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientWithNoHistory() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClient();

    loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, h.getHicn()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for HICNs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientWithNoHistoryIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClient("true", "true");

    loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, h.getHicn()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForMissingPatientByHicnHash() {
    IGenericClient fhirClient = createFhirClient();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "1234"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   *
   * <p>works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                        beneficiary.getMbiHash().get()))
            .returnBundle(Bundle.class)
            .execute();

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
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));

    String mbiHashIdentifier =
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                identifier ->
                    identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH))
            .findFirst()
            .get()
            .getValue();
    Assert.assertEquals(
        "mbiHash identifier exists", beneficiary.getMbiHash().get(), mbiHashIdentifier);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByMbiHashIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("true", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                        beneficiary.getMbiHash().get()))
            .returnBundle(Bundle.class)
            .execute();

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
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertTrue(hicnUnhashedPresent);
    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that the correct bene id or exception is returned when an MBI points to more than one
   * bene id in either the Beneficiaries and/or BeneficiariesHistory table.
   */
  @Test
  public void searchForExistingPatientByMbiHashWithBeneDups() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records for
    // testing
    loadedRecords.addAll(
        ServerTestUtils.loadData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    IGenericClient fhirClient = createFhirClient();

    Stream<Beneficiary> beneficiariesStream =
        loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r);
    List<Beneficiary> beneficiariesList = beneficiariesStream.collect(Collectors.toList());

    Stream<BeneficiaryHistory> beneficiariesHistoryStream =
        loadedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r);
    List<BeneficiaryHistory> beneficiariesHistoryList =
        beneficiariesHistoryStream.collect(Collectors.toList());

    boolean useMbiFromBeneficiaryTable;
    boolean expectsSingleBeneMatch;

    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries table but points to different bene ids.
     */
    useMbiFromBeneficiaryTable = true;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "567834",
        "3456789",
        useMbiFromBeneficiaryTable,
        "mbi",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when only one mbi is in the
     * Beneficiaries table.
     */
    useMbiFromBeneficiaryTable = true;
    expectsSingleBeneMatch = true;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "123456NULLREFYR",
        "3456789N",
        useMbiFromBeneficiaryTable,
        "mbi",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The bene id
     * is different between the tables so the bene record from the
     * Beneficiaries table should be used.
     *
     * bene id=BENE1234 mbi=SAMEMBI rfrnc_yr=2019 should be pulled back.
     */
    useMbiFromBeneficiaryTable = true;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "BENE1234",
        "SAMEMBI",
        useMbiFromBeneficiaryTable,
        "mbi",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the requested mbi is only in the
     * BeneficiariesHistory table. Use the bene id from the
     * BeneficiariesHistory table to then read the Beneficiaries table.
     */
    useMbiFromBeneficiaryTable = false;
    expectsSingleBeneMatch = true;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "55555",
        "HISTMBI",
        useMbiFromBeneficiaryTable,
        "mbi",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when the requested mbi is only in the
     * BeneficiariesHistory table but this mbi points to more than one bene
     * id in history.
     */
    useMbiFromBeneficiaryTable = false;
    expectsSingleBeneMatch = false;
    assertPatientByHashTypeMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "66666",
        "DUPHISTMBI",
        useMbiFromBeneficiaryTable,
        "mbi",
        expectsSingleBeneMatch);

    /*
     * The following scenario tests when a mbi is not found in the
     * Beneficiaries and BeneficiariesHistory table.
     *
     */
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "notfoundmbi"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * The following method tests that a ResourceNotFoundException exception is thrown when there are
   * instances of one hash value (hicn or mbi) pointing to more than bene id between the
   * Beneficiaries and BeneficiariesHistory tables.
   *
   * <p>Or that single match is found when the expectsSingleBeneMatch param is = true.
   *
   * <p>The hashType param chooses which type of values/hash to use. This is either "hicn" or "mbi".
   *
   * @param fhirClient
   * @param beneficiariesList
   * @param beneficiariesHistoryList
   * @param beneficiaryId
   * @param unhashedValue
   * @param useFromBeneficiaryTable
   * @param hashType
   * @param expectsSingleBeneMatch
   */
  private void assertPatientByHashTypeMatch(
      IGenericClient fhirClient,
      List<Beneficiary> beneficiariesList,
      List<BeneficiaryHistory> beneficiariesHistoryList,
      String beneficiaryId,
      String unhashedValue,
      Boolean useFromBeneficiaryTable,
      String hashType,
      Boolean expectsSingleBeneMatch) {

    Bundle searchResults = null;
    String hicnHashed = "";
    String mbiHash = "";

    if (hashType != "hicn" && hashType != "mbi") {
      Assert.fail("hashType value must be: hicn or mbi.");
    }

    if (useFromBeneficiaryTable) {
      if (hashType.equals("hicn")) {
        Beneficiary beneficiaryHicnToMatchTo =
            beneficiariesList.stream()
                .filter(r -> unhashedValue.equals(r.getHicnUnhashed().get()))
                .findFirst()
                .get();
        hicnHashed = beneficiaryHicnToMatchTo.getHicn();
      } else if (hashType.equals("mbi")) {
        Beneficiary beneficiaryMbiToMatchTo =
            beneficiariesList.stream()
                .filter(r -> unhashedValue.equals(r.getMedicareBeneficiaryId().get()))
                .findFirst()
                .get();
        mbiHash = beneficiaryMbiToMatchTo.getMbiHash().get();
      }
    } else {
      if (hashType.equals("hicn")) {
        BeneficiaryHistory beneficiaryHistoryHicnToMatchTo =
            beneficiariesHistoryList.stream()
                .filter(r -> unhashedValue.equals(r.getHicnUnhashed().get()))
                .findFirst()
                .get();
        hicnHashed = beneficiaryHistoryHicnToMatchTo.getHicn();
      } else if (hashType.equals("mbi")) {
        BeneficiaryHistory beneficiaryHistoryMbiToMatchTo =
            beneficiariesHistoryList.stream()
                .filter(r -> unhashedValue.equals(r.getMedicareBeneficiaryId().get()))
                .findFirst()
                .get();
        mbiHash = beneficiaryHistoryMbiToMatchTo.getMbiHash().get();
      }
    }

    try {
      // return bene record based on unhashedValue passed to this method
      if (hashType.equals("hicn")) {
        searchResults =
            fhirClient
                .search()
                .forResource(Patient.class)
                .where(
                    Patient.IDENTIFIER
                        .exactly()
                        .systemAndIdentifier(
                            TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, hicnHashed))
                .returnBundle(Bundle.class)
                .execute();
      } else if (hashType.equals("mbi")) {
        searchResults =
            fhirClient
                .search()
                .forResource(Patient.class)
                .where(
                    Patient.IDENTIFIER
                        .exactly()
                        .systemAndIdentifier(
                            TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, mbiHash))
                .returnBundle(Bundle.class)
                .execute();
      }

      if (!expectsSingleBeneMatch) {
        // Should throw exception before here, so assert a failed test.
        Assert.fail("An exception was expected when there are duplicate bene id matches.");
      }
    } catch (ResourceNotFoundException e) {
      // Test passes if an exception was thrown.
    }

    // Validate result if a single match is expected for test.
    if (expectsSingleBeneMatch) {
      Assert.assertNotNull(searchResults);
      Assert.assertEquals(1, searchResults.getTotal());

      Beneficiary beneficiary =
          beneficiariesList.stream()
              .filter(r -> beneficiaryId.equals(r.getBeneficiaryId()))
              .findAny()
              .get();
      Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
      BeneficiaryTransformerTest.assertMatches(
          beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByMbiHashIncludeIdentifiersFalse() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient("false", "true");

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                        beneficiary.getMbiHash().get()))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    /*
     * Ensure the unhashed values for HICN and MBI are *not* present.
     */
    Boolean hicnUnhashedPresent = false;
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED))
        hicnUnhashedPresent = true;
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
        mbiUnhashedPresent = true;
    }

    Assert.assertFalse(hicnUnhashedPresent);
    Assert.assertFalse(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with paging.
   */
  @Test
  public void searchForExistingPatientByMbiHashWithPaging() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                        beneficiary.getMbiHash().get()))
            .count(1)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getTotal());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    BeneficiaryTransformerTest.assertMatches(
        beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs that should be present as a {@link BeneficiaryHistory} record.
   */
  @Test
  public void searchForExistingPatientByHistoricalMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    loadedRecords.stream()
        .filter(r -> r instanceof BeneficiaryHistory)
        .map(r -> (BeneficiaryHistory) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                                  h.getMbiHash().get()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientByMbiWithNoHistory() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClient();

    loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                                  h.getMbiHash().get()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientByMbiWithNoHistoryIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClient("true", "true");

    loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .forEach(
            h -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(Patient.class)
                      .where(
                          Patient.IDENTIFIER
                              .exactly()
                              .systemAndIdentifier(
                                  TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                                  h.getMbiHash().get()))
                      .returnBundle(Bundle.class)
                      .execute();

              Assert.assertNotNull(searchResults);
              Assert.assertEquals(1, searchResults.getTotal());
              Patient patientFromSearchResult =
                  (Patient) searchResults.getEntry().get(0).getResource();
              Assert.assertEquals(
                  h.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForMissingPatientByMbiHash() {
    IGenericClient fhirClient = createFhirClient();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "1234"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * PatientResourceProvider#searchByCoverageContract(ca.uhn.fhir.rest.param.TokenParam,
   * ca.uhn.fhir.rest.param.TokenParam, String, ca.uhn.fhir.rest.api.server.RequestDetails)} works
   * as expected.
   */
  @Test
  public void searchByPartDContract() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(
            Arrays.asList(
                StaticRifResource.SAMPLE_A_BENES,
                StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY,
                StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY_EXTRA));
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    // Verify that it found the expected bene.
    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    Beneficiary expectedBene = (Beneficiary) loadedRecords.get(0);
    Assert.assertEquals(
        expectedBene.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());

    /*
     * Verify that the unhashed MBIs are present, as expected. Note that checking for more than just
     * one MBI and verifying that they're all unique is a regression test for BFD-525.
     */
    Assert.assertEquals(
        3,
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                i ->
                    i.getSystem()
                        .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
            .collect(Collectors.toSet())
            .size());
  }

  /**
   * Verifies that {@link
   * PatientResourceProvider#searchByCoverageContract(ca.uhn.fhir.rest.param.TokenParam,
   * ca.uhn.fhir.rest.param.TokenParam, String, ca.uhn.fhir.rest.api.server.RequestDetails)} works
   * as expected, when no year is specified (hopefully causing it to substitute the current year).
   */
  @Test
  public void searchByPartDContractWithoutYear() {
    /*
     * TODO Once AB2D has switched to always specifying the year, this needs to become an invalid
     * request and this test will need to be updated to reflect that, then.
     */

    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    // First, adjust the bene's reference year in the DB.
    ServerTestUtils.doTransaction(
        (entityManager) -> {
          CriteriaBuilder builder = entityManager.getCriteriaBuilder();

          CriteriaQuery<BeneficiaryMonthly> select = builder.createQuery(BeneficiaryMonthly.class);
          select.from(BeneficiaryMonthly.class);
          List<BeneficiaryMonthly> beneMonthlys = entityManager.createQuery(select).getResultList();

          for (BeneficiaryMonthly beneMonthly : beneMonthlys) {
            LocalDate yearMonth = beneMonthly.getYearMonth();
            CriteriaUpdate<BeneficiaryMonthly> update =
                builder.createCriteriaUpdate(BeneficiaryMonthly.class);
            Root<BeneficiaryMonthly> beneMonthlyRoot = update.from(BeneficiaryMonthly.class);
            update.set(
                BeneficiaryMonthly_.yearMonth,
                LocalDate.of(
                    Year.now().getValue(), yearMonth.getMonthValue(), yearMonth.getDayOfMonth()));
            update.where(
                builder.equal(
                    beneMonthlyRoot.get(BeneficiaryMonthly_.parentBeneficiary),
                    beneMonthly.getParentBeneficiary()),
                builder.equal(beneMonthlyRoot.get(BeneficiaryMonthly_.yearMonth), yearMonth));

            entityManager.createQuery(update).executeUpdate();
          }
        });

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .returnBundle(Bundle.class)
            .execute();

    // Verify that it found the expected bene.
    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    Beneficiary expectedBene = (Beneficiary) loadedRecords.get(0);
    Assert.assertEquals(
        expectedBene.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());
  }

  /**
   * Verifies that {@link
   * PatientResourceProvider#searchByCoverageContract(ca.uhn.fhir.rest.param.TokenParam,
   * ca.uhn.fhir.rest.param.TokenParam, String, ca.uhn.fhir.rest.api.server.RequestDetails)} works
   * as expected, when paging is requested.
   */
  @Test
  public void searchByPartDContractWithPaging() {
    ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .count(1)
            .returnBundle(Bundle.class)
            .execute();

    // Verify that it found the expected bene and no extra pages.
    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
  }

  /**
   * Verifies that {@link
   * PatientResourceProvider#searchByCoverageContract(ca.uhn.fhir.rest.param.TokenParam,
   * ca.uhn.fhir.rest.param.TokenParam, String, ca.uhn.fhir.rest.api.server.RequestDetails)} works
   * as expected, when searching for a contract-year-month with no benes.
   */
  @Test
  public void searchByPartDContractForEmptyContract() {
    ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.PTDCNTRCT01),
                        "A1234"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        TransformerUtils.calculateVariableReferenceUrl(
                            CcwCodebookVariable.RFRNC_YR),
                        "2010"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getEntry().size());
  }

  /**
   * Verifies that {@link
   * PatientResourceProvider#searchByCoverageContract(ca.uhn.fhir.rest.param.TokenParam,
   * ca.uhn.fhir.rest.param.TokenParam, String, ca.uhn.fhir.rest.api.server.RequestDetails)} works
   * as expected, when an invalid year is specified.
   */
  @Test(expected = InvalidRequestException.class)
  public void searchByPartDContractWithInvalidYear() {
    ServerTestUtils.loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(
                    TransformerUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                    "S4607"))
        .where(
            new TokenClientParam("_has:Coverage.rfrncyr")
                .exactly()
                .systemAndIdentifier(
                    TransformerUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                    "ABC"))
        .returnBundle(Bundle.class)
        .execute();
  }

  @Test
  public void searchWithLastUpdated() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    // Build up a list of lastUpdatedURLs that return > all values values
    DateTimeFormatter dtf =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSx':00'")
            .withZone(ZoneId.systemDefault());
    String nowDateTime = dtf.format(Instant.now().plusSeconds(1));
    String earlyDateTime = "2019-10-01T00:00:00-04:00";
    List<String> allUrls =
        Arrays.asList(
            "_lastUpdated=gt" + earlyDateTime,
            "_lastUpdated=ge" + earlyDateTime,
            "_lastUpdated=le" + nowDateTime,
            "_lastUpdated=ge" + earlyDateTime + "&_lastUpdated=le" + nowDateTime,
            "_lastUpdated=gt" + earlyDateTime + "&_lastUpdated=lt" + nowDateTime);
    testLastUpdatedUrls(fhirClient, beneficiary.getBeneficiaryId(), allUrls, 1);

    // Empty searches
    List<String> emptyUrls =
        Arrays.asList("_lastUpdated=lt" + earlyDateTime, "_lastUpdated=le" + earlyDateTime);
    testLastUpdatedUrls(fhirClient, beneficiary.getBeneficiaryId(), emptyUrls, 0);
  }

  /**
   * Test the set of lastUpdated values
   *
   * @param fhirClient to use
   * @param id the beneficiary id to use
   * @param urls is a list of lastUpdate values to test to find
   * @param expectedValue number of matches
   */
  private void testLastUpdatedUrls(
      IGenericClient fhirClient, String id, List<String> urls, int expectedValue) {
    String baseResourceUrl = "Patient?_id=" + id + "&_format=application%2Fjson%2Bfhir";

    // Search for each lastUpdated value
    for (String lastUpdatedValue : urls) {
      String theSearchUrl = baseResourceUrl + "&" + lastUpdatedValue;
      Bundle searchResults =
          fhirClient.search().byUrl(theSearchUrl).returnBundle(Bundle.class).execute();
      Assert.assertEquals(
          String.format(
              "Expected %s to filter resources using lastUpdated correctly", lastUpdatedValue),
          expectedValue,
          searchResults.getTotal());
    }
  }

  /** Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called after each test case. */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    ServerTestUtils.cleanDatabaseServer();
  }

  /**
   * test helper
   *
   * @param value of all include identifier values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldIdntityHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, value);
  }

  /**
   * test helper
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldAddrFldHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }

  /**
   * helper create a client w/o extra params
   *
   * @return the client
   */
  public static IGenericClient createFhirClient() {
    return createFhirClient(null);
  }

  /**
   * @return a FHIR {@link IGenericClient} where the {@link
   *     CommonHeaders#HEADER_NAME_INCLUDE_IDENTIFIERS} is set to <code>"true"</code>
   */
  public static IGenericClient createFhirClientWithIncludeIdentifiersMbi() {
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "mbi");
    return createFhirClient(requestHeader);
  }

  /**
   * helper
   *
   * @param idHdrVal - includeIdentifiers header value
   * @param addrHdrVal - includeAddressFields header value
   * @return the client
   */
  public static IGenericClient createFhirClient(String idHdrVal, String addrHdrVal) {
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            idHdrVal,
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            addrHdrVal);
    return createFhirClient(requestHeader);
  }
  /**
   * helper
   *
   * @return the client with extra params registered
   */
  public static IGenericClient createFhirClient(RequestHeaders requestHeader) {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    if (requestHeader != null) {
      ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
      extraParamsInterceptor.setHeaders(requestHeader);
      fhirClient.registerInterceptor(extraParamsInterceptor);
    }
    return fhirClient;
  }
}
