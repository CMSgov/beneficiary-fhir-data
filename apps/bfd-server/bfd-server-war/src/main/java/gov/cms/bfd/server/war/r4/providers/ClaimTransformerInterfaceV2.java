package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterfaceV2 {
  /**
   * Interface method to transform claims.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @return {@link ExplanationOfBenefit}
   */
  ExplanationOfBenefit transform(ClaimWithSecurityTags<?> claimEntity);
}
