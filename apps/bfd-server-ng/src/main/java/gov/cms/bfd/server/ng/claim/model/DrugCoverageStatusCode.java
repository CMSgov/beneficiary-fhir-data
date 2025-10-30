package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@AllArgsConstructor
@Getter
public enum DrugCoverageStatusCode {
  /** C - Covered */
  C("C", "Covered"),
  /** E - Supplemental drugs (reported by plans that provide Enhanced Alternative coverage) */
  E("E", "Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)"),
  /** O - Over-the-counter drugs */
  O("O", "Over-the-counter drugs");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return drug coverage status code
   */
  public static Optional<DrugCoverageStatusCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_DRUG_CVRG_STUS_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_DRUG_COVERAGE_STATUS_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
