package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ProtocolMessageEnum;
import gov.cms.mpsm.rda.v1.EnumOptions;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The RDA API uses a combination of oneof and custom options for their enum values. Specifically
 * the oneof will contain either a valid enum value or a string. The string is used when they have
 * no match on their side for one of the defined enum cases.
 *
 * <p>When the string does match one of the standard cases the API provides access to the underlying
 * string in the form of a protobuf "custom option".
 *
 * <p>In our database we simply store the original string value. Getting to that value requires a
 * series of tests and calls to pull the value from either the custom option data or the
 * unrecognized value.
 *
 * <p>This class encapsulates the logic needed to extract the string value of an enum. It is
 * immutable and uses lambdas to access the values it needs for a specific record at runtime.
 * Lambdas have to be used because protobuf generates the record classes and does not generate any
 * kind of base class we can use.
 *
 * @param <TRecord> the protobuf object class
 * @param <TEnum> the protobuf field's enum class
 */
public class EnumStringExtractor<TRecord, TEnum extends ProtocolMessageEnum>
    implements gov.cms.model.rda.codegen.library.EnumStringExtractor<TRecord, TEnum> {
  private final Predicate<TRecord> hasEnumValue;
  private final Function<TRecord, ProtocolMessageEnum> getEnumValue;
  private final Predicate<TRecord> hasUnrecognizedValue;
  private final Function<TRecord, String> getUnrecognizedValue;
  private final TEnum invalidValue;
  private final Set<ProtocolMessageEnum> unsupportedEnumValues;
  private final Set<Options> options;

  /**
   * Constructs a value using the provided functions and value.
   *
   * @param hasEnumValue lambda used to determine if the enun has a value
   * @param getEnumValue lambda used to get the enum's value
   * @param hasUnrecognizedValue lambda used to determine if the field has an unrecognized value
   *     string
   * @param getUnrecognizedValue lambda used to get the value string
   * @param invalidValue enum value (usually TEnum.UNRECOGNIZED) for protobuf's bad enum value
   * @param unsupportedEnumValues set of enum values that should generate an UnsupportedValue Result
   * @param options the (usually empty) set of options to be used while processing
   */
  public EnumStringExtractor(
      Predicate<TRecord> hasEnumValue,
      Function<TRecord, ProtocolMessageEnum> getEnumValue,
      Predicate<TRecord> hasUnrecognizedValue,
      Function<TRecord, String> getUnrecognizedValue,
      TEnum invalidValue,
      Set<ProtocolMessageEnum> unsupportedEnumValues,
      Set<Options> options) {
    this.hasEnumValue = hasEnumValue;
    this.getEnumValue = getEnumValue;
    this.hasUnrecognizedValue = hasUnrecognizedValue;
    this.getUnrecognizedValue = getUnrecognizedValue;
    this.invalidValue = invalidValue;
    this.unsupportedEnumValues = ImmutableSet.copyOf(unsupportedEnumValues);
    this.options = ImmutableSet.copyOf(options);
  }

  /**
   * Check the record to see if a value exists and return a Result object containing either the
   * string value or a Status indicating why a value could not be returned.
   *
   * @param record object containing the enum field
   * @return the Result object containing Status and (possibly) a value
   */
  public Result getEnumString(TRecord record) {
    if (hasEnumValue.test(record)) {
      final ProtocolMessageEnum value = getEnumValue.apply(record);
      if (value == invalidValue) {
        return INVALID_VALUE_RESULT;
      }
      final String strValue =
          value.getValueDescriptor().getOptions().getExtension(EnumOptions.stringValue);
      final Status status =
          unsupportedEnumValues.contains(value) ? Status.UnsupportedValue : Status.HasValue;
      return new Result(status, strValue);
    }
    if (hasUnrecognizedValue.test(record)) {
      final String strValue = getUnrecognizedValue.apply(record);
      final Status status =
          options.contains(Options.RejectUnrecognized) ? Status.UnsupportedValue : Status.HasValue;
      return new Result(status, strValue);
    }
    return NO_VALUE_RESULT;
  }
}
