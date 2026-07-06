package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** TODO. */
public class SupportingInfoFactory {
  private final SequenceGenerator sequenceGenerator;

  /** TODO. */
  public SupportingInfoFactory() {
    this.sequenceGenerator = new SequenceGenerator();
  }

  /**
   * TODO.
   *
   * @return TODO.
   */
  public ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
