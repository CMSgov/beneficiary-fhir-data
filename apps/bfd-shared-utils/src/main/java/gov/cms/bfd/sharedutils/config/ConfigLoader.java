package gov.cms.bfd.sharedutils.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Abstracts loading configuration values from one or more sources of key/value pairs. Provides
 * commonly needed methods to pull in configuration data and parse it various ways. A lambda
 * function or method reference can be used as the source of data (e.g. System::getenv or
 * myMap::get) or the Builder object can be used to construct chained sources that can use a basic
 * set of defaults but allow something else to override them selectively. For example using a Map of
 * default values but allow environment variables to override anything in the Map.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigLoader {
  /** Format string for {@link String#format} for unparseable value. */
  @VisibleForTesting
  private static final String NOT_VALID_PARSED = "not a valid %s value: exception=%s message=%s";

  /** Error message for missing required value. */
  @VisibleForTesting static final String NOT_PROVIDED = "required option not provided";

  /** Error message for non-positive integer. */
  public static final String NOT_POSITIVE_INTEGER = "not a positive integer";

  /** Error message for invalid hex strings. */
  @VisibleForTesting static final String NOT_VALID_HEX = "invalid hex string";

  /** Error message for invalid boolean. */
  private static final String NOT_VALID_BOOLEAN = "invalid boolean value";

  /** Used to find and remove leading and trailing spaces and tabs. */
  private static final Pattern TRIM_PATTERN = Pattern.compile("^([ \\t]*)(.*?)([ \\t]*)$");

  /** Group number for trimmed string group in {@link #TRIM_PATTERN}. */
  private static final int TRIM_PATTERN_CENTER_GROUP = 2;

  /** The data source to load data from. */
  private final ConfigLoaderSource source;

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
   * Returns a set containing all names that are known to have values.
   *
   * @return the set of keys
   */
  public Set<String> validNames() {
    return source.validNames();
  }

  /**
   * Returns the string values for the specified configuration data. Null values are converted into
   * empty strings. Strings have leading and trailing whitespace removed. Empty strings are
   * retained. If there are no values for the given name an empty immutable list is returned.
   *
   * @param name the name to look up
   * @return the values in a list
   */
  public List<String> stringValues(String name) {
    final Collection<String> values = source.lookup(name);

    if (values == null || values.isEmpty()) {
      throw new ConfigException(name, NOT_PROVIDED);
    } else {
      return ImmutableList.copyOf(values);
    }
  }

  /**
   * Returns the string values for the specified configuration data. Null values are converted into
   * empty strings. Strings have leading and trailing whitespace removed. Empty strings are
   * retained. If there are no values for the given name an immutable list containing the provided
   * default values is returned.
   *
   * @param name the name to look up
   * @param defaults the defaults for the values if no value found for name
   * @return immutable list containing the values for the specified name, using the specified
   *     defaults if no value was found
   */
  public List<String> stringValues(String name, Collection<String> defaults) {
    final Collection<String> values = source.lookup(name);

    return (values == null || values.isEmpty())
        ? ImmutableList.copyOf(defaults)
        : ImmutableList.copyOf(values);
  }

  /**
   * Gets a required configuration value.
   *
   * @param name name of configuration value
   * @return non-empty string value
   * @throws ConfigException if there is no non-empty value
   */
  public String stringValue(String name) {
    return stringOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Gets an optional configuration value or a defaultValue if there is no non-empty value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the non-empty string value or defaultValue
   */
  public String stringValue(String name, String defaultValue) {
    return stringOption(name).orElse(defaultValue);
  }

  /**
   * Gets an Optional for the specified configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no non-empty value, otherwise Option holding the value
   */
  public Optional<String> stringOption(String name) {
    final Collection<String> values = source.lookup(name);
    if (values == null || values.isEmpty()) {
      return Optional.empty();
    } else {
      return values.stream()
          .filter(Objects::nonNull)
          .map(ConfigLoader::trim)
          .filter(s -> !s.isEmpty())
          .findFirst();
    }
  }

  /**
   * Gets an Optional for the specified configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is a value, otherwise Option holding the value
   */
  public Optional<String> stringOptionEmptyOK(String name) {
    final Collection<String> values = source.lookup(name);
    if (values == null || values.isEmpty()) {
      return Optional.empty();
    } else {
      return values.stream().map(Strings::nullToEmpty).map(ConfigLoader::trim).findFirst();
    }
  }

  /**
   * Gets a required float configuration value.
   *
   * @param name name of configuration value
   * @return float value
   * @throws ConfigException if there is no valid float value
   */
  public float floatValue(String name) {
    return floatOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
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
    return floatOption(name).orElse(defaultValue);
  }

  /**
   * Gets an Optional for the specified float configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid float
   */
  public Optional<Float> floatOption(String name) {
    return parsedOption(name, Float.class, Float::parseFloat);
  }

  /**
   * Gets a required integer configuration value.
   *
   * @param name name of configuration value
   * @return integer value
   * @throws ConfigException if there is no valid integer value
   */
  public int intValue(String name) {
    return intOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Gets a required positive integer configuration value.
   *
   * @param name name of configuration value
   * @return integer value
   * @throws ConfigException if there is no valid integer value or value is not positive
   */
  public int positiveIntValue(String name) {
    return positiveIntOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Gets an optional positive integer configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @param defaultValue the default value
   * @return either the integer value or defaultValue
   * @throws ConfigException if a value existed but was not a valid positive integer
   */
  public int positiveIntValue(String name, int defaultValue) {
    return positiveIntOption(name).orElse(defaultValue);
  }

  /**
   * Gets an optional positive integer configuration value.
   *
   * @param name name of configuration value
   * @return optional integer value
   * @throws ConfigException if there is an integer value and it is not positive
   */
  public Optional<Integer> positiveIntOption(String name) {
    return intOption(name).map(x -> validate(name, x, NOT_POSITIVE_INTEGER, x > 0));
  }

  /**
   * Gets a required positive integer configuration value. Zero is considered valid even though it
   * is not strictly positive.
   *
   * @param name name of configuration value
   * @return integer value
   * @throws ConfigException if there is no valid integer value or value is not positive
   */
  public int positiveIntValueZeroOK(String name) {
    return positiveIntOptionZeroOK(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Gets an optional positive integer configuration value. Zero is considered valid even though it
   * is not strictly positive.
   *
   * @param name name of configuration value
   * @return optional integer value
   * @throws ConfigException if there is an integer value and it is not positive
   */
  public Optional<Integer> positiveIntOptionZeroOK(String name) {
    return intOption(name).map(x -> validate(name, x, NOT_POSITIVE_INTEGER, x >= 0));
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
    return intOption(name).orElse(defaultValue);
  }

  /**
   * Gets an Optional for the specified integer configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid integer
   */
  public Optional<Integer> intOption(String name) {
    return parsedOption(name, Integer.class, Integer::parseInt);
  }

  /**
   * Gets an Optional for the specified long configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no value, otherwise Option holding the value
   * @throws ConfigException if a value existed but was not a valid long
   */
  public Optional<Long> longOption(String name) {
    return parsedOption(name, Long.class, Long::parseLong);
  }

  /**
   * Gets a required enum configuration value.
   *
   * @param <T> the type parameter
   * @param name name of configuration value
   * @param klass the enum class
   * @return enum value
   * @throws ConfigException if there is no valid enum value
   */
  public <T extends Enum<T>> T enumValue(String name, Class<T> klass) {
    return enumOption(name, klass).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Gets an optional enum configuration value.
   *
   * @param <T> the type parameter
   * @param name name of configuration value
   * @param klass the enum class
   * @return Optional enum value
   * @throws ConfigException if there is no valid enum value
   */
  public <T extends Enum<T>> Optional<T> enumOption(String name, Class<T> klass) {
    return parsedOption(name, klass, s -> Enum.valueOf(klass, s));
  }

  /**
   * Gets a required File configuration value.
   *
   * @param name name of configuration value
   * @return File value
   * @throws ConfigException if there is no value or the file is not readable
   */
  public File readableFile(String name) {
    return readableFileOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
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
   * Gets an optional boolean configuration value.
   *
   * @param name name of configuration value
   * @return either the boolean value or empty
   * @throws ConfigException if a value existed but it wasn't a valid boolean
   */
  public Optional<Boolean> booleanOption(String name) {
    return parsedOption(name, Boolean.class, ConfigLoader::parseBoolean);
  }

  /**
   * Gets an optional boolean configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @return either the boolean value or defaultValue
   * @throws ConfigException if a value was missing or it wasn't a valid boolean
   */
  public boolean booleanValue(String name) {
    return booleanOption(name).orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
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
    return booleanOption(name).orElse(defaultValue);
  }

  /**
   * Gets a required hex encoded binary configuration value.
   *
   * @param name name of configuration value
   * @return the decoded byte array
   * @throws ConfigException if a value is missing or could not be decoded
   */
  public byte[] hexBytes(String name) {
    final var hexString = stringValue(name);
    try {
      return Hex.decodeHex(hexString.toCharArray());
    } catch (DecoderException e) {
      throw new ConfigException(name, NOT_VALID_HEX, e);
    }
  }

  /**
   * Gets an optional configuration value and parses it into an object using provided function.
   *
   * @param name name of configuration value
   * @param klass class of object returned by the parser
   * @param parser function that parses a string
   * @return the parsed object
   * @throws ConfigException if a value is missing or could not be parsed
   * @param <T> type returned by the parser
   */
  public <T> Optional<T> parsedOption(String name, Class<T> klass, Function<String, T> parser) {
    return stringOption(name).map(source -> parseString(name, source, klass, parser));
  }

  /**
   * Gets a required configuration value and parses it into an object using provided function.
   *
   * @param name name of configuration value
   * @param klass class of object returned by the parser
   * @param parser function that parses a string
   * @return the parsed object
   * @throws ConfigException if a value is missing or could not be parsed
   * @param <T> type returned by the parser
   */
  public <T> T parsedValue(String name, Class<T> klass, Function<String, T> parser) {
    return parsedOption(name, klass, parser)
        .orElseThrow(() -> new ConfigException(name, NOT_PROVIDED));
  }

  /**
   * Suitable for use in a {@link Optional#map} call to validate that a condition is true.
   *
   * @param name name of the value
   * @param value the value being tested
   * @param errorMessage error message in case condition is false
   * @param isValid condition to check
   * @return the value if condition is true
   * @param <T> type of value being checked
   * @throws ConfigException if the condition is false
   */
  private static <T> T validate(String name, T value, String errorMessage, boolean isValid) {
    if (!isValid) {
      throw new ConfigException(name, errorMessage);
    }
    return value;
  }

  /**
   * More robust version of {@link Boolean#parseBoolean}.
   *
   * @param s string to parse
   * @return boolean value
   * @throws IllegalArgumentException if string is invalid
   */
  private static Boolean parseBoolean(String s) {
    return switch (s.toLowerCase()) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalArgumentException(NOT_VALID_BOOLEAN);
    };
  }

  /**
   * Calls a parsing function to parse a string..
   *
   * @param name name of configuration value
   * @param source the string to parse
   * @param klass class of object returned by the parser
   * @param parser function that parses a string
   * @return the parsed object
   * @throws ConfigException if a value is missing or could not be parsed
   * @param <T> type returned by the parser
   */
  private <T> T parseString(
      String name, String source, Class<T> klass, Function<String, T> parser) {
    try {
      return parser.apply(source);
    } catch (RuntimeException e) {
      final var message = parseFailedMessage(source, klass, e);
      throw new ConfigException(name, message);
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
   * Removes spaces and tabs from the beginning and end of a string.
   *
   * @param rawString string to be modified
   * @return the resulting string or same if no changes needed
   */
  @VisibleForTesting
  static String trim(String rawString) {
    var matcher = TRIM_PATTERN.matcher(rawString);
    return matcher.matches() ? matcher.group(TRIM_PATTERN_CENTER_GROUP) : rawString;
  }

  /**
   * Create an exception message for a parse failure. Filters out the source value from the error's
   * message so we can't accidentally leak sensitive configuration values to log files.
   *
   * @param sourceValue source that failed to parse
   * @param klass class we tried to parse into
   * @param error exception that was trigger when parsin
   * @return helpful error message describing the failure
   */
  private static String parseFailedMessage(String sourceValue, Class<?> klass, Exception error) {
    var reasonMessage = error.getMessage();
    if (!Strings.isNullOrEmpty(sourceValue)) {
      reasonMessage = reasonMessage.replaceAll(sourceValue, "***");
    }
    return String.format(
        NOT_VALID_PARSED, klass.getSimpleName(), error.getClass().getSimpleName(), reasonMessage);
  }

  /**
   * Builder to construct ConfigLoader instances. Each call to add a source inserts the specified
   * source as the primary source and any old source becomes a fallback if the new one has no value.
   * Multiple calls can chain any number of sources in this way. All methods return the Builder so
   * that calls can be chained.
   */
  public static class Builder {
    /**
     * The data sources to load data from. New layers are added to the end of the list and take
     * priority over sources added previously.
     */
    private final List<ConfigLoaderSource> sources = new ArrayList<>();

    /**
     * Builds a new {@link ConfigLoader}.
     *
     * @return the config loader
     */
    public ConfigLoader build() {
      final var combinedSources =
          sources.size() == 1 ? sources.get(0) : ConfigLoaderSource.fromPrioritizedSources(sources);
      return new ConfigLoader(combinedSources);
    }

    /**
     * Adds a new configuration source that takes precedence over the current source.
     *
     * @param newSource the source to add
     * @return the builder for chaining
     */
    public Builder add(ConfigLoaderSource newSource) {
      sources.add(newSource);
      return this;
    }

    /**
     * Adds a source that pulls values from environment variables.
     *
     * @return the builder for chaining
     */
    public Builder addEnvironmentVariables() {
      return add(ConfigLoaderSource.fromEnv());
    }

    /**
     * Adds a source that pulls values from system properties.
     *
     * @return the builder for chaining
     */
    public Builder addSystemProperties() {
      return add(ConfigLoaderSource.fromProperties(System.getProperties()));
    }

    /**
     * Call this after adding some other source to add an extra source that will look in the most
     * recently added source using a name with {@link
     * ConfigLoaderSource#fromOtherUsingSsmToPropertyMapping}. Can be used to allow SSM parameter
     * paths to be found in a java properties source using a java properties compatible name.
     *
     * @param propertyNamePrefix Prefix to add when mapping SSM name into property name.
     * @return the builder for chaining
     */
    public Builder alsoWithSsmToPropertyMapping(String propertyNamePrefix) {
      if (sources.isEmpty()) {
        throw new BadCodeMonkeyException("No sources have been added yet.");
      }
      final var previousSource = sources.getLast();
      final var mappedSource =
          ConfigLoaderSource.fromOtherUsingSsmToPropertyMapping(propertyNamePrefix, previousSource);
      return add(mappedSource);
    }

    /**
     * Call this after adding some other source to add an extra source that will look in the most
     * recently added source using a name with {@link
     * ConfigLoaderSource#fromOtherUsingSsmToEnvVarMapping}. Can be used to allow SSM parameter
     * paths to be found in an environment variable source using an environment variable compatible
     * name.
     *
     * @param envVarNamePrefix Prefix to add when mapping SSM name into an environment variable
     *     name.
     * @return the builder for chaining
     */
    public Builder alsoWithSsmToEnvVarMapping(String envVarNamePrefix) {
      if (sources.isEmpty()) {
        throw new BadCodeMonkeyException("No sources have been added yet.");
      }
      final var previousSource = sources.getLast();
      final var mappedSource =
          ConfigLoaderSource.fromOtherUsingSsmToEnvVarMapping(envVarNamePrefix, previousSource);
      return add(mappedSource);
    }

    /**
     * Adds a source that pulls values from the specified properties object.
     *
     * @param properties source of properties
     * @return the builder for chaining
     */
    public Builder addProperties(Properties properties) {
      return add(ConfigLoaderSource.fromProperties(properties));
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
     * Adds a lookup function that retrieves values from the specified {@link Map}.
     *
     * @param valuesMap source of values
     * @return this builder
     */
    public Builder addMap(Map<String, String> valuesMap) {
      return add(ConfigLoaderSource.fromMap(valuesMap));
    }

    /**
     * Adds a lookup function that retrieves values from the specified {@link Map} of collections.
     * An empty collection for any name is treated the same as if there was no collection at all for
     * that name.
     *
     * @param valuesMap source of values
     * @return this builder
     */
    public Builder addMultiMap(Map<String, ? extends Collection<String>> valuesMap) {
      return add(ConfigLoaderSource.fromMultiMap(valuesMap));
    }

    /**
     * Adds key value pairs from an array of strings. Used with command line arguments array to pull
     * in options like key:value. Puts them into a Map and adds the Map's get method as a source.
     *
     * @param args command line arguments of the form key:value
     * @return the builder with the arguments mapped
     */
    public Builder addKeyValueCommandLineArguments(String[] args) {
      final var map = ImmutableMap.<String, String>builder();
      for (String arg : args) {
        final int prefixEnd = arg.indexOf(":");
        if (prefixEnd > 0) {
          map.put(arg.substring(0, prefixEnd), arg.substring(prefixEnd + 1));
        }
      }
      return addMap(map.build());
    }
  }
}
