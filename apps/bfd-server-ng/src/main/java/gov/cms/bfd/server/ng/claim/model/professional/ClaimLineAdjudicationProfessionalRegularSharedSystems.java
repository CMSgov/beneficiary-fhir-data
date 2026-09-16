package gov.cms.bfd.server.ng.claim.model.professional;

import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication data for Professional-Regular-SharedSystems. */
@Embeddable
public class ClaimLineAdjudicationProfessionalRegularSharedSystems
    extends ClaimLineAdjudicationProfessional {

  @Override
  List<ExplanationOfBenefit.AdjudicationComponent> addSubclassAdjudications() {
    return new ArrayList<>(super.addSubclassAdjudications());
  }
}
