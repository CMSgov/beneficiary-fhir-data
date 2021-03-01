package gov.cms.bfd.pipeline.rif.extract.exceptions;

/**
 * Indicates that an unrecoverable failure has occurred when interacting with Amazon Web Services.
 */
public final class AwsFailureException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@link AwsFailureException}.
   *
   * @param cause the value to use for {@link #getCause()}
   */
  public AwsFailureException(Throwable cause) {
    super(cause);
  }
}
