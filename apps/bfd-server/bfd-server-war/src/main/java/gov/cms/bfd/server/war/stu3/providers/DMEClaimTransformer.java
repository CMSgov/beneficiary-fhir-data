package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.springframework.stereotype.Component;

/** Transforms {@link DMEClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
final class DMEClaimTransformer implements ClaimTransformerInterface {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The {@link FdaDrugCodeDisplayLookup} is to provide what drugCodeDisplay to return. */
  private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param drugCodeDisplayLookup the drug code display lookup
   */
  public DMEClaimTransformer(
      MetricRegistry metricRegistry, FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.drugCodeDisplayLookup = requireNonNull(drugCodeDisplayLookup);
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param claim the {@link DMEClaim} to use
   * @param includeTaxNumber boolean denoting whether to include tax numbers in the response
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Trace
  @Override
  public ExplanationOfBenefit transform(Object claim, boolean includeTaxNumber) {
    if (!(claim instanceof DMEClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob = null;
    try (Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(DMEClaimTransformer.class.getSimpleName(), "transform"))
            .time()) {
      eob = transformClaim((DMEClaim) claim, includeTaxNumber);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link DMEClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the {@link DMEClaim} to use
   * @param includeTaxNumbers whether to include tax numbers in the transformed EOB
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     DMEClaim}
   */
  private ExplanationOfBenefit transformClaim(DMEClaim claimGroup, boolean includeTaxNumbers) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.DME,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    TransformerUtils.mapEobWeeklyProcessDate(eob, claimGroup.getWeeklyProcessDate());

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
        claimGroup.getAllowedChargeAmount(),
        claimGroup.getClaimDispositionCode(),
        claimGroup.getClaimCarrierControlNumber());

    TransformerUtils.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .stream()
        .forEach(d -> TransformerUtils.addDiagnosisCode(eob, d));

    for (DMEClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());

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

        // PRTCPTNG_IND_CD => ExplanationOfBenefit.careTeam.extension
        boolean performingHasMatchingExtension =
            claimLine.getProviderParticipatingIndCode().isPresent()
                && TransformerUtils.careTeamHasMatchingExtension(
                    performingCareTeamMember,
                    TransformerUtils.getReferenceUrl(CcwCodebookVariable.PRTCPTNG_IND_CD),
                    String.valueOf(claimLine.getProviderParticipatingIndCode()));

        if (!performingHasMatchingExtension) {
          performingCareTeamMember.addExtension(
              TransformerUtils.createExtensionCoding(
                  eob,
                  CcwCodebookVariable.PRTCPTNG_IND_CD,
                  claimLine.getProviderParticipatingIndCode()));
        }
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

      /*
       * FIXME This value seems to be just a "synonym" for the performing physician NPI and should
       * probably be mapped as an extra identifier with it (if/when that lands in a contained
       * Practitioner resource).
       */
      if (includeTaxNumbers) {
        ExplanationOfBenefit.CareTeamComponent providerTaxNumber =
            TransformerUtils.addCareTeamPractitioner(
                eob,
                item,
                IdentifierType.TAX.getSystem(),
                claimLine.getProviderTaxNumber(),
                ClaimCareteamrole.OTHER);
        providerTaxNumber.setResponsible(true);
      }

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
          claimLine.getNationalDrugCode(),
          drugCodeDisplayLookup.retrieveFDADrugCodeDisplay(claimLine.getNationalDrugCode()));

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
