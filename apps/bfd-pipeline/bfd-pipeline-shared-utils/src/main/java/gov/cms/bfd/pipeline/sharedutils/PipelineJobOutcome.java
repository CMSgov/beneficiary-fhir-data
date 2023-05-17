package gov.cms.bfd.pipeline.sharedutils;

/**
 * Enumerates the possible successful outcomes of a {@link PipelineJob} run. Please note that job
 * failure is not represented here, as {@link PipelineJob}s report failures via exceptions, instead.
 */
public enum PipelineJobOutcome {
  /**
   * Indicates that the {@link PipelineJob} completed successfully, but didn't have any data to
   * process or work to do (as opposed to {@link #WORK_DONE}.
   */
  NOTHING_TO_DO,

  /**
   * Indicates that the {@link PipelineJob} completed successfully, having processed some data or
   * done whatever work needed doing (as opposed to {@link #NOTHING_TO_DO}.
   */
  WORK_DONE;
}
