package gov.cms.bfd.pipeline.rda.grpc;

/** Interface for CleanupJobs. */
public interface CleanupJob {
  /**
   * Entry point for executing cleanup jobs.
   *
   * @return the number of claims deleted.
   */
  int run();
}
