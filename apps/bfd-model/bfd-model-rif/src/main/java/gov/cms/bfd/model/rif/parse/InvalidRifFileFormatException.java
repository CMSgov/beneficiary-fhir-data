package gov.cms.bfd.model.rif.parse;

/** Represents an exception related to an invalid rif file format. */
public final class InvalidRifFileFormatException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  /** Instantiates a new exception. */
  public InvalidRifFileFormatException() {}

  /**
   * Instantiates a new exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public InvalidRifFileFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new exception.
   *
   * @param message the message
   */
  public InvalidRifFileFormatException(String message) {
    super(message);
  }

  /**
   * Instantiates a new exception.
   *
   * @param cause the cause
   */
  public InvalidRifFileFormatException(Throwable cause) {
    super(cause);
  }
}
