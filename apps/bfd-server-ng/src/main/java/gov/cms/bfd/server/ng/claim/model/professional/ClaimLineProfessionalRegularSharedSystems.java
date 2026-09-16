package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line data for Professional-Regular-SharedSystems. */
@Embeddable
public class ClaimLineProfessionalRegularSharedSystems extends ClaimLineProfessionalRegular {

  @Embedded private ClaimLineProfessionalSharedSystemsCore sharedSystemsCore;
  @Embedded private ClaimLineAdjudicationProfessionalRegular adjudicationCharge;

  @Override
  Optional<AdjudicationEmbedded> getClaimLineAdjudication() {
    return Optional.of(adjudicationCharge);
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {
    sharedSystemsCore.populateProductAndQuantity(item, getHcpcsCode(), getServiceUnitQuantity());
  }
}
