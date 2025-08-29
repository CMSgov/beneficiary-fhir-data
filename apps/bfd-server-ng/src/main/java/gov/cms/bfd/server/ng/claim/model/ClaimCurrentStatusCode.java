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
  SUSPENDED("S", ExplanationOfBenefit.RemittanceOutcome.QUEUED),
  DENIED("D", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  PAID("P", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  REJECTED("R", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
  RETURN("T", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),
  INACTIVE("I", ExplanationOfBenefit.RemittanceOutcome.PARTIAL);

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
