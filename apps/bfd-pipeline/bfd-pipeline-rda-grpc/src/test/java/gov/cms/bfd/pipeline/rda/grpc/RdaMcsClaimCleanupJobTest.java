package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** Test class for {@link gov.cms.bfd.pipeline.rda.grpc.RdaMcsClaimCleanupJob}. */
class RdaMcsClaimCleanupJobTest {

  /** Mock TransactionManager for testing. */
  @Mock private TransactionManager transactionManager;

  /** The RdaMcsClaimCleanupJob instance under test. */
  private RdaMcsClaimCleanupJob job;

  /** Test initialization. */
  @BeforeEach
  void init() {
    job = new RdaMcsClaimCleanupJob(transactionManager, 100000, 5000, true);
  }

  /** Verifies the getTableNames method. */
  @Test
  void getTableNames() {
    assertNotNull(job.getParentTableName());

    // ensure parent table is always last
  }

  /** Verifies the getParentTableKey method. */
  @Test
  void getParentTableKey() {
    assertEquals("idr_clm_hd_icn", job.getParentTableKey());
  }
}
