package gov.cms.bfd.sharedutils.exceptions;

/**
 * This exception should only be used to cover edge cases that <em>appear</em> to be impossible;
 * edge cases that will only occur if the code itself is somehow incorrect.
 *
 * <blockquote>
 *
 * Bad Code Monkey! No more cheesy snacks for you!
 *
 * </blockquote>
 */
public final class BadCodeMonkeyException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@link BadCodeMonkeyException}.
   *
   * @param message a brief programmer-readable message explaining the error, or <code>null</code>
   *     if no further details are available or appropriate
   * @param cause the underlying exception that caused this one, or <code>null</code> if this
   *     exception was not caused by another
   */
  public BadCodeMonkeyException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new {@link BadCodeMonkeyException}.
   *
   * @param cause the underlying exception that caused this one
   */
  public BadCodeMonkeyException(Throwable cause) {
    this(null, cause);
  }

  /**
   * Constructs a new {@link BadCodeMonkeyException}.
   *
   * @param message a brief programmer-readable message explaining the error, or <code>null</code>
   *     if no further details are available or appropriate
   */
  public BadCodeMonkeyException(String message) {
    this(message, null);
  }

  /** Constructs a new {@link BadCodeMonkeyException}. */
  public BadCodeMonkeyException() {
    this(null, null);
  }
}
