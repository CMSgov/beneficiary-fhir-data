package gov.cms.bfd.model.rif.parse;

/** Represents an exception related to an invalid rif value. */
public final class InvalidRifValueException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  /** Instantiates a new exception. */
  public InvalidRifValueException() {}

  /**
   * Instantiates a new exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public InvalidRifValueException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new exception.
   *
   * @param message the message
   */
  public InvalidRifValueException(String message) {
    super(message);
  }

  /**
   * Instantiates a new exception.
   *
   * @param cause the cause
   */
  public InvalidRifValueException(Throwable cause) {
    super(cause);
  }
}
