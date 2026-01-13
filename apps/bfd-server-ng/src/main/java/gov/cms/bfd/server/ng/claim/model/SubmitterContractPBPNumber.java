package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Submitter Contract PBP Number. */
@Embeddable
public class SubmitterContractPBPNumber {
  @Column(name = "clm_sbmtr_cntrct_pbp_num")
  private String contractPbpNumber;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    if (contractPbpNumber.isBlank()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_SBMTR_CNTRCT_PBP_NUM.toFhir())
            .setValue(new StringType(contractPbpNumber));

    return Optional.of(component);
  }
}
