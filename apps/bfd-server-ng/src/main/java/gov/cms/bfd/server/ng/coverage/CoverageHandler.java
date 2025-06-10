package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.IdrConstants;
import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlement;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlementReason;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler methods for the Coverage resource. This is called after the FHIR inputs from the resource
 * provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class CoverageHandler {

  private final BeneficiaryRepository beneficiaryRepository;

  private final CoverageRepository coverageRepository;

  /**
   * Reads a Coverage resource based on a composite ID ({part}-{bene_sk}).
   *
   * @param coverageCompositeId The parsed and validated composite ID containing the CoveragePart
   *     and beneSk.
   * @param compositeId The original full ID string from the request, used for setting Coverage.id.
   * @return An {@link Optional} containing the {@link Coverage} resource if found, otherwise empty.
   * @throws InvalidRequestException if the compositeId format is invalid.
   */
  @Transactional(readOnly = true)
  public Optional<Coverage> readCoverage(
      final CoverageCompositeId coverageCompositeId, final String compositeId) {

    long beneSk = coverageCompositeId.beneSk();

    // Fetch all necessary details using the CoverageRepository
    Optional<CoverageDetails> detailsOpt =
        coverageRepository.findCoverageDetailsSingleQueryAttempt(
            coverageCompositeId, new DateTimeRange());

    return toFhir(detailsOpt.get(), coverageCompositeId.coveragePart(), compositeId);
  }

  /**
   * Transforms the aggregated {@link CoverageDetails} into a FHIR {@link Coverage} resource.
   *
   * @param detailsSO The service object containing all necessary beneficiary and related details.
   * @param partEnumInstance The {@link CoveragePart} enum instance (e.g., PART_A, PART_B).
   * @param fullFhirIdString The complete ID for the FHIR Coverage resource.
   * @return An {@link Optional} containing the populated {@link Coverage} resource.
   */
  private Optional<Coverage> toFhir(
      CoverageDetails detailsSO, CoveragePart partEnumInstance, String fullFhirIdString) {

    Beneficiary beneficiary = detailsSO.getBeneficiary();

    Coverage coverage = beneficiary.toFhirCoverage(fullFhirIdString, partEnumInstance);

    List<Extension> extensions = new ArrayList<>();

    // Populate Period, Status, and Buy-in Extension from ThirdPartyDetails
    Optional<BeneficiaryThirdParty> thirdPartyOpt = detailsSO.getThirdPartyDetails();

    thirdPartyOpt.ifPresent(
        currentThirdParty -> {
          LocalDate benefitPeriodStartDate = currentThirdParty.getBenefitRangeBeginDate();
          LocalDate benefitPeriodEndDate = currentThirdParty.getBenefitRangeEndDate();

          if (benefitPeriodStartDate != null) { // Period requires at least a start date
            Period period = new Period().setStart(DateUtil.toDate(benefitPeriodStartDate));
            if (benefitPeriodEndDate != null
                && benefitPeriodEndDate.isBefore(LocalDate.of(9999, 12, 31))) {
              period.setEnd(DateUtil.toDate(benefitPeriodEndDate));
            }
            coverage.setPeriod(period);
          }
          // Add Buy-in Code Extension
          addExtensionIfPresent(
              extensions,
              SystemUrls.EXT_BENE_BUYIN_CD_URL,
              SystemUrls.SYS_BENE_BUYIN_CD,
              currentThirdParty.getBuyInCode());
        });

    // Populate Extensions from other details
    Optional<BeneficiaryStatus> statusOpt = detailsSO.getCurrentStatus();
    String medicareStatusCode =
        statusOpt.map(BeneficiaryStatus::getMedicareStatusCode).orElse(null);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_MDCR_STUS_CD_URL,
        SystemUrls.SYS_BENE_MDCR_STUS_CD,
        medicareStatusCode);

    Optional<BeneficiaryEntitlement> entitlementOpt = detailsSO.getPartEntitlement();
    String enrollmentReasonCode =
        entitlementOpt.map(BeneficiaryEntitlement::getMedicareEnrollmentReasonCode).orElse(null);
    String entitlementStatusCode =
        entitlementOpt.map(BeneficiaryEntitlement::getMedicareEntitlementStatusCode).orElse(null);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_ENRLMT_RSN_CD_URL,
        SystemUrls.SYS_BENE_ENRLMT_RSN_CD,
        enrollmentReasonCode);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_MDCR_ENTLMT_STUS_CD_URL,
        SystemUrls.SYS_BENE_MDCR_ENTLMT_STUS_CD,
        entitlementStatusCode);

    Optional<BeneficiaryEntitlementReason> reasonOpt = detailsSO.getCurrentEntitlementReason();
    String currentEntitlementReasonCode =
        reasonOpt.map(BeneficiaryEntitlementReason::getMedicareEntitlementReasonCode).orElse(null);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL,
        SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD,
        currentEntitlementReasonCode);

    // ESRD and Disability extensions (depend on medicareStatusCode)
    if (medicareStatusCode != null) {
      IdrConstants.translateMedicareStatusToEsrdCode(medicareStatusCode)
          .ifPresent(
              esrdCode ->
                  addExtensionIfPresent(
                      extensions,
                      SystemUrls.EXT_BENE_ESRD_STUS_ID_URL,
                      SystemUrls.SYS_BENE_ESRD_STUS_ID,
                      esrdCode));
      IdrConstants.translateMedicareStatusToDisabilityCode(medicareStatusCode)
          .ifPresent(
              disabilityCode ->
                  addExtensionIfPresent(
                      extensions,
                      SystemUrls.EXT_BENE_DSBLD_STUS_ID_URL,
                      SystemUrls.SYS_BENE_DSBLD_STUS_ID,
                      disabilityCode));
    }

    if (!extensions.isEmpty()) {
      coverage.setExtension(extensions);
    }

    return Optional.of(coverage);
  }

  private void addExtensionIfPresent(
      List<Extension> extensionsList, String url, String system, String code) {
    if (code != null && !code.isBlank()) {
      extensionsList.add(new Extension(url).setValue(new Coding(system, code, null)));
    }
  }
}
