package gov.cms.bfd.sharedutils.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;

/**
 * These exceptions are thrown by the {@link ConfigLoader} class when it encounters a requirement
 * that is not met. For example a file doesn't exist or an integer setting isn't a valid integer
 * string.
 */
public class ConfigException extends RuntimeException {

  /** Name of the configuration variable that triggered this exception. */
  @Getter private final String name;

  /** Detail of the particular error. */
  @Getter private final String detail;

  /**
   * Instantiates a new Config exception.
   *
   * @param name the name of the variable
   * @param detail describes the error
   */
  public ConfigException(String name, String detail) {
    super(createMessage(name, detail));
    this.name = name;
    this.detail = detail;
  }

  /**
   * Instantiates a new Config exception.
   *
   * @param name the name of the variable
   * @param detail describes the error
   * @param cause the cause
   */
  public ConfigException(String name, String detail, Throwable cause) {
    super(createMessage(name, detail), cause);
    this.name = name;
    this.detail = detail;
  }

  /**
   * Combines name and detail into a single error message suitable for return by {@link
   * #getMessage}.
   *
   * @param name the name of the variable
   * @param detail describes the error
   * @return the error message
   */
  @VisibleForTesting
  static String createMessage(String name, String detail) {
    return String.format("Configuration value error: name='%s' detail='%s'", name, detail);
  }
}
