package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Claim audit trail location codes mapped from CLM_AUDT_TRL_LCTN_CD. */
@Getter
@AllArgsConstructor
@SuppressWarnings("java:S115")
public enum ClaimAuditTrailLocationCode {
  /** 0 - Completed. */
  COMPLETED("00", "Completed"),
  /** 1 - Pre-Computer. */
  PRE_COMPUTER("01", "Pre-Computer"),
  /** 2 - Development. */
  DEVELOPMENT("02", "Development"),
  /** 3 - DME/OQC. */
  DME_OQC("03", "DME/OQC"),
  /** 4 - System Reject. */
  SYSTEM_REJECT("04", "System Reject"),
  /** 5 - Edit. */
  EDIT("05", "Edit"),
  /** 6 - Reasonable Charge/PSC/ZPIC. */
  REASONABLE_CHARGE("06", "Reasonable Charge/PSC/ZPIC"),
  /** 7 - Utilization Duplicate. */
  UTILIZATION_DUPLICATION("07", "Utilization Duplicate"),
  /** 8 - Query. */
  QUERY("08", "Query"),
  /** 9 - Reply. */
  REPLY("09", "Reply"),
  /** "" - Not Applicable. */
  NA("", "Not Applicable"), // MCS & FISS are not dependent on location code
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim audit trail location code
   */
  public static Optional<ClaimAuditTrailLocationCode> tryFromCode(String code) {
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
   * @return claim audit trail location code
   */
  public static ClaimAuditTrailLocationCode handleInvalidValue(String invalidValue) {
    var invalidClaimAuditTrailLocationCode = ClaimAuditTrailLocationCode.INVALID;
    invalidClaimAuditTrailLocationCode.code = invalidValue;
    return invalidClaimAuditTrailLocationCode;
  }
}
