package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Embedded;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line data for Professional-Regular-SharedSystems. */
public class ClaimLineProfessionalRegularSharedSystems extends ClaimLineProfessionalRegular {

  @Embedded private ClaimLineAdjudicationProfessionalRegularSharedSystems adjudicationCharge;

  @Override
  Optional<AdjudicationEmbedded> getClaimLineAdjudication() {
    return Optional.of(adjudicationCharge);
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {}
}
