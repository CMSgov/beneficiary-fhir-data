package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdcQuantity;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineProfessionalCmsSharedSystems extends ClaimLineProfessionalCms {

  @Embedded private ClaimLineNdcQuantity ndc;
  @Embedded private ClaimLineAdjudicationProfessionalCmsSharedSystems adjudicationCharge;

  @Override
  Optional<ClaimLineAdjudicationProfessional> getAdjudicationCharge() {
    return Optional.of(adjudicationCharge);
  }

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
