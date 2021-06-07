package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import org.hl7.fhir.r4.model.Claim;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
public class McsClaimTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimTransformerV2.class.getSimpleName(), "transform");

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  static Claim transform(MetricRegistry metricRegistry, Object claimEntity) {
    // TODO: Update this check when entity available
    if (!(claimEntity instanceof Object)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((Object) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified {@link PreAdjMcsClaim}
   */
  private static Claim transformClaim(Object claimGroup) {
    Claim claim = new Claim();

    // TODO: Transform claim

    return claim;
  }
}
