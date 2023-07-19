package gov.cms.bfd.sharedutils.exceptions;

import java.io.IOException;
import lombok.Getter;

/**
 * Wraps a checked {@link IOException} in an unchecked {@link RuntimeException} derivative, so that
 * error handling can be deferred to elsewhere in the call stack, without requiring all of the
 * method signature noise that checked exceptions mandate.
 */
public class UncheckedIOException extends RuntimeException {
  @java.io.Serial private static final long serialVersionUID = 1L;

  /**
   * The actual exception stored in a field with its proper type so no casts are required to access
   * it as such.
   */
  @Getter private final IOException realException;

  /**
   * Initializes the instance.
   *
   * @param cause the exception we are wrapping
   */
  public UncheckedIOException(IOException cause) {
    super(cause);
    this.realException = cause;
  }
}
