package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.InpatientClaimTransformer}. */
public final class InpatientClaimTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.InpatientClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_INPATIENT} {@link
   * InpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit eob =
        InpatientClaimTransformer.transform(new MetricRegistry(), claim, Optional.empty());
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link InpatientClaim}.
   *
   * @param claim the {@link InpatientClaim} that the {@link ExplanationOfBenefit} was generated
   *     from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     InpatientClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(InpatientClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.INPATIENT,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // test the common field provider number is set as expected in the EOB
    TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

    if (claim.getPatientStatusCd().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claim.getPatientStatusCd(),
          eob);

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_PASS_THRU_PER_DIEM_AMT, claim.getPassThruPerDiemAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT, claim.getProfessionalComponentCharge(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_TOT_PPS_CPTL_AMT, claim.getClaimTotalPPSCapitalAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.IME_OP_CLM_VAL_AMT, claim.getIndirectMedicalEducationAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.DSH_OP_CLM_VAL_AMT, claim.getDisproportionateShareAmount(), eob);

    // test common eob information between Inpatient, HHA, Hospice and SNF claims are set as
    // expected
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFEquals(
        eob,
        claim.getClaimAdmissionDate(),
        claim.getBeneficiaryDischargeDate(),
        Optional.of(claim.getUtilizationDayCount()));

    // test common benefit components between SNF and Inpatient claims are set as expected
    TransformerTestUtils.assertCommonGroupInpatientSNF(
        eob,
        claim.getCoinsuranceDayCount(),
        claim.getNonUtilizationDayCount(),
        claim.getDeductibleAmount(),
        claim.getPartACoinsuranceLiabilityAmount(),
        claim.getBloodPintsFurnishedQty(),
        claim.getNoncoveredCharge(),
        claim.getTotalDeductionAmount(),
        claim.getClaimPPSCapitalDisproportionateShareAmt(),
        claim.getClaimPPSCapitalExceptionAmount(),
        claim.getClaimPPSCapitalFSPAmount(),
        claim.getClaimPPSCapitalIMEAmount(),
        claim.getClaimPPSCapitalOutlierAmount(),
        claim.getClaimPPSOldCapitalHoldHarmlessAmount());

    // test common eob information between SNF and Inpatient claims are set as expected
    TransformerTestUtils.assertCommonEobInformationInpatientSNF(
        eob,
        claim.getNoncoveredStayFromDate(),
        claim.getNoncoveredStayThroughDate(),
        claim.getCoveredCareThoughDate(),
        claim.getMedicareBenefitsExhaustedDate(),
        claim.getDiagnosisRelatedGroupCd());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_DRG_OUTLIER_APRVD_PMT_AMT,
        claim.getDrgOutlierApprovedPaymentAmount(),
        eob);

    // Test to ensure common group fields between Inpatient, Outpatient and SNF
    // match
    TransformerTestUtils.assertEobCommonGroupInpOutSNFEquals(
        eob,
        claim.getBloodDeductibleLiabilityAmount(),
        claim.getOperatingPhysicianNpi(),
        claim.getOtherPhysicianNpi(),
        claim.getClaimQueryCode(),
        claim.getMcoPaidSw());

    // Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
    // and SNF match
    TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(
        eob,
        claim.getOrganizationNpi(),
        claim.getClaimFacilityTypeCode(),
        claim.getClaimFrequencyCode(),
        claim.getClaimNonPaymentReasonCode(),
        claim.getPatientDischargeStatusCode(),
        claim.getClaimServiceClassificationTypeCode(),
        claim.getClaimPrimaryPayerCode(),
        claim.getAttendingPhysicianNpi(),
        claim.getTotalChargeAmount(),
        claim.getPrimaryPayerPaidAmount(),
        claim.getFiscalIntermediaryNumber(),
        claim.getFiDocumentClaimControlNumber(),
        claim.getFiOriginalClaimControlNumber());

    Assert.assertEquals(9, eob.getDiagnosis().size());

    // test to ensure the diagnosis code display lookup table process works
    Optional<Diagnosis> diagnosis =
        Diagnosis.from(claim.getDiagnosis5Code(), claim.getDiagnosis5CodeVersion());
    TransformerTestUtils.assertHasCoding(
        diagnosis.get().getFhirSystem(),
        null,
        TransformerUtils.retrieveIcdCodeDisplay(diagnosis.get().getCode()),
        diagnosis.get().getCode(),
        eob.getDiagnosis().get(6).getDiagnosisCodeableConcept().getCoding());

    CCWProcedure ccwProcedure =
        new CCWProcedure(
            claim.getProcedure1Code(), claim.getProcedure1CodeVersion(), claim.getProcedure1Date());
    TransformerTestUtils.assertHasCoding(
        ccwProcedure.getFhirSystem().toString(),
        claim.getProcedure1Code().get(),
        eob.getProcedure().get(0).getProcedureCodeableConcept().getCoding());
    Assert.assertEquals(
        TransformerUtils.convertToDate(claim.getProcedure1Date().get()),
        eob.getProcedure().get(0).getDate());

    // test to ensure the procedure code display lookup table process works
    CCWProcedure ccwProcedureDisplay =
        new CCWProcedure(
            claim.getProcedure6Code(), claim.getProcedure6CodeVersion(), claim.getProcedure6Date());
    TransformerTestUtils.assertHasCoding(
        ccwProcedureDisplay.getFhirSystem().toString(),
        null,
        TransformerUtils.retrieveProcedureCodeDisplay(claim.getProcedure6Code().get()),
        claim.getProcedure6Code().get(),
        eob.getProcedure().get(5).getProcedureCodeableConcept().getCoding());

    Assert.assertEquals(1, eob.getItem().size());
    ItemComponent eobItem0 = eob.getItem().get(0);
    InpatientClaimLine claimLine1 = claim.getLines().get(0);
    Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

    Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        0 /* index */);

    // Test to ensure common group field coinsurance between Inpatient, HHA, Hospice and SNF match
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFCoinsuranceEquals(
        eobItem0, claimLine1.getDeductibleCoinsuranceCd());

    String claimControlNumber = "0000000000";

    // Test to ensure item level fields between Inpatient, Outpatient, HHA, Hopsice
    // and SNF match
    TransformerTestUtils.assertEobCommonItemRevenueEquals(
        eobItem0,
        eob,
        claimLine1.getRevenueCenter(),
        claimLine1.getRateAmount(),
        claimLine1.getTotalChargeAmount(),
        claimLine1.getNonCoveredChargeAmount(),
        claimLine1.getUnitCount(),
        claimControlNumber,
        claimLine1.getNationalDrugCodeQuantity(),
        claimLine1.getNationalDrugCodeQualifierCode(),
        claimLine1.getRevenueCenterRenderingPhysicianNPI(),
        0 /* index */);

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.INPATIENT,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
