package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim Admission Type codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimAdmissionTypeCode {
  /** 0 - Unknown Value (but present in data). */
  _0("0", "Unknown Value (but present in data)"),
  /**
   * 1 - Emergency - The patient required immediate medical intervention as a result of severe, life
   * threatening, or potentially disabling conditions. Generally, the patient was admitted through
   * the emergency room.
   */
  _1(
      "1",
      "Emergency - The patient required immediate medical intervention as a result of severe, life threatening, or potentially disabling conditions. Generally, the patient was admitted through the emergency room."),
  /**
   * 2 - Urgent - The patient required immediate attention for the care and treatment of a physical
   * or mental disorder. Generally, the patient was admitted to the first available and suitable
   * accommodation.
   */
  _2(
      "2",
      "Urgent - The patient required immediate attention for the care and treatment of a physical or mental disorder. Generally, the patient was admitted to the first available and suitable accommodation."),
  /**
   * 3 - Elective - The patient's condition permitted adequate time to schedule the availability of
   * suitable accommodations.
   */
  _3(
      "3",
      "Elective - The patient's condition permitted adequate time to schedule the availability of suitable accommodations."),
  /** 4 - Newborn - Necessitates the use of special source of admission codes. */
  _4("4", "Newborn - Necessitates the use of special source of admission codes."),
  /**
   * 5 - Trauma Center - visits to a trauma center/hospital as licensed or designated by the State
   * or local government authority authorized to do so, or as verified by the American College of
   * Surgeons and involving a trauma activation.
   */
  _5(
      "5",
      "Trauma Center - visits to a trauma center/hospital as licensed or designated by the State or local government authority authorized to do so, or as verified by the American College of Surgeons and involving a trauma activation."),
  /** 6 - Reserved. */
  _6("6", "Reserved"),
  /** 7 - Reserved. */
  _7("7", "Reserved"),
  /** 8 - Reserved. */
  _8("8", "Reserved"),
  /** 9 - Unknown - Information not available. */
  _9("9", "Unknown - Information not available.");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim admission type code
   */
  public static ClaimAdmissionTypeCode fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.ADMISSION_TYPE_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(SystemUrls.NUBC_TYPE_OF_ADMIT).setCode(code))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADMISSION_TYPE_CODE)
                        .setCode(code)));
  }
}
