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
public sealed interface ClaimLineBrandGenericCode
    permits ClaimLineBrandGenericCode.Valid, ClaimLineBrandGenericCode.Invalid {

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
   * @return generic brand indicator code or empty Optional if code is null or blank
   */
  static Optional<ClaimLineBrandGenericCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimLineBrandGenericCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps interface to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.BRAND_GENERIC_IND_CODE.toFhir());

    var codeableConcept =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.HL7_GENERIC_BRAND_IND)
                    .setCode(getCode())
                    .setDisplay(getDisplay()))
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_GENERIC_BRAND_IND)
                    .setCode(getCode())
                    .setDisplay(getDisplay()));
    supportingInfo.setCode(codeableConcept);
    return supportingInfo;
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimLineBrandGenericCode {
    /** B - Brand. */
    B("B", "Brand"),
    /** G - Generic Null/Missing. */
    G("G", "Generic Null/Missing");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimLineBrandGenericCode {
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
