package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.BenefitEnhancementCodes;
import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPlaceOfServiceCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

/** clm_line row data, Professional domain, CMS profile, shared sources. */
@MappedSuperclass
@Getter
abstract class ClaimLineProfessionalCms extends ClaimLineProfessionalBase {

  @Embedded ExtensionsProfessionalCms extensions;

  private Optional<String> trackingNumber;

  @Column(name = "clm_pos_cd")
  private Optional<ClaimPlaceOfServiceCode> placeOfServiceCode;

  @Embedded
  @AttributeOverride(
      name = "benefitEnhancement1Code",
      column = @Column(name = "clm_line_bnft_enhncmt_1_cd"))
  @AttributeOverride(
      name = "benefitEnhancement2Code",
      column = @Column(name = "clm_line_bnft_enhncmt_2_cd"))
  @AttributeOverride(
      name = "benefitEnhancement3Code",
      column = @Column(name = "clm_line_bnft_enhncmt_3_cd"))
  @AttributeOverride(
      name = "benefitEnhancement4Code",
      column = @Column(name = "clm_line_bnft_enhncmt_4_cd"))
  @AttributeOverride(
      name = "benefitEnhancement5Code",
      column = @Column(name = "clm_line_bnft_enhncmt_5_cd"))
  private BenefitEnhancementCodes lineBenefitEnhancementCodes;

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {
    var line = super.toFhirItemComponent(options);
    line.ifPresent(item -> placeOfServiceCode.map(c -> item.setLocation(c.toFhir())));
    return line;
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            super.toFhirSupportingInfo(supportingInfoFactory).stream(),
            lineBenefitEnhancementCodes.toFhir(supportingInfoFactory).stream(),
            getTrackingNumberSupportingInfo(supportingInfoFactory).stream())
        .flatMap(Function.identity())
        .toList();
  }

  @Override
  public List<Extension> getExtensions(ClaimFilterOptions options) {
    return extensions.toFhir(options);
  }

  /**
   * Helper method with logic for professional tracking number mapping.
   *
   * @param supportingInfoFactory the factory of supporting info
   * @return a list of 0..1 supporting info components
   */
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      getTrackingNumberSupportingInfo(SupportingInfoFactory supportingInfoFactory) {
    return trackingNumber
        .map(
            number ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(
                        BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM.toFhir())
                    .setValue(new StringType(number)))
        .stream()
        .toList();
  }
}
