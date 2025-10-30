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
public enum CatastrophicCoverageCode {
  /** A - Attachment point met on this even */
  A("A", "Attachment point met on this even"),
  /** C - Above attachment point */
  C("C", "Above attachment point");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return catastrophic coverage code
   */
  public static Optional<CatastrophicCoverageCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_CTSTRPHC_CVRG_IND_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CATASTROPHIC_COVERAGE_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
