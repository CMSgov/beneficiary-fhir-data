package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimRelatedCondition {
  @Column(name = "clm_rlt_cond_cd")
  private Optional<ClaimRelatedConditionCode> claimRelatedConditionCode;

  /** Precompile regex for speed gains. Matches on any two characters. */
  private static final Pattern TWO_CHAR_CHUNK = Pattern.compile("(?<=\\G.{2})");

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

  /**
   * Takes in a String which can be empty, or contain one or multiple concatenated codes. It
   * replaces the filler character `~` with `0`. Then splits the string into 2 character chunks.
   * Finally converts these into a list of code(s).
   *
   * @param unsplitCodes String of concatenated codes. Length should be zero or a multiple of two
   * @return a list of the split ClaimRelatedConditionCode values
   */
  private List<ClaimRelatedConditionCode> splitRelatedConditionCodes(String unsplitCodes) {
    return Arrays.stream(TWO_CHAR_CHUNK.split(unsplitCodes.replace('~', '0')))
        .map(ClaimRelatedConditionCode::fromCode)
        .flatMap(Optional::stream)
        .toList();
  }
}
