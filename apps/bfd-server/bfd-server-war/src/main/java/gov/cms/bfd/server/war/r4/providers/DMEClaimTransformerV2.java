package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static gov.cms.bfd.server.war.commons.TransformerConstants.CODING_NPI_US;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link DMEClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
final class DMEClaimTransformerV2 implements ClaimTransformerInterfaceV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(DMEClaimTransformerV2.class.getSimpleName(), "transform");

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
  DMEClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a {@link DMEClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimEntity the {@link Object} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof DMEClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      DMEClaim dmeClaim = (DMEClaim) claim;
      eob = transformClaim(dmeClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link DMEClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link DMEClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  private ExplanationOfBenefit transformClaim(DMEClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(Profile.C4BB.getVersionedEobInpatientUrl());

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }

    Optional<String> providerNpi = Optional.empty();
    if (claimGroup.getLines() != null && !claimGroup.getLines().isEmpty()) {
      providerNpi = claimGroup.getLines().getFirst().getProviderNPI();
    }
    Reference providerReference =
        new Reference()
            .setIdentifier(
                new Identifier().setSystem(CODING_NPI_US).setValue(providerNpi.orElse("UNKNOWN")))
            .setDisplay(
                providerNpi.map(CommonTransformerUtils::retrieveNpiCodeDisplay).orElse("UNKNOWN"))
            .setType("Practitioner");
    eob.setProvider(providerReference);

    // Common group level fields between all claim types
    // Claim Type + Claim ID => ExplanationOfBenefit.id
    // CLM_ID => ExplanationOfBenefit.identifier
    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage
    // BENE_ID => ExplanationOfBenefit.patient (reference)pwd
    // FINAL_ACTION => ExplanationOfBenefit.status
    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.DME,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    // EOB Type => ExplanationOfBenefit.type.coding
    // Claim Type (Professional) => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimType.DME,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // CARR_CLM_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.total.amount
    TransformerUtilsV2.addTotal(
        eob,
        TransformerUtilsV2.createTotalAdjudicationAmountSlice(
            eob,
            CcwCodebookVariable.CLM_TOT_CHRG_AMT,
            C4BBAdjudication.PRIOR_PAYER_PAID,
            claimGroup.getPrimaryPayerPaidAmount()));

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationRecievedDateSlice(
            eob,
            CcwCodebookVariable.NCH_WKLY_PROC_DT,
            Optional.of(claimGroup.getWeeklyProcessDate())));

    // Common group level fields between Carrier and DME
    // BENE_ID =>
    // CARR_NUM => ExplanationOfBenefit.extension
    // CLM_CLNCL_TRIL_NUM => ExplanationOfBenefit.extension
    // CARR_CLM_CASH_DDCTBL_APLD_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CARR_CLM_PMT_DNL_CD => ExplanationOfBenefit.extension
    // RFR_PHYSN_NPI                  => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // RFR_PHYSN_UPIN                 => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // CARR_CLM_PRVDR_ASGNMT_IND_SW => ExplanationOfBenefit.extension
    // NCH_CLM_PRVDR_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CLM_BENE_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_SBMTD_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_ALOWD_AMT => ExplanationOfBenefit.benefitBalance.financial
    // CLM_DISP_CD => ExplanationOfBenefit.disposition
    // CARR_CLM_CNTL_NUM => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobCommonGroupCarrierDME(
        eob,
        claimGroup.getCarrierNumber(),
        claimGroup.getClinicalTrialNumber(),
        claimGroup.getBeneficiaryPartBDeductAmount(),
        claimGroup.getPaymentDenialCode(),
        claimGroup.getReferringPhysicianNpi(),
        CommonTransformerUtils.buildReplaceTaxonomy(claimGroup.getReferringPhysicianNpi()),
        claimGroup.getReferringPhysicianUpin(),
        Optional.of(claimGroup.getProviderAssignmentIndicator()),
        claimGroup.getProviderPaymentAmount(),
        claimGroup.getBeneficiaryPaymentAmount(),
        claimGroup.getSubmittedChargeAmount(),
        claimGroup.getAllowedChargeAmount(),
        claimGroup.getClaimDispositionCode(),
        claimGroup.getClaimCarrierControlNumber());

    // PRNCPAL_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_VRSN_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    DiagnosisUtilV2.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(diagnosis -> DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimType.DME));

    // CARR_CLM_ENTRY_CD => ExplanationOfBenefit.extension
    eob.addExtension(
        TransformerUtilsV2.createExtensionCoding(
            eob, CcwCodebookVariable.CARR_CLM_ENTRY_CD, claimGroup.getClaimEntryCode()));

    handleClaimLines(claimGroup, eob);
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }

  /**
   * Adds information to the eob using the claim line information.
   *
   * @param claimGroup the claim group with the lines to get data from
   * @param eob the eob to add information to
   */
  private void handleClaimLines(DMEClaim claimGroup, ExplanationOfBenefit eob) {
    for (DMEClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber());

      // add an extension for the provider billing number as there is not a good place
      // to map this in the existing FHIR specification
      // PRVDR_NUM => ExplanationOfBenefit.provider.value
      line.getProviderBillingNumber()
          .ifPresent(
              c ->
                  item.addExtension(
                      TransformerUtilsV2.createExtensionIdentifier(
                          CcwCodebookVariable.SUPLRNUM, line.getProviderBillingNumber())));

      // PRVDR_NPI => ExplanationOfBenefit.careTeam.provider
      Optional<CareTeamComponent> performing =
          TransformerUtilsV2.addCareTeamMember(
              eob,
              item,
              C4BBPractitionerIdentifierType.NPI,
              C4BBClaimProfessionalAndNonClinicianCareTeamRole.PERFORMING,
              line.getProviderNPI(),
              CommonTransformerUtils.buildReplaceTaxonomy(line.getProviderNPI()));

      // Update the responsible flag
      if (performing.isPresent()) {
        CareTeamComponent careTeam = performing.get();
        performing.get().setResponsible(true);

        // PRVDR_SPCLTY => ExplanationOfBenefit.careTeam.qualification
        TransformerUtilsV2.addCareTeamQualification(
            careTeam, eob, CcwCodebookVariable.PRVDR_SPCLTY, line.getProviderSpecialityCode());

        // PRTCPTNG_IND_CD => ExplanationOfBenefit.careTeam.extension
        TransformerUtilsV2.addCareTeamExtension(
            CcwCodebookVariable.PRTCPTNG_IND_CD,
            line.getProviderParticipatingIndCode(),
            careTeam,
            eob);
      }

      // PRVDR_STATE_CD => ExplanationOfBenefit.item.location.extension
      if (item.getLocation() != null) {
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, line.getProviderStateCode()));
      }

      // HCPCS_CD => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD => ExplanationOfBenefit.item.modifier
      // HCPCS_3RD_MDFR_CD => ExplanationOfBenefit.item.modifier
      // HCPCS_4Th_MDFR_CD => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          line.getHcpcsCode(),
          claimGroup.getHcpcsYearCode(),
          Arrays.asList(
              line.getHcpcsInitialModifierCode(),
              line.getHcpcsSecondModifierCode(),
              line.getHcpcsThirdModifierCode(),
              line.getHcpcsFourthModifierCode()));

      // REV_CNTR_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
              C4BBAdjudication.PAID_TO_PROVIDER,
              line.getProviderPaymentAmount()));

      // TODO - check w/jack if this is right ELIGIBLE
      // LINE_PRMRY_ALOWD_CHRG_AMT => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT,
              C4BBAdjudication.ELIGIBLE,
              line.getPrimaryPayerAllowedChargeAmount()));

      // LINE_DME_PRCHS_PRICE_AMT => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
              C4BBAdjudication.SUBMITTED,
              line.getPurchasePriceAmount()));

      // DMERC_LINE_SCRN_SVGS_AMT => ExplanationOfBenefit.item.extension
      line.getScreenSavingsAmount()
          .ifPresent(
              c ->
                  item.addExtension(
                      // TODO should this be an adjudication?
                      TransformerUtilsV2.createExtensionQuantity(
                          CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT,
                          line.getScreenSavingsAmount())));

      // DMERC_LINE_MTUS_CNT => ExplanationOfBenefit.item.extension
      Extension mtusQuantityExtension =
          TransformerUtilsV2.createExtensionQuantity(
              CcwCodebookVariable.DMERC_LINE_MTUS_CNT, line.getMtusCount());

      item.addExtension(mtusQuantityExtension);

      // DMERC_LINE_MTUS_CD => ExplanationOfBenefit.item.extension
      if (line.getMtusCode().isPresent()) {
        Quantity mtusQuantity = (Quantity) mtusQuantityExtension.getValue();
        TransformerUtilsV2.setQuantityUnitInfo(
            CcwCodebookVariable.DMERC_LINE_MTUS_CD, line.getMtusCode(), eob, mtusQuantity);
      }

      // DMERC_LINE_PRCNG_STATE_CD => ExplanationOfBenefit.item.extension
      line.getPricingStateCode()
          .ifPresent(
              c ->
                  item.addExtension(
                      TransformerUtilsV2.createExtensionCoding(
                          eob,
                          CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
                          line.getPricingStateCode())));

      // DMERC_LINE_SUPPLR_TYPE_CD => ExplanationOfBenefit.item.extension
      line.getSupplierTypeCode()
          .ifPresent(
              c ->
                  item.addExtension(
                      TransformerUtilsV2.createExtensionCoding(
                          eob,
                          CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
                          line.getSupplierTypeCode())));

      // Common item level fields between Carrier and DME
      // LINE_NUM => ExplanationOfBenefit.item.sequence
      // LINE_SRVC_CNT => ExplanationOfBenefit.item.quantity
      // LINE_CMS_TYPE_SRVC_CD => ExplanationOfBenefit.item.category
      // LINE_PLACE_OF_SRVC_CD => ExplanationOfBenefit.item.location
      // LINE_1ST_EXPNS_DT => ExplanationOfBenefit.item.servicedPeriod
      // LINE_LAST_EXPNS_DT => ExplanationOfBenefit.item.servicedPeriod
      // LINE_NCH_PMT_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_PMT_80_100_CD => ExplanationOfBenefit.item.adjudication.extension
      // PAID_TO_PATIENT => ExplanationOfBenefit.item.adjudication
      // LINE_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD => ExplanationOfBenefit.item.extension
      // LINE_BENE_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.item.adjudication
      // BETOS_CD => ExplanationOfBenefit.item.extension
      // LINE_COINSRNC_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_SBMTD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_ALOWD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_SERVICE_DEDUCTIBLE => ExplanationOfBenefit.item.extension
      // LINE_HCT_HGB_TYPE_CD => Observation.code
      // LINE_HCT_HGB_RSLT_NUM => Observation.value
      // LINE_NDC_CD => ExplanationOfBenefit.item.productOrService
      // LINE_BENE_PMT_AMT => ExplanationOfBenefit.item.adjudication.value
      // LINE_PRCSG_IND_CD => ExplanationOfBenefit.item.extension
      // LINE_DME_PRCHS_PRICE_AMT => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.mapEobCommonItemCarrierDME(
          item,
          eob,
          claimGroup.getClaimId(),
          item.getSequence(),
          line.getServiceCount(),
          line.getPlaceOfServiceCode(),
          line.getFirstExpenseDate(),
          line.getLastExpenseDate(),
          line.getBeneficiaryPaymentAmount(),
          line.getProviderPaymentAmount(),
          line.getBeneficiaryPartBDeductAmount(),
          line.getPrimaryPayerCode(),
          line.getPrimaryPayerPaidAmount(),
          line.getBetosCode(),
          line.getPaymentAmount(),
          line.getPaymentCode(),
          line.getCoinsuranceAmount(),
          line.getSubmittedChargeAmount(),
          line.getAllowedChargeAmount(),
          line.getProcessingIndicatorCode(),
          line.getServiceDeductibleCode(),
          line.getHctHgbTestTypeCode(),
          line.getHctHgbTestResult(),
          line.getCmsServiceTypeCode(),
          line.getNationalDrugCode(),
          CommonTransformerUtils.buildReplaceDrugCode(line.getNationalDrugCode()));

      // LINE_ICD_DGNS_CD => ExplanationOfBenefit.item.diagnosisSequence
      // LINE_ICD_DGNS_VRSN_CD => ExplanationOfBenefit.item.diagnosisSequence
      DiagnosisUtilV2.addDiagnosisLink(
          eob,
          item,
          Diagnosis.from(
              line.getDiagnosisCode(), line.getDiagnosisCodeVersion(), DiagnosisLabel.OTHER),
          ClaimType.CARRIER);

      // PRVDR_STATE_CD => ExplanationOfBenefit.item.location.extension
      if (line.getProviderStateCode() != null) {
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, line.getProviderStateCode()));
      }

      // LINE_BENE_PMT_AMT
      // claimLine.getBeneficiaryPaymentAmount() => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_BENE_PMT_AMT,
              C4BBAdjudication.PAID_TO_PROVIDER,
              line.getPurchasePriceAmount()));

      // LINE_DME_PRCHS_PRICE_AMT
      // claimLine.getPurchasePriceAmount() => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
              C4BBAdjudication.SUBMITTED,
              line.getPurchasePriceAmount()));
    }
  }
}
