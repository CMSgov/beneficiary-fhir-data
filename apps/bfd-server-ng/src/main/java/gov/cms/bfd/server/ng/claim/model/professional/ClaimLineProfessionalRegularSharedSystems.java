package gov.cms.bfd.server.ng.claim.model.professional;

import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Line item, professional, regular profile, shared system. It is what it is. */
public class ClaimLineProfessionalRegularSharedSystems extends ClaimLineProfessionalRegular {
  @Override
  Optional<ClaimLineAdjudicationProfessional> getAdjudicationCharge() {
    return Optional.empty();
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {}
}
