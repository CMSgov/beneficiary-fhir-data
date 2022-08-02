package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import lombok.Data;
import lombok.Getter;

/**
 * Stateful, mutable, non-thread safe object to facilitate transformation of data from incoming RDA
 * API objects into database entity objects. Every copy method validates the field value in an
 * appropriate manner and then passes the transformed value to a Consumer (usually a setter on the
 * entity). All data validation failures are tracked in a List. Following the transformation, the
 * caller can invoke the isSuccessful method to determine if there were any errors. All copy methods
 * return this instance so that calls can be chained.
 */
public class DataTransformer {
  private final List<ErrorMessage> errors = new ArrayList<>();

  /**
   * Determines if all of the transoformations were successful.
   *
   * @return true if all transofmrations were successful, false otherwise
   */
  public boolean isSuccessful() {
    return errors.isEmpty();
  }

  /** @return A (possibly empty) list of all of the transformation errors. */
  public List<ErrorMessage> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  /**
   * Throws an exception if there are any transformation errors. Makes it easy to generate a
   * standard exception following all of the transformation steps.
   *
   * @throws TransformationException if there are any transformation errors
   * @return this
   */
  public DataTransformer throwIfErrorsPresent() throws TransformationException {
    if (errors.size() > 0) {
      throw new TransformationException(
          String.format("failed with %d errors", errors.size()), getErrors());
    }
    return this;
  }

  /**
   * Checks the nullability and length of a string.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param minLength minimum allowed length for non-null value
   * @param maxLength maximum allowed length for non-null value
   * @param value value to validate
   * @return true if the string value is valid
   */
  public boolean validateString(
      String fieldName, boolean nullable, int minLength, int maxLength, String value) {
    return nonNull(fieldName, value, nullable) && lengthOk(fieldName, value, minLength, maxLength);
  }

  /**
   * Checks to ensure that at least one of the two fields has a non-empty string value. If neither
   * does an error is added to the list of errors.
   *
   * @param fieldName1 name of the first field
   * @param value1 value (possibly null) of the first field
   * @param fieldName2 name of the second field
   * @param value2 value (possibly null) of the second field
   * @return true if at least one of the two fields has a non-null, non-empty string value
   */
  public boolean validateAtLeastOneIsPresent(
      String fieldName1, String value1, String fieldName2, String value2) {
    final var isPresent1 = !Strings.isNullOrEmpty(value1);
    final var isPresent2 = !Strings.isNullOrEmpty(value2);
    final var isValid = isPresent1 || isPresent2;
    if (!isValid) {
      addError(
          fieldName1,
          "expected either %s or %s to have value but neither did",
          fieldName1,
          fieldName2);
    }
    return isValid;
  }

  /**
   * Checks the nullability and length of a string and then delivers it to the Consumer if the
   * checks are successful. Valid null values are silently accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param minLength minimum allowed length for non-null value
   * @param maxLength maximum allowed length for non-null value
   * @param value value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyString(
      String fieldName,
      boolean nullable,
      int minLength,
      int maxLength,
      String value,
      Consumer<String> copier) {
    if (validateString(fieldName, nullable, minLength, maxLength, value)) {
      copier.accept(value);
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Checks the nullability and length of a string and then delivers it to the Consumer if the
   * checks are successful. Valid null values are silently accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param minLength minimum allowed length for non-null value
   * @param maxLength maximum allowed length for non-null value
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyOptionalString(
      String fieldName,
      int minLength,
      int maxLength,
      BooleanSupplier exists,
      Supplier<String> value,
      Consumer<String> copier) {
    if (exists.getAsBoolean()) {
      return copyString(fieldName, false, minLength, maxLength, value.get(), copier);
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists and is non-empty. Uses lambda expressions for
   * the existence test as well as the value extraction. Optional fields must be nullable at the
   * database level but must return non-null values when the supplier is called.
   *
   * <p>Checks the nullability and length of a string and then delivers it to the Consumer if the
   * checks are successful. Valid null or empty string values are silently accepted without calling
   * the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param minLength minimum allowed length for non-null value
   * @param maxLength maximum allowed length for non-null value
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyOptionalNonEmptyString(
      String fieldName,
      int minLength,
      int maxLength,
      BooleanSupplier exists,
      Supplier<String> value,
      Consumer<String> copier) {
    if (exists.getAsBoolean() && !Strings.isNullOrEmpty(value.get())) {
      return copyString(fieldName, false, minLength, maxLength, value.get(), copier);
    }
    return this;
  }

  /**
   * Checks the nullability and length of a string and then delivers it to the Consumer if the
   * checks are successful. Valid null values are silently accepted without calling the Consumer.
   * Ensures that the actual value exactly matches an expected value. This is used to ensure an
   * invariant is being followed in the source data.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param minLength minimum allowed length for non-null value
   * @param maxLength maximum allowed length for non-null value
   * @param expectedValue value to compare actualValue to
   * @param actualValue value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyStringWithExpectedValue(
      String fieldName,
      boolean nullable,
      int minLength,
      int maxLength,
      String expectedValue,
      String actualValue,
      Consumer<String> copier) {
    if (nonNull(fieldName, actualValue, nullable)
        && lengthOk(fieldName, actualValue, minLength, maxLength)
        && valueMatches(fieldName, expectedValue, actualValue)) {
      copier.accept(actualValue);
    }
    return this;
  }

  /**
   * If the value exists, check that the integer value of the given {@link IntSupplier} is unsigned
   * (not negative) and small enough to fit in a {@link Short} type, and passes it to the given
   * {@link IntConsumer}.
   *
   * @param fieldName The name of the field from which the value originates.
   * @param exists Indicates if the value exists in the supplier.
   * @param value The value being validated / copied.
   * @param copier The consumer to receive the value.
   * @return this
   */
  public DataTransformer copyOptionalUIntToShort(
      String fieldName, BooleanSupplier exists, IntSupplier value, Consumer<Short> copier) {
    if (exists.getAsBoolean()) {
      copyUIntToShort(fieldName, value, copier);
    }

    return this;
  }

