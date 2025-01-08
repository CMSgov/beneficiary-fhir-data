package gov.cms.bfd.pipeline.sharedutils;

/** Defines the desired behavior when the pipeline finishes running. */
public enum PipelineOutcome {
  /** The pipeline service should shut down. */
  STOP_SERVICE,
  /** The underlying EC2 instance should terminate. */
  TERMINATE_INSTANCE
}
