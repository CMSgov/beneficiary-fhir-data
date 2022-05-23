package gov.cms.bfd.model.rda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link StringList}. */
public class StringListTest {
  /** Verify that the filtered add methods ignore nulls and empty strings. */
  @Test
  public void shouldFilterOutNullsAndEmpties() {
    final var empty = StringList.of();
    var list = StringList.ofNonEmpty();
    assertEquals(empty, list.addIfNonEmpty(null));
    assertEquals(empty, list.addIfNonEmpty(""));
    assertEquals(empty, StringList.ofNonEmpty("", null));
  }

  /** Verify that the filtered add methods add non-empty strings to the list. */
  @Test
  public void shouldAddNonEmptyStrings() {
    var list = StringList.ofNonEmpty();
    assertEquals(StringList.of("y"), list.addIfNonEmpty("y"));
  }

  /** Verify that the unfiltered add methods will add anything. */
  @Test
  public void shouldAddValues() {
    var list = StringList.ofNonEmpty();
    assertEquals(StringList.of("x", "y", "", null), list.add('x').add("y").add("").add(null));
  }
}
