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
public sealed interface HhaLupaIndicatorCode
    permits HhaLupaIndicatorCode.Valid, HhaLupaIndicatorCode.Invalid {

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
   * @return HHA LUPA indicator code
   */
  static Optional<HhaLupaIndicatorCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(c -> c.code.equals(code))
            .map(c -> (HhaLupaIndicatorCode) c)
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
        .setCategory(BlueButtonSupportingInfoCategory.CLM_HHA_LUP_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_HHA_LUPA_INDICATOR_CODE)
                    .setCode(getCode())
                    .setDisplay(getDisplay())));
  }

  /** Enum for all known, valid HHA LUPA codes. */
  @Getter
  @AllArgsConstructor
  enum Valid implements HhaLupaIndicatorCode {
    /** L. */
    L("L", "Low utilization payment adjustment (LUPA) claim");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements HhaLupaIndicatorCode {
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
