package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Carrier line miles/time/units/services (MTUS) indicator codes. */
public sealed interface CarrierLineMTUSIndicatorCode
    permits CarrierLineMTUSIndicatorCode.Valid, CarrierLineMTUSIndicatorCode.Invalid {

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
   * @return carrier line miles/time/units/services (MTUS) indicator code
   */
  static Optional<CarrierLineMTUSIndicatorCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (CarrierLineMTUSIndicatorCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return Extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_MTUS_IND_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_MTUS_IND_CD, getCode(), getDisplay()));
  }

  /**
   * Carrier line miles/time/units/services (MTUS) indicator codes. Suppress SonarQube warning that
   * constant names should comply with naming conventions.
   */
  @AllArgsConstructor
  @Getter
  enum Valid implements CarrierLineMTUSIndicatorCode {
    /** 0 - VALUES REPORTED AS ZERO. */
    _0("0", "VALUES REPORTED AS ZERO"),
    /** 1 - TRANSPORTATION (AMBULANCE) MILES. */
    _1("1", "TRANSPORTATION (AMBULANCE) MILES"),
    /** 2 - ANESTHESIA TIME UNITS. */
    _2("2", "ANESTHESIA TIME UNITS"),
    /** 3 - NUMBER OF SERVICES. */
    _3("3", "NUMBER OF SERVICES"),
    /** 4 - OXYGEN VOLUME UNITS. */
    _4("4", "OXYGEN VOLUME UNITS"),
    /** 5 - UNITS OF BLOOD. */
    _5("5", "UNITS OF BLOOD"),
    /** 6 - ANESTHESIA BASE AND TIME UNITS (PRIOR TO 1991; FROM BMAD). */
    _6("6", "ANESTHESIA BASE AND TIME UNITS (PRIOR TO 1991; FROM BMAD");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements CarrierLineMTUSIndicatorCode {
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
