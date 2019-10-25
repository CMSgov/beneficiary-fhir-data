package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.PartDEventTransformer}. */
public final class PartDEventTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.PartDEventTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_PDE} {@link PartDEvent}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    PartDEvent claim =
        parsedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();

    ExplanationOfBenefit eob = PartDEventTransformer.transform(new MetricRegistry(), claim);
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link PartDEvent}.
   *
   * @param claim the {@link PartDEvent} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     PartDEvent}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(PartDEvent claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getEventId(),
        claim.getBeneficiaryId(),
        ClaimType.PDE,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_D,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        claim.getFinalAction());

    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.PLAN_CNTRCT_REC_ID,
        claim.getPlanContractId(),
        eob.getInsurance().getCoverage());
    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.PLAN_PBP_REC_NUM,
        claim.getPlanBenefitPackageId(),
        eob.getInsurance().getCoverage());

    Assert.assertEquals("01", claim.getServiceProviderIdQualiferCode());
    Assert.assertEquals("01", claim.getPrescriberIdQualifierCode());

    ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

    TransformerTestUtils.assertHasCoding(
        TransformerConstants.CODING_NDC,
        null,
        TransformerUtils.retrieveFDADrugCodeDisplay(claim.getNationalDrugCode()),
        claim.getNationalDrugCode(),
        rxItem.getService().getCoding());

    TransformerTestUtils.assertHasCoding(
        V3ActCode.RXDINV.getSystem(),
        V3ActCode.RXDINV.toCode(),
        rxItem.getDetail().get(0).getType().getCoding());

    Assert.assertEquals(
        Date.valueOf(claim.getPrescriptionFillDate()), rxItem.getServicedDateType().getValue());

    TransformerTestUtils.assertReferenceEquals(
        TransformerConstants.CODING_NPI_US, claim.getServiceProviderId(), eob.getOrganization());
    TransformerTestUtils.assertReferenceEquals(
        TransformerConstants.CODING_NPI_US, claim.getServiceProviderId(), eob.getFacility());

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claim.getPharmacyTypeCode(), eob.getFacility());

    if (claim.getDrugCoverageStatusCode() == 'C')
      TransformerTestUtils.assertAdjudicationAmountEquals(
          CcwCodebookVariable.CVRD_D_PLAN_PD_AMT,
          claim.getPartDPlanCoveredPaidAmount(),
          rxItem.getAdjudication());
    else
      TransformerTestUtils.assertAdjudicationAmountEquals(
          CcwCodebookVariable.NCVRD_PLAN_PD_AMT,
          claim.getPartDPlanCoveredPaidAmount(),
          rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.PTNT_PAY_AMT, claim.getPatientPaidAmount(), rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.OTHR_TROOP_AMT,
        claim.getOtherTrueOutOfPocketPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LICS_AMT,
        claim.getLowIncomeSubsidyPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.PLRO_AMT,
        claim.getPatientLiabilityReductionOtherPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.TOT_RX_CST_AMT,
        claim.getTotalPrescriptionCost(),
        rxItem.getAdjudication());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.RPTD_GAP_DSCNT_NUM,
        claim.getGapDiscountAmount(),
        rxItem.getAdjudication());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.FILL_NUM, claim.getFillNumber(), rxItem.getQuantity());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.DAYS_SUPLY_NUM, claim.getDaysSupply(), rxItem.getQuantity());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.PDE,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PHARMACY),
        Optional.empty(),
        Optional.empty());

    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        claim.getDispenseAsWrittenProductSelectionCode(),
        eob);
    if (claim.getDispensingStatusCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          claim.getDispensingStatusCode(),
          eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claim.getDrugCoverageStatusCode(),
        eob);
    if (claim.getAdjustmentDeletionCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          claim.getAdjustmentDeletionCode(),
          eob);
    if (claim.getNonstandardFormatCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.NSTD_FRMT_CD,
          CcwCodebookVariable.NSTD_FRMT_CD,
          claim.getNonstandardFormatCode(),
          eob);
    if (claim.getPricingExceptionCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          claim.getPricingExceptionCode(),
          eob);
    if (claim.getCatastrophicCoverageCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          claim.getCatastrophicCoverageCode(),
          eob);
    if (claim.getPrescriptionOriginationCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.RX_ORGN_CD,
          CcwCodebookVariable.RX_ORGN_CD,
          claim.getPrescriptionOriginationCode(),
          eob);
    if (claim.getBrandGenericCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.BRND_GNRC_CD,
          CcwCodebookVariable.BRND_GNRC_CD,
          claim.getBrandGenericCode(),
          eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        claim.getPharmacyTypeCode(),
        eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.PTNT_RSDNC_CD,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        claim.getPatientResidenceCode(),
        eob);
    if (claim.getSubmissionClarificationCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.SUBMSN_CLR_CD,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          claim.getSubmissionClarificationCode(),
          eob);
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
    try {
      TransformerTestUtils.assertFDADrugCodeDisplayEquals(
          claim.getNationalDrugCode(), "Childrens Acetaminophen Cherry - ACETAMINOPHEN");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    try {
      TransformerTestUtils.assertNPICodeDisplayEquals(
          claim.getPrescriberId(), "DR. ROBERT BISBEE MD");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
