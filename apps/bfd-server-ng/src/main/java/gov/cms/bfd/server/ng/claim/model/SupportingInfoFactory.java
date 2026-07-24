package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Factory class responsible for creating SupportingInformationComponents with unique sequence
 * numbers.
 */
public class SupportingInfoFactory {
  private final SequenceGenerator sequenceGenerator;

  /** Creates a new factory and sets up the sequence generator. */
  public SupportingInfoFactory() {
    this.sequenceGenerator = new SequenceGenerator();
  }

  /**
   * Creates a new instance of SupportingInformationComponent with a unique sequence number.
   *
   * @return a SupportingInformationComponent with its sequence property set
   */
  ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
