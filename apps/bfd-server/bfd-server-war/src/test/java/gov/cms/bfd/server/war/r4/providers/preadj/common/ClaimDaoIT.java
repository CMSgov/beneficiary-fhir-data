package gov.cms.bfd.server.war.r4.providers.preadj.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClaimDaoIT {
  private static final RDATestUtils testUtils = new RDATestUtils();
  private final MetricRegistry metricRegistry = new MetricRegistry();

  @BeforeAll
  public static void init() {
    testUtils.init();
  }

  @AfterAll
  public static void tearDown() {
    testUtils.destroy();
  }

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
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
    assertEquals(2, claims.size());
  }

  /** Verifies that doing a claims search with an unknown MBI hash will return no claims. */
  @Test
  public void verifyQueryWithUnknownMbiHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiHashQuery(claimDao, "not-an-mbi-hash");
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
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
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
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
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
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
    assertEquals(2, claims.size());
  }

  /**
   * Verifies that doing a claims search with an unknown MBI hash returns no results even with old
   * hash enabled
   */
  @Test
  public void verifyQueryWithOldHashEnabledAndUnknownHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runFissMbiHashQuery(claimDao, "not-a-hash");
    assertEquals(0, claims.size());
  }

  /**
   * Helper function to run the common MBI lookup method on the {@link ClaimDao}.
   *
   * @param claimDao The {@link ClaimDao} to execute the query on.
   * @param mbi The MBI value to lookup.
   * @return The claims that were found from the lookup on the {@link ClaimDao} with the given MBI.
   */
  private List<RdaFissClaim> runFissMbiQuery(ClaimDao claimDao, String mbi) {
    return claimDao.findAllByMbiAttribute(
        RdaFissClaim.class,
        RdaFissClaim.Fields.mbiRecord,
        mbi,
        false,
        null,
        null,
        RdaFissClaim.Fields.dcn,
        RdaFissClaim.Fields.stmtCovToDate);
  }

  /**
   * Helper function to run the common MBI hash lookup method on the {@link ClaimDao}.
   *
   * @param claimDao The {@link ClaimDao} to execute the query on.
   * @param mbiHash The MBI hash value to lookup.
   * @return The claims that were found from the lookup on the {@link ClaimDao} with the given MBI
   *     hash.
   */
  private List<RdaFissClaim> runFissMbiHashQuery(ClaimDao claimDao, String mbiHash) {
    return claimDao.findAllByMbiAttribute(
        RdaFissClaim.class,
        RdaFissClaim.Fields.mbiRecord,
        mbiHash,
        true,
        null,
        null,
        RdaFissClaim.Fields.dcn,
        RdaFissClaim.Fields.stmtCovToDate);
  }
}
