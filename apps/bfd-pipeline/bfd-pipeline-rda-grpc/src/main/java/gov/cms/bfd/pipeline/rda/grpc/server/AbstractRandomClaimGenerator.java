package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

abstract class AbstractRandomClaimGenerator {
  protected static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  protected static final String DIGIT = "1234567890";
  protected static final String ALNUM = ALPHA + DIGIT;
  protected static final int MAX_DAYS_AGO = 180;
  private final Random random;
  private final boolean optionalTrue;

  AbstractRandomClaimGenerator(long seed, boolean optionalTrue) {
    this.random = new Random(seed);
    this.optionalTrue = optionalTrue;
  }

  protected static <T extends Enum<T>> List<T> enumValues(T[] values) {
    return Arrays.stream(values)
        .filter(v -> !v.name().equals("UNRECOGNIZED"))
        .collect(ImmutableList.toImmutableList());
  }

  protected int randomCount(int maxValue) {
    return random.nextInt(maxValue);
  }

  protected char randomChar(String characters) {
    return characters.charAt(random.nextInt(characters.length()));
  }

  protected String randomString(String characters, int minLength, int maxLength) {
    final StringBuilder sb = new StringBuilder();
    final int len = minLength + random.nextInt(maxLength - minLength + 1);
    while (sb.length() < len) {
      sb.append(randomChar(characters));
    }
    return sb.toString();
  }

  protected String randomDate() {
    final LocalDate date = LocalDate.now().minusDays(random.nextInt(MAX_DAYS_AGO));
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
}
