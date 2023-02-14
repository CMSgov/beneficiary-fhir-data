package gov.cms.model.dsl.codegen.library;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import lombok.Data;
import lombok.Getter;

/**
 * Stateful, mutable, non-thread safe object to facilitate transformation of data from arbitrary
 * data objects (such as RDA API messages or RIF-CSV objects) into database entity objects. Every
 * copy method validates the field value in an appropriate manner and then passes the transformed
 * value to a Consumer (usually a setter on the entity). All data validation failures are tracked in
 * a List. Following the transformation, the caller can invoke the isSuccessful method to determine
 * if there were any errors. All copy methods return this instance so that calls can be chained.
 */
public class DataTransformer {
  /** {@link DateTimeFormatter} used to parse RIF 8 character date values. */
  private static final DateTimeFormatter RifEightCharacterDate =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyyMMdd").toFormatter();

  /** {@link DateTimeFormatter} used to parse RIF 11 character date values. */
  private static final DateTimeFormatter RifElevenCharacterDate =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("dd-MMM-yyyy")
          .toFormatter();

  /** {@link DateTimeFormatter} used to parse RIF time stamp values. */
  private static final DateTimeFormatter RifTimestamp =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("dd-MMM-yyyy HH:mm:ss")
          .toFormatter();

  /** List of error messages for errors detected during transformation. */
  private final List<ErrorMessage> errors = new ArrayList<>();

  /**
   * Determines if all of the transformations were successful.
   *
   * @return true if all transformations were successful, false otherwise
   */
  public boolean isSuccessful() {
    return errors.isEmpty();
  }

