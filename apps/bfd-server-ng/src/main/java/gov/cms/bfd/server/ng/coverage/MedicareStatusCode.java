package gov.cms.bfd.server.ng.coverage;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

/** Medicare Status Code. */
@Getter
public enum MedicareStatusCode {
  /** Represents Medicare Status CODE 0. */
  CODE_0("0"),
  /** Represents Medicare Status CODE 00. */
  CODE_00("00"),
  /** Represents Medicare Status CODE 10. */
  CODE_10("10"),
  /** Represents Medicare Status CODE 11. */
  CODE_11("11"),
  /** Represents Medicare Status CODE 20. */
  CODE_20("20"),
  /** Represents Medicare Status CODE 21. */
  CODE_21("21"),
  /** Represents Medicare Status CODE 31. */
  CODE_31("31"),
  /** Represents Medicare Status CODE 40. */
  CODE_40("40"),
  /** Represents Medicare Status CODE null. */
  CODE_TILDE("~"); // Represents the "missing" or "null" mapping source

  private final String code;

  MedicareStatusCode(String code) {
    this.code = code;
  }

  /**
   * from Code.
   *
   * @param code code
   * @return Medicare Status Code
   */
  public static Optional<MedicareStatusCode> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(enumVal -> enumVal.getCode().equals(code)).findFirst();
  }
}
