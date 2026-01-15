package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.DMEClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class DMEClaimTransformerTest {
  /** The transformer under test. */
  DMEClaimTransformer dmeClaimTransformer;

  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;

  /** The securityTagManager. */
  @Mock SecurityTagManager securityTagManager;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /** One-time setup of objects that are normally injected. */
  @BeforeEach
  protected void setup() {
    TransformerTestUtils.touch();
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    dmeClaimTransformer = new DMEClaimTransformer(metricRegistry, securityTagManager, false);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    DMEClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .orElseThrow();

    dmeClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = dmeClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_A_DME} {@link DMEClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    DMEClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        dmeClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    assertMatches(claim, eob);
  }

  /**
   * Tests that the transformer sets the expected values for the care team member extensions and
   * does not error when only the required care team values exist.
   */
  @Test
  public void testCareTeamExtensionsWhenOptionalValuesAbsent() {

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    DMEClaim loadedClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .get();
    loadedClaim.setLastUpdated(Instant.now());

    // Set the optional care team fields to empty
    for (DMEClaimLine line : loadedClaim.getLines()) {
      line.setProviderParticipatingIndCode(Optional.empty());
      line.setProviderSpecialityCode(Optional.empty());
    }

    ExplanationOfBenefit genEob =
        dmeClaimTransformer.transform(new ClaimWithSecurityTags<>(loadedClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());

    // Ensure the extension for PRTCPTNG_IND_CD wasnt added
    // Also the qualification coding should be empty if specialty code is not set
    String prtIndCdUrl =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PRTCPTNG_IND_CD);
    for (ExplanationOfBenefit.CareTeamComponent careTeam : genEob.getCareTeam()) {
      assertFalse(careTeam.getExtension().stream().anyMatch(i -> i.getUrl().equals(prtIndCdUrl)));
      assertTrue(careTeam.getQualification().getCoding().isEmpty());
    }
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link DMEClaim}.
   *
   * @param claim the {@link DMEClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     DMEClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(DMEClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.DME,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // Test to ensure common group fields between Carrier and DME match
    TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(
        eob,
        claim.getBeneficiaryId(),
        claim.getCarrierNumber(),
        claim.getClinicalTrialNumber(),
        claim.getBeneficiaryPartBDeductAmount(),
        claim.getPaymentDenialCode(),
        claim.getReferringPhysicianNpi(),
        Optional.of(claim.getProviderAssignmentIndicator()),
        claim.getProviderPaymentAmount(),
        claim.getBeneficiaryPaymentAmount(),
        claim.getSubmittedChargeAmount(),
        claim.getAllowedChargeAmount());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, claim.getPrimaryPayerPaidAmount(), eob);

    assertEquals(3, eob.getDiagnosis().size());
    assertEquals(1, eob.getItem().size());
    ItemComponent eobItem0 = eob.getItem().get(0);
    DMEClaimLine claimLine1 = claim.getLines().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());

    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.SUPLRNUM, claimLine1.getProviderBillingNumber(), eobItem0);

    TransformerTestUtils.assertCareTeamEquals(
        claimLine1.getProviderNPI().get(), ClaimCareteamrole.PRIMARY, eob);
    CareTeamComponent performingCareTeamEntry =
        TransformerTestUtils.findCareTeamEntryForProviderNpi(
            claimLine1.getProviderNPI().get(), eob.getCareTeam());
    TransformerTestUtils.assertHasCoding(
        CcwCodebookVariable.PRVDR_SPCLTY,
        claimLine1.getProviderSpecialityCode(),
        performingCareTeamEntry.getQualification());
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PRTCPTNG_IND_CD,
        claimLine1.getProviderParticipatingIndCode(),
        performingCareTeamEntry);

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PRVDR_STATE_CD,
        claimLine1.getProviderStateCode(),
        eobItem0.getLocation());

    CareTeamComponent taxNumberCareTeamEntry =
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            claimLine1.getProviderTaxNumber(), eob.getCareTeam());
    // We assert that tax number entries are always null as of 01/26/BFD-4489 to ensure tax numbers
    // are never included
    assertNull(taxNumberCareTeamEntry);

    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        claim.getHcpcsYearCode(),
        0 /* index */);
    TransformerTestUtils.assertHasCoding(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        "" + claim.getHcpcsYearCode().get(),
        null,
        claimLine1.getHcpcsCode().get(),
        eobItem0.getService().getCoding());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT,
        claimLine1.getPrimaryPayerAllowedChargeAmount(),
        eobItem0.getAdjudication());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
        claimLine1.getPurchasePriceAmount(),
        eobItem0.getAdjudication());

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
        claimLine1.getPricingStateCode(),
        eobItem0.getLocation());

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
        claimLine1.getSupplierTypeCode(),
        eobItem0.getLocation());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT,
        claimLine1.getScreenSavingsAmount(),
        eobItem0);

    TransformerTestUtils.assertQuantityUnitInfoEquals(
        CcwCodebookVariable.DMERC_LINE_MTUS_CNT,
        CcwCodebookVariable.DMERC_LINE_MTUS_CD,
        claimLine1.getMtusCode(),
        eobItem0);

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.DMERC_LINE_MTUS_CNT, claimLine1.getMtusCount(), eobItem0);

    TransformerTestUtils.assertExtensionCodingEquals(
        eobItem0,
        TransformerConstants.CODING_NDC,
        TransformerConstants.CODING_NDC,
        claimLine1.getNationalDrugCode().get());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.DME,
        // FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
        // for this claim type. If added then the Optional empty parameter below should
        // be updated to match expected result.
        Optional.empty(),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test to ensure common item fields between Carrier and DME match
    TransformerTestUtils.assertEobCommonItemCarrierDMEEquals(
        eobItem0,
        eob,
        claimLine1.getServiceCount(),
        claimLine1.getPlaceOfServiceCode(),
        claimLine1.getFirstExpenseDate(),
        claimLine1.getLastExpenseDate(),
        claimLine1.getBeneficiaryPaymentAmount(),
        claimLine1.getProviderPaymentAmount(),
        claimLine1.getBeneficiaryPartBDeductAmount(),
        claimLine1.getPrimaryPayerCode(),
        claimLine1.getPrimaryPayerPaidAmount(),
        claimLine1.getBetosCode(),
        claimLine1.getPaymentAmount(),
        claimLine1.getPaymentCode(),
        claimLine1.getCoinsuranceAmount(),
        claimLine1.getSubmittedChargeAmount(),
        claimLine1.getAllowedChargeAmount(),
        claimLine1.getProcessingIndicatorCode(),
        claimLine1.getServiceDeductibleCode(),
        claimLine1.getDiagnosisCode(),
        claimLine1.getDiagnosisCodeVersion(),
        claimLine1.getHctHgbTestTypeCode(),
        claimLine1.getHctHgbTestResult(),
        claimLine1.getCmsServiceTypeCode(),
        claimLine1.getNationalDrugCode());

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
