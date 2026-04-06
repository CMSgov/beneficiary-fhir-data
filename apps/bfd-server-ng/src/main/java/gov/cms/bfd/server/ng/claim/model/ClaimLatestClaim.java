package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Claim Latest Claim Indicator. */
@Getter
@AllArgsConstructor
public enum ClaimLatestClaim {
  /** Latest Claim = Yes. */
  YES('Y', "active"),
  /** Latest Claim = No. */
  NO('N', "cancelled");

  private final Character code;
  private final String latestClaim;

  /**
   * Converts from a database code.
   *
   * @param code code
   * @return Claim latest claim
   */
  public static ClaimLatestClaim fromCode(Character code) {
    return Arrays.stream(values())
        .filter(c -> c.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown ClaimLatestClaim code: " + code));
  }

  /**
   * Converts to Latest Claim Coding.
   *
   * @return FHIR coding
   */
  public Optional<Coding> toFhirFinalAction() {
    return Optional.of(new Coding(SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS, latestClaim, null));
  }
}
