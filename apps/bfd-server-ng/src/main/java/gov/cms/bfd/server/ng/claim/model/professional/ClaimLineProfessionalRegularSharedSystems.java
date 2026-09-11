package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;

/** Line item, professional, regular profile, shared system. It is what it is. */
public class ClaimLineProfessionalRegularSharedSystems extends ClaimLineProfessionalRegular {
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

    }
}
