package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(beneficiary, patient);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "true".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersTrue() {
    String includeIdentifiersValue = "true";
    boolean expectingHicn = true;
    boolean expectingMbi = true;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "hicn,mbi".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersHicnMbi() {
    String includeIdentifiersValue = "hicn,mbi";
    boolean expectingHicn = true;
    boolean expectingMbi = true;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "hicn".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersHicn() {
    String includeIdentifiersValue = "hicn";
    boolean expectingHicn = true;
    boolean expectingMbi = false;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "mbi".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersMbi() {
    String includeIdentifiersValue = "mbi";
    boolean expectingHicn = false;
    boolean expectingMbi = true;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "false".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersFalse() {
    String includeIdentifiersValue = "false";
    boolean expectingHicn = false;
    boolean expectingMbi = false;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersBlank() {
    String includeIdentifiersValue = "";
    boolean expectingHicn = false;
    boolean expectingMbi = false;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * "invalid-identifier-value" and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid1() {
    String includeIdentifiersValue = "invalid-identifier-value";
    boolean expectingHicn = false;
    boolean expectingMbi = false;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * ["mbi,invalid-identifier-value"] and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid2() {
    String includeIdentifiersValue = "mbi,invalid-identifier-value";
    boolean expectingHicn = false;
    boolean expectingMbi = false;

    assertExistingPatientIncludeIdentifiersExpected(
        includeIdentifiersValue, expectingHicn, expectingMbi);
  }

  /**
   * Asserts that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * contains expected/present identifiers for a {@link Patient}.
   *
   * @param includeIdentifiersValue header value
   * @param expectingHicn true if expecting a HICN
   * @param expectingMbi true if expecting a MBI
   */
  public void assertExistingPatientIncludeIdentifiersExpected(
      String includeIdentifiersValue, boolean expectingHicn, boolean expectingMbi) {

    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers(includeIdentifiersValue);
    fhirClient.registerInterceptor(extraParamsInterceptor);

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(beneficiary, patient);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    Assert.assertNotNull(patient);
    BeneficiaryTransformerTest.assertMatches(beneficiary, patient);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("false");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
  @Ignore
  @Test
  public void searchForExistingPatientByHicnHashWithBeneDups() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records for
    // testing
    loadedRecords.addAll(
        ServerTestUtils.loadData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    /*
     * The following scenario tests when the same hicn is in the
     * Beneficiaries table but points to different bene ids. Next check is
     * to pull back the bene record in the Beneficiaries table with the most
     * recent rfrnc_yr.
     *
     * bene id=567834 hicn=543217066U rfrnc_yr=2019 should be pulled back.
     */
    useHicnFromBeneficiaryTable = true;
    assertPatientByHicnHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "567834",
        "543217066U",
        useHicnFromBeneficiaryTable);

    /*
     * The following scenario tests when only one hicn is in the
     * Beneficiaries table but has a rfrnc_yr value of null.
     *
     * bene id=123456NULLREFYR hicn=543217066N rfrnc_yr=null should be
     * pulled back.
     */
    useHicnFromBeneficiaryTable = true;
    assertPatientByHicnHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "123456NULLREFYR",
        "543217066N",
        useHicnFromBeneficiaryTable);

    /*
     * The following scenario tests when the same hicn is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The bene id
     * is different between the tables so the bene record from the
     * Beneficiaries table should be used.
     *
     * bene id=BENE1234 hicn=SAMEHICN rfrnc_yr=2019 should be pulled back.
     */
    useHicnFromBeneficiaryTable = true;
    assertPatientByHicnHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "BENE1234",
        "SAMEHICN",
        useHicnFromBeneficiaryTable);

    /*
     * The following scenario tests when the requested hicn is only in the
     * BeneficiariesHistory table. Use the bene id from the
     * BeneficiariesHistory table to then read the Beneficiaries table.
     *
     * bene id=55555 hicn=HISTHICN rfrnc_yr=2019 should be pulled back.
     */
    useHicnFromBeneficiaryTable = false;
    assertPatientByHicnHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "55555",
        "HISTHICN",
        useHicnFromBeneficiaryTable);

    /*
     * The following scenario tests when the requested hicn is only in the
     * BeneficiariesHistory table but this hicn points to more than one bene
     * id in history. Next check is to pull back the bene record in the
     * Beneficiaries table with the most recent rfrnc_yr.
     *
     * bene id=66666 hicn=DUPHISTHIC rfrnc_yr=2018 should be pulled back.
     */
    useHicnFromBeneficiaryTable = false;
    assertPatientByHicnHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "66666",
        "DUPHISTHIC",
        useHicnFromBeneficiaryTable);

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
   * The following method tests which beneficiary record should be returned when there are instances
   * of one hicn pointing to more than bene id between the Beneficiaries and BeneficiariesHistory
   * tables.
   *
   * @param fhirClient
   * @param beneficiariesList
   * @param beneficiariesHistoryList
   * @param beneficiaryId
   * @param hicnUnhashed
   * @param useHicnFromBeneficiaryTable
   */
  private void assertPatientByHicnHashWithBeneDupsMatch(
      IGenericClient fhirClient,
      List<Beneficiary> beneficiariesList,
      List<BeneficiaryHistory> beneficiariesHistoryList,
      String beneficiaryId,
      String hicnUnhashed,
      Boolean useHicnFromBeneficiaryTable) {

    String hicnHashed;
    if (useHicnFromBeneficiaryTable) {
      Beneficiary beneficiaryHicnToMatchTo =
          beneficiariesList.stream()
              .filter(r -> hicnUnhashed.equals(r.getHicnUnhashed().get()))
              .findFirst()
              .get();
      hicnHashed = beneficiaryHicnToMatchTo.getHicn();
    } else {
      BeneficiaryHistory beneficiaryHistoryHicnToMatchTo =
          beneficiariesHistoryList.stream()
              .filter(r -> hicnUnhashed.equals(r.getHicnUnhashed().get()))
              .findFirst()
              .get();
      hicnHashed = beneficiaryHistoryHicnToMatchTo.getHicn();
    }

    // return bene record based on hicnUnhashed passed to this method
    Bundle searchResults =
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

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getTotal());

    Beneficiary beneficiary =
        beneficiariesList.stream()
            .filter(r -> beneficiaryId.equals(r.getBeneficiaryId()))
            .findAny()
            .get();
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("false");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for HICNs that should be present as a {@link BeneficiaryHistory} record.
   */
  @Test
  public void searchForExistingPatientByHistoricalHicnHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
   * Verifies that the correct bene id is returned when an MBI points to more than one bene id in
   * either the Beneficiaries and/or BeneficiariesHistory table.
   */
  @Ignore
  @Test
  public void searchForExistingPatientByMbiHashWithBeneDups() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records for
    // testing
    loadedRecords.addAll(
        ServerTestUtils.loadData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries table but points to different bene ids. Next check is
     * to pull back the bene record in the Beneficiaries table with the most
     * recent rfrnc_yr.
     *
     * bene id=567834 mbi=3456789 rfrnc_yr=2019 should be pulled back.
     */
    useMbiFromBeneficiaryTable = true;
    assertPatientByMbiHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "567834",
        "3456789",
        useMbiFromBeneficiaryTable);

    /*
     * The following scenario tests when only one mbi is in the
     * Beneficiaries table but has a rfrnc_yr value of null.
     *
     * bene id=123456NULLREFYR mbi=3456789N rfrnc_yr=null should be
     * pulled back.
     */
    useMbiFromBeneficiaryTable = true;
    assertPatientByMbiHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "123456NULLREFYR",
        "3456789N",
        useMbiFromBeneficiaryTable);

    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The bene id
     * is different between the tables so the bene record from the
     * Beneficiaries table should be used.
     *
     * bene id=BENE1234 mbi=SAMEMBI rfrnc_yr=2019 should be pulled back.
     */
    useMbiFromBeneficiaryTable = true;
    assertPatientByMbiHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "BENE1234",
        "SAMEMBI",
        useMbiFromBeneficiaryTable);

    /*
     * The following scenario tests when the requested mbi is only in the
     * BeneficiariesHistory table. Use the bene id from the
     * BeneficiariesHistory table to then read the Beneficiaries table.
     *
     * bene id=55555 mbi=HISTMBI rfrnc_yr=2019 should be pulled back.
     */
    useMbiFromBeneficiaryTable = false;
    assertPatientByMbiHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "55555",
        "HISTMBI",
        useMbiFromBeneficiaryTable);

    /*
     * The following scenario tests when the requested mbi is only in the
     * BeneficiariesHistory table but this mbi points to more than one bene
     * id in history. Next check is to pull back the bene record in the
     * Beneficiaries table with the most recent rfrnc_yr.
     *
     * bene id=66666 mbi=DUPHISTMBI rfrnc_yr=2018 should be pulled back.
     */
    useMbiFromBeneficiaryTable = false;
    assertPatientByMbiHashWithBeneDupsMatch(
        fhirClient,
        beneficiariesList,
        beneficiariesHistoryList,
        "66666",
        "DUPHISTMBI",
        useMbiFromBeneficiaryTable);

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
   * The following method tests which beneficiary record should be returned when there are instances
   * of one MBI pointing to more than bene id between the Beneficiaries and BeneficiariesHistory
   * tables.
   *
   * @param fhirClient
   * @param beneficiariesList
   * @param beneficiariesHistoryList
   * @param beneficiaryId
   * @param mbi
   * @param useMbiFromBeneficiaryTable
   */
  private void assertPatientByMbiHashWithBeneDupsMatch(
      IGenericClient fhirClient,
      List<Beneficiary> beneficiariesList,
      List<BeneficiaryHistory> beneficiariesHistoryList,
      String beneficiaryId,
      String mbi,
      Boolean useMbiFromBeneficiaryTable) {

    String mbiHash;
    if (useMbiFromBeneficiaryTable) {
      Beneficiary beneficiaryMbiToMatchTo =
          beneficiariesList.stream()
              .filter(r -> mbi.equals(r.getMedicareBeneficiaryId().get()))
              .findFirst()
              .get();
      mbiHash = beneficiaryMbiToMatchTo.getMbiHash().get();
    } else {
      BeneficiaryHistory beneficiaryHistoryMbiToMatchTo =
          beneficiariesHistoryList.stream()
              .filter(r -> mbi.equals(r.getMedicareBeneficiaryId().get()))
              .findFirst()
              .get();
      mbiHash = beneficiaryHistoryMbiToMatchTo.getMbiHash().get();
    }

    // return bene record based on MBI passed to this method
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, mbiHash))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getTotal());

    Beneficiary beneficiary =
        beneficiariesList.stream()
            .filter(r -> beneficiaryId.equals(r.getBeneficiaryId()))
            .findAny()
            .get();
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    BeneficiaryTransformerTest.assertMatches(beneficiary, patientFromSearchResult);
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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("false");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs that should be present as a {@link BeneficiaryHistory} record.
   */
  @Test
  public void searchForExistingPatientByHistoricalMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

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
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

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

  /** Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called after each test case. */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    ServerTestUtils.cleanDatabaseServer();
  }
}
