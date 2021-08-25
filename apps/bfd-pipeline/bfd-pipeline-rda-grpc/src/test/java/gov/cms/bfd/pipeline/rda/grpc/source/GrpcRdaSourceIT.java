package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

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

  @Test
  public void testGrpcCall() throws Exception {
    Server server = null;
    try {
      // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
      final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
      final MetricRegistry appMetrics = new MetricRegistry();
      final IdHasher hasher = new IdHasher(new IdHasher.Config(5, "pepper-pepper-pepper"));
      final String claimsJson = SOURCE_CLAIM_1 + System.lineSeparator() + SOURCE_CLAIM_2;
      server =
          RdaServer.startLocal(
              () ->
                  WrappedClaimSource.wrapFissClaims(
                      new JsonMessageSource<>(claimsJson, JsonMessageSource::parseFissClaim),
                      clock),
              EmptyMessageSource::new);
      final ManagedChannel channel =
          ManagedChannelBuilder.forAddress("localhost", server.getPort())
              .usePlaintext()
              .idleTimeout(5, TimeUnit.SECONDS)
              .build();
      final JsonCaptureSink sink = new JsonCaptureSink();
      int count;
      FissClaimStreamCaller streamCaller =
          new FissClaimStreamCaller(new FissClaimTransformer(clock, hasher));
      try (GrpcRdaSource<RdaChange<PreAdjFissClaim>> source =
          new GrpcRdaSource<>(channel, streamCaller, appMetrics, "fiss")) {
        count = source.retrieveAndProcessObjects(3, sink);
      }
      assertEquals(2, count);
      assertEquals(2, sink.getValues().size());
      assertEquals(
          "{\n"
              + "  \"dcn\" : \"63843470\",\n"
              + "  \"hicNo\" : \"916689703543\",\n"
              + "  \"currStatus\" : \"P\",\n"
              + "  \"currLoc1\" : \"M\",\n"
              + "  \"currLoc2\" : \"uma\",\n"
              + "  \"medaProvId\" : \"oducjgzt67joc\",\n"
              + "  \"totalChargeAmount\" : 3.75,\n"
              + "  \"currTranDate\" : \"2021-03-20\",\n"
              + "  \"principleDiag\" : \"uec\",\n"
              + "  \"mbi\" : \"c1ihk7q0g3i57\",\n"
              + "  \"mbiHash\" : \"56dd7e48bfbfcfe851d4cda2dbd863775b450aea207614a9cc118ed2765713e7\",\n"
              + "  \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
              + "  \"procCodes\" : [ {\n"
              + "    \"dcn\" : \"63843470\",\n"
              + "    \"priority\" : 2,\n"
              + "    \"procCode\" : \"zhaj\",\n"
              + "    \"procDate\" : \"2021-01-07\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  }, {\n"
              + "    \"dcn\" : \"63843470\",\n"
              + "    \"priority\" : 1,\n"
              + "    \"procCode\" : \"egkkkw\",\n"
              + "    \"procFlag\" : \"hsw\",\n"
              + "    \"procDate\" : \"2021-02-03\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  }, {\n"
              + "    \"dcn\" : \"63843470\",\n"
              + "    \"priority\" : 3,\n"
              + "    \"procCode\" : \"ods\",\n"
              + "    \"procDate\" : \"2021-01-03\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  }, {\n"
              + "    \"dcn\" : \"63843470\",\n"
              + "    \"priority\" : 0,\n"
              + "    \"procCode\" : \"uec\",\n"
              + "    \"procFlag\" : \"nli\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  } ],\n"
              + "  \"diagCodes\" : [ ],\n"
              + "  \"payers\" : [ ]\n"
              + "}",
          sink.getValues().get(0));
      assertEquals(
          "{\n"
              + "  \"dcn\" : \"2643602\",\n"
              + "  \"hicNo\" : \"640930211775\",\n"
              + "  \"currStatus\" : \"R\",\n"
              + "  \"currLoc1\" : \"O\",\n"
              + "  \"currLoc2\" : \"p6s\",\n"
              + "  \"totalChargeAmount\" : 55.91,\n"
              + "  \"receivedDate\" : \"2021-05-14\",\n"
              + "  \"currTranDate\" : \"2020-12-21\",\n"
              + "  \"principleDiag\" : \"egnj\",\n"
              + "  \"npiNumber\" : \"5764657700\",\n"
              + "  \"mbi\" : \"0vtc7u321x0se\",\n"
              + "  \"mbiHash\" : \"9c0e61338935c978c25f73442c5593cdc20e35164ad8d8e426955b626de24e2c\",\n"
              + "  \"fedTaxNumber\" : \"2845244764\",\n"
              + "  \"lastUpdated\" : \"2021-06-03T18:02:37Z\",\n"
              + "  \"procCodes\" : [ {\n"
              + "    \"dcn\" : \"2643602\",\n"
              + "    \"priority\" : 2,\n"
              + "    \"procCode\" : \"fipyd\",\n"
              + "    \"procFlag\" : \"g\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  }, {\n"
              + "    \"dcn\" : \"2643602\",\n"
              + "    \"priority\" : 1,\n"
              + "    \"procCode\" : \"vvqtwoz\",\n"
              + "    \"procDate\" : \"2021-04-29\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  }, {\n"
              + "    \"dcn\" : \"2643602\",\n"
              + "    \"priority\" : 0,\n"
              + "    \"procCode\" : \"egnj\",\n"
              + "    \"procDate\" : \"2021-05-13\",\n"
              + "    \"lastUpdated\" : \"2021-06-03T18:02:37Z\"\n"
              + "  } ],\n"
              + "  \"diagCodes\" : [ ],\n"
              + "  \"payers\" : [ ]\n"
              + "}",
          sink.getValues().get(1));
    } finally {
      if (server != null) {
        server.shutdown();
        server.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }

  private static class JsonCaptureSink implements RdaSink<RdaChange<PreAdjFissClaim>> {
    private final List<String> values = new ArrayList<>();
    private final ObjectMapper mapper;

    public JsonCaptureSink() {
      mapper =
          new JsonMapper()
              .enable(SerializationFeature.INDENT_OUTPUT)
              .registerModule(new Jdk8Module())
              .registerModule(new JavaTimeModule())
              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public synchronized int writeObject(RdaChange<PreAdjFissClaim> change)
        throws ProcessingException {
      try {
        values.add(mapper.writeValueAsString(change.getClaim()));
        return 1;
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }

    @Override
    public void close() throws Exception {}

    public synchronized List<String> getValues() {
      return values;
    }
  }
}
