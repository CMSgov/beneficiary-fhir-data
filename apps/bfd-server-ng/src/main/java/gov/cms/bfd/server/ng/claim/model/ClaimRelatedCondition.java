package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimRelatedCondition {

  @Column(name = "clm_rlt_cond_sgntr_sqnc_num")
  private long claimRelatedConditionSgnrNumber;

  @Column(name = "clm_rlt_cond_cd")
  private String claimRelatedConditionCd;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    if (claimRelatedConditionCd.isBlank()) {
      return Optional.empty();
    }

    ExplanationOfBenefit.SupportingInformationComponent component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_RLT_COND_CD.toFhirRelatedCondition())
            .setCode(ClaimRelatedConditionCode.toFhir(claimRelatedConditionCd));

    return Optional.of(component);
  }
}
