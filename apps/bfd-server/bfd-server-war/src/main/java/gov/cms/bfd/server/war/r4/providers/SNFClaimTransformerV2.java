package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.commons.C4BBInstutionalClaimSubtypes;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link SNFClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
public class SNFClaimTransformerV2 implements ClaimTransformerInterfaceV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(SNFClaimTransformerV2.class.getSimpleName(), "transform");

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  private final boolean samhsaV2Enabled;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param securityTagManager SamhsaSecurityTags lookup
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public SNFClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a {@link SNFClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterfaceV2}; ignored.
   * @param claimEntity      the {@link Object} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   * SNFClaim}
   */
  @Override
  public ExplanationOfBenefit transform(
      ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof SNFClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      SNFClaim snfClaim = (SNFClaim) claim;
      eob = transformClaim(snfClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link SNFClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link SNFClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  private ExplanationOfBenefit transformClaim(SNFClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(Profile.C4BB.getVersionedEobInpatientUrl());

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }

    // Common group level fields between all claim types
    // Claim Type + Claim ID => ExplanationOfBenefit.id
    // CLM_ID => ExplanationOfBenefit.identifier
    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage
    // BENE_ID => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION => ExplanationOfBenefit.status
    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.SNF,
        String.valueOf(claimGroup.getClaimGroupId()),
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
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    // EOB Type => ExplanationOfBenefit.type.coding
    // Claim Type (Professional) => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimType.SNF,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated(),
        Profile.C4BB);

    // add EOB information to fields that are common between the Inpatient and SNF claim types
    // CLM_IP_ADMSN_TYPE_CD => ExplanationOfBenefit.supportingInfo.code
    // CLM_SRC_IP_ADMSN_CD => ExplanationOfBenefit.supportingInfo.code
    // NCH_VRFD_NCVRD_STAY_FROM_DT      => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_VRFD_NCVRD_STAY_THRU_DT      => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_ACTV_OR_CVRD_LVL_CARE_THRU   => ExplanationOfBenefit.supportingInfo.timingDate
    // NCH_BENE_MDCR_BNFTS_EXHTD_DT_I   => ExplanationOfBenefit.supportingInfo.timingDate
    // CLM_DRG_CD => ExplanationOfBenefit.supportingInfo.code
    // FI_CLM_ACTN_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.addCommonEobInformationInpatientSNF(
        eob,
        claimGroup.getAdmissionTypeCd(),
        claimGroup.getSourceAdmissionCd(),
        claimGroup.getNoncoveredStayFromDate(),
        claimGroup.getNoncoveredStayThroughDate(),
        claimGroup.getCoveredCareThroughDate(),
        claimGroup.getMedicareBenefitsExhaustedDate(),
        claimGroup.getDiagnosisRelatedGroupCd(),
        claimGroup.getFiscalIntermediaryClaimActionCode());

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

    // CLM_ADMSN_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    // NCH_BENE_DSCHRG_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationAdmPeriodSlice(
            eob, claimGroup.getClaimAdmissionDate(), claimGroup.getBeneficiaryDischargeDate()));

    // CLM_UTLZTN_DAY_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.CLM_UTLZTN_DAY_CNT, claimGroup.getUtilizationDayCount());

    // This is messy but appears to be specific to SNF. Maybe revisit and clean in the future
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
              c ->
                  period.setStart(
                      CommonTransformerUtils.convertToDate(c), TemporalPrecisionEnum.DAY));

      // NCH_QLFYD_STAY_THRU_DT
      claimGroup
          .getQualifiedStayThroughDate()
          .ifPresent(
              c ->
                  period.setEnd(
                      CommonTransformerUtils.convertToDate(c), TemporalPrecisionEnum.DAY));

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
    // BENE_TOT_COINSRNC_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_NON_UTLZTN_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_IP_DDCTBL_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_PTA_COINSRNC_LBLTY_AMT  => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BLOOD_PNTS_FRNSHD_QTY        => ExplanationOfBenefit.supportingInfo.valueQuantity
    // NCH_IP_NCVRD_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_IP_TOT_DDCTN_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT   => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_EXCPTN_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_FSP_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_IME_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_OUTLIER_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT => ExplanationOfBenefit.benefitBalance.financial
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
    // AT_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider (Primary)
    // AT_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    // OP_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider (Assisting)
    // OP_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
    // OT_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider (Other)
    // OT_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    TransformerUtilsV2.mapCareTeam(
        eob,
        claimGroup.getAttendingPhysicianNpi(),
        CommonTransformerUtils.buildReplaceTaxonomy(claimGroup.getAttendingPhysicianNpi()),
        claimGroup.getOperatingPhysicianNpi(),
        CommonTransformerUtils.buildReplaceTaxonomy(claimGroup.getOperatingPhysicianNpi()),
        claimGroup.getOtherPhysicianNpi(),
        CommonTransformerUtils.buildReplaceTaxonomy(claimGroup.getOtherPhysicianNpi()),
        claimGroup.getAttendingPhysicianUpin(),
        claimGroup.getOperatingPhysicianUpin(),
        claimGroup.getOtherPhysicianUpin());

    // Common group level fields between Inpatient, Outpatient and SNF
    // NCH_BENE_BLOOD_DDCTBL_LBLTY_AM => ExplanationOfBenefit.benefitBalance.financial
    // CLAIM_QUERY_CODE => ExplanationOfBenefit.billablePeriod.extension
    // CLM_MCO_PD_SW => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.mapEobCommonGroupInpOutSNF(
        eob, claimGroup.getBloodDeductibleLiabilityAmount(), claimGroup.getMcoPaidSw());

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
    // FI_DOC_CLM_CNTL_NUM => ExplanationOfBenefit.extension
    // FI_CLM_PROC_DT => ExplanationOfBenefit.extension
    // C4BBInstutionalClaimSubtypes.Inpatient for SNF Claims
    // CLAIM_QUERY_CODE => ExplanationOfBenefit.billablePeriod.extension
    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        CommonTransformerUtils.buildReplaceOrganization(claimGroup.getOrganizationNpi())
            .map(NPIData::getProviderOrganizationName),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber(),
        claimGroup.getLastUpdated(),
        claimGroup.getFiDocumentClaimControlNumber(),
        claimGroup.getFiscalIntermediaryClaimProcessDate(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.of(claimGroup.getClaimQueryCode()),
        Profile.C4BB);

    // Handle Diagnosis
    // ADMTG_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // ADMTG_DGNS_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_CD => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    DiagnosisUtilV2.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(diagnosis -> DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimType.SNF));

    // Handle Procedures
    TransformerUtilsV2.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .forEach(p -> TransformerUtilsV2.addProcedureCode(eob, p));

    for (SNFClaimLine line : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

      // HCPCS_CD => ExplanationOfBenefit.item.productOrService
      TransformerUtilsV2.mapHcpcs(
          eob, item, line.getHcpcsCode(), Optional.empty(), Collections.emptyList());

      // REV_CNTR => ExplanationOfBenefit.item.revenue
      // REV_CNTR_RATE_AMT => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_TOT_CHRG_AMT => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_NCVRD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
      // REV_CNTR_NDC_QTY => ExplanationOfBenefit.item.quantity
      // REV_CNTR_NDC_QTY_QLFR_CD => ExplanationOfBenefit.modifier
      // REV_CNTR_UNIT_CNT => ExplanationOfBenefit.item.extension.valueQuantity
      TransformerUtilsV2.mapEobCommonItemRevenue(
          item,
          eob,
          line.getRevenueCenter(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          Optional.of(line.getNonCoveredChargeAmount()),
          line.getNationalDrugCodeQuantity(),
          line.getNationalDrugCodeQualifierCode(),
          BigDecimal.valueOf(line.getUnitCount()));

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
          line.getRevenueCenterRenderingPhysicianNPI(),
          CommonTransformerUtils.buildReplaceTaxonomy(
              line.getRevenueCenterRenderingPhysicianNPI()));
    }

    // Last Updated => ExplanationOfBenefit.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
