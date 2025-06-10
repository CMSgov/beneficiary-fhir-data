package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Arrays;

@Getter
public enum ClaimAdmissionTypeCode {
  UNKNOWN("0", "Unknown Value (but present in data)"),
  EMERGENCY(
      "1",
      "Emergency - The patient required immediate medical intervention as a result of severe, life threatening, or potentially disabling conditions. Generally, the patient was admitted through the emergency room."),
  URGENT(
      "2",
      "Urgent - The patient required immediate attention for the care and treatment of a physical or mental disorder. Generally, the patient was admitted to the first available and suitable accommodation."),
  ELECTIVE(
      "3",
      "Elective - The patient's condition permitted adequate time to schedule the availability of suitable accommodations."),
  NEWBORN("4", "Newborn - Necessitates the use of special source of admission codes."),
  TRAUMA(
      "5",
      "Trauma Center - visits to a trauma center/hospital as licensed or designated by the State or local government authority authorized to do so, or as verified by the American College of Surgeons and involving a trauma activation."),
  RESERVED_6("6", "Reserved"),
  RESERVED_7("7", "Reserved"),
  RESERVED_8("8", "Reserved"),
  UNKNOWN_INFO_NOT_AVAILABLE("9", "Unknown - Information not available.");

  private String code;
  private String display;

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
