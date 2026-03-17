package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Claim service deductible codes. Suppress SonarQube warning that constant names should comply with
 * naming conventions.
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimServiceDeductibleCode {
  /** 0 - Service Subject to Deductible. */
  _0("0", "Service Subject to Deductible"),
  /** 1 - Service Not Subject to Deductible. */
  _1("1", "Service Not Subject to Deductible");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim service deductible code
   */
  public static Optional<ClaimServiceDeductibleCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }
}
