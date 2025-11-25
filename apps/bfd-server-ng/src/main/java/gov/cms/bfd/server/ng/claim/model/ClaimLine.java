package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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

  Optional<ExplanationOfBenefit.ItemComponent> toFhir(ClaimItem claimItem) {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());

    var claimLineInstitutional = claimItem.getClaimLineInstitutional();
    var claimLineRx = claimItem.getClaimLineRx();
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    claimLineInstitutional
        .flatMap(i -> i.getHippsCode().toFhir())
        .ifPresent(productOrService::addCoding);
    boolean isNDCCompound =
        claimLineRx
            .flatMap(c -> c.getClaimRxSupportingInfo().getCompoundCode())
            .filter(c -> c == ClaimLineCompoundCode._2)
            .isPresent();
    if (isNDCCompound) {
      ndc.toDetail().ifPresent(line::addDetail);
      productOrService.addCoding(
          new Coding().setSystem(SystemUrls.CARIN_COMPOUND_LITERAL).setCode("compound"));
    } else {
      ndc.toFhir().ifPresent(productOrService::addCoding);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));

    var quantity = serviceUnitQuantity.toFhir();
    ndc.getQualifier().ifPresent(quantity::setUnit);
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
        .map(ClaimLineInstitutional::getRevenueCenterDate)
        .ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    var adjudicationLines =
        Stream.of(
            claimLineInstitutional.flatMap(
                c -> c.getAnsiSignature().map(ClaimAnsiSignature::toFhir)),
            Optional.of(adjudicationCharge.toFhir()),
            claimLineInstitutional.map(c -> c.getAdjudicationCharge().toFhir()),
            claimLineRx.map(c -> c.getAdjudicationCharge().toFhir()));
    adjudicationLines
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);

    line.setDiagnosisSequence(diagnosisRelatedLines(claimItem.getClaim()));

    claimLineInstitutional
        .map(ClaimLineInstitutional::getExtensions)
        .ifPresent(e -> line.setExtension(e.toFhir()));

    return Optional.of(line);
  }

  /**
   * Finds the line numbers of a claim procedure that matches the diagnosis code from this claim
   * line.
   *
   * @param claim The parent claim entity containing all claim procedures.
   * @return The row ids of the matching claim procedure
   */
  public List<PositiveIntType> diagnosisRelatedLines(Claim claim) {
    if (diagnosisCode.isEmpty()) {
      return List.of();
    }
    var currentDiagnosisCode = diagnosisCode.get();

    return claim.getClaimItems().stream()
        .filter(
            item ->
                item.getClaimProcedure().getDiagnosisCode().orElse("").equals(currentDiagnosisCode))
        .map(item -> item.getClaimItemId().getBfdRowId())
        .map(PositiveIntType::new)
        .collect(Collectors.toList());
  }
}