  /**
   * Returns a (possibly empty) list of all of the transformation errors.
   *
   * @return A (possibly empty) list of all of the transformation errors.
   */
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
  @CanIgnoreReturnValue
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
   * Checks the nullability, length, and value of a string and then delivers it to the Consumer if
   * the checks are successful. Valid null values are silently accepted without calling the
   * Consumer. Ensures that the actual value exactly matches an expected value. This is used to
   * ensure an invariant is being followed in the source data.
   *
   * <p>For security reasons when the strings do not match the resulting {@link ErrorMessage} will
   * contain a masked version of the string rather than the string itself. A masked string contains
   * '.' to indicate characters that match, '+' to indicate an extra character at the end of the
   * string, '-' to indicate a missing character at the end of the string, '#' to indicate a
   * mismatching character. {@link #maskString} for more details.
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
   * Copies a required java int value. Uses lambda expression for the value extraction.
   *
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyInt(IntSupplier value, IntConsumer copier) {
    copier.accept(value.getAsInt());
    return this;
  }

  /**
   * Copies an optional java int value only if its value exists. Uses lambda expressions for the
   * existence test as well as the value extraction.
   *
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
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
   * Copies a required java long value. Uses lambda expression for the value extraction.
   *
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyLong(LongSupplier value, LongConsumer copier) {
    copier.accept(value.getAsLong());
    return this;
  }

  /**
   * Copies an optional java long value only if its value exists. Uses lambda expressions for the
   * existence test as well as the value extraction.
   *
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the value
   * @return this
   */
  public DataTransformer copyOptionalLong(
      BooleanSupplier exists, LongSupplier value, LongConsumer copier) {
    if (exists.getAsBoolean()) {
      copier.accept(value.getAsLong());
    }
    return this;
  }

  /**
   * Checks that the integer value of the given {@link IntSupplier} is unsigned (not negative) and
   * small enough to fit in a {@link Short} type, and passes it to the given {@link Consumer}.
   *
   * @param fieldName The name of the field from which the value originates.
   * @param value The value being validated / copied.
   * @param copier The consumer to receive the value.
   * @return this
   */
  public DataTransformer copyUIntToShort(String fieldName, int value, Consumer<Short> copier) {
    if (validateUnsigned(fieldName, value) && validateShort(fieldName, value)) {
      copier.accept((short) value);
    }
    return this;
  }

  /**
   * Checks to see if a value is present and, if it is, calls {@link #copyUIntToShort} to copy the
   * value.
   *
   * @param fieldName The name of the field from which the value originates.
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier The consumer to receive the value.
   * @return this
   */
  public DataTransformer copyOptionalUIntToShort(
      String fieldName, BooleanSupplier exists, IntSupplier value, Consumer<Short> copier) {
    if (exists.getAsBoolean()) {
      return copyUIntToShort(fieldName, value.getAsInt(), copier);
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
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called. Optional character fields must be
   * of java type {@code Character}.
   *
   * <p>If the value exists extracts the first character of the string and delivers it to the
   * Consumer. The string MUST not be null and MUST have length one to be valid.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the string value of length 1 to copy
   * @param copier Consumer to receive the hashed value
   * @return this
   */
  public DataTransformer copyOptionalCharacter(
      String fieldName,
      BooleanSupplier exists,
      Supplier<String> value,
      Consumer<Character> copier) {
    if (exists.getAsBoolean()) {
      copyCharacter(fieldName, value.get(), copier);
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
        LocalDate date;
        switch (value.length()) {
          case 8:
            date = LocalDate.parse(value, RifEightCharacterDate);
            break;
          case 11:
            date = LocalDate.parse(value, RifElevenCharacterDate);
            break;
          default:
            date = LocalDate.parse(value);
            break;
        }
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
   * Parses the string into an Integer and delivers it to the Consumer. The string value must be a
   * valid integer. Valid null values are silently accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value integer string
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyIntString(
      String fieldName, boolean nullable, String value, Consumer<Integer> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        int intValue = Integer.parseInt(value);
        copier.accept(intValue);
      } catch (NumberFormatException ex) {
        addError(fieldName, "invalid integer");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into a Integer. Valid null values are silently accepted without calling
   * the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyOptionalIntString(
      String fieldName, BooleanSupplier exists, Supplier<String> value, Consumer<Integer> copier) {
    if (exists.getAsBoolean()) {
      return copyIntString(fieldName, false, value.get(), copier);
    }
    return this;
  }

  /**
   * Parses the string into a Short and delivers it to the Consumer. The string value must be a
   * valid short. Valid null values are silently accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value short string
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyShortString(
      String fieldName, boolean nullable, String value, Consumer<Short> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        short shortValue = Short.parseShort(value);
        copier.accept(shortValue);
      } catch (NumberFormatException ex) {
        addError(fieldName, "invalid short");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into a Short. Valid null values are silently accepted without calling the
   * Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyOptionalShortString(
      String fieldName, BooleanSupplier exists, Supplier<String> value, Consumer<Short> copier) {
    if (exists.getAsBoolean()) {
      return copyShortString(fieldName, false, value.get(), copier);
    }
    return this;
  }

  /**
   * Parses the string into an Long and delivers it to the Consumer. The string value must be a
   * valid long. Valid null values are silently accepted without calling the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value long string
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyLongString(
      String fieldName, boolean nullable, String value, Consumer<Long> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        long longValue = Long.parseLong(value);
        copier.accept(longValue);
      } catch (NumberFormatException ex) {
        addError(fieldName, "invalid long");
      }
    }
    return this;
  }

  /**
   * Copies an optional field only if its value exists. Uses lambda expressions for the existence
   * test as well as the value extraction. Optional fields must be nullable at the database level
   * but must return non-null values when the supplier is called.
   *
   * <p>Parses the string into a Long. Valid null values are silently accepted without calling the
   * Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the date
   * @return this
   */
  public DataTransformer copyOptionalLongString(
      String fieldName, BooleanSupplier exists, Supplier<String> value, Consumer<Long> copier) {
    if (exists.getAsBoolean()) {
      return copyLongString(fieldName, false, value.get(), copier);
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
        // In Java 11, Instant.parse() doesn't support offsets, so using OffsetDateTime
        Instant timestamp = OffsetDateTime.parse(value).toInstant();
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
   * Parses the string into an Instant and delivers it to the Consumer. The string value must be in
   * RIF timestamp format format ({@code "dd-MMM-yyyy HH:mm:ss"}). Valid null values are silently
   * accepted without calling the Consumer. The timezone is assumed to be UTC as per {@code
   * RifParsingUtils.parseTimestamp()}.
   *
   * @param fieldName name of the field from which the value originates
   * @param nullable true if null is a valid value
   * @param value timestamp string in RIF format
   * @param copier Consumer to receive the timestamp
   * @return this
   */
  public DataTransformer copyRifTimestamp(
      String fieldName, boolean nullable, String value, Consumer<Instant> copier) {
    if (nonNull(fieldName, value, nullable)) {
      try {
        Instant timestamp = LocalDateTime.parse(value, RifTimestamp).toInstant(ZoneOffset.UTC);
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
   * <p>Parses the string into an Instant and delivers it to the Consumer. The string value must be
   * in {@code dd-MMM-yyyy HH:mm:ss} format. Valid null values are silently accepted without calling
   * the Consumer.
   *
   * @param fieldName name of the field from which the value originates
   * @param exists returns true if the value exists
   * @param value returns the value to copy
   * @param copier Consumer to receive the timestamp
   * @return this
   */
  public DataTransformer copyOptionalRifTimestamp(
      String fieldName, BooleanSupplier exists, Supplier<String> value, Consumer<Instant> copier) {
    if (exists.getAsBoolean()) {
      return copyRifTimestamp(fieldName, false, value.get(), copier);
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
   * @return true if the value is non-negative, false otherwise.
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
   * Detects if the two values are the same. Adds an appropriate error message if they are not.
   *
   * @param fieldName name of the field from which the value originates
   * @param expectedValue expected value
   * @param actualValue actual value of the field
   * @return true if the values are equal
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
   * Adds a formatted error message to the list of errors.
   *
   * @param fieldName name of the field containing the error
   * @param errorFormat {@link String#format} compatible format string for the message
   * @param args array of arguments to fill placeholders in the format string
   */
  public void addError(String fieldName, String errorFormat, Object... args) {
    final String message = String.format(errorFormat, args);
    errors.add(new ErrorMessage(fieldName, message));
  }

  /**
   * Convert a {@link Timestamp} into an {@link Instant}.
   *
   * @param timestamp the {@link Timestamp} to convert
   * @return the corresponding {@link Instant}
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
    /** The name of the field the error is associated with. */
    private final String fieldName;
    /** The message that describes the error that was found. */
    private final String errorMessage;

    @Override
    public String toString() {
      return "<'" + fieldName + "','" + errorMessage + "'>";
    }
  }

  /** Exception thrown to indicate that there was an issue with transforming an object. */
  @Getter
  public static class TransformationException extends RuntimeException {
    /** Non-empty list of {@link ErrorMessage} objects. */
    private final List<ErrorMessage> errors;

    /**
     * Create an instance.
     *
     * @param message human readable error message
     * @param errors list of errors
     */
    public TransformationException(String message, List<ErrorMessage> errors) {
      super(message);
      this.errors = errors;
    }

    @Override
    public String toString() {
      return getMessage() + getErrors();
    }
  }
}
