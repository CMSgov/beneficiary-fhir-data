package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.FDADrugTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.DMEClaimTransformer}. */
public final class DMEClaimTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.DMEClaimTransformer#transform(Object)} works as expected
   * when run against the {@link StaticRifResource#SAMPLE_A_DME} {@link DMEClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    DMEClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        DMEClaimTransformer.transform(
            new MetricRegistry(), claim, Optional.of(true), new FDADrugTestUtils());
    assertMatches(claim, eob, Optional.of(true));
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link DMEClaim}.
   *
   * @param claim the {@link DMEClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     DMEClaim}@param includedTaxNumbers whether or not to include tax numbers are expected to be
   *     included in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(
      DMEClaim claim, ExplanationOfBenefit eob, Optional<Boolean> includedTaxNumbers)
      throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
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
    assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

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
    if (includedTaxNumbers.orElse(false)) {
      assertNotNull(taxNumberCareTeamEntry);
    } else {
      assertNull(taxNumberCareTeamEntry);
    }

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
