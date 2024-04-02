package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

/** Integration test for the {@link StandardGrpcRdaSource}. */
public class StandardGrpcRdaSourceIT {
  /** Example paid claim. */
  private static final String SOURCE_CLAIM_1 =
      """
      {
        "timestamp": "2022-01-25T15:02:35Z",
        "seq": "1",
        "changeType": "CHANGE_TYPE_UPDATE",
        "rdaClaimKey": "63843470id",
        "dcn": "63843470",
        "intermediaryNb": "53412",
        "claim": {
          "rdaClaimKey": "63843470id",
          "dcn": "63843470",
          "intermediaryNb": "53412",
          "hicNo": "916689703543",
          "currStatusEnum": "CLAIM_STATUS_PAID",
          "currLoc1Enum": "PROCESSING_TYPE_MANUAL",
          "currLoc2Unrecognized": "uma",
          "totalChargeAmount": "3.75",
          "currTranDtCymd": "2021-03-20",
          "principleDiag": "uec",
          "mbi": "c1ihk7q0g3i",
          "clmTypIndEnum": "CLAIM_TYPE_INPATIENT",
          "fissProcCodes": [
            {
              "procCd": "uec",
              "procFlag": "nli",
              "rdaPosition": 1
            },
            {
              "procCd": "egkkkw",
              "procFlag": "hsw",
              "procDt": "2021-02-03",
              "rdaPosition": 2
            },
            {
              "procCd": "zhaj",
              "procDt": "2021-01-07",
              "rdaPosition": 3
            },
            {
              "procCd": "ods",
              "procDt": "2021-01-03",
              "rdaPosition": 4
            }
          ],
          "medaProvId": "oducjgzt67joc",
          "admTypCdEnum": "3"
        }
      }
      """
          .replaceAll("\n", "");

  /** Example rejected claim. */
  private static final String SOURCE_CLAIM_2 =
      """
      {
        "timestamp": "2022-01-25T15:02:35Z",
        "seq": "2",
        "changeType": "CHANGE_TYPE_UPDATE",
        "rdaClaimKey": "2643602id",
        "dcn": "2643602",
        "intermediaryNb": "24153",
        "claim": {
          "rdaClaimKey": "2643602id",
          "dcn": "2643602",
          "intermediaryNb": "24153",
          "hicNo": "640930211775",
          "currStatusEnum": "CLAIM_STATUS_REJECT",
          "currLoc1Enum": "PROCESSING_TYPE_OFFLINE",
          "currLoc2Unrecognized": "p6s",
          "totalChargeAmount": "55.91",
          "recdDtCymd": "2021-05-14",
          "currTranDtCymd": "2020-12-21",
          "principleDiag": "egnj",
          "npiNumber": "5764657700",
          "mbi": "0vtc7u321x0",
          "clmTypIndEnum": "CLAIM_TYPE_OUTPATIENT",
          "fedTaxNb": "2845244764",
          "fissProcCodes": [
            {
              "procCd": "egnj",
              "procDt": "2021-05-13",
              "rdaPosition": 1
            },
            {
              "procCd": "vvqtwoz",
              "procDt": "2021-04-29",
              "rdaPosition": 2
            },
            {
              "procCd": "fipyd",
              "procFlag": "g",
              "rdaPosition": 3
            }
          ],
          "admTypCdEnum": "3"
        }
      }
      """
          .replaceAll("\n", "");

  /** Expected paid claim. */
  public static final String EXPECTED_CLAIM_1 =
      "{\n"
          + "  \"admTypCd\" : \"3\",\n"
          + "  \"apiSource\" : \""
          + RdaService.RDA_PROTO_VERSION
          + "\",\n"
          + "  \"auditTrail\" : [ ],\n"
          + "  \"claimId\" : \"NjM4NDM0NzBpZA\",\n"
          + "  \"clmTypInd\" : \"1\",\n"
          + "  \"currLoc1\" : \"M\",\n"
          + "  \"currLoc2\" : \"uma\",\n"
          + "  \"currStatus\" : \"P\",\n"
          + "  \"currTranDate\" : \"2021-03-20\",\n"
          + "  \"dcn\" : \"63843470\",\n"
          + "  \"diagCodes\" : [ ],\n"
          + "  \"hicNo\" : \"916689703543\",\n"
          + "  \"intermediaryNb\" : \"53412\",\n"
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
          + "    \"claimId\" : \"NjM4NDM0NzBpZA\",\n"
          + "    \"procCode\" : \"egkkkw\",\n"
          + "    \"procDate\" : \"2021-02-03\",\n"
          + "    \"procFlag\" : \"hsw\",\n"
          + "    \"rdaPosition\" : 2\n"
          + "  }, {\n"
          + "    \"claimId\" : \"NjM4NDM0NzBpZA\",\n"
          + "    \"procCode\" : \"ods\",\n"
          + "    \"procDate\" : \"2021-01-03\",\n"
          + "    \"rdaPosition\" : 4\n"
          + "  }, {\n"
          + "    \"claimId\" : \"NjM4NDM0NzBpZA\",\n"
          + "    \"procCode\" : \"uec\",\n"
          + "    \"procFlag\" : \"nli\",\n"
          + "    \"rdaPosition\" : 1\n"
          + "  }, {\n"
          + "    \"claimId\" : \"NjM4NDM0NzBpZA\",\n"
          + "    \"procCode\" : \"zhaj\",\n"
          + "    \"procDate\" : \"2021-01-07\",\n"
          + "    \"rdaPosition\" : 3\n"
          + "  } ],\n"
          + "  \"revenueLines\" : [ ],\n"
          + "  \"sequenceNumber\" : 1,\n"
          + "  \"totalChargeAmount\" : 3.75\n"
          + "}";

