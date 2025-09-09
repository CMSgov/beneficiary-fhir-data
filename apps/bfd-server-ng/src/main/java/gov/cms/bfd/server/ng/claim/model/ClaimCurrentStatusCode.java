package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The current status code of the claim. */
@Getter
@AllArgsConstructor
public enum ClaimCurrentStatusCode {
  /** S. */
  S("S", ExplanationOfBenefit.RemittanceOutcome.QUEUED),
  /** D. */
  D("D", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  /** P. */
  P("P", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  /** R. */
  R("R", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  /** T. */
  T("T", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),
  /** I. */
  I("I", ExplanationOfBenefit.RemittanceOutcome.PARTIAL);

  private final String code;
  private final ExplanationOfBenefit.RemittanceOutcome outcome;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim admission type code
   */
  public static Optional<ClaimCurrentStatusCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }
}
