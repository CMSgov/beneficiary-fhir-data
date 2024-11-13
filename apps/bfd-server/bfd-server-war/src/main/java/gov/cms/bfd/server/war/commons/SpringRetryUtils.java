package gov.cms.bfd.server.war.commons;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;

/**
 * Helper utility class defining methods used for {@link Retryable#exceptionExpression()} SpEL
 * expressions for matching on criteria that determine whether a {@link Retryable} annotated
 * operation can be retried.
 */
public final class SpringRetryUtils {
  /** Logger for this class. */
  static final Logger LOGGER = LoggerFactory.getLogger(SpringRetryUtils.class);

  /**
   * Constant SpEL expression used for {@link Retryable#exceptionExpression()}s that need to invoke
   * the {@link #shouldRetryIfFailoverOrConnectionException(Exception)} method.
   */
  public static final String SHOULD_RETRY_IF_FAILOVER_OR_CONNECTION_EXCEPTION_EXPRESSION =
      "T(gov.cms.bfd.server.war.commons.SpringRetryUtils).shouldRetryIfFailoverOrConnectionException(#root)";

  /**
   * Returns if a given {@link Exception} should be retried or not provided that it is an instance
   * of {@link FailoverSQLException} or {@link JDBCConnectionException} or if any of its inner
   * causes ({@link Throwable}s) are themselves instances of the aforementioned exceptions.
   *
   * @param ex the {@link Exception} to check
   * @return {@code true} if the given {@link Exception} is a {@link FailoverSQLException}, {@link
   *     JDBCConnectionException}, or any of its causes are either. {@code false} otherwise
   */
  public static boolean shouldRetryIfFailoverOrConnectionException(Exception ex) {
    LOGGER.warn("Retrying operation due to exception.", ex);
    return ex instanceof JDBCConnectionException
        || ex instanceof FailoverSQLException
        || ExceptionUtils.getThrowableList(ex).stream()
            .anyMatch(
                innerEx ->
                    innerEx instanceof JDBCConnectionException
                        || innerEx instanceof FailoverSQLException);
  }
}
