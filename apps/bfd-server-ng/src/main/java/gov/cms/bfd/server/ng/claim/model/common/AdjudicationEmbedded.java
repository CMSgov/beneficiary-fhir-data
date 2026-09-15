package gov.cms.bfd.server.ng.claim.model.common;

import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adjudication Charge Base interface. */
public interface AdjudicationEmbedded {

  /**
   * toFhirAdjudication().
   *
   * @return a list of eob.AdjudicationComponent
   */
  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication();

  /**
   * toFhirTotal(), optional.
   *
   * @return a list of eob.TotalComponent
   */
  default List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of();
  }
}