  /**
   * Checks that the integer value of the given {@link IntSupplier} is unsigned (not negative) and
   * small enough to fit in a {@link Short} type, and passes it to the given {@link IntConsumer}.
   *
   * @param fieldName The name of the field from which the value originates.
   * @param value The value being validated / copied.
   * @param copier The consumer to receive the value.
   * @return this
   */
  public DataTransformer copyUIntToShort(
      String fieldName, IntSupplier value, Consumer<Short> copier) {
    int v = value.getAsInt();

    if (validateUnsigned(fieldName, v) && validateShort(fieldName, v)) {
      copier.accept((short) v);
    }

    return this;
  }

  /**
   * If the value exists, denoted by the given {@link BooleanSupplier}, it will be copied from the
   * given {@link IntSupplier} into the given {@link IntConsumer}.
   *
   * @param exists Denotes if the value exists.
   * @param value The value to be copied, if it exists.
   * @param copier The consumer to receive the value.
   * @return this
   */
  public DataTransformer copyOptionalInt(
      BooleanSupplier exists, IntSupplier value, IntConsumer copier) {
    if (exists.getAsBoolean()) {
      copier.accept(value.getAsInt());
    }
    return this;
  }

  /**
   * RDA API 0.2 MVP has some enums that have numeric values matching the ASCII character from the
   * upstream source record. Check the integer value and copy it if it represents an ASCII character
   * or add an error otherwise.
   *
   * @param fieldName name of the field from which the value originates
   * @param enumResult the enum result
   * @param copier Consumer to receive the character value
   * @return this data transformer
   */
  public DataTransformer copyEnumAsCharacter(
      String fieldName, EnumStringExtractor.Result enumResult, Consumer<Character> copier) {
    return copyEnumAsString(fieldName, false, 1, 1, enumResult, s -> copier.accept(s.charAt(0)));
  }

  /**
   * Same as copyString() but with a EnumStringExtractor.Result and minLength of 0.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable if the field should be nullable
   * @param maxLength the max length
   * @param enumResult the enum result
   * @param copier Consumer to receive the character value as a String
   * @return this data transformer
   */
  public DataTransformer copyEnumAsString(
      String fieldName,
      boolean nullable,
      int maxLength,
      EnumStringExtractor.Result enumResult,
      Consumer<String> copier) {
    return copyEnumAsString(fieldName, nullable, 0, maxLength, enumResult, copier);
  }

  /**
   * Same as copyString() but with a EnumStringExtractor.Result.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable if the field should be nullable
   * @param minLength the min length
   * @param maxLength the max length
   * @param enumResult the enum result
   * @param copier Consumer to receive the character value as a String
   * @return this data transformer
   */
  public DataTransformer copyEnumAsString(
      String fieldName,
      boolean nullable,
      int minLength,
      int maxLength,
      EnumStringExtractor.Result enumResult,
      Consumer<String> copier) {
    final EnumStringExtractor.Status status = enumResult.getStatus();
    if (status == EnumStringExtractor.Status.NoValue && !nullable) {
      addError(fieldName, "no value set");
    } else if (status == EnumStringExtractor.Status.InvalidValue) {
      addError(fieldName, "unrecognized enum value");
    } else if (status == EnumStringExtractor.Status.UnsupportedValue) {
      addError(fieldName, "unsupported enum value");
    } else {
      final String value = enumResult.getValue();
      copyString(fieldName, nullable, minLength, maxLength, value, copier);
    }
    return this;
  }

