package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Compound codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimLineCompoundCode {

  /** 0 - Not specified (missing values are also possible). */
  _0("0", "Not specified (missing values are also possible)"),
  /** 1 - Not a compound. */
  _1("1", "Not a compound"),
  /** 2 - Compound. */
  _2("2", "Compound"),
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
  public static Optional<ClaimLineCompoundCode> tryFromCode(String code) {
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
  public static ClaimLineCompoundCode handleInvalidValue(String invalidValue) {
    var invalidClaimLineCompoundCode = ClaimLineCompoundCode.INVALID;
    invalidClaimLineCompoundCode.code = invalidValue;
    return invalidClaimLineCompoundCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.COMPOUND_CODE.toFhir());

    var codeableConcept =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.HL7_CLAIM_COMPOUND_CODE)
                    .setCode(code)
                    .setDisplay(display))
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_COMPOUND_CODE)
                    .setCode(code)
                    .setDisplay(display));
    supportingInfo.setCode(codeableConcept);
    return supportingInfo;
  }
}
