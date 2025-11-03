package gov.cms.bfd.server.ng.claim.model;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Quantity;

@Embeddable
class ClaimLineRxRefillNumber {
  @Column(name = "clm_line_authrzd_fill_num")
  private String refillsAuthorized;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    long value;
    try {
      value = Long.parseLong(refillsAuthorized);
    } catch (NumberFormatException e) {
      throw new InvalidRequestException("Invalid authorized fill number in claim line");
    }

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.REFILLS_AUTHORIZED.toFhir())
        .setValue(new Quantity().setValue(value).setUnit("refills"));
  }
}
