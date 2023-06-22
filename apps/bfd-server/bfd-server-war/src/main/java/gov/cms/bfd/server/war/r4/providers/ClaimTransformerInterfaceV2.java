package gov.cms.bfd.server.war.r4.providers;

import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterfaceV2 {
  /**
   * Defines a single interface method to transform claim entities.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @param includeTaxNumbers optional {@link Boolean} denoting inclusion of NPI tax info.
   * @return {@link ExplanationOfBenefit}
   */
  public ExplanationOfBenefit transform(Object claimEntity, Optional<Boolean> includeTaxNumbers);
}
