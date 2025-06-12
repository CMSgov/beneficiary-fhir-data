package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlement;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlementReason;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoveragePart;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

/**
 * A Service Object (SO) or Data Transfer Object (DTO) that aggregates all necessary data pieces
 * from various beneficiary-related tables required to construct a comprehensive FHIR Coverage
 * resource.
 */
@Getter
@Builder
@AllArgsConstructor
public class CoverageDetails {

  private Beneficiary beneficiary;

  private final Optional<BeneficiaryThirdParty> thirdPartyDetails;
  private final Optional<BeneficiaryStatus> currentStatus;
  private final Optional<BeneficiaryEntitlement> partEntitlement;
  private final Optional<BeneficiaryEntitlementReason> currentEntitlementReason;

  /**
   * No-arg constructor for frameworks or manual instantiation if needed, or if @Builder doesn't
   * generate one and it's required elsewhere.
   */
  public CoverageDetails() {
    this.thirdPartyDetails = Optional.empty();
    this.currentStatus = Optional.empty();
    this.partEntitlement = Optional.empty();
    this.currentEntitlementReason = Optional.empty();
  }

  /**
   * CoverageDetails Constructor used in CoverageDetails repository jpa query The Beneficiary object
   * will be set separately.
   *
   * @param entitlement entitlement
   * @param reason reason
   * @param status status
   * @param thirdParty thirdParty
   */
  public CoverageDetails(
      BeneficiaryThirdParty thirdParty,
      BeneficiaryStatus status,
      BeneficiaryEntitlement entitlement,
      BeneficiaryEntitlementReason reason) {
    this.thirdPartyDetails = Optional.ofNullable(thirdParty);
    this.currentStatus = Optional.ofNullable(status);
    this.partEntitlement = Optional.ofNullable(entitlement);
    this.currentEntitlementReason = Optional.ofNullable(reason);
    // this.beneficiary will be set later by the calling code
  }

  /**
   * Setter for beneficiary.
   *
   * @param beneficiary the beneficiary
   */
  public void setBeneficiary(Beneficiary beneficiary) {
    this.beneficiary = beneficiary;
  }

  /**
   * Transforms the data held in this SO into a fully populated FHIR Coverage resource.
   *
   * @param fullFhirId The ID to be assigned to the Coverage resource.
   * @param partEnumInstance The specific {@link CoveragePart} (e.g., PART_A, PART_B) this Coverage
   *     resource represents.
   * @return A FHIR {@link Coverage} object.
   */
  public Coverage toFhir(String fullFhirId, CoveragePart partEnumInstance) {

    Coverage coverage = beneficiary.toFhirCoverage(fullFhirId, partEnumInstance);

    List<Extension> extensions = new ArrayList<>();

    // Populate Period, Status (derived from period), and Buy-in Extension
    this.getThirdPartyDetails()
        .ifPresent(
            currentThirdParty -> {
              LocalDate benefitPeriodStartDate = currentThirdParty.getBenefitRangeBeginDate();
              LocalDate benefitPeriodEndDate = currentThirdParty.getBenefitRangeEndDate();

              if (benefitPeriodStartDate != null) {
                Period period = new Period().setStart(DateUtil.toDate(benefitPeriodStartDate));
                if (benefitPeriodEndDate != null
                    && benefitPeriodEndDate.isBefore(LocalDate.of(9999, 12, 31))) {
                  period.setEnd(DateUtil.toDate(benefitPeriodEndDate));
                }
                coverage.setPeriod(period);
                coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
              }
              // Add Buy-in Code Extension
              addExtensionIfPresent(
                  extensions,
                  SystemUrls.EXT_BENE_BUYIN_CD_URL,
                  SystemUrls.SYS_BENE_BUYIN_CD,
                  currentThirdParty.getBuyInCode());
            });

    // Populate other Extensions
    String medicareStatusCode =
        this.getCurrentStatus().map(BeneficiaryStatus::getMedicareStatusCode).orElse(null);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_MDCR_STUS_CD_URL,
        SystemUrls.SYS_BENE_MDCR_STUS_CD,
        medicareStatusCode);

    String enrollmentReasonCode =
        this.getPartEntitlement()
            .map(BeneficiaryEntitlement::getMedicareEnrollmentReasonCode)
            .orElse(null);
    String entitlementStatusCode =
        this.getPartEntitlement()
            .map(BeneficiaryEntitlement::getMedicareEntitlementStatusCode)
            .orElse(null);
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

    String currentEntitlementReasonCode =
        this.getCurrentEntitlementReason()
            .map(BeneficiaryEntitlementReason::getMedicareEntitlementReasonCode)
            .orElse(null);
    addExtensionIfPresent(
        extensions,
        SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL,
        SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD,
        currentEntitlementReasonCode);

    if (medicareStatusCode != null) {
      MedicareStatusCodeType.fromCode(medicareStatusCode)
          .ifPresent(
              statusCodeType -> {
                addExtensionIfPresent(
                    extensions,
                    SystemUrls.EXT_BENE_ESRD_STUS_ID_URL,
                    SystemUrls.SYS_BENE_ESRD_STUS_ID,
                    statusCodeType.getEsrdIndicator());
                addExtensionIfPresent(
                    extensions,
                    SystemUrls.EXT_BENE_DSBLD_STUS_ID_URL,
                    SystemUrls.SYS_BENE_DSBLD_STUS_ID,
                    statusCodeType.getDisabilityIndicator());
              });
    }

    if (!extensions.isEmpty()) {
      if (coverage.hasExtension()) {
        coverage.getExtension().addAll(extensions); // Add to existing if any
      } else {
        coverage.setExtension(extensions);
      }
    }

    return coverage;
  }

  // Helper method to add an extension if the code value is present.
  private static void addExtensionIfPresent(
      List<Extension> extensionsList, String url, String system, String code) {
    if (code != null && !code.isBlank()) {
      extensionsList.add(new Extension(url).setValue(new Coding(system, code, null)));
    }
  }
}
