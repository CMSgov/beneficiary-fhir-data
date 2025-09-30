package gov.cms.bfd.server.ng.claim.model;

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
  M("M", "Part B DMEPOS claim record (processed by DME Regional Carrier)"),
  /**
   * O - Part B physician/supplier claim record (processed by local carriers; can include DMEPOS
   * services).
   */
  O(
      "O",
      "Part B physician/supplier claim record (processed by local carriers; can include DMEPOS services)"),
  /** U - Both Part A and B institutional home health agency (HHA) claim records. */
  U("U", "Both Part A and B institutional home health agency (HHA) claim records"),
  /**
   * V - Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice
   * [HOS], or home health agency [HHA]).
   */
  V(
      "V",
      "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])."),
  /** W - Part B institutional claim record (outpatient [HOP], HHA). */
  W("W", "Part B institutional claim record (outpatient [HOP], HHA)");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim record type code
   */
  public static Optional<ClaimRecordTypeCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_NRLN_RIC_CD.toFhir())
        .setCode(new CodeableConcept(new Coding().setDisplay(display).setCode(code)));
  }
}
