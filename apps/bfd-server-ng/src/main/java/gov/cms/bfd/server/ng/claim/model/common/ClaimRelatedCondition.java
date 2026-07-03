package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * TODO.
 */
@Embeddable
public class ClaimRelatedCondition {
  @Column(name = "clm_rlt_cond_cd")
  private Optional<ClaimRelatedConditionCode> claimRelatedConditionCode;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return claimRelatedConditionCode.map(
        codeEnum ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_RLT_COND_CD.toFhir())
                .setCode(codeEnum.toFhir()));
  }
}
