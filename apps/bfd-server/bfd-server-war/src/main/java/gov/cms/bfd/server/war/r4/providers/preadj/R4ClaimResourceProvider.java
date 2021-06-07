package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Optional;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link Claim} resources. */
@Component
public class R4ClaimResourceProvider extends AbstractR4ResourceProvider<Claim> {

  /**
   * Helper method to make mocking easier in tests.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimTypeV2} type.
   */
  @VisibleForTesting
  Optional<ResourceTypeV2<Claim>> parseClaimType(String typeText) {
    return ClaimTypeV2.parse(typeText);
  }
}
