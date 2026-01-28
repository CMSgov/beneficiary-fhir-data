package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.commons.C4BBInstutionalClaimSubtypes;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transforms CCW {@link InpatientClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
@Component
final class InpatientClaimTransformerV2 implements ClaimTransformerInterfaceV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(InpatientClaimTransformerV2.class.getSimpleName(), "transform");

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
   * @param securityTagManager SamhsaSecurityTag lookup
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public InpatientClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a {@link InpatientClaim} into an {@link ExplanationOfBenefit}.
   *
   * @param claimEntity the {@link Object} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  @Override
  public ExplanationOfBenefit transform(ClaimWithSecurityTags<?> claimEntity) {
    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof InpatientClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      InpatientClaim inpatientClaim = (InpatientClaim) claim;
      eob = transformClaim(inpatientClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link InpatientClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link InpatientClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     InpatientClaim}
   */
  private ExplanationOfBenefit transformClaim(
      InpatientClaim claimGroup, List<Coding> securityTags) {
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
    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage (reference)
    // BENE_ID => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION => ExplanationOfBenefit.status
    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
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

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    // EOB Type => ExplanationOfBenefit.type.coding
    // Claim Type (institutional) => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimType.INPATIENT,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated(),
        Profile.C4BB);

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

    // add EOB information to fields that are common between the Inpatient and SNF claim types
    // CLM_IP_ADMSN_TYPE_CD => ExplanationOfBenefit.supportingInfo.code
    // CLM_SRC_IP_ADMSN_CD => ExplanationOfBenefit.supportingInfo.code
    // NCH_VRFD_NCVRD_STAY_FROM_DT => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_VRFD_NCVRD_STAY_THRU_DT => ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_ACTV_OR_CVRD_LVL_CARE_THRU => ExplanationOfBenefit.supportingInfo.timingDate
    // NCH_BENE_MDCR_BNFTS_EXHTD_DT_I => ExplanationOfBenefit.supportingInfo.timingDate
    // CLM_DRG_CD => ExplanationOfBenefit.supportingInfo.code
    // FI_CLM_ACTN_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.addCommonEobInformationInpatientSNF(
        eob,
        claimGroup.getAdmissionTypeCd(),
        claimGroup.getSourceAdmissionCd(),
        claimGroup.getNoncoveredStayFromDate(),
        claimGroup.getNoncoveredStayThroughDate(),
        claimGroup.getCoveredCareThoughDate(),
        claimGroup.getMedicareBenefitsExhaustedDate(),
        claimGroup.getDiagnosisRelatedGroupCd(),
        claimGroup.getFiscalIntermediaryClaimActionCode());

    // IME_OP_CLM_VAL_AMT => ExplanationOfBenefit.extension
    TransformerUtilsV2.addAdjudicationTotal(
        eob,
        CcwCodebookVariable.IME_OP_CLM_VAL_AMT,
        claimGroup.getIndirectMedicalEducationAmount());

    // CLM_UNCOMPD_CARE_PMT_AMT => ExplanationOfBenefit.extension[N].valueMoney.value
    if (claimGroup.getClaimUncompensatedCareAmount().isPresent()) {
      TransformerUtilsV2.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.CLM_UNCOMPD_CARE_PMT_AMT,
          claimGroup.getClaimUncompensatedCareAmount().get());
    }

    // DSH_OP_CLM_VAL_AMT => ExplanationOfBenefit.extension
    TransformerUtilsV2.addAdjudicationTotal(
        eob, CcwCodebookVariable.DSH_OP_CLM_VAL_AMT, claimGroup.getDisproportionateShareAmount());

    // CLM_PASS_THRU_PER_DIEM_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.CLM_PASS_THRU_PER_DIEM_AMT,
        Optional.ofNullable(claimGroup.getPassThruPerDiemAmount()));

    // NCH_PROFNL_CMPNT_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT,
        Optional.ofNullable(claimGroup.getProfessionalComponentCharge()));

    // CLM_TOT_PPS_CPTL_AMT => ExplanationOfBenefit.benefitBalance.financial
    if (claimGroup.getClaimTotalPPSCapitalAmount().isPresent()) {
      TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
          eob,
          CcwCodebookVariable.CLM_TOT_PPS_CPTL_AMT,
          claimGroup.getClaimTotalPPSCapitalAmount());
    }

    /*
     * add field values to the benefit balances that are common between the
     * Inpatient and SNF claim types
     */
    // BENE_TOT_COINSRNC_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_NON_UTLZTN_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_IP_DDCTBL_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BENE_PTA_COINSRNC_LBLTY_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_BLOOD_PNTS_FRNSHD_QTY => ExplanationOfBenefit.supportingInfo.valueQuantity
    // NCH_IP_NCVRD_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_IP_TOT_DDCTN_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT => ExplanationOfBenefit.benefitBalance.financial
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

    // NCH_DRG_OUTLIER_APRVD_PMT_AMT => ExplanationOfBenefit.extension
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.NCH_DRG_OUTLIER_APRVD_PMT_AMT,
        claimGroup.getDrgOutlierApprovedPaymentAmount());

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
    // NCH_PRMRY_PYR_CLM_PD_AMT => ExplanationOfBenefit.benefitBalance.financial (PRPAYAMT)
    // FI_DOC_CLM_CNTL_NUM => ExplanationOfBenefit.extension
    // FI_CLM_PROC_DT => ExplanationOfBenefit.extension
    // C4BBInstutionalClaimSubtypes.Inpatient for Hospice Claims
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

    // CLM_UTLZTN_DAY_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob,
        CcwCodebookVariable.CLM_UTLZTN_DAY_CNT,
        Optional.of(claimGroup.getUtilizationDayCount()));

    // Handle Diagnosis
    // ADMTG_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // ADMTG_DGNS_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // CLM_POA_IND_SW(1-25) => diagnosis.type
    // FST_DGNS_E_CD => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // CLM_E_POA_IND_SW(1-12) => diagnosis.type
    DiagnosisUtilV2.extractDiagnoses(
            claimGroup.getDiagnosisCodes(),
            claimGroup.getDiagnosisCodeVersions(),
            claimGroup.getDiagnosisPresentOnAdmissionCodes())
        .forEach(
            diagnosis -> DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimType.INPATIENT));

    // Handle Procedures
    TransformerUtilsV2.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .forEach(p -> TransformerUtilsV2.addProcedureCode(eob, p));

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationRecievedDateSlice(
            eob,
            CcwCodebookVariable.NCH_WKLY_PROC_DT,
            Optional.of(claimGroup.getWeeklyProcessDate())));

    // CLM_PPS_CPTL_DRG_WT_NUM => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob,
        CcwCodebookVariable.CLM_PPS_CPTL_DRG_WT_NUM,
        claimGroup.getClaimPPSCapitalDrgWeightNumber());

    // BENE_LRD_USED_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.BENE_LRD_USED_CNT, claimGroup.getLifetimeReservedDaysUsedCount());

    // ClaimLine => ExplanationOfBenefit.item
    for (InpatientClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

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
          line.getUnitCount());

      // REV_CNTR_DDCTBL_COINSRNC_CD => item.revenue
      TransformerUtilsV2.addItemRevenue(
          item,
          eob,
          CcwCodebookVariable.REV_CNTR_DDCTBL_COINSRNC_CD,
          line.getDeductibleCoinsuranceCd());

      // HCPCS_CD => item.productOrService
      Optional<String> hcpcsCode = line.getHcpcsCode();
      if (hcpcsCode.isPresent()) {
        item.setProductOrService(
            new CodeableConcept()
                .setCoding(
                    Arrays.asList(
                        new Coding()
                            .setSystem(
                                CCWUtils.calculateVariableReferenceUrl(
                                    CcwCodebookVariable.HCPCS_CD))
                            .setCode(hcpcsCode.get()),
                        new Coding()
                            .setSystem(TransformerConstants.CODING_SYSTEM_HCPCS)
                            .setCode(hcpcsCode.get()))));
      } else {
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept
            .addCoding()
            .setSystem(TransformerConstants.CODING_DATA_ABSENT)
            .setCode(TransformerConstants.DATA_ABSENT_REASON_NULL_CODE)
            .setDisplay(TransformerConstants.DATA_ABSENT_REASON_DISPLAY);
        item.setProductOrService(codeableConcept);
      }

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
