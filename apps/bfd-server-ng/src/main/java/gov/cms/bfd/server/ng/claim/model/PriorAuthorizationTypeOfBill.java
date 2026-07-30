package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class PriorAuthorizationTypeOfBill {

  @Column(name = "tob")
  private String typeOfBill;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.TYPE_OF_BILL_CODE.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding().setSystem(SystemUrls.NUBC_TYPE_OF_BILL).setCode(typeOfBill)));
  }
}
