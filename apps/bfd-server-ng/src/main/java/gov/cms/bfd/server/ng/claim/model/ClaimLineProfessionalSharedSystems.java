package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.PositiveIntType;
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

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(List<ClaimProcedure> diagnoses) {
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

    line.setDiagnosisSequence(diagnosisRelatedLines(diagnoses));

    return Optional.of(line);
  }

  /**
   * Finds the line numbers of a claim procedure that matches the diagnosis code from this claim
   * line.
   *
   * @param diagnoses The parent claim entity containing all claim procedures.
   * @return The row ids of the matching claim procedure
   */
  public List<PositiveIntType> diagnosisRelatedLines(List<ClaimProcedure> diagnoses) {
    if (diagnosisCode.isEmpty()) {
      return List.of();
    }
    var currentDiagnosisCode = diagnosisCode.get();

    return IntStream.range(0, diagnoses.size())
        .filter(i -> diagnoses.get(i).getDiagnosisCode().orElse("").equals(currentDiagnosisCode))
        .mapToObj(i -> new PositiveIntType(i + 1))
        .toList();
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
