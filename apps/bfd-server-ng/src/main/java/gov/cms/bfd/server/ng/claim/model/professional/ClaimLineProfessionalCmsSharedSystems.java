package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineProfessionalCmsSharedSystems extends ClaimLineProfessionalCms {

  @Embedded private ClaimLineProfessionalSharedSystemsCore sharedSystemsCore;
  @Embedded private ClaimLineAdjudicationProfessionalCmsSharedSystems adjudicationCharge;

  @Override
  Optional<AdjudicationEmbedded> getClaimLineAdjudication() {
    return Optional.of(adjudicationCharge);
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {
    sharedSystemsCore.populateProductAndQuantity(item, getHcpcsCode(), getServiceUnitQuantity());
  }
}
