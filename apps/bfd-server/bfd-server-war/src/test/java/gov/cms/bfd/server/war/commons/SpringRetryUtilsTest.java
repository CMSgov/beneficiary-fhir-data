package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLDataException;
import java.sql.SQLTimeoutException;
import java.util.stream.Stream;
import org.hibernate.JDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import software.amazon.jdbc.plugin.failover.FailoverFailedSQLException;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;
import software.amazon.jdbc.plugin.failover.FailoverSuccessSQLException;

/** Unit tests for {@link SpringRetryUtils}. */
public class SpringRetryUtilsTest {

  /**
   * Parameterized test verifying that {@link
   * SpringRetryUtils#shouldRetryIfFailoverOrConnectionException(Exception)} returns {@code true} if
   * the provided {@link Exception} is a {@link FailoverSQLException}.
   *
   * @param ex a {@link FailoverSQLException}
   */
  @ParameterizedTest
  @MethodSource("provideBareFailoverExceptions")
  public void testItShouldReturnTrueIfExceptionIsFailoverSQLException(Exception ex) {
    assertTrue(SpringRetryUtils.shouldRetryIfFailoverOrConnectionException(ex));
  }

  /**
   * Parameterized test verifying that {@link
   * SpringRetryUtils#shouldRetryIfFailoverOrConnectionException(Exception)} returns {@code true} if
   * the provided {@link Exception} is wrapping a {@link FailoverSQLException}.
   *
   * @param ex an {@link Exception} wrapping a {@link FailoverSQLException}
   */
  @ParameterizedTest
  @MethodSource("provideWrappedFailoverExceptions")
  public void testItShouldReturnTrueIfExceptionIsWrappedFailoverSQLException(Exception ex) {
    assertTrue(SpringRetryUtils.shouldRetryIfFailoverOrConnectionException(ex));
  }

  /**
   * Parameterized test verifying that {@link
   * SpringRetryUtils#shouldRetryIfFailoverOrConnectionException(Exception)} returns {@code false}
   * if the provided {@link Exception} is not a {@link FailoverSQLException} or wrapping a {@link
   * FailoverSQLException}.
   *
   * @param ex an {@link Exception} that does not wrap a {@link FailoverSQLException} or is a {@link
   *     FailoverSQLException}
   */
  @ParameterizedTest
  @MethodSource("provideExceptionsThatDontMatch")
  public void testItShouldReturnFalseIfExceptionIsNotWrappedFailoverSQLException(Exception ex) {
    assertFalse(SpringRetryUtils.shouldRetryIfFailoverOrConnectionException(ex));
  }

  /**
   * Provider method source for {@link
   * #testItShouldReturnTrueIfExceptionIsFailoverSQLException(Exception)} returning bare {@link
   * FailoverSQLException}s; that is, {@link Exception}s that are of the type {@link
   * FailoverSQLException} and are not wrapping other {@link Exception}s.
   *
   * @return a {@link Stream} of {@link Arguments}s containing all variants of {@link
   *     FailoverSQLException}
   */
  private static Stream<Arguments> provideBareFailoverExceptions() {
    return Stream.of(
        Arguments.of(new FailoverFailedSQLException("test")),
        Arguments.of(new FailoverSuccessSQLException()),
        Arguments.of(new FailoverSQLException("test", "test")));
  }

  /**
   * Provider method source for {@link
   * #testItShouldReturnTrueIfExceptionIsWrappedFailoverSQLException(Exception)} returning {@link
   * Exception}s wrapping {@link FailoverSQLException}s.
   *
   * @return a {@link Stream} of {@link Arguments}s containing {@link Exception}s that wrap {@link
   *     FailoverSQLException}
   */
  private static Stream<Arguments> provideWrappedFailoverExceptions() {
    return Stream.of(
        Arguments.of(new JDBCException("test", new FailoverFailedSQLException("test"))),
        Arguments.of(new JDBCConnectionException("test", new FailoverSuccessSQLException())),
        Arguments.of(
            new RuntimeException(
                new JDBCConnectionException("test", new FailoverSuccessSQLException()))),
        // Tests an inner cause (not the root cause) that matches a connection exception still
        // causes a retry
        Arguments.of(
            new RuntimeException(
                new JDBCConnectionException(
                    "test", new PSQLException("test", PSQLState.UNKNOWN_STATE)))));
  }

  /**
   * Provider method source for {@link
   * #testItShouldReturnFalseIfExceptionIsNotWrappedFailoverSQLException(Exception)} returning
   * {@link Exception}s that are not wrapping {@link FailoverSQLException} or are {@link
   * FailoverSQLException}s.
   *
   * @return a {@link Stream} of {@link Arguments}s containing {@link Exception}s that do not wrap
   *     {@link FailoverSQLException}s or are {@link FailoverSQLException}s
   */
  private static Stream<Arguments> provideExceptionsThatDontMatch() {
    return Stream.of(
        Arguments.of(new JDBCException("test", new SQLDataException())),
        Arguments.of(new JDBCException("test", new SQLTimeoutException())),
        Arguments.of(new RuntimeException()),
        Arguments.of(new RuntimeException(new SQLTimeoutException())),
        Arguments.of(new NullPointerException()),
        Arguments.of(new Exception()));
  }
}