  /** Example rejected claim. */
  public static final String EXPECTED_CLAIM_2 =
      "{\n"
          + "  \"admTypCd\" : \"3\",\n"
          + "  \"apiSource\" : \""
          + RdaService.RDA_PROTO_VERSION
          + "\",\n"
          + "  \"auditTrail\" : [ ],\n"
          + "  \"claimId\" : \"MjY0MzYwMmlk\",\n"
          + "  \"clmTypInd\" : \"3\",\n"
          + "  \"currLoc1\" : \"O\",\n"
          + "  \"currLoc2\" : \"p6s\",\n"
          + "  \"currStatus\" : \"R\",\n"
          + "  \"currTranDate\" : \"2020-12-21\",\n"
          + "  \"dcn\" : \"2643602\",\n"
          + "  \"diagCodes\" : [ ],\n"
          + "  \"fedTaxNumber\" : \"2845244764\",\n"
          + "  \"hicNo\" : \"640930211775\",\n"
          + "  \"intermediaryNb\" : \"24153\",\n"
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
          + "    \"claimId\" : \"MjY0MzYwMmlk\",\n"
          + "    \"procCode\" : \"egnj\",\n"
          + "    \"procDate\" : \"2021-05-13\",\n"
          + "    \"rdaPosition\" : 1\n"
          + "  }, {\n"
          + "    \"claimId\" : \"MjY0MzYwMmlk\",\n"
          + "    \"procCode\" : \"fipyd\",\n"
          + "    \"procFlag\" : \"g\",\n"
          + "    \"rdaPosition\" : 3\n"
          + "  }, {\n"
          + "    \"claimId\" : \"MjY0MzYwMmlk\",\n"
          + "    \"procCode\" : \"vvqtwoz\",\n"
          + "    \"procDate\" : \"2021-04-29\",\n"
          + "    \"rdaPosition\" : 2\n"
          + "  } ],\n"
          + "  \"receivedDate\" : \"2021-05-14\",\n"
          + "  \"revenueLines\" : [ ],\n"
          + "  \"sequenceNumber\" : 2,\n"
          + "  \"totalChargeAmount\" : 55.91\n"
          + "}";

