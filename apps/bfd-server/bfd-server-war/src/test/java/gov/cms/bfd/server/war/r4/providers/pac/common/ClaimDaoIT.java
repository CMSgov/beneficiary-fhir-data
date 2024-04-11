package gov.cms.bfd.server.war.r4.providers.pac.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.r4.providers.pac.ClaimResponseTypeV2;
import gov.cms.bfd.server.war.r4.providers.pac.ClaimTypeV2;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Integration tests for the {@link gov.cms.bfd.server.war.r4.providers.pac.common.ClaimDao}. */
public class ClaimDaoIT {
  /** Test utils. */
  private static final RDATestUtils testUtils = new RDATestUtils();

  /** The test metric registry. */
  private final MetricRegistry metricRegistry = new MetricRegistry();

  /** Initializes test resources. */
  @BeforeAll
  public static void init() {
    testUtils.init();
  }

  /** Cleans up testing resources after all tests have run. */
  @AfterAll
  public static void tearDown() {
    testUtils.destroy();
  }

  /** Cleans up the database resources after each test. */
  @AfterEach
  public void cleanupDatabase() {
    testUtils.getEntityManager().clear();
    testUtils.truncateTables();
  }

  /**
   * Verifies that doing a claims search with a known MBI will return the expected number of claims.
   */
  @Test
  public void verifyQueryWithKnownMbiFindsMatch() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiQuery(claimDao, RDATestUtils.MBI);
    assertEquals(2, claims.size());
  }

  /** Verifies that doing a claims search with an unknown MBI will return no claims. */
  @Test
  public void verifyQueryWithUnknownMbiFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiQuery(claimDao, "not-an-mbi");
    assertEquals(0, claims.size());
  }

  /**
   * Verifies that doing a claims search with a known MBI hash will return the expected number of
   * claims.
   */
  @Test
  public void verifyQueryWithKnownMbiHashFindsMatch() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runMcsMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
    assertEquals(2, claims.size());
  }

  /** Verifies that doing a claims search with an unknown MBI hash will return no claims. */
  @Test
  public void verifyQueryWithUnknownMbiHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runMcsMbiHashQuery(claimDao, "not-an-mbi-hash");
    assertEquals(0, claims.size());
  }

  /**
   * Verifies that doing a claims search with an old MBI hash returns no results if old hash was
   * disabled.
   */
  @Test
  public void verifyQueryWithOldHashDisabledIgnoresOldHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(true);
    var claims = runMcsMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
    assertEquals(0, claims.size());
  }

  /**
   * Verifies that doing a claims search with an MBI hash returns the expected number of claims if
   * old hash was enabled.
   */
  @Test
  public void verifyQueryWithOldHashEnabledFindsHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runMcsMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
    assertEquals(2, claims.size());
  }

  /**
   * Verifies that doing a claims search with an old MBI hash returns the expected number of claims
   * if old hash was enabled.
   */
  @Test
  public void verifyQueryWithOldHashEnabledFindsOldHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runMcsMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
    assertEquals(2, claims.size());
  }

  /**
   * Verifies that doing a claims search with an unknown MBI hash returns no results even with old
   * hash enabled.
   */
  @Test
  public void verifyQueryWithOldHashEnabledAndUnknownHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runMcsMbiHashQuery(claimDao, "not-a-hash");
    assertEquals(0, claims.size());
  }

  /**
   * Generates parameters for {@link ClaimDaoIT#testMcsServiceDateQuery}.
   *
   * @return all test parameters
   */
  private static Stream<ServiceDateQueryParam> getFissServiceDateQueryParameters() {
    final var goodMbiHash = RDATestUtils.MBI_HASH;
    final var badMbiHash = "not-a-valid-hash";
    return Stream.of(
        new ServiceDateQueryParam(
            "no-dates-matches-all",
            goodMbiHash,
            null,
            null,
            List.of(RDATestUtils.FISS_CLAIM_A_CLAIM_ID, RDATestUtils.FISS_CLAIM_B_CLAIM_ID)),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-no-dates", badMbiHash, null, null, List.of()),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-last-updated",
            badMbiHash,
            new DateRangeParam(new DateParam("ge1970-01-01"), null),
            null,
            List.of()),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-service-date",
            badMbiHash,
            null,
            new DateRangeParam(new DateParam("ge1970-01-01"), null),
            List.of()),
        new ServiceDateQueryParam(
            "lastUpdated-mismatch",
            goodMbiHash,
            new DateRangeParam(new DateParam("gt1970-08-08"), null),
            new DateRangeParam(new DateParam("ge1970-01-01"), null),
            List.of()),
        new ServiceDateQueryParam(
            "lastUpdated-matches-1",
            goodMbiHash,
            new DateRangeParam(
                new DateParam("ge1970-08-01T00:00:00Z"), new DateParam("lt1970-08-07T00:00:00Z")),
            new DateRangeParam(new DateParam("ge1970-01-01"), null),
            List.of(RDATestUtils.FISS_CLAIM_A_CLAIM_ID)),
        new ServiceDateQueryParam(
            "lastUpdated-matches-2",
            goodMbiHash,
            new DateRangeParam(new DateParam("ge1970-08-01T00:00:00Z"), null),
            new DateRangeParam(new DateParam("ge1970-01-01"), null),
            List.of(RDATestUtils.FISS_CLAIM_A_CLAIM_ID, RDATestUtils.FISS_CLAIM_B_CLAIM_ID)),
        new ServiceDateQueryParam(
            "serviceDate-matches-1-from",
            goodMbiHash,
            null,
            new DateRangeParam(null, new DateParam("lt1970-07-11")),
            List.of(RDATestUtils.FISS_CLAIM_A_CLAIM_ID)),
        new ServiceDateQueryParam(
            "serviceDate-matches-1-to",
            goodMbiHash,
            null,
            new DateRangeParam(new DateParam("gt1970-08-01"), null),
            List.of(RDATestUtils.FISS_CLAIM_B_CLAIM_ID)),
        new ServiceDateQueryParam(
            "serviceDate-matches-2",
            goodMbiHash,
            null,
            new DateRangeParam(new DateParam("ge1970-07-19"), new DateParam("lt1970-08-01")),
            List.of(RDATestUtils.FISS_CLAIM_A_CLAIM_ID, RDATestUtils.FISS_CLAIM_B_CLAIM_ID)));
  }

  /**
   * Runs a query using the given test parameters to find matching FISS claims.
   *
   * @param testParam defines the test case to run
   */
  @ParameterizedTest()
  @MethodSource("getFissServiceDateQueryParameters")
  protected void testFissServiceDateQuery(ServiceDateQueryParam testParam) {
    final ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    final var claims =
        claimDao.findAllByMbiAttribute(
            ClaimTypeV2.F,
            testParam.mbiHash,
            true,
            testParam.lastUpdatedParam,
            testParam.serviceDateParam);
    assertEquals(
        testParam.expectedClaimIds,
        claims.stream().map(RdaFissClaim::getClaimId).collect(Collectors.toList()));
  }

  /**
   * Generates parameters for {@link ClaimDaoIT#testMcsServiceDateQuery}.
   *
   * @return all test parameters
   */
  private static Stream<ServiceDateQueryParam> getMcsServiceDateQueryParameters() {
    final var goodMbiHash = RDATestUtils.MBI_HASH;
    final var badMbiHash = "not-a-valid-hash";
    return Stream.of(
        new ServiceDateQueryParam(
            "no-dates-matches-all",
            goodMbiHash,
            null,
            null,
            List.of(
                "both-same",
                "claim-earlier",
                "claim-empty-dtl",
                "claim-only",
                "detail-earlier",
                "detail-only",
                "multi-detail",
                "no-dates")),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-no-dates", badMbiHash, null, null, List.of()),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-last-updated",
            badMbiHash,
            new DateRangeParam(new DateParam("ge2022-01-01"), null),
            null,
            List.of()),
        new ServiceDateQueryParam(
            "bad-mbi-matches-none-service-date",
            badMbiHash,
            null,
            new DateRangeParam(new DateParam("ge2022-01-01"), null),
            List.of()),
        new ServiceDateQueryParam(
            "lastUpdated-mismatch",
            goodMbiHash,
            new DateRangeParam(new DateParam("gt2022-08-03"), null),
            new DateRangeParam(new DateParam("ge2022-01-01"), null),
            List.of()),
        new ServiceDateQueryParam(
            "lastUpdated-matches-3",
            goodMbiHash,
            new DateRangeParam(new DateParam("ge2022-06-06T00:00:00Z"), null),
            new DateRangeParam(new DateParam("ge2022-01-01"), null),
            List.of("claim-earlier", "detail-earlier", "multi-detail")),
        new ServiceDateQueryParam(
            "serviceDate-matches-all-dated",
            goodMbiHash,
            null,
            new DateRangeParam(new DateParam("ge2022-05-25"), new DateParam("le2022-06-01")),
            List.of(
                "both-same", "claim-empty-dtl", "claim-only", "detail-earlier", "multi-detail")),
        new ServiceDateQueryParam(
            "serviceDate-matches-3",
            goodMbiHash,
            null,
            new DateRangeParam(new DateParam("ge2022-05-28"), new DateParam("le2022-05-30")),
            List.of("both-same", "detail-earlier")));
  }

  /**
   * Establishes a known set of MCS claims and runs a query using the given test parameters.
   *
   * @param testParam defines the test case to run
   */
  @ParameterizedTest()
  @MethodSource("getMcsServiceDateQueryParameters")
  protected void testMcsServiceDateQuery(ServiceDateQueryParam testParam) {
    final ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedMbiRecord();
    testUtils.seedMcsClaimForServiceIdTest("no-dates", LocalDate.of(2022, 6, 1), null, List.of());
    testUtils.seedMcsClaimForServiceIdTest(
        "claim-only", LocalDate.of(2022, 6, 2), LocalDate.of(2022, 5, 25), List.of());
    testUtils.seedMcsClaimForServiceIdTest(
        "claim-empty-dtl",
        LocalDate.of(2022, 6, 3),
        LocalDate.of(2022, 5, 26),
        Arrays.asList(null, null));
    testUtils.seedMcsClaimForServiceIdTest(
        "detail-only", LocalDate.of(2022, 6, 4), null, List.of(LocalDate.of(2022, 5, 27)));
    testUtils.seedMcsClaimForServiceIdTest(
        "both-same",
        LocalDate.of(2022, 6, 5),
        LocalDate.of(2022, 5, 28),
        List.of(LocalDate.of(2022, 5, 28)));
    testUtils.seedMcsClaimForServiceIdTest(
        "claim-earlier",
        LocalDate.of(2022, 6, 6),
        LocalDate.of(2022, 5, 15),
        List.of(LocalDate.of(2022, 5, 29)));
    testUtils.seedMcsClaimForServiceIdTest(
        "detail-earlier",
        LocalDate.of(2022, 6, 7),
        LocalDate.of(2022, 5, 30),
        List.of(LocalDate.of(2022, 5, 15)));
    testUtils.seedMcsClaimForServiceIdTest(
        "multi-detail",
        LocalDate.of(2022, 6, 8),
        LocalDate.of(2022, 6, 1),
        Arrays.asList(
            LocalDate.of(2022, 5, 15), LocalDate.of(2022, 5, 20), null, LocalDate.of(2022, 6, 1)));
    final var claims =
        runMcsServiceDateQuery(
            claimDao, testParam.mbiHash, testParam.lastUpdatedParam, testParam.serviceDateParam);
    assertEquals(
        testParam.expectedClaimIds,
        claims.stream().map(RdaMcsClaim::getIdrClmHdIcn).collect(Collectors.toList()));
  }

  /**
   * Helper function to run the common MBI lookup method on the {@link ClaimDao}.
   *
   * @param claimDao The {@link ClaimDao} to execute the query on.
   * @param mbi The MBI value to lookup.
   * @return The claims that were found from the lookup on the {@link ClaimDao} with the given MBI.
   */
  private List<RdaFissClaim> runFissMbiQuery(ClaimDao claimDao, String mbi) {
    return claimDao.findAllByMbiAttribute(ClaimResponseTypeV2.F, mbi, false, null, null);
  }

  /**
   * Helper function to run the common MBI hash lookup method on the {@link ClaimDao}.
   *
   * @param claimDao The {@link ClaimDao} to execute the query on.
   * @param mbiHash The MBI hash value to lookup.
   * @return The claims that were found from the lookup on the {@link ClaimDao} with the given MBI
   *     hash.
   */
  private List<RdaMcsClaim> runMcsMbiHashQuery(ClaimDao claimDao, String mbiHash) {
    return claimDao.findAllByMbiAttribute(ClaimTypeV2.M, mbiHash, true, null, null);
  }

  /**
   * Helper function to run the MBI hash lookup method on the {@link ClaimDao} with the given date
   * range parameters.
   *
   * @param claimDao The {@link ClaimDao} to execute the query on.
   * @param mbiHash the hashed MBI value to use
   * @param lastUpdated the (possibly null) lastUpdated {@link DateRangeParam}
   * @param serviceDate The (possibly null) service date {@link DateRangeParam}
   * @return The claims that were found from the lookup on the {@link ClaimDao} with the given MBI
   *     hash and satisfying the date requirement.
   */
  private List<RdaMcsClaim> runMcsServiceDateQuery(
      ClaimDao claimDao,
      String mbiHash,
      @Nullable DateRangeParam lastUpdated,
      @Nullable DateRangeParam serviceDate) {
    return claimDao.findAllByMbiAttribute(ClaimTypeV2.M, mbiHash, true, lastUpdated, serviceDate);
  }

  /**
   * Parameter object defining a test case for {@link ClaimDaoIT#testMcsServiceDateQuery} and {@link
   * ClaimDaoIT#testFissServiceDateQuery}.
   */
  @AllArgsConstructor
  private static class ServiceDateQueryParam {
    /** Meaningful name for test case. Used for toString() method. */
    private final String testName;

    /** MBI hash value for {@link ClaimDaoIT#runMcsServiceDateQuery}. */
    private final String mbiHash;

    /** lastUpdatedParam value for {@link ClaimDaoIT#runMcsServiceDateQuery}. */
    @Nullable private final DateRangeParam lastUpdatedParam;

    /** serviceDateParam value for {@link ClaimDaoIT#runMcsServiceDateQuery}. */
    @Nullable private final DateRangeParam serviceDateParam;

    /** List of claimId values expected in the query result. */
    private final List<String> expectedClaimIds;

    @Override
    public String toString() {
      return testName;
    }
  }
}
