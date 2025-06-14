package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SequenceGenerator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

public class SupportingInfoFactory {
  SequenceGenerator sequenceGenerator;

  ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
