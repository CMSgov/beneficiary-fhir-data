package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineProfessionalSharedSystems {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_dgns_cd")
  private Optional<String> diagnosisCode;

  @Column(name = "clm_line_pa_uniq_trkng_num")
  private Optional<String> partiallyAdjudicatedTrackingNumber;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private ClaimLineAdjudicationChargeProfessionalSharedSystems adjudicationCharge;
  @Embedded private RenderingProviderLineHistory claimLineRenderingProvider;

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());

    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    var quantity = serviceUnitQuantity.toFhir();

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);
    line.addModifier(hcpcsModifierCode.toFhir());
    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    adjudicationCharge.toFhir().forEach(line::addAdjudication);

    //    line.setDiagnosisSequence(diagnosisRelatedLines(diagnoses));

    return Optional.of(line);
  }

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {

    if (partiallyAdjudicatedTrackingNumber.isEmpty()) {
      return Optional.empty();
    }

    var category = BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM;

    return Optional.of(
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(category.toFhir())
            .setValue(new StringType(partiallyAdjudicatedTrackingNumber.get())));
  }
}
