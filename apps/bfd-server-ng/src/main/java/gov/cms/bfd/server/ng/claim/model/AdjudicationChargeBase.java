package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
interface AdjudicationChargeBase {
  List<ExplanationOfBenefit.TotalComponent> toFhirTotal();

  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication();
}
