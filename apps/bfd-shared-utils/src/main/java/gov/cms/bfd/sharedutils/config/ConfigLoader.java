package gov.cms.bfd.sharedutils.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

/**
 * Abstracts loading configuration values from one or more sources of key/value pairs. Provides
 * commonly needed methods to pull in configuration data and parse it various ways. A lambda
 * function or method reference can be used as the source of data (e.g. System::getenv or
 * myMap::get) or the Builder object can be used to construct chained sources that can use a basic
 * set of defaults but allow something else to override them selectively. For example using a Map of
 * default values but allow environment variables to override anything in the Map.
 */
public class ConfigLoader {

  /**
   * The data source to load data from. A lambda function or method reference can be used as the
   * source of data (e.g. System::getenv or myMap::get).
   */
  private final Function<String, Collection<String>> source;

  /** Error message for invalid integer. */
  private static final String NOT_VALID_INTEGER = "not a valid integer";

  /** Error message for invalid float. */
  private static final String NOT_VALID_FLOAT = "not a valid float";

  /**
   * Constructs a ConfigLoader that uses the provided Function as the source of key/value
   * configuration data. The function will be called whenever a specific configuration value is
   * needed. It can return null to indicate that no value is available for the requested key.
   *
   * @param source function used to obtain key/value configuration data
   */
  public ConfigLoader(Function<String, Collection<String>> source) {
    this.source = source;
  }

  /**
   * Creates a Builder that can be used to combine multiple sources of configuration data into a
   * prioritized chain.
   *
   * @return a Builder object for creating ConfigLoader objects.
   */
  public static ConfigLoader.Builder builder() {
    return new Builder();
  }

  /**
   * Returns the string values for the specified configuration data.
   *
   * @param name the name to look up
   * @return the values in a list
   */
  public List<String> stringValues(String name) {
    final Collection<String> values = source.apply(name);

    if (values == null || values.isEmpty()) {
      throw new ConfigException(name, "required option not provided");
    } else {
      return new ArrayList<>(values);
    }
  }

  /**
   * Returns the string values for the specified configuration data.
   *
   * @param name the name to look up
   * @param defaults the defaults for the values if no value found for name
   * @return the values for the specified name, using the specified defaults if no value was found
   */
  public List<String> stringValues(String name, Collection<String> defaults) {
    final Collection<String> values = source.apply(name);

    return (values == null || values.isEmpty())
        ? new ArrayList<>(defaults)
        : new ArrayList<>(values);
  }

  /**
   * Gets a required configuration value.
   *
   * @param name name of configuration value
   * @return non-empty string value
   * @throws ConfigException if there is no non-empty value
   */
  public String stringValue(String name) {
    return stringValues(name).get(0);
  }

  /**
   * Gets an optional configuration value or a defaultValue if there is no non-empty value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the non-empty string value or defaultValue
   */
  public String stringValue(String name, String defaultValue) {
    return stringValues(name, Collections.singletonList(defaultValue)).get(0);
  }

  /**
   * Gets an optonal configuration value list.
   *
   * @param name the name of configuration value
   * @return the optional list of string values for the name
   */
  public Optional<List<String>> stringsOption(String name) {
    final Collection<String> values = source.apply(name);

    return (values == null || values.isEmpty())
        ? Optional.empty()
        : Optional.of(new ArrayList<>(values));
  }

  /**
   * Gets an Optional for the specified configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no non-empty value, otherwise Option holding the value
   */
  public Optional<String> stringOption(String name) {
    Optional<List<String>> optional = stringsOption(name);

    return optional.map(strings -> strings.get(0));
  }

