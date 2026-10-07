package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimRelatedCondition {
  @Column(name = "clm_rlt_cond_cd")
  private Optional<ClaimRelatedConditionCode> claimRelatedConditionCode;

  // The length of clm_rlt_cond_cd will always be a multiple of 2 due to our pipeline concatenation
  // rules.
  private static final int CODE_SPLIT_LENGTH = 2;

  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return claimRelatedConditionCode
        .map(codeEnum -> splitRelatedConditionCodes(codeEnum.getCode()))
        .orElseGet(List::of)
        .stream()
        .map(
            singleCode ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(BlueButtonSupportingInfoCategory.CLM_RLT_COND_CD.toFhir())
                    .setCode(singleCode.toFhir()))
        .toList();
  }

  private List<ClaimRelatedConditionCode> splitRelatedConditionCodes(String unsplitCodes) {
    List<ClaimRelatedConditionCode> splitCodes = new ArrayList<>();

    for (int i = 0; i < unsplitCodes.length(); i += CODE_SPLIT_LENGTH) {
      String unparsedCode = unsplitCodes.substring(i, i + CODE_SPLIT_LENGTH);

      String parsedCode =
          unparsedCode.charAt(0) == '~' ? "0" + unparsedCode.substring(1) : unparsedCode;

      ClaimRelatedConditionCode.fromCode(parsedCode).ifPresent(splitCodes::add);
    }

    return splitCodes;
  }
}
