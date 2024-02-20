package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of the AbstractCleanupJob for the RDA FISS claims table. */
public class RdaFissClaimCleanupJob extends AbstractCleanupJob {

  /** logger to use. */
  static final Logger LOGGER = LoggerFactory.getLogger(RdaFissClaimCleanupJob.class);

  /** List of table names for use in native queries. Parent claims table must be last. */
  private static final List<String> TABLE_NAMES =
      List.of(
          "rda.fiss_revenue_lines",
          "rda.fiss_payers",
          "rda.fiss_diagnosis_codes",
          "rda.fiss_proc_codes",
          "rda.fiss_audit_trails",
          "rda.fiss_claims");

  /** Key column name for the parent table for use in native queries. */
  private static final String KEY_COLUMN = "claim_id";

  /**
   * Constructs a RdaFissClaimCleanupJob.
   *
   * @param transactionManager the TransactionManager to use.
   * @param claimsPerRun the number of claims to remove in a single run of this job.
   * @param claimsPerTransaction the number of claims to remove in a single transaction.
   * @param enabled true if this job should run, false otherwise.
   */
  public RdaFissClaimCleanupJob(
      TransactionManager transactionManager,
      int claimsPerRun,
      int claimsPerTransaction,
      boolean enabled) {
    super(transactionManager, claimsPerRun, claimsPerTransaction, enabled, LOGGER);
  }

  /** {@inheritDoc} */
  @Override
  List<String> getTableNames() {
    return TABLE_NAMES;
  }

  /** {@inheritDoc} */
  @Override
  String getParentTableKey() {
    return KEY_COLUMN;
  }
}