  /**
   * Gets a required float configuration value.
   *
   * @param name name of configuration value
   * @return float value
   * @throws ConfigException if there is no valid float value
   */
  public float floatValue(String name) {
    final String value = stringValue(name);

    try {
      return Float.parseFloat(value);
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_FLOAT, ex);
    }
  }

  /**
   * Gets an optional float configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the float value or defaultValue
   * @throws ConfigException if a value existed but was not a valid float
   */
  public float floatValue(String name, float defaultValue) {
    Optional<String> optional = stringOption(name);

    if (optional.isEmpty()) {
      return defaultValue;
    }

    try {
      return Float.parseFloat(optional.get());
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_FLOAT, ex);
    }
  }

  /**
   * Gets an Optional for the specified float configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid float
   */
  public Optional<Float> floatOption(String name) {
    try {
      return stringOption(name).map(Float::parseFloat);
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_FLOAT, ex);
    }
  }

  /**
   * Gets a required integer configuration value.
   *
   * @param name name of configuration value
   * @return integer value
   * @throws ConfigException if there is no valid integer value
   */
  public int intValue(String name) {
    final String value = stringValue(name);

    try {
      return Integer.parseInt(value);
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_INTEGER, ex);
    }
  }

  /**
   * Gets an optional integer configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the integer value or defaultValue
   * @throws ConfigException if a value existed but was not a valid integer
   */
  public int intValue(String name, int defaultValue) {
    Optional<String> optional = stringOption(name);

    if (!optional.isPresent()) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(optional.get());
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_INTEGER, ex);
    }
  }

  /**
   * Gets an Optional for the specified integer configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid integer
   */
  public Optional<Integer> intOption(String name) {
    try {
      return stringOption(name).map(Integer::parseInt);
    } catch (Exception ex) {
      throw new ConfigException(name, NOT_VALID_INTEGER, ex);
    }
  }

  /**
   * Gets an Optional for the specified long configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid long
   */
  public Optional<Long> longOption(String name) {
    try {
      return stringOption(name).map(Long::parseLong);
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid long", ex);
    }
  }

  /**
   * Gets a required enum configuration value.
   *
   * @param <T> the type parameter
   * @param name name of configuration value
   * @param parser the function to parse the enum with
   * @return enum value
   * @throws ConfigException if there is no valid enum value
   */
  public <T extends Enum<T>> T enumValue(String name, Function<String, T> parser) {
    return enumOption(name, parser)
        .orElseThrow(() -> new ConfigException(name, "required enum not provided"));
  }

  /**
   * Gets an optional enum configuration value.
   *
   * @param <T> the type parameter
   * @param name name of configuration value
   * @param parser the function to parse the enum with
   * @return Optional enum value
   * @throws ConfigException if there is no valid enum value
   */
  public <T extends Enum<T>> Optional<T> enumOption(String name, Function<String, T> parser) {
    try {
      return stringOption(name).map(parser);
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid enum value: " + ex.getMessage(), ex);
    }
  }

  /**
   * Gets a required File configuration value.
   *
   * @param name name of configuration value
   * @return File value
   * @throws ConfigException if there is no value or the file is not readable
   */
  public File readableFile(String name) {
    return readableFileOption(name)
        .orElseThrow(() -> new ConfigException(name, "required option not provided"));
  }

  /**
   * Gets an Optional for the specified File configuration value.
   *
   * @param name name of configuration value
   * @return Optional containing File value or empty if no file specified
   * @throws ConfigException if there is no value or the file is not readable
   */
  public Optional<File> readableFileOption(String name) {
    Optional<File> file = stringOption(name).map(File::new);
    file.ifPresent(f -> validateReadableFile(name, f));
    return file;
  }

  /**
   * Gets a required File configuration value. Either the file must exist and be writeable or else
   * its parent directory must exist and be writeable.
   *
   * @param name name of configuration value
   * @return File value
   * @throws ConfigException if there is no value or the file/parent is not writeable
   */
  public File writeableFile(String name) {
    String value = stringValue(name);
    File file = new File(value);
    return validateWriteableFile(name, file);
  }

  /**
   * Gets an optional boolean configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the boolean value or defaultValue
   * @throws ConfigException if a value existed but it wasn't a valid boolean
   */
  public boolean booleanValue(String name, boolean defaultValue) {
    Optional<String> value = stringOption(name);

    if (!value.isPresent()) {
      return defaultValue;
    }

    switch (value.get().toLowerCase()) {
      case "true":
        return true;
      case "false":
        return false;
      default:
        throw new ConfigException(name, "invalid boolean value: " + value);
    }
  }

  /**
   * Verifies that the specified file is readable. Returns the file if the file is readable.
   * Otherwise, throws a ConfigException.
   *
   * @param name name of configuration value that created this file
   * @param file the file object to be validated
   * @return the original File object
   * @throws ConfigException if the file is not readable
   */
  @VisibleForTesting
  File validateReadableFile(String name, File file) throws ConfigException {
    try {
      if (!file.isFile()) {
        throw new ConfigException(name, "object referenced by path is not a file");
      }
      if (!file.canRead()) {
        throw new ConfigException(name, "file is not readable");
      }
    } catch (ConfigException ex) {
      // pass our own exception through
      throw ex;
    } catch (Exception ex) {
      // wrap any other exception
      throw new ConfigException(name, "attempt to validate the file failed with an exception", ex);
    }
    return file;
  }

  /**
   * Verifies that the specified file is writeable. Either the file must exist and be writeable or
   * else its parent directory must exist and be writeable. Returns the file if the file is
   * writeable. Otherwise, throws a ConfigException.
   *
   * @param name name of configuration value that created this file
   * @param file the file object to be validated
   * @return the original File object
   * @throws ConfigException if the file/parent is not writeable
   */
  @VisibleForTesting
  File validateWriteableFile(String name, File file) throws ConfigException {
    try {
      if (file.exists()) {
        if (!file.isFile()) {
          throw new ConfigException(name, "object referenced by path is not a file");
        } else if (!file.canWrite()) {
          throw new ConfigException(name, "file is not writeable");
        }
      } else if (!file.getAbsoluteFile().getParentFile().canWrite()) {
        throw new ConfigException(name, "file does not exist and parent is not writeable");
      }
    } catch (ConfigException ex) {
      // pass our own exception through
      throw ex;
    } catch (Exception ex) {
      throw new ConfigException(name, "attempt to validate the file failed with an exception", ex);
    }
    return file;
  }

  /**
   * Builder to construct ConfigLoader instances. Each call to add a source inserts the specified
   * source as the primary source and any old source becomes a fallback if the new one has no value.
   * Multiple calls can chain any number of sources in this way. All methods return the Builder so
   * that calls can be chained.
   */
  public static class Builder {

    /**
     * The data source to load data from. A lambda function or method reference can be used as the
     * source of data (e.g. System::getenv or myMap::get).
     */
    private Function<String, Collection<String>> source = ignored -> null;

    /**
     * Builds a new {@link ConfigLoader}.
     *
     * @return the config loader
     */
    public ConfigLoader build() {
      return new ConfigLoader(source);
    }

    /**
     * Adds a configuration collection by copying the input source configuration.
     *
     * @param newSource the source to add
     * @return the builder for chaining
     */
    public Builder add(Function<String, Collection<String>> newSource) {
      Function<String, Collection<String>> oldSource = this.source;
      this.source =
          name -> {
            Collection<String> values = newSource.apply(name);
            return (values == null || values.isEmpty()) ? oldSource.apply(name) : values;
          };
      return this;
    }

    /**
     * Adds a single configuration by copying the value of the input source configuration.
     *
     * @param newSource the source to add
     * @return the builder for chaining
     */
    public Builder addSingle(Function<String, String> newSource) {
      Function<String, Collection<String>> wrappedNewSource =
          name -> {
            String value = newSource.apply(name);

            return (Strings.isNullOrEmpty(value)) ? null : Collections.singletonList(value);
          };

      return add(wrappedNewSource);
    }

    /**
     * Adds a source that pulls values from environment variables.
     *
     * @return the builder for chaining
     */
    public Builder addEnvironmentVariables() {
      return addSingle(System::getenv);
    }

    /**
     * Adds a source that pulls values from system properties.
     *
     * @return the builder for chaining
     */
    public Builder addSystemProperties() {
      return addSingle(System::getProperty);
    }

    /**
     * Adds a source that pulls values from the specified properties object.
     *
     * @param properties source of properties
     * @return the builder for chaining
     */
    public Builder addProperties(Properties properties) {
      return addSingle(properties::getProperty);
    }

    /**
     * Reads properties from the specified file and adds the resulting Properties object as a
     * source. The file must exist and must be a valid Properties file.
     *
     * @param propertiesFile normal java Properties file
     * @return the builder for chaining
     * @throws IOException if reading the file failed
     */
    public Builder addPropertiesFile(File propertiesFile) throws IOException {
      Properties props = new Properties();
      try (Reader in = new BufferedReader(new FileReader(propertiesFile))) {
        props.load(in);
      }
      return addProperties(props);
    }

    /**
     * Adds key value pairs from an array of strings. Used with command line arguments array to pull
     * in options like key:value. Puts them into a Map and adds the Map's get method as a source.
     *
     * @param args command line arguments of the form key:value
     * @return the builder with the arguments mapped
     */
    public Builder addKeyValueCommandLineArguments(String[] args) {
      Map<String, String> map = new HashMap<>();
      for (String arg : args) {
        int prefixEnd = arg.indexOf(":");
        if (prefixEnd > 0) {
          map.put(arg.substring(0, prefixEnd), arg.substring(prefixEnd + 1));
        }
      }
      return addSingle(map::get);
    }
  }
}