  /**
   * Extract the first character of the string and deliver it to the Consumer. The string MUST not
   * be null and MUST have length one to be valid.
   *
   * @param fieldName name of the field from which the value originates
   * @param value string of length 1
   * @param copier Consumer to receive the hashed value
   * @return this
   */
  public DataTransformer copyCharacter(String fieldName, String value, Consumer<Character> copier) {
    if (nonNull(fieldName, value, false) && lengthOk(fieldName, value, 1, 1)) {
      copier.accept(value.charAt(0));
    }
    return this;
  }

  /**
   * Parses the string into a LocalDate and delivers it to the Consumer. The string value must be in
   * ISO-8601 format (YYYY-MM-DD). Valid null values are silently accepted without calling the
   * Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value date string in ISO-8601 format
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyDate(
      String fieldName, boolean nullable, String value, Consumer<LocalDate> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        LocalDate date = LocalDate.parse(value);
        copier.accept(date);
      } catch (DateTimeParseException ex) {
        addError(fieldName, "invalid date");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into a LocalDate and delivers it to the Consumer. The string value must be
   * in ISO-8601 format (YYYY-MM-DD). Valid null values are silently accepted without calling the
   * Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyOptionalDate(
      String fieldName,
      BooleanSupplier exists,
      Supplier<String> value,
      Consumer<LocalDate> copier) {
    if (exists.getAsBoolean()) {
      return copyDate(fieldName, false, value.get(), copier);
    }
    return this;
  }

  /**
   * Parses the string into an {@link Instant} and delivers it to the Consumer. The string value
   * must be in ISO-8601 format (YYYY-MM-DDTHH:ii:ss.SSSSSSZ). Valid null values are silently
   * accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value timestamp string in ISO-8601 format
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyTimestamp(
      String fieldName, boolean nullable, String value, Consumer<Instant> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        Instant timestamp = Instant.parse(value);
        copier.accept(timestamp);
      } catch (DateTimeParseException ex) {
        addError(fieldName, "invalid timestamp");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into an {@link Instant} and delivers it to the Consumer. The string value
   * must be in ISO-8601 format (YYYY-MM-DDTHH:ii:ss.SSSSSSZ). Valid null values are silently
   * accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the timestamp
   * @return this
   */
  public DataTransformer copyOptionalTimestamp(
      String fieldName, BooleanSupplier exists, Supplier<String> value, Consumer<Instant> copier) {
    if (exists.getAsBoolean()) {
      return copyTimestamp(fieldName, false, value.get(), copier);
    }
    return this;
  }

