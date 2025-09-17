package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class SupportingInfoFactory {
  private final SequenceGenerator sequenceGenerator;

  public SupportingInfoFactory() {
    this.sequenceGenerator = new SequenceGenerator();
  }

  ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
