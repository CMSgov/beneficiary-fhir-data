package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

public class GrpcRdaSourceIT {
  private static final String SOURCE_CLAIM_1 =
      "{"
          + "  \"dcn\": \"63843470\","
          + "  \"hicNo\": \"916689703543\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_PAID\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_MANUAL\","
          + "  \"currLoc2Unrecognized\": \"uma\","
          + "  \"totalChargeAmount\": \"3.75\","
          + "  \"currTranDtCymd\": \"2021-03-20\","
          + "  \"principleDiag\": \"uec\","
          + "  \"mbi\": \"c1ihk7q0g3i\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"uec\","
          + "      \"procFlag\": \"nli\","
          + "      \"rdaPosition\": 1"
          + "    },"
          + "    {"
          + "      \"procCd\": \"egkkkw\","
          + "      \"procFlag\": \"hsw\","
          + "      \"procDt\": \"2021-02-03\","
          + "      \"rdaPosition\": 2"
          + "    },"
          + "    {"
          + "      \"procCd\": \"zhaj\","
          + "      \"procDt\": \"2021-01-07\","
          + "      \"rdaPosition\": 3"
          + "    },"
          + "    {"
          + "      \"procCd\": \"ods\","
          + "      \"procDt\": \"2021-01-03\","
          + "      \"rdaPosition\": 4"
          + "    }"
          + "  ],"
          + "  \"medaProvId\": \"oducjgzt67joc\""
          + "}";
  private static final String SOURCE_CLAIM_2 =
      "{"
          + "  \"dcn\": \"2643602\","
          + "  \"hicNo\": \"640930211775\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_REJECT\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_OFFLINE\","
          + "  \"currLoc2Unrecognized\": \"p6s\","
          + "  \"totalChargeAmount\": \"55.91\","
          + "  \"recdDtCymd\": \"2021-05-14\","
          + "  \"currTranDtCymd\": \"2020-12-21\","
          + "  \"principleDiag\": \"egnj\","
          + "  \"npiNumber\": \"5764657700\","
          + "  \"mbi\": \"0vtc7u321x0\","
          + "  \"fedTaxNb\": \"2845244764\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"egnj\","
          + "      \"procDt\": \"2021-05-13\","
          + "      \"rdaPosition\": 1"
          + "    },"
          + "    {"
          + "      \"procCd\": \"vvqtwoz\","
          + "      \"procDt\": \"2021-04-29\","
          + "      \"rdaPosition\": 2"
          + "    },"
          + "    {"
          + "      \"procCd\": \"fipyd\","
          + "      \"procFlag\": \"g\","
          + "      \"rdaPosition\": 3"
          + "    }"
          + "  ]"
          + "}";
  private final String claimsJson = SOURCE_CLAIM_1 + System.lineSeparator() + SOURCE_CLAIM_2;
  public static final String EXPECTED_CLAIM_1 =
      "{\n"
          + "  \"apiSource\" : \"0.4\",\n"
          + "  \"auditTrail\" : [ ],\n"
          + "  \"currLoc1\" : \"M\",\n"
          + "  \"currLoc2\" : \"uma\",\n"
          + "  \"currStatus\" : \"P\",\n"
          + "  \"currTranDate\" : \"2021-03-20\",\n"
          + "  \"dcn\" : \"63843470\",\n"
          + "  \"diagCodes\" : [ ],\n"
          + "  \"hicNo\" : \"916689703543\",\n"
          + "  \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "  \"mbi\" : \"c1ihk7q0g3i\",\n"
          + "  \"mbiHash\" : \"c3b21bb6fef6e8af99a175e53b20893048dc2cd9f566a4930d8c1e6f8a30822d\",\n"
          + "  \"mbiRecord\" : {\n"
          + "    \"hash\" : \"c3b21bb6fef6e8af99a175e53b20893048dc2cd9f566a4930d8c1e6f8a30822d\",\n"
          + "    \"mbi\" : \"c1ihk7q0g3i\"\n"
          + "  },\n"
          + "  \"medaProvId\" : \"oducjgzt67joc\",\n"
          + "  \"payers\" : [ ],\n"
          + "  \"principleDiag\" : \"uec\",\n"
          + "  \"procCodes\" : [ {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"procCode\" : \"egkkkw\",\n"
          + "    \"procDate\" : \"2021-02-03\",\n"
          + "    \"procFlag\" : \"hsw\",\n"
          + "    \"rdaPosition\" : 2\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"procCode\" : \"uec\",\n"
          + "    \"procFlag\" : \"nli\",\n"
          + "    \"rdaPosition\" : 1\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"procCode\" : \"ods\",\n"
          + "    \"procDate\" : \"2021-01-03\",\n"
          + "    \"rdaPosition\" : 4\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"procCode\" : \"zhaj\",\n"
          + "    \"procDate\" : \"2021-01-07\",\n"
          + "    \"rdaPosition\" : 3\n"
          + "  } ],\n"
          + "  \"sequenceNumber\" : 0,\n"
          + "  \"totalChargeAmount\" : 3.75\n"
          + "}";
  public static final String EXPECTED_CLAIM_2 =
      "{\n"
          + "  \"apiSource\" : \"0.4\",\n"
          + "  \"auditTrail\" : [ ],\n"
          + "  \"currLoc1\" : \"O\",\n"
          + "  \"currLoc2\" : \"p6s\",\n"
          + "  \"currStatus\" : \"R\",\n"
          + "  \"currTranDate\" : \"2020-12-21\",\n"
          + "  \"dcn\" : \"2643602\",\n"
          + "  \"diagCodes\" : [ ],\n"
          + "  \"fedTaxNumber\" : \"2845244764\",\n"
          + "  \"hicNo\" : \"640930211775\",\n"
          + "  \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "  \"mbi\" : \"0vtc7u321x0\",\n"
          + "  \"mbiHash\" : \"b30cb27025eceae66fcedf88c3c2a8631381f1ffc26fcc9d46271038dae58721\",\n"
          + "  \"mbiRecord\" : {\n"
          + "    \"hash\" : \"b30cb27025eceae66fcedf88c3c2a8631381f1ffc26fcc9d46271038dae58721\",\n"
          + "    \"mbi\" : \"0vtc7u321x0\"\n"
          + "  },\n"
          + "  \"npiNumber\" : \"5764657700\",\n"
          + "  \"payers\" : [ ],\n"
          + "  \"principleDiag\" : \"egnj\",\n"
          + "  \"procCodes\" : [ {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"procCode\" : \"vvqtwoz\",\n"
          + "    \"procDate\" : \"2021-04-29\",\n"
          + "    \"rdaPosition\" : 2\n"
          + "  }, {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"procCode\" : \"egnj\",\n"
          + "    \"procDate\" : \"2021-05-13\",\n"
          + "    \"rdaPosition\" : 1\n"
          + "  }, {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"procCode\" : \"fipyd\",\n"
          + "    \"procFlag\" : \"g\",\n"
          + "    \"rdaPosition\" : 3\n"
          + "  } ],\n"
          + "  \"receivedDate\" : \"2021-05-14\",\n"
          + "  \"sequenceNumber\" : 1,\n"
          + "  \"totalChargeAmount\" : 55.91\n"
          + "}";

  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(5, "pepper-pepper-pepper"));
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(hasher.getConfig()));
  private final FissClaimStreamCaller streamCaller = new FissClaimStreamCaller();
  private MetricRegistry appMetrics;
  private JsonCaptureSink sink;

  @BeforeEach
  public void setUp() throws Exception {
    appMetrics = new MetricRegistry();
    sink = new JsonCaptureSink();
  }

  @Test
  public void grpcCallNoAuthTokenNeeded() throws Exception {
    createServerConfig()
        .build()
        .runWithPortParam(
            port -> {
              int count;
              GrpcRdaSource.Config config = createSourceConfig(port).build();
              try (GrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                  createSource(config)) {
                count = source.retrieveAndProcessObjects(3, sink);
              }
              assertEquals(2, count);
              assertEquals(2, sink.getValues().size());
              assertEquals(EXPECTED_CLAIM_1, sink.getValues().get(0));
              assertEquals(EXPECTED_CLAIM_2, sink.getValues().get(1));
            });
  }

  @Test
  public void grpcCallWithCorrectAuthToken() throws Exception {
    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              int count;
              GrpcRdaSource.Config config =
                  createSourceConfig(port).authenticationToken("secret").build();
              try (GrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                  createSource(config)) {
                count = source.retrieveAndProcessObjects(3, sink);
              }
              assertEquals(2, count);
              assertEquals(2, sink.getValues().size());
              assertEquals(EXPECTED_CLAIM_1, sink.getValues().get(0));
              assertEquals(EXPECTED_CLAIM_2, sink.getValues().get(1));
            });
  }

  @Test
  public void grpcCallWithMissingAuthToken() throws Exception {
    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              GrpcRdaSource.Config config = createSourceConfig(port).build();
              try {
                try (GrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                    createSource(config)) {
                  source.retrieveAndProcessObjects(3, sink);
                }
                fail("should have thrown an exception due to missing token");
              } catch (ProcessingException ex) {
                assertEquals(0, ex.getProcessedCount());
                assertTrue(ex.getOriginalCause() instanceof StatusRuntimeException);
                assertEquals(
                    Status.UNAUTHENTICATED,
                    ((StatusRuntimeException) ex.getOriginalCause()).getStatus());
              }
            });
  }

  @Test
  public void grpcCallWithIncorrectAuthToken() throws Exception {
    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              GrpcRdaSource.Config config =
                  createSourceConfig(port).authenticationToken("wrong-secret").build();
              try {
                try (GrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                    createSource(config)) {
                  source.retrieveAndProcessObjects(3, sink);
                }
                fail("should have thrown an exception due to missing token");
              } catch (ProcessingException ex) {
                assertEquals(0, ex.getProcessedCount());
                assertTrue(ex.getOriginalCause() instanceof StatusRuntimeException);
                assertEquals(
                    Status.UNAUTHENTICATED,
                    ((StatusRuntimeException) ex.getOriginalCause()).getStatus());
              }
            });
  }

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

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void grpcCallLogMessages(
      final String testName,
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
                createServerConfig()
                    .build()
                    .runWithPortParam(
                        port -> {
                          GrpcRdaSource.Config config =
                              createSourceConfig(port).authenticationToken(token).build();
                          try (var source = createSource(config)) {
                            source.retrieveAndProcessObjects(3, sink);
                          }
                        }));

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
    final Logger LOGGER = (Logger) LoggerFactory.getLogger(GrpcRdaSource.class);

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

  private RdaServer.LocalConfig.LocalConfigBuilder createServerConfig() {
    return RdaServer.LocalConfig.builder()
        .fissSourceFactory(
            sequenceNumber ->
                WrappedClaimSource.wrapFissClaims(
                    new JsonMessageSource<>(claimsJson, JsonMessageSource::parseFissClaim),
                    clock,
                    sequenceNumber - 1));
  }

  private GrpcRdaSource.Config.ConfigBuilder createSourceConfig(Integer port) {
    return GrpcRdaSource.Config.builder()
        .serverType(GrpcRdaSource.Config.ServerType.Remote)
        .host("localhost")
        .port(port)
        .maxIdle(Duration.ofSeconds(30));
  }

  @Nonnull
  private GrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> createSource(
      GrpcRdaSource.Config config) {
    return new GrpcRdaSource<>(config, streamCaller, appMetrics, "fiss", Optional.empty());
  }

  private class JsonCaptureSink implements RdaSink<FissClaimChange, RdaChange<RdaFissClaim>> {
    private final List<String> values = new ArrayList<>();
    private final ObjectMapper mapper;

    public JsonCaptureSink() {
      mapper =
          new JsonMapper()
              .enable(SerializationFeature.INDENT_OUTPUT)
              .registerModule(new Jdk8Module())
              .registerModule(new JavaTimeModule())
              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
              .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public synchronized int writeMessage(String dataVersion, FissClaimChange message)
        throws ProcessingException {
      try {
        var change = transformMessage(dataVersion, message);
        values.add(mapper.writeValueAsString(change.getClaim()));
        return 1;
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }

    @Override
    public String getDedupKeyForMessage(FissClaimChange object) {
      return object.getClaim().getDcn();
    }

    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {}

    @Override
    public long getSequenceNumberForObject(FissClaimChange object) {
      return object.getSeq();
    }

    @Nonnull
    @Override
    public RdaChange<RdaFissClaim> transformMessage(String apiVersion, FissClaimChange message) {
      var change = transformer.transformClaim(message);
      change.getClaim().setApiSource(apiVersion);
      if (change.getClaim().getMbiRecord() != null) {
        change.getClaim().getMbiRecord().setLastUpdated(null);
      }
      return change;
    }

    @Override
    public int writeClaims(Collection<RdaChange<RdaFissClaim>> objects) throws ProcessingException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getProcessedCount() throws ProcessingException {
      return 0;
    }

    @Override
    public void shutdown(Duration waitTime) throws ProcessingException {}

    @Override
    public void close() throws Exception {}

    public synchronized List<String> getValues() {
      return values;
    }
  }
}
