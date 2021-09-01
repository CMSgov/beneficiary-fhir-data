package gov.cms.bfd.pipeline.rda.grpc.sink;

/**
 * These exceptions are thrown by the ConfigLoader class when it encounters a requirement that is
 * not met. For example a file doesn't exist or an integer setting isn't a valid integer string.
 */
public class ConfigException extends RuntimeException {
  private final String name;

  public ConfigException(String name, String message) {
    super(message);
    this.name = name;
  }

  public ConfigException(String name, String message, Throwable cause) {
    super(message, cause);
    this.name = name;
  }

  /** @return The name of the configuration option that had an invalid value. */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("invalid option: name='%s' message='%s'", name, getMessage());
  }
}
