package gov.cms.bfd.server.war.r4.providers.pac;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link Claim} resources. */
@Component
public class R4ClaimResourceProvider extends AbstractR4ResourceProvider<Claim> {

  private static final Map<String, ResourceTypeV2<Claim, ?>> claimTypeMap =
      ImmutableMap.of("fiss", ClaimTypeV2.F, "mcs", ClaimTypeV2.M);

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Optional<ResourceTypeV2<Claim, ?>> parseClaimType(String typeText) {
    return ClaimTypeV2.parse(typeText);
  }

  /**
   * Returns a set of all supported resource types.
   *
   * @return Set of all supported resource types.
   */
  @VisibleForTesting
  Set<ResourceTypeV2<Claim, ?>> getResourceTypes() {
    return Sets.newHashSet(ClaimTypeV2.values());
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Map<String, ResourceTypeV2<Claim, ?>> getResourceTypeMap() {
    return claimTypeMap;
  }
}
