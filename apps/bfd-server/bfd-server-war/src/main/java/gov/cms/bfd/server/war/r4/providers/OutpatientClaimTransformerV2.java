package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.commons.C4BBInstutionalClaimSubtypes;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transforms CCW {@link OutpatientClaim} instances into FHIR {@link ExplanationOfBenefit}
 * resources.
 */
@Component
final class OutpatientClaimTransformerV2 implements ClaimTransformerInterfaceV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(OutpatientClaimTransformerV2.class.getSimpleName(), "transform");

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
  public OutpatientClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a {@link OutpatientClaim} into an {@link ExplanationOfBenefit}.
   *
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterfaceV2}; ignored
   * @param claimEntity      the {@link Object} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   * OutpatientClaim}
   */
  @Override
  public ExplanationOfBenefit transform(
      ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof OutpatientClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {

      OutpatientClaim outpatientClaim = (OutpatientClaim) claim;
      eob = transformClaim(outpatientClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link InpatientClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link InpatientClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     OutpatientClaim}
   */
  private ExplanationOfBenefit transformClaim(
      OutpatientClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(Profile.C4BB.getVersionedEobOutpatientUrl());

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }

    // Common group level fields between all claim types
    // Claim Type + Claim ID => ExplanationOfBenefit.id
    // CLM_ID => ExplanationOfBenefit.identifier
    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    // FINAL_ACTION => ExplanationOfBenefit.status
    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_B,
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

    // set the provider number which is common among several claim types
    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated(),
        Profile.C4BB);

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
    // (PRPAYAMT)
    // FI_DOC_CLM_CNTL_NUM => ExplanationOfBenefit.extension
    // FI_CLM_PROC_DT => ExplanationOfBenefit.extension
    // C4BBInstutionalClaimSubtypes.Outpatient for Outpatient Claims
    // CLAIM_QUERY_CODE => ExplanationOfBenefit.billablePeriod.extension
    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        CommonTransformerUtils.buildReplaceOrganization(claimGroup.getOrganizationNpi())
            .map(NPIData::getProviderOrganizationName),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode().get(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber(),
        claimGroup.getLastUpdated(),
        claimGroup.getFiDocumentClaimControlNumber(),
        claimGroup.getFiscalIntermediaryClaimProcessDate(),
        C4BBInstutionalClaimSubtypes.Outpatient,
        Optional.of(claimGroup.getClaimQueryCode()),
        Profile.C4BB);

    // Handle Diagnosis
    // PRNCPAL_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-25) => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_CD => diagnosis.diagnosisCodeableConcept
    // FST_DGNS_E_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_E_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    DiagnosisUtilV2.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(
            diagnosis -> DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimType.OUTPATIENT));

    // Handle Procedures
    TransformerUtilsV2.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .forEach(p -> TransformerUtilsV2.addProcedureCode(eob, p));

    // ClaimLine => ExplanationOfBenefit.item
    for (OutpatientClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber());

      // PRVDR_STATE_CD => item.location
      TransformerUtilsV2.addLocationState(item, claimGroup.getProviderStateCode());

      // REV_CNTR => item.revenue
      item.setRevenue(
          TransformerUtilsV2.createCodeableConcept(
              eob, CcwCodebookVariable.REV_CNTR, line.getRevenueCenterCode()));

      // REV_CNTR_1ST_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob, CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD, line.getRevCntr1stAnsiCd()));

      // REV_CNTR_2ND_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob, CcwCodebookVariable.REV_CNTR_2ND_ANSI_CD, line.getRevCntr2ndAnsiCd()));

      // REV_CNTR_3RD_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob, CcwCodebookVariable.REV_CNTR_3RD_ANSI_CD, line.getRevCntr3rdAnsiCd()));

      // REV_CNTR_4RD_ANSI_CD => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob, CcwCodebookVariable.REV_CNTR_4TH_ANSI_CD, line.getRevCntr4thAnsiCd()));

      // HCPCS_CD => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          line.getHcpcsCode(),
          Optional.empty(),
          Arrays.asList(line.getHcpcsInitialModifierCode(), line.getHcpcsSecondModifierCode()));

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
          line.getRevenueCenterCode(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          Optional.of(line.getNonCoveredChargeAmount()),
          line.getNationalDrugCodeQuantity(),
          line.getNationalDrugCodeQualifierCode(),
          line.getUnitCount());

      // REV_CNTR_BLOOD_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_BLOOD_DDCTBL_AMT,
              C4BBAdjudication.DEDUCTIBLE,
              line.getBloodDeductibleAmount()));

      // REV_CNTR_CASH_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_CASH_DDCTBL_AMT,
              C4BBAdjudication.DEDUCTIBLE,
              line.getCashDeductibleAmount()));

      // REV_CNTR_COINSRNC_WGE_ADJSTD_C => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_COINSRNC_WGE_ADJSTD_C,
              C4BBAdjudication.COINSURANCE,
              line.getWageAdjustedCoinsuranceAmount()));

      // REV_CNTR_RDCD_COINSRNC_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_RDCD_COINSRNC_AMT,
              C4BBAdjudication.COINSURANCE,
              line.getReducedCoinsuranceAmount()));

      // REV_CNTR_1ST_MSP_PD_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_1ST_MSP_PD_AMT,
              C4BBAdjudication.PRIOR_PAYER_PAID,
              line.getFirstMspPaidAmount()));

      // REV_CNTR_2ND_MSP_PD_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_2ND_MSP_PD_AMT,
              C4BBAdjudication.PRIOR_PAYER_PAID,
              line.getSecondMspPaidAmount()));

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

      // REV_CNTR_PTNT_RSPNSBLTY_PMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_PTNT_RSPNSBLTY_PMT,
              C4BBAdjudication.PAID_BY_PATIENT,
              line.getPatientResponsibilityAmount()));

      // Common item level fields between Outpatient, HHA and Hospice
      // REV_CNTR_DT => ExplanationOfBenefit.item.servicedDate
      // REV_CNTR_PMT_AMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.mapEobCommonItemRevenueOutHHAHospice(
          item, line.getRevenueCenterDate(), line.getPaymentAmount());

      // REV_CNTR_IDE_NDC_UPC_NUM =>
      // ExplanationOfBenefit.item.productOrService.extension
      TransformerUtilsV2.addNationalDrugCode(
          item,
          line.getNationalDrugCode(),
          CommonTransformerUtils.buildReplaceDrugCode(line.getNationalDrugCode()));

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

      // REV_CNTR_STUS_IND_CD => ExplanationOfBenefit.item.revenue.extension
      TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, eob, line.getStatusCode());
    }

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
