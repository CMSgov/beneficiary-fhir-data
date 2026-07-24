package gov.cms.bfd.server.ng.claim.model.common;

import java.util.Arrays;
import java.util.List;
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

  /**
   * Gets all claim paid status codes that map to the provided remittance outcome.
   *
   * @param outcome the remittance outcome
   * @return claim paid status codes for the outcome
   */
  public static List<ClaimPaidStatusCode> findByOutcome(
      ExplanationOfBenefit.RemittanceOutcome outcome) {
    return Arrays.stream(values())
        .filter(statusCode -> outcome.equals(statusCode.getOutcome()))
        .toList();
  }

  /**
   * Safely resolves the remittance outcome from a status code, defaulting to PARTIAL if the status
   * code or its mapped outcome is empty.
   *
   * @param statusCode the status code to evaluate
   * @return the resolved FHIR RemittanceOutcome
   */
  public static Optional<ExplanationOfBenefit.RemittanceOutcome> resolveOutcome(
      Optional<ClaimPaidStatusCode> statusCode) {
    return statusCode
        .map(ClaimPaidStatusCode::getOutcome)
        .or(() -> Optional.of(ExplanationOfBenefit.RemittanceOutcome.PARTIAL));
  }
}
