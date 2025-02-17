package gov.cms.bfd.server.war.stu3.providers;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterface {
  /**
   * Interface method to transform claims; if the Claim type supports inclusion of NPI tax
   * information, and the boolean includeTaxNumbers is true, then the transformer will attempt to
   * provide NPI tax info.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @param includeTaxNumbers {@link boolean} denoting inclusion of tax numbers.
   * @return {@link ExplanationOfBenefit}
   */
  ExplanationOfBenefit transform(Object claimEntity, boolean includeTaxNumbers);
}
