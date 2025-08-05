package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line table. */
@Entity
@Table(name = "claim_line", schema = "idr")
public class ClaimLine {
  @EmbeddedId private ClaimLineId claimLineId;

  @Column(name = "clm_line_rev_ctr_cd")
  private Optional<ClaimLineRevenueCenterCode> revenueCenterCode;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private ClaimLineAdjudicationCharge adjudicationCharge;

  @JoinColumns({
    @JoinColumn(name = "clm_uniq_id", referencedColumnName = "clm_uniq_id"),
    @JoinColumn(name = "clm_line_num", referencedColumnName = "clm_line_num")
  })
  @OneToOne
  private ClaimLineInstitutional claimLineInstitutional;

  private Optional<ClaimLineInstitutional> getClaimLineInstitutional() {
    return Optional.ofNullable(claimLineInstitutional);
  }

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  ExplanationOfBenefit.ItemComponent toFhir() {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineId.getClaimLineNumber());
    var institutional = getClaimLineInstitutional();
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    institutional.flatMap(i -> i.getHippsCode().toFhir()).ifPresent(productOrService::addCoding);

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhir().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());

    revenueCenterCode.ifPresent(
        c -> {
          var revenueCoding =
              c.toFhir(institutional.flatMap(ClaimLineInstitutional::getDeductibleCoinsuranceCode));
          line.setRevenue(revenueCoding);
        });

    line.addModifier(hcpcsModifierCode.toFhir());
    institutional
        .map(ClaimLineInstitutional::getRevenueCenterDate)
        .ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    Stream.of(
            institutional.flatMap(c -> c.getAnsiSignature().map(ClaimAnsiSignature::toFhir)),
            Optional.of(adjudicationCharge.toFhir()),
            getClaimLineInstitutional().map(c -> c.getAdjudicationCharge().toFhir()))
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);

    return line;
  }
}
