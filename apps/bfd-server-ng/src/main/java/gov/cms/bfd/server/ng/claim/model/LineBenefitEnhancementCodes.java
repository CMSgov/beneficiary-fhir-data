package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Claim line level benefit enhancement codes. */
@Embeddable
@Getter
public class LineBenefitEnhancementCodes {

  @Column(name = "clm_line_bnft_enhncmt_1_cd")
  private Optional<String> benefitEnhancement1Code;

  @Column(name = "clm_line_bnft_enhncmt_2_cd")
  private Optional<String> benefitEnhancement2Code;

  @Column(name = "clm_line_bnft_enhncmt_3_cd")
  private Optional<String> benefitEnhancement3Code;

  @Column(name = "clm_line_bnft_enhncmt_4_cd")
  private Optional<String> benefitEnhancement4Code;

  @Column(name = "clm_line_bnft_enhncmt_5_cd")
  private Optional<String> benefitEnhancement5Code;

  /**
   * Converts codes to a list of benefit enhancement codes.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return list of SupportingInformationComponents
   */
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return Stream.of(
            benefitEnhancement1Code,
            benefitEnhancement2Code,
            benefitEnhancement3Code,
            benefitEnhancement4Code,
            benefitEnhancement5Code)
        .flatMap(Optional::stream)
        .filter(s -> !s.isBlank())
        .map(
            code ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(BlueButtonSupportingInfoCategory.CLM_BNFT_ENHNCMT_CD.toFhir())
                    .setValue(new StringType(code)))
        .toList();
  }
}
