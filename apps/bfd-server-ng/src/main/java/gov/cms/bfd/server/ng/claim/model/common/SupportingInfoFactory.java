package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public class SupportingInfoFactory {
  private final SequenceGenerator sequenceGenerator;


  public SupportingInfoFactory() {
    this.sequenceGenerator = new SequenceGenerator();
  }

  public ExplanationOfBenefit.SupportingInformationComponent createSupportingInfo() {
    return new ExplanationOfBenefit.SupportingInformationComponent()
        .setSequence(sequenceGenerator.next());
  }
}
