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
 * Home Health Agency (HHA) Low Utilization Payment Adjustment (LUPA) indicator codes for claims.
 */
@Getter
@AllArgsConstructor
public enum HhaLupaIndicatorCode {
  /** L. */
  L("L", "Low utilization payment adjustment (LUPA) claim"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Converts from a database code.
   *
   * @param code database code.
   * @return HHA LUPA indicator code
   */
  public static Optional<HhaLupaIndicatorCode> tryFromCode(String code) {
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
   * @return HHA LUPA indicator code
   */
  public static HhaLupaIndicatorCode handleInvalidValue(String invalidValue) {
    var invalidHhaLupaIndicatorCode = HhaLupaIndicatorCode.INVALID;
    invalidHhaLupaIndicatorCode.code = invalidValue;
    return invalidHhaLupaIndicatorCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_HHA_LUP_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_HHA_LUPA_INDICATOR_CODE)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
