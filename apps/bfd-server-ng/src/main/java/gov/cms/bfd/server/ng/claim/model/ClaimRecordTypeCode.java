package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim Record Type Codes. */
public sealed interface ClaimRecordTypeCode
    permits ClaimRecordTypeCode.Valid, ClaimRecordTypeCode.Invalid {

  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim record type code
   */
  static Optional<ClaimRecordTypeCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimRecordTypeCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_NRLN_RIC_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_RECORD_TYPE)
                    .setDisplay(getDisplay())
                    .setCode(getCode())));
  }

  /**
   * Enum for all known, valid claim record type codes. Suppress SonarQube warning that constant
   * names should comply with naming conventions.
   */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S115")
  enum Valid implements ClaimRecordTypeCode {
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
     * V - Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF],
     * hospice [HOS], or home health agency [HHA]).
     */
    V(
        "V",
        "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])",
        "Part A"),
    /** W - Part B institutional claim record (outpatient [HOP], HHA). */
    W("W", "Part B institutional claim record (outpatient [HOP], HHA)", "Part B");

    private final String code;
    private final String display;
    private final String partDisplay;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimRecordTypeCode {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
