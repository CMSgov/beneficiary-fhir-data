package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

/** Transforms CCW {@link DMEClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
final class DMEClaimTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link DMEClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(DMEClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof DMEClaim)) throw new BadCodeMonkeyException();
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
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.DME,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(
        eob,
        ClaimType.DME,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // TODO is this column nullable? If so, why isn't it Optional?
    if (claimGroup.getPrimaryPayerPaidAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.PRPAYAMT, claimGroup.getPrimaryPayerPaidAmount());
    }

    // Common group level fields between Carrier and DME
    TransformerUtils.mapEobCommonGroupCarrierDME(
        eob,
        claimGroup.getBeneficiaryId(),
        claimGroup.getCarrierNumber(),
        claimGroup.getClinicalTrialNumber(),
        claimGroup.getBeneficiaryPartBDeductAmount(),
        claimGroup.getPaymentDenialCode(),
        claimGroup.getReferringPhysicianNpi(),
        Optional.of(claimGroup.getProviderAssignmentIndicator()),
        claimGroup.getProviderPaymentAmount(),
        claimGroup.getBeneficiaryPaymentAmount(),
        claimGroup.getSubmittedChargeAmount(),
        claimGroup.getAllowedChargeAmount());

    for (Diagnosis diagnosis :
        TransformerUtils.extractDiagnoses1Thru12(
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
            claimGroup.getDiagnosis12CodeVersion()))
      TransformerUtils.addDiagnosisCode(eob, diagnosis);

    for (DMEClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber().intValue());

      /*
       * add an extension for the provider billing number as there is not a good place
       * to map this in the existing FHIR specification
       */
      if (claimLine.getProviderBillingNumber().isPresent()) {
        item.addExtension(
            TransformerUtils.createExtensionIdentifier(
                CcwCodebookVariable.SUPLRNUM, claimLine.getProviderBillingNumber()));
      }

      /*
       * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing provider
       * _should_ always be present. However, we've found some examples in production
       * where it's not for some claim lines. (This is annoying, as it's present on
       * other lines in the same claim, and the data indicates that the same NPI
       * probably applies to the lines where it's not specified. Still, it's not safe
       * to guess at this, so we'll leave it blank.)
       */
      if (claimLine.getProviderNPI().isPresent()) {
        ExplanationOfBenefit.CareTeamComponent performingCareTeamMember =
            TransformerUtils.addCareTeamPractitioner(
                eob,
                item,
                TransformerConstants.CODING_NPI_US,
                claimLine.getProviderNPI().get(),
                ClaimCareteamrole.PRIMARY);
        performingCareTeamMember.setResponsible(true);

        /*
         * The provider's "specialty" and "type" code are equivalent.
         * However, the "specialty" codes are more granular, and seem to
         * better match the example FHIR
         * `http://hl7.org/fhir/ex-providerqualification` code set.
         * Accordingly, we map the "specialty" codes to the
         * `qualification` field here, and stick the "type" code into an
         * extension. TODO: suggest that the spec allows more than one
         * `qualification` entry.
         */
        performingCareTeamMember.setQualification(
            TransformerUtils.createCodeableConcept(
                eob, CcwCodebookVariable.PRVDR_SPCLTY, claimLine.getProviderSpecialityCode()));

        performingCareTeamMember.addExtension(
            TransformerUtils.createExtensionCoding(
                eob,
                CcwCodebookVariable.PRTCPTNG_IND_CD,
                claimLine.getProviderParticipatingIndCode()));
      }

      TransformerUtils.mapHcpcs(
          eob,
          item,
          claimGroup.getHcpcsYearCode(),
          claimLine.getHcpcsCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(),
              claimLine.getHcpcsSecondModifierCode(),
              claimLine.getHcpcsThirdModifierCode(),
              claimLine.getHcpcsFourthModifierCode()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getPrimaryPayerAllowedChargeAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getPurchasePriceAmount()));

      if (claimLine.getScreenSavingsAmount().isPresent()) {
        // TODO should this be an adjudication?
        item.addExtension(
            TransformerUtils.createExtensionQuantity(
                CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT, claimLine.getScreenSavingsAmount()));
      }

      Extension mtusQuantityExtension =
          TransformerUtils.createExtensionQuantity(
              CcwCodebookVariable.DMERC_LINE_MTUS_CNT, claimLine.getMtusCount());
      item.addExtension(mtusQuantityExtension);
      if (claimLine.getMtusCode().isPresent()) {
        Quantity mtusQuantity = (Quantity) mtusQuantityExtension.getValue();
        TransformerUtils.setQuantityUnitInfo(
            CcwCodebookVariable.DMERC_LINE_MTUS_CD, claimLine.getMtusCode(), eob, mtusQuantity);
      }

      // Common item level fields between Carrier and DME
      TransformerUtils.mapEobCommonItemCarrierDME(
          item,
          eob,
          claimGroup.getClaimId(),
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

      if (!claimLine.getProviderStateCode().isEmpty()) {
        // FIXME Should this be pulled to a common mapping method?
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, claimLine.getProviderStateCode()));
      }
      if (claimLine.getPricingStateCode().isPresent()) {
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
                    claimLine.getPricingStateCode()));
      }

      if (claimLine.getSupplierTypeCode().isPresent()) {
        // TODO should this be elsewhere; does it item.location make sense?
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
                    claimLine.getSupplierTypeCode()));
      }
    }
    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
