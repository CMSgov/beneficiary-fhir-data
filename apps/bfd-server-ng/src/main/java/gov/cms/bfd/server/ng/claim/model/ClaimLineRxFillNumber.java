package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Quantity;

@Embeddable
class ClaimLineRxFillNumber {
  @Column(name = "clm_line_rx_fill_num")
  private int fullNumber;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.REFILL_NUM.toFhir())
        .setValue(new Quantity().setValue(fullNumber).setUnit("fill"));
  }
}
