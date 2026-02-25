package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim Record Type Codes. Suppress SonarQube warning that constant names should comply with naming
 * conventions
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimRecordTypeCode {
  /** M - Part B DMEPOS claim record (processed by DME Regional Carrier). */
  M("M", "Part B DMEPOS claim record (processed by DME Regional Carrier)", "Part B"),
  /**
   * O - Part B physician/supplier claim record (processed by local carriers; can include DMEPOS
   * services).
   */
  O(
      "O",
      "Part B physician/supplier claim record (processed by local carriers; can include DMEPOS services)",
      "Part B"),
  /** U - Both Part A and B institutional home health agency (HHA) claim records. */
  U("U", "Both Part A and B institutional home health agency (HHA) claim records", "Part A"),
  /**
   * V - Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice
   * [HOS], or home health agency [HHA]).
   */
  V(
      "V",
      "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])",
      "Part A"),
  /** W - Part B institutional claim record (outpatient [HOP], HHA). */
  W("W", "Part B institutional claim record (outpatient [HOP], HHA)", "Part B"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "", "");

  private String code;
  private final String display;
  private final String partDisplay;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim record type code
   */
  public static Optional<ClaimRecordTypeCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(values())
            .filter(v -> v.code.equals(code))
            .findFirst()
            .orElse(handleInvalidValue(code)));
  }

  /**
   * Handles scenarios where code could not be mapped to a valid value.
   *
   * @param invalidValue the invalid value to capture
   * @return INVALID claim record type code
   */
  public static ClaimRecordTypeCode handleInvalidValue(String invalidValue) {
    var invalidClaimRecordTypeCode = ClaimRecordTypeCode.INVALID;
    invalidClaimRecordTypeCode.code = invalidValue;
    return invalidClaimRecordTypeCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_NRLN_RIC_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_RECORD_TYPE)
                    .setDisplay(display)
                    .setCode(code)));
  }
}
