package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

@SuppressWarnings({"checkstyle:MissingJavadocType", "checkstyle:MissingJavadocMethod"})
@Embeddable
public class ClaimLineRxNumber {
  @Column(name = "clm_line_rx_num")
  private String claimLineRxNum;

  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
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
