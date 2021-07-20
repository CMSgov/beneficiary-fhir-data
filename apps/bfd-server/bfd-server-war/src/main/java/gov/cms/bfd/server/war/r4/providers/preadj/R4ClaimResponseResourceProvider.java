package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link ClaimResponse} resources. */
@Component
public class R4ClaimResponseResourceProvider extends AbstractR4ResourceProvider<ClaimResponse> {

  private static final Map<String, ResourceTypeV2<ClaimResponse>> claimTypeMap =
      ImmutableMap.of("fiss", ClaimResponseTypeV2.F, "mcs", ClaimResponseTypeV2.M);

  /**
   * Claim type parsing for {@link ClaimResponse} resources.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed claim type.
   */
  @Override
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

  /**
   * Returns implementation specific {@link ResourceTypeV2} map.
   *
   * @return The implementation specific {@link ResourceTypeV2} map.
   */
  @VisibleForTesting
  @Override
  Map<String, ResourceTypeV2<ClaimResponse>> getResourceTypeMap() {
    return claimTypeMap;
  }
}
