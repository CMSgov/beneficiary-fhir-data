package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.server.war.r4.providers.pac.PhaseResourceFilter;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceFilter;
import org.junit.jupiter.api.Test;

/** Tests for factory methods in {@link SpringConfiguration}. */
public class SpringConfigurationTest {
  /**
   * Verifies the edge cases and normal case for creation of {@link ResourceFilter}. Missing or
   * empty property creates filter that retains everything. Invalid version range string creates
   * filter that retains nothing. Valid version range creates filter that enforces that range.
   */
  @Test
  public void pacResourceFilterShouldCreateResourceFilter() {
    var config = new SpringConfiguration();

    // handles edge cases properly
    assertSame(ResourceFilter.RetainEverything, config.pacResourceFilter(null));
    assertSame(ResourceFilter.RetainEverything, config.pacResourceFilter(""));
    assertSame(ResourceFilter.RetainNothing, config.pacResourceFilter("invalid"));

    // enforces valid range properly
    var filter = config.pacResourceFilter("[1.2,)");
    assertInstanceOf(PhaseResourceFilter.class, filter);

    var claim = new RdaFissClaim();
    assertFalse(filter.shouldRetain(claim));
    claim.setPhase((short) 1);
    claim.setPhaseSeqNum((short) 1);
    assertFalse(filter.shouldRetain(claim));
    claim.setPhaseSeqNum((short) 2);
    assertTrue(filter.shouldRetain(claim));
  }
}
