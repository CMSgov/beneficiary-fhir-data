package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim line carrier health professional shortage area scarcity codes.
 */
public sealed interface HealthProfessionalShortageAreaScarcityCode
    permits HealthProfessionalShortageAreaScarcityCode.Valid,
        HealthProfessionalShortageAreaScarcityCode.Invalid {

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
   * @return claim line carrier health professional shortage area scarcity code or empty Optional if
   *     code is null or blank
   */
  static Optional<HealthProfessionalShortageAreaScarcityCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (HealthProfessionalShortageAreaScarcityCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_LINE_CARR_HPSA_SCRCTY_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_LINE_CARR_HPSA_SCRCTY_CD, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements HealthProfessionalShortageAreaScarcityCode {
    /** 1 - HPSA. */
    _1("1", "HPSA"),
    /** 2 - Scarcity. */
    _2("2", "Scarcity"),
    /** 3 - Both. */
    _3("3", "Both");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements HealthProfessionalShortageAreaScarcityCode {
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
