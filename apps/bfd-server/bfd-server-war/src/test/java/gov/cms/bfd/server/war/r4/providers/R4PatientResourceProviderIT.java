package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.stu3.providers.ExtraParamsInterceptor;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public final class R4PatientResourceProviderIT {
  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called once to
   * initialize data in the test suite.
   */
  @BeforeClass
  public static void cleanupDatabaseBeforeTestSuite() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void readExistingPatient() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    comparePatient(beneficiary, patient);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "true".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersTrue() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "true",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "mbi".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersMbi() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "mbi",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "false".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersFalse() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "false",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value = "".
   */
  @Test
  public void readExistingPatientIncludeIdentifiersBlank() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * "invalid-identifier-value" and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid1() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "invalid-identifier-value",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} when include identifiers value =
   * ["mbi,invalid-identifier-value"] and that an exception is thrown.
   */
  @Test(expected = InvalidRequestException.class)
  public void readExistingPatientIncludeIdentifiersInvalid2() {
    assertExistingPatientIncludeIdentifiersExpected(
        R4PatientResourceProvider.CNST_INCL_IDENTIFIERS_EXPECT_MBI,
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "mbi,invalid-identifier-value",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} that does exist in the DB but has no {@link
   * BeneficiaryHistory} or {@link MedicareBeneficiaryIdHistory} records when include identifiers
   * value = ["true"].
   */
  @Test
  public void readExistingPatientWithNoHistoryIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "true",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
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

    comparePatient(beneficiary, patient, requestHeader);

    /*
     * Ensure the unhashed values for HICN and MBI are present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patient.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();

      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_ID)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readMissingPatient() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
    // No data is loaded, so this should return nothing.
    fhirClient.read().resource(Patient.class).withId("1234").execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByLogicalId() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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

    comparePatient(beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByLogicalIdIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
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
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_ID)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByLogicalIdIncludeIdentifiersFalse() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
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
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_ID)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with paging.
   */
  @Test
  public void searchForPatientByLogicalIdWithPaging() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForMissingPatientByLogicalId() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   *
   * <p>works as expected for a {@link Patient} that does exist in the DB.
   */
  @Test
  public void searchForExistingPatientByMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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

    comparePatient(beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));

    String mbiIdentifier =
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                identifier ->
                    identifier
                        .getSystem()
                        .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED))
            .findFirst()
            .get()
            .getValue();

    Assert.assertEquals(
        "mbiHash identifier exists", beneficiary.getMedicareBeneficiaryId().get(), mbiIdentifier);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, including identifiers to
   * return the unhashed HICN and MBI.
   */
  @Test
  public void searchForExistingPatientByMbiHashIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
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
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Verifies that the correct bene id or exception is returned when an MBI points to more than one
   * bene id in either the Beneficiaries and/or BeneficiariesHistory table.
   */
  @Test
  public void searchForExistingPatientByMbiHashWithBeneDups() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records for
    // testing
    loadedRecords.addAll(
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with paging.
   */
  @Test
  public void searchForExistingPatientByMbiHashWithPaging() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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

    comparePatient(beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));

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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs that should be present as a {@link BeneficiaryHistory} record.
   */
  @Test
  public void searchForExistingPatientByHistoricalMbiHash() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientByMbiWithNoHistory() {
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for MBIs associated with {@link Beneficiary}s that have <strong>no</strong>
   * {@link BeneficiaryHistory} records.
   */
  @Test
  public void searchForExistingPatientByMbiWithNoHistoryIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForMissingPatientByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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

  @Test
  public void searchForExistingPatientByPartDContractNum() {
    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
  }

  @Test
  public void searchForExistingPatientByPartDContractNumIncludeIdentifiersTrue() {
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    Beneficiary expectedBene = (Beneficiary) loadedRecords.get(0);
    Assert.assertEquals(
        expectedBene.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());

    /*
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is requested. (This oddly specific combo had been bugged earlier and
   * was quite tricky to resolve).
   */
  @Test
  public void
      searchForExistingPatientByPartDContractNumIncludeIdentifiersTrueWithPagingAndMultipleMbis() {
    ServerTestUtils.get()
        .loadData(
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .count(1)
            .returnBundle(Bundle.class)
            .execute();

    // Verify that the bene wasn't duplicated.
    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());

    // Double-check that the bene has multiple identifiers.
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    Assert.assertEquals(
        1, // was 4
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                i ->
                    TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED.equals(
                        i.getSystem()))
            .count());
  }

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is not requested. (This oddly specific combo had been bugged earlier
   * and was quite tricky to resolve).
   */
  @Test
  public void searchForExistingPatientByPartDContractNumIncludeIdentifiersTrueAndMultipleMbis() {
    ServerTestUtils.get()
        .loadData(
            Arrays.asList(
                StaticRifResource.SAMPLE_A_BENES,
                StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY,
                StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY_EXTRA));
    IGenericClient fhirClient = createFhirClient("mbi", "true");

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    // Verify that the bene wasn't duplicated.
    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());

    // Double-check that the bene has multiple identifiers.
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();
    Assert.assertEquals(
        1, // was 4
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                i ->
                    TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED.equals(
                        i.getSystem()))
            .count());
  }

  @Test
  public void searchForExistingPatientByPartDContractNumIncludeIdentifiersFalse() {
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = createFhirClient("mbi, false", "true");

    // Should return a single match
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    Beneficiary expectedBene = (Beneficiary) loadedRecords.get(0);
    Assert.assertEquals(
        expectedBene.getBeneficiaryId(), patientFromSearchResult.getIdElement().getIdPart());

    /*
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)) {
        mbiUnhashedPresent = true;
      }
    }

    Assert.assertTrue(mbiUnhashedPresent);
  }

  @Test
  public void searchForPatientByPartDContractNumWithPaging() {

    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .count(10)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());

    /*
     * Verify that only the first and last paging links exist, since there should
     * only be one page.
     */
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
  }

  @Test
  public void searchForMissingPatientByPartDContractNum() {
    IGenericClient fhirClient = createFhirClientWithIncludeIdentifiersMbi();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                new TokenClientParam("_has:Coverage.extension")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "12345"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getEntry().size());
  }

  @Test
  public void searchWithLastUpdated() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    // Build up a list of lastUpdatedURLs that return > all values values
    String nowDateTime = new DateTimeDt(Date.from(Instant.now().plusSeconds(1))).getValueAsString();
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

  @Test
  public void searchForExistingPatientByPartDContractNumAndYear() {
    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2018"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(1, searchResults.getEntry().size());
  }

  @Test
  public void searchForNonExistingPatientByPartDContractNumAndYear() {
    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4607"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2010"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getEntry().size());
  }

  @Test
  public void searchForPatientByPartDContractNumWithAInvalidContract() {
    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
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
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                        "S4600"))
            .where(
                new TokenClientParam("_has:Coverage.rfrncyr")
                    .exactly()
                    .systemAndIdentifier(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR),
                        "2010"))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getEntry().size());
  }

  @Test(expected = InvalidRequestException.class)
  public void searchForPatientByPartDContractNumWithAInvalidYear() {

    ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    // Should return a single match
    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(
                    CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01),
                    "S4607"))
        .where(
            new TokenClientParam("_has:Coverage.rfrncyr")
                .exactly()
                .systemAndIdentifier(
                    CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR), "201"))
        .returnBundle(Bundle.class)
        .execute();
  }

  /**
   * Asserts that {@link
   * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * contains expected/present identifiers for a {@link Patient}.
   *
   * @param includeIdentifiersValue header value
   * @param expectingMbi true if expecting a MBI
   * @param includeAddressValues header value
   */
  public void assertExistingPatientIncludeIdentifiersExpected(
      boolean expectingMbi, RequestHeaders requestHeader) {

    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Patient expected =
        BeneficiaryTransformerV2.transform(
            PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
            beneficiary,
            requestHeader);

    IGenericClient fhirClient = createFhirClient(requestHeader);
    Patient patient =
        fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();

    // Because of how transform doesn't go through R4PatientResourceProvider, `expected` won't have
    // the historical MBI data.
    // Also, SAMPLE_A does not have mbi history (it used to); however, what used to be denoted as
    // historical
    // is not provided as the 'current' MBI identifier (no historical).
    comparePatient(expected, patient);

    /*
     * Ensure the unhashed values for MBI are present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patient.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier.getSystem().equals(TransformerConstants.CODING_BBAPI_BENE_ID)) {
        mbiUnhashedPresent = true;
      }
    }

    // Unhashed MBI should always be present in V2
    Assert.assertTrue(mbiUnhashedPresent);
  }

  /**
   * test helper
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldAddrFldHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }

  /**
   * helper
   *
   * @return the client with extra params registered
   */
  public static IGenericClient createFhirClient(RequestHeaders requestHeader) {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
    if (requestHeader != null) {
      ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
      extraParamsInterceptor.setHeaders(requestHeader);
      fhirClient.registerInterceptor(extraParamsInterceptor);
    }
    return fhirClient;
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
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            idHdrVal,
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            addrHdrVal);
    return createFhirClient(requestHeader);
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
      Boolean expectsSingleBeneMatch) {

    Bundle searchResults = null;
    String mbiHash = "";

    if (useFromBeneficiaryTable) {
      Beneficiary beneficiaryMbiToMatchTo =
          beneficiariesList.stream()
              .filter(r -> unhashedValue.equals(r.getMedicareBeneficiaryId().get()))
              .findFirst()
              .get();

      mbiHash = beneficiaryMbiToMatchTo.getMbiHash().get();
    } else {
      BeneficiaryHistory beneficiaryHistoryMbiToMatchTo =
          beneficiariesHistoryList.stream()
              .filter(r -> unhashedValue.equals(r.getMedicareBeneficiaryId().get()))
              .findFirst()
              .get();

      mbiHash = beneficiaryHistoryMbiToMatchTo.getMbiHash().get();
    }

    try {
      // return bene record based on unhashedValue passed to this method
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

      comparePatient(beneficiary, patientFromSearchResult, getRHwithIncldAddrFldHdr("false"));
    }
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

  private void comparePatient(Beneficiary beneficiary, Patient patient, RequestHeaders headers) {
    Assert.assertNotNull(patient);

    Patient expected =
        BeneficiaryTransformerV2.transform(
            PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
            beneficiary,
            headers);

    comparePatient(expected, patient);
  }

  private void comparePatient(Beneficiary beneficiary, Patient patient) {
    comparePatient(beneficiary, patient, RequestHeaders.getHeaderWrapper());
  }

  private void comparePatient(Patient expected, Patient patient) {
    // The ID returned from the FHIR client differs from the transformer.  It adds URL information.
    // Here we verify that the resource it is pointing to is the same, and then set up to do a deep
    // compare of the rest
    Assert.assertTrue(patient.getId().endsWith(expected.getId()));
    patient.setIdElement(expected.getIdElement());

    // Last updated time will also differ, so fix this before the deep compare
    Assert.assertNotNull(patient.getMeta().getLastUpdated());
    patient.getMeta().setLastUpdatedElement(expected.getMeta().getLastUpdatedElement());

    Assert.assertTrue(expected.equalsDeep(patient));
  }

  public static IGenericClient createFhirClientWithIncludeIdentifiersMbi() {
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "mbi");
    return createFhirClient(requestHeader);
  }

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }
}
