package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum ClaimAdmissionSourceCode {
  NON_HEALTH_CARE_FACILITY(
      "1",
      "NON-HEALTH CARE FACILITY POINT OF ORIGIN (NON-NEWBORN)/RESERVED FOR NATIONAL ASSIGNMENT (NEWBORN)");
  private final String code;
  private final String display;

  public static ClaimAdmissionSourceCode fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.ADMISSION_SOURCE_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(SystemUrls.NUBC_POINT_OF_ORIGIN).setCode(code))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADMISSION_SOURCE_CODE)
                        .setCode(code)
                        .setDisplay(display)));
  }
}
