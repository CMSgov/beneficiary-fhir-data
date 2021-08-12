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

public class ConfigLoader {
  private final Function<String, String> source;

  public ConfigLoader(Function<String, String> source) {
    this.source = source;
  }

  public static ConfigLoader fromEnvironmentVariables() {
    return new ConfigLoader(System::getenv);
  }

  public static ConfigLoader fromSystemProperties() {
    return new ConfigLoader(System::getProperty);
  }

  public ConfigLoader withFallback(ConfigLoader fallback) {
    return new ConfigLoader(
        name -> {
          String value = source.apply(name);
          return Strings.isNullOrEmpty(value) ? fallback.source.apply(name) : value;
        });
  }

  public static ConfigLoader fromPropertiesFile(File propertiesFile) throws IOException {
    Properties props = new Properties();
    try (Reader in = new BufferedReader(new FileReader(propertiesFile))) {
      props.load(in);
    }
    return new ConfigLoader(props::getProperty);
  }

  public String stringValue(String name) {
    final String value = source.apply(name);
    if (Strings.isNullOrEmpty(value)) {
      throw new ConfigException(name, "required option not provided");
    } else {
      return value;
    }
  }

  public String stringValue(String name, String defaultValue) {
    final String value = source.apply(name);
    return Strings.isNullOrEmpty(value) ? defaultValue : value;
  }

  public Optional<String> stringOption(String name) {
    final String value = source.apply(name);
    return Strings.isNullOrEmpty(value) ? Optional.empty() : Optional.of(value);
  }

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

  public <T extends Enum<T>> T enumValue(String name, Function<String, T> parser) {
    String value = stringValue(name);
    try {
      return parser.apply(value);
    } catch (Exception ex) {
      throw new ConfigException(name, "not a valid enum value: " + ex.getMessage(), ex);
    }
  }

  public File readableFile(String name) {
    return readableFileOption(name)
        .orElseThrow(() -> new ConfigException(name, "required option not provided"));
  }

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

    public String getErrorMessage() {
      return String.format("invalid option: name='%s' message='%s'", name, getMessage());
    }
  }
}
