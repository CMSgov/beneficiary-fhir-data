package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.converter.WhitespaceTrimConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim header-level benefit enhancement codes. */
@Embeddable
@Getter
public class BenefitEnhancementCodes {

  @Column(name = "clm_bnft_enhncmt_1_cd")
  @Convert(converter = WhitespaceTrimConverter.class)
  private Optional<String> benefitEnhancement1Code;

  @Column(name = "clm_bnft_enhncmt_2_cd")
  @Convert(converter = WhitespaceTrimConverter.class)
  private Optional<String> benefitEnhancement2Code;

  @Column(name = "clm_bnft_enhncmt_3_cd")
  @Convert(converter = WhitespaceTrimConverter.class)
  private Optional<String> benefitEnhancement3Code;

  @Column(name = "clm_bnft_enhncmt_4_cd")
  @Convert(converter = WhitespaceTrimConverter.class)
  private Optional<String> benefitEnhancement4Code;

  @Column(name = "clm_bnft_enhncmt_5_cd")
  @Convert(converter = WhitespaceTrimConverter.class)
  private Optional<String> benefitEnhancement5Code;

  /**
   * Converts the codes to a list of FHIR SupportingInformationComponent.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return SupportingInfoComponent list
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
        .map(
            code ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(BlueButtonSupportingInfoCategory.CLM_BNFT_ENHNCMT_CD.toFhir())
                    .setCode(
                        new CodeableConcept()
                            .addCoding(
                                new Coding()
                                    .setSystem(
                                        SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BENEFIT_ENHANCEMENT_CODE)
                                    .setCode(code))))
        .toList();
  }
}
