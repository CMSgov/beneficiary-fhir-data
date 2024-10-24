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
 * used which calls {@link SpringRetryUtils#shouldRetryIfFailover(Exception)} which will enable
 * retries for {@link Exception}s that are {@link FailoverSQLException}s or {@link Exception}s with
 * a root cause of {@link FailoverSQLException}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(exceptionExpression = SpringRetryUtils.SHOULD_RETRY_IF_FAILOVER_EXCEPTION_EXPRESSION)
public @interface RetryOnRDSFailover {
  /**
   * Alias for {@link Retryable#backoff()}.
   *
   * @return a default {@link Backoff} with a delay of 5000 milliseconds
   */
  @AliasFor(annotation = Retryable.class, attribute = "backoff")
  Backoff backoff() default @Backoff(delay = 5000);

  /**
   * Alias for {@link Retryable#stateful()}.
   *
   * @return a default of {@code false}
   */
  @AliasFor(annotation = Retryable.class, attribute = "stateful")
  boolean stateful() default false;

  /**
   * Alias for {@link Retryable#maxAttempts()}.
   *
   * @return a default of {@code 3} attempts
   */
  @AliasFor(annotation = Retryable.class, attribute = "maxAttempts")
  int maxAttempts() default 3;

  /**
   * Alias for {@link Retryable#listeners()}.
   *
   * @return an empty {@link String} array indicating no listeners
   */
  @AliasFor(annotation = Retryable.class, attribute = "listeners")
  String[] listeners() default {};

  /**
   * Alias for {@link Retryable#recover()}.
   *
   * @return an empty {@link String} indicating no recovery method
   */
  @AliasFor(annotation = Retryable.class, attribute = "recover")
  String recover() default "";

  /**
   * Alias for {@link Retryable#interceptor()}.
   *
   * @return an empty {@link String} indicating no interceptor method
   */
  @AliasFor(annotation = Retryable.class, attribute = "interceptor")
  String interceptor() default "";
}
