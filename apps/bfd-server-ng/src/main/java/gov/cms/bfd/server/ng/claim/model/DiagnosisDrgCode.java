package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class DiagnosisDrgCode {
  @Column(name = "dgns_drg_cd") // SAMHSA
  private String diagnosisDrgCode;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.DIAGNOSIS_DRG_CODE.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding().setSystem(SystemUrls.CMS_MS_DRG).setCode(diagnosisDrgCode)));

    return supportingInfo;
  }
}
