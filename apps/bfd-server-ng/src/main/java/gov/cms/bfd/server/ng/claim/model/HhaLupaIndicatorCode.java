package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Home Health Agency (HHA) Low Utilization Payment Adjustment (LUPA) indicator codes for claims.
 */
@Getter
@AllArgsConstructor
public enum HhaLupaIndicatorCode {
  /** L. */
  L("L", "Low utilization payment adjustment (LUPA) claim");

  private final String code;
  private final String display;

  /**
   * Converts from a database code.
   *
   * @param code database code.
   * @return paid switch
   */
  public static Optional<HhaLupaIndicatorCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(c -> c.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_HHA_LUP_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_HHA_LUPA_INDICATOR_CODE)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
