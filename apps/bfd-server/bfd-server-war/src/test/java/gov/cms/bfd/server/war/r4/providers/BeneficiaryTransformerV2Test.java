package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.collection.IsEmptyCollection;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}. */
public final class BeneficiaryTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static Beneficiary beneficiary = null;

  @Before
  public void setup() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    beneficiary.setMbiHash(Optional.of("someMBIhash"));

    // Add the history records to the Beneficiary, but nill out the HICN fields.
    Set<BeneficiaryHistory> beneficiaryHistories =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId()))
            .collect(Collectors.toSet());

    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);
    /*
    for (BeneficiaryHistory beneficiaryHistory : beneficiary.getBeneficiaryHistories()) {
      beneficiaryHistory.setHicnUnhashed(null);
      beneficiaryHistory.setHicn(null);
    }
    */

    // Add the MBI history records to the Beneficiary.
    Set<MedicareBeneficiaryIdHistory> beneficiaryMbis =
        parsedRecords.stream()
            .filter(r -> r instanceof MedicareBeneficiaryIdHistory)
            .map(r -> (MedicareBeneficiaryIdHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId().orElse(null)))
            .collect(Collectors.toSet());
    beneficiary.getMedicareBeneficiaryIdHistories().addAll(beneficiaryMbis);
    assertThat(beneficiary, is(notNullValue()));
  }

  @After
  public void tearDown() {
    beneficiary = null;
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer} works
   * correctly when passed a {@link Beneficiary} where all {@link Optional} fields are set to {@link
   * Optional#empty()}.
   */
  @Test
  public void transformBeneficiaryWithAllOptionalsEmpty() {
    TransformerTestUtilsV2.setAllOptionalsToEmpty(beneficiary);
    // TODO - enable the next line when a PatientResourceProviderITV2 is available.
    /*
    RequestHeaders requestHeader = getRHwithIncldIdntityHdr("");
    Patient patient =
        BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
    */
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
    TransformerTestUtilsV2.setAllOptionalsToEmpty(beneficiary);
    RequestHeaders requestHeader = getRHwithIncldAddrFldHdr("true");
    Patient patient =
        BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer} works
   * correctly when passed a {@link Beneficiary} with various includeAddressFields header values.
   */
  @Test
  public void transformSampleARecordWithIncludeAddressFields() {
    RequestHeaders requestHeader = getRHwithIncldAddrFldHdr("true");
    Patient patient =
        BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = getRHwithIncldAddrFldHdr("false");
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = getRHwithIncldAddrFldHdr("");
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);

    requestHeader = RequestHeaders.getHeaderWrapper();
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);
    assertMatches(beneficiary, patient, requestHeader);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary}.
   */
  @Test
  public void transformSampleARecord() {

    RequestHeaders requestHeader = getRHwithIncldIdentityHdr("true");
    Patient patient =
        BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);

    // System.out.println(fhirContext.newJsonParser().encodeResourceToString(patient));
    assertThat(patient.getIdentifier(), not(IsEmptyCollection.empty()));
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
    TransformerTestUtilsV2.assertNoEncodedOptionals(patient);

    Assert.assertEquals(beneficiary.getBeneficiaryId(), patient.getIdElement().getIdPart());

    Assert.assertEquals(java.sql.Date.valueOf(beneficiary.getBirthDate()), patient.getBirthDate());

    if (beneficiary.getSex() == Sex.MALE.getCode()) {
      Assert.assertEquals(
          AdministrativeGender.MALE.toString(), patient.getGender().toString().trim());
    } else if (beneficiary.getSex() == Sex.FEMALE.getCode()) {
      Assert.assertEquals(
          AdministrativeGender.FEMALE.toString(), patient.getGender().toString().trim());
    }

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.RACE, beneficiary.getRace(), patient);
    Assert.assertEquals(
        beneficiary.getNameGiven(), patient.getName().get(0).getGiven().get(0).toString());
    if (beneficiary.getNameMiddleInitial().isPresent()) {
      Assert.assertEquals(
          beneficiary.getNameMiddleInitial().get().toString(),
          patient.getName().get(0).getGiven().get(1).toString());
    }
    Assert.assertEquals(beneficiary.getNameSurname(), patient.getName().get(0).getFamily());

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), patient);
    }
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      TransformerTestUtilsV2.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), patient);
    }

    TransformerTestUtilsV2.assertLastUpdatedEquals(beneficiary.getLastUpdated(), patient);

    Boolean inclAddrFlds =
        (Boolean)
            requestHeader.getValue(R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS);

    if (inclAddrFlds != null && inclAddrFlds) {
      Assert.assertEquals(1, patient.getAddress().size());
      // assert address fields etc.
      Assert.assertEquals(beneficiary.getStateCode(), patient.getAddress().get(0).getState());
      // assert CountyCode is no longer mapped
      Assert.assertNull(patient.getAddress().get(0).getDistrict());
      Assert.assertEquals(beneficiary.getPostalCode(), patient.getAddress().get(0).getPostalCode());
      Assert.assertEquals(
          beneficiary.getDerivedCityName().orElse(null), patient.getAddress().get(0).getCity());

      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress1().orElse(""),
          patient.getAddress().get(0).getLine().get(0).getValueNotNull());
      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress2().orElse(""),
          patient.getAddress().get(0).getLine().get(1).getValueNotNull());
      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress3().orElse(""),
          patient.getAddress().get(0).getLine().get(2).getValueNotNull());
      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress4().orElse(""),
          patient.getAddress().get(0).getLine().get(3).getValueNotNull());
      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress5().orElse(""),
          patient.getAddress().get(0).getLine().get(4).getValueNotNull());
      Assert.assertEquals(
          beneficiary.getDerivedMailingAddress6().orElse(""),
          patient.getAddress().get(0).getLine().get(5).getValueNotNull());
    } else {
      Assert.assertEquals(1, patient.getAddress().size());
      Assert.assertEquals(beneficiary.getStateCode(), patient.getAddress().get(0).getState());
      // assert CountyCode is no longer mapped
      Assert.assertNull(patient.getAddress().get(0).getDistrict());
      Assert.assertEquals(beneficiary.getPostalCode(), patient.getAddress().get(0).getPostalCode());
      // assert address city name and line 0 - 5 fields etc.
      Assert.assertNull(patient.getAddress().get(0).getCity());
      Assert.assertEquals(0, patient.getAddress().get(0).getLine().size());
    }
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
    Assert.assertEquals(
        "Identifier "
            + identifierSystem
            + " value = "
            + identifierValue
            + " does not match an expected value.",
        identifierFound,
        true);
  }

  /**
   * @return the {@link StaticRifResourceGroup#SAMPLE_A} {@link Beneficiary} record, with the {@link
   *     Beneficiary#getBeneficiaryHistories()} and {@link
   *     Beneficiary#getMedicareBeneficiaryIdHistories()} fields populated.
   */

  /**
   * test helper
   *
   * @param value of all include identifier values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldIdentityHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, value);
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
}
