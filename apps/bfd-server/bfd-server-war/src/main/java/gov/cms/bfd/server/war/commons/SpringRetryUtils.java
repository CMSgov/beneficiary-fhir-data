package gov.cms.bfd.server.war.commons;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.retry.annotation.Retryable;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;

/**
 * Helper utility class defining methods used for {@link Retryable#exceptionExpression()} SpEL
 * expressions for matching on criteria that determine whether a {@link Retryable} annotated
 * operation can be retried.
 */
public final class SpringRetryUtils {
  /**
   * Constant SpEL expression used for {@link Retryable#exceptionExpression()}s that need to invoke
   * the {@link #shouldRetryIfFailover(Exception)} method.
   */
  public static final String SHOULD_RETRY_IF_FAILOVER_EXCEPTION_EXPRESSION =
      "T(gov.cms.bfd.server.war.commons.SpringRetryUtils).shouldRetryIfFailover(#root)";

  /**
   * Returns if a given {@link Exception} should be retried or not provided that its "root cause"
   * (innermost {@link Throwable}) is an instance of {@link FailoverSQLException}.
   *
   * @param ex the {@link Exception} to check for a root cause of {@link FailoverSQLException}
   * @return <code>true</code> if the root cause of the given {@link Exception} is a {@link
   *     FailoverSQLException}, <code>false</code> otherwise
   */
  public static boolean shouldRetryIfFailover(Exception ex) {
    return ex instanceof FailoverSQLException
        || ExceptionUtils.getRootCause(ex) instanceof FailoverSQLException;
  }
}
