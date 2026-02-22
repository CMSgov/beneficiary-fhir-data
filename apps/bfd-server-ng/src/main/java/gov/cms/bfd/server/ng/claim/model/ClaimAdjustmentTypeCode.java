package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim adjustment type codes. */
public record ClaimAdjustmentTypeCode(String code, String display) {
  /** 0 - ORIGINAL. */
  public static final ClaimAdjustmentTypeCode _0 = new ClaimAdjustmentTypeCode("0", "ORIGINAL");

  /** 1 - VOID. */
  public static final ClaimAdjustmentTypeCode _1 = new ClaimAdjustmentTypeCode("1", "VOID");

  /** 2 - REPLACEMENT. */
  public static final ClaimAdjustmentTypeCode _2 = new ClaimAdjustmentTypeCode("2", "REPLACEMENT");

  /** 3 - CREDIT ADJUSTMENT (NEGATIVE SUPPLEMENTAL). */
  public static final ClaimAdjustmentTypeCode _3 =
      new ClaimAdjustmentTypeCode("3", "CREDIT ADJUSTMENT (NEGATIVE SUPPLEMENTAL)");

  /** 4 - DEBIT ADJUSTMENT (POSITIVE SUPPLEMENTAL). */
  public static final ClaimAdjustmentTypeCode _4 =
      new ClaimAdjustmentTypeCode("4", "DEBIT ADJUSTMENT (POSITIVE SUPPLEMENTAL)");

  /** 5 - GROSS ADJUSTMENT. */
  public static final ClaimAdjustmentTypeCode _5 =
      new ClaimAdjustmentTypeCode("5", "GROSS ADJUSTMENT");

  /** 8 - BALANCE RECORD. */
  public static final ClaimAdjustmentTypeCode _8 =
      new ClaimAdjustmentTypeCode("8", "BALANCE RECORD");

  /** 9 - UNKNOWN. */
  public static final ClaimAdjustmentTypeCode _9 = new ClaimAdjustmentTypeCode("9", "UNKNOWN");

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim adjustment type code and its matching display, or an empty string for the display
   *     if the code is unrecognized.
   */
  public static Optional<ClaimAdjustmentTypeCode> fromCode(String code) {
    return switch (code) {
      case "0" -> Optional.of(_0);
      case "1" -> Optional.of(_1);
      case "2" -> Optional.of(_2);
      case "3" -> Optional.of(_3);
      case "4" -> Optional.of(_4);
      case "5" -> Optional.of(_5);
      case "8" -> Optional.of(_8);
      case "9" -> Optional.of(_9);
      default -> Optional.of(new ClaimAdjustmentTypeCode(code, ""));
    };
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
