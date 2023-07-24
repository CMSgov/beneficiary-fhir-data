package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaimLine;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerContext;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Collections;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;

/**
 * Transforms CCW {@link InpatientClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
final class InpatientClaimTransformer {
  /**
   * Transforms a specified claim into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param transformerContext the {@link TransformerContext} to use
   * @param claim the {@link Object} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(TransformerContext transformerContext, Object claim) {
    Timer.Context timer =
        transformerContext
            .getMetricRegistry()
            .timer(
                MetricRegistry.name(InpatientClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof InpatientClaim)) throw new BadCodeMonkeyException();
    ExplanationOfBenefit eob = transformClaim((InpatientClaim) claim, transformerContext);

    timer.stop();
    return eob;
  }

  /**
   * Transforms a specified {@link InpatientClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link InpatientClaim} to transform
   * @param transformerContext the transformer context
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  private static ExplanationOfBenefit transformClaim(
      InpatientClaim claimGroup, TransformerContext transformerContext) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.INPATIENT,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    TransformerUtils.mapEobWeeklyProcessDate(eob, claimGroup.getWeeklyProcessDate());

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(
        eob,
        ClaimType.INPATIENT,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

    if (claimGroup.getPatientStatusCd().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claimGroup.getPatientStatusCd());
    }

    // add EOB information to fields that are common between the Inpatient and SNF claim types
    TransformerUtils.addCommonEobInformationInpatientSNF(
        eob,
        claimGroup.getAdmissionTypeCd(),
        claimGroup.getSourceAdmissionCd(),
        claimGroup.getNoncoveredStayFromDate(),
        claimGroup.getNoncoveredStayThroughDate(),
        claimGroup.getCoveredCareThoughDate(),
        claimGroup.getMedicareBenefitsExhaustedDate(),
        claimGroup.getDiagnosisRelatedGroupCd());

    // Claim Operational Indirect Medical Education Amount
    if (claimGroup.getIndirectMedicalEducationAmount().isPresent()) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.IME_OP_CLM_VAL_AMT,
          claimGroup.getIndirectMedicalEducationAmount());
    }

    // Claim Operational disproportionate Amount
    if (claimGroup.getDisproportionateShareAmount().isPresent()) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.DSH_OP_CLM_VAL_AMT, claimGroup.getDisproportionateShareAmount());
    }

    // TODO If actually nullable, should be Optional.
    if (claimGroup.getPassThruPerDiemAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.CLM_PASS_THRU_PER_DIEM_AMT,
          claimGroup.getPassThruPerDiemAmount());
    }

    // TODO If actually nullable, should be Optional.
    if (claimGroup.getProfessionalComponentCharge() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT,
          claimGroup.getProfessionalComponentCharge());
    }

    // TODO If actually nullable, should be Optional.
    if (claimGroup.getClaimTotalPPSCapitalAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.CLM_TOT_PPS_CPTL_AMT,
          claimGroup.getClaimTotalPPSCapitalAmount());
    }

    if (claimGroup.getIndirectMedicalEducationAmount().isPresent()) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.IME_OP_CLM_VAL_AMT,
          claimGroup.getIndirectMedicalEducationAmount().get());
    }

    // Claim Uncompensated Care Payment Amount
    if (claimGroup.getClaimUncompensatedCareAmount().isPresent()) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.CLM_UNCOMPD_CARE_PMT_AMT,
          claimGroup.getClaimUncompensatedCareAmount().get());
    }

    /*
     * add field values to the benefit balances that are common between the
     * Inpatient and SNF claim types
     */
    TransformerUtils.addCommonGroupInpatientSNF(
        eob,
        claimGroup.getCoinsuranceDayCount(),
        claimGroup.getNonUtilizationDayCount(),
        claimGroup.getDeductibleAmount(),
        claimGroup.getPartACoinsuranceLiabilityAmount(),
        claimGroup.getBloodPintsFurnishedQty(),
        claimGroup.getNoncoveredCharge(),
        claimGroup.getTotalDeductionAmount(),
        claimGroup.getClaimPPSCapitalDisproportionateShareAmt(),
        claimGroup.getClaimPPSCapitalExceptionAmount(),
        claimGroup.getClaimPPSCapitalFSPAmount(),
        claimGroup.getClaimPPSCapitalIMEAmount(),
        claimGroup.getClaimPPSCapitalOutlierAmount(),
        claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount());

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getDrgOutlierApprovedPaymentAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.NCH_DRG_OUTLIER_APRVD_PMT_AMT,
          claimGroup.getDrgOutlierApprovedPaymentAmount());
    }

    // Common group level fields between Inpatient, Outpatient and SNF
    TransformerUtils.mapEobCommonGroupInpOutSNF(
        eob,
        claimGroup.getBloodDeductibleLiabilityAmount(),
        claimGroup.getOperatingPhysicianNpi(),
        claimGroup.getOtherPhysicianNpi(),
        claimGroup.getClaimQueryCode(),
        claimGroup.getMcoPaidSw());

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        transformerContext.getNPIOrgLookup().retrieveNPIOrgDisplay(claimGroup.getOrganizationNpi()),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getAttendingPhysicianNpi(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber(),
        claimGroup.getFiDocumentClaimControlNumber(),
        claimGroup.getFiOriginalClaimControlNumber());

    // Common group level fields between Inpatient, HHA, Hospice and SNF
    TransformerUtils.mapEobCommonGroupInpHHAHospiceSNF(
        eob,
        claimGroup.getClaimAdmissionDate(),
        claimGroup.getBeneficiaryDischargeDate(),
        Optional.of(claimGroup.getUtilizationDayCount()));

    TransformerUtils.extractDiagnoses(
            claimGroup.getDiagnosisCodes(),
            claimGroup.getDiagnosisCodeVersions(),
            Optional.ofNullable(claimGroup.getDiagnosisPresentOnAdmissionCodes()))
        .stream()
        .forEach(d -> TransformerUtils.addDiagnosisCode(eob, d));

    TransformerUtils.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .stream()
        .forEach(p -> TransformerUtils.addProcedureCode(eob, p));

    for (InpatientClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());

      TransformerUtils.mapHcpcs(
          eob, item, Optional.empty(), claimLine.getHcpcsCode(), Collections.emptyList());

      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

      // Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonItemRevenue(
          item,
          eob,
          claimLine.getRevenueCenter(),
          claimLine.getRateAmount(),
          claimLine.getTotalChargeAmount(),
          claimLine.getNonCoveredChargeAmount(),
          claimLine.getUnitCount(),
          claimLine.getNationalDrugCodeQuantity(),
          claimLine.getNationalDrugCodeQualifierCode(),
          claimLine.getRevenueCenterRenderingPhysicianNPI());

      // Common group level field coinsurance between Inpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonGroupInpHHAHospiceSNFCoinsurance(
          eob, item, claimLine.getDeductibleCoinsuranceCd());
    }
    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
