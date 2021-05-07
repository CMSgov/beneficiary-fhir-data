package gov.cms.bfd.sharedutils.exceptions;

import java.sql.SQLException;

/**
 * Wraps a checked {@link SQLException} in an unchecked {@link RuntimeException} derivative, so that
 * error handling can be deferred to elsewhere in the call stack, without requiring all of the
 * method signature noise that checked exceptions mandate.
 */
public final class UncheckedSqlException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@link UncheckedSqlException}.
   *
   * @param cause the checked {@link SQLException} that caused the exception
   */
  public UncheckedSqlException(SQLException cause) {
    super(cause);
  }
}
