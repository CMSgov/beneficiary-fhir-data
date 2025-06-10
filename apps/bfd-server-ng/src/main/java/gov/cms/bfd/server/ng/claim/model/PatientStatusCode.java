package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Arrays;

@Getter
public enum PatientStatusCode {
  DISCHARGED_TO_HOME("01", "DISCHARGED TO HOME OR SELF CARE (ROUTINE DISCHARGE)");

  private String code;
  private String display;

  public static PatientStatusCode fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.PATIENT_STATUS_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(
                    new Coding().setSystem(SystemUrls.NUBC_PATIENT_DISCHARGE_STATUS).setCode(code))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PATIENT_STATUS_CODE)
                        .setCode(code)
                        .setDisplay(display)));
  }
}
