package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

@Embeddable
public class ClaimLineRxNumber {
  @Column(name = "clm_line_rx_num")
  private String claimLineRxNum;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_LINE_RX_NUM.toFhir())
        .setValue(new StringType(claimLineRxNum));
  }
}
