package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer}. */
public final class BeneficiaryTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary}.
   */
  @Test
  public void transformSampleARecord() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("false");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    assertEquals(2, patient.getIdentifier().size(), "Number of identifiers should be 2");

    // Verify identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with {@link IncludeIdentifiersValues} = ["hicn","mbi"].
   */
  @Test
  public void transformSampleARecordWithIdentifiers() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("hicn,mbi");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(8, patient.getIdentifier().size(), "Number of identifiers should be 8");
    // Verify patient identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "someHICNhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066U");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "3456789");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "9AB2WW3GR44");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with {@link IncludeIdentifiersValues} = ["true"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersTrue() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("true");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(8, patient.getIdentifier().size(), "Number of identifiers should be 8");
    // Verify patient identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "someHICNhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066U");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "3456789");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "9AB2WW3GR44");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with {@link IncludeIdentifiersValues} = ["hicn"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersHicn() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("hicn");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(6, patient.getIdentifier().size(), "Number of identifiers should be 6");
    // Verify patient identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, "someHICNhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066U");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066U");
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z"); }
   *
   * <p>/** Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with {@link IncludeIdentifiersValues} = ["mbi"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersMbi() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("mbi");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(4, patient.getIdentifier().size(), "Number of identifiers should be 4");
    // Verify patient identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "3456789");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "9AB2WW3GR44");
  }

  /**
   * Verifies that the {@link Patient} identifiers contain expected values.
   *
   * @param Patient {@link Patient} containing identifiers
   * @param identifierSystem value to be matched
   * @param identifierValue value to be matched
   */
  private static void assertValuesInPatientIdentifiers(
      Patient patient, String identifierSystem, String identifierValue) {
    boolean identifierFound = false;

    for (Identifier temp : patient.getIdentifier()) {
      if (identifierSystem.equals(temp.getSystem()) && identifierValue.equals(temp.getValue())) {
        identifierFound = true;
        break;
      }
    }
    assertEquals(
        identifierFound,
        true,
        "Identifier "
            + identifierSystem
            + " value = "
            + identifierValue
            + " does not match an expected value.");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a lastUpdated field set.
   */
  @Test
  public void transformSampleARecordWithLastUpdated() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    beneficiary.setLastUpdated(Instant.now());
    Patient patientWithLastUpdated =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patientWithLastUpdated, requestHeader);

    beneficiary.setLastUpdated(Optional.empty());
    Patient patientWithoutLastUpdated =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patientWithoutLastUpdated, requestHeader);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a lastUpdated field not set.
   */
  @Test
  public void transformSampleARecordWithoutLastUpdated() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    beneficiary.setLastUpdated(Optional.empty());
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  @Test
  public void transformSampleARecordWithoutReferenceYear() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    beneficiary.setBeneEnrollmentReferenceYear(Optional.empty());
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    String url = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";
    Optional<Extension> ex =
        patient.getExtension().stream().filter(e -> url.equals(e.getUrl())).findFirst();

    assertEquals(true, ex.isEmpty());
  }

  /**
   * @return the {@link StaticRifResourceGroup#SAMPLE_A} {@link Beneficiary} record, with the {@link
   *     Beneficiary#getBeneficiaryHistories()} and {@link
   *     Beneficiary#getMedicareBeneficiaryIdHistories()} fields populated.
   */
  private static Beneficiary loadSampleABeneficiary() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    Beneficiary beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    beneficiary.setHicn("someHICNhash");
    beneficiary.setMbiHash(Optional.of("someMBIhash"));

    // Add the HICN history records to the Beneficiary, and fix their HICN fields.
    Set<BeneficiaryHistory> beneficiaryHistories =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId()))
            .collect(Collectors.toSet());
    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);
    for (BeneficiaryHistory beneficiaryHistory : beneficiary.getBeneficiaryHistories()) {
      beneficiaryHistory.setHicnUnhashed(Optional.of(beneficiaryHistory.getHicn()));
      beneficiaryHistory.setHicn("someHICNhash");
    }

    // Add the MBI history records to the Beneficiary.
    Set<MedicareBeneficiaryIdHistory> beneficiaryMbis =
        parsedRecords.stream()
            .filter(r -> r instanceof MedicareBeneficiaryIdHistory)
            .map(r -> (MedicareBeneficiaryIdHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId().orElse(null)))
            .collect(Collectors.toSet());
    beneficiary.getMedicareBeneficiaryIdHistories().addAll(beneficiaryMbis);

    return beneficiary;
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer} works
   * correctly when passed a {@link Beneficiary} where all {@link Optional} fields are set to {@link
   * Optional#empty()}.
   */
  @Test
  public void transformBeneficiaryWithAllOptionalsEmpty() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    TransformerTestUtils.setAllOptionalsToEmpty(beneficiary);
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Notes for reviewer: for header related coverage, do not test on the combination of headers
   * values if there is no correlation between the headers, hence removed includeAddressFields
   * header tests out of includeIdentifiers header tests to speed up tests and keep the same level
   * of coverage at the same time.
   */

  /**
   * Verifies that {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer} works
   * correctly when passed a {@link Beneficiary} where all {@link Optional} fields are set to {@link
   * Optional#empty()} and includeAddressFields header take all possible values.
   */
  @Test
  public void transformBeneficiaryWithIncludeAddressFieldsAllOptEmpty() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    TransformerTestUtils.setAllOptionalsToEmpty(beneficiary);
    RequestHeaders requestHeader = getRHwithIncldAddrFldHdr("true");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer} works
   * correctly when passed a {@link Beneficiary} with various includeAddressFields header values.
   */
  @Test
  public void transformSampleARecordWithIncludeAddressFields() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    RequestHeaders requestHeader = getRHwithIncldAddrFldHdr("true");
    Patient patient =
        BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = BeneficiaryTransformer.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that the {@link Patient} "looks like" it should, if it were produced from the
   * specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the {@link Patient} was generated from
   * @param patient the {@link Patient} that was generated from the specified {@link Beneficiary}
   */
  static void assertMatches(
      Beneficiary beneficiary, Patient patient, RequestHeaders requestHeader) {
    TransformerTestUtils.assertNoEncodedOptionals(patient);

    assertEquals(beneficiary.getBeneficiaryId(), patient.getIdElement().getIdPart());

    assertEquals(java.sql.Date.valueOf(beneficiary.getBirthDate()), patient.getBirthDate());

    if (beneficiary.getSex() == Sex.MALE.getCode())
      assertEquals(AdministrativeGender.MALE.toString(), patient.getGender().toString().trim());
    else if (beneficiary.getSex() == Sex.FEMALE.getCode())
      assertEquals(AdministrativeGender.FEMALE.toString(), patient.getGender().toString().trim());
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.RACE, beneficiary.getRace(), patient);
    assertEquals(beneficiary.getNameGiven(), patient.getName().get(0).getGiven().get(0).toString());
    if (beneficiary.getNameMiddleInitial().isPresent())
      assertEquals(
          beneficiary.getNameMiddleInitial().get().toString(),
          patient.getName().get(0).getGiven().get(1).toString());
    assertEquals(beneficiary.getNameSurname(), patient.getName().get(0).getFamily());

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), patient);
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent())
      TransformerTestUtils.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), patient);

    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), patient);

    Boolean inclAddrFlds =
        (Boolean)
            requestHeader.getValue(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS);

    if (inclAddrFlds != null && inclAddrFlds) {
      assertEquals(1, patient.getAddress().size());
      // assert address fields etc.
      assertEquals(beneficiary.getStateCode(), patient.getAddress().get(0).getState());
      // assert CountyCode is no longer mapped
      assertNull(patient.getAddress().get(0).getDistrict());
      assertEquals(beneficiary.getPostalCode(), patient.getAddress().get(0).getPostalCode());
      assertEquals(
          beneficiary.getDerivedCityName().orElse(null), patient.getAddress().get(0).getCity());

      assertEquals(
          beneficiary.getDerivedMailingAddress1().orElse(""),
          patient.getAddress().get(0).getLine().get(0).getValueNotNull());
      assertEquals(
          beneficiary.getDerivedMailingAddress2().orElse(""),
          patient.getAddress().get(0).getLine().get(1).getValueNotNull());
      assertEquals(
          beneficiary.getDerivedMailingAddress3().orElse(""),
          patient.getAddress().get(0).getLine().get(2).getValueNotNull());
      assertEquals(
          beneficiary.getDerivedMailingAddress4().orElse(""),
          patient.getAddress().get(0).getLine().get(3).getValueNotNull());
      assertEquals(
          beneficiary.getDerivedMailingAddress5().orElse(""),
          patient.getAddress().get(0).getLine().get(4).getValueNotNull());
      assertEquals(
          beneficiary.getDerivedMailingAddress6().orElse(""),
          patient.getAddress().get(0).getLine().get(5).getValueNotNull());
    } else {
      assertEquals(1, patient.getAddress().size());
      assertEquals(beneficiary.getStateCode(), patient.getAddress().get(0).getState());
      // assert CountyCode is no longer mapped
      assertNull(patient.getAddress().get(0).getDistrict());
      assertEquals(beneficiary.getPostalCode(), patient.getAddress().get(0).getPostalCode());
      // assert address city name and line 0 - 5 fields etc.
      assertNull(patient.getAddress().get(0).getCity());
      assertEquals(0, patient.getAddress().get(0).getLine().size());
    }
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
}
