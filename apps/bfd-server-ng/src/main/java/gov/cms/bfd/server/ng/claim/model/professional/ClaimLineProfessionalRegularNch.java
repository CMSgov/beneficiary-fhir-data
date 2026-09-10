package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

/** Line item, professional, regular profile nch data source. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineProfessionalRegularNch extends ClaimLineProfessionalRegular {
  @Override
  ClaimLineAdjudicationChargeProfessional getAdjudicationCharge() {
    return null;
  }

  @Override
  List<Extension> getExtensions(ClaimFilterOptions options) {
    return List.of();
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent item) {}
}
