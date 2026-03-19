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
public sealed interface PharmacySrvcTypeCode
    permits PharmacySrvcTypeCode.Valid, PharmacySrvcTypeCode.Invalid {

  /** Returns the code. */
  @SuppressWarnings("checkstyle:JavadocMethod")
  String getCode();

  /** Returns the display or returns an empty string if invalid. */
  @SuppressWarnings("checkstyle:JavadocMethod")
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return pharmacy service type code
   */
  static Optional<PharmacySrvcTypeCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (PharmacySrvcTypeCode) v)
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
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_PHRMCY_SRVC_TYPE_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PHARMACY_SRVC_TYPE_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /**
   * Enum for all known, valid pharmacy service type codes.
   */
  @AllArgsConstructor
  @Getter
  enum Valid implements PharmacySrvcTypeCode {
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
    _08("08", "Specialty care pharmacy"),
    /** 99 - Other. */
    _99("99", "Other");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements PharmacySrvcTypeCode {
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
