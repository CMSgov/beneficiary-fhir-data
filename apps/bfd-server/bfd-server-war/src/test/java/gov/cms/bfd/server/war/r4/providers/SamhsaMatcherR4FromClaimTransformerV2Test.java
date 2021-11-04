package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.RifRecordBase;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Verifies that transformations that contain SAMHSA codes are filtered as expected. */
@RunWith(Parameterized.class)
public class SamhsaMatcherR4FromClaimTransformerV2Test {

  private R4SamhsaMatcher samhsaMatcherV2;
  private static final String CODING_SYSTEM_HCPCS_CD =
      TransformerConstants.BASE_URL_CCW_VARIABLES
          + "/"
          + CcwCodebookVariable.HCPCS_CD.getVariable().getId().toLowerCase();
  private static final String BLACKLISTED_HCPCS_CODE = "M1034";
  private static final String NON_SAMHSA_HCPCS_CODE = "11111";
  private static final String BLACKLISTED_IC9_DIAGNOSIS_CODE = "291.0";
  private static final String BLACKLISTED_IC10_DIAGNOSIS_CODE = "F10.10";
  private static final String NON_BLACKLISTED_IC_CODE = "111111";

  private final RifRecordBase claim;
  private final ExplanationOfBenefit loadedExplanationOfBenefit;

  /** Sets up the test. */
  @Before
  public void setup() {
    samhsaMatcherV2 = new R4SamhsaMatcher();
  }

  /**
   * Data collection.
   *
   * @return the collection
   */
  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    // Load and transform the various claim types for testing
    RifRecordBase inpatientClaim = getClaim(InpatientClaim.class);
    ExplanationOfBenefit inpatientEob =
        InpatientClaimTransformerV2.transform(
            new MetricRegistry(), inpatientClaim, Optional.empty());

    RifRecordBase outpatientClaim = getClaim(OutpatientClaim.class);
    ExplanationOfBenefit outpatientEob =
        OutpatientClaimTransformerV2.transform(
            new MetricRegistry(), outpatientClaim, Optional.empty());

    RifRecordBase dmeClaim = getClaim(DMEClaim.class);
    ExplanationOfBenefit dmeEob =
        DMEClaimTransformerV2.transform(new MetricRegistry(), dmeClaim, Optional.empty());

    RifRecordBase hhaClaim = getClaim(HHAClaim.class);
    ExplanationOfBenefit hhaEob =
        HHAClaimTransformerV2.transform(new MetricRegistry(), hhaClaim, Optional.empty());

    RifRecordBase hospiceClaim = getClaim(HospiceClaim.class);
    ExplanationOfBenefit hospiceEob =
        HospiceClaimTransformerV2.transform(new MetricRegistry(), hospiceClaim, Optional.empty());

    RifRecordBase snfClaim = getClaim(SNFClaim.class);
    ExplanationOfBenefit snfEob =
        SNFClaimTransformerV2.transform(new MetricRegistry(), snfClaim, Optional.empty());

    RifRecordBase carrierClaim = getClaim(CarrierClaim.class);
    ExplanationOfBenefit carrierEob =
        CarrierClaimTransformerV2.transform(new MetricRegistry(), carrierClaim, Optional.empty());

    RifRecordBase pdeClaim = getClaim(PartDEvent.class);
    ExplanationOfBenefit pdeEob =
        PartDEventTransformerV2.transform(new MetricRegistry(), pdeClaim, Optional.empty());

