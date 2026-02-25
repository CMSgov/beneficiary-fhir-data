package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Pharmacy service type codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum PharmacySrvcTypeCode {
  /** 01 - Community/retail pharmacy. */
  _01("01", "Community/retail pharmacy"),
  /** 02 - Compounding pharmacy. */
  _02("02", "Compounding pharmacy"),
  /** 03 - Home infusion therapy provider. */
  _03("03", "Home infusion therapy provider"),
  /** 04 - Institutional pharmacy. */
  _04("04", "Institutional pharmacy"),
  /** 05 - Long-term care pharmacy. */
  _05("05", "Long-term care pharmacy"),
  /** 06 - Mail order pharmacy. */
  _06("06", "Mail order pharmacy"),
  /** 07 - Managed care organization (MCO) pharmacy. */
  _07("07", "Managed care organization (MCO) pharmacy"),
  /** 08 - Specialty care pharmacy. */
  _08("99", "Specialty care pharmacy"),
  /** 18 - Other. */
  _99("99", "Other"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return pharmacy service type code
   */
  public static Optional<PharmacySrvcTypeCode> tryFromCode(String code) {
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
   * @return pharmacy service type code
   */
  public static PharmacySrvcTypeCode handleInvalidValue(String invalidValue) {
    var invalidPharmacySrvcTypeCode = PharmacySrvcTypeCode.INVALID;
    invalidPharmacySrvcTypeCode.code = invalidValue;
    return invalidPharmacySrvcTypeCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_PHRMCY_SRVC_TYPE_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PHARMACY_SRVC_TYPE_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
