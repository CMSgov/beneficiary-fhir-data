package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.Period;

/** Transforms CCW {@link SNFClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
public class SNFClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link SNFClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(SNFClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof SNFClaim)) {
      throw new BadCodeMonkeyException();
    }

    timer.stop();
    return transformClaim((SNFClaim) claim);
  }

  /**
   * @param claimGroup the CCW {@link SNFClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  private static ExplanationOfBenefit transformClaim(SNFClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL);

    // Common group level fields between all claim types
    // Claim Type + Claim ID    => ExplanationOfBenefit.id
    // CLM_ID                   => ExplanationOfBenefit.identifier
    // CLM_GRP_ID               => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type  => ExplanationOfBenefit.insurance.coverage
    // BENE_ID                  => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION             => ExplanationOfBenefit.status
    // CLM_FROM_DT              => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT              => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT              => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimTypeV2.SNF,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationRecievedDateSlice(
            eob,
            CcwCodebookVariable.NCH_WKLY_PROC_DT,
            Optional.of(claimGroup.getWeeklyProcessDate())));

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD            => ExplanationOfBenefit.type.coding
    // EOB Type                   => ExplanationOfBenefit.type.coding
    // Claim Type (Professional)  => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimTypeV2.HOSPICE,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated());

    // add EOB information to fields that are common between the Inpatient and SNF claim types
    // CLM_IP_ADMSN_TYPE_CD             => ExplanationOfBenefit.supportingInfo.code
    // CLM_SRC_IP_ADMSN_CD              => ExplanationOfBenefit.supportingInfo.code
    // NCH_VRFD_NCVRD_STAY_FROM_DT      => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_VRFD_NCVRD_STAY_THRU_DT      => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_ACTV_OR_CVRD_LVL_CARE_THRU   => ExplanationOfBenefit.supportingInfo.timingDate
    // NCH_BENE_MDCR_BNFTS_EXHTD_DT_I   => ExplanationOfBenefit.supportingInfo.timingDate
    // CLM_DRG_CD                       => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.addCommonEobInformationInpatientSNF(
        eob,
        claimGroup.getAdmissionTypeCd(),
        claimGroup.getSourceAdmissionCd(),
        claimGroup.getNoncoveredStayFromDate(),
        claimGroup.getNoncoveredStayThroughDate(),
        claimGroup.getCoveredCareThroughDate(),
        claimGroup.getMedicareBenefitsExhaustedDate(),
        claimGroup.getDiagnosisRelatedGroupCd());

    // NCH_PTNT_STUS_IND_CD => ExplanationOfBenefit.supportingInfo.code
    claimGroup
        .getPatientStatusCd()
        .ifPresent(
            c ->
                TransformerUtilsV2.addInformationWithCode(
                    eob,
                    CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
                    CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
                    c));

    // CLM_ADMSN_DT       => ExplanationOfBenefit.supportingInfo:admissionperiod
    // NCH_BENE_DSCHRG_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationAdmPeriodSlice(
            eob, claimGroup.getClaimAdmissionDate(), claimGroup.getBeneficiaryDischargeDate()));

    // CLM_UTLZTN_DAY_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.CLM_UTLZTN_DAY_CNT, claimGroup.getUtilizationDayCount());

    // This is messy but appears to be specific to SNF.  Maybe revisit and clean in the future
    // NCH_QLFYD_STAY_FROM_DT => ExplanationOfBenefit.supportingInfo
    // NCH_QLFYD_STAY_THRU_DT => ExplanationOfBenefit.supportingInfo
    if (claimGroup.getQualifiedStayFromDate().isPresent()
        || claimGroup.getQualifiedStayThroughDate().isPresent()) {
      TransformerUtilsV2.validatePeriodDates(
          claimGroup.getQualifiedStayFromDate(), claimGroup.getQualifiedStayThroughDate());

      Period period = new Period();

      // NCH_QLFYD_STAY_FROM_DT
      claimGroup
          .getQualifiedStayFromDate()
          .ifPresent(
              c -> period.setStart(TransformerUtilsV2.convertToDate(c), TemporalPrecisionEnum.DAY));

      // NCH_QLFYD_STAY_THRU_DT
      claimGroup
          .getQualifiedStayThroughDate()
          .ifPresent(
              c -> period.setEnd(TransformerUtilsV2.convertToDate(c), TemporalPrecisionEnum.DAY));

      // Add to EOB
      TransformerUtilsV2.addInformation(eob, CcwCodebookVariable.NCH_QLFYD_STAY_FROM_DT)
          .setTiming(period);
    }

    // CLM_PPS_IND_CODE => ExplanationOfBenefit.supportingInfo
    claimGroup
        .getProspectivePaymentCode()
        .ifPresent(
            c ->
                TransformerUtilsV2.addInformationWithCode(
                    eob,
                    CcwCodebookVariable.CLM_PPS_IND_CD,
                    CcwCodebookVariable.CLM_PPS_IND_CD,
                    c));

    /*
     * add field values to the benefit balances that are common between the
     * Inpatient and SNF claim types
     */
    // BENE_TOT_COINSRNC_DAYS_CNT       => ExplanationOfBenefit.benefitBalance.financial
    // CLM_NON_UTLZTN_DAYS_CNT          => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_IP_DDCTBL_AMT           => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_PTA_COINSRNC_LBLTY_AMT  => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BLOOD_PNTS_FRNSHD_QTY        => ExplanationOfBenefit.supportingInfo.valueQuantity
    // NCH_IP_NCVRD_CHRG_AMT            => ExplanationOfBenefit.benefitBalance.financial
    // NCH_IP_TOT_DDCTN_AMT             => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT   => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_EXCPTN_AMT          => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_FSP_AMT             => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_IME_AMT             => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_OUTLIER_AMT         => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT   => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addCommonGroupInpatientSNF(
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

    // Common group level fields between Inpatient, Outpatient and SNF
    // NCH_BENE_BLOOD_DDCTBL_LBLTY_AM   => ExplanationOfBenefit.benefitBalance.financial
    // CLAIM_QUERY_CODE                 => ExplanationOfBenefit.billablePeriod.extension
    // CLM_MCO_PD_SW                    => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.mapEobCommonGroupInpOutSNF(
        eob,
        claimGroup.getBloodDeductibleLiabilityAmount(),
        claimGroup.getClaimQueryCode(),
        claimGroup.getMcoPaidSw());

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    // ORG_NPI_NUM              => ExplanationOfBenefit.provider
    // CLM_FAC_TYPE_CD          => ExplanationOfBenefit.facility.extension
    // CLM_FREQ_CD              => ExplanationOfBenefit.supportingInfo
    // CLM_MDCR_NON_PMT_RSN_CD  => ExplanationOfBenefit.extension
    // PTNT_DSCHRG_STUS_CD      => ExplanationOfBenefit.supportingInfo
    // CLM_SRVC_CLSFCTN_TYPE_CD => ExplanationOfBenefit.extension
    // NCH_PRMRY_PYR_CD         => ExplanationOfBenefit.supportingInfo
    // CLM_TOT_CHRG_AMT         => ExplanationOfBenefit.total.amount
    // NCH_PRMRY_PYR_CLM_PD_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber(),
        claimGroup.getLastUpdated());

    // Handle Diagnosis
    // ADMTG_DGNS_CD            => diagnosis.diagnosisCodeableConcept
    // ADMTG_DGNS_VRSN_CD       => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_CD          => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-25)        => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-25)   => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_CD            => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD       => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12)      => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    for (Diagnosis diagnosis : DiagnosisUtilV2.extractDiagnoses(claimGroup)) {
      DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimTypeV2.SNF);
    }

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

    for (SNFClaimLine line : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber().intValue());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

      // HCPCS_CD => ExplanationOfBenefit.item.productOrService
      TransformerUtilsV2.mapHcpcs(
          eob, item, line.getHcpcsCode(), Optional.empty(), Collections.emptyList());

      // REV_CNTR                   => ExplanationOfBenefit.item.revenue
      // REV_CNTR_RATE_AMT          => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_TOT_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_NCVRD_CHRG_AMT    => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_UNIT_CNT          => ExplanationOfBenefit.item.quantity
      // REV_CNTR_NDC_QTY           => TODO: ??
      // REV_CNTR_NDC_QTY_QLFR_CD   => ExplanationOfBenefit.modifier
      TransformerUtilsV2.mapEobCommonItemRevenue(
          item,
          eob,
          line.getRevenueCenter(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          Optional.of(line.getNonCoveredChargeAmount()),
          BigDecimal.valueOf(line.getUnitCount()),
          line.getNationalDrugCodeQuantity(),
          line.getNationalDrugCodeQualifierCode());

      // REV_CNTR_DDCTBL_COINSRNC_CD => item.revenue
      TransformerUtilsV2.addItemRevenue(
          item,
          eob,
          CcwCodebookVariable.REV_CNTR_DDCTBL_COINSRNC_CD,
          line.getDeductibleCoinsuranceCd());

      // RNDRNG_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
      TransformerUtilsV2.addCareTeamMember(
          eob,
          item,
          C4BBPractitionerIdentifierType.UPIN,
          C4BBClaimInstitutionalCareTeamRole.PERFORMING,
          line.getRevenueCenterRenderingPhysicianUPIN());

      // RNDRNG_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
      TransformerUtilsV2.addCareTeamMember(
          eob,
          item,
          C4BBPractitionerIdentifierType.NPI,
          C4BBClaimInstitutionalCareTeamRole.PERFORMING,
          line.getRevenueCenterRenderingPhysicianNPI());
    }

    // Last Updated => ExplanationOfBenefit.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
