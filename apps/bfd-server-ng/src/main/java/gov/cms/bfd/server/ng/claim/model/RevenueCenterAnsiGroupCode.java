package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Revenue center ANSI adjustment group codes for claims. */
@Getter
@AllArgsConstructor
public enum RevenueCenterAnsiGroupCode {
  /**
   * CO - Contractual Obligations -- this group code should be used when a contractual agreement
   * between the payer and payee, or a regulatory requirement, resulted in an adjustment. Generally,
   * these adjustments are considered a write-off for the provider and are not billed to the
   * patient.
   */
  CO(
      "CO",
      "Contractual Obligations -- this group code should be used when a contractual agreement between the payer and payee, or a regulatory requirement, resulted in an adjustment. Generally, these adjustments are considered a write-off for the provider and are not billed to the patient."),
  /**
   * CR - Corrections and Reversals -- this group code should be used for correcting a prior claim.
   * It applies when there is a change to a previously adjudicated claim.
   */
  CR(
      "CR",
      "Corrections and Reversals -- this group code should be used for correcting a prior claim. It applies when there is a change to a previously adjudicated claim."),
  /**
   * OA - Other Adjustments -- this group code should be used when no other group code applies to
   * the adjustment.
   */
  OA(
      "OA",
      "Other Adjustments -- this group code should be used when no other group code applies to the adjustment."),
  /**
   * PI - Payer Initiated Reductions -- this group code should be used when, in the opinion of the
   * payer, the adjustment is not the responsibility of the patient, but there is no supporting
   * contract between the provider and the payer (i.e., medical review or professional review
   * organization adjustments).
   */
  PI(
      "PI",
      "Payer Initiated Reductions -- this group code should be used when, in the opinion of the payer, the adjustment is not the responsibility of the patient, but there is no supporting contract between the provider and the payer (i.e., medical review or professional review organization adjustments)."),
  /**
   * PR - Patient Responsibility -- this group should be used when the adjustment represents an
   * amount that should be billed to the patient or insured. This group would typically be used for
   * deductible and copay adjustments.
   */
  PR(
      "PR",
      "Patient Responsibility -- this group should be used when the adjustment represents an amount that should be billed to the patient or insured. This group would typically be used for deductible and copay adjustments."),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return revenue center ANSI group code
   */
  public static Optional<RevenueCenterAnsiGroupCode> tryFromCode(String code) {
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
   * @return revenue center ANSI group code
   */
  public static RevenueCenterAnsiGroupCode handleInvalidValue(String invalidValue) {
    var invalidRevenueCenterAnsiGroupCode = RevenueCenterAnsiGroupCode.INVALID;
    invalidRevenueCenterAnsiGroupCode.code = invalidValue;
    return invalidRevenueCenterAnsiGroupCode;
  }

  /**
   * Converts this ANSI group code into a list of FHIR {@link Coding} objects for multiple code
   * systems.
   *
   * @return list of FHIR codings
   */
  public Optional<List<Coding>> toFhirCodings() {
    return Optional.of(
        List.of(
            new Coding().setSystem(SystemUrls.X12_CLAIM_ADJUSTMENT_GROUP_CODES).setCode(code),
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ANSI_GRP_CODE)
                .setCode(code)));
  }
}
