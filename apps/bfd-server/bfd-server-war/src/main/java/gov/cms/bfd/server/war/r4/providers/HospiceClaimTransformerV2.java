package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
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
 * Transforms CCW {@link HospiceClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
@Component
final class HospiceClaimTransformerV2 implements ClaimTransformerInterfaceV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(HospiceClaimTransformerV2.class.getSimpleName(), "transform");

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
  public HospiceClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a @link HospiceClaim} into an {@link ExplanationOfBenefit}.
   *
   * @param claimEntity the {@link Object} to use
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterfaceV2}; ignored
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(
      ClaimWithSecurityTags<?> claimEntity, boolean includeTaxNumber) {
    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof HospiceClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      HospiceClaim hospiceClaim = (HospiceClaim) claim;
      eob = transformClaim(hospiceClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link HospiceClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link HospiceClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  private ExplanationOfBenefit transformClaim(HospiceClaim claimGroup, List<Coding> securityTags) {
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
        ClaimType.HOSPICE,
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
        ClaimType.HOSPICE,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // PRVDR_NUM => ExplanationOfBenefit.provider.identifier
    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.PRN,
        claimGroup.getProviderNumber(),
        claimGroup.getLastUpdated(),
        Profile.C4BB);

    // NCH_PTNT_STUS_IND_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getPatientStatusCd().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claimGroup.getPatientStatusCd());
    }

    // CLM_ADMSN_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    // NCH_BENE_DSCHRG_DT => ExplanationOfBenefit.supportingInfo:admissionperiod
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationAdmPeriodSlice(
            eob, claimGroup.getClaimHospiceStartDate(), claimGroup.getBeneficiaryDischargeDate()));

    // CLM_UTLZTN_DAY_CNT => ExplanationOfBenefit.benefitBalance.financial
    TransformerUtilsV2.addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.CLM_UTLZTN_DAY_CNT, claimGroup.getUtilizationDayCount());

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
        claimGroup.getClaimQueryCode(),
        Profile.C4BB);

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
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(diagnosis -> DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimType.HOSPICE));

    // Map care team
    // AT_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
    // AT_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    TransformerUtilsV2.mapCareTeam(
        eob,
        claimGroup.getAttendingPhysicianNpi(),
        CommonTransformerUtils.buildReplaceTaxonomy(claimGroup.getAttendingPhysicianNpi()),
        Optional.empty(),
        Optional.empty(),
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
          line.getRevenueCenterCode(),
          line.getRateAmount(),
          line.getTotalChargeAmount(),
          line.getNonCoveredChargeAmount(),
          line.getNationalDrugCodeQuantity(),
          line.getNationalDrugCodeQualifierCode(),
          line.getUnitCount());

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

      // HCPCS_CD => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD => ExplanationOfBenefit.item.modifier
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
      // REV_CNTR_DT => ExplanationOfBenefit.item.servicedDate
      // REV_CNTR_PMT_AMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.mapEobCommonItemRevenueOutHHAHospice(
          item, line.getRevenueCenterDate(), line.getPaymentAmount());
    }

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
