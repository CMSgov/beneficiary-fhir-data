package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HHAClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.Quantity;

public class HHAClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link HHAClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HHAClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(HHAClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof HHAClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob = transformClaim((HHAClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link HHAClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HHAClaim}
   */
  private static ExplanationOfBenefit transformClaim(HHAClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(ProfileConstants.C4BB_EOB_NONCLINICIAN_PROFILE_URL);

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
        ClaimTypeV2.HHA,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.createInformationRecievedDateSlice(
        eob, CcwCodebookVariable.NCH_WKLY_PROC_DT, Optional.of(claimGroup.getWeeklyProcessDate()));

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD            => ExplanationOfBenefit.type.coding
    // EOB Type                   => ExplanationOfBenefit.type.coding
    // Claim Type                 => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimTypeV2.HHA,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated());

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

    // CLM_PPS_IND_CODE => ExplanationOfBenefit.supportingInfo
    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.CLM_PPS_IND_CD,
        CcwCodebookVariable.CLM_PPS_IND_CD,
        claimGroup.getProspectivePaymentCode());

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

    // CLM_HHA_LUPA_IND_CD => ExplanationOfBenefit.supportinginfo.code
    claimGroup
        .getClaimLUPACode()
        .ifPresent(
            c ->
                TransformerUtilsV2.addInformationWithCode(
                    eob,
                    CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
                    CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
                    c));

    // CLM_HHA_RFRL_CD => ExplanationOfBenefit.supportinginfo.code
    claimGroup
        .getClaimReferralCode()
        .ifPresent(
            c ->
                TransformerUtilsV2.addInformationWithCode(
                    eob,
                    CcwCodebookVariable.CLM_HHA_RFRL_CD,
                    CcwCodebookVariable.CLM_HHA_RFRL_CD,
                    c));

    // CLM_HHA_TOT_VISIT_CNT => ExplanationOfBenefit.supportinginfo.value[x]
    TransformerUtilsV2.addInformation(eob, CcwCodebookVariable.CLM_HHA_TOT_VISIT_CNT)
        .setValue(new Quantity(claimGroup.getTotalVisitCount().intValue()));

    // CLM_ADMSN_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationAdmPeriodSlice(
            eob, claimGroup.getCareStartDate(), Optional.empty()));

    for (HHAClaimLine line : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber().intValue());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

      // HCPCS_CD           => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD  => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD  => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          line.getHcpcsCode(),
          Optional.empty(),
          Arrays.asList(line.getHcpcsInitialModifierCode(), line.getHcpcsSecondModifierCode()));

      // REV_CNTR_1ST_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob, CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD, line.getRevCntr1stAnsiCd()));

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
          line.getRevenueCenterCode(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          Optional.of(line.getNonCoveredChargeAmount()),
          line.getUnitCount(),
          line.getNationalDrugCodeQuantity(),
          line.getNationalDrugCodeQualifierCode());

      // Common item level fields between Outpatient, HHA and Hospice
      // REV_CNTR_DT              => ExplanationOfBenefit.item.servicedDate
      // REV_CNTR_PMT_AMT_AMT     => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.mapEobCommonItemRevenueOutHHAHospice(
          item, line.getRevenueCenterDate(), line.getPaymentAmount());

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
          C4BBClaimProfessionalAndNonClinicianCareTeamRole.PERFORMING,
          line.getRevenueCenterRenderingPhysicianUPIN());

      // RNDRNG_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
      TransformerUtilsV2.addCareTeamMember(
          eob,
          item,
          C4BBPractitionerIdentifierType.NPI,
          C4BBClaimProfessionalAndNonClinicianCareTeamRole.PERFORMING,
          line.getRevenueCenterRenderingPhysicianNPI());
    }

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
