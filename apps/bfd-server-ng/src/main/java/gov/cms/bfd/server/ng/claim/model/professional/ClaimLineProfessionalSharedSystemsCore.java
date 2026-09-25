package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdcQuantity;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineServiceUnitQuantity;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Embeddable that contains core SharedSystems clm_line properties and logic. */
@Embeddable
public class ClaimLineProfessionalSharedSystemsCore {

  @Embedded private ClaimLineNdcQuantity ndc;

  void populateProductAndQuantity(
      ExplanationOfBenefit.ItemComponent item,
      ClaimLineHcpcsCode hcpcsCode,
      ClaimLineServiceUnitQuantity serviceUnitQuantity) {
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    ndc.toFhirDetail().ifPresent(item::addDetail);
    item.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    item.setQuantity(serviceUnitQuantity.toFhir());
  }
}
