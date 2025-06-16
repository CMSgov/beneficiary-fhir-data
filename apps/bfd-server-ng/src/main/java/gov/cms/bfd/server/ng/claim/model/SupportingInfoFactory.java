package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SequenceGenerator;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

public class SupportingInfoFactory {
  private final SequenceGenerator sequenceGenerator;

  public SupportingInfoFactory() {
    this.sequenceGenerator = new SequenceGenerator();
  }

  ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
