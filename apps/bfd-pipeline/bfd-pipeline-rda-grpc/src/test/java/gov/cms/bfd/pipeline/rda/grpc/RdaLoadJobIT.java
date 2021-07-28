package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.rda.grpc.server.RdaServer.runWithLocalServer;
import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.Assert.*;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.ExceptionMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RdaLoadJobIT {
  private static final CharSource fissClaimsSource =
      Resources.asCharSource(Resources.getResource("FISS.ndjson"), StandardCharsets.UTF_8);
  private static final CharSource mcsClaimsSource =
      Resources.asCharSource(Resources.getResource("MCS.ndjson"), StandardCharsets.UTF_8);
  private static final int BATCH_SIZE = 17;

  private ImmutableList<String> fissClaimJson;
  private ImmutableList<String> mcsClaimJson;
  private PipelineApplicationState appState;
  private Connection dbLifetimeConnection;

  @Before
  public void setUp() throws Exception {
    if (fissClaimJson == null) {
      fissClaimJson = fissClaimsSource.readLines();
    }
    if (mcsClaimJson == null) {
      mcsClaimJson = mcsClaimsSource.readLines();
    }
    final String dbUrl = "jdbc:hsqldb:mem:RdaLoadJobIT";
    // the HSQLDB database will be destroyed when this connection is closed
    dbLifetimeConnection = DriverManager.getConnection(dbUrl + ";shutdown=true", "", "");
    final DatabaseOptions dbOptions = new DatabaseOptions(dbUrl, "", "", 10);
    final MetricRegistry appMetrics = new MetricRegistry();
    final HikariDataSource dataSource =
        PipelineApplicationState.createPooledDataSource(dbOptions, appMetrics);
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);
    appState = new PipelineApplicationState(appMetrics, dataSource, RDA_PERSISTENCE_UNIT_NAME);
  }

  @After
  public void tearDown() throws Exception {
    if (appState != null) {
      appState.close();
      appState = null;
    }
    if (dbLifetimeConnection != null) {
      // ensures that the HSQLDB database is destroyed before the next test begins
      dbLifetimeConnection.close();
      dbLifetimeConnection = null;
    }
  }

  @Test
  public void fissClaimsTest() throws Exception {
    assertTablesAreEmpty();
    runWithLocalServer(
        jsonSource(fissClaimJson),
        EmptyMessageSource::new,
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createFissClaimsLoadJob(appState);
          job.call();
        });
    runHibernateAssertions(
        entityManager -> {
          final ImmutableList<ClaimChange> expectedClaims =
              JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource::parseClaimChange);
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
    assertTablesAreEmpty();
    final List<String> badFissClaimJson = new ArrayList<>(fissClaimJson);
    final int badClaimIndex = badFissClaimJson.size() - 1;
    final int fullBatchSize = badFissClaimJson.size() - badFissClaimJson.size() % BATCH_SIZE;
    badFissClaimJson.set(
        badClaimIndex,
        badFissClaimJson
            .get(badClaimIndex)
            .replaceAll("\"hicNo\":\"\\d+\"", "\"hicNo\":\"123456789012345\""));
    runWithLocalServer(
        jsonSource(badFissClaimJson),
        EmptyMessageSource::new,
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createFissClaimsLoadJob(appState);
          try {
            job.call();
            fail("expected an exception to be thrown");
          } catch (ProcessingException ex) {
            assertEquals(fullBatchSize, ex.getProcessedCount());
            assertEquals(true, ex.getMessage().contains("invalid length"));
          }
        });
    runHibernateAssertions(
        entityManager -> {
          List<PreAdjFissClaim> claims = getPreAdjFissClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  @Test
  public void mcsClaimsTest() throws Exception {
    assertTablesAreEmpty();
    runWithLocalServer(
        EmptyMessageSource::new,
        jsonSource(mcsClaimJson),
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createMcsClaimsLoadJob(appState);
          job.call();
        });
    runHibernateAssertions(
        entityManager -> {
          final ImmutableList<ClaimChange> expectedClaims =
              JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource::parseClaimChange);
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
    assertTablesAreEmpty();
    final int claimsToSendBeforeThrowing = mcsClaimJson.size() / 2;
    final int fullBatchSize = claimsToSendBeforeThrowing - claimsToSendBeforeThrowing % BATCH_SIZE;
    assertEquals(true, fullBatchSize > 0);
    runWithLocalServer(
        EmptyMessageSource::new,
        () ->
            new ExceptionMessageSource<>(
                new JsonMessageSource<>(mcsClaimJson, JsonMessageSource::parseClaimChange),
                claimsToSendBeforeThrowing,
                () -> new IOException("oops")),
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createMcsClaimsLoadJob(appState);
          try {
            job.call();
            fail("expected an exception to be thrown");
          } catch (ProcessingException ex) {
            assertEquals(fullBatchSize, ex.getProcessedCount());
            assertEquals(true, ex.getMessage().contains("UNKNOWN"));
          }
        });
    runHibernateAssertions(
        entityManager -> {
          List<PreAdjMcsClaim> claims = getPreAdjMcsClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  @Nullable
  private FissClaim findMatchingFissClaim(
      ImmutableList<ClaimChange> expectedClaims, PreAdjFissClaim resultClaim) {
    return expectedClaims.stream()
        .map(ClaimChange::getFissClaim)
        .filter(claim -> claim.getDcn().equals(resultClaim.getDcn()))
        .findAny()
        .orElse(null);
  }

  @Nullable
  private McsClaim findMatchingMcsClaim(
      ImmutableList<ClaimChange> expectedClaims, PreAdjMcsClaim resultClaim) {
    return expectedClaims.stream()
        .map(ClaimChange::getMcsClaim)
        .filter(claim -> claim.getIdrClmHdIcn().equals(resultClaim.getIdrClmHdIcn()))
        .findAny()
        .orElse(null);
  }

  private void assertTablesAreEmpty() throws Exception {
    runHibernateAssertions(
        entityManager -> {
          assertEquals(0, getPreAdjFissClaims(entityManager).size());
          assertEquals(0, getPreAdjMcsClaims(entityManager).size());
        });
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
        new AbstractRdaLoadJob.Config(Duration.ofSeconds(1), BATCH_SIZE),
        new GrpcRdaSource.Config("localhost", serverPort, Duration.ofMinutes(1)),
        new IdHasher.Config(100, "thisisjustatest"));
  }

  private Supplier<MessageSource<ClaimChange>> jsonSource(List<String> claimJson) {
    return () -> new JsonMessageSource<>(claimJson, JsonMessageSource::parseClaimChange);
  }

  /**
   * Sadly the EntityManager is not AutoCloseable so we need to use try/finally to close one. This
   * method creates one using our PipelineApplicationState, passes it to the provided lambda
   * function, and closes it before returning.
   *
   * @param assertions some lambda that uses the EntityManager.
   * @throws Exception any exception is passed through to the caller
   */
  private void runHibernateAssertions(ThrowableConsumer<EntityManager> assertions)
      throws Exception {
    final EntityManager entityManager = appState.getEntityManagerFactory().createEntityManager();
    try {
      assertions.accept(entityManager);
    } finally {
      entityManager.close();
    }
  }
}
