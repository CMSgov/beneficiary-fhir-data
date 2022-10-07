package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.sharedutils.config.SemanticVersionRange;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PhaseResourceFilter}. */
public class PhaseResourceFilterTest {
  private final PhaseResourceFilter filter =
      SemanticVersionRange.parse("[1.2,2.0)").map(PhaseResourceFilter::new).orElseThrow();

  /** Verify unsupported objects are retained. */
  @Test
  public void shouldRetainUnsupportedObjects() {
    assertTrue(filter.shouldRetain(null));
    assertTrue(filter.shouldRetain("just a string"));
  }

  /** Verify range restrictions are enforced. */
  @Test
  public void shouldEnforceRangeRestriction() {
    var claim = new RdaMcsClaim();

    // nulls become zeros and 0.0 is below our minimum
    assertFalse(filter.shouldRetain(claim));

    // 1.1 is below our minimum
    claim.setPhase((short) 1);
    claim.setPhaseSeqNum((short) 1);
    assertFalse(filter.shouldRetain(claim));

    // 1.2 is at our minimum
    claim.setPhaseSeqNum((short) 2);
    assertTrue(filter.shouldRetain(claim));

    // 1.9 is within our range
    claim.setPhaseSeqNum((short) 9);
    assertTrue(filter.shouldRetain(claim));

    // 2.0 is above our maximum since we used exclusive bound
    claim.setPhase((short) 2);
    claim.setPhaseSeqNum((short) 0);
    assertFalse(filter.shouldRetain(claim));
  }
}