    // Load the claim types into the test data that will be run against each test
    return Arrays.asList(
        new Object[][] {
          {inpatientClaim, inpatientEob},
          {outpatientClaim, outpatientEob},
          {dmeClaim, dmeEob},
          {hhaClaim, hhaEob},
          {hospiceClaim, hospiceEob},
          {snfClaim, snfEob},
          {carrierClaim, carrierEob},
          {pdeClaim, pdeEob}
        });
  }

  /**
   * Creates a new test with the specified parameter.
   *
   * @param explanationOfBenefit the explanation of benefit to use
   */
  public SamhsaMatcherR4FromClaimTransformerV2Test(
      RifRecordBase claim, ExplanationOfBenefit explanationOfBenefit) {
    this.claim = claim;
    this.loadedExplanationOfBenefit = explanationOfBenefit;
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * TransformerConstants.CODING_SYSTEM_HCPCS) and the CPT code is blacklisted, the SAMHSA matcher's
   * test method will successfully identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithHcpcsCodeAndMatchingCptExpectMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    // When/Then
    verifySamhsaMatcherForItem(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        BLACKLISTED_HCPCS_CODE,
        expectMatch,
        loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * TransformerConstants.CODING_SYSTEM_HCPCS) and the code is not included in the blacklist, the
   * SAMHSA matcher's test method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithHcpcsCodeAndNonMatchingCptExpectNoMatch() {
    // When/Then/
    verifySamhsaMatcherForItem(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        NON_SAMHSA_HCPCS_CODE,
        false,
        loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * CcwCodebookVariable.HCPCS_CD) and the CPT code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithHcpcsCdCodeAndMatchingCptExpectMatch() {
    // Only inpatient claims will match HCPCS_CD
    boolean expectMatch = claim instanceof InpatientClaim;

    // When/Then
    verifySamhsaMatcherForItem(
        CODING_SYSTEM_HCPCS_CD, BLACKLISTED_HCPCS_CODE, expectMatch, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * CcwCodebookVariable.HCPCS_CD) and the CPT code is not included in the blacklist the SAMHSA
   * matcher's test method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithHcpcsCdCodeAndNonMatchingCptExpectNoMatch() {
    // When/Then
    verifySamhsaMatcherForItem(
        CODING_SYSTEM_HCPCS_CD, NON_SAMHSA_HCPCS_CODE, false, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when the item[n].productOrService.coding[n].system is an unknown/unexpected
   * value, the matcher will identify this as a SAMHSA related ExplanationOfBenefit using its
   * fallback logic to assume the claim is SAMHSA.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithUnknownSystemExpectFallbackMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    verifySamhsaMatcherForItem(
        "unknknown/system/value", NON_SAMHSA_HCPCS_CODE, expectMatch, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a claim into an ExplanationOfBenefit which has no
   * item[n].productOrService.coding[n].system (procedure code) values which =
   * TransformerConstants.CODING_SYSTEM_HCPCS, has no eob.diagnosis SAMHSA code, and has no
   * eob.procedure SAMHSA code, then the SAMHSA matcher's test method will not identify this as a
   * SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithNoHcpcsCodeExpectNoMatch() {
    // When/Then
    verifyNonSamhsaCodingDoesNotTriggerSamhsaFiltering(loadedExplanationOfBenefit);
  }

  /**
   * Verifies that a claim with no samhsa diagnosis, procedure, or item-level HCPCS codes does not
   * trigger filtering.
   *
   * @param explanationOfBenefit the loaded benefit to use for the test
   */
  private void verifyNonSamhsaCodingDoesNotTriggerSamhsaFiltering(
      ExplanationOfBenefit explanationOfBenefit) {

    ExplanationOfBenefit modifiedEob = explanationOfBenefit.copy();

    // Set Top level diagnosis and package code to null and coding to empty
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent : modifiedEob.getDiagnosis()) {
      diagnosisComponent.getDiagnosisCodeableConcept().setCoding(new ArrayList<>());
      diagnosisComponent.setPackageCode(null);
    }

    // Set procedure to empty
    modifiedEob.setProcedure(new ArrayList<>());

    // Set item level codings to non-SAMHSA
    modifiedEob
        .getItem()
        .get(0)
        .getProductOrService()
        .setCoding(Collections.singletonList(new Coding("Test/other/url", "2436467", "")));

    // When
    boolean isMatch = samhsaMatcherV2.test(modifiedEob);

    // Then
    assertFalse(isMatch);
  }

  /**
   * Verify SAMHSA matcher for item with the given system, code and if the expectation is that there
   * should be a match for this combination.
   *
   * @param system the system value
   * @param code the code
   * @param shouldMatch if the matcher should match on this combination
   * @param explanationOfBenefit the explanation of benefit
   */
  private void verifySamhsaMatcherForItem(
      String system, String code, boolean shouldMatch, ExplanationOfBenefit explanationOfBenefit) {

    ExplanationOfBenefit modifiedEob = explanationOfBenefit.copy();

    // Set Top level diagnosis and package code to null so we can test item logic
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent : modifiedEob.getDiagnosis()) {
      CodeableConcept codeableConcept = diagnosisComponent.getDiagnosisCodeableConcept();
      codeableConcept.setCoding(new ArrayList<>());
      diagnosisComponent.setPackageCode(null);
    }

    // Set item level code to the correct coding system
    modifiedEob.getItem().get(0).getProductOrService().getCoding().get(0).setSystem(system);
    // Set CPT code
    modifiedEob.getItem().get(0).getProductOrService().getCoding().get(0).setCode(code);

    assertEquals(shouldMatch, samhsaMatcherV2.test(modifiedEob));
  }

  /**
   * Verify SAMHSA matcher for ICD item with the given system, code and if the expectation is that
   * there should be a match for this combination.
   *
   * @param system the system value
   * @param code the code
   * @param shouldMatch if the matcher should match on this combination
   */
  private void verifySamhsaMatcherForIcd(
      String system, String code, boolean shouldMatch, ExplanationOfBenefit explanationOfBenefit) {

    ExplanationOfBenefit modifiedEob = explanationOfBenefit.copy();

    // Set Top level diagnosis and package code to null so we can test item logic
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent : modifiedEob.getDiagnosis()) {
      CodeableConcept codeableConcept = diagnosisComponent.getDiagnosisCodeableConcept();
      ArrayList<Coding> codingList = new ArrayList<>();
      codingList.add(new Coding().setSystem(system).setCode(code));

      codeableConcept.setCoding(codingList);
      diagnosisComponent.setPackageCode(null);
    }

    // Set item coding to empty so we dont check it for matches
    modifiedEob.getItem().get(0).getProductOrService().setCoding(new ArrayList<>());

    assertEquals(shouldMatch, samhsaMatcherV2.test(modifiedEob));
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_9) and the ICD code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithBlacklistedIcd9CodeExpectMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    // When/Then
    verifySamhsaMatcherForIcd(
        IcdCode.CODING_SYSTEM_ICD_9,
        BLACKLISTED_IC9_DIAGNOSIS_CODE,
        expectMatch,
        loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_9) and the ICD code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithUnknownIcd9SystemExpectMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    verifySamhsaMatcherForIcd(
        "not valid icd9 system", NON_BLACKLISTED_IC_CODE, expectMatch, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_9) and the ICD code is not blacklisted the SAMHSA matcher's test
   * method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithNonBlacklistedIcd9CodeExpectNoMatch() {
    // When/Then
    verifySamhsaMatcherForIcd(
        IcdCode.CODING_SYSTEM_ICD_9, NON_BLACKLISTED_IC_CODE, false, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_10) and the ICD code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithBlacklistedIcd10CodeExpectMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    // When/Then
    verifySamhsaMatcherForIcd(
        IcdCode.CODING_SYSTEM_ICD_10,
        BLACKLISTED_IC10_DIAGNOSIS_CODE,
        expectMatch,
        loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_10) and the ICD code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedClaimHasItemWithUnknownIcd10SystemExpectMatch() {
    boolean expectMatch = true;

    // PDE has no SAMHSA, so expect no match on SAMSHA filter
    if (claim instanceof PartDEvent) {
      expectMatch = false;
    }

    // When/Then
    verifySamhsaMatcherForIcd(
        "not valid icd10 system", NON_BLACKLISTED_IC_CODE, expectMatch, loadedExplanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * IcdCode.CODING_SYSTEM_ICD_10) and the ICD code is not blacklisted the SAMHSA matcher's test
   * method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedClaimHasItemWithNonBlacklistedIcd10CodeExpectNoMatch() {
    // When/Then
    verifySamhsaMatcherForIcd(
        IcdCode.CODING_SYSTEM_ICD_10, NON_BLACKLISTED_IC_CODE, false, loadedExplanationOfBenefit);
  }

  /**
   * Generates the Claim object to be used in a test.
   *
   * @param type the type
   * @return the claim to be used for the test, should match the input type
   */
  public static RifRecordBase getClaim(Class<? extends RifRecordBase> type) {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    RifRecordBase claim =
        parsedRecords.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);

    if (claim != null) {
      claim.setLastUpdated(Instant.now());
    } else {
      throw new IllegalStateException(
          "Test setup issue, did not find expected InpatientClaim in sample record.");
    }

    return claim;
  }
}
