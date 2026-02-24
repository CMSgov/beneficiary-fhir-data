package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim adjustment type codes. Suppress SonarQube warning that constant names should comply with
 * naming conventions.
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimAdjustmentTypeCode {
  /** 0 - ORIGINAL. */
  _0("0", "ORIGINAL"),
  /** 1 - VOID. */
  _1("1", "VOID"),
  /** 2 - REPLACEMENT. */
  _2("2", "REPLACEMENT"),
  /** 3 - CREDIT ADJUSTMENT (NEGATIVE SUPPLEMENTAL). */
  _3("3", "CREDIT ADJUSTMENT (NEGATIVE SUPPLEMENTAL)"),
  /** 4 - DEBIT ADJUSTMENT (POSITIVE SUPPLEMENTAL). */
  _4("4", "DEBIT ADJUSTMENT (POSITIVE SUPPLEMENTAL)"),
  /** 5 - GROSS ADJUSTMENT. */
  _5("5", "GROSS ADJUSTMENT"),
  /** 8 - BALANCE RECORD. */
  _8("8", "BALANCE RECORD"),
  /** 9 - UNKNOWN. */
  _9("9", "UNKNOWN"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim adjustment type code
   */
  public static Optional<ClaimAdjustmentTypeCode> fromCode(String code) {
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
   * @return INVALID catastrophic coverage code
   */
  public static ClaimAdjustmentTypeCode handleInvalidValue(String invalidValue) {
    var invalidClaimAdjustmentTypeCode = ClaimAdjustmentTypeCode.INVALID;
    invalidClaimAdjustmentTypeCode.code = invalidValue;
    return invalidClaimAdjustmentTypeCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_ADJSTMT_TYPE_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADJUSTMENT_TYPE_CODE)
                    .setDisplay(display)
                    .setCode(code)));
  }
}
