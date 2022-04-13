package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;

/**
 * Transforms CCW {@link HospiceClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
public class HospiceClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link HospiceClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry,
      Object claim,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(HospiceClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof HospiceClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob = transformClaim((HospiceClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link HospiceClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  private static ExplanationOfBenefit transformClaim(HospiceClaim claimGroup) {
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
        ClaimTypeV2.HOSPICE,
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

    // NCH_PTNT_STUS_IND_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getPatientStatusCd().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claimGroup.getPatientStatusCd());
    }

    // CLM_ADMSN_DT       => ExplanationOfBenefit.supportingInfo:admissionperiod
    // NCH_BENE_DSCHRG_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationAdmPeriodSlice(
            eob, claimGroup.getClaimHospiceStartDate(), claimGroup.getBeneficiaryDischargeDate()));

    // CLM_UTLZTN_DAY_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.CLM_UTLZTN_DAY_CNT, claimGroup.getUtilizationDayCount());

    // ORG_NPI_NUM              => ExplanationOfBenefit.provider
    // CLM_FAC_TYPE_CD          => ExplanationOfBenefit.facility.extension
    // CLM_FREQ_CD              => ExplanationOfBenefit.supportingInfo
    // CLM_MDCR_NON_PMT_RSN_CD  => ExplanationOfBenefit.extension
    // PTNT_DSCHRG_STUS_CD      => ExplanationOfBenefit.supportingInfo
    // CLM_SRVC_CLSFCTN_TYPE_CD => ExplanationOfBenefit.extension
    // NCH_PRMRY_PYR_CD         => ExplanationOfBenefit.supportingInfo
    // CLM_TOT_CHRG_AMT         => ExplanationOfBenefit.total.amount
    // NCH_PRMRY_PYR_CLM_PD_AMT => ExplanationOfBenefit.benefitBalance.financial (PRPAYAMT)
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
    // CLM_POA_IND_SW(1-25)     => diagnosis.type
    // FST_DGNS_E_CD            => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD       => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12)      => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // CLM_E_POA_IND_SW(1-12)   => diagnosis.type
    for (Diagnosis diagnosis : DiagnosisUtilV2.extractDiagnoses(claimGroup)) {
      DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimTypeV2.HOSPICE);
    }

    // Map care team
    // AT_PHYSN_NPI     => ExplanationOfBenefit.careTeam.provider
    // AT_PHYSN_UPIN    => ExplanationOfBenefit.careTeam.provider
    TransformerUtilsV2.mapCareTeam(
        eob,
        claimGroup.getAttendingPhysicianNpi(),
        Optional.empty(),
        Optional.empty(),
        claimGroup.getAttendingPhysicianUpin(),
        Optional.empty(),
        Optional.empty());

    // BENE_HOSPC_PRD_CNT => ExplanationOfBenefit.extension
    if (claimGroup.getHospicePeriodCount().isPresent()) {
      eob.addExtension(
          TransformerUtilsV2.createExtensionQuantity(
              CcwCodebookVariable.BENE_HOSPC_PRD_CNT, claimGroup.getHospicePeriodCount()));
    }

    for (HospiceClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber().intValue());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

      // REV_CNTR                   => ExplanationOfBenefit.item.revenue
      // REV_CNTR_RATE_AMT          => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_TOT_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_NCVRD_CHRG_AMT    => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_NDC_QTY           => ExplanationOfBenefit.item.quantity
      // REV_CNTR_NDC_QTY_QLFR_CD   => ExplanationOfBenefit.modifier
      TransformerUtilsV2.mapEobCommonItemRevenue(
          item,
          eob,
          line.getRevenueCenterCode(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          line.getNonCoveredChargeAmount(),
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

      // HCPCS_CD           => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD  => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD  => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          line.getHcpcsCode(),
          Optional.empty(),
          Arrays.asList(line.getHcpcsInitialModifierCode(), line.getHcpcsSecondModifierCode()));

      // REV_CNTR_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
              C4BBAdjudication.PAID_TO_PROVIDER,
              line.getProviderPaymentAmount()));

      // REV_CNTR_BENE_PMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_BENE_PMT_AMT,
              C4BBAdjudication.PAID_TO_PATIENT,
              line.getBenficiaryPaymentAmount()));

      // Common item level fields between Outpatient, HHA and Hospice
      // REV_CNTR_DT              => ExplanationOfBenefit.item.servicedDate
      // REV_CNTR_PMT_AMT_AMT     => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.mapEobCommonItemRevenueOutHHAHospice(
          item, line.getRevenueCenterDate(), line.getPaymentAmount());
    }

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
