package gov.cms.bfd.pipeline.rif.extract.exceptions;

/**
 * Indicates that a checksum failure has occurred after downloading the files from Amazon Web
 * Services S3..
 */
public final class ChecksumException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@link ChecksumException}.
   *
   * @param cause the value/description to use for {@link #getCause()}
   */
  public ChecksumException(String cause) {
    super(cause);
  }

  /**
   * Creates a {@code ChecksumException} with the specified detail message and cause.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method).
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     (A {@code null} value is permitted, and indicates that the cause is nonexistent or
   *     unknown.)
   * @since 1.5
   */
  public ChecksumException(String message, Throwable cause) {
    super(message, cause);
  }
}
