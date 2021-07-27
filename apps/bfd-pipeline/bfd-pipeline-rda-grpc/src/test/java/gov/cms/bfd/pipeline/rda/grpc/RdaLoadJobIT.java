package gov.cms.bfd.pipeline.rda.grpc;

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
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import io.grpc.Server;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

  private PipelineApplicationState appState;

  @Before
  public void setUp() {
    final String dbUrl = String.format("jdbc:hsqldb:mem:testing-%d", System.currentTimeMillis());
    final DatabaseOptions dbOptions = new DatabaseOptions(dbUrl, "", "", 10);
    final MetricRegistry appMetrics = new MetricRegistry();
    final HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(dbOptions, appMetrics);
    DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
    appState =
        new PipelineApplicationState(appMetrics, pooledDataSource, RDA_PERSISTENCE_UNIT_NAME);
  }

  @After
  public void tearDown() throws Exception {
    if (appState != null) {
      appState.close();
      appState = null;
    }
  }

  @Test
  public void fissClaimsTest() throws Exception {
    final ImmutableList<String> fissClaimJson = fissClaimsSource.readLines();
    runServerTest(
        fissClaimJson,
        ImmutableList.of(),
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createFissClaimsLoadJob(appState);
          job.call();
        });
    runHibernateAssertions(
        entityManager -> {
          final ImmutableList<ClaimChange> expectedClaims =
              JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource::parseClaimChange);
          List<PreAdjFissClaim> claims =
              entityManager
                  .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
                  .getResultList();
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

  @Test
  public void mcsClaimsTest() throws Exception {
    final ImmutableList<String> mcsClaimJson = mcsClaimsSource.readLines();
    runServerTest(
        ImmutableList.of(),
        mcsClaimJson,
        port -> {
          final RdaLoadOptions config = createRdaLoadOptions(port);
          final PipelineJob<?> job = config.createMcsClaimsLoadJob(appState);
          job.call();
        });
    runHibernateAssertions(
        entityManager -> {
          final ImmutableList<ClaimChange> expectedClaims =
              JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource::parseClaimChange);
          List<PreAdjMcsClaim> claims =
              entityManager
                  .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
                  .getResultList();
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

  private static RdaLoadOptions createRdaLoadOptions(int serverPort) {
    return new RdaLoadOptions(
        new AbstractRdaLoadJob.Config(Duration.ofSeconds(1), 17),
        new GrpcRdaSource.Config("localhost", serverPort, Duration.ofMinutes(1)),
        new IdHasher.Config(100, "thisisjustatest"));
  }

  /**
   * Used to define lambdas that might throw a checked exception. Allowing the exception to pass
   * through makes it easier for a specific test to assert on it.
   */
  @FunctionalInterface
  private interface ThrowableConsumer<T> {
    void accept(T arg) throws Exception;
  }

  /**
   * Starts a server, runs a test with the server's port as a parameter, and then shuts down the
   * server once the test has finished running.
   *
   * @param fissClaimJson the FISS claims in JSON format, one per line
   * @param mcsClaimJson the MCS claims in JSON format, one per line
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  private static void runServerTest(
      List<String> fissClaimJson, List<String> mcsClaimJson, ThrowableConsumer<Integer> test)
      throws Exception {
    final Server server =
        RdaServer.startLocal(
            () -> new JsonMessageSource<>(fissClaimJson, JsonMessageSource::parseClaimChange),
            () -> new JsonMessageSource<>(mcsClaimJson, JsonMessageSource::parseClaimChange));
    try {
      test.accept(server.getPort());
    } finally {
      server.shutdown();
      server.awaitTermination(5, TimeUnit.SECONDS);
    }
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
