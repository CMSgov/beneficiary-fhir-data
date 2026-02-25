package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Provider Assignment Indicator Switch info. * */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ProviderAssignmentIndicatorSwitch {
  /** L - Assigned Claim. */
  L("L", "Assigned Claim"),
  /** N - Non-assigned claim. */
  N("N", "Non-assigned claim"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return provider assignment indicator switch
   */
  public static Optional<ProviderAssignmentIndicatorSwitch> tryFromCode(String code) {
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
   * @return provider assignment indicator switch
   */
  public static ProviderAssignmentIndicatorSwitch handleInvalidValue(String invalidValue) {
    var invalidProviderAssignmentIndicatorSwitch = ProviderAssignmentIndicatorSwitch.INVALID;
    invalidProviderAssignmentIndicatorSwitch.code = invalidValue;
    return invalidProviderAssignmentIndicatorSwitch;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(
                        SystemUrls
                            .BLUE_BUTTON_CODE_SYSTEM_CLAIM_PROVIDER_ASSIGNMENT_INDICATOR_SWITCH)
                    .setDisplay(display)
                    .setCode(code)));
  }
}
