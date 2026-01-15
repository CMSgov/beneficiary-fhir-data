package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms {@link CarrierClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
final class CarrierClaimTransformer implements ClaimTransformerInterface {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(CarrierClaimTransformer.class.getSimpleName(), "transform");

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
  public CarrierClaimTransformer(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param claimEntity the {@link CarrierClaim} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevelDstu3(claimEntity.getSecurityTags());

    if (!(claim instanceof CarrierClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      CarrierClaim carrierClaim = (CarrierClaim) claim;
      eob = transformClaim(carrierClaim, securityTags);
    }

    return eob;
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link CarrierClaim} to transform
   * @param securityTags securityTags of a claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     CarrierClaim}
   */
  private ExplanationOfBenefit transformClaim(CarrierClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.CARRIER,
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

    TransformerUtils.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(d -> TransformerUtils.addDiagnosisCode(eob, d));

    for (CarrierClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());

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
        TransformerUtils.addCareTeamQualification(
            performingCareTeamMember,
            eob,
            CcwCodebookVariable.PRVDR_SPCLTY,
            claimLine.getProviderSpecialityCode());

        // CARR_LINE_PRVDR_TYPE_CD => ExplanationOfBenefit.careTeam.extension
        TransformerUtils.addCareTeamExtension(
            CcwCodebookVariable.CARR_LINE_PRVDR_TYPE_CD,
            claimLine.getProviderTypeCode(),
            performingCareTeamMember,
            eob);

        // PRTCPTNG_IND_CD => ExplanationOfBenefit.careTeam.extension
        TransformerUtils.addCareTeamExtension(
            CcwCodebookVariable.PRTCPTNG_IND_CD,
            claimLine.getProviderParticipatingIndCode(),
            performingCareTeamMember,
            eob);

        // addExtensionReference
        boolean createNpiUsExtension =
            claimLine.getOrganizationNpi().isPresent()
                && !Strings.isNullOrEmpty(claimLine.getOrganizationNpi().get())
                && !TransformerUtils.careTeamHasMatchingExtension(
                    performingCareTeamMember,
                    TransformerConstants.CODING_NPI_US,
                    claimLine.getOrganizationNpi().get());

        if (createNpiUsExtension) {
          TransformerUtils.addExtensionCoding(
              performingCareTeamMember,
              TransformerConstants.CODING_NPI_US,
              TransformerConstants.CODING_NPI_US,
              CommonTransformerUtils.buildReplaceOrganization(claimLine.getOrganizationNpi())
                  .map(NPIData::getProviderOrganizationName),
              claimLine.getOrganizationNpi().get());
        }
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
          CommonTransformerUtils.buildReplaceDrugCode(claimLine.getNationalDrugCode()));

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

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }
    return eob;
  }
}
