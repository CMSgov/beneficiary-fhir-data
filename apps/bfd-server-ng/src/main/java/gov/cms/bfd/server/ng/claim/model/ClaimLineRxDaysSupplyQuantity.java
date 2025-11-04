package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Quantity;

@Embeddable
class ClaimLineRxDaysSupplyQuantity {
  @Column(name = "clm_line_days_suply_qty")
  private int daysSupply;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.DAYS_SUPPLY.toFhir())
        .setValue(new Quantity().setValue(daysSupply).setUnit("days"));
  }
}
