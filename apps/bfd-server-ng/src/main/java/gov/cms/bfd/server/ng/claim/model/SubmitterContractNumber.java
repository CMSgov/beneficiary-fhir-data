package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Submitter Contract Number. */
@Embeddable
public class SubmitterContractNumber {
  @Column(name = "clm_sbmtr_cntrct_num")
  private Optional<String> contractNumber;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return contractNumber.map(
        num ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_SBMTR_CNTRCT_NUM.toFhir())
                .setValue(new StringType(num)));
  }
}
