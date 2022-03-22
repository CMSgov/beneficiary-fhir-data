package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.server.war.IDrugCodeProvider;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

/**
 * Transforms CCW {@link CarrierClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
final class CarrierClaimTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link CarrierClaim} to transform
   * @param includeTaxNumbers whether or not to include tax numbers in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     CarrierClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry,
      Object claim,
      Optional<Boolean> includeTaxNumbers,
      IDrugCodeProvider drugCodeProvider) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(CarrierClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof CarrierClaim)) throw new BadCodeMonkeyException();
    ExplanationOfBenefit eob =
        transformClaim((CarrierClaim) claim, includeTaxNumbers, drugCodeProvider);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link CarrierClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     CarrierClaim}
   */
  private static ExplanationOfBenefit transformClaim(
      CarrierClaim claimGroup,
      Optional<Boolean> includeTaxNumbers,
      IDrugCodeProvider drugCodeProvider) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.CARRIER,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    TransformerUtils.mapEobWeeklyProcessDate(eob, claimGroup.getWeeklyProcessDate());

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(
        eob,
        ClaimType.CARRIER,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    TransformerUtils.addAdjudicationTotal(
        eob, CcwCodebookVariable.PRPAYAMT, claimGroup.getPrimaryPayerPaidAmount());

    // Common group level fields between Carrier and DME
    TransformerUtils.mapEobCommonGroupCarrierDME(
        eob,
        claimGroup.getBeneficiaryId(),
        claimGroup.getCarrierNumber(),
        claimGroup.getClinicalTrialNumber(),
        claimGroup.getBeneficiaryPartBDeductAmount(),
        claimGroup.getPaymentDenialCode(),
        claimGroup.getReferringPhysicianNpi(),
        claimGroup.getProviderAssignmentIndicator(),
        claimGroup.getProviderPaymentAmount(),
        claimGroup.getBeneficiaryPaymentAmount(),
        claimGroup.getSubmittedChargeAmount(),
        claimGroup.getAllowedChargeAmount(),
        claimGroup.getClaimDispositionCode(),
        claimGroup.getClaimCarrierControlNumber());

    for (Diagnosis diagnosis :
        TransformerUtils.extractDiagnoses1Thru12(
            claimGroup.getDiagnosisPrincipalCode(), claimGroup.getDiagnosisPrincipalCodeVersion(),
            claimGroup.getDiagnosis1Code(), claimGroup.getDiagnosis1CodeVersion(),
            claimGroup.getDiagnosis2Code(), claimGroup.getDiagnosis2CodeVersion(),
            claimGroup.getDiagnosis3Code(), claimGroup.getDiagnosis3CodeVersion(),
            claimGroup.getDiagnosis4Code(), claimGroup.getDiagnosis4CodeVersion(),
            claimGroup.getDiagnosis5Code(), claimGroup.getDiagnosis5CodeVersion(),
            claimGroup.getDiagnosis6Code(), claimGroup.getDiagnosis6CodeVersion(),
            claimGroup.getDiagnosis7Code(), claimGroup.getDiagnosis7CodeVersion(),
            claimGroup.getDiagnosis8Code(), claimGroup.getDiagnosis8CodeVersion(),
            claimGroup.getDiagnosis9Code(), claimGroup.getDiagnosis9CodeVersion(),
            claimGroup.getDiagnosis10Code(), claimGroup.getDiagnosis10CodeVersion(),
            claimGroup.getDiagnosis11Code(), claimGroup.getDiagnosis11CodeVersion(),
            claimGroup.getDiagnosis12Code(), claimGroup.getDiagnosis12CodeVersion()))
      TransformerUtils.addDiagnosisCode(eob, diagnosis);

    for (CarrierClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber().intValue());

      /*
       * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing provider _should_ always
       * be present. However, we've found some examples in production where it's not for some claim
       * lines. (This is annoying, as it's present on other lines in the same claim, and the data
       * indicates that the same NPI probably applies to the lines where it's not specified. Still,
       * it's not safe to guess at this, so we'll leave it blank.)
       */
      if (claimLine.getPerformingPhysicianNpi().isPresent()) {
        ExplanationOfBenefit.CareTeamComponent performingCareTeamMember =
            TransformerUtils.addCareTeamPractitioner(
                eob,
                item,
                TransformerConstants.CODING_NPI_US,
                claimLine.getPerformingPhysicianNpi().get(),
                ClaimCareteamrole.PRIMARY);
        performingCareTeamMember.setResponsible(true);

        /*
         * The provider's "specialty" and "type" code are equivalent. However, the "specialty" codes
         * are more granular, and seem to better match the example FHIR
         * `http://hl7.org/fhir/ex-providerqualification` code set. Accordingly, we map the
         * "specialty" codes to the `qualification` field here, and stick the "type" code into an
         * extension. TODO: suggest that the spec allows more than one `qualification` entry.
         */

        performingCareTeamMember.setQualification(
            TransformerUtils.createCodeableConcept(
                eob, CcwCodebookVariable.PRVDR_SPCLTY, claimLine.getProviderSpecialityCode()));
        performingCareTeamMember.addExtension(
            TransformerUtils.createExtensionCoding(
                eob, CcwCodebookVariable.CARR_LINE_PRVDR_TYPE_CD, claimLine.getProviderTypeCode()));

        performingCareTeamMember.addExtension(
            TransformerUtils.createExtensionCoding(
                eob,
                CcwCodebookVariable.PRTCPTNG_IND_CD,
                claimLine.getProviderParticipatingIndCode()));
        // FIXME: Following addExtensionCoding should be a new method
        // addExtensionReference
        if (claimLine.getOrganizationNpi().isPresent()) {
          TransformerUtils.addExtensionCoding(
              performingCareTeamMember,
              TransformerConstants.CODING_NPI_US,
              TransformerConstants.CODING_NPI_US,
              TransformerUtils.retrieveNpiCodeDisplay(claimLine.getOrganizationNpi().get()),
              "" + claimLine.getOrganizationNpi().get());
        }
      }

      /*
       * FIXME This value seems to be just a "synonym" for the performing physician NPI and should
       * probably be mapped as an extra identifier with it (if/when that lands in a contained
       * Practitioner resource).
       */
      if (includeTaxNumbers.orElse(false)) {
        ExplanationOfBenefit.CareTeamComponent providerTaxNumber =
            TransformerUtils.addCareTeamPractitioner(
                eob,
                item,
                IdentifierType.TAX.getSystem(),
                claimLine.getProviderTaxNumber(),
                ClaimCareteamrole.OTHER);
        providerTaxNumber.setResponsible(true);
      }

      item.addAdjudication(
          TransformerUtils.createAdjudicationWithReason(
              eob,
              CcwCodebookVariable.CARR_LINE_RDCD_PMT_PHYS_ASTN_C,
              claimLine.getReducedPaymentPhysicianAsstCode()));

      TransformerUtils.mapHcpcs(
          eob,
          item,
          claimGroup.getHcpcsYearCode(),
          claimLine.getHcpcsCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode()));

      if (claimLine.getAnesthesiaUnitCount().compareTo(BigDecimal.ZERO) > 0) {
        item.getService()
            .addExtension(
                TransformerUtils.createExtensionQuantity(
                    CcwCodebookVariable.CARR_LINE_ANSTHSA_UNIT_CNT,
                    claimLine.getAnesthesiaUnitCount()));
      }

      if (claimLine.getMtusCode().isPresent()) {
        item.addExtension(
            TransformerUtils.createExtensionCoding(
                eob, CcwCodebookVariable.CARR_LINE_MTUS_CD, claimLine.getMtusCode()));
      }

      if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
        item.addExtension(
            TransformerUtils.createExtensionQuantity(
                CcwCodebookVariable.CARR_LINE_MTUS_CNT, claimLine.getMtusCount()));
      }

      String drugCodeName = null;
      if (claimLine.getNationalDrugCode().isPresent()) {
        drugCodeProvider.retrieveFDADrugCodeDisplay(claimLine.getNationalDrugCode().get());
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
          claimLine.getNationalDrugCode(),
          drugCodeName);

      if (claimLine.getProviderStateCode().isPresent()) {
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_STATE_CD, claimLine.getProviderStateCode()));
      }

      if (claimLine.getProviderZipCode().isPresent()) {
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob, CcwCodebookVariable.PRVDR_ZIP, claimLine.getProviderZipCode()));
      }
      item.getLocation()
          .addExtension(
              TransformerUtils.createExtensionCoding(
                  eob,
                  CcwCodebookVariable.CARR_LINE_PRCNG_LCLTY_CD,
                  claimLine.getLinePricingLocalityCode()));
      if (claimLine.getCliaLabNumber().isPresent()) {
        item.getLocation()
            .addExtension(
                TransformerUtils.createExtensionIdentifier(
                    CcwCodebookVariable.CARR_LINE_CLIA_LAB_NUM, claimLine.getCliaLabNumber()));
      }
    }

    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
