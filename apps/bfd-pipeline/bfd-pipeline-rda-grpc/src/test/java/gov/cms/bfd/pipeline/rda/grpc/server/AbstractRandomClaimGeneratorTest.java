package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Tests the functionality of the {@link AbstractRandomClaimGenerator} class. */
class AbstractRandomClaimGeneratorTest {

  /** The fully developed set of random data that could potentially be produced. */
  private static final Map<String, Object> expected =
      Map.of(
          "randomInt", 32,
          "randomNumericString", "544365311023",
          "randomAlphaNumericString", "fh3kxnp73k4s",
          "randomEnum", TestGenerator.MyEnums.ONE,
          "randomCharacter", 'd',
          "randomAlphaString", "ddpwfcrncqsb",
          "randomDate", "2021-05-05",
          "randomAmount", "411.57");

  /**
   * Check that for any given configuration for a generated claim, the generated claims follow a
   * stable random value generation pattern.
   *
   * <p>The test checks that altering the min/max values for a string just changes the length, and
   * not the values themselves (aside from obvious truncation)
   *
   * <p>This test also checks that setting a value using always(), optional(), or even not setting
   * the field at all also has no affect on the other generated field values
   */
  @Test
  void changingAttributesTest() {
    Clock clock = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);

    // Try all the different field combinations
    for (int fieldMask = Byte.MIN_VALUE; fieldMask <= Byte.MAX_VALUE; ++fieldMask) {
      // Try always(), optional(), and even skipping the field entirely
      for (TestGenerator.TestingState testingState : TestGenerator.TestingState.values()) {
        TestGenerator generator = new TestGenerator(5L, false, clock);

        Map<String, Object> randomData =
            generator.generateRandomMap((byte) fieldMask, testingState, fieldMask % 12, 12, 33);

        assertSubsetOf(expected, randomData);
      }
    }

    // Try again with optional overridden and max string lengths
    TestGenerator generator = new TestGenerator(5L, true, clock);

    Map<String, Object> randomData =
        generator.generateRandomMap((byte) 0, TestGenerator.TestingState.ALWAYS, 12, 12, 33);

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

  /** Special derived class for testing purposes. */
  private static class TestGenerator extends AbstractRandomClaimGenerator<Map<String, Object>> {

    /** A mask that dictates which fields are selected for altered testing. */
    private byte fieldMask;
    /** Determines how often a field is set when randomly generated. */
    private TestingState testingState;
    /** The minimum length of generated string values. */
    private int min;
    /** The maximum length of generated string values. */
    private int max;
    /** The maximum value for randomly generated integers. */
    private int maxInt;

    /** Test enum for random enum selection. */
    private enum MyEnums {
      /** Sample one. */
      ONE,
      /** Sample two. */
      TWO,
      /** Sample three. */
      THREE,
      /** Sample four. */
      FOUR
    }

    /** Helper enum for executing various testing states. */
    private enum TestingState {
      /** Always adds to the field set. */
      ALWAYS,
      /** Field is optional in the set. */
      OPTIONAL,
      /** Do not add to the field set. */
      MISSING
    }

    /**
     * Instantiates a new test generator.
     *
     * @param seed the seed
     * @param optionalOverride the optional override
     * @param clock the clock for timestamps
     */
    TestGenerator(long seed, boolean optionalOverride, Clock clock) {
      super(
          RandomClaimGeneratorConfig.builder()
              .seed(seed)
              .optionalOverride(optionalOverride)
              .clock(clock)
              .build());
    }

    /**
     * Generates the random test data to use in testing.
     *
     * @param fieldMask A mask that dictates which fields are selected for altered testing
     * @param testingState the testing state
     * @param min The minimum length of generated string values
     * @param max The maximum length of generated string values
     * @param maxInt The maximum value for randomly generated integers
     * @return A {@link Map} of randomly generated data
     */
    public Map<String, Object> generateRandomMap(
        byte fieldMask, TestingState testingState, int min, int max, int maxInt) {
      this.fieldMask = fieldMask;
      this.testingState = testingState;
      this.min = min;
      this.max = max;
      this.maxInt = maxInt;

      setSequence(0);
      return createRandomClaim();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> createRandomClaim() {
      Map<String, Object> randomData = new HashMap<>();

      FieldSet[] fieldSets = {
        new FieldSet("randomAlphaNumericString", () -> randomAlphaNumeric(min, max)),
        new FieldSet("randomAlphaString", () -> randomLetter(min, max)),
        new FieldSet("randomNumericString", () -> randomDigit(min, max)),
        new FieldSet("randomCharacter", () -> randomChar("abcdef")),
        new FieldSet("randomEnum", () -> randomEnum(enumValues(MyEnums.values()))),
        new FieldSet("randomAmount", this::randomAmount),
        new FieldSet("randomDate", this::randomDate),
        new FieldSet("randomInt", () -> randomInt(maxInt))
      };

      // Based on the mask, we will alter the testing state of certain fields
      for (int i = 0; i < fieldSets.length; i++) {
        FieldSet fieldSet = fieldSets[i];

        // If this is a field that should have an altered testing state
        if (((fieldMask >> i) & 1) == 1) {
          // Check how the field should be tested
          switch (testingState) {
            case ALWAYS:
              always(
                  fieldSet.propertyName,
                  () -> randomData.put(fieldSet.propertyName, fieldSet.valueSupplier.get()));
              break;
            case OPTIONAL:
              optional(
                  fieldSet.propertyName,
                  () -> randomData.put(fieldSet.propertyName, fieldSet.valueSupplier.get()));
              break;
            case MISSING:
            default:
              // Do not add the field at all.
              break;
          }
        } else {
          // Or if this is not a field marked by the mask, just test it normally
          always(
              fieldSet.propertyName,
              () -> randomData.put(fieldSet.propertyName, fieldSet.valueSupplier.get()));
        }
      }

      return randomData;
    }
  }

  /** Helper class for storing different test field logic and their associated property name. */
  private static class FieldSet {
    /** The name of the field being set. */
    private final String propertyName;
    /** The logic used to supply the field value. */
    private final Supplier<Object> valueSupplier;

    /**
     * Instantiates a new Field set.
     *
     * @param propertyName the property name
     * @param valueSupplier the value supplier
     */
    private FieldSet(String propertyName, Supplier<Object> valueSupplier) {
      this.propertyName = propertyName;
      this.valueSupplier = valueSupplier;
    }
  }
}
