package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Entity
public class ClaimLine {
  @Column(name = "clm_line_num")
  private int lineNumber;

  @OneToOne private ClaimLineInstitutional claimLineInstitutional;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;

  ExplanationOfBenefit.ItemComponent toFhir() {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(lineNumber);
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    claimLineInstitutional.getHippsCode().toFhir().ifPresent(productOrService::addCoding);
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhir().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());
    return line;
  }
}
