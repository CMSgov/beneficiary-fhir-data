package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Prescription Origination Codes. */
public sealed interface ClaimPrescriptionOriginCode
    permits ClaimPrescriptionOriginCode.Valid, ClaimPrescriptionOriginCode.Invalid {

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
   * @return Prescription Origination Code or empty Optional if code is null or blank
   */
  static Optional<ClaimPrescriptionOriginCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPrescriptionOriginCode) v)
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
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.RX_ORIGIN_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.HL7_CLAIM_PRESCRIPTION_ORIGIN_CODE)
                        .setCode(getCode()))
                .addCoding(
                    new Coding()
                        .setSystem(
                            SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_PRESCRIPTION_ORIGIN_CODE)
                        .setCode(getCode())));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S115")
  enum Valid implements ClaimPrescriptionOriginCode {
    /** 0 - Not specified. */
    _0("0", "Not specified"),
    /** 1 - Written. */
    _1("1", "Written"),
    /** 2 - Telephone. */
    _2("2", "Telephone"),
    /** 3 - Electronic. */
    _3("3", "Electronic"),
    /** 4 - Facsimile. */
    _4("4", "Facsimile"),
    /** 5 - Pharmacy. */
    _5("5", "Pharmacy");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPrescriptionOriginCode {
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
