package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;

/** Institutional claim line table. */
@Getter
@MappedSuperclass
public abstract class ClaimLineInstitutionalBase implements ClaimLineBase {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_rev_ctr_cd")
  private Optional<ClaimLineRevenueCenterCode> revenueCenterCode;

  private Optional<String> trackingNumber;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private Optional<LocalDate> revenueCenterDate;

  @Column(name = "clm_line_thru_dt")
  private Optional<LocalDate> thruDate;

  @Embedded private ClaimLineHippsCode hippsCode;
  @Embedded private ClaimLineInstitutionalExtensions extensions;
  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private RenderingCareTeamLine claimLineRenderingProvider;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            trackingNumber.map(
                number ->
                    supportingInfoFactory
                        .createSupportingInfo()
                        .setCategory(
                            BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM.toFhir())
                        .setValue(new StringType(number))))
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.of(claimLineRenderingProvider);
  }

  @Override
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    return Optional.empty();
  }

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());

    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    getHippsCode().toFhir().ifPresent(productOrService::addCoding);

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());

    revenueCenterCode.ifPresent(
        c -> {
          var revenueCoding = c.toFhir(getDeductibleCoinsuranceCode());
          line.setRevenue(revenueCoding);
        });

    line.addModifier(hcpcsModifierCode.toFhir());
    thruDate.ifPresentOrElse(
        thru -> {
          var period = new Period();
          getRevenueCenterDate().ifPresent(d -> period.setStart(DateUtil.toDate(d)));
          fromDate.ifPresent(d -> period.setStart(DateUtil.toDate(d)));
          period.setEnd(DateUtil.toDate(thru));
          line.setServiced(period);
        },
        () -> {
          getRevenueCenterDate().ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));
          fromDate.ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));
        });

    addAdjudication(line);
    getExtensions().toFhir().forEach(line::addExtension);

    return Optional.of(line);
  }

  abstract void addAdjudication(ExplanationOfBenefit.ItemComponent line);
}
