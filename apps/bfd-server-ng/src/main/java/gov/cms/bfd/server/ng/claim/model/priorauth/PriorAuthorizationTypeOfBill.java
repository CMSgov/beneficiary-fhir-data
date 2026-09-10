package gov.cms.bfd.server.ng.claim.model.priorauth;

import gov.cms.bfd.server.ng.claim.model.common.CarinSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** SupportingInformationComponent for a type of bill in a prior auth ExplanationOfBenefit. */
@Embeddable
public class PriorAuthorizationTypeOfBill {

  @Column(name = "tob")
  private String typeOfBill;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.TYPE_OF_BILL_CODE.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding().setSystem(SystemUrls.NUBC_TYPE_OF_BILL).setCode(typeOfBill)));
  }
}
