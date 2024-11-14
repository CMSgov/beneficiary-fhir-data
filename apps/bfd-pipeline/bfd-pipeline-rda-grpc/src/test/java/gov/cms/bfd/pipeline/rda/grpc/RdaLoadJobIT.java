package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.samhsa.FissTag;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The integration test for the full RDA load job. */
public class RdaLoadJobIT {

  /** Arbitrary RDA API version to use for testing. */
  private static final RdaService.Version ARBITRARY_RDA_VERSION =
      RdaService.Version.builder().version("0.0.1").build();

  /** Clock for making timestamps. using a fixed Clock ensures our timestamp is predictable. */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);

  /** The test fiss claim source. */
  private static final CharSource fissClaimsSource =
      Resources.asCharSource(Resources.getResource("FISS.ndjson"), StandardCharsets.UTF_8);

  /** The test MCS claim source. */
  private static final CharSource mcsClaimsSource =
      Resources.asCharSource(Resources.getResource("MCS.ndjson"), StandardCharsets.UTF_8);

  /** The batch size to use for testing. */
  private static final int BATCH_SIZE = 17;

  /** List of json fiss claims to load. */
  private ImmutableList<String> fissClaimJson;

  /** List of json MCS claims to load. */
  private ImmutableList<String> mcsClaimJson;

  /**
   * Sets up the test resources.
   *
   * @throws Exception if there is an issue setting the test up
   */
  @BeforeEach
  public void setUp() throws Exception {
    if (fissClaimJson == null) {
      fissClaimJson = fissClaimsSource.readLines();
    }
    if (mcsClaimJson == null) {
      mcsClaimJson = mcsClaimsSource.readLines();
    }
  }

  /**
   * All of our test claims should be valid for our IT tests to succeed. This test ensures this is
   * the case and catches any incompatibility issues when a new RDA API version contains breaking
   * changes.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void fissClaimsAreValid() throws Exception {
    final ImmutableList<FissClaimChange> expectedClaims =
        JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource.fissParser());
    final FissClaimTransformer transformer =
        new FissClaimTransformer(clock, MbiCache.computedCache(new IdHasher.Config(1, "testing")));
    for (FissClaimChange claim : expectedClaims) {
      try {
        transformer.transformClaim(claim);
      } catch (DataTransformer.TransformationException ex) {
        fail(String.format("bad sample claim: seq=%d error=%s", claim.getSeq(), ex.getErrors()));
      }
    }
  }

  /**
   * Verifies that Fiss JSON files can be parsed and the claims have the expected values.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void fissClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        clock,
        (appState, transactionManager) -> {
          assertTablesAreEmpty(transactionManager);
          RdaServer.LocalConfig.builder()
              .serviceConfig(
                  RdaMessageSourceFactory.Config.builder()
                      .version(ARBITRARY_RDA_VERSION)
                      .fissClaimJsonList(fissClaimJson)
                      .build())
              .build()
              .runWithPortParam(
                  port -> {
                    final RdaLoadOptions config = createRdaLoadOptions(port);
                    final var mbiCache = config.createComputedMbiCache(appState);
                    final PipelineJob job = config.createFissClaimsLoadJob(appState, mbiCache);
                    job.call();
                  });
          final ImmutableList<FissClaimChange> expectedClaims =
              JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource.fissParser());
          List<RdaFissClaim> claims = getRdaFissClaims(transactionManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (RdaFissClaim resultClaim : claims) {
            FissClaim expected = findMatchingFissClaim(expectedClaims, resultClaim);
            assertNotNull(expected);
            assertEquals(expected.getHicNo(), resultClaim.getHicNo());
            assertEquals(
                expected.getPracLocCity(), Strings.nullToEmpty(resultClaim.getPracLocCity()));
            assertEquals(expected.getFissProcCodesCount(), resultClaim.getProcCodes().size());
            assertEquals(expected.getFissDiagCodesCount(), resultClaim.getDiagCodes().size());
          }
          List<FissTag> fissTags = getRdaFissTags(transactionManager);
          // There should be one matching claim, with two FissTag entries.
          assertEquals(fissTags.size(), 2);
          assertTrue(fissTags.stream().anyMatch(f -> f.getCode().equals("42CFRPart2")));
          assertTrue(fissTags.stream().anyMatch(f -> f.getCode().equals("R")));
          // There should be five hits for SAMHSA codes in this claim (one for each revenueLine).
          assertEquals(5, fissTags.getFirst().getDetails().size());
        });
  }

  /**
   * Verifies that an invalid FISS claim terminates the job and that all complete batches prior to
   * the bad claim have been written to the database.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void invalidFissClaimTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        clock,
        (appState, transactionManager) -> {
          assertTablesAreEmpty(transactionManager);
          final List<String> badFissClaimJson = new ArrayList<>(fissClaimJson);
          final int badClaimIndex = badFissClaimJson.size() - 2;
          final int fullBatchSize = badFissClaimJson.size() - badFissClaimJson.size() % BATCH_SIZE;
          badFissClaimJson.set(
              badClaimIndex,
              badFissClaimJson
                  .get(badClaimIndex)
                  .replaceAll("\"hicNo\":\"\\d+\"", "\"hicNo\":\"123456789012345\""));
          RdaServer.LocalConfig.builder()
              .serviceConfig(
                  RdaMessageSourceFactory.Config.builder()
                      .version(ARBITRARY_RDA_VERSION)
                      .fissClaimJsonList(badFissClaimJson)
                      .build())
              .build()
              .runWithPortParam(
                  port -> {
                    final RdaLoadOptions config = createRdaLoadOptions(port);
                    final var mbiCache = config.createComputedMbiCache(appState);
                    final RdaFissClaimLoadJob job =
                        config.createFissClaimsLoadJob(appState, mbiCache);
                    try {
                      job.callRdaServiceAndStoreRecords();
                      fail("expected an exception to be thrown");
                    } catch (ProcessingException ex) {
                      assertEquals(fullBatchSize, ex.getProcessedCount());
                      assertTrue(ex.getMessage().contains("Error limit reached"));
                    }
                  });
          List<RdaFissClaim> claims = getRdaFissClaims(transactionManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  /**
   * All of our test claims should be valid for our IT tests to succeed. This test ensures this is
   * the case and catches any incompatibility issues when a new RDA API version contains breaking
   * changes.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void mcsClaimsAreValid() throws Exception {
    final ImmutableList<McsClaimChange> expectedClaims =
        JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource.mcsParser());
    final McsClaimTransformer transformer =
        new McsClaimTransformer(clock, MbiCache.computedCache(new IdHasher.Config(1, "testing")));
    for (McsClaimChange claim : expectedClaims) {
      try {
        transformer.transformClaim(claim);
      } catch (DataTransformer.TransformationException ex) {
        fail(String.format("bad sample claim: seq=%d error=%s", claim.getSeq(), ex.getErrors()));
      }
    }
  }

  /**
   * Verifies that MCS JSON files can be parsed and the claims have the expected values.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void mcsClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        clock,
        (appState, transactionManager) -> {
          assertTablesAreEmpty(transactionManager);
          RdaServer.InProcessConfig.builder()
              .serviceConfig(
                  RdaMessageSourceFactory.Config.builder()
                      .version(ARBITRARY_RDA_VERSION)
                      .mcsClaimJsonList(mcsClaimJson)
                      .build())
              .serverName(RdaServerJob.Config.DEFAULT_SERVER_NAME)
              .build()
              .runWithNoParam(
                  () -> {
                    final var config = createRdaLoadOptions(-1);
                    final var mbiCache = config.createComputedMbiCache(appState);
                    final PipelineJob job = config.createMcsClaimsLoadJob(appState, mbiCache);
                    job.call();
                  });
          final ImmutableList<McsClaimChange> expectedClaims =
              JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource.mcsParser());
          List<RdaMcsClaim> claims = getRdaMcsClaims(transactionManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (RdaMcsClaim resultClaim : claims) {
            McsClaim expected = findMatchingMcsClaim(expectedClaims, resultClaim);
            assertNotNull(expected);
            assertEquals(expected.getIdrHic(), Strings.nullToEmpty(resultClaim.getIdrHic()));
            assertEquals(
                expected.getIdrClaimMbi(), Strings.nullToEmpty(resultClaim.getIdrClaimMbi()));
            assertEquals(expected.getMcsDetailsCount(), resultClaim.getDetails().size());
            assertEquals(expected.getMcsDiagnosisCodesCount(), resultClaim.getDiagCodes().size());
          }
        });
  }

  /**
   * Verifies that a Server error terminates the job and that all complete batches prior to the bad
   * claim have been written to the database.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void serverExceptionTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        clock,
        (appState, transactionManager) -> {
          assertTablesAreEmpty(transactionManager);
          final int claimsToSendBeforeThrowing = mcsClaimJson.size() / 2;
          final int fullBatchSize =
              claimsToSendBeforeThrowing - claimsToSendBeforeThrowing % BATCH_SIZE;
          assertTrue(fullBatchSize > 0);
          RdaServer.LocalConfig.builder()
              .serviceConfig(
                  RdaMessageSourceFactory.Config.builder()
                      .version(ARBITRARY_RDA_VERSION)
                      .mcsClaimJsonList(mcsClaimJson)
                      .throwExceptionAfterCount(claimsToSendBeforeThrowing)
                      .build())
              .build()
              .runWithPortParam(
                  port -> {
                    final var config = createRdaLoadOptions(port);
                    final var mbiCache = config.createComputedMbiCache(appState);
                    final RdaMcsClaimLoadJob job =
                        config.createMcsClaimsLoadJob(appState, mbiCache);
                    try {
                      job.callRdaServiceAndStoreRecords();
                      fail("expected an exception to be thrown");
                    } catch (ProcessingException ex) {
                      assertEquals(fullBatchSize, ex.getProcessedCount());
                      assertTrue(ex.getOriginalCause() instanceof StatusRuntimeException);
                    }
                  });
          List<RdaMcsClaim> claims = getRdaMcsClaims(transactionManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  /**
   * Finds a matching Fiss claim from the list of expected claims. If not found, returns {@code
   * null}.
   *
   * @param expectedClaims the expected claims to search through
   * @param resultClaim the result claim to search for
   * @return the fiss claim if found, else {@code null}
   */
  @Nullable
  private FissClaim findMatchingFissClaim(
      ImmutableList<FissClaimChange> expectedClaims, RdaFissClaim resultClaim) {
    final String decodedClaimId =
        new String(
            Base64.getUrlDecoder()
                .decode(resultClaim.getClaimId().getBytes(StandardCharsets.UTF_8)));
    return expectedClaims.stream()
        .map(FissClaimChange::getClaim)
        .filter(claim -> claim.getRdaClaimKey().equals(decodedClaimId))
        .findAny()
        .orElse(null);
  }

  /**
   * Finds a matching MCS claim from the list of expected claims. If not found, returns {@code
   * null}.
   *
   * @param expectedClaims the expected claims to search through
   * @param resultClaim the result claim to search for
   * @return the fiss claim if found, else {@code null}
   */
  @Nullable
  private McsClaim findMatchingMcsClaim(
      ImmutableList<McsClaimChange> expectedClaims, RdaMcsClaim resultClaim) {
    return expectedClaims.stream()
        .map(McsClaimChange::getClaim)
        .filter(claim -> claim.getIdrClmHdIcn().equals(resultClaim.getIdrClmHdIcn()))
        .findAny()
        .orElse(null);
  }

  /**
   * Asserts that the Fiss and MCS tables are empty.
   *
   * @param transactionManager the transaction manager
   */
  private void assertTablesAreEmpty(TransactionManager transactionManager) {
    assertEquals(0, getRdaFissClaims(transactionManager).size());
    assertEquals(0, getRdaMcsClaims(transactionManager).size());
  }

  /**
   * Gets the MCS claims from the database using a query.
   *
   * @param transactionManager the transaction manager to connect to the database
   * @return the rda mcs claims
   */
  private List<RdaMcsClaim> getRdaMcsClaims(TransactionManager transactionManager) {
    return transactionManager.executeFunction(
        entityManager ->
            entityManager
                .createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class)
                .getResultList());
  }

  /**
   * Gets the Fiss claims from the database using a query.
   *
   * @param transactionManager the transaction manager to connect to the database
   * @return the rda fiss claims
   */
  private List<RdaFissClaim> getRdaFissClaims(TransactionManager transactionManager) {
    return transactionManager.executeFunction(
        entityManager ->
            entityManager
                .createQuery("select c from RdaFissClaim c", RdaFissClaim.class)
                .getResultList());
  }
  private List<FissTag> getRdaFissTags(TransactionManager transactionManager) {
      return transactionManager.executeFunction(
              entityManager ->
                      entityManager
                              .createQuery("select c from FissTag c", FissTag.class)
                              .getResultList());

  }
  /**
   * Creates the RDA load options.
   *
   * @param serverPort the server port to use
   * @return the rda load options
   */
  private static RdaLoadOptions createRdaLoadOptions(int serverPort) {
    final RdaSourceConfig.RdaSourceConfigBuilder rdaSourceConfig = RdaSourceConfig.builder();
    if (serverPort > 0) {
      rdaSourceConfig
          .serverType(RdaSourceConfig.ServerType.Remote)
          .host("localhost")
          .port(serverPort);
    } else {
      rdaSourceConfig
          .serverType(RdaSourceConfig.ServerType.InProcess)
          .inProcessServerName(RdaServerJob.Config.DEFAULT_SERVER_NAME);
    }
    rdaSourceConfig.maxIdle(Duration.ofMinutes(1));
    rdaSourceConfig.authenticationToken("secret-token");
    return new RdaLoadOptions(
        AbstractRdaLoadJob.Config.builder()
            .runInterval(Duration.ofSeconds(1))
            .batchSize(BATCH_SIZE)
            .rdaVersion(
                RdaVersion.builder()
                    .versionString("~" + ARBITRARY_RDA_VERSION.getVersion())
                    .build())
            .build(),
        rdaSourceConfig.build(),
        new RdaServerJob.Config(),
        0,
        new IdHasher.Config(100, "thisisjustatest"));
  }
}
