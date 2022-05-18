package gov.cms.bfd.model.rda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link StringListConverter}. */
public class StringListConverterTest {
  private final StringListConverter converter = new StringListConverter();

  /** Verify that lists are serialized correctly. */
  @Test
  public void shouldSerializeToJsonArrayString() {
    assertEquals("[ ]", converter.convertToDatabaseColumn(StringList.of()));
    assertEquals("[ \"a\", \"b\" ]", converter.convertToDatabaseColumn(StringList.of("a", "b")));
  }

  /** Verify that strings are deserialized correctly. */
  @Test
  public void shouldDeserializeJsonArrayStrings() {
    assertEquals(StringList.of(), converter.convertToEntityAttribute(null));
    assertEquals(StringList.of(), converter.convertToEntityAttribute("[]"));
    assertEquals(StringList.of("a"), converter.convertToEntityAttribute("[\"a\"]"));
  }

  /** Verify that null and empty strings are included when serializing. */
  @Test
  public void shouldIncludeEmptyStringsWhenSerialized() {
    assertEquals(
        StringList.of("a", null, "b"), converter.convertToEntityAttribute("[\"a\",null,\"b\"]"));
    assertEquals(
        StringList.of("a", "", "b"), converter.convertToEntityAttribute("[\"a\",\"\",\"b\"]"));
  }
}
