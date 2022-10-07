package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimDao;
import gov.cms.bfd.sharedutils.config.SemanticVersionRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for @link {@link R4ClaimResourceProvider}. */
public class R4ClaimResourceProviderTest {

  /** Verifies expected class hierarchy. */
  @Test
  public void shouldExtendAbstractR4ResourceProvider() {
    assertTrue(AbstractR4ResourceProvider.class.isAssignableFrom(R4ClaimResourceProvider.class));
  }

  /** Verifies that ResourceFilter is applied to every claim. */
  @Test
  public void shouldApplyResourceFilter() {
    final var claimDao = mock(ClaimDao.class);
    final var metricRegistry = new MetricRegistry();
    final var paging = mock(OffsetLinkBuilder.class);
    final var filter =
        spy(SemanticVersionRange.parse("[1.2,2.0)").map(PhaseResourceFilter::new).orElseThrow());
    final var provider = new R4ClaimResourceProvider();
    provider.setClaimDao(claimDao);
    provider.setMetricRegistry(metricRegistry);
    provider.setResourceFilter(filter);

    final var claim1 = RdaFissClaim.builder().phase((short) 1).lastUpdated(Instant.now()).build();
    final var claim2 =
        RdaFissClaim.builder()
            .phase((short) 1)
            .phaseSeqNum((short) 2)
            .lastUpdated(Instant.now())
            .build();
    final var claim3 =
        RdaFissClaim.builder()
            .phase((short) 1)
            .phaseSeqNum((short) 10)
            .lastUpdated(Instant.now())
            .build();
    doReturn(List.of(claim1, claim2, claim3))
        .when(claimDao)
        .findAllByMbiAttribute(any(), any(), anyBoolean(), any(), any());
    doReturn(false).when(paging).isPagingRequested();

    provider.createBundleFor(
        Set.of(ClaimTypeV2.F),
        "mbi",
        true,
        false,
        new DateRangeParam(),
        new DateRangeParam(),
        paging);
    verify(filter, times(3)).shouldRetain(any(RdaFissClaim.class));
  }
}
