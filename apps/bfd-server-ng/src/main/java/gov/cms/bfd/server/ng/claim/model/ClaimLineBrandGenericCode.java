package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Brand Generic Codes. */
@AllArgsConstructor
@Getter
public enum ClaimLineBrandGenericCode {

  /** B - Brand. */
  B("B", "Brand"),
  /** G - Generic Null/Missing. */
  G("G", "Generic Null/Missing"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return genric brand indicator code
   */
  public static Optional<ClaimLineBrandGenericCode> tryFromCode(String code) {
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
   * @return genric brand indicator code
   */
  public static ClaimLineBrandGenericCode handleInvalidValue(String invalidValue) {
    var invalidClaimLineBrandGenericCode = ClaimLineBrandGenericCode.INVALID;
    invalidClaimLineBrandGenericCode.code = invalidValue;
    return invalidClaimLineBrandGenericCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.BRAND_GENERIC_IND_CODE.toFhir());

    var codeableConcept =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.HL7_GENERIC_BRAND_IND)
                    .setCode(code)
                    .setDisplay(display))
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_GENERIC_BRAND_IND)
                    .setCode(code)
                    .setDisplay(display));
    supportingInfo.setCode(codeableConcept);
    return supportingInfo;
  }
}
