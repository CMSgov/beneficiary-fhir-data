package gov.cms.bfd.pipeline.rda.grpc;

import javax.persistence.EntityManagerFactory;

/** Concrete implementation of the AbstractCleanupJob for the RDA FISS claims table. */
public class RdaFissClaimCleanupJob extends AbstractCleanupJob {

  /**
   * Constructs a RdaFissClaimCleanupJob.
   *
   * @param entityManagerFactory the entitymanagerfactory to use.
   * @param claimsPerTransaction the number of claims to remove in a single transaction.
   * @param claimsPerRun the number of claims to remove in a single run of this job.
   * @param enabled true if this job should run, false otherwise.
   */
  public RdaFissClaimCleanupJob(
      EntityManagerFactory entityManagerFactory,
      int claimsPerTransaction,
      int claimsPerRun,
      boolean enabled) {
    super(
        entityManagerFactory,
        claimsPerTransaction,
        claimsPerRun,
        RDA_FISS_CLAIMS_ENTITY,
        RDA_FISS_CLAIMS_TABLE,
        enabled);
  }
}
