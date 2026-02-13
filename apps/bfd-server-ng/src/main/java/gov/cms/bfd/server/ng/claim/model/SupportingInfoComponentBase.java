package gov.cms.bfd.server.ng.claim.model;

import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Common interface for supporting-info types. */
public interface SupportingInfoComponentBase {

  /**
   * Converts the date fields to FHIR supporting-info components.
   *
   * @param factory the factory used to assign sequence numbers
   * @return list of supporting-info components; may be empty
   */
  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(SupportingInfoFactory factory);
}
