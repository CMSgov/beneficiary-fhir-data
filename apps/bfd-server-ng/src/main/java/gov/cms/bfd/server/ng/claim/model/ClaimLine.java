package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.PositiveIntType;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLine {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_rev_ctr_cd")
  private Optional<ClaimLineRevenueCenterCode> revenueCenterCode;

  @Column(name = "clm_line_dgns_cd")
  private Optional<String> diagnosisCode;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private ClaimLineAdjudicationCharge adjudicationCharge;
  @Embedded private ClaimRenderingProvider claimRenderingProvider;

  Optional<ExplanationOfBenefit.ItemComponent> toFhir(
      ClaimItem claimItem, List<ClaimProcedure> diagnoses) {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());

    var claimLineInstitutional = claimItem.getClaimLineInstitutional();
    var claimLineRx = claimItem.getClaimLineRx();
    var claimLineProfessional = claimItem.getClaimLineProfessional();
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    claimLineInstitutional
        .flatMap(i -> i.getHippsCode().toFhir())
        .ifPresent(productOrService::addCoding);

    var quantity = serviceUnitQuantity.toFhir();

    claimLineRx.flatMap(ClaimLineRx::toFhirNdcCompound).ifPresent(productOrService::addCoding);

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);

    revenueCenterCode.ifPresent(
        c -> {
          var revenueCoding =
              c.toFhir(
                  claimLineInstitutional.flatMap(
                      ClaimLineInstitutional::getDeductibleCoinsuranceCode));
          line.setRevenue(revenueCoding);
        });

    line.addModifier(hcpcsModifierCode.toFhir());
    claimLineInstitutional
        .flatMap(ClaimLineInstitutional::getRevenueCenterDate)
        .ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    var adjudicationLines =
        Stream.of(
            claimLineInstitutional.flatMap(
                c -> c.getAnsiSignature().map(ClaimAnsiSignature::toFhir)),
            Optional.of(adjudicationCharge.toFhir()),
            claimLineInstitutional.map(
                c -> c.getClaimLineAdjudicationChargeInstitutional().toFhir()),
            claimLineRx.map(c -> c.getClaimLineAdjudicationChargeRx().toFhir()),
            claimLineProfessional.map(
                c -> c.getClaimLineAdjudicationChargeProfessional().toFhir()));
    adjudicationLines
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);

    line.setDiagnosisSequence(diagnosisRelatedLines(diagnoses));

    claimLineInstitutional
        .map(ClaimLineInstitutional::getExtensions)
        .ifPresent(e -> line.setExtension(e.toFhir()));

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
}
