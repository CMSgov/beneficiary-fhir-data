package gov.cms.bfd.pipeline.ccw.rif.extract.exceptions;

/**
 * Represents an exception thrown when the loaded RIF file type is not supported by the pipeline.
 */
public final class UnsupportedRifFileTypeException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  /** Instantiates a new Unsupported rif file type exception. */
  public UnsupportedRifFileTypeException() {}

  /**
   * Instantiates a new Unsupported rif file type exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public UnsupportedRifFileTypeException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new Unsupported rif file type exception.
   *
   * @param message the message
   */
  public UnsupportedRifFileTypeException(String message) {
    super(message);
  }

  /**
   * Instantiates a new Unsupported rif file type exception.
   *
   * @param cause the cause
   */
  public UnsupportedRifFileTypeException(Throwable cause) {
    super(cause);
  }
}
