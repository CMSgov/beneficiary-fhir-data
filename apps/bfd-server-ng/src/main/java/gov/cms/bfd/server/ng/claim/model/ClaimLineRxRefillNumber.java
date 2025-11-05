package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.StringToIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Quantity;

@Embeddable
class ClaimLineRxRefillNumber {
  @Column(name = "clm_line_authrzd_fill_num")
  @Convert(converter = StringToIntConverter.class)
  private int refillsAuthorized;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.REFILLS_AUTHORIZED.toFhir())
        .setValue(new Quantity().setValue(refillsAuthorized).setUnit("refills"));
  }
}
