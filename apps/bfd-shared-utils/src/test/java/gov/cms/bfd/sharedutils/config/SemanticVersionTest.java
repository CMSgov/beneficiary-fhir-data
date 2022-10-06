package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SemanticVersion}. */
public class SemanticVersionTest {
  /** Verifies that parsing invalid strings returns empty value. */
  @Test
  public void parsingInvalidStringsShouldReturnEmpty() {
    assertEquals(Optional.empty(), SemanticVersion.parse(""));
    assertEquals(Optional.empty(), SemanticVersion.parse("what?"));
    assertEquals(Optional.empty(), SemanticVersion.parse("a1"));
    assertEquals(Optional.empty(), SemanticVersion.parse("1,2,3"));
    assertEquals(Optional.empty(), SemanticVersion.parse("1.2.3-a"));
    assertEquals(Optional.empty(), SemanticVersion.parse("-1.2.3"));
    assertEquals(Optional.empty(), SemanticVersion.parse("1.2.-3"));
  }

  /** Verifies that parsing valid strings returns a version. */
  @Test
  public void parsingValidStringsShouldReturnCorrectVersions() {
    assertEquals(Optional.of(new SemanticVersion(1, 0, 0)), SemanticVersion.parse("1"));
    assertEquals(Optional.of(new SemanticVersion(1, 2, 0)), SemanticVersion.parse("1.2"));
    assertEquals(Optional.of(new SemanticVersion(1, 2, 3)), SemanticVersion.parse("1.2.3"));
    assertEquals(
        Optional.of(new SemanticVersion(12, 345, 6789)), SemanticVersion.parse("12.345.6789"));
  }

  /** Verifies that comparisons are performed in ascending order. */
  @Test
  public void compareShouldSortInAscendingOrder() {
    final var v0_0_9 = new SemanticVersion(0, 0, 9);
    final var v0_8_0 = new SemanticVersion(0, 8, 0);
    final var v5_0_0 = new SemanticVersion(5, 0, 0);
    assertEquals(1, v0_8_0.compareTo(v0_0_9));
    assertEquals(-1, v0_0_9.compareTo(v0_8_0));

    assertEquals(-1, v0_8_0.compareTo(v5_0_0));
    assertEquals(1, v5_0_0.compareTo(v0_8_0));

    assertEquals(0, v5_0_0.compareTo(new SemanticVersion(5, 0, 0)));
  }

  /** Verifies that {@link SemanticVersion#isValid} works as expected. */
  @Test
  public void invalidVersionsShouldBeRecognized() {
    assertFalse(new SemanticVersion(0, 0, 0).isValid());
    assertFalse(new SemanticVersion(-1, 0, 0).isValid());
    assertFalse(new SemanticVersion(0, -1, 0).isValid());
    assertFalse(new SemanticVersion(0, 0, -1).isValid());

    assertTrue(new SemanticVersion(1, 0, 0).isValid());
    assertTrue(new SemanticVersion(0, 1, 0).isValid());
    assertTrue(new SemanticVersion(0, 0, 1).isValid());
  }
}
