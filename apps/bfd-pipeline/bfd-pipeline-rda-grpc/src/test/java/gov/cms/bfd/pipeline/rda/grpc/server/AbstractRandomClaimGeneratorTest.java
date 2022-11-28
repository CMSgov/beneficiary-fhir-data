package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractRandomClaimGeneratorTest {

  /** The fully developed set of random data that could potentially be produced. */
  private static final Map<String, Object> expected =
      Map.of(
          "randomInt", 21,
          "randomNumericString", "645716924393",
          "randomAlphaNumericString", "3j69rwcr5khj",
          "randomEnum", TestGenerator.MyEnums.TWO,
          "randomCharacter", 'e',
          "randomAlphaString", "sfrzcdxbhvcp",
          "randomDate", "2021-05-22",
          "randomAmount", "16.17");

  /**
   * Check that for any given min/max/optional settings, the randomly generated data remains stable,
   * producing the same data each time (with some variation in value length due to min/max)
   *
   * <p>Cycles through using {@link AbstractRandomClaimGenerator#optional(Runnable)} for different
   * actions in different combinations to ensure data consistency.
   */
  @Test
  void changingAttributesTest() {
    Clock clock = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);

    // Try all the different optional combinations
    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
      TestGenerator generator = new TestGenerator(5L, false, clock);

      Map<String, Object> randomData = generator.generateRandomMap((byte) i, i % 12, 12, 33);

      assertSubsetOf(expected, randomData);
    }

    // Try again with optional overridden and max string lengths
    TestGenerator generator = new TestGenerator(5L, true, clock);

    Map<String, Object> randomData = generator.generateRandomMap((byte) 0, 12, 12, 33);

    assertSubsetOf(expected, randomData);
  }

  /**
   * Tests that the given subset data is all included within the given full set.
   *
   * <p>In cases such as a variable length string, the subset data may contain a string that is
   * shorter than the full set one. As long as the shorter string matches the beginning of the
   * longer one, it's correct.
   *
   * @param fullSet The full set of potential data values (at maximum length)
   * @param subset The generated data to test for at least a partial match
   */
  private void assertSubsetOf(Map<String, Object> fullSet, Map<String, Object> subset) {
    for (Map.Entry<String, Object> entry : subset.entrySet()) {
      // First make sure the key is valid
      assertTrue(
          fullSet.containsKey(entry.getKey()),
          String.format("Subset contains invalid key: %s", entry.getKey()));

      Object expectedValue = expected.get(entry.getKey());

      // Check if the value for that attribute matches
      final String valueCanonicalName = entry.getValue().getClass().getCanonicalName();

      switch (valueCanonicalName) {
        case "java.lang.String":
          // For strings, as long as the full set value starts with the entire subset value, it's
          // good.
          String expectedString = (String) expectedValue;
          String actualString = (String) entry.getValue();

          if (expectedString.length() == actualString.length()) {
            assertEquals(expectedString, actualString);
          } else {
            assertTrue(
                expectedString.startsWith(actualString),
                String.format("'%s' does not start with '%s'", expectedString, actualString));
          }
          break;
        case "java.lang.Integer":
        case "java.lang.Character":
        case "gov.cms.bfd.pipeline.rda.grpc.server.AbstractRandomClaimGeneratorTest.TestGenerator.MyEnums":
          // Other supported types can just be a direct match
          assertEquals(expectedValue, entry.getValue());
          break;
        default:
          fail(String.format("Unexpected value type: %s", valueCanonicalName));
      }
    }
  }

  /** Special derived class for testing purposes */
  private static class TestGenerator extends AbstractRandomClaimGenerator {

    private enum MyEnums {
      ONE,
      TWO,
      THREE,
      FOUR
    }

    TestGenerator(long seed, boolean optionalOverride, Clock clock) {
      super(seed, optionalOverride, clock);
    }

    /**
     * Generates the random test data to use in testing.
     *
     * @param min The minimum length of generated string values.
     * @param max The maximum length of generated string values.
     * @param maxInt The maximum value for randomly generated integers.
     * @return A {@link Map} of randomly generated data.
     */
    public Map<String, Object> generateRandomMap(byte optionalMask, int min, int max, int maxInt) {
      Map<String, Object> randomData = new HashMap<>();

      Runnable[] actions = {
        () -> randomData.put("randomAlphaNumericString", randomAlphaNumeric(min, max)),
        () -> randomData.put("randomAlphaString", randomLetter(min, max)),
        () -> randomData.put("randomNumericString", randomDigit(min, max)),
        () -> randomData.put("randomCharacter", randomChar("abcdef")),
        () -> randomData.put("randomEnum", randomEnum(enumValues(MyEnums.values()))),
        () -> randomData.put("randomAmount", randomAmount()),
        () -> randomData.put("randomDate", randomDate()),
        () -> randomData.put("randomInt", randomInt(maxInt))
      };

      // Apply the optional() logic based on the given mask
      for (int i = 0; i < actions.length; i++) {
        Runnable action = actions[i];

        if (((optionalMask >> i) & 1) == 1) {
          optional(action);
        } else {
          action.run();
        }
      }

      return randomData;
    }
  }
}
