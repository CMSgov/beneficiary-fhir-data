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
public sealed interface ClaimOutpatientServiceTypeCode
    permits ClaimOutpatientServiceTypeCode.Valid, ClaimOutpatientServiceTypeCode.Invalid {

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
   * Convert from a database code.
   *
   * @param code database code
   * @return claim outpatient service type code or empty Optional if code is null or blank
   */
  static Optional<ClaimOutpatientServiceTypeCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimOutpatientServiceTypeCode) v)
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
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_OP_SRVC_TYPE_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_OUTPATIENT_SERVICE_TYPE_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S1192")
  enum Valid implements ClaimOutpatientServiceTypeCode {
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
    _9("9", "Unknown (Information not available)");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimOutpatientServiceTypeCode {
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
