package gov.cms.bfd.sharedutils.config;

/**
 * These exceptions are thrown by the {@link ConfigLoader} class when it encounters a requirement
 * that is not met. For example a file doesn't exist or an integer setting isn't a valid integer
 * string.
 */
public class ConfigException extends RuntimeException {

  /** The configuration value that triggered this exception. */
  private final String name;

  /**
   * Instantiates a new Config exception.
   *
   * @param name the name
   * @param message the message
   */
  public ConfigException(String name, String message) {
    super(message);
    this.name = name;
  }

  /**
   * Instantiates a new Config exception.
   *
   * @param name the name
   * @param message the message
   * @param cause the cause
   */
  public ConfigException(String name, String message, Throwable cause) {
    super(message, cause);
    this.name = name;
  }

  /**
   * Gets the name.
   *
   * @return The name
   */
  public String getName() {
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("invalid option: name='%s' message='%s'", name, getMessage());
  }
}
