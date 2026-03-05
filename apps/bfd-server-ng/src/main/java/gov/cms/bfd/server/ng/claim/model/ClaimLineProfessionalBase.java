package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
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
  private Optional<String> diagnosisCode;

  private Optional<String> trackingNumber;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;

  @Embedded private RenderingCareTeamLine claimLineRenderingProvider;

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());
    populateProductAndQuantity(line);
    line.addModifier(hcpcsModifierCode.toFhir());
    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));
    getAdjudicationCharge().toFhir().forEach(line::addAdjudication);

    return Optional.of(line);
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.of(claimLineRenderingProvider);
  }

  @Override
  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return trackingNumber.map(
        number ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM.toFhir())
                .setValue(new StringType(number)));
  }

  abstract ClaimLineAdjudicationChargeProfessionalBase getAdjudicationCharge();

  abstract void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item);
}
