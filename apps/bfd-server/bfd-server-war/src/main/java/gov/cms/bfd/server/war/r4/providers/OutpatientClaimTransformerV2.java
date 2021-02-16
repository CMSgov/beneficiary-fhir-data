package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;

/**
 * Transforms CCW {@link OutpatientClaim} instances into FHIR {@link ExplanationOfBenefit}
 * resources.
 */
public class OutpatientClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link InpatientClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    OutpatientClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof OutpatientClaim)) {
      throw new BadCodeMonkeyException();
    }

    ExplanationOfBenefit eob = transformClaim((OutpatientClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link InpatientClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  private static ExplanationOfBenefit transformClaim(OutpatientClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta()
        .addProfile(
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Outpatient-Institutional");

    // "claim" => ExplanationOfBenefit.use
    eob.setUse(Use.CLAIM);

    // Common group level fields between all claim types
    // Claim Type + Claim ID    => ExplanationOfBenefit.id
    // CLM_ID                   => ExplanationOfBenefit.identifier
    // CLM_GRP_ID               => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type  => ExplanationOfBenefit.insurance.coverage (reference)
    // BENE_ID                  => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION             => ExplanationOfBenefit.status
    // CLM_FROM_DT              => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT              => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT              => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.addInformationWithDate(
        eob,
        CcwCodebookVariable.NCH_WKLY_PROC_DT,
        CcwCodebookVariable.NCH_WKLY_PROC_DT,
        Optional.of(claimGroup.getWeeklyProcessDate()));

    // Map care team
    // AT_PHYSN_NPI     => ExplanationOfBenefit.careTeam.provider (Primary)
    // AT_PHYSN_UPIN    => ExplanationOfBenefit.careTeam.provider
    // OP_PHYSN_NPI     => ExplanationOfBenefit.careTeam.provider (Assisting)
    // OP_PHYSN_NPI     => ExplanationOfBenefit.careTeam.provider
    // OT_PHYSN_NPI     => ExplanationOfBenefit.careTeam.provider (Other)
    // OT_PHYSN_UPIN    => ExplanationOfBenefit.careTeam.provider
    TransformerUtilsV2.mapCareTeam(
        eob,
        claimGroup.getAttendingPhysicianNpi(),
        claimGroup.getOperatingPhysicianNpi(),
        claimGroup.getOtherPhysicianNpi(),
        claimGroup.getAttendingPhysicianUpin(),
        claimGroup.getOperatingPhysicianUpin(),
        claimGroup.getOtherPhysicianUpin());

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    // EOB Type => ExplanationOfBenefit.type.coding
    // Claim Type (institutional) => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimType.OUTPATIENT,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // NCH_PROFNL_CMPNT_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT,
        Optional.ofNullable(claimGroup.getProfessionalComponentCharge()));

    // NCH_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.NCH_BENE_PTB_DDCTBL_AMT,
        Optional.ofNullable(claimGroup.getDeductibleAmount()));

    // NCH_BENE_PTB_COINSRNC_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.NCH_BENE_PTB_COINSRNC_AMT,
        Optional.ofNullable(claimGroup.getCoinsuranceAmount()));

    // CLM_OP_PRVDR_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.CLM_OP_PRVDR_PMT_AMT,
        Optional.ofNullable(claimGroup.getProviderPaymentAmount()));

    // CLM_OP_PRVDR_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.CLM_OP_BENE_PMT_AMT,
        Optional.ofNullable(claimGroup.getBeneficiaryPaymentAmount()));

    // Common group level fields between Inpatient, Outpatient and SNF
    // NCH_BENE_BLOOD_DDCTBL_LBLTY_AM =>
    // ExplanationOfBenefit.benefitBalance.financial
    // CLAIM_QUERY_CODE => ExplanationOfBenefit.billablePeriod.extension
    // CLM_MCO_PD_SW => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.mapEobCommonGroupInpOutSNF(
        eob,
        claimGroup.getBloodDeductibleLiabilityAmount(),
        claimGroup.getClaimQueryCode(),
        claimGroup.getMcoPaidSw());

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    // ORG_NPI_NUM => ExplanationOfBenefit.provider
    // CLM_FAC_TYPE_CD => ExplanationOfBenefit.facility.extension
    // CLM_FREQ_CD => ExplanationOfBenefit.supportingInfo
    // CLM_MDCR_NON_PMT_RSN_CD => ExplanationOfBenefit.extension
    // PTNT_DSCHRG_STUS_CD => ExplanationOfBenefit.supportingInfo
    // CLM_SRVC_CLSFCTN_TYPE_CD => ExplanationOfBenefit.extension
    // NCH_PRMRY_PYR_CD => ExplanationOfBenefit.supportingInfo
    // CLM_TOT_CHRG_AMT => ExplanationOfBenefit.total.amount
    // NCH_PRMRY_PYR_CLM_PD_AMT => ExplanationOfBenefit.benefitBalance.financial
    // (PRPAYAMT)
    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode().get(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber());

    // Handle Diagnosis
    // PRNCPAL_DGNS_CD          => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_VRSN_CD     => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-25)        => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-25)   => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_CD            => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD       => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12)      => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    for (Diagnosis diagnosis : TransformerUtilsV2.extractDiagnoses(claimGroup)) {
      TransformerUtilsV2.addDiagnosisCode(eob, diagnosis);
    }

    // Handle Inpatient Diagnosis.  Only three, so just brute force it
    // RSN_VISIT_CD1        => diagnosis.diagnosisCodeableConcept
    // RSN_VISIT_VRSN_CD1   => diagnosis.diagnosisCodeableConcept
    TransformerUtilsV2.addDiagnosisCode(
        eob,
        TransformerUtilsV2.extractDiagnosis(
            "Admission1", claimGroup, Optional.empty(), DiagnosisLabel.REASONFORVISIT));

    // RSN_VISIT_CD2        => diagnosis.diagnosisCodeableConcept
    // RSN_VISIT_VRSN_CD2   => diagnosis.diagnosisCodeableConcept
    TransformerUtilsV2.addDiagnosisCode(
        eob,
        TransformerUtilsV2.extractDiagnosis(
            "Admission2", claimGroup, Optional.empty(), DiagnosisLabel.REASONFORVISIT));

    // RSN_VISIT_CD3        => diagnosis.diagnosisCodeableConcept
    // RSN_VISIT_VRSN_CD3   => diagnosis.diagnosisCodeableConcept
    TransformerUtilsV2.addDiagnosisCode(
        eob,
        TransformerUtilsV2.extractDiagnosis(
            "Admission3", claimGroup, Optional.empty(), DiagnosisLabel.REASONFORVISIT));

    // Handle Procedures
    // ICD_PRCDR_CD(1-25)        => ExplanationOfBenefit.procedure.procedureCodableConcept
    // ICD_PRCDR_VRSN_CD(1-25)   => ExplanationOfBenefit.procedure.procedureCodableConcept
    // PRCDR_DT(1-25)            => ExplanationOfBenefit.procedure.date
    final int FIRST_PROCEDURE = 1;
    final int LAST_PROCEDURE = 25;

    IntStream.range(FIRST_PROCEDURE, LAST_PROCEDURE + 1)
        .mapToObj(i -> TransformerUtilsV2.extractCCWProcedure(i, claimGroup))
        .filter(p -> p.isPresent())
        .forEach(p -> TransformerUtilsV2.addProcedureCode(eob, p.get()));

    // ClaimLine => ExplanationOfBenefit.item
    for (OutpatientClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber().intValue());

      // PRVDR_STATE_CD => item.location
      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

      // REV_CNTR => item.revenue
      item.setRevenue(
          TransformerUtilsV2.createCodeableConcept(
              eob, CcwCodebookVariable.REV_CNTR, line.getRevenueCenterCode()));

      // REV_CNTR_DT => item.servicedDate
      if (line.getRevenueCenterDate().isPresent()) {
        item.setServiced(
            new DateType(TransformerUtilsV2.convertToDate(line.getRevenueCenterDate().get())));
      }

      // REV_CNTR_1ST_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationWithReason(
              eob, CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD, line.getRevCntr1stAnsiCd()));

      // REV_CNTR_2ND_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationWithReason(
              eob, CcwCodebookVariable.REV_CNTR_2ND_ANSI_CD, line.getRevCntr2ndAnsiCd()));

      // REV_CNTR_3RD_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationWithReason(
              eob, CcwCodebookVariable.REV_CNTR_3RD_ANSI_CD, line.getRevCntr3rdAnsiCd()));

      // REV_CNTR_3RD_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationWithReason(
              eob, CcwCodebookVariable.REV_CNTR_4TH_ANSI_CD, line.getRevCntr4thAnsiCd()));

      // HCPCS_CD           => ExplanationOfBenefit.item.modifier
      // HCPCS_1ST_MDFR_CD  => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD  => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          Optional.empty(),
          Arrays.asList(
              line.getHcpcsCode(),
              line.getHcpcsInitialModifierCode(),
              line.getHcpcsSecondModifierCode()));
    }

    return eob;
  }
}
