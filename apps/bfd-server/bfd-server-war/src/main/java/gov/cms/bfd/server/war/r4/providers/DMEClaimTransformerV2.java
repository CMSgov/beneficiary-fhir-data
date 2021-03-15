package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
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
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(DMEClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof DMEClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob = transformClaim((DMEClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link DMEClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  private static ExplanationOfBenefit transformClaim(DMEClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

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
    // TODO is this column nullable? If so, why isn't it Optional?
    if (claimGroup.getPrimaryPayerPaidAmount() != null) {
      TransformerUtilsV2.addAdjudicationTotal(
          eob, CcwCodebookVariable.PRPAYAMT, claimGroup.getPrimaryPayerPaidAmount());
    }

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

    // ICD_DGNS_CD1         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD1    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD2         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD2    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD3         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD3    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD4         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD4    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD5         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD5    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD6         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD6    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD7         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD7    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD8         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD8    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD9         => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD9    => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD10        => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD10   => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD11        => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD11   => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD12        => ExplanationOfBenefit.diagnosis
    // ICD_DGNS_VRSN_CD12   => ExplanationOfBenefit.diagnosis.diagnosisCodeableConcept
    for (Diagnosis diagnosis :
        TransformerUtilsV2.extractDiagnoses1Thru12(
            claimGroup.getDiagnosisPrincipalCode(),
            claimGroup.getDiagnosisPrincipalCodeVersion(),
            claimGroup.getDiagnosis1Code(),
            claimGroup.getDiagnosis1CodeVersion(),
            claimGroup.getDiagnosis2Code(),
            claimGroup.getDiagnosis2CodeVersion(),
            claimGroup.getDiagnosis3Code(),
            claimGroup.getDiagnosis3CodeVersion(),
            claimGroup.getDiagnosis4Code(),
            claimGroup.getDiagnosis4CodeVersion(),
            claimGroup.getDiagnosis5Code(),
            claimGroup.getDiagnosis5CodeVersion(),
            claimGroup.getDiagnosis6Code(),
            claimGroup.getDiagnosis6CodeVersion(),
            claimGroup.getDiagnosis7Code(),
            claimGroup.getDiagnosis7CodeVersion(),
            claimGroup.getDiagnosis8Code(),
            claimGroup.getDiagnosis8CodeVersion(),
            claimGroup.getDiagnosis9Code(),
            claimGroup.getDiagnosis9CodeVersion(),
            claimGroup.getDiagnosis10Code(),
            claimGroup.getDiagnosis10CodeVersion(),
            claimGroup.getDiagnosis11Code(),
            claimGroup.getDiagnosis11CodeVersion(),
            claimGroup.getDiagnosis12Code(),
            claimGroup.getDiagnosis12CodeVersion())) {
      TransformerUtilsV2.addDiagnosisCode(eob, diagnosis);
    }

    handleClaimLines(claimGroup, eob);
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }

  private static void handleClaimLines(DMEClaim claimGroup, ExplanationOfBenefit eob) {
    for (DMEClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);
      System.out.println("\n\nitem: " + item);

      // Override the default sequence
      // CLM_LINE_NUM => item.sequence
      item.setSequence(claimLine.getLineNumber().intValue());
      System.out.println("lineNumber: " + claimLine.getLineNumber().intValue());

      // add an extension for the provider billing number as there is not a good place
      // to map this in the existing FHIR specification
      // PRVDR_NUM => ExplanationOfBenefit.provider.value
      if (claimLine.getProviderBillingNumber().isPresent()) {
        item.addExtension(
            TransformerUtilsV2.createExtensionIdentifier(
                CcwCodebookVariable.SUPLRNUM, claimLine.getProviderBillingNumber()));
      }

      if (claimLine.getProviderNPI().isPresent()) {
        handleProviderInfo(claimGroup, eob, claimLine, item);
        System.out.println("handleProviderInfo");
      }

      // HCPCS_CD            => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD   => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD   => ExplanationOfBenefit.item.modifier
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          claimLine.getHcpcsCode(),
          claimGroup.getHcpcsYearCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(),
              claimLine.getHcpcsSecondModifierCode(),
              claimLine.getHcpcsThirdModifierCode(),
              claimLine.getHcpcsFourthModifierCode()));

      // REV_CNTR_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
              C4BBAdjudication.PAID_TO_PROVIDER,
              claimLine.getProviderPaymentAmount()));

      // REV_CNTR_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
              C4BBAdjudication.PAID_TO_PROVIDER,
              claimLine.getProviderPaymentAmount()));

      // TODO - check w/jack if this is right ELIGIBLE
      // LINE_PRMRY_ALOWD_CHRG_AMT => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT,
              C4BBAdjudication.ELIGIBLE,
              claimLine.getPrimaryPayerAllowedChargeAmount()));

      // LINE_DME_PRCHS_PRICE_AMT => ExplanationOfBenefit.item.adjudication.value
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
              C4BBAdjudication.SUBMITTED,
              claimLine.getPurchasePriceAmount()));

      // DMERC_LINE_SCRN_SVGS_AMT => ExplanationOfBenefit.item.extension
      claimLine
          .getScreenSavingsAmount()
          .ifPresent(
              c ->
                  item.addExtension(
                      // TODO should this be an adjudication?
                      TransformerUtilsV2.createExtensionQuantity(
                          CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT,
                          claimLine.getScreenSavingsAmount())));

      // DMERC_LINE_MTUS_CNT => ExplanationOfBenefit.item.extension
      Extension mtusQuantityExtension =
          TransformerUtilsV2.createExtensionQuantity(
              CcwCodebookVariable.DMERC_LINE_MTUS_CNT, claimLine.getMtusCount());

      item.addExtension(mtusQuantityExtension);

      // DMERC_LINE_MTUS_CD => ExplanationOfBenefit.item.extension
      if (claimLine.getMtusCode().isPresent()) {
        Quantity mtusQuantity = (Quantity) mtusQuantityExtension.getValue();
        TransformerUtilsV2.setQuantityUnitInfo(
            CcwCodebookVariable.DMERC_LINE_MTUS_CD, claimLine.getMtusCode(), eob, mtusQuantity);
      }

      // Common item level fields between Carrier and DME
      // LINE_SRVC_CNT            => ExplanationOfBenefit.item.quantity
      // LINE_CMS_TYPE_SRVC_CD    => ExplanationOfBenefit.item.category
      // LINE_PLACE_OF_SRVC_CD    => ExplanationOfBenefit.item.location
      // BETOS_CD                 => ExplanationOfBenefit.item.extension
      // LINE_1ST_EXPNS_DT        => ExplanationOfBenefit.item.servicedPeriod
      // LINE_LAST_EXPNS_DT       => ExplanationOfBenefit.item.servicedPeriod
      // LINE_NCH_PMT_AMT         => ExplanationOfBenefit.item.adjudication
      // LINE_PMT_80_100_CD       => ExplanationOfBenefit.item.adjudication.extension
      // PAID_TO_PATIENT          => ExplanationOfBenefit.item.adjudication
      // LINE_PRVDR_PMT_AMT       => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_BENE_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_COINSRNC_AMT        => ExplanationOfBenefit.item.adjudication
      // LINE_SBMTD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_ALOWD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_SERVICE_DEDUCTIBLE  => ExplanationOfBenefit.item.extension
      // LINE_ICD_DGNS_CD         => ExplanationOfBenefit.item.diagnosisSequence
      // LINE_ICD_DGNS_VRSN_CD    => ExplanationOfBenefit.item.diagnosisSequence
      // LINE_HCT_HGB_TYPE_CD     => Observation.code
      // LINE_HCT_HGB_RSLT_NUM    => Observation.value
      // LINE_NDC_CD              => ExplanationOfBenefit.item.productOrService
      TransformerUtilsV2.mapEobCommonItemCarrierDME(
          item,
          eob,
          claimGroup.getClaimId(),
          item.getSequence(),
          claimLine.getServiceCount(),
          claimLine.getPlaceOfServiceCode(),
          claimLine.getFirstExpenseDate(),
          claimLine.getLastExpenseDate(),
          claimLine.getBeneficiaryPaymentAmount(),
          claimLine.getProviderPaymentAmount(),
          claimLine.getBeneficiaryPartBDeductAmount(),
          claimLine.getPrimaryPayerCode(),
          claimLine.getPrimaryPayerPaidAmount(),
          claimLine.getBetosCode(),
          claimLine.getPaymentAmount(),
          claimLine.getPaymentCode(),
          claimLine.getCoinsuranceAmount(),
          claimLine.getSubmittedChargeAmount(),
          claimLine.getAllowedChargeAmount(),
          claimLine.getProcessingIndicatorCode(),
          claimLine.getServiceDeductibleCode(),
          claimLine.getDiagnosisCode(),
          claimLine.getDiagnosisCodeVersion(),
          claimLine.getHctHgbTestTypeCode(),
          claimLine.getHctHgbTestResult(),
          claimLine.getCmsServiceTypeCode(),
          claimLine.getNationalDrugCode());

      // DMERC_LINE_PRCNG_STATE_CD => ExplanationOfBenefit.item.extension
      if (claimLine.getPricingStateCode().isPresent()) {
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
                    claimLine.getPricingStateCode()));
      }

      // DMERC_LINE_SUPPLR_TYPE_CD => ExplanationOfBenefit.item.extension
      if (claimLine.getSupplierTypeCode().isPresent()) {
        // TODO should this be elsewhere; does it item.location make sense?
        item.getLocation()
            .addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
                    claimLine.getSupplierTypeCode()));
      }
    }
  }

  private static void handleProviderInfo(
      DMEClaim claimGroup, ExplanationOfBenefit eob, DMEClaimLine line, ItemComponent item) {

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
    System.out.println("getProviderStateCode " + line.getProviderStateCode());
    if (line.getProviderStateCode() != null && !line.getProviderStateCode().isEmpty()) {
      item.getLocation()
          .addExtension(
              TransformerUtilsV2.createExtensionCoding(
                  eob, CcwCodebookVariable.PRVDR_STATE_CD, line.getProviderStateCode()));
      System.out.println("handleProviderInfo 3");
    }

    System.out.println("\n\nPart 7\n\n");

    // REV_CNTR                   => ExplanationOfBenefit.item.revenue
    // REV_CNTR_RATE_AMT          => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_TOT_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_NCVRD_CHRG_AMT    => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_UNIT_CNT          => ExplanationOfBenefit.item.quantity
    // REV_CNTR_NDC_QTY           => TODO: ??
    // REV_CNTR_NDC_QTY_QLFR_CD   => ExplanationOfBenefit.modifier
    /*
    TransformerUtilsV2.mapEobCommonItemRevenue(
        item,
        eob,
        line.getRevenueCenterCode(),
        line.getRateAmount(),
        line.getTotalChargeAmount(),
        line.getNonCoveredChargeAmount(),
        line.getUnitCount(),
        line.getNationalDrugCodeQuantity(),
        line.getNationalDrugCodeQualifierCode());
        */

    // claimGroup.getPrimaryPayerPaidAmount().ifPresent(value -> coverage.setSubscriberId(value));

    // REV_CNTR                   => ExplanationOfBenefit.item.revenue
    // REV_CNTR_RATE_AMT          => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_TOT_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_NCVRD_CHRG_AMT    => ExplanationOfBenefit.item.adjudication
    // REV_CNTR_UNIT_CNT          => ExplanationOfBenefit.item.quantity
    // REV_CNTR_NDC_QTY           => TODO: ??
    // REV_CNTR_NDC_QTY_QLFR_CD   => ExplanationOfBenefit.modifier
    /*
    TransformerUtilsV2.mapEobCommonItemRevenue(
        item,
        eob,
        line.getRevenueCenterCode(),
        line.getRateAmount(),
        line.getTotalChargeAmount(),
        line.getNonCoveredChargeAmount(),
        line.getUnitCount(),
        line.getNationalDrugCodeQuantity(),
        line.getNationalDrugCodeQualifierCode());
        */

    // add an extension for the provider billing number as there is not a good place
    // to map this in the existing FHIR specification
    // PRVDR_NUM => ExplanationOfBenefit.provider.value
    // TODO - fix this
    /*
    claimGroup
        .getProviderBillingNumber()
        .ifPresent(
            value ->
                item.addExtension(
                    TransformerUtilsV2.createExtensionIdentifier(
                        CcwCodebookVariable.SUPLRNUM, line.getProviderBillingNumber())));
                        */
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
