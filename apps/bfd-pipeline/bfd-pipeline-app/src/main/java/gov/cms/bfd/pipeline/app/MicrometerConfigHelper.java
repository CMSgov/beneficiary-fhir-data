package gov.cms.bfd.pipeline.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.config.validate.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;

/**
 * Micrometer uses interfaces for config objects and does not provide concrete implementation
 * classes for them. This helper class provides facilities needed to create concrete implementations
 * that can read properties from environment variables with different names and default values.
 */
@AllArgsConstructor
public class MicrometerConfigHelper {
  /** Maps property names to {@link MicrometerConfigHelper.PropertyMapping}s. */
  private final Map<String, PropertyMapping> propertiesByName;

  /**
   * Maps names to values in some way. Usually just {@code System#getenv} but in tests might be
   * {@code Map#get}.
   */
  private final Function<String, String> valueLookupFunction;

  /** * boolean flag for initialization. */
  private final boolean initialized;

  /**
   * Constructs a new instance. A function is used to do environment variable lookup so that any
   * source of values can be used (for testing, etc).
   *
   * @param propertyMappings list of mappings {@link PropertyMapping} defines supported properties
   * @param valueLookupFunction used to look up environment variables
   */
  public MicrometerConfigHelper(
      List<PropertyMapping> propertyMappings, Function<String, String> valueLookupFunction) {
    this(
        propertyMappings.stream()
            .collect(ImmutableMap.toImmutableMap(pm -> pm.propertyName, pm -> pm)),
        valueLookupFunction,
        true);
  }

  /**
   * Creates a copy of this object that differs only in the value lookup function. Intended for use
   * in unit tests to plug in a testable lookup function (like {@link Map#get}) instead of an
   * untestable one like {@link System#getenv}.
   *
   * @param valueLookupFunction replacement lookup function
   * @return instance with same config and default maps but new lookup function
   */
  @VisibleForTesting
  MicrometerConfigHelper withValueLookupFunction(Function<String, String> valueLookupFunction) {
    return new MicrometerConfigHelper(propertiesByName, valueLookupFunction, initialized);
  }

  /**
   * Maps the Micrometer property name to a lookup name, looks up the value using the lookup name,
   * and returns the value or a default value if the lookup fails. Unsupported properties also
   * return null. Intended for use in implementing the {@link
   * io.micrometer.core.instrument.config.MeterRegistryConfig#get} method.
   *
   * @param propertyName name of micrometer config property
   * @return value or null if there is no value for the requested property or property is
   *     unsupported
   */
  public String get(String propertyName) {
    return findProperty(propertyName).flatMap(this::lookupKey).orElse(null);
  }

  /**
   * Throws an exception is any validation failures are present. Otherwise does nothing.
   *
   * @param result result from meter configs validate method
   * @throws AppConfigurationException if validation failures are present
   */
  public void throwIfConfigurationNotValid(Validated<?> result) {
    List<String> messages = new ArrayList<>();
    for (Validated.Invalid<?> failure : result.failures()) {
      String envVarName =
          findProperty(failure.getProperty()).map(pm -> pm.lookupVariableName).orElse("unmatched");
      messages.add(
          String.format(
              "'%s'/'%s': '%s'", envVarName, failure.getProperty(), failure.getMessage()));
    }
    if (messages.size() > 0) {
      final var errorMessage =
          String.format(
              "Invalid value for %d configuration environment variable(s): %s",
              messages.size(), String.join(", ", messages));
      throw new AppConfigurationException(errorMessage, new ValidationException(result));
    }
  }

  /**
   * Finds the {@link PropertyMapping} corresponding to a Micrometer config property name.
   *
   * @param propertyName name of a Micrometer config property
   * @return corresponding {@link PropertyMapping} or empty if none matches the property name
   */
  @VisibleForTesting
  Optional<PropertyMapping> findProperty(String propertyName) {
    return Optional.ofNullable(propertiesByName.get(propertyName));
  }

  /**
   * Looks up a value using the lookup function. If no value is found returns either a default value
   * (if defined) or an empty value.
   *
   * @param propertyMapping defines the property to look up
   * @return empty if no value is found or the value if one is found
   */
  @VisibleForTesting
  Optional<String> lookupKey(PropertyMapping propertyMapping) {
    final String value = valueLookupFunction.apply(propertyMapping.lookupVariableName);
    return Optional.ofNullable(value).or(() -> propertyMapping.defaultValue);
  }

  /**
   * Mappings determine how micrometer property names are mapped to lookup (usually environment
   * variable) names and, optionally, a default value to use when the lookup returns null.
   */
  @AllArgsConstructor
  public static class PropertyMapping {
    /** Property name used by micrometer config objects. */
    private final String propertyName;

    /** Lookup (env var) name used to look up the object. */
    private final String lookupVariableName;

    /** Default value (if any) for value if not found during lookup. */
    private final Optional<String> defaultValue;
  }
}
