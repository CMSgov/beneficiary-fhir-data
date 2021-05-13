package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import org.hl7.fhir.r4.model.ClaimResponse;

/** Transforms Fiss/MCS instances into FHIR {@link ClaimResponse} resources. */
public class PreAdjMcsClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(PreAdjMcsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    Timer.Context timer = metricRegistry.timer(METRIC_NAME).time();

    // TODO: Update this check when entity available
    if (!(claimEntity instanceof Object)) {
      throw new BadCodeMonkeyException();
    }

    // TODO: Cast to specific claim entity type
    ClaimResponse claim = transformClaim((Object) claimEntity);

    timer.stop();
    return claim;
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjMcsClaim}
   */
  private static ClaimResponse transformClaim(Object claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    // TODO: Transform claim

    return claim;
  }
}
