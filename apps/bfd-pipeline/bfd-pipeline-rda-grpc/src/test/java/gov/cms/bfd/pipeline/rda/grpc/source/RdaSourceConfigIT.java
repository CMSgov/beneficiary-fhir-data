package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/** Tests for the {@link RdaSourceConfig} class. */
public class RdaSourceConfigIT {

  /**
   * Parameters for the {@link RdaSourceConfigIT#grpcCallLogMessages(String, boolean, Level, String,
   * String)} test.
   *
   * @return The parameters for the associated test.
   */
  public static Stream<Arguments> grpcCallLogMessages() {
    String claimsToken = Base64.getEncoder().encodeToString("{\"nexp\":0}".getBytes());
    final String NO_EXP_AUTH_TOKEN = String.format("NotAReal.%s.Token", claimsToken);

    final String ONE_MONTH_EXP_AUTH_TOKEN =
        createTokenWithExpiration(Instant.now().plus(20, ChronoUnit.DAYS).getEpochSecond());

    final String TWO_WEEK_EXP_AUTH_TOKEN =
        createTokenWithExpiration(Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond());

    final String EXPIRED_AUTH_TOKEN =
        createTokenWithExpiration(Instant.now().plus(-1, ChronoUnit.DAYS).getEpochSecond());

    return Stream.of(
        arguments(
            "Token null, expect NO log message",
            false,
            Level.WARN,
            "Could not parse Authorization token as JWT",
            null),
        arguments(
            "Token blank, expect NO log message",
            false,
            Level.WARN,
            "Could not parse Authorization token as JWT",
            ""),
        arguments(
            "Unparsable Token, expect log message",
            true,
            Level.WARN,
            "Could not parse Authorization token as JWT",
            "secret"),
        arguments(
            "No 'exp' claim Token, expect log message",
            true,
            Level.WARN,
            "Could not find expiration claim",
            NO_EXP_AUTH_TOKEN),
        arguments(
            "Expire in <1 month Token, expect log message",
            true,
            Level.WARN,
            "JWT will expire in 19 days",
            ONE_MONTH_EXP_AUTH_TOKEN),
        arguments(
            "Expire in <2 weeks Token, expect log message",
            true,
            Level.ERROR,
            "JWT will expire in 9 days",
            TWO_WEEK_EXP_AUTH_TOKEN),
        arguments(
            "Expired Token, expect log message",
            true,
            Level.ERROR,
            "JWT is expired!",
            EXPIRED_AUTH_TOKEN));
  }

  /**
   * Parameterized test to check to see if the appropriate log messages are being created under
   * various conditions.
   *
   * @param testName The name of the test
   * @param expectLog Denotes if a message is expected or not in the logs.
   * @param logLevel The {@link Level} of the log message that is expected or not.
   * @param logMessage The log message that is either expected or not.
   * @param token The token to use for creating the {@link RdaSourceConfig}.
   * @throws Exception If there was an unexpected error in the tested logic.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void grpcCallLogMessages(
      // unused - Just here so it can be used in the test name
      @SuppressWarnings("unused") final String testName,
      final boolean expectLog,
      final Level logLevel,
      final String logMessage,
      final String token)
      throws Exception {
    boolean logFound =
        messageIsLogged(
            logLevel,
            logMessage,
            () ->
                RdaSourceConfig.builder()
                    .serverType(RdaSourceConfig.ServerType.InProcess)
                    .inProcessServerName("some name")
                    .maxIdle(Duration.ofSeconds(2))
                    .sequenceRangeUpdateIntervalSeconds(300)
                    .authenticationToken(token)
                    .build()
                    .createCallOptions());

    String errorMessage =
        expectLog
            ? "Expected log message '[%s] %s' not found"
            : "Unexpected log message '[%s] %s' found";

    assertEquals(expectLog, logFound, String.format(errorMessage, logLevel, logMessage));
  }

  /**
   * Helper method for checking if a particular log message was generated. This method creates a
   * temporary appender to add to the logging framework, which it then removes again after the given
   * runnable has been executed.
   *
   * @param logLevel The expected log level of the expected message to find
   * @param logMessage The expected log message to find
   * @param runnable The logic to execute that should generate the given expected log message.
   * @return True if the expected log message was logged, False otherwise.
   * @throws Exception If anything unexpected went wrong
   */
  private boolean messageIsLogged(
      Level logLevel, String logMessage, ThrowingRunnable<Exception> runnable) throws Exception {
    final Logger LOGGER = (Logger) LoggerFactory.getLogger(RdaSourceConfig.class);

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.setName("UNIT_TEST_APPENDER");
    listAppender.start();

    LOGGER.addAppender(listAppender);

    runnable.run();

    LOGGER.detachAppender("UNIT_TEST_APPENDER");

    return listAppender.list.stream()
        .anyMatch(e -> e.getLevel() == logLevel && e.getMessage().equals(logMessage));
  }

  /**
   * Helper Functional Interface for defining runnable logic that can throw some sort of exception.
   *
   * @param <E> The type of exception the runnable logic can throw.
   */
  private interface ThrowingRunnable<E extends Throwable> {
    /**
     * Runs the specified logic.
     *
     * @throws E the exception thrown from this runnable
     */
    void run() throws E;
  }

  /**
   * Helper method to generate a JWT with the given expiration date in epoch seconds.
   *
   * @param expirationDateEpochSeconds The desired expiration date (in epoch seconds) of the
   *     generated jwt.
   * @return The generated JWT with an expiration set to the given value.
   */
  public static String createTokenWithExpiration(long expirationDateEpochSeconds) {
    String claimsString = String.format("{\"exp\":%d}", expirationDateEpochSeconds);
    String claimsToken = Base64.getEncoder().encodeToString(claimsString.getBytes());
    return String.format("NotAReal.%s.Token", claimsToken);
  }
}
