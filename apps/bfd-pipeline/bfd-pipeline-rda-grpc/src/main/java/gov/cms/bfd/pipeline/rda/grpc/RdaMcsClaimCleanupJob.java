package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of the AbstractCleanupJob for the RDA MCS claims table. */
public class RdaMcsClaimCleanupJob extends AbstractCleanupJob {

  /** logger to use. */
  static final Logger LOGGER = LoggerFactory.getLogger(RdaMcsClaimCleanupJob.class);

  /** List of table names for use in native queries. Parent claims table must be last. */
  private static final List<String> TABLE_NAMES =
      List.of(
          "rda.mcs_adjustments",
          "rda.mcs_audits",
          "rda.mcs_details",
          "rda.mcs_diagnosis_codes",
          "rda.mcs_locations",
          "rda.mcs_claims");

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
  List<String> getTableNames() {
    return TABLE_NAMES;
  }

  /** {@inheritDoc} */
  @Override
  String getParentTableKey() {
    return KEY_COLUMN;
  }
}
