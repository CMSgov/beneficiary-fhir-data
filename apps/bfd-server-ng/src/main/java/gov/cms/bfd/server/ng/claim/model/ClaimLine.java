package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.PositiveIntType;

/** Claim line info. */
@Embeddable
@Getter
public class ClaimLine {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_rev_ctr_cd")
  private Optional<ClaimLineRevenueCenterCode> revenueCenterCode;

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
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    claimLineInstitutional
        .flatMap(i -> i.getHippsCode().toFhir())
        .ifPresent(productOrService::addCoding);

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhir().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());

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

    Stream.of(
            claimLineInstitutional.flatMap(
                c -> c.getAnsiSignature().map(ClaimAnsiSignature::toFhir)),
            Optional.of(adjudicationCharge.toFhir()),
            claimLineInstitutional.map(c -> c.getAdjudicationCharge().toFhir()))
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);

    line.setDiagnosisSequence(
        List.of(new PositiveIntType(claimItem.getClaimItemId().getBfdRowId())));

    claimLineInstitutional
        .map(ClaimLineInstitutional::getExtensions)
        .ifPresent(e -> line.setExtension(e.toFhir()));

    return Optional.of(line);
  }
}
