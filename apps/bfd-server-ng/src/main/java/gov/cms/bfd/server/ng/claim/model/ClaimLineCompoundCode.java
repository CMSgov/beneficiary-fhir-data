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
public sealed interface ClaimLineCompoundCode
    permits ClaimLineCompoundCode.Valid, ClaimLineCompoundCode.Invalid {

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
   * @return genric brand indicator code
   */
  static Optional<ClaimLineCompoundCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimLineCompoundCode) v)
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
    supportingInfo.setCategory(CarinSupportingInfoCategory.COMPOUND_CODE.toFhir());

    var codeableConcept =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.HL7_CLAIM_COMPOUND_CODE)
                    .setCode(getCode())
                    .setDisplay(getDisplay()))
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_COMPOUND_CODE)
                    .setCode(getCode())
                    .setDisplay(getDisplay()));
    supportingInfo.setCode(codeableConcept);
    return supportingInfo;
  }

  /**
   * Enum for all known, valid compound codes. Suppress SonarQube warning that constant names should
   * comply with naming conventions.
   */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S115")
  enum Valid implements ClaimLineCompoundCode {
    /** 0 - Not specified (missing values are also possible). */
    _0("0", "Not specified (missing values are also possible)"),
    /** 1 - Not a compound. */
    _1("1", "Not a compound"),
    /** 2 - Compound. */
    _2("2", "Compound");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimLineCompoundCode {
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
