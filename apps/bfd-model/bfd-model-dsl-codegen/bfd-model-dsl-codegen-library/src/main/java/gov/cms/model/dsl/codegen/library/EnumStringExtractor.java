package gov.cms.model.dsl.codegen.library;

import com.google.protobuf.ProtocolMessageEnum;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Data;

/**
 * The RDA API uses a combination of {@code oneof} and custom options for their enum values.
 * Specifically the {@code oneof} will contain either a valid enum value or a string. The string is
 * used when they have no match on their side for one of the defined enum cases.
 *
 * <p>When the string does match one of the standard cases the API provides access to the underlying
 * string in the form of a protobuf "custom option".
 *
 * <p>In our database we simply store the original string value. Getting to that value requires a
 * series of tests and calls to pull the value from either the custom option data or the
 * unrecognized value.
 *
 * <p>Classes implementing this interface encapsulate the logic needed to extract the string value
 * of an enum.
 *
 * @param <TRecord> the protobuf object class
 * @param <TEnum> the protobuf field's enum class
 */
public interface EnumStringExtractor<TRecord, TEnum extends ProtocolMessageEnum> {

  /**
   * Check the record to see if a value exists and return a Result object containing either the
   * string value or a Status indicating why a value could not be returned.
   *
   * @param record object containing the enum field
   * @return the Result object containing Status and (possibly) a value
   */
  Result getEnumString(TRecord record);

  /** Used to indicate the outcome of the value lookup in a Result object. */
  public enum Status {
    /** Neither the enum nor the unrecognized value string were set in the protobuf field. */
    NoValue,
    /**
     * The server set an invalid ordinal value in the enum field and protobuf mapped this to the
     * unrecognizedValue.
     */
    InvalidValue,
    /** Either the enum was set to a valid value or the unrecognized string value was set. */
    HasValue,
    /** A value was present but was rejected as unsupported. */
    UnsupportedValue
  }

  /** Used to contain the result of a value extraction. */
  @Data
  class Result {
    /** Outcome of the extraction. */
    private final Status status;

    /** Extracted value (if any - depends on status). */
    @Nullable private final String value;

    /**
     * Constructor for no extracted value.
     *
     * @param status outcome of the extraction
     */
    public Result(Status status) {
      this.status = status;
      value = null;
    }

    /**
     * Constructor for an extracted value.
     *
     * @param value extracted value
     */
    public Result(@Nullable String value) {
      status = Status.HasValue;
      this.value = value;
    }

    /**
     * General purpose constructor.
     *
     * @param status outcome of the extraction
     * @param value extracted value (if any)
     */
    public Result(Status status, @Nullable String value) {
      this.status = status;
      this.value = value;
    }
  }

  /**
   * Additional options that can be used to alter default behavior. Currently, there is only one
   * option available but using an enum instead of a boolean to enable the option improves code
   * clarity.
   */
  enum Options {
    /** Report an unsupported value result if the field has its unrecognized value. */
    RejectUnrecognized
  }

  /** Interface for lambdas that create {@link EnumStringExtractor}s. */
  interface Factory {
    /**
     * Constructs an {@link EnumStringExtractor} using the provided functions and value.
     *
     * @param hasEnumValue lambda used to determine if the enum has a value
     * @param getEnumValue lambda used to get the enum's value
     * @param hasUnrecognizedValue lambda used to determine if the field has an unrecognized value
     *     string
     * @param getUnrecognizedValue lambda used to get the value string
     * @param invalidValue enum value (usually TEnum.UNRECOGNIZED) for protobuf's bad enum value
     * @param unsupportedEnumValues set of enum values that should generate an UnsupportedValue
     *     Result
     * @param options the (usually empty) set of options to be used while processing
     * @param <TRecord> type of objects containing an enum value to extract
     * @param <TEnum> type of the enum to extract
     * @return an {@link EnumStringExtractor} instance
     */
    <TRecord, TEnum extends ProtocolMessageEnum>
        EnumStringExtractor<TRecord, TEnum> createEnumStringExtractor(
            Predicate<TRecord> hasEnumValue,
            Function<TRecord, ProtocolMessageEnum> getEnumValue,
            Predicate<TRecord> hasUnrecognizedValue,
            Function<TRecord, String> getUnrecognizedValue,
            TEnum invalidValue,
            Set<ProtocolMessageEnum> unsupportedEnumValues,
            Set<Options> options);
  }
}
