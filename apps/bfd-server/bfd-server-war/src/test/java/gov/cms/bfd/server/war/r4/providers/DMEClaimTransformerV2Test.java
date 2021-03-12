package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.codesystems.ClaimCareteamrole;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.v4.providers.DMEClaimTransformerV2}. */
public final class DMEClaimTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static DMEClaim claim = null;

  @Before
  public void setup() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
  }

  @After
  public void tearDown() {
    claim = null;
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.DMEClaimTransformerV2#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_DME} {@link DMEClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  // @Ignore
  @Test
  public void outputSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob = DMEClaimTransformerV2.transform(new MetricRegistry(), claim);
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
    assertMatches(1, 1);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.DMEClaimTransformerV2#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_DME} {@link DMEClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob = DMEClaimTransformerV2.transform(new MetricRegistry(), claim);
    assertMatches(claim, eob);
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
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.DME,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // Test to ensure common group fields between Carrier and DME match
    TransformerTestUtilsV2.assertEobCommonGroupCarrierDMEEquals(
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

    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, claim.getPrimaryPayerPaidAmount(), eob);

    Assert.assertEquals(3, eob.getDiagnosis().size());
    Assert.assertEquals(1, eob.getItem().size());
    ItemComponent eobItem0 = eob.getItem().get(0);
    DMEClaimLine claimLine1 = claim.getLines().get(0);
    Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

    TransformerTestUtilsV2.assertExtensionIdentifierEquals(
        CcwCodebookVariable.SUPLRNUM, claimLine1.getProviderBillingNumber(), eobItem0);

    TransformerTestUtilsV2.assertCareTeamEquals(
        claimLine1.getProviderNPI().get(), ClaimCareteamrole.PRIMARY, eob);
    CareTeamComponent performingCareTeamEntry =
        TransformerTestUtilsV2.findCareTeamEntryForProviderIdentifier(
            claimLine1.getProviderNPI().get(), eob.getCareTeam());
    TransformerTestUtilsV2.assertHasCoding(
        CcwCodebookVariable.PRVDR_SPCLTY,
        claimLine1.getProviderSpecialityCode(),
        performingCareTeamEntry.getQualification());
    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.PRTCPTNG_IND_CD,
        claimLine1.getProviderParticipatingIndCode(),
        performingCareTeamEntry);

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.PRVDR_STATE_CD,
        claimLine1.getProviderStateCode(),
        eobItem0.getLocation());

    TransformerTestUtilsV2.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        claim.getHcpcsYearCode(),
        0 /* index */);
    TransformerTestUtilsV2.assertHasCoding(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        "" + claim.getHcpcsYearCode().get(),
        null,
        claimLine1.getHcpcsCode().get(),
        eobItem0.getService().getCoding());

    TransformerTestUtilsV2.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT,
        claimLine1.getPrimaryPayerAllowedChargeAmount(),
        eobItem0.getAdjudication());

    TransformerTestUtilsV2.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
        claimLine1.getPurchasePriceAmount(),
        eobItem0.getAdjudication());

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
        claimLine1.getPricingStateCode(),
        eobItem0.getLocation());

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
        claimLine1.getSupplierTypeCode(),
        eobItem0.getLocation());

    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT,
        claimLine1.getScreenSavingsAmount(),
        eobItem0);

    TransformerTestUtilsV2.assertQuantityUnitInfoEquals(
        CcwCodebookVariable.DMERC_LINE_MTUS_CNT,
        CcwCodebookVariable.DMERC_LINE_MTUS_CD,
        claimLine1.getMtusCode(),
        eobItem0);

    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookVariable.DMERC_LINE_MTUS_CNT, claimLine1.getMtusCount(), eobItem0);

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        eobItem0,
        TransformerConstants.CODING_NDC,
        TransformerConstants.CODING_NDC,
        claimLine1.getNationalDrugCode().get());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtilsV2.assertMapEobType(
        eob.getType(),
        ClaimType.DME,
        // FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
        // for this claim type. If added then the Optional empty parameter below should
        // be updated to match expected result.
        Optional.empty(),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test to ensure common item fields between Carrier and DME match
    TransformerTestUtilsV2.assertEobCommonItemCarrierDMEEquals(
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
    TransformerTestUtilsV2.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
