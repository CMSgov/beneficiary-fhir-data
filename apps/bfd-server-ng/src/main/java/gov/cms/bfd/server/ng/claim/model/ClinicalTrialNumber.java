package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

@Embeddable
class ClinicalTrialNumber {
  @Column(name = "clm_clncl_tril_num")
  private Optional<String> clinicalTrialNum;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return clinicalTrialNum.map(
        trialNumber ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_CLNCL_TRIL_NUM.toFhir())
                .setValue(new StringType(trialNumber)));
  }
}
