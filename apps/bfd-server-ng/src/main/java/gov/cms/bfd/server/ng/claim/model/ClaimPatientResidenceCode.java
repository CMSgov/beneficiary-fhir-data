package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Patient Residence Codes. */
public sealed interface ClaimPatientResidenceCode
    permits ClaimPatientResidenceCode.Valid, ClaimPatientResidenceCode.Invalid {

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
   * @return claim patient residence code or empty Optional if code is null or blank
   */
  static Optional<ClaimPatientResidenceCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPatientResidenceCode) v)
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
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_PTNT_RSDNC_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PATIENT_RESIDENCE_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimPatientResidenceCode {
    /** 00 - Not specified, other patient residence not identified below. */
    _00("00", "Not specified, other patient residence not identified below"),
    /** 01 - Home. */
    _01("01", "Home"),
    /** 02 - Skilled Nursing Facility. */
    _02("02", "Skilled Nursing Facility"),
    /** 03 - Nursing facility (long-term care facility). */
    _03("03", "Nursing facility (long-term care facility)"),
    /** 04 - Assisted living facility. */
    _04("04", "Assisted living facility"),
    /** 05 - Custodial Care Facility (residential but not medical care). */
    _05("05", "Custodial Care Facility (residential but not medical care)"),
    /** 06 - Group home (e.g., congregate residential foster care). */
    _06("06", "Group home (e.g., congregate residential foster care)"),
    /** 07 - Inpatient Psychiatric Facility. */
    _07("07", "Inpatient Psychiatric Facility"),
    /** 08 - Psychiatric Facility – Partial Hospitalization. */
    _08("08", "Psychiatric Facility – Partial Hospitalization"),
    /** 09 - Intermediate care facility for the mentally retarded (ICF/MR). */
    _09("09", "Intermediate care facility for the mentally retarded (ICF/MR)"),
    /** 10 - Residential Substance Abuse Treatment Facility. */
    _10("10", "Residential Substance Abuse Treatment Facility"),
    /** 11 - Hospice. */
    _11("11", "Hospice"),
    /** 12 - Psychiatric Residential Treatment Facility. */
    _12("12", "Psychiatric Residential Treatment Facility"),
    /** 13 - Comprehensive Inpatient Rehabilitation Facility. */
    _13("13", "Comprehensive Inpatient Rehabilitation Facility"),
    /** 14 - Homeless Shelter. */
    _14("14", "Homeless Shelter"),
    /** 15 - Correctional Institution. */
    _15("15", "Correctional Institution");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPatientResidenceCode {
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
