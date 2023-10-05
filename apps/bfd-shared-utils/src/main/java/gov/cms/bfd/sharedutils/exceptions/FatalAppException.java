package gov.cms.bfd.sharedutils.exceptions;

import lombok.Getter;

/**
 * Thrown to inform the {@code main} method of an application that app should be shut down with a
 * specific exit code.
 */
public class FatalAppException extends Exception {
  /** Code to pass to {@link System#exit}. */
  @Getter private final int exitCode;

  /**
   * Initializes an instance.
   *
   * @param message Error message to log on exit.
   * @param exitCode Exit code for the application.
   */
  public FatalAppException(String message, int exitCode) {
    super(message);
    this.exitCode = exitCode;
  }

  /**
   * Initializes an instance.
   *
   * @param message Error message to log on exit.
   * @param cause Exception that triggered the shutdown.
   * @param exitCode Exit code for the application.
   */
  public FatalAppException(String message, Throwable cause, int exitCode) {
    super(message, cause);
    this.exitCode = exitCode;
  }
}
