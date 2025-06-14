package gov.cms.bfd.server.ng.claim.model;

import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@AllArgsConstructor
public enum PpsIndicatorCode {
  PPS("2", "PPS bill; claim contains PPS indicator"),
  NOT_PPS("unknown", "Not a PPS bill");

  private final String code;
  private final String display;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_PPS_IND_CD.toFhir())
        .setCode(new CodeableConcept(new Coding().setDisplay(display).setCode(code)));
  }
}
