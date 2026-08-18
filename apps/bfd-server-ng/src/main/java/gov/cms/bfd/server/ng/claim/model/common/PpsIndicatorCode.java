package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** PPS Indicator codes. */
public sealed interface PpsIndicatorCode permits PpsIndicatorCode.Valid, PpsIndicatorCode.Invalid {

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
   * @return claim PPS indicator code or empty Optional if code is null or blank
   */
  static Optional<PpsIndicatorCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (PpsIndicatorCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Convert using clm_fiss.clm_pps_ind to PpsIndicatorCode.
   *
   * @param code clm_pps_ind to be converted
   * @return matching enum constant
   */
  static Optional<PpsIndicatorCode> fromFISSCode(String code) {
    return Optional.of(
        Stream.of(Valid.fromFISSIndicator(code != null && !code.isBlank() ? code.charAt(0) : 'N'))
            .map(v -> (PpsIndicatorCode) v)
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
        .setCategory(BlueButtonSupportingInfoCategory.CLM_PPS_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PPS_INDICATOR_CODE)
                    .setCode(getCode())
                    .setDisplay(getDisplay())));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements PpsIndicatorCode {
    /** 2 - PPS bill; claim contains PPS indicator. */
    PPS("2", "PPS bill; claim contains PPS indicator"),
    /** unknown - Not a PPS bill. */
    NOT_PPS("unknown", "Not a PPS bill");

    private final String code;
    private final String display;

    /**
     * Character level conversion mapping clm_fiss.clm_pps_ind to PpsIndicatorCode.
     *
     * @param indicator clm_pps_ind to be converted
     * @return matching enum constant
     */
    public static Valid fromFISSIndicator(char indicator) {
      return switch (indicator) {
        case 'Y' -> PPS;
        case 'N' -> NOT_PPS;
        default -> NOT_PPS;
      };
    }
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements PpsIndicatorCode {
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
