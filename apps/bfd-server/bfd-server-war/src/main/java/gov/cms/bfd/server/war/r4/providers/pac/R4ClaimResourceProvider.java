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

  /** The map of claim types. */
  private static final Map<String, ResourceTypeV2<Claim, ?>> claimTypeMap =
      ImmutableMap.of("fiss", ClaimTypeV2.F, "mcs", ClaimTypeV2.M);

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Optional<ResourceTypeV2<Claim, ?>> parseClaimType(String typeText) {
    return ClaimTypeV2.parse(typeText);
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  Set<ResourceTypeV2<Claim, ?>> getDefinedResourceTypes() {
    return Sets.newHashSet(ClaimTypeV2.values());
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Map<String, ResourceTypeV2<Claim, ?>> getResourceTypeMap() {
    return claimTypeMap;
  }
}
