package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

@Embeddable
class ClaimLineRxNumber {
  @Column(name = "clm_line_rx_num")
  private String claimLineRxNum;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    if (claimLineRxNum.isBlank()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_LINE_RX_NUM.toFhir())
            .setValue(new StringType(claimLineRxNum));

    return Optional.of(component);
  }
}
