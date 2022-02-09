package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
          + "  \"mbi\": \"c1ihk7q0g3i57\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"uec\","
          + "      \"procFlag\": \"nli\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"egkkkw\","
          + "      \"procFlag\": \"hsw\","
          + "      \"procDt\": \"2021-02-03\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"zhaj\","
          + "      \"procDt\": \"2021-01-07\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"ods\","
          + "      \"procDt\": \"2021-01-03\""
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
          + "  \"mbi\": \"0vtc7u321x0se\","
          + "  \"fedTaxNb\": \"2845244764\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"egnj\","
          + "      \"procDt\": \"2021-05-13\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"vvqtwoz\","
          + "      \"procDt\": \"2021-04-29\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"fipyd\","
          + "      \"procFlag\": \"g\""
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
          + "  \"mbi\" : \"c1ihk7q0g3i57\",\n"
          + "  \"mbiHash\" : \"56dd7e48bfbfcfe851d4cda2dbd863775b450aea207614a9cc118ed2765713e7\",\n"
          + "  \"mbiRecord\" : {\n"
          + "    \"hash\" : \"56dd7e48bfbfcfe851d4cda2dbd863775b450aea207614a9cc118ed2765713e7\",\n"
          + "    \"mbi\" : \"c1ihk7q0g3i57\"\n"
          + "  },\n"
          + "  \"medaProvId\" : \"oducjgzt67joc\",\n"
          + "  \"payers\" : [ ],\n"
          + "  \"principleDiag\" : \"uec\",\n"
          + "  \"procCodes\" : [ {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 2,\n"
          + "    \"procCode\" : \"zhaj\",\n"
          + "    \"procDate\" : \"2021-01-07\"\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 1,\n"
          + "    \"procCode\" : \"egkkkw\",\n"
          + "    \"procDate\" : \"2021-02-03\",\n"
          + "    \"procFlag\" : \"hsw\"\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 3,\n"
          + "    \"procCode\" : \"ods\",\n"
          + "    \"procDate\" : \"2021-01-03\"\n"
          + "  }, {\n"
          + "    \"dcn\" : \"63843470\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 0,\n"
          + "    \"procCode\" : \"uec\",\n"
          + "    \"procFlag\" : \"nli\"\n"
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
          + "  \"mbi\" : \"0vtc7u321x0se\",\n"
          + "  \"mbiHash\" : \"9c0e61338935c978c25f73442c5593cdc20e35164ad8d8e426955b626de24e2c\",\n"
          + "  \"mbiRecord\" : {\n"
          + "    \"hash\" : \"9c0e61338935c978c25f73442c5593cdc20e35164ad8d8e426955b626de24e2c\",\n"
          + "    \"mbi\" : \"0vtc7u321x0se\"\n"
          + "  },\n"
          + "  \"npiNumber\" : \"5764657700\",\n"
          + "  \"payers\" : [ ],\n"
          + "  \"principleDiag\" : \"egnj\",\n"
          + "  \"procCodes\" : [ {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 2,\n"
          + "    \"procCode\" : \"fipyd\",\n"
          + "    \"procFlag\" : \"g\"\n"
          + "  }, {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 1,\n"
          + "    \"procCode\" : \"vvqtwoz\",\n"
          + "    \"procDate\" : \"2021-04-29\"\n"
          + "  }, {\n"
          + "    \"dcn\" : \"2643602\",\n"
          + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
          + "    \"priority\" : 0,\n"
          + "    \"procCode\" : \"egnj\",\n"
          + "    \"procDate\" : \"2021-05-13\"\n"
          + "  } ],\n"
          + "  \"receivedDate\" : \"2021-05-14\",\n"
          + "  \"sequenceNumber\" : 1,\n"
          + "  \"totalChargeAmount\" : 55.91\n"
          + "}";

  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(5, "pepper-pepper-pepper"));
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, hasher.getConfig());
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
              try (GrpcRdaSource<FissClaimChange, RdaChange<PreAdjFissClaim>> source =
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
              try (GrpcRdaSource<FissClaimChange, RdaChange<PreAdjFissClaim>> source =
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
                try (GrpcRdaSource<FissClaimChange, RdaChange<PreAdjFissClaim>> source =
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
                try (GrpcRdaSource<FissClaimChange, RdaChange<PreAdjFissClaim>> source =
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

  private RdaServer.LocalConfig.LocalConfigBuilder createServerConfig() {
    return RdaServer.LocalConfig.builder()
        .fissSourceFactory(
            sequenceNumber ->
                WrappedClaimSource.wrapFissClaims(
                    new JsonMessageSource<>(claimsJson, JsonMessageSource::parseFissClaim),
                    clock,
                    sequenceNumber));
  }

  private GrpcRdaSource.Config.ConfigBuilder createSourceConfig(Integer port) {
    return GrpcRdaSource.Config.builder()
        .serverType(GrpcRdaSource.Config.ServerType.Remote)
        .host("localhost")
        .port(port)
        .maxIdle(Duration.ofSeconds(30));
  }

  @Nonnull
  private GrpcRdaSource<FissClaimChange, RdaChange<PreAdjFissClaim>> createSource(
      GrpcRdaSource.Config config) {
    return new GrpcRdaSource<>(config, streamCaller, appMetrics, "fiss", Optional.empty());
  }

  private class JsonCaptureSink implements RdaSink<FissClaimChange, RdaChange<PreAdjFissClaim>> {
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
    public RdaChange<PreAdjFissClaim> transformMessage(String apiVersion, FissClaimChange message) {
      var change = transformer.transformClaim(message);
      change.getClaim().setApiSource(apiVersion);
      if (change.getClaim().getMbiRecord() != null) {
        change.getClaim().getMbiRecord().setLastUpdated(null);
      }
      return change;
    }

    @Override
    public int writeClaims(Collection<RdaChange<PreAdjFissClaim>> objects)
        throws ProcessingException {
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
