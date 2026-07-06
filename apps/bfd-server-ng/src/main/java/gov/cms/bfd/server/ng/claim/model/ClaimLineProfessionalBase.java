package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsModifierCode;
import gov.cms.bfd.server.ng.claim.model.common.RenderingCareTeamLine;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;

/** Claim line info. */
@MappedSuperclass
@Getter
@SuppressWarnings("java:S2201")
abstract class ClaimLineProfessionalBase implements ClaimLineBase {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_dgns_cd")
  private Optional<String> claimLineDiagnosisCode;

  private Optional<String> trackingNumber;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Column(name = "clm_line_thru_dt")
  private Optional<LocalDate> thruDate;

  @Column(name = "clm_pos_cd")
  private Optional<ClaimPlaceOfServiceCode> placeOfServiceCode;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private RenderingCareTeamLine claimLineRenderingProvider;

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

  @Embedded private ClaimLineProfessionalExtensions extensions;

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());
    populateProductAndQuantity(line);
    line.addModifier(hcpcsModifierCode.toFhir());

    thruDate.ifPresentOrElse(
        thru -> {
          var period = new Period();
          fromDate.ifPresent(d -> period.setStartElement(DateUtil.toFhirDate(d)));
          period.setEndElement(DateUtil.toFhirDate(thru));
          line.setServiced(period);
        },
        () -> fromDate.ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d)))));

    getAdjudicationCharge().toFhir().forEach(line::addAdjudication);
    placeOfServiceCode.map(c -> line.setLocation(c.toFhir()));
    getFhirExtensions(options).forEach(line::addExtension);

    return Optional.of(line);
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.of(claimLineRenderingProvider);
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    var trackingSupportingInfo =
        trackingNumber.map(
            number ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(
                        BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM.toFhir())
                    .setValue(new StringType(number)));

    return Stream.of(
            trackingSupportingInfo.stream().toList(),
            lineBenefitEnhancementCodes.toFhir(supportingInfoFactory))
        .flatMap(Collection::stream)
        .toList();
  }

  abstract ClaimLineAdjudicationChargeProfessionalBase getAdjudicationCharge();

  abstract void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item);

  protected List<Extension> getFhirExtensions(ClaimFilterOptions options) {
    return getExtensions().toFhir(options);
  }
}