  /** Clock for creating for consistent values in JSON (2021-06-03T18:02:37Z). */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);

  /** The test hasher. */
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(5, "pepper-pepper-pepper"));

  /** The transformer to create results for correctness verification. */
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(hasher.getConfig()));

  /** The stream caller for calling the service. */
  private final FissClaimStreamCaller streamCaller = new FissClaimStreamCaller();

  /** The test metrics. */
  private MeterRegistry appMetrics;

  /** The json sink. */
  private JsonCaptureSink sink;

  /** The RdaVersion to require. */
  private RdaVersion rdaVersion;

  /**
   * Sets the test dependencies up.
   *
   * @throws Exception if there is an error setting up the test
   */
  @BeforeEach
  public void setUp() throws Exception {
    appMetrics = new SimpleMeterRegistry();
    sink = new JsonCaptureSink();
    rdaVersion = RdaVersion.builder().versionString("~" + RdaService.RDA_PROTO_VERSION).build();
  }

  /**
   * Verifies that a GRPC call without an auth token required can successfully return claims.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void grpcCallNoAuthTokenNeeded() throws Exception {
    createServerConfig()
        .build()
        .runWithPortParam(
            port -> {
              int count;
              RdaSourceConfig config = createSourceConfig(port).build();
              try (StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                  createSource(config)) {
                count = source.retrieveAndProcessObjects(3, sink);
              }
              assertEquals(2, count);
              assertEquals(2, sink.getValues().size());
              assertEquals(EXPECTED_CLAIM_1, sink.getValues().get(0));
              assertEquals(EXPECTED_CLAIM_2, sink.getValues().get(1));
            });
  }

  /**
   * Verifies that a GRPC call with an auth token required and supplied can successfully return
   * claims.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void grpcCallWithCorrectAuthToken() throws Exception {

    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              int count;
              RdaSourceConfig config =
                  createSourceConfig(port).authenticationToken("secret").build();
              try (StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                  createSource(config)) {
                count = source.retrieveAndProcessObjects(3, sink);
              }
              assertEquals(2, count);
              assertEquals(2, sink.getValues().size());
              assertEquals(EXPECTED_CLAIM_1, sink.getValues().get(0));
              assertEquals(EXPECTED_CLAIM_2, sink.getValues().get(1));
            });
  }

  /** Verifies that a GRPC call with an incompatible RDA version will throw an exception. */
  @Test
  public void grpcCallWithIncompatibleRdaVersion() {
    final RdaVersion requireHigherRdaVersion =
        RdaVersion.builder().versionString("100.100.100").build();

    try {
      createServerConfig()
          .authorizedToken("secret")
          .build()
          .runWithPortParam(
              port -> {
                RdaSourceConfig config =
                    createSourceConfig(port).authenticationToken("secret").build();
                try (StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
                    new StandardGrpcRdaSource<>(
                        config,
                        streamCaller,
                        appMetrics,
                        "fiss",
                        Optional.empty(),
                        requireHigherRdaVersion)) {
                  source.retrieveAndProcessObjects(3, sink);
                }
              });
      fail("Should have thrown exception");
    } catch (Exception e) {
      assertEquals(e.getCause().getClass(), IllegalStateException.class);
      assertEquals(
          e.getCause().getMessage(),
          String.format(
              "Can not ingest data from API running version '%s'", RdaService.RDA_PROTO_VERSION));
    }
  }

  /**
   * Verifies that a GRPC call with an auth token required and no token supplied throws an auth
   * exception.
   *
   * @throws Exception indicates test failure (correct exception is caught)
   */
  @Test
  public void grpcCallWithMissingAuthToken() throws Exception {
    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              RdaSourceConfig config = createSourceConfig(port).build();
              try {
                try (StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
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

  /**
   * Verifies that a GRPC call with an auth token required and an incorrect token supplied throws an
   * auth exception.
   *
   * @throws Exception indicates test failure (correct exception is caught)
   */
  @Test
  public void grpcCallWithIncorrectAuthToken() throws Exception {
    createServerConfig()
        .authorizedToken("secret")
        .build()
        .runWithPortParam(
            port -> {
              RdaSourceConfig config =
                  createSourceConfig(port).authenticationToken("wrong-secret").build();
              try {
                try (StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> source =
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

  /**
   * Creates the local server config for the test.
   *
   * @return the server config
   */
  private RdaServer.LocalConfig.LocalConfigBuilder createServerConfig() {
    return RdaServer.LocalConfig.builder()
        .serviceConfig(
            RdaMessageSourceFactory.Config.builder()
                .fissClaimJsonList(List.of(SOURCE_CLAIM_1, SOURCE_CLAIM_2))
                .build());
  }

  /**
   * Creates the source config for the test.
   *
   * @param port the port to use
   * @return the server config
   */
  private RdaSourceConfig.RdaSourceConfigBuilder createSourceConfig(Integer port) {
    return RdaSourceConfig.builder()
        .serverType(RdaSourceConfig.ServerType.Remote)
        .host("localhost")
        .port(port)
        .maxIdle(Duration.ofSeconds(30));
  }

  /**
   * Creates the grpc source from a config.
   *
   * @param config the config for the source
   * @return the grpc rda source
   */
  @Nonnull
  private StandardGrpcRdaSource<FissClaimChange, RdaChange<RdaFissClaim>> createSource(
      RdaSourceConfig config) {
    return new StandardGrpcRdaSource<>(
        config, streamCaller, appMetrics, "fiss", Optional.empty(), rdaVersion);
  }

  /** The sink for json data. */
  private class JsonCaptureSink implements RdaSink<FissClaimChange, RdaChange<RdaFissClaim>> {
    /** The values being written. */
    private final List<String> values = new ArrayList<>();

    /** The mapper for the json mapping config. */
    private final ObjectMapper mapper;

    /** Creates a json capture sink with a set configuration. */
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
        var changeOpt = transformMessage(dataVersion, message);
        values.add(mapper.writeValueAsString(changeOpt.get().getClaim()));
        return 1;
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }

    @Override
    public void checkErrorCount() {
      // Do nothing
    }

    @Override
    public String getClaimIdForMessage(FissClaimChange object) {
      return object.getClaim().getRdaClaimKey();
    }

    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {}

    @Override
    public long getSequenceNumberForObject(FissClaimChange object) {
      return object.getSeq();
    }

    @Nonnull
    @Override
    public Optional<RdaChange<RdaFissClaim>> transformMessage(
        String apiVersion, FissClaimChange message) {
      var change = transformer.transformClaim(message);
      change.getClaim().setApiSource(apiVersion);
      if (change.getClaim().getMbiRecord() != null) {
        change.getClaim().getMbiRecord().setLastUpdated(null);
      }
      return Optional.of(change);
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

    /**
     * Gets the values.
     *
     * @return the values
     */
    public synchronized List<String> getValues() {
      return values;
    }
  }
}