  /**
   * Parses the string into a BigDecimal and delivers it to the Consumer. The string value must be a
   * valid positive or negative numeric format. Valid null values are silently accepted without
   * calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value string containing valid real number
   * @param copier Consumer to receive the BigDecimal
   * @return this
   */
  public DataTransformer copyAmount(
      String fieldName, boolean nullable, String value, Consumer<BigDecimal> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        BigDecimal amount = new BigDecimal(value);
        copier.accept(amount);
      } catch (NumberFormatException ex) {
        addError(fieldName, "invalid amount");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into a BigDecimal and delivers it to the Consumer. The string value must
   * be a valid positive or negative numeric format. Valid null values are silently accepted without
   * calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the BigDecimal
   * @return this
   */
  public DataTransformer copyOptionalAmount(
      String fieldName,
      BooleanSupplier exists,
      Supplier<String> value,
      Consumer<BigDecimal> copier) {
    if (exists.getAsBoolean()) {
      return copyAmount(fieldName, false, value.get(), copier);
    }
    return this;
  }

  /**
   * Checks if the given value is unsigned (positive).
   *
   * @param fieldName The name of the attribute the value is associated with (for error tracking).
   * @param value The value being validated.
   * @return True if the value is positive (>= 0), alse otherwise.
   */
  private boolean validateUnsigned(String fieldName, long value) {
    boolean isValid = true;

    if (value < 0) {
      addError(fieldName, "is signed");
      isValid = false;
    }

    return isValid;
  }

  /**
   * Checks if the given value is within the {@link Short} value range.
   *
   * @param fieldName The name of the attribute the value is associated with (for error tracking).
   * @param value The value being validated.
   * @return True if the value is within the {@link Short} value range, False otherwise.
   */
  private boolean validateShort(String fieldName, long value) {
    boolean isValid = true;

    if (value > Short.MAX_VALUE) {
      addError(fieldName, "is too large");
      isValid = false;
    } else if (value < Short.MIN_VALUE) {
      addError(fieldName, "is too small");
      isValid = false;
    }

    return isValid;
  }

  /**
   * Used internally to stop transformation if the value is null. A null value with nullable=false
   * adds an error to the errors list.
   *
   * @param fieldName name of the field from which the value originates
   * @param value value to heck for null
   * @param nullable true if null is a valid value
   * @return true if the value is non-null
   */
  private boolean nonNull(String fieldName, Object value, boolean nullable) {
    if (value != null) {
      return true;
    }
    if (!nullable) {
      addError(fieldName, "is null");
    }
    return false;
  }

  /**
   * Used internally to stop transformation if the value length is out of bounds.
   *
   * @param fieldName name of the field from which the value originates
   * @param value non-null string value
   * @param minLength minimum valid length for the string
   * @param maxLength maximum valid length for the string
   * @return true if the value has a valid length
   */
  private boolean lengthOk(String fieldName, String value, int minLength, int maxLength) {
    final int length = value.length();
    if (length < minLength || length > maxLength) {
      addError(
          fieldName, "invalid length: expected=[%d,%d] actual=%d", minLength, maxLength, length);
      return false;
    }
    return true;
  }

  /**
   * Checks that the given expectedValue matches the given actualValue for a given fieldName,
   * tracking the error if they do not match.
   *
   * @param fieldName The name of the field from which the value originates.
   * @param expectedValue The expected value of the field.
   * @param actualValue The aal value of the field.
   * @return True if the given expected and actual values match, False otherwise.
   */
  private boolean valueMatches(String fieldName, String expectedValue, String actualValue) {
    final boolean matches =
        (expectedValue == null && actualValue == null)
            || (expectedValue != null && expectedValue.equals(actualValue));
    if (!matches) {
      final String masked = maskString(actualValue, expectedValue);
      addError(fieldName, "value mismatch: masked=%s", masked);
    }
    return matches;
  }

  /**
   * Adds an error to the list of tracked errors.
   *
   * @param fieldName The name of the field that had an error.
   * @param errorFormat The format string for the error message.
   * @param args The arguments for the formatted error string.
   */
  public void addError(String fieldName, String errorFormat, Object... args) {
    final String message = String.format(errorFormat, args);
    errors.add(new ErrorMessage(fieldName, message));
  }

  /**
   * Helper method to create an {@link Instant} object from the given {@link Timestamp}.
   *
   * @param timestamp The {@link Timestamp} to create an {@link Instant} from.
   * @return The created {@link Instant} object.
   */
  public Instant instant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  /**
   * Produces a string suitable for logging comparison mis-matches of two potentially sensitive
   * strings. The resulting string contains '.' to indicate characters that match, '+' to indicate
   * an extra character at the end of the string, '-' to indicate a missing character at the end of
   * the string, '#' to indicate a mismatching character.
   *
   * @param source String containing some mismatched characters.
   * @param comparison String containing the expected value.
   * @return the masked string for logging.
   */
  private String maskString(String source, String comparison) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < source.length(); ++i) {
      if (i >= comparison.length()) {
        sb.append('+');
      } else {
        char sourceChar = source.charAt(i);
        char comparisonChar = comparison.charAt(i);
        if (sourceChar == comparisonChar) {
          sb.append('.');
        } else {
          sb.append('#');
        }
      }
    }
    for (int i = 0; i < comparison.length() - source.length(); ++i) {
      sb.append('-');
    }
    return sb.toString();
  }

  /** Helper class for tracking error messages. */
  @Data
  public static class ErrorMessage {
    /** The name of the field the error is associated with */
    private final String fieldName;
    /** The message that describes the error that was found */
    private final String errorMessage;

    @Override
    public String toString() {
      return "<'" + fieldName + "','" + errorMessage + "'>";
    }
  }

  /** Exception thrown to indicate that there was an issue with transforming an object. */
  @Getter
  public static class TransformationException extends RuntimeException {
    private final List<ErrorMessage> errors;

    public TransformationException(String message, List<ErrorMessage> errors) {
      super(message);
      this.errors = errors;
    }
  }
}
