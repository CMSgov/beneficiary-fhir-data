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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
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

  @OneToOne(mappedBy = "claimLine")
  private ClaimLineInstitutional claimLineInstitutional;

  @ManyToOne
  @JoinColumn(name = "clm_uniq_id")
  private Claim claim;

  ExplanationOfBenefit.ItemComponent toFhir() {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineId.getLineNumber());
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
            claimLineInstitutional.getAnsiSignature().toFhir(),
            adjudicationCharge.toFhir(),
            claimLineInstitutional.getAdjudicationCharge().toFhir())
        .flatMap(Collection::stream)
        .forEach(line::addAdjudication);
    line.setServiced(
        new DateTimeType(DateUtil.toDate(claimLineInstitutional.getRevenueCenterDate())));

    return line;
  }
}
