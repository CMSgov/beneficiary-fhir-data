package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim line carrier health professional shortage area scarcity codes. Suppress SonarQube warning
 * that constant names should comply with naming conventions.
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum HealthProfessionalShortageAreaScarcityCode {
  /** 1 - HPSA. */
  _1("1", "HPSA"),
  /** 2 - Scarcity. */
  _2("2", "Scarcity"),
  /** 3 - Both. */
  _3("3", "Both"),
  /** 4 - N/A. */
  _4("4", "N/A"),
  /** 5 - HPSA and HSIP. */
  _5("5", "HPSA and HSIP"),
  /** 6 - PCIP. */
  _6("6", "PCIP"),
  /** 7 - HPSA and PCIP. */
  _7("7", "HPSA and PCIP");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim line carrier health professional shortage area scarcity code
   */
  public static Optional<HealthProfessionalShortageAreaScarcityCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_LINE_CARR_HPSA_SCRCTY_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_LINE_CARR_HPSA_SCRCTY_CD, code, display));
  }
}
