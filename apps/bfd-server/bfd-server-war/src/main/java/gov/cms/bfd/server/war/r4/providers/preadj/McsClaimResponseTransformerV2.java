package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import org.hl7.fhir.r4.model.ClaimResponse;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class McsClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    // TODO: [DCGEO-88, DCGEO-98] Update this check when entity available
    if (!(claimEntity instanceof Object)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((Object) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjMcsClaim}
   */
  private static ClaimResponse transformClaim(Object claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    // TODO: [DCGEO-98] Transform claim

    return claim;
  }
}
