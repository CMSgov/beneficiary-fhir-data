package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim Dispensing Status Codes. */
@AllArgsConstructor
@Getter
public enum ClaimDispenseStatusCode {
  /** P - Partially filled. */
  P("P", "Partially filled"),
  /** C - Completely filled. */
  C("C", "Completely filled"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim dispense status code
   */
  public static Optional<ClaimDispenseStatusCode> tryFromCode(String code) {
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
   * @return claim dispense status code
   */
  public static ClaimDispenseStatusCode handleInvalidValue(String invalidValue) {
    var invalidClaimDispenseStatusCode = ClaimDispenseStatusCode.INVALID;
    invalidClaimDispenseStatusCode.code = invalidValue;
    return invalidClaimDispenseStatusCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_DSPNSNG_STUS_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_DISPENSE_STATUS_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
