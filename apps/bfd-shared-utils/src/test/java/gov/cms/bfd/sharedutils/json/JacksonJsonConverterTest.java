package gov.cms.bfd.sharedutils.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.junit.jupiter.api.Test;

/** Unit test for {@link JacksonJsonConverter}. */
public class JacksonJsonConverterTest {
  /** Test round-trip conversion using the pretty printing converter instance. */
  @Test
  void testPrettyPrinting() {
    final var converter = JacksonJsonConverter.prettyInstance();
    final var sample = new SampleBean(1, "hello", List.of("world!"));
    final var json = converter.objectToJson(sample);
    assertEquals(
"""
{
  "anInt" : 1,
  "manyStrings" : [ "world!" ],
  "oneString" : "hello"
}""",
        json);
    assertEquals(sample, converter.jsonToObject(json, SampleBean.class));
  }

  /** Test round-trip conversion using the minimal json converter instance. */
  @Test
  void testMinimal() {
    final var converter = JacksonJsonConverter.minimalInstance();
    final var sample = new SampleBean(1, "hello", List.of("world!"));
    final var json = converter.objectToJson(sample);
    assertEquals("{\"anInt\":1,\"manyStrings\":[\"world!\"],\"oneString\":\"hello\"}", json);
    assertEquals(sample, converter.jsonToObject(json, SampleBean.class));
  }

  /** Sample bean for testing JSON round trip. */
  @Getter
  @EqualsAndHashCode
  static class SampleBean {
    /** An integer field. */
    private final int anInt;

    /** A string field. */
    private final String oneString;

    /** A list field. */
    private final List<String> manyStrings;

    /**
     * Constructor for use by Jackson.
     *
     * @param anInt value for field with same name
     * @param oneString value for field with same name
     * @param manyStrings value for field with same name
     */
    public SampleBean(
        @JsonProperty("anInt") int anInt,
        @JsonProperty("oneString") String oneString,
        @JsonProperty("manyStrings") List<String> manyStrings) {
      this.anInt = anInt;
      this.oneString = oneString;
      this.manyStrings = manyStrings;
    }
  }
}
