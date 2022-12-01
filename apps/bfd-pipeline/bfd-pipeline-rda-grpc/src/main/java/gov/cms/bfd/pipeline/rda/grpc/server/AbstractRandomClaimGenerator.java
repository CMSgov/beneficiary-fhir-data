package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The base class provides common functionality used by the FISS and MCS claim generators to create
 * random claim data.
 */
abstract class AbstractRandomClaimGenerator {
  /**
   * Characters of the alphabet (left out vowels to avoid real word generation, and ambiguous
   * letters like 'l').
   */
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  /** Numbers, zero through nine */
  private static final String DIGIT = "1234567890";
  /** Aggregated sequence of letters and numbers */
  private static final String ALNUM = ALPHA + DIGIT;
  /** The maximum number of days in the past that a random date value can be generated for */
  private static final int MAX_DAYS_AGO = 180;

  /** The {@link Random} object instance to use for generating random values */
  private final Random randomValue;
  /**
   * The {@link Random} object instance to use for deciding optional logic
   *
   * <p>{@link #optional(Runnable)} logic is done with a separate {@link Random} instance to
   * minimize the impact to other random value generations
   */
  private final Random randomOpt;
  /**
   * The {@link Random} object instance to use for deciding oneOf logic
   *
   * <p>{@link #oneOf(Runnable...)} logic is done with a separate {@link Random} instance to
   * minimize the impact to other random value generations
   */
  private final Random randomOneOf;
  /**
   * Denotes if all {@link #optional(Runnable)} or {@link #optionalOneOf(Runnable...)} should be
   * executed
   */
  private final boolean optionalOverride;
  /** The {@link Clock} object to use with generating time based values */
  private final Clock clock;

  /**
   * The global {@link RandomValueContext} to use when generating random values
   *
   * <p>This is used in combination with {@link #optional(Runnable)}
   */
  private RandomValueContext context;

  /**
   * Constructs an instance.
   *
   * @param seed numeric seed value for the PRNG
   * @param optionalOverride when true optionals will always be generated (used for tests)
   * @param clock Clock to generate current time/date values (needed for tests)
   */
  AbstractRandomClaimGenerator(long seed, boolean optionalOverride, Clock clock) {
    this.randomValue = new Random(seed);
    this.randomOpt = new Random(seed);
    this.randomOneOf = new Random(seed);
    this.optionalOverride = optionalOverride;
    this.clock = clock;
  }

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
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    return ctx.randomInteger(maxValue);
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
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    final LocalDate date = LocalDate.now(clock).minusDays(ctx.randomInteger(MAX_DAYS_AGO));
    return date.toString();
  }

  /**
   * Generates a random decimal string in the range 0.00 to 99999.99. The value will always have at
   * least one digit before the decimal point and exactly two digits after.
   *
   * @return random decimal string.
   */
  protected String randomAmount() {
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    return ctx.randomAmount();
  }

  /**
   * Selects a random enum value from the list and returns it.
   *
   * @param values all possible values
   * @param <T> the enum type
   * @return one of the values selected at random
   */
  protected <T> T randomEnum(List<T> values) {
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    return values.get(ctx.randomInteger(values.size()));
  }

  /**
   * ets the current {@link RandomValueContext} and triggers the action 50% of the time (or always
   * if optionalOverride has been set).
   *
   * @param action action to trigger half the time
   */
  protected void optional(Runnable action) {
    final RandomValueContext oldContext = context;
    // By storing a context every time, this creates stability regardless of if the optional logic
    // is executed or not.
    context = new RandomValueContext(randomValue.nextInt());
    boolean shouldRun = randomOpt.nextBoolean();

    if (optionalOverride || shouldRun) {
      action.run();
    }

    context = oldContext;
  }

  /**
   * Used when one of several possible values should be generated. Usually used for enums. The
   * possibilities are triggered with equal probability.
   *
   * <p>Sets the {@link RandomValueContext} if it has not already been set, or uses the existing one
   * if it is.
   *
   * @param actions variadic list of possible actions to trigger
   */
  protected void oneOf(Runnable... actions) {
    final int index = randomOneOf.nextInt(actions.length);
    actions[index].run();
  }

  /**
   * Used when an optional field can have one of several possible values (usually used for enums).
   *
   * <p>Sets the current {@link RandomValueContext} and triggers the action 50% of the time (or
   * always if optionalOverride has been set).
   *
   * <p>When a value is triggered the possibilities are selected with equal probability.
   *
   * @param actions variadic list of possible actions to trigger
   */
  protected void optionalOneOf(Runnable... actions) {
    optional(() -> oneOf(actions));
  }

  /**
   * Returns a reference to the internal {@link Clock} used by this instance.
   *
   * @return A reference to the internal {@link Clock} used by this instance.
   */
  protected Clock getClock() {
    return clock;
  }

  /**
   * Returns a random character of the provided sequence.
   *
   * @param characters The character sequence to provide a random character from.
   * @return A random character from the provided sequence.
   */
  protected char randomChar(String characters) {
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    return ctx.randomChar(characters);
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
    RandomValueContext ctx = context;

    if (ctx == null) {
      // Creating a RandomValueContext, even for simple values, creates stability throughout
      ctx = new RandomValueContext(randomValue.nextInt());
    }

    return ctx.randomString(characters, minLength, maxLength);
  }

  /** Provides an isolated context for creating a subset of random data. */
  private static class RandomValueContext {

    /** The {@link Random} instance for this context */
    private final Random random;

    public RandomValueContext(int seed) {
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
  }
}
