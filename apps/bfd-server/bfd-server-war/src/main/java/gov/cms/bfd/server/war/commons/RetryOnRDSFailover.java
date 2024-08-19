package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;

/**
 * Annotation encapsulating common parameters for the {@link Retryable} spring-retry annotation for
 * use with {@link Search} and {@link Read} annotated resource provider methods that need to retry
 * on {@link FailoverSQLException}s.
 *
 * <p>Some providers wrap checked {@link Exception}s thrown by asynchronous tasks in {@link
 * RuntimeException}s, thus using a simple "retryFor" expression matching on {@link
 * FailoverSQLException} is not possible for all provider operations. Instead, a SpEL expression is
 * used to get the root cause of the thrown {@link Exception} and see if it is an instance of {@link
 * FailoverSQLException} (note that the root cause of a {@link FailoverSQLException} is itself).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(
    exceptionExpression = SpringRetryUtils.SHOULD_RETRY_IF_FAILOVER_EXCEPTION_EXPRESSION,
    maxAttempts = 3)
public @interface RetryOnRDSFailover {
  /**
   * Alias for {@link Retryable#backoff()}.
   *
   * @return a default {@link Backoff} with a delay of 5000 milliseconds
   */
  @AliasFor(annotation = Retryable.class, attribute = "backoff")
  Backoff backoff() default @Backoff(delay = 5000);
}
