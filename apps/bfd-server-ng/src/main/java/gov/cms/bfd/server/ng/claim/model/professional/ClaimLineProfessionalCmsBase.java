package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.BenefitEnhancementCodes;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPlaceOfServiceCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

/** ExplanationOfBenefit.Item component Professional domain, CMS profile. */
@MappedSuperclass
@Getter
abstract class ClaimLineProfessionalCmsBase extends ClaimLineProfessionalBase {

  @Embedded ClaimLineExtensionsCms extensions;

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
    var supportingInfo = new ArrayList<>(super.toFhirSupportingInfo(supportingInfoFactory));
    supportingInfo.addAll(lineBenefitEnhancementCodes.toFhir(supportingInfoFactory));
    return supportingInfo;
  }

  @Override
  public List<Extension> getExtensions(ClaimFilterOptions options) {
    return extensions.toFhir(options);
  }
}
