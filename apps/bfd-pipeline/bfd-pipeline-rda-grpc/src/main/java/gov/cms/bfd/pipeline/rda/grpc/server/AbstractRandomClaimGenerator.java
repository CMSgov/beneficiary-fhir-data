package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * The base class provides common functionality used by the FISS and MCS claim generators to create
 * random claim data.
 *
 * <p>To keep the randomly generated values stable, each field should be set using it's own path,
 * defined by the propertyName of {@link #always(String, Runnable)}, {@link #optional(String,
 * Runnable)}, {@link #oneOf(String, Runnable...)}, and {@link #optionalOneOf(String, Runnable...)}.
 * The {@link Random} object is specific to each field path and is seeded using the {@link
 * RandomClaimGeneratorConfig#seed}, {@link #sequence}, and currently stored {@link #path}.
 *
 * @param <T> The type of claim this generate generates.
 */
abstract class AbstractRandomClaimGenerator<T> {
  /**
   * Characters of the alphabet (left out vowels to avoid real word generation, and ambiguous
   * letters like 'l').
   */
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  /** Numbers, zero through nine. */
  private static final String DIGIT = "1234567890";
  /** Aggregated sequence of letters and numbers. */
  private static final String ALNUM = ALPHA + DIGIT;
  /** The maximum number of days in the past that a random date value can be generated for. */
  private static final int MAX_DAYS_AGO = 180;

  /** Our configuration settings. */
  private final RandomClaimGeneratorConfig config;

  /** The sequence number of the generated claim, which regulates randomness between claims. */
  private int sequence;

  /**
   * A path that will be used to randomly generate values.
   *
   * <p>The path should be updated for each value being generated, ensuring each field has a
   * different random, but stable, value. The path is used along with the {@link
   * RandomClaimGeneratorConfig#seed} and {@link #sequence} to seed the {@link Random} object used
   * to generate field values.
   */
  private final Stack<String> path;

  /**
   * Constructs an instance.
   *
   * @param config configuration settings
   */
  AbstractRandomClaimGenerator(RandomClaimGeneratorConfig config) {
    this.config = config;
    this.sequence = 0;
    this.path = new Stack<>();
  }

  /**
   * Set the starting sequence value.
   *
   * <p>This value is used to alter the random values generated between two different claims.
   *
   * @param sequence The sequence number to start generating claims from.
   */
  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  /**
   * Generates a random claim, prefixing the {@link #path} with the current sequence number to
   * ensure uniqueness between sequential claims.
   *
   * <p>Each call to this method increments the value of {@link #sequence}, ensuring unique, random,
   * but stable, values between claims.
   *
   * @return The generated claim.
   */
  public T randomClaim() {
    // Add the sequence value to the beginning of our path
    PathLayer pathLayer = addLayer(String.format("[%d]", sequence++));
    // Create the claim
    T randomClaim = createRandomClaim();
    // Remove the sequence value when we're done
    pathLayer.remove();
    return randomClaim;
  }

  /**
   * Implementation defined logic for creating a random claim.
   *
   * @return the claim type
   */
  public abstract T createRandomClaim();

  /**
   * RDA API enums always define a special value that the protobuf API intends as a special
   * placeholder for garbled/invalid data. This field is named UNRECOGNIZED. This method is used to
   * accept all the defined enum values, skip this special value, and return an immutable list of
   * valid enums to be included when generating a random enum value.
   *
   * @param values all possible values for an enum type
   * @param <T> the actual enum type
   * @return immutable list of valid enum values
   */
  protected static <T extends Enum<T>> List<T> enumValues(T[] values) {
    return Arrays.stream(values)
        .filter(v -> !v.name().equals("UNRECOGNIZED"))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns a random integer value.
   *
   * @param maxValue The maximum value for the return integer.
   * @return The random integer value.
   */
  protected int randomInt(int maxValue) {
    return createContext().randomInteger(maxValue);
  }

  /**
   * Returns a random numeric string.
   *
   * @param minLength The minimum length of the numeric string.
   * @param maxLength The maximum length of the numeric string.
   * @return A random numeric string.
   */
  protected String randomDigit(int minLength, int maxLength) {
    return randomString(DIGIT, minLength, maxLength);
  }

  /**
   * Returns a random string of letters.
   *
   * @param minLength The minimum length of the string of letters.
   * @param maxLength The maximum length of the string of letters.
   * @return A random string of letters.
   */
  protected String randomLetter(int minLength, int maxLength) {
    return randomString(ALPHA, minLength, maxLength);
  }

  /**
   * Returns a random alphanumeric string.
   *
   * @param minLength The minimum length of the alphanumeric string.
   * @param maxLength The maximum length of the alphanumeric string.
   * @return A random alphanumeric string.
   */
  protected String randomAlphaNumeric(int minLength, int maxLength) {
    return randomString(ALNUM, minLength, maxLength);
  }

  /**
   * A random date string in YYYY-MM-DD format for a date no more than 100 days ago and no later
   * than today.
   *
   * @return random YYYY-MM-DD date string.
   */
  protected String randomDate() {
    RandomValueContext ctx = createContext();
    final LocalDate date =
        LocalDate.now(config.getClock()).minusDays(ctx.randomInteger(MAX_DAYS_AGO));
    return date.toString();
  }

  /**
   * Generates a random decimal string in the range 0.00 to 99999.99. The value will always have at
   * least one digit before the decimal point and exactly two digits after.
   *
   * @return random decimal string.
   */
  protected String randomAmount() {
    return createContext().randomAmount();
  }

  /**
   * Selects a random enum value from the list and returns it.
   *
   * @param values all possible values
   * @param <TEnum> the enum type
   * @return one of the values selected at random
   */
  protected <TEnum> TEnum randomEnum(List<TEnum> values) {
    RandomValueContext ctx = createContext();
    return values.get(ctx.randomInteger(values.size()));
  }

  /**
   * Creates a {@link RandomValueContext} using the current {@link RandomClaimGeneratorConfig#seed},
   * {@link #sequence}, and {@link #path} attributes that can be used to generate random values.
   *
   * @return The created {@link RandomValueContext}.
   */
  private RandomValueContext createContext() {
    return createContext(null);
  }

  /**
   * Creates a {@link RandomValueContext} using the current {@link RandomClaimGeneratorConfig#seed},
   * {@link #sequence}, and {@link #path} attributes that can be used to generate random values.
   *
   * @param prefix An optional (nullable) prefix that can be used for accessory random values, such
   *     as determining if an optional() value should be added.
   * @return The created {@link RandomValueContext}.
   */
  private RandomValueContext createContext(String prefix) {
    String prefixString = prefix != null && !prefix.isBlank() ? prefix + "." : "";
    String propertyPath =
        prefixString + path.stream().filter(Objects::nonNull).collect(Collectors.joining("."));
    return new RandomValueContext(config.getSeed() + sequence + propertyPath.hashCode());
  }

  /**
   * Helper method to add a new layer to the current {@link #path}, returning a {@link PathLayer}
   * object that can be used to reset the path again to just before the layer was applied.
   *
   * @param propertyName The property name to add to the path.
   * @return A {@link PathLayer} object that can be used to {@link PathLayer#remove()} the added
   *     layer.
   */
  private PathLayer addLayer(String propertyName) {
    if (propertyName != null && !propertyName.isBlank()) {
      path.push(propertyName);
      return path::pop;
    }

    return () -> {
      // Don't pop any values if we didn't add any
    };
  }

  /**
   * Adds a new layer to the {@link #path} using the propertyName, which will affect the random
   * values generated in the given action, then resets the {@link #path} to its prior state just
   * before this method was invoked.
   *
   * @param propertyName The property name to add to the {@link #path}.
   * @param action The {@link Runnable} action to execute, which should include calls methods within
   *     this class to generate random values.
   */
  protected void always(String propertyName, Runnable action) {
    PathLayer pathLayer = addLayer(propertyName);

    action.run();

    pathLayer.remove();
  }

  /**
   * Adds a new layer to the {@link #path} using the propertyName, which will affect the random
   * values generated in the given action, then resets the {@link #path} to its prior state just
   * before this method was invoked.
   *
   * @param propertyName The property name to add to the {@link #path}.
   * @param action The {@link Runnable} action to execute, which should include calls methods within
   *     this class to generate random values.
   */
  protected void optional(String propertyName, Runnable action) {
    PathLayer pathLayer = addLayer(propertyName);

    boolean shouldRun = createContext("Optional").randomBoolean();

    if (config.isOptionalOverride() || shouldRun) {
      action.run();
    }

    pathLayer.remove();
  }

  /**
   * Used when one of several possible values should be generated. Usually used for enums. The
   * possibilities are triggered with equal probability.
   *
   * <p>Adds a new layer to the {@link #path} using the propertyName, which will affect the random
   * values generated in the given action, then resets the {@link #path} to its prior state just
   * before this method was invoked.
   *
   * @param propertyName The property name to add to the {@link #path}.
   * @param actions A variable list of {@link Runnable} actions to execute, which should include
   *     calls methods within this class to generate random values.
   */
  protected void oneOf(String propertyName, Runnable... actions) {
    PathLayer pathLayer = addLayer(propertyName);

    final int index = createContext("OneOf").randomInteger(actions.length);

    actions[index].run();

    pathLayer.remove();
  }

  /**
   * Used when an optional field can have one of several possible values (usually used for enums).
   *
   * <p>Adds a new layer to the {@link #path} using the propertyName, which will affect the random
   * values generated in the given action, then resets the {@link #path} to its prior state just
   * before this method was invoked.
   *
   * <p>When a value is triggered the possibilities are selected with equal probability.
   *
   * @param propertyName The property name to add to the {@link #path}.
   * @param actions A variable list of {@link Runnable} actions to execute, which should include
   *     calls methods within this class to generate random values.
   */
  protected void optionalOneOf(String propertyName, Runnable... actions) {
    PathLayer pathLayer = addLayer(propertyName);

    boolean shouldRun = createContext("Optional").randomBoolean();
    final int index = createContext("OneOf").randomInteger(actions.length);

    if (config.isOptionalOverride() || shouldRun) {
      actions[index].run();
    }

    pathLayer.remove();
  }

  /**
   * Returns a reference to the internal {@link Clock} used by this instance.
   *
   * @return A reference to the internal {@link Clock} used by this instance.
   */
  protected Clock getClock() {
    return config.getClock();
  }

  /**
   * Returns a random character of the provided sequence.
   *
   * @param characters The character sequence to provide a random character from.
   * @return A random character from the provided sequence.
   */
  protected char randomChar(String characters) {
    return createContext().randomChar(characters);
  }

  /**
   * Returns a randomly generated string using characters from the provided sequence.
   *
   * @param characters The character sequence to use in generating the random string.
   * @param minLength The minimum length of the generated random string.
   * @param maxLength THe maximum length of the generated random string.
   * @return The generated random string.
   */
  private String randomString(String characters, int minLength, int maxLength) {
    return createContext().randomString(characters, minLength, maxLength);
  }

  /** Provides an isolated context for creating a subset of random data. */
  private static class RandomValueContext {

    /** The {@link Random} instance for this context. */
    private final Random random;

    /**
     * Instantiates the class random value based on the specified seed.
     *
     * @param seed the seed for randomization
     */
    public RandomValueContext(long seed) {
      random = new Random(seed);
    }

    /**
     * Returns a random character of the provided sequence.
     *
     * @param characters The character sequence to provide a random character from.
     * @return A random character from the provided sequence.
     */
    private char randomChar(String characters) {
      return characters.charAt(random.nextInt(characters.length()));
    }

    /**
     * Returns a randomly generated string using characters from the provided sequence.
     *
     * @param characters The character sequence to use in generating the random string.
     * @param minLength The minimum length of the generated random string.
     * @param maxLength THe maximum length of the generated random string.
     * @return The generated random string.
     */
    public String randomString(String characters, int minLength, int maxLength) {
      final StringBuilder sb = new StringBuilder();
      final int len = minLength + random.nextInt(maxLength - minLength + 1);
      while (sb.length() < len) {
        sb.append(randomChar(characters));
      }
      return sb.toString();
    }

    /**
     * Generates a random decimal string in the range 0.00 to 99999.99. The value will always have
     * at least one digit before the decimal point and exactly two digits after.
     *
     * @return random decimal string.
     */
    public String randomAmount() {
      final int dollarDigits = 1 + random.nextInt(5);
      final StringBuilder amount =
          new StringBuilder(randomString(DIGIT, dollarDigits, dollarDigits));
      while (amount.length() > 1 && amount.charAt(0) == '0') {
        amount.setCharAt(0, randomChar(DIGIT));
      }
      amount.append(".").append(randomString(DIGIT, 2, 2));
      return amount.toString();
    }

    /**
     * Returns a random value up to the given max value.
     *
     * @param maxValue A maximum value >= 0 to return
     * @return A random value greater than or equal to zero, up to the given max value.
     */
    public int randomInteger(int maxValue) {
      return maxValue == 0 ? 0 : random.nextInt(maxValue);
    }

    /**
     * Returns a random boolean value.
     *
     * @return A random boolean value
     */
    public boolean randomBoolean() {
      return random.nextBoolean();
    }
  }

  /** Helper class for managing path layers. */
  interface PathLayer {
    /** Should remove a path layer only if one was previously added. */
    void remove();
  }
}
