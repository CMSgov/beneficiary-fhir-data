package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import org.hl7.fhir.r4.model.Claim;

/** Transforms Fiss/MCS instances into FHIR {@link Claim} resources. */
public class PreAdjMcsClaimTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(PreAdjMcsClaimTransformerV2.class.getSimpleName(), "transform");

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  static Claim transform(MetricRegistry metricRegistry, Object claimEntity) {
    Timer.Context timer = metricRegistry.timer(METRIC_NAME).time();

    // TODO: Update this check when entity available
    if (!(claimEntity instanceof Object)) {
      throw new BadCodeMonkeyException();
    }

    // TODO: Cast to specific claim entity type
    Claim claim = transformClaim((Object) claimEntity);

    timer.stop();
    return claim;
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
