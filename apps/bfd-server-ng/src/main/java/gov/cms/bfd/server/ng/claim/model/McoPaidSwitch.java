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
public sealed interface McoPaidSwitch permits McoPaidSwitch.Valid, McoPaidSwitch.Invalid {

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
   * Converts from a database code.
   *
   * @param code database code.
   * @return paid switch or empty Optional if code is null or blank
   */
  static Optional<McoPaidSwitch> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (McoPaidSwitch) v)
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
        .setCategory(BlueButtonSupportingInfoCategory.CLM_MDCR_INSTNL_MCO_PD_SW.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_MCO_PAID_SWITCH)
                    .setCode(getCode())
                    .setDisplay(getDisplay())));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements McoPaidSwitch {
    /** Unpaid. */
    UNPAID("0", "No managed care organization (MCO) payment"),
    /** Paid. */
    PAID("1", "MCO paid provider for the claim");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements McoPaidSwitch {
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
