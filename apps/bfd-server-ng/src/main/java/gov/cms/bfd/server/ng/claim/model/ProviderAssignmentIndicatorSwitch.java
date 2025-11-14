package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Provider Assignment Indicator Switch info. * */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ProviderAssignmentIndicatorSwitch {
  /** L - Assigned Claim. */
  L("L", "Assigned Claim"),
  /** N - Non-assigned claim. */
  N("N", "Non-assigned claim");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim payment denial code
   */
  public static Optional<ProviderAssignmentIndicatorSwitch> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension()
        .setUrl(
            SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROVIDER_ASSIGNMENT_INDICATOR_SWITCH)
        .setValue(
            new Coding(
                SystemUrls
                    .BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROVIDER_ASSIGNMENT_INDICATOR_SWITCH,
                code,
                display));
  }
}
