package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaNameType;
import gov.cms.model.dsl.codegen.plugin.model.validation.TransformerName;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 * Model class for transformation specifications in a mapping. Each transformation specification
 * defines how to transform the value of one field in the source object and store it into a
 * corresponding field in the destination object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationBean implements ModelBean {
  /**
   * Special value for {@link TransformationBean#transformer} to signify that the transformation is
   * to copy an array rather than a column.
   */
  public static final String ArrayTransformName = "Array";

  /** Name of the field/property in the source object to transform. */
  @NotNull
  @JavaName(type = JavaNameType.Property)
  private String from;

  /**
   * Name of the field/property in the destination object to copy the data to. Can be omitted (left
   * null) if the destination field name is identical to the source field name.
   */
  @JavaName(type = JavaNameType.Property)
  private String to;

  /**
   * Specifies which components (if any) of the {@code from} are optional (possess a {@code has}
   * method). Optional values can be null or empty in the source without triggering an error.
   * Transformation of missing values in non-optional fields will trigger an error. The various
   * values in the enum control which components of a compound field reference are optional.
   */
  @Builder.Default
  private OptionalComponents optionalComponents = OptionalComponents.FieldAndProperty;

  /**
   * Name of the transformer to use when transforming/copying the value. Refer to {@link
   * gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil} for how this is mapped to actual
   * transformer instances.
   */
  @TransformerName private String transformer;

  /** Default value to use if a field has empty string value in the source object. */
  private String defaultValue;

  /**
   * Map of configuration option key/value pairs that can modify the default behavior of the
   * transformer. Which key/value pairs are appropriate depend on the transformer.
   */
  @Singular Map<String, String> transformerOptions = new HashMap<>();

  /**
   * Used to quickly determine if a transformation is used to indicate an array transformation.
   *
   * @return true if this is an array transformation
   */
  public boolean isArray() {
    return ArrayTransformName.equals(transformer);
  }

  /**
   * Determines the appropriate destination object field name to use. This will be the value of the
   * {@code to} property if one is defined. Otherwise it will be derived from the {@code from}
   * field. If the {@code from} is used and is a dotted name (i.e. property in a nested object
   * within the source) only the last component of the dotted name will be returned.
   *
   * @return appropriate name for accessing the field in the destination object
   */
  public String getTo() {
    if (to != null) {
      return to;
    } else {
      final int dotIndex = from.indexOf('.');
      if (dotIndex < 0) {
        return from;
      } else {
        return from.substring(dotIndex + 1);
      }
    }
  }

  /**
   * Indicates that the transformation is optional and could be skipped if the value is not present
   * in the message.
   *
   * @return true if the message might not contain a value
   */
  public boolean isOptional() {
    return optionalComponents != OptionalComponents.None;
  }

  /**
   * Determines whether a {@code transformer} has been defined.
   *
   * @return true if a transformer has been explicitly defined
   */
  public boolean hasTransformer() {
    return !Strings.isNullOrEmpty(transformer);
  }

  /**
   * Looks up a transformer option by name. If no matching option has been set this method will
   * throw an IllegalArgumentException.
   *
   * @param optionName name of the option
   * @return value of the option
   * @throws IllegalArgumentException if the option is not defined
   */
  public String findTransformerOption(String optionName) throws IllegalArgumentException {
    return transformerOption(optionName)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "reference to undefined option %s in transformation %s from %s",
                        optionName, transformer, from)));
  }

  /**
   * Looks up a transformer option by name.
   *
   * @param optionName name of the option
   * @return an {@link Optional} containing value if the option has been defined or empty if it has
   *     not
   */
  public Optional<String> transformerOption(String optionName) {
    if (transformerOptions == null || transformerOptions.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(transformerOptions.get(optionName)).map(String::trim);
    }
  }

  /**
   * Looks up a transformer option by name. The value is expected to be a comma separated list of
   * values. Creates a list containing each of the individual values.
   *
   * @param optionName name of the option
   * @return an {@link Optional} containing list of values if the option has been defined or empty
   *     if it has not
   */
  public Optional<List<String>> transformerListOption(String optionName) {
    return transformerOption(optionName).map(value -> List.of(value.split(" *, *")));
  }

  @Override
  public String getDescription() {
    return "transformation of " + from;
  }

  /** Enum that defines possible values for the {@link #optionalComponents} field. */
  public enum OptionalComponents {
    /** The field is optional. For compound fields the property within the field is not optional. */
    FieldOnly,
    /**
     * The field is not optional. For compound fields the field is not optional but the property
     * within the field is optional.
     */
    PropertyOnly,
    /**
     * The field is optional. For compound fields both the field and the property within the field
     * are optional.
     */
    FieldAndProperty,
    /**
     * The field is not optional. For compound fields neither the field nor the property within the
     * field are optional.
     */
    None
  }
}
