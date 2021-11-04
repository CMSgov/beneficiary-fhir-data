package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.Before;
import org.junit.Test;

/** Verifies that transformations that contain SAMHSA codes are filtered as expected. */
public final class SamhsaMatcherR4FromClaimTransformerV2Test {

  private R4SamhsaMatcher samhsaMatcherV2;
  private static final String CODING_SYSTEM_HCPCS_CD =
      TransformerConstants.BASE_URL_CCW_VARIABLES
          + "/"
          + CcwCodebookVariable.HCPCS_CD.getVariable().getId().toLowerCase();
  private static final String BLACKLISTED_HCPCS_CODE = "M1034";
  private static final String NON_SAMHSA_HCPCS_CODE = "11111";

  /** Sets up the test. */
  @Before
  public void setup() {
    samhsaMatcherV2 = new R4SamhsaMatcher();
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * TransformerConstants.CODING_SYSTEM_HCPCS) and the CPT code is blacklisted, the SAMHSA matcher's
   * test method will successfully identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedInpatientHasItemWithHcpcsCodeAndMatchingCptExpectMatch() {
    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifySamhsaMatcherForItem(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        BLACKLISTED_HCPCS_CODE,
        true,
        explanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * TransformerConstants.CODING_SYSTEM_HCPCS) and the code is not included in the blacklist, the
   * SAMHSA matcher's test method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedInpatientHasItemWithHcpcsCodeAndNonMatchingCptExpectNoMatch() {
    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifySamhsaMatcherForItem(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        NON_SAMHSA_HCPCS_CODE,
        false,
        explanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * CcwCodebookVariable.HCPCS_CD) and the CPT code is blacklisted the SAMHSA matcher's test method
   * will identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedInpatientHasItemWithHcpcsCdCodeAndMatchingCptExpectMatch() {
    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifySamhsaMatcherForItem(
        CODING_SYSTEM_HCPCS_CD, BLACKLISTED_HCPCS_CODE, true, explanationOfBenefit);
  }

  /**
   * Verifies that when transforming a SAMHSA claim into an ExplanationOfBenefit (where the
   * ExplanationOfBenefit then contains an item[n].productOrService.coding[n].system =
   * CcwCodebookVariable.HCPCS_CD) and the CPT code is not included in the blacklist the SAMHSA
   * matcher's test method will not identify this as a SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedInpatientHasItemWithHcpcsCdCodeAndNonMatchingCptExpectNoMatch() {
    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifySamhsaMatcherForItem(
        CODING_SYSTEM_HCPCS_CD, NON_SAMHSA_HCPCS_CODE, false, explanationOfBenefit);
  }

  /**
   * Verifies that when the item[n].productOrService.coding[n].system is an unknown/unexpected
   * value, the matcher will identify this as a SAMHSA related ExplanationOfBenefit using its
   * fallback logic to assume the claim is SAMHSA.
   */
  @Test
  public void
      testR4SamhsaMatcherWhenTransformedInpatientHasItemWithUnknownSystemExpectFallbackMatch() {
    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifySamhsaMatcherForItem(
        "unknknown/system/value", NON_SAMHSA_HCPCS_CODE, true, explanationOfBenefit);
  }

  /**
   * Verifies that when transforming a claim into an ExplanationOfBenefit which has no
   * item[n].productOrService.coding[n].system (procedure code) values which =
   * TransformerConstants.CODING_SYSTEM_HCPCS, has no eob.diagnosis SAMHSA code, and has no
   * eob.procedure SAMHSA code, then the SAMHSA matcher's test method will not identify this as a
   * SAMHSA related ExplanationOfBenefit.
   */
  @Test
  public void testR4SamhsaMatcherWhenTransformedInpatientHasItemWithNoHcpcsCodeExpectNoMatch() {

    // Given
    InpatientClaim claim = getInpatientClaim();
    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());

    // When/Then
    verifyNonSamhsaCodingDoesNotTriggerSamhsaFiltering(explanationOfBenefit);
  }

  /**
   * Verifies that a claim with no samhsa diagnosis, procedure, or item-level HCPCS codes does not
   * trigger filtering.
   *
   * @param explanationOfBenefit the loaded benefit to use for the test
   */
  private void verifyNonSamhsaCodingDoesNotTriggerSamhsaFiltering(
      ExplanationOfBenefit explanationOfBenefit) {
    // Set Top level diagnosis and package code to null and coding to empty
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent :
        explanationOfBenefit.getDiagnosis()) {
      diagnosisComponent.getDiagnosisCodeableConcept().setCoding(new ArrayList<>());
      diagnosisComponent.setPackageCode(null);
    }

    // Set procedure to empty
    explanationOfBenefit.setProcedure(new ArrayList<>());

    // Set item level codings to non-SAMHSA
    explanationOfBenefit
        .getItem()
        .get(0)
        .getProductOrService()
        .setCoding(Collections.singletonList(new Coding("Test/other/url", "2436467", "")));

    // When
    boolean isMatch = samhsaMatcherV2.test(explanationOfBenefit);

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
   */
  private void verifySamhsaMatcherForItem(
      String system, String code, boolean shouldMatch, ExplanationOfBenefit explanationOfBenefit) {

    // Set Top level diagnosis and package code to null so we can test item logic
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent :
        explanationOfBenefit.getDiagnosis()) {
      CodeableConcept codeableConcept = diagnosisComponent.getDiagnosisCodeableConcept();
      codeableConcept.setCoding(new ArrayList<>());
      diagnosisComponent.setPackageCode(null);
    }

    // Set item level code to the correct coding system
    explanationOfBenefit
        .getItem()
        .get(0)
        .getProductOrService()
        .getCoding()
        .get(0)
        .setSystem(system);
    // Set allowed CPT code
    explanationOfBenefit.getItem().get(0).getProductOrService().getCoding().get(0).setCode(code);

    assertEquals(shouldMatch, samhsaMatcherV2.test(explanationOfBenefit));
  }

  /**
   * Generates the Claim object to be used in a test.
   *
   * @return the inpatient claim to be used for the test
   */
  public InpatientClaim getInpatientClaim() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .orElse(null);

    if (claim != null) {
      claim.setLastUpdated(Instant.now());
    } else {
      throw new IllegalStateException(
          "Test setup issue, did not find expected InpatientClaim in sample record.");
    }

    return claim;
  }
}
