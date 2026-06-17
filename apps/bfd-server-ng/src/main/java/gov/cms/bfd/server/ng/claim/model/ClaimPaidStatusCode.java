package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The paid status code of the claim. */
@Getter
@AllArgsConstructor
public enum ClaimPaidStatusCode {

  /** P. */
  P("P", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** 1. */
  NUM_1("1", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** R. */
  R("R", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** 2. */
  NUM_2("2", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** D. */
  D("D", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Y. */
  Y("Y", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Empty value (normalized from "~"). */
  EMPTY("", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** I. */
  I("I", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** S. */
  S("S", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** T. */
  T("T", ExplanationOfBenefit.RemittanceOutcome.PARTIAL);

  private final String code;
  private final ExplanationOfBenefit.RemittanceOutcome outcome;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim paid status code
   */
  public static Optional<ClaimPaidStatusCode> tryFromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }

    return Arrays.stream(values()).filter(v -> v.code.equals(code.trim())).findFirst();
  }
}
