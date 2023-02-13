package gov.cms.bfd.server.war.r4.providers.pac;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link ClaimResponse} resources. */
@Component
public class R4ClaimResponseResourceProvider extends AbstractR4ResourceProvider<ClaimResponse> {

  /** The map of claim types. */
  private static final Map<String, ResourceTypeV2<ClaimResponse, ?>> claimTypeMap =
      ImmutableMap.of("fiss", ClaimResponseTypeV2.F, "mcs", ClaimResponseTypeV2.M);

  /** {@inheritDoc} */
  @Override
  Optional<ResourceTypeV2<ClaimResponse, ?>> parseClaimType(String typeText) {
    return ClaimResponseTypeV2.parse(typeText);
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  Set<ResourceTypeV2<ClaimResponse, ?>> getDefinedResourceTypes() {
    return Sets.newHashSet(ClaimResponseTypeV2.values());
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Map<String, ResourceTypeV2<ClaimResponse, ?>> getResourceTypeMap() {
    return claimTypeMap;
  }
}
