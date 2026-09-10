package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

/** Claim Line, Professional, basis profile, shared systems system. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineProfessionalBasisSharedSystems extends ClaimLineProfessionalBasis {

  @Override
  ClaimLineAdjudicationChargeProfessional getAdjudicationCharge() {
    return null;
  }

  @Override
  List<Extension> getExtensions(ClaimFilterOptions options) {
    return List.of();
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {
    // TODO document why this method is empty
  }
}
