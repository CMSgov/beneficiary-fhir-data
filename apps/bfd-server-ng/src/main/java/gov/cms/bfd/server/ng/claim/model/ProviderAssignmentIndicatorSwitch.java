package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Provider Assignment Indicator Switch info. */
public sealed interface ProviderAssignmentIndicatorSwitch
    permits ProviderAssignmentIndicatorSwitch.Valid, ProviderAssignmentIndicatorSwitch.Invalid {

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
   * @return provider assignment indicator switch or empty Optional if code is null or blank
   */
  static Optional<ProviderAssignmentIndicatorSwitch> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ProviderAssignmentIndicatorSwitch) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps interface to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
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
                    .setDisplay(getDisplay())
                    .setCode(getCode())));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ProviderAssignmentIndicatorSwitch {
    /** L - Assigned Claim. */
    L("L", "Assigned Claim"),
    /** N - Non-assigned claim. */
    N("N", "Non-assigned claim");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ProviderAssignmentIndicatorSwitch {
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
