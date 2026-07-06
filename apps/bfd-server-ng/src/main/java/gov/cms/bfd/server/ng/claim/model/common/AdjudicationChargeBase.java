package gov.cms.bfd.server.ng.claim.model.common;

import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adjudication Charge Base interface. */
public interface AdjudicationChargeBase {
  /**
   * toFhir().
   *
   * @return an eob.TotalComponent
   */
  List<ExplanationOfBenefit.TotalComponent> toFhirTotal();

  /**
   * toFhirAdjudication().
   *
   * @return an eob.AdjudicationComponent
   */
  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication();
}
