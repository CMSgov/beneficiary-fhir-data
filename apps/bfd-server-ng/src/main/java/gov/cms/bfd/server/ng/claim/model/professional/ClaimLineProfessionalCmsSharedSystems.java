package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdc;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineProfessionalCmsSharedSystems extends ClaimLineProfessionalCmsBase {

  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineAdjudicationChargeProfessionalSharedSystems adjudicationCharge;

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    var productOrService = new CodeableConcept();
    getHcpcsCode().toFhir().ifPresent(productOrService::addCoding);
    var quantity = getServiceUnitQuantity().toFhir();
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    line.setQuantity(quantity);
  }
}
