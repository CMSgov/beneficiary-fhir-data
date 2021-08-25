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
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  private static final String DIGIT = "1234567890";
  private static final String ALNUM = ALPHA + DIGIT;
  private static final int MAX_DAYS_AGO = 180;

  private final Random random;
  private final boolean optionalTrue;
  private final Clock clock;

  /**
   * Constructs an instance.
   *
   * @param seed numeric seed value for the PRNG
   * @param optionalTrue when true optionals will always be generated (used for tests)
   * @param clock Clock to generate current time/date values (needed for tests)
   */
  AbstractRandomClaimGenerator(long seed, boolean optionalTrue, Clock clock) {
    this.random = new Random(seed);
    this.optionalTrue = optionalTrue;
    this.clock = clock;
  }

  /**
   * RDA API enums always define a special value that the protobuf API intends as a special place
   * holder for garbled/invalid data. This field is named UNRECOGNIZED. This method is used to
   * accept all of the defined enum values, skip this special value, and return an immutable list of
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

  protected int randomInt(int maxValue) {
    return maxValue == 0 ? 0 : random.nextInt(maxValue);
  }

  protected String randomDigit(int minLength, int maxLength) {
    return randomString(DIGIT, minLength, maxLength);
  }

  protected String randomLetter(int minLength, int maxLength) {
    return randomString(ALPHA, minLength, maxLength);
  }

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
    final LocalDate date = LocalDate.now(clock).minusDays(random.nextInt(MAX_DAYS_AGO));
    return date.toString();
  }

  /**
   * Generates a random decimal string in the range 0.00 to 99999.99. The value will always have at
   * least one digit before the decimal point and exactly two digits after.
   *
   * @return random decimal string.
   */
  protected String randomAmount() {
    final int dollarDigits = 1 + random.nextInt(5);
    final StringBuilder amount = new StringBuilder(randomString(DIGIT, dollarDigits, dollarDigits));
    while (amount.length() > 1 && amount.charAt(0) == '0') {
      amount.setCharAt(0, randomChar(DIGIT));
    }
    amount.append(".").append(randomString(DIGIT, 2, 2));
    return amount.toString();
  }

  /**
   * Selects a random enum value from the list and returns it.
   *
   * @param values all possible values
   * @param <T> the enum type
   * @return one of the values selected at random
   */
  protected <T> T randomEnum(List<T> values) {
    return values.get(random.nextInt(values.size()));
  }

  /**
   * Triggers the action 50% of the time (or always if optionalTrue has been set).
   *
   * @param action action to trigger half the time
   */
  protected void optional(Runnable action) {
    if (optionalTrue || random.nextBoolean()) {
      action.run();
    }
  }

  /**
   * Used when one of several possible values should be generated. Usually used for enums. The
   * possibilities are triggered with equal probability.
   *
   * @param actions variadic list of possible actions to trigger
   */
  protected void oneOf(Runnable... actions) {
    final int index = random.nextInt(actions.length);
    actions[index].run();
  }

  /**
   * Used when an optional field can have one of several possible values. Usually used for enums. A
   * value will be triggered 50% of the time. When a value is triggered the possibilities are
   * selected with equal probability.
   *
   * @param actions variadic list of possible actions to trigger
   */
  protected void optionalOneOf(Runnable... actions) {
    optional(() -> oneOf(actions));
  }

  protected Clock getClock() {
    return clock;
  }

  private char randomChar(String characters) {
    return characters.charAt(random.nextInt(characters.length()));
  }

  private String randomString(String characters, int minLength, int maxLength) {
    final StringBuilder sb = new StringBuilder();
    final int len = minLength + random.nextInt(maxLength - minLength + 1);
    while (sb.length() < len) {
      sb.append(randomChar(characters));
    }
    return sb.toString();
  }
}
