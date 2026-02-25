package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim outpatient service type codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings({"java:S115", "java:S1192"})
public enum ClaimOutpatientServiceTypeCode {
  /** 0 - Blank. */
  _0("0", "Blank"),
  /** 1 - Emergency. */
  _1("1", "Emergency"),
  /** 2 - Urgent. */
  _2("2", "Urgent"),
  /** 3 - Elective. */
  _3("3", "Elective"),
  /** 5 - Reserved. */
  _5("5", "Reserved"),
  /** 6 - Reserved. */
  _6("6", "Reserved"),
  /** 7 - Reserved. */
  _7("7", "Reserved"),
  /** 8 - Reserved. */
  _8("8", "Reserved"),
  /** 9 - Unknown (Information not available). */
  _9("9", "Unknown (Information not available)"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim outpatient service type code
   */
  public static Optional<ClaimOutpatientServiceTypeCode> tryFromCode(String code) {
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
   * @return claim outpatient service type code
   */
  public static ClaimOutpatientServiceTypeCode handleInvalidValue(String invalidValue) {
    var invalidClaimOutpatientServiceTypeCode = ClaimOutpatientServiceTypeCode.INVALID;
    invalidClaimOutpatientServiceTypeCode.code = invalidValue;
    return invalidClaimOutpatientServiceTypeCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_OP_SRVC_TYPE_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_OUTPATIENT_SERVICE_TYPE_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
