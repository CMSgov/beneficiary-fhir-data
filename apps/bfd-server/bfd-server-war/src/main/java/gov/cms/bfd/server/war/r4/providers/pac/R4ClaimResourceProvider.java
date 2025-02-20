package gov.cms.bfd.server.war.r4.providers.pac;

import static gov.cms.bfd.server.war.SpringConfiguration.PAC_OLD_MBI_HASH_ENABLED;
import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_PAC_CLAIM_SOURCE_TYPES;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.server.war.V2SamhsaConsentSimulation;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link Claim} resources. */
@Component
public class R4ClaimResourceProvider extends AbstractR4ResourceProvider<Claim> {

  /** The map of claim types. */
  private static final Map<String, ResourceTypeV2<Claim, ?>> claimTypeMap =
      ImmutableMap.of("fiss", ClaimTypeV2.F, "mcs", ClaimTypeV2.M);

  /**
   * Instantiates a new R4 claim response provider. This should be called by spring during component
   * initialization.
   *
   * @param metricRegistry the metric registry bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param oldMbiHashEnabled true if old MBI hash should be used
   * @param fissClaimTransformerV2 is the fiss claim transformer
   * @param mcsClaimTransformerV2 is the mcs claim transformer
   * @param claimSourceTypeNames determines the type of claim sources to enable for constructing PAC
   * @param v2SamhsaConsentSimulation v2SamhsaConsentSimulation resources ({@link
   *     org.hl7.fhir.r4.model.Claim} / {@link org.hl7.fhir.r4.model.ClaimResponse}
   */
  public R4ClaimResourceProvider(
      MetricRegistry metricRegistry,
      R4ClaimSamhsaMatcher samhsaMatcher,
      @Qualifier(PAC_OLD_MBI_HASH_ENABLED) Boolean oldMbiHashEnabled,
      FissClaimTransformerV2 fissClaimTransformerV2,
      McsClaimTransformerV2 mcsClaimTransformerV2,
      @Value("${" + SSM_PATH_PAC_CLAIM_SOURCE_TYPES + ":}") String claimSourceTypeNames,
      V2SamhsaConsentSimulation v2SamhsaConsentSimulation) {
    super(
        metricRegistry,
        samhsaMatcher,
        oldMbiHashEnabled,
        fissClaimTransformerV2,
        mcsClaimTransformerV2,
        claimSourceTypeNames,
        v2SamhsaConsentSimulation);
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
  Optional<ResourceTypeV2<Claim, ?>> parseClaimType(String typeText) {
    return ClaimTypeV2.parse(typeText);
  }

  /** {@inheritDoc} */
  @VisibleForTesting
  @Override
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
