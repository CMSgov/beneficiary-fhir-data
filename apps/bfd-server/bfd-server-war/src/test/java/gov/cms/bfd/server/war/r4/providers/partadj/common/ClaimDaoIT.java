package gov.cms.bfd.server.war.r4.providers.partadj.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PartAdjFissClaim;
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

  @Test
  public void verifyQueryWithKnownMbiFindsMatch() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiQuery(claimDao, RDATestUtils.MBI);
    assertEquals(2, claims.size());
  }

  @Test
  public void verifyQueryWithUnknownMbiFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiQuery(claimDao, "not-an-mbi");
    assertEquals(0, claims.size());
  }

  @Test
  public void verifyQueryWithKnownMbiHashFindsMatch() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
    assertEquals(2, claims.size());
  }

  @Test
  public void verifyQueryWithUnknownMbiHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(false);
    var claims = runFissMbiHashQuery(claimDao, "not-an-mbi-hash");
    assertEquals(0, claims.size());
  }

  @Test
  public void verifyQueryWithOldHashDisabledIgnoresOldHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, false);
    testUtils.seedData(true);
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
    assertEquals(0, claims.size());
  }

  @Test
  public void verifyQueryWithOldHashEnabledFindsHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_HASH);
    assertEquals(2, claims.size());
  }

  @Test
  public void verifyQueryWithOldHashEnabledFindsOldHash() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runFissMbiHashQuery(claimDao, RDATestUtils.MBI_OLD_HASH);
    assertEquals(2, claims.size());
  }

  @Test
  public void verifyQueryWithOldHashEnabledAndUnknownHashFindsNothing() {
    ClaimDao claimDao = new ClaimDao(testUtils.getEntityManager(), metricRegistry, true);
    testUtils.seedData(true);
    var claims = runFissMbiHashQuery(claimDao, "not-a-hash");
    assertEquals(0, claims.size());
  }

  private List<PartAdjFissClaim> runFissMbiQuery(ClaimDao claimDao, String mbi) {
    return claimDao.findAllByMbiAttribute(
        PartAdjFissClaim.class,
        PartAdjFissClaim.Fields.mbiRecord,
        mbi,
        false,
        null,
        null,
        PartAdjFissClaim.Fields.stmtCovToDate);
  }

  private List<PartAdjFissClaim> runFissMbiHashQuery(ClaimDao claimDao, String mbiHash) {
    return claimDao.findAllByMbiAttribute(
        PartAdjFissClaim.class,
        PartAdjFissClaim.Fields.mbiRecord,
        mbiHash,
        true,
        null,
        null,
        PartAdjFissClaim.Fields.stmtCovToDate);
  }
}
