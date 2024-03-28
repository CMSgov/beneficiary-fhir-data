package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** Test class for {@link gov.cms.bfd.pipeline.rda.grpc.RdaFissClaimCleanupJob}. */
class RdaFissClaimCleanupJobTest {

  /** Mock TransactionManager for testing. */
  @Mock private TransactionManager transactionManager;

  /** The RdaFissClaimCleanupJob instance under test. */
  private RdaFissClaimCleanupJob job;

  /** Test initialization. */
  @BeforeEach
  void init() {
    job = new RdaFissClaimCleanupJob(transactionManager, 100000, 5000, true);
  }

  /** Verifies the getTableNames method. */
  @Test
  void getTableNames() {
    assertNotNull(job.getParentTableName());
  }

  /** Verifies the getParentTableKey method. */
  @Test
  void getParentTableKey() {
    assertEquals("claim_id", job.getParentTableKey());
  }
}
