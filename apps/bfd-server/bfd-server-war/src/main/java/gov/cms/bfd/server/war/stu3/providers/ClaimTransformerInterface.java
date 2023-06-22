package gov.cms.bfd.server.war.stu3.providers;

import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterface {
  /**
   * Defines a single interface method to transform claim entities.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @param includeTaxNumbers {@link Boolean} denoting inclusion of tax numbers.
   * @return {@link ExplanationOfBenefit}
   */
  public ExplanationOfBenefit transform(Object claimEntity, Optional<Boolean> includeTaxNumbers);
}
