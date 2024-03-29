package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of the AbstractCleanupJob for the RDA MCS claims table. */
public class RdaMcsClaimCleanupJob extends AbstractCleanupJob {

  /** logger to use. */
  static final Logger LOGGER = LoggerFactory.getLogger(RdaMcsClaimCleanupJob.class);

  /** Parent table name of the query. */
  private static final String PARENT_TABLE_NAME = "rda.mcs_claims";

  /** Key column name for the parent table for use in native queries. */
  private static final String KEY_COLUMN = "idr_clm_hd_icn";

  /**
   * Constructs a RdaMcsClaimCleanupJob.
   *
   * @param transactionManager the TransactionManager to use.
   * @param claimsPerRun the number of claims to remove in a single run of this job.
   * @param claimsPerTransaction the number of claims to remove in a single transaction.
   * @param enabled true if this job should run, false otherwise.
   */
  public RdaMcsClaimCleanupJob(
      TransactionManager transactionManager,
      int claimsPerRun,
      int claimsPerTransaction,
      boolean enabled) {
    super(transactionManager, claimsPerRun, claimsPerTransaction, enabled, LOGGER);
  }

  /** {@inheritDoc} */
  @Override
  String getParentTableName() {
    return PARENT_TABLE_NAME;
  }

  /** {@inheritDoc} */
  @Override
  String getParentTableKey() {
    return KEY_COLUMN;
  }
}
