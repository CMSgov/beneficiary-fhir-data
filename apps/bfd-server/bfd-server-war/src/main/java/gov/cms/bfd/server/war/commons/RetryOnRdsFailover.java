package gov.cms.bfd.server.war.commons;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;

/**
 * Annotation encapsulating common parameters for the {@link Retryable} spring-retry annotation for
 * use with {@link ca.uhn.fhir.rest.annotation.Search} and {@link ca.uhn.fhir.rest.annotation.Read}
 * annotated resource provider methods that need to retry on {@link FailoverSQLException}s.
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
    maxAttempts = 3,
    backoff = @Backoff(delay = 5000))
public @interface RetryOnRdsFailover {}
