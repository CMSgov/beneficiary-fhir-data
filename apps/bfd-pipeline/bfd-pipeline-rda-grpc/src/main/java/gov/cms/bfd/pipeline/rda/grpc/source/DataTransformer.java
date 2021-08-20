package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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
    if (nonNull(fieldName, value, nullable) && lengthOk(fieldName, value, minLength, maxLength)) {
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
   * @param enumValue value of the enum
   * @param unsetValue enum instance for unset values
   * @param unrecognizedValue enum instance for unrecognized values
   * @param copier Consumer to receive the character value
   * @return this
   */
  public DataTransformer copyEnumAsCharacter(
      String fieldName, EnumStringExtractor.Result enumResult, Consumer<Character> copier) {
    return copyEnumAsString(fieldName, false, 1, 1, enumResult, s -> copier.accept(s.charAt(0)));
  }

  /**
   * Same as copyString() but with a EnumStringExtractor.Result.
   *
   * @param fieldName name of the field from which the value originates
   * @param enumValue value of the enum
   * @param unsetValue enum instance for unset values
   * @param unrecognizedValue enum instance for unrecognized values
   * @param copier Consumer to receive the character value as a String
   * @return this
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

  public void addError(String fieldName, String errorFormat, Object... args) {
    final String message = String.format(errorFormat, args);
    errors.add(new ErrorMessage(fieldName, message));
  }

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

  public static class ErrorMessage {
    private final String fieldName;
    private final String errorMessage;

    public ErrorMessage(String fieldName, String errorMessage) {
      this.fieldName = fieldName;
      this.errorMessage = errorMessage;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ErrorMessage that = (ErrorMessage) o;
      return Objects.equals(fieldName, that.fieldName)
          && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fieldName, errorMessage);
    }

    @Override
    public String toString() {
      return "<'" + fieldName + "','" + errorMessage + "'>";
    }
  }

  public static class TransformationException extends RuntimeException {
    private final List<ErrorMessage> errors;

    public TransformationException(String message, List<ErrorMessage> errors) {
      super(message);
      this.errors = errors;
    }

    public List<ErrorMessage> getErrors() {
      return errors;
    }
  }
}
