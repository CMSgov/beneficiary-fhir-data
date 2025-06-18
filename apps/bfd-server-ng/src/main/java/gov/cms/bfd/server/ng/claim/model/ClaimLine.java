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

@Entity
@Table(name = "claim_line", schema = "idr")
public class ClaimLine {
  @EmbeddedId private ClaimLineId claimLineId;

  @Column(name = "clm_line_rev_ctr_cd")
  private ClaimLineRevenueCenterCode revenueCenterCode;

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

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  ExplanationOfBenefit.ItemComponent toFhir() {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineId.getClaimLineNumber());
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    claimLineInstitutional.getHippsCode().toFhir().ifPresent(productOrService::addCoding);
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhir().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());

    var revenueCoding = revenueCenterCode.toFhir();
    revenueCoding.addCoding(claimLineInstitutional.getDeductibleCoinsuranceCode().toFhir());
    line.setRevenue(revenueCoding);
    line.addModifier(hcpcsModifierCode.toFhir());

    Stream.of(
            claimLineInstitutional.getAnsiSignature().map(ClaimAnsiSignature::toFhir),
            Optional.of(adjudicationCharge.toFhir()),
            Optional.of(claimLineInstitutional.getAdjudicationCharge().toFhir()))
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);
    line.setServiced(new DateType(DateUtil.toDate(claimLineInstitutional.getRevenueCenterDate())));

    return line;
  }
}
