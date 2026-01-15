package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim submission format code. */
@AllArgsConstructor
@Getter
public enum ClaimSubmissionFormatCode {
  /** S - STATE-TO-PLAN PDES. */
  S("S", "S", "STATE-TO-PLAN PDES"),
  /** P - PAPER CLAIM FROM PROVIDER. */
  P("P", "P", "PAPER CLAIM FROM PROVIDER"),
  /** X - X12 837. */
  X("X", "X", "X12 837"),
  /** C - COB CLAIM. */
  C("C", "C", "COB CLAIM"),
  /** B - BENEFICIARY SUBMITTED. */
  B("B", "B", "BENEFICIARY SUBMITTED"),
  /** "" - NCPDP ELECTRONIC SUBMISSION. */
  N("", "N", "NCPDP ELECTRONIC SUBMISSION"),
  /** A - MEDICAID SUBROGATION CLAIM. */
  A("A", "A", "MEDICAID SUBROGATION CLAIM");

  private final String idrCode;
  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param idrCode database code
   * @return claim submission format code
   */
  public static Optional<ClaimSubmissionFormatCode> fromCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_SBMT_FRMT_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_FORMAT_CODE)
                    .setDisplay(display)
                    .setCode(code)));
  }
}
