package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Quantity;

/** Transforms CCW {@link DMEClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
final class DMEClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link DMEClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry, Object claim, Optional<Boolean> includeTaxNumbers) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(DMEClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof DMEClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob = transformClaim((DMEClaim) claim, includeTaxNumbers);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link DMEClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  private static ExplanationOfBenefit transformClaim(
      DMEClaim claimGroup, Optional<Boolean> includeTaxNumbers) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL);

    // Common group level fields between all claim types
    // Claim Type + Claim ID    => ExplanationOfBenefit.id
    // CLM_ID                   => ExplanationOfBenefit.identifier
    // CLM_GRP_ID               => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type  => ExplanationOfBenefit.insurance.coverage
    // BENE_ID                  => ExplanationOfBenefit.patient (reference)pwd
    // FINAL_ACTION             => ExplanationOfBenefit.status
    // CLM_FROM_DT              => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT              => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT              => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimTypeV2.DME,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD            => ExplanationOfBenefit.type.coding
    // EOB Type                   => ExplanationOfBenefit.type.coding
    // Claim Type (Professional)  => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimTypeV2.DME,
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
    // CARR_NUM                       => ExplanationOfBenefit.extension
    // CLM_CLNCL_TRIL_NUM             => ExplanationOfBenefit.extension
    // CARR_CLM_CASH_DDCTBL_APLD_AMT  => ExplanationOfBenefit.benefitBalance.financial
    // CARR_CLM_PMT_DNL_CD            => ExplanationOfBenefit.extension
    // RFR_PHYSN_NPI                  => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // RFR_PHYSN_UPIN                 => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // CARR_CLM_PRVDR_ASGNMT_IND_SW   => ExplanationOfBenefit.extension
    // NCH_CLM_PRVDR_PMT_AMT          => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CLM_BENE_PMT_AMT           => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_SBMTD_CHRG_AMT    => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_ALOWD_AMT         => ExplanationOfBenefit.benefitBalance.financial
    // CLM_DISP_CD                    => ExplanationOfBenefit.disposition
    // CARR_CLM_CNTL_NUM              => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobCommonGroupCarrierDME(
        eob,
        claimGroup.getBeneficiaryId(),
        claimGroup.getCarrierNumber(),
        claimGroup.getClinicalTrialNumber(),
        claimGroup.getBeneficiaryPartBDeductAmount(),
        claimGroup.getPaymentDenialCode(),
        claimGroup.getReferringPhysicianNpi(),
        claimGroup.getReferringPhysicianUpin(),
        Optional.of(claimGroup.getProviderAssignmentIndicator()),
        claimGroup.getProviderPaymentAmount(),
        claimGroup.getBeneficiaryPaymentAmount(),
        claimGroup.getSubmittedChargeAmount(),
        claimGroup.getAllowedChargeAmount(),
        claimGroup.getClaimDispositionCode(),
        claimGroup.getClaimCarrierControlNumber());

    // PRNCPAL_DGNS_CD          => diagnosis.diagnosisCodeableConcept
    // PRNCPAL_DGNS_VRSN_CD     => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-12)        => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-12)   => diagnosis.diagnosisCodeableConcept
    for (Diagnosis diagnosis : DiagnosisUtilV2.extractDiagnoses(claimGroup)) {
      DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimTypeV2.DME);
    }

    // CARR_CLM_ENTRY_CD => ExplanationOfBenefit.extension
    eob.addExtension(
        TransformerUtilsV2.createExtensionCoding(
            eob, CcwCodebookVariable.CARR_CLM_ENTRY_CD, claimGroup.getClaimEntryCode()));

    handleClaimLines(claimGroup, eob, includeTaxNumbers);
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }

  private static void handleClaimLines(
      DMEClaim claimGroup, ExplanationOfBenefit eob, Optional<Boolean> includeTaxNumbers) {
    for (DMEClaimLine line : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(line.getLineNumber().intValue());

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
              line.getProviderNPI());

      // Update the responsible flag
      performing.ifPresent(
          p -> {
            p.setResponsible(true);

            // PRVDR_SPCLTY => ExplanationOfBenefit.careTeam.qualification
            p.setQualification(
                TransformerUtilsV2.createCodeableConcept(
                    eob, CcwCodebookVariable.PRVDR_SPCLTY, line.getProviderSpecialityCode()));

            // PRTCPTNG_IND_CD => ExplanationOfBenefit.careTeam.extension
            p.addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.PRTCPTNG_IND_CD,
                    line.getProviderParticipatingIndCode()));
          });

      // PRVDR_STATE_CD => ExplanationOfBenefit.item.location.extension
      if (item.getLocation() != null) {
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, line.getProviderStateCode()));
      }

      // HCPCS_CD            => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD   => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD   => ExplanationOfBenefit.item.modifier
      // HCPCS_3RD_MDFR_CD   => ExplanationOfBenefit.item.modifier
      // HCPCS_4Th_MDFR_CD   => ExplanationOfBenefit.item.modifier
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

      if (includeTaxNumbers.orElse(false)) {
        item.addExtension(
            TransformerUtilsV2.createExtensionCoding(
                eob, CcwCodebookVariable.TAX_NUM, line.getProviderTaxNumber()));
      }

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
      // LINE_NUM                 => ExplanationOfBenefit.item.sequence
      // LINE_SRVC_CNT            => ExplanationOfBenefit.item.quantity
      // LINE_CMS_TYPE_SRVC_CD    => ExplanationOfBenefit.item.category
      // LINE_PLACE_OF_SRVC_CD    => ExplanationOfBenefit.item.location
      // LINE_1ST_EXPNS_DT        => ExplanationOfBenefit.item.servicedPeriod
      // LINE_LAST_EXPNS_DT       => ExplanationOfBenefit.item.servicedPeriod
      // LINE_NCH_PMT_AMT         => ExplanationOfBenefit.item.adjudication
      // LINE_PMT_80_100_CD       => ExplanationOfBenefit.item.adjudication.extension
      // PAID_TO_PATIENT          => ExplanationOfBenefit.item.adjudication
      // LINE_PRVDR_PMT_AMT       => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_BENE_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.item.adjudication
      // BETOS_CD                 => ExplanationOfBenefit.item.extension
      // LINE_COINSRNC_AMT        => ExplanationOfBenefit.item.adjudication
      // LINE_SBMTD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_ALOWD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_SERVICE_DEDUCTIBLE  => ExplanationOfBenefit.item.extension
      // LINE_HCT_HGB_TYPE_CD     => Observation.code
      // LINE_HCT_HGB_RSLT_NUM    => Observation.value
      // LINE_NDC_CD              => ExplanationOfBenefit.item.productOrService
      // LINE_BENE_PMT_AMT        => ExplanationOfBenefit.item.adjudication.value
      // LINE_PRCSG_IND_CD        => ExplanationOfBenefit.item.extension
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
          line.getNationalDrugCode());

      // LINE_ICD_DGNS_CD      => ExplanationOfBenefit.item.diagnosisSequence
      // LINE_ICD_DGNS_VRSN_CD => ExplanationOfBenefit.item.diagnosisSequence
      DiagnosisUtilV2.addDiagnosisLink(
          eob,
          item,
          Diagnosis.from(line.getDiagnosisCode(), line.getDiagnosisCodeVersion()),
          ClaimTypeV2.CARRIER);

      // PRVDR_STATE_CD => ExplanationOfBenefit.item.location.extension
      if (line.getProviderStateCode() != null) {
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, line.getProviderStateCode()));
      }

      // LINE_BENE_PRMRY_PYR_CD
      // claimLine.getPrimaryPayerCode()) => ExplanationOfBenefit.item.extension
      line.getPrimaryPayerCode()
          .ifPresent(
              c ->
                  item.addExtension(
                      TransformerUtilsV2.createExtensionCoding(
                          eob,
                          CcwCodebookVariable.LINE_BENE_PRMRY_PYR_CD,
                          line.getPrimaryPayerCode())));

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

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param eob The {@link ExplanationOfBenefit} to ExplanationOfBenefit details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the
   *     ExplanationOfBenefit
   * @param optVal The {@link String} value associated with the ExplanationOfBenefit
   */
  static void addExtension(
      ExplanationOfBenefit eob, CcwCodebookVariable ccwVariable, Optional<String> optVal) {
    optVal.ifPresent(
        value ->
            eob.addExtension(TransformerUtilsV2.createExtensionCoding(eob, ccwVariable, value)));
  }

  /**
   * Sets the ExplanationOfBenefit.relationship Looks up or adds a contained {@link Identifier}
   * object to the current {@link Patient}. This is used to store Identifier slices related to the
   * Provider organization.
   *
   * @param eob The {@link ExplanationOfBenefit} to ExplanationOfBenefit details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the
   *     ExplanationOfBenefit
   * @param optVal The {@link Character} value associated with the ExplanationOfBenefit
   */
  static void addCodeExtension(
      ExplanationOfBenefit eob, CcwCodebookVariable ccwVariable, Optional<Character> optVal) {
    optVal.ifPresent(
        value ->
            eob.addExtension(TransformerUtilsV2.createExtensionCoding(eob, ccwVariable, value)));
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param eob The {@link ExplanationOfBenefit} to ExplanationOfBenefit details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the
   *     ExplanationOfBenefit
   * @param optVal The {@link BigDecimal} value associated with the ExplanationOfBenefit
   */
  static void addDecimalExtension(
      ExplanationOfBenefit eob, CcwCodebookVariable ccwVariable, Optional<BigDecimal> optVal) {
    eob.addExtension(TransformerUtilsV2.createExtensionDate(ccwVariable, optVal));
  }
}
