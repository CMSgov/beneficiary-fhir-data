package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Payment indicator. */
@Getter
@AllArgsConstructor
public enum McoPaidSwitch {
  /** Unpaid. */
  UNPAID("0", "No managed care organization (MCO) payment"),
  /** Paid. */
  PAID("1", "MCO paid provider for the claim"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Converts from a database code.
   *
   * @param code database code.
   * @return paid switch
   */
  public static Optional<McoPaidSwitch> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(values())
            .filter(c -> c.code.equals(code))
            .findFirst()
            .orElse(handleInvalidValue(code)));
  }

  /**
   * Handles scenarios where code could not be mapped to a valid value.
   *
   * @param invalidValue the invalid value to capture
   * @return paid switch
   */
  public static McoPaidSwitch handleInvalidValue(String invalidValue) {
    var invalidMcoPaidSwitch = McoPaidSwitch.INVALID;
    invalidMcoPaidSwitch.code = invalidValue;
    return invalidMcoPaidSwitch;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_MDCR_INSTNL_MCO_PD_SW.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_MCO_PAID_SWITCH)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
