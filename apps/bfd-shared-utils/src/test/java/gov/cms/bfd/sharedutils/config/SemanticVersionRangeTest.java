package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link SemanticVersionRange}. */
public class SemanticVersionRangeTest {
  /** Test various invalid range strings return empty value. */
  @Test
  public void invalidRangesReturnEmptyWhenParsed() {
    assertTrue(SemanticVersionRange.parse("a").isEmpty());
    assertTrue(SemanticVersionRange.parse("a1").isEmpty());

    assertTrue(SemanticVersionRange.parse("1,2").isEmpty());
    assertTrue(SemanticVersionRange.parse("[-3,2)").isEmpty());
    assertTrue(SemanticVersionRange.parse("(1,2,3]").isEmpty());
  }

  /** Test range containing just a version matches just that version. */
  @Test
  public void singleVersionMatchesThatVersionOnly() {
    final var range = SemanticVersionRange.parse("1.2.3").orElse(null);
    assertNotNull(range);

    assertTrue(range.contains(new SemanticVersion(1, 2, 3)));
    assertFalse(range.contains(new SemanticVersion(1, 2, 2)));
    assertFalse(range.contains(new SemanticVersion(1, 2, 4)));
  }

  /** Test inclusive bounds are contained within the range. */
  @Test
  public void inclusiveBoundsAcceptBoundaryValue() {
    final var range = SemanticVersionRange.parse("[1.2.3,4.5.6]").orElse(null);
    assertNotNull(range);

    assertFalse(range.contains(new SemanticVersion(1, 2, 2)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 3)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 4)));

    assertTrue(range.contains(new SemanticVersion(4, 5, 5)));
    assertTrue(range.contains(new SemanticVersion(4, 5, 6)));
    assertFalse(range.contains(new SemanticVersion(4, 5, 7)));
  }

  /** Test exclusive bounds are not contained within the range. */
  @Test
  public void exclusiveBoundsRejectBoundaryValue() {
    final var range = SemanticVersionRange.parse("(1.2.3,4.5.6)").orElse(null);
    assertNotNull(range);

    assertFalse(range.contains(new SemanticVersion(1, 2, 2)));
    assertFalse(range.contains(new SemanticVersion(1, 2, 3)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 4)));

    assertTrue(range.contains(new SemanticVersion(4, 5, 5)));
    assertFalse(range.contains(new SemanticVersion(4, 5, 6)));
    assertFalse(range.contains(new SemanticVersion(4, 5, 7)));
  }

  /** Test empty version strings match any version. */
  @Test
  public void emptyBoundsAcceptAnything() {
    var range = SemanticVersionRange.parse("(,4.5.6)").orElse(null);
    assertNotNull(range);

    assertTrue(range.contains(new SemanticVersion(1, 2, 2)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 3)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 4)));

    assertTrue(range.contains(new SemanticVersion(4, 5, 5)));
    assertFalse(range.contains(new SemanticVersion(4, 5, 6)));
    assertFalse(range.contains(new SemanticVersion(4, 5, 7)));

    range = SemanticVersionRange.parse("(1.2.3,)").orElse(null);
    assertNotNull(range);

    assertFalse(range.contains(new SemanticVersion(1, 2, 2)));
    assertFalse(range.contains(new SemanticVersion(1, 2, 3)));
    assertTrue(range.contains(new SemanticVersion(1, 2, 4)));

    assertTrue(range.contains(new SemanticVersion(4, 5, 5)));
    assertTrue(range.contains(new SemanticVersion(4, 5, 6)));
    assertTrue(range.contains(new SemanticVersion(4, 5, 7)));
  }

  /** Simple test to verify that {@link SemanticVersionRange#AcceptAll} does accept versions. */
  @Test
  public void constantAcceptAllShouldAcceptAll() {
    assertTrue(SemanticVersionRange.AcceptAll.contains(new SemanticVersion(1, 0, 0)));
    assertTrue(SemanticVersionRange.AcceptAll.contains(new SemanticVersion(12, 20, 44)));
  }
}
