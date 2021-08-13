package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

/** Simple class to abstract loading configuration values from multiple sources. */
public class ConfigLoader {
  private final Function<String, String> source;

  public ConfigLoader(Function<String, String> source) {
    this.source = source;
  }

  /** @return a Builder object for creating ConfigLoader objects */
  public static ConfigLoader.Builder builder() {
    return new Builder();
  }

  /**
   * Gets a required configuration value.
   *
   * @param name name of configuration value
   * @return non-empty string value
   * @throws ConfigException if there is no non-empty value
   */
  public String stringValue(String name) {
    final String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      throw new ConfigException(name, "required option not provided");
    } else {
      return value;
    }
  }

  /**
   * Gets an optional configuration value or a defaultValue if there is no non-empty value.
   *
   * @param name name of configuration value
   * @return either the non-empty string value or defaultValue
   */
  public String stringValue(String name, String defaultValue) {
    final String value = source.apply(name);
    return Strings.isNullOrEmpty(value) ? defaultValue : value;
  }

  /**
   * Gets an Optional for the specified configuration value.
   *
   * @param name name of configuration value
   * @return empty Option if there is no non-empty value, otherwise Option holding the value
   */
  public Optional<String> stringOption(String name) {
    final String value = source.apply(name);
    return Strings.isNullOrEmpty(value) ? Optional.empty() : Optional.of(value);
  }

  /**
   * Gets a required integer configuration value.
   *
   * @param name name of configuration value
   * @return integer value
   * @throws ConfigException if there is no valid integer value
   */
  public int intValue(String name) {
    final String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      throw new ConfigException(name, "required option not provided");
    }
    try {
      return Integer.parseInt(value);
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid integer", ex);
    }
  }

  /**
   * Gets an optional integer configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @return either the integer value or defaultValue
   * @throws ConfigException if a value existed but was not a valid integer
   */
  public int intValue(String name, int defaultValue) {
    final String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid integer", ex);
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
    final String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Integer.parseInt(value));
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid integer", ex);
    }
  }

  /**
   * Gets a required enum configuration value.
   *
   * @param name name of configuration value
   * @return enum value
   * @throws ConfigException if there is no valid enum value
   */
  public <T extends Enum<T>> T enumValue(String name, Function<String, T> parser) {
    String value = stringValue(name);
    try {
      return parser.apply(value);
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
    file.ifPresent(
        f -> {
          if (!f.canRead()) {
            throw new ConfigException(name, "file is not readable");
          }
        });
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
    if (file.isFile()) {
      if (!file.canWrite()) {
        throw new ConfigException(name, "file is not writeable");
      }
    } else if (!file.getAbsoluteFile().getParentFile().canWrite()) {
      throw new ConfigException(name, "file does not exist and parent is not writeable");
    }
    return file;
  }

  /**
   * Gets an optional boolean configuration value or a defaultValue if there is no value.
   *
   * @param name name of configuration value
   * @return either the boolean value or defaultValue
   * @throws ConfigException if a value existed but it wasn't a valid boolean
   */
  public boolean booleanValue(String name, boolean defaultValue) {
    String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    switch (value.toLowerCase()) {
      case "true":
        return true;
      case "false":
        return false;
      default:
        throw new ConfigException(name, "invalid boolean value: " + value);
    }
  }

  public static class ConfigException extends RuntimeException {
    private final String name;

    public ConfigException(String name, String message) {
      super(message);
      this.name = name;
    }

    public ConfigException(String name, String message, Throwable cause) {
      super(message, cause);
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return String.format("invalid option: name='%s' message='%s'", name, getMessage());
    }
  }

  /**
   * Builder to construct ConfigLoader instances. Each call to add a source inserts the specified
   * source as the primary source and any old source becomes a fallback if the new one has no value.
   * Multiple calls can chain any number of sources in this way.
   */
  public static class Builder {
    private Function<String, String> source = ignored -> null;

    public ConfigLoader build() {
      return new ConfigLoader(source);
    }

    public Builder add(Function<String, String> newSource) {
      Function<String, String> oldSource = this.source;
      this.source =
          name -> {
            String value = newSource.apply(name);
            return Strings.isNullOrEmpty(value) ? oldSource.apply(name) : value;
          };
      return this;
    }

    public Builder addEnvironmentVariables() {
      return add(System::getenv);
    }

    public Builder addSystemProperties() {
      return add(System::getProperty);
    }

    public Builder addPropertiesFile(File propertiesFile) throws IOException {
      Properties props = new Properties();
      try (Reader in = new BufferedReader(new FileReader(propertiesFile))) {
        props.load(in);
      }
      return add(props::getProperty);
    }
  }
}
