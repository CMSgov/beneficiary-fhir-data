package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

abstract class AbstractRandomClaimGenerator {
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  private static final String DIGIT = "1234567890";
  private static final String ALNUM = ALPHA + DIGIT;
  private static final int MAX_DAYS_AGO = 180;

  private final Random random;
  private final boolean optionalTrue;
  private final Clock clock;

  AbstractRandomClaimGenerator(long seed, boolean optionalTrue, Clock clock) {
    this.random = new Random(seed);
    this.optionalTrue = optionalTrue;
    this.clock = clock;
  }

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

  protected String randomDate() {
    final LocalDate date = LocalDate.now(clock).minusDays(random.nextInt(MAX_DAYS_AGO));
    return date.toString();
  }

  protected String randomAmount() {
    final int dollarDigits = 1 + random.nextInt(5);
    final StringBuilder amount = new StringBuilder(randomString(DIGIT, dollarDigits, dollarDigits));
    while (amount.length() > 1 && amount.charAt(0) == '0') {
      amount.setCharAt(0, randomChar(DIGIT));
    }
    amount.append(".").append(randomString(DIGIT, 2, 2));
    return amount.toString();
  }

  protected <T> T randomEnum(List<T> values) {
    return values.get(random.nextInt(values.size()));
  }

  protected void optional(Runnable action) {
    if (optionalTrue || random.nextBoolean()) {
      action.run();
    }
  }

  protected void either(Runnable action1, Runnable action2) {
    if (random.nextBoolean()) {
      action1.run();
    } else {
      action2.run();
    }
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
