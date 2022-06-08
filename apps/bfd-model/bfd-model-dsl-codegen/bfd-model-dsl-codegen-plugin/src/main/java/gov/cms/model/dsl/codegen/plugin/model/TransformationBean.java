package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
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
public class TransformationBean {
  /** Name of the field/property in the source object to transform. */
  private String from;
  /**
   * Name of the field/property in the destination object to copy the data to. Can be omitted (left
   * null) if the destination field name is identical to the source field name.
   */
  private String to;
  /**
   * Specifies whether a value in the source object is optional. Optional values can be null or
   * empty in the source without triggering an error. Transformation of missing values in
   * non-optional fields will trigger an error.
   */
  @Builder.Default private boolean optional = true;
  /**
   * Name of the transformer to use when transforming/copying the value. Refer to {@link
   * gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil} for how this is mapped to actual
   * transformer instances.
   */
  private String transformer;
  /** Default value to use if a field is optional and has no value in the source object. */
  private String defaultValue;
  /**
   * Map of configuration option key/value pairs that can modify the default behavior of the
   * transformer. Which key/value pairs are appropriate depend on the transformer.
   */
  @Singular Map<String, String> transformerOptions = new HashMap<>();

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
   * Looks up a transformer option by name. The value is expetced to be a comma separated list of
   * values. Creates a list containing each of the individual values.
   *
   * @param optionName name of the option
   * @return an {@link Optional} containing list of values if the option has been defined or empty
   *     if it has not
   */
  public Optional<List<String>> transformerListOption(String optionName) {
    return transformerOption(optionName).map(value -> List.of(value.split(" *, *")));
  }
}
