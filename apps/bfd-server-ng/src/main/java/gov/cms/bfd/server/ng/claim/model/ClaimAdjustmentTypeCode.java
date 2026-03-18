package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim adjustment type codes. */
public sealed interface ClaimAdjustmentTypeCode
    permits ClaimAdjustmentTypeCode.Valid, ClaimAdjustmentTypeCode.Invalid {

  /** Returns the code. */
  @SuppressWarnings("checkstyle:JavadocMethod")
  String getCode();

  /** Returns the display or returns an empty string if invalid. */
  @SuppressWarnings("checkstyle:JavadocMethod")
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim adjustment type code
   */
  static Optional<ClaimAdjustmentTypeCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimAdjustmentTypeCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
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
        .setCategory(BlueButtonSupportingInfoCategory.CLM_ADJSTMT_TYPE_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADJUSTMENT_TYPE_CODE)
                    .setCode(getCode())
                    .setDisplay(getDisplay())));
  }

  /**
   * Enum for all known, valid claim adjustment type codes. Suppress SonarQube warning that constant
   * names should comply with naming conventions.
   */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S115")
  enum Valid implements ClaimAdjustmentTypeCode {
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
    _9("9", "UNKNOWN");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimAdjustmentTypeCode {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
