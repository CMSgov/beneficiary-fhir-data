package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** PPS Indicator codes. */
@AllArgsConstructor
@Getter
public enum PpsIndicatorCode {
  /** 2 - PPS bill; claim contains PPS indicator. */
  PPS("2", "PPS bill; claim contains PPS indicator"),
  /** unknown - Not a PPS bill. */
  NOT_PPS("unknown", "Not a PPS bill"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim PPS indicator code
   */
  public static Optional<PpsIndicatorCode> fromCode(String code) {
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
   * @return claim PPS indicator code
   */
  public static PpsIndicatorCode handleInvalidValue(String invalidValue) {
    var invalidPpsIndicatorCode = PpsIndicatorCode.INVALID;
    invalidPpsIndicatorCode.code = invalidValue;
    return invalidPpsIndicatorCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_PPS_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PPS_INDICATOR_CODE)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
