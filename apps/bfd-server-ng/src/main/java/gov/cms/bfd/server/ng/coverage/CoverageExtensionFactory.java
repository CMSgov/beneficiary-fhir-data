package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** A Coverage Extension Factory Class. */
public final class CoverageExtensionFactory {

  private CoverageExtensionFactory() {}

  /**
   * Creates a list of all applicable extensions for a Coverage resource based on the provided
   * details.
   *
   * @param details The {@link CoverageDetails} SO containing all necessary data.
   * @return A list of FHIR {@link Extension} objects. Can be empty.
   */
  public static List<Extension> createAllExtensions(CoverageDetails details) {
    List<Extension> allExtensions = new ArrayList<>();

    // Add extensions from thirdPartyDetails (Buy-In Code)
    details
        .getThirdPartyDetails()
        .ifPresent(tp -> createBuyInCodeExtension(tp.getBuyInCode()).ifPresent(allExtensions::add));

    // Add extensions from currentStatus (Medicare Status, ESRD, Disability)
    String medicareStatusCode =
        details.getCurrentStatus().map(BeneficiaryStatus::getMedicareStatusCode).orElse(null);

    createMedicareStatusCodeExtension(medicareStatusCode).ifPresent(allExtensions::add);

    if (medicareStatusCode != null) {
      MedicareStatusCodeType.fromCode(medicareStatusCode)
          .ifPresent(
              statusCodeType -> {
                createEsrdIndicatorExtension(statusCodeType.getEsrdIndicator())
                    .ifPresent(allExtensions::add);
                createDisabilityIndicatorExtension(statusCodeType.getDisabilityIndicator())
                    .ifPresent(allExtensions::add);
              });
    }

    // Add extensions from partEntitlement (Enrollment Reason, Entitlement Status)
    details
        .getPartEntitlement()
        .ifPresent(
            entitlement -> {
              createEnrollmentReasonCodeExtension(entitlement.getMedicareEnrollmentReasonCode())
                  .ifPresent(allExtensions::add);
              createEntitlementStatusCodeExtension(entitlement.getMedicareEntitlementStatusCode())
                  .ifPresent(allExtensions::add);
            });

    // Add extensions from currentEntitlementReason (Current Entitlement Reason)
    details
        .getCurrentEntitlementReason()
        .ifPresent(
            reason ->
                createCurrentEntitlementReasonCodeExtension(
                        reason.getMedicareEntitlementReasonCode())
                    .ifPresent(allExtensions::add));

    return allExtensions;
  }

  /**
   * create BuyIn Code Extension.
   *
   * @param buyInCode buy In Code.
   * @return optional extension
   */
  public static Optional<Extension> createBuyInCodeExtension(String buyInCode) {
    if (buyInCode == null || buyInCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_BUYIN_CD_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_BUYIN_CD, buyInCode, null)));
  }

  /**
   * create create Medicare Status Extension.
   *
   * @param medicareStatusCode medicare Status Code.
   * @return optional extension
   */
  public static Optional<Extension> createMedicareStatusCodeExtension(String medicareStatusCode) {
    if (medicareStatusCode == null || medicareStatusCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_MDCR_STUS_CD_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_MDCR_STUS_CD, medicareStatusCode, null)));
  }

  /**
   * create create Medicare Status Extension.
   *
   * @param enrollmentReasonCode enrollment Reason Code.
   * @return optional extension
   */
  public static Optional<Extension> createEnrollmentReasonCodeExtension(
      String enrollmentReasonCode) {
    if (enrollmentReasonCode == null || enrollmentReasonCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_ENRLMT_RSN_CD_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_ENRLMT_RSN_CD, enrollmentReasonCode, null)));
  }

  /**
   * create entitlement Status Extension.
   *
   * @param entitlementStatusCode entitlement Status Code.
   * @return optional extension
   */
  public static Optional<Extension> createEntitlementStatusCodeExtension(
      String entitlementStatusCode) {
    if (entitlementStatusCode == null || entitlementStatusCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_STUS_CD_URL)
            .setValue(
                new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_STUS_CD, entitlementStatusCode, null)));
  }

  /**
   * create entitlement reason Extension.
   *
   * @param currentEntitlementReasonCode entitlement reason Code.
   * @return optional extension
   */
  public static Optional<Extension> createCurrentEntitlementReasonCodeExtension(
      String currentEntitlementReasonCode) {
    if (currentEntitlementReasonCode == null || currentEntitlementReasonCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL)
            .setValue(
                new Coding(
                    SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD, currentEntitlementReasonCode, null)));
  }

  /**
   * create esrd Indicator Extension.
   *
   * @param esrdIndicator entitlement reason Code.
   * @return optional extension
   */
  public static Optional<Extension> createEsrdIndicatorExtension(String esrdIndicator) {
    if (esrdIndicator == null || esrdIndicator.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_ESRD_STUS_ID_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_ESRD_STUS_ID, esrdIndicator, null)));
  }

  /**
   * create disability Indicator Extension.
   *
   * @param disabilityIndicator disability Indicator.
   * @return optional extension
   */
  public static Optional<Extension> createDisabilityIndicatorExtension(String disabilityIndicator) {
    if (disabilityIndicator == null || disabilityIndicator.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new Extension(SystemUrls.EXT_BENE_DSBLD_STUS_ID_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_DSBLD_STUS_ID, disabilityIndicator, null)));
  }

  private static void addExtensionToListIfPresent(
      List<Extension> extensionsList, String url, String system, String code) {
    if (code != null && !code.isBlank()) {
      extensionsList.add(new Extension(url).setValue(new Coding(system, code, null)));
    }
  }
}
