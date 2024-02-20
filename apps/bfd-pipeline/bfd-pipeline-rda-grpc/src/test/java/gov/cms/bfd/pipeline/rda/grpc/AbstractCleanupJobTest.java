package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.pipeline.CleanupTestUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for {@link gov.cms.bfd.pipeline.rda.grpc.AbstractCleanupJob}. */
class AbstractCleanupJobTest {

  /** test utilities. */
  private CleanupTestUtils utils;

  /** test initialization. */
  @BeforeEach
  void init() {
    utils = new CleanupTestUtils();
    utils.init();
  }

  /** test teardown. */
  @AfterEach
  void cleanupAfterEach() {
    utils.truncateTables();
  }

  /**
   * Basic test scenario, normal case where it executes multiple transactions to remove old claims.
   *
   * @throws ProcessingException if errors occurs during processing.
   */
  @Test
  void runComplete() throws ProcessingException {
    var cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
    utils.seedData(cutoff, 8, 4);

    var cleanUpJob = new RdaFissClaimCleanupJob(utils.getTransactionManager(), 6, 2, true);
    var deleted = cleanUpJob.run();

    // after first run 6 should be deleted and 6 remaining
    assertEquals(6, deleted);

    deleted = cleanUpJob.run();

    // after second run 2 should be deleted and 4 remaining, none older than 60 days;
    assertEquals(2, deleted);
    assertEquals(4, utils.count());
    assertTrue(utils.oldestLastUpdatedDate().isAfter(cutoff));

    deleted = cleanUpJob.run();

    // after third run 0 should be deleted and 4 remaining, none older than 60 days;
    assertEquals(0, deleted);
    assertEquals(4, utils.count());
    assertTrue(utils.oldestLastUpdatedDate().isAfter(cutoff));
  }

  /**
   * Test scenario where there are no old claims to delete.
   *
   * @throws ProcessingException if errors occurs during processing.
   */
  @Test
  void runNoneFound() throws ProcessingException {
    var cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
    utils.seedData(cutoff, 0, 8);

    var cleanUpJob = new RdaFissClaimCleanupJob(utils.getTransactionManager(), 6, 2, true);
    var deleted = cleanUpJob.run();

    // expecting no deletes and all claims still remaining
    assertEquals(0, deleted);
    assertEquals(8, utils.count());
    assertTrue(utils.oldestLastUpdatedDate().isAfter(cutoff));
  }

  /**
   * Test scenario where all claims are older that 60 days.
   *
   * @throws ProcessingException if errors occurs during processing.
   */
  @Test
  void runAllDeleted() throws ProcessingException {
    var cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
    utils.seedData(cutoff, 5, 0);

    var cleanUpJob = new RdaFissClaimCleanupJob(utils.getTransactionManager(), 6, 2, true);
    var deleted = cleanUpJob.run();

    // expecting 5 deletes and no claims remaining
    assertEquals(5, deleted);
    assertEquals(0, utils.count());
  }

  /**
   * Test scenario where there are no claims.
   *
   * @throws ProcessingException if errors occurs during processing.
   */
  @Test
  void runEmpty() throws ProcessingException {
    var cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
    utils.seedData(cutoff, 0, 0);

    var cleanUpJob = new RdaFissClaimCleanupJob(utils.getTransactionManager(), 6, 2, true);
    var deleted = cleanUpJob.run();

    // expecting 0 deletes and no claims remaining
    assertEquals(0, deleted);
    assertEquals(0, utils.count());
  }

  /**
   * Test scenario where the job is not enabled.
   *
   * @throws ProcessingException if errors occurs during processing.
   */
  @Test
  void notEnabled() throws ProcessingException {
    var cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
    utils.seedData(cutoff, 4, 4);

    var cleanUpJob = new RdaFissClaimCleanupJob(utils.getTransactionManager(), 6, 2, false);
    var deleted = cleanUpJob.run();

    // expecting 0 deletes and all 8 claims remaining
    assertEquals(0, deleted);
    assertEquals(8, utils.count());
  }
}
