package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Carrier line miles/time/units/services (MTUS) indicator codes. Suppress SonarQube warning that
 * constant names should comply with naming conventions.
 */
@AllArgsConstructor
@Getter
public enum CarrierLineMTUSIndicatorCode {
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
  /** 6 - ANESTHESIA BASE AND TIME UNITS (PRIOR TO 1991; FROM BMAD. */
  _6("6", "ANESTHESIA BASE AND TIME UNITS (PRIOR TO 1991; FROM BMAD");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return carrier line miles/time/units/services (MTUS) indicator code
   */
  public static Optional<CarrierLineMTUSIndicatorCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_MTUS_IND_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_MTUS_IND_CD, code, display));
  }
}
