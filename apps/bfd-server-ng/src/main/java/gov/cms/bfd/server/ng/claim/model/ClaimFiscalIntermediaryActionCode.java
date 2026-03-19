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
public sealed interface ClaimFiscalIntermediaryActionCode
    permits ClaimFiscalIntermediaryActionCode.Valid, ClaimFiscalIntermediaryActionCode.Invalid {

  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Converts from a database code.
   *
   * @param code database code
   * @return claim fiscal intermediary action code
   */
  static Optional<ClaimFiscalIntermediaryActionCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(c -> c.code.equals(code))
            .map(c -> (ClaimFiscalIntermediaryActionCode) c)
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
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_FI_ACTN_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_FISCAL_INTERMEDIARY_ACTION_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /**
   * Enum for all known, valid fiscal intermediary action codes.
   */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimFiscalIntermediaryActionCode {
    /** 1 - Original debit action (always a 1 for all regular bills). */
    _1("1", "Original debit action (always a 1 for all regular bills)"),
    /** 5 - Force action code 3 (secondary debit adjustment). */
    _5("5", "Force action code 3 (secondary debit adjustment)"),
    /** 8 - Benefits refused. */
    _8("8", "Benefits refused");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimFiscalIntermediaryActionCode {
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
