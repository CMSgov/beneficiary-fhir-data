package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdc;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineServiceUnitQuantity;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Embeddable that contains core NCH clm_line properties and logic. */
@Embeddable
public class ClaimLineProfessionalNchCore {

  @Embedded private ClaimLineNdc ndc;

  void populateProductAndQuantity(
      ExplanationOfBenefit.ItemComponent line,
      ClaimLineHcpcsCode hcpcsCode,
      ClaimLineServiceUnitQuantity serviceUnitQuantity) {
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(serviceUnitQuantity.toFhir());
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
  }
}
