package gov.cms.bfd.pipeline.ccw.rif.extract.exceptions;

/** Represents an exception thrown when the RIF version is not supported by the pipeline. */
public final class UnsupportedRifVersionException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  /** Instantiates a new unsupported rif version exception. */
  public UnsupportedRifVersionException() {}

  /**
   * Instantiates a new unsupported rif version exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public UnsupportedRifVersionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new unsupported rif version exception.
   *
   * @param version the version
   */
  public UnsupportedRifVersionException(int version) {
    super("Unsupported record version: " + version);
  }

  /**
   * Instantiates a new unsupported rif version exception.
   *
   * @param cause the cause
   */
  public UnsupportedRifVersionException(Throwable cause) {
    super(cause);
  }
}
