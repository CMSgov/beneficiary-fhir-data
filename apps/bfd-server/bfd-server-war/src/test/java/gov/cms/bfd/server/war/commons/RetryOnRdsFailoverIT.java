package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.JDBCException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;
import software.amazon.jdbc.plugin.failover.FailoverSuccessSQLException;

/**
 * Spring integration tests for the {@link RetryOnRdsFailover} annotation that extends the {@link
 * Retryable} annotation from spring-retry.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RetryOnRdsFailoverIT.TestContextConfiguration.class})
public class RetryOnRdsFailoverIT {

  /**
   * Autowired bean used in order to activate spring-retry's proxying via the {@link
   * TestContextConfiguration} configuration.
   */
  @Autowired private RetryableOperationBean retryableOperationBean;

  /**
   * Empty {@link Configuration} that imports {@link RetryableOperationBean} as a bean. Used for
   * {@link EnableRetry}.
   */
  @Configuration
  @EnableRetry
  @Import(RetryableOperationBean.class)
  static class TestContextConfiguration {}

  /**
   * Bean providing a means for counting retry attempts per-test and a method that is used to test
   * the {@link Exception}-matching of the {@link RetryOnRdsFailover} annotation.
   */
  static class RetryableOperationBean {
    /**
     * Map of random GUIDs, which map to a given test case run, to the number of retries for that
     * given test case. Used to assert on the number of retries per-test-case.
     */
    public static final ConcurrentHashMap<String, Integer> RETRY_COUNTS_PER_GUID =
        new ConcurrentHashMap<>();

    /**
     * Simple helper method annotated with {@link RetryOnRdsFailover} (with a backoff delay of 1 ms
     * to speed up tests) that simply throws the {@link Exception}s provided and tracks the number
     * of times its been retried. Once all {@link Exception}s are exhausted from the provided list,
     * this method simply returns <code>true</code>.
     *
     * @param exceptions mutable {@link List<Exception>} which indicate which {@link Exception}s to
     *     throw prior to executing actual method logic
     * @param guid random GUID that is used as a key to identify a given test-case run for tracking
     *     retry counts
     * @return <code>true</code>, always
     * @throws Exception whichever {@link Exception} that is at the top of the provided list of
     *     {@link Exception}s, if not empty
     */
    @RetryOnRdsFailover(backoff = @Backoff(delay = 1))
    boolean retryableOperation(List<Exception> exceptions, String guid) throws Exception {
      RETRY_COUNTS_PER_GUID.put(guid, RETRY_COUNTS_PER_GUID.getOrDefault(guid, -1) + 1);

      if (!exceptions.isEmpty()) {
        throw exceptions.removeFirst();
      }

      return true;
    }
  }

  /**
   * Test that verifies {@link RetryOnRdsFailover} will cause a single retry to occur when a single
   * {@link FailoverSuccessSQLException} is thrown.
   */
  @Test
  public void testItRetriesOnceWhenFailoverSuccessSQLExceptionIsThrown() throws Exception {
    // Arrange
    final var exceptionsToThrow =
        new ArrayList<Exception>(List.of(new FailoverSuccessSQLException()));
    final var retryGuid = UUID.randomUUID().toString();

    // Act
    final var actualVal = retryableOperationBean.retryableOperation(exceptionsToThrow, retryGuid);

    // Assert
    assertTrue(actualVal);
    assertEquals(1, RetryableOperationBean.RETRY_COUNTS_PER_GUID.get(retryGuid));
  }

  /**
   * Test that verifies {@link RetryOnRdsFailover} will cause a single retry to occur when a single
   * {@link RuntimeException} wrapping {@link FailoverSuccessSQLException} is thrown.
   */
  @Test
  public void testItRetriesOnceWhenWrappedFailoverSuccessSQLExceptionIsThrown() throws Exception {
    // Arrange
    final var exceptionsToThrow =
        new ArrayList<Exception>(
            List.of(
                new RuntimeException(
                    new JDBCException("test", new FailoverSuccessSQLException()))));
    final var retryGuid = UUID.randomUUID().toString();

    // Act
    final var actualVal = retryableOperationBean.retryableOperation(exceptionsToThrow, retryGuid);

    // Assert
    assertTrue(actualVal);
    assertEquals(1, RetryableOperationBean.RETRY_COUNTS_PER_GUID.get(retryGuid));
  }

  /**
   * Test that verifies {@link RetryOnRdsFailover} will cause two retries to occur when two matching
   * {@link Exception}s to occur.
   */
  @Test
  public void testItRetriesTwiceWhenMultipleMatchingExceptionsAreThrown() throws Exception {
    // Arrange
    final var exceptionsToThrow =
        new ArrayList<>(
            Arrays.asList(
                new RuntimeException(new JDBCException("test", new FailoverSuccessSQLException())),
                new FailoverSuccessSQLException()));
    final var retryGuid = UUID.randomUUID().toString();

    // Act
    final var actualVal = retryableOperationBean.retryableOperation(exceptionsToThrow, retryGuid);

    // Assert
    assertTrue(actualVal);
    assertEquals(2, RetryableOperationBean.RETRY_COUNTS_PER_GUID.get(retryGuid));
  }

  /**
   * Test that verifies {@link RetryOnRdsFailover} will cause the operation to retry upto two times
   * and then throw the final {@link Exception} once the number of maximum attempts is reached.
   */
  @Test
  public void testItThrowsWhenMaxAttemptsAreExceeded() {
    // Arrange
    final var exceptionsToThrow =
        new ArrayList<Exception>(
            Arrays.asList(
                new FailoverSuccessSQLException(),
                new FailoverSuccessSQLException(),
                new FailoverSuccessSQLException()));
    final var retryGuid = UUID.randomUUID().toString();

    // Act
    assertThrows(
        FailoverSuccessSQLException.class,
        () -> retryableOperationBean.retryableOperation(exceptionsToThrow, retryGuid));
    assertEquals(2, RetryableOperationBean.RETRY_COUNTS_PER_GUID.get(retryGuid));
  }

  /**
   * Test that verifies {@link RetryOnRdsFailover} will not retry on {@link Exception}s that do not
   * match {@link FailoverSQLException}s.
   */
  @Test
  public void testItThrowsWhenUnmatchedExceptionIsThrown() {
    // Arrange
    final var exceptionsToThrow =
        new ArrayList<>(
            Arrays.asList(new FailoverSuccessSQLException(), new NullPointerException()));
    final var retryGuid = UUID.randomUUID().toString();

    // Act
    assertThrows(
        NullPointerException.class,
        () -> retryableOperationBean.retryableOperation(exceptionsToThrow, retryGuid));
    assertEquals(1, RetryableOperationBean.RETRY_COUNTS_PER_GUID.get(retryGuid));
  }
}
