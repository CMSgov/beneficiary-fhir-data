package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim fiscal intermediary action codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimFiscalIntermediaryActionCode {
  /** 1 - Original debit action (always a 1 for all regular bills). */
  _1("1", "Original debit action (always a 1 for all regular bills)"),
  /** 5 - Force action code 3 (secondary debit adjustment). */
  _5("5", "Force action code 3 (secondary debit adjustment)"),
  /** 8 - Benefits refused. */
  _8("8", "Benefits refused");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim fiscal intermediary action code
   */
  public static Optional<ClaimFiscalIntermediaryActionCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_FI_ACTN_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_FISCAL_INTERMEDIARY_ACTION_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
