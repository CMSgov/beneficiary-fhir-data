package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class BeneficiaryTransformerTest {

  /** The class under test. */
  public BeneficiaryTransformer beneficiaryTransformer;

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** Sets the test dependencies up. */
  @BeforeEach
  public void setup() {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    beneficiaryTransformer = new BeneficiaryTransformer(metricRegistry);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("false");
    beneficiaryTransformer.transform(beneficiary, requestHeader);

    String expectedTimerName = beneficiaryTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }

  /**
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary}.
   */
  @Test
  public void transformSampleARecord() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("false");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    assertEquals(2, patient.getIdentifier().size(), "Number of identifiers should be 2");

    // Verify identifiers and values match.
    assertValuesInPatientIdentifiers(
        patient, CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID), "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
  }

  /**
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with IncludeIdentifiers = ["hicn","mbi"].
   */
  @Test
  public void transformSampleARecordWithIdentifiers() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("hicn,mbi");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(12, patient.getIdentifier().size(), "Number of identifiers should be 12");
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
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with IncludeIdentifiers = ["true"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersTrue() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("true");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(12, patient.getIdentifier().size(), "Number of identifiers should be 12");
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
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * with IncludeIdentifiers = ["hicn"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersHicn() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("hicn");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
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
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z");
  }

  /**
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary},
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066U");
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066T");
   * assertValuesInPatientIdentifiers( patient,
   * TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, "543217066Z"); }
   *
   * <p>/** Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)}
   * works as expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link
   * Beneficiary}, with IncludeIdentifiers = ["mbi"].
   */
  @Test
  public void transformSampleARecordWithIdentifiersMbi() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("mbi");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    assertEquals(6, patient.getIdentifier().size(), "Number of identifiers should be 6");
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
   * @param patient {@link Patient} containing identifiers
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
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a lastUpdated field set.
   */
  @Test
  public void transformSampleARecordWithLastUpdated() {
    Beneficiary beneficiary = loadSampleABeneficiary();

    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    beneficiary.setLastUpdated(Instant.now());
    Patient patientWithLastUpdated = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patientWithLastUpdated, requestHeader);

    beneficiary.setLastUpdated(Optional.empty());
    Patient patientWithoutLastUpdated =
        beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patientWithoutLastUpdated, requestHeader);
  }

  /**
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a lastUpdated field not set.
   */
  @Test
  public void transformSampleARecordWithoutLastUpdated() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    beneficiary.setLastUpdated(Optional.empty());
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a reference year field not found.
   */
  @Test
  public void transformSampleARecordWithoutReferenceYear() {
    Beneficiary beneficiary = loadSampleABeneficiary();
    beneficiary.setBeneEnrollmentReferenceYear(Optional.empty());
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    String url = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";
    Optional<Extension> ex =
        patient.getExtension().stream().filter(e -> url.equals(e.getUrl())).findFirst();

    assertEquals(true, ex.isEmpty());
  }

  /**
   * Loads and returns the sample a beneficiary.
   *
   * @return the {@link StaticRifResourceGroup#SAMPLE_A} {@link Beneficiary} record, with the {@link
   *     Beneficiary#getBeneficiaryHistories()} fields populated.
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
            .filter(r -> r.getBeneficiaryId() == beneficiary.getBeneficiaryId())
            .collect(Collectors.toSet());
    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);
    for (BeneficiaryHistory beneficiaryHistory : beneficiary.getBeneficiaryHistories()) {
      beneficiaryHistory.setHicnUnhashed(Optional.of(beneficiaryHistory.getHicn()));
      beneficiaryHistory.setHicn("someHICNhash");
    }
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
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

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
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
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
    Patient patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = beneficiaryTransformer.transform(beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that the {@link Patient} "looks like" it should, if it were produced from the
   * specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the {@link Patient} was generated from
   * @param patient the {@link Patient} that was generated from the specified {@link Beneficiary}
   * @param requestHeader the request header
   */
  static void assertMatches(
      Beneficiary beneficiary, Patient patient, RequestHeaders requestHeader) {
    TransformerTestUtils.assertNoEncodedOptionals(patient);

    assertEquals(
        String.valueOf(beneficiary.getBeneficiaryId()), patient.getIdElement().getIdPart());

    assertEquals(java.sql.Date.valueOf(beneficiary.getBirthDate()), patient.getBirthDate());

    if (beneficiary.getSex() == Sex.MALE.getCode()) {
      assertEquals(AdministrativeGender.MALE.toString(), patient.getGender().toString().trim());
      assertEquals(
          TransformerConstants.US_CORE_SEX_MALE,
          patient.getExtensionByUrl(TransformerConstants.US_CORE_SEX_URL).getValue().toString());
    } else if (beneficiary.getSex() == Sex.FEMALE.getCode()) {
      assertEquals(AdministrativeGender.FEMALE.toString(), patient.getGender().toString().trim());
      assertEquals(
          TransformerConstants.US_CORE_SEX_FEMALE,
          patient.getExtensionByUrl(TransformerConstants.US_CORE_SEX_URL).getValue().toString());
    }
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.RACE, beneficiary.getRace(), patient);
    assertEquals(beneficiary.getNameGiven(), patient.getName().get(0).getGiven().get(0).toString());
    if (beneficiary.getNameMiddleInitial().isPresent())
      assertEquals(
          beneficiary.getNameMiddleInitial().get().toString(),
          patient.getName().get(0).getGiven().get(1).toString());
    assertEquals(beneficiary.getNameSurname(), patient.getName().get(0).getFamily());

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      TransformerTestUtils.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), patient);
      addMedicaidDualEligibility(patient, beneficiary);
    }

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
   * Adds medicaid dual eligibility codes to the specified patient.
   *
   * @param patient the patient to add the codes to
   * @param beneficiary the beneficiary to read information from
   */
  private static void addMedicaidDualEligibility(Patient patient, Beneficiary beneficiary) {
    // Monthly Medicare-Medicaid dual eligibility codes
    if (beneficiary.getMedicaidDualEligibilityJanCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_01,
              beneficiary.getMedicaidDualEligibilityJanCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_02,
              beneficiary.getMedicaidDualEligibilityFebCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMarCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_03,
              beneficiary.getMedicaidDualEligibilityMarCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAprCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_04,
              beneficiary.getMedicaidDualEligibilityAprCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMayCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_05,
              beneficiary.getMedicaidDualEligibilityMayCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJunCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_06,
              beneficiary.getMedicaidDualEligibilityJunCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJulCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_07,
              beneficiary.getMedicaidDualEligibilityJulCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAugCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_08,
              beneficiary.getMedicaidDualEligibilityAugCode()));
    }
    if (beneficiary.getMedicaidDualEligibilitySeptCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_09,
              beneficiary.getMedicaidDualEligibilitySeptCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityOctCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_10,
              beneficiary.getMedicaidDualEligibilityOctCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityNovCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_11,
              beneficiary.getMedicaidDualEligibilityNovCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityDecCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_12,
              beneficiary.getMedicaidDualEligibilityDecCode()));
    }
  }

  /**
   * Gets a header with {@link PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to the
   * specified value.
   *
   * @param value of all include identifier values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldIdntityHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, value);
  }

  /**
   * Gets a header with {@link PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} set to
   * the specified value.
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldAddrFldHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }
}
