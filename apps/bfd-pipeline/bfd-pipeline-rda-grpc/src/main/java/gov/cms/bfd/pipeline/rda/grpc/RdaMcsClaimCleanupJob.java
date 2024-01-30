package gov.cms.bfd.pipeline.rda.grpc;

import java.util.List;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of the AbstractCleanupJob for the RDA MCS claims table. */
public class RdaMcsClaimCleanupJob extends AbstractCleanupJob {

  /** logger to use. */
  static final Logger LOGGER = LoggerFactory.getLogger(RdaMcsClaimCleanupJob.class);

  /** List of child entity class names for use in jpql. */
  private static final List<String> CHILD_ENTITY_NAMES =
      List.of(
          "RdaFissRevenueLine",
          "RdaFissPayer",
          "RdaFissDiagnosisCode",
          "RdaFissProcCode",
          "RdaFissAuditTrail");

  /** Principal entity class name for use in jpql. */
  private static final String ENTITY_NAME = "RdaFissClaim";

  /** Principal entity db table name for use in native query. */
  private static final String ENTITY_TABLE_NAME = "rda.fiss_claims";

  /**
   * Constructs a RdaMcsClaimCleanupJob.
   *
   * @param entityManagerFactory the EntityManagerFactory to use.
   * @param claimsPerTransaction the number of claims to remove in a single transaction.
   * @param claimsPerRun the number of claims to remove in a single run of this job.
   * @param enabled true if this job should run, false otherwise.
   */
  public RdaMcsClaimCleanupJob(
      EntityManagerFactory entityManagerFactory,
      int claimsPerTransaction,
      int claimsPerRun,
      boolean enabled) {
    super(entityManagerFactory, claimsPerTransaction, claimsPerRun, enabled, LOGGER);
  }

  /** {@inheritDoc} */
  @Override
  List<String> getChildEntityNames() {
    return CHILD_ENTITY_NAMES;
  }

  /** {@inheritDoc} */
  @Override
  String getEntityName() {
    return ENTITY_NAME;
  }

  /** {@inheritDoc} */
  @Override
  String getEntityTableName() {
    return ENTITY_TABLE_NAME;
  }
}
