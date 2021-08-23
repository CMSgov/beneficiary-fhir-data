package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.rda.grpc.server.RdaServer.runWithLocalServer;
import static org.junit.Assert.*;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.ExceptionMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;

public class RdaLoadJobIT {
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  private static final CharSource fissClaimsSource =
      Resources.asCharSource(Resources.getResource("FISS.ndjson"), StandardCharsets.UTF_8);
  private static final CharSource mcsClaimsSource =
      Resources.asCharSource(Resources.getResource("MCS.ndjson"), StandardCharsets.UTF_8);
  private static final int BATCH_SIZE = 17;

  private ImmutableList<String> fissClaimJson;
  private ImmutableList<String> mcsClaimJson;

  @Before
  public void setUp() throws Exception {
    if (fissClaimJson == null) {
      fissClaimJson = fissClaimsSource.readLines();
    }
    if (mcsClaimJson == null) {
      mcsClaimJson = mcsClaimsSource.readLines();
    }
  }

  @Test
  public void fissClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          runWithLocalServer(
              fissJsonSource(fissClaimJson),
              EmptyMessageSource.factory(),
              port -> {
                final RdaLoadOptions config = createRdaLoadOptions(port);
                final PipelineJob<?> job = config.createFissClaimsLoadJob(appState);
                job.call();
              });
          final ImmutableList<FissClaimChange> expectedClaims =
              JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource::parseFissClaimChange);
          List<PreAdjFissClaim> claims = getPreAdjFissClaims(entityManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (PreAdjFissClaim resultClaim : claims) {
            FissClaim expected = findMatchingFissClaim(expectedClaims, resultClaim);
            assertNotNull(expected);
            assertEquals(expected.getHicNo(), resultClaim.getHicNo());
            assertEquals(
                expected.getPracLocCity(), Strings.nullToEmpty(resultClaim.getPracLocCity()));
            assertEquals(expected.getFissProcCodesCount(), resultClaim.getProcCodes().size());
            assertEquals(expected.getFissDiagCodesCount(), resultClaim.getDiagCodes().size());
          }
        });
  }

  /**
   * Verifies that an invalid FISS claim terminates the job and that all complete batches prior to
   * the bad claim have been written to the database.
   */
  @Test
  public void invalidFissClaimTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          final List<String> badFissClaimJson = new ArrayList<>(fissClaimJson);
          final int badClaimIndex = badFissClaimJson.size() - 1;
          final int fullBatchSize = badFissClaimJson.size() - badFissClaimJson.size() % BATCH_SIZE;
          badFissClaimJson.set(
              badClaimIndex,
              badFissClaimJson
                  .get(badClaimIndex)
                  .replaceAll("\"hicNo\":\"\\d+\"", "\"hicNo\":\"123456789012345\""));
          runWithLocalServer(
              fissJsonSource(badFissClaimJson),
              EmptyMessageSource.factory(),
              port -> {
                final RdaLoadOptions config = createRdaLoadOptions(port);
                final RdaFissClaimLoadJob job = config.createFissClaimsLoadJob(appState);
                try {
                  job.callRdaServiceAndStoreRecords();
                  fail("expected an exception to be thrown");
                } catch (ProcessingException ex) {
                  assertEquals(fullBatchSize, ex.getProcessedCount());
                  assertEquals(true, ex.getMessage().contains("invalid length"));
                }
              });
          List<PreAdjFissClaim> claims = getPreAdjFissClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  @Test
  public void mcsClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          runWithLocalServer(
              EmptyMessageSource.factory(),
              mcsJsonSource(mcsClaimJson),
              port -> {
                final RdaLoadOptions config = createRdaLoadOptions(port);
                final PipelineJob<?> job = config.createMcsClaimsLoadJob(appState);
                job.call();
              });
          final ImmutableList<McsClaimChange> expectedClaims =
              JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource::parseMcsClaimChange);
          List<PreAdjMcsClaim> claims = getPreAdjMcsClaims(entityManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (PreAdjMcsClaim resultClaim : claims) {
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
   */
  @Test
  public void serverExceptionTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          final int claimsToSendBeforeThrowing = mcsClaimJson.size() / 2;
          final int fullBatchSize =
              claimsToSendBeforeThrowing - claimsToSendBeforeThrowing % BATCH_SIZE;
          assertEquals(true, fullBatchSize > 0);
          runWithLocalServer(
              EmptyMessageSource.factory(),
              ignored ->
                  new ExceptionMessageSource<>(
                      new JsonMessageSource<>(mcsClaimJson, JsonMessageSource::parseMcsClaimChange),
                      claimsToSendBeforeThrowing,
                      () -> new IOException("oops")),
              port -> {
                final RdaLoadOptions config = createRdaLoadOptions(port);
                final RdaMcsClaimLoadJob job = config.createMcsClaimsLoadJob(appState);
                try {
                  job.callRdaServiceAndStoreRecords();
                  fail("expected an exception to be thrown");
                } catch (ProcessingException ex) {
                  assertEquals(fullBatchSize, ex.getProcessedCount());
                  assertEquals(true, ex.getOriginalCause() instanceof StatusRuntimeException);
                }
              });
          List<PreAdjMcsClaim> claims = getPreAdjMcsClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  @Nullable
  private FissClaim findMatchingFissClaim(
      ImmutableList<FissClaimChange> expectedClaims, PreAdjFissClaim resultClaim) {
    return expectedClaims.stream()
        .map(FissClaimChange::getClaim)
        .filter(claim -> claim.getDcn().equals(resultClaim.getDcn()))
        .findAny()
        .orElse(null);
  }

  @Nullable
  private McsClaim findMatchingMcsClaim(
      ImmutableList<McsClaimChange> expectedClaims, PreAdjMcsClaim resultClaim) {
    return expectedClaims.stream()
        .map(McsClaimChange::getClaim)
        .filter(claim -> claim.getIdrClmHdIcn().equals(resultClaim.getIdrClmHdIcn()))
        .findAny()
        .orElse(null);
  }

  private void assertTablesAreEmpty(EntityManager entityManager) throws Exception {
    assertEquals(0, getPreAdjFissClaims(entityManager).size());
    assertEquals(0, getPreAdjMcsClaims(entityManager).size());
  }

  private List<PreAdjMcsClaim> getPreAdjMcsClaims(EntityManager entityManager) {
    return entityManager
        .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
        .getResultList();
  }

  private List<PreAdjFissClaim> getPreAdjFissClaims(EntityManager entityManager) {
    return entityManager
        .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
        .getResultList();
  }

  private static RdaLoadOptions createRdaLoadOptions(int serverPort) {
    return new RdaLoadOptions(
        new AbstractRdaLoadJob.Config(
            Duration.ofSeconds(1), BATCH_SIZE, Optional.empty(), Optional.empty()),
        new GrpcRdaSource.Config("localhost", serverPort, Duration.ofMinutes(1)),
        new IdHasher.Config(100, "thisisjustatest"));
  }

  private MessageSource.Factory<FissClaimChange> fissJsonSource(List<String> claimJson) {
    return sequenceNumber ->
        new JsonMessageSource<>(claimJson, JsonMessageSource::parseFissClaimChange)
            .skip(sequenceNumber);
  }

  private MessageSource.Factory<McsClaimChange> mcsJsonSource(List<String> claimJson) {
    return sequenceNumber ->
        new JsonMessageSource<>(claimJson, JsonMessageSource::parseMcsClaimChange)
            .skip(sequenceNumber);
  }
}
