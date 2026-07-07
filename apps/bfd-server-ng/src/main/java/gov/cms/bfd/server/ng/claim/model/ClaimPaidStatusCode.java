package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The paid status code of the claim. */
public sealed interface ClaimPaidStatusCode
    permits ClaimPaidStatusCode.Valid, ClaimPaidStatusCode.Invalid {
  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the outcome value.
   *
   * @return the outcome
   */
  ExplanationOfBenefit.RemittanceOutcome getOutcome();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim paid status code
   */
  static Optional<ClaimPaidStatusCode> tryFromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }

    return Optional.of(
        Arrays.stream(ClaimPaidStatusCode.Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPaidStatusCode) v)
            .findFirst()
            .orElseGet(() -> new ClaimPaidStatusCode.Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_PD_STUS_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_PAID_STATUS_CODE)
                    .setCode(getCode())));
  }

  /**
   * Gets all claim paid status codes that map to the provided remittance outcome.
   *
   * @param outcome the remittance outcome
   * @return claim paid status codes for the outcome
   */
  static List<ClaimPaidStatusCode> findByOutcome(ExplanationOfBenefit.RemittanceOutcome outcome) {
    return Arrays.stream(ClaimPaidStatusCode.Valid.values())
        .filter(v -> v.outcome.equals(outcome))
        .map(v -> (ClaimPaidStatusCode) v)
        .toList();
  }

  /**
   * Safely resolves the remittance outcome from a status code, defaulting to PARTIAL if the status
   * code or its mapped outcome is empty.
   *
   * @param statusCode the status code to evaluate
   * @return the resolved FHIR RemittanceOutcome
   */
  static Optional<ExplanationOfBenefit.RemittanceOutcome> resolveOutcome(
      Optional<ClaimPaidStatusCode> statusCode) {
    return statusCode
        .map(ClaimPaidStatusCode::getOutcome)
        .or(() -> Optional.of(ExplanationOfBenefit.RemittanceOutcome.PARTIAL));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimPaidStatusCode {
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
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPaidStatusCode {
    @Override
    public ExplanationOfBenefit.RemittanceOutcome getOutcome() {
      return ExplanationOfBenefit.RemittanceOutcome.NULL;
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
