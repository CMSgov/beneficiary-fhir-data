package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link ClaimResponse} resources. */
@Component
public class R4ClaimResponseResourceProvider extends AbstractR4ResourceProvider<ClaimResponse> {

  /**
   * Specifies the exact resource configuration class to use for parsing.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimResponseTypeV2} type.
   */
  @VisibleForTesting
  Optional<ResourceTypeV2<ClaimResponse>> parseClaimType(String typeText) {
    return ClaimResponseTypeV2.parse(typeText);
  }

  /**
   * Returns a set of all supported resource types.
   *
   * @return Set of all supported resource types.
   */
  @VisibleForTesting
  Set<ResourceTypeV2<ClaimResponse>> getResourceTypes() {
    return Sets.newHashSet(ClaimResponseTypeV2.values());
  }
}
