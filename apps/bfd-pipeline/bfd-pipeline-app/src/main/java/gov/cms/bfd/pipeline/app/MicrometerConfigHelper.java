package gov.cms.bfd.pipeline.app;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.config.validate.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Micrometer uses interfaces for config objects and does not provide concrete implementation
 * classes for them. Since t This helper class provides facilities needed to create concrete
 * implementations that share common behavior.
 */
public class MicrometerConfigHelper {
  /** Maps property names to names compatible with the lookup function. */
  private final Map<String, String> configKeyMap;
  /** Maps names to default values. Names are those passed to the lookup function. */
  private final Map<String, String> defaultValuesMap;
  /**
   * Maps names to values in some way. Usually just {@code System::getenv} but in tests might be
   * {@code Map::get}.
   */
  private final Function<String, String> valueLookupFunction;

  /**
   * Constructs a new instance. A function is used to do environment variable lookup so that any
   * source of values can be used (for testing, etc).
   *
   * @param configKeyMap maps property names to environment variable names
   * @param defaultValuesMap provides default values for environment variables
   * @param valueLookupFunction used to look up environment variables
   */
  public MicrometerConfigHelper(
      Map<String, String> configKeyMap,
      Map<String, String> defaultValuesMap,
      Function<String, String> valueLookupFunction) {
    this.configKeyMap = configKeyMap;
    this.defaultValuesMap = defaultValuesMap;
    this.valueLookupFunction = valueLookupFunction;
  }

  /**
   * Map the property name to a lookup name, look up the value or a default value. Intended for use
   * in implementing the meter config's get method.
   *
   * @param propertyName name of config property
   * @return value or null if there is no value for the requested property
   */
  public String get(String propertyName) {
    return renameProperty(propertyName).flatMap(this::lookupKey).orElse(null);
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
      String envVarName = renameProperty(failure.getProperty()).orElse("unmatched");
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
   * Converts a property name into a name compatible with the lookup function.
   *
   * @param propertyName name of a meter config property
   * @return corresponding name safe for use with lookup function
   */
  @VisibleForTesting
  Optional<String> renameProperty(String propertyName) {
    return Optional.ofNullable(configKeyMap.get(propertyName));
  }

  /**
   * Looks up a value using the lookup function. If no value is found returns either a default value
   * (if defined) or an empty value.
   *
   * @param key lookup function compatible key
   * @return empty if no value is found or the value if one is found
   */
  @VisibleForTesting
  Optional<String> lookupKey(String key) {
    String value = valueLookupFunction.apply(key);
    if (value == null) {
      value = defaultValuesMap.get(key);
    }
    return Optional.ofNullable(value);
  }
}
